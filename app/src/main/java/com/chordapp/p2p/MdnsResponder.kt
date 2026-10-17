package com.chordapp.p2p

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket

/**
 * Lightweight embedded mDNS A-record responder.
 * Listens on 224.0.0.251:5353 and resolves queries for [hostname] (e.g. "kordapp.local")
 * directly to the device's current local IP address, enabling laptops and browsers
 * to connect via http://kordapp.local:8990 without hosts file modifications.
 */
class MdnsResponder(
    val hostname: String = "kordapp.local",
    private val getIpAddress: () -> String?
) {
    companion object {
        private const val TAG = "MdnsResponder"
        const val MDNS_GROUP = "224.0.0.251"
        const val MDNS_PORT = 5353
    }

    private var job: Job? = null
    private var socket: MulticastSocket? = null

    fun start(scope: CoroutineScope) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            var s: MulticastSocket? = null
            try {
                val group = InetAddress.getByName(MDNS_GROUP)
                s = MulticastSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(MDNS_PORT))
                    try {
                        joinGroup(group)
                    } catch (e: Exception) {
                        Log.w(TAG, "joinGroup mDNS failed: ${e.message}")
                    }
                    soTimeout = 0
                }
                socket = s
                Log.d(TAG, "mDNS A-record responder active on port $MDNS_PORT for $hostname")

                val buffer = ByteArray(1500)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)
                    handlePacket(packet, s)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "mDNS responder socket error", e)
                }
            } finally {
                try { s?.close() } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }

    internal fun handlePacket(packet: DatagramPacket, s: DatagramSocket) {
        val data = packet.data
        val len = packet.length
        if (len < 12) return

        // QR bit (bit 7 of byte 2) must be 0 (standard query)
        val flags = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return

        val qdCount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        if (qdCount < 1) return

        // Parse QNAME starting at byte 12
        var idx = 12
        val qnameParts = mutableListOf<String>()
        while (idx < len) {
            val labelLen = data[idx].toInt() and 0xFF
            if (labelLen == 0) {
                idx++
                break
            }
            if ((labelLen and 0xC0) == 0xC0) {
                idx += 2
                break
            }
            idx++
            if (idx + labelLen > len) return
            val part = String(data, idx, labelLen, Charsets.US_ASCII)
            qnameParts.add(part)
            idx += labelLen
        }

        val queryName = qnameParts.joinToString(".").lowercase()
        if (idx + 4 > len) return
        val qtype = ((data[idx].toInt() and 0xFF) shl 8) or (data[idx + 1].toInt() and 0xFF)
        val qclassRaw = ((data[idx + 2].toInt() and 0xFF) shl 8) or (data[idx + 3].toInt() and 0xFF)
        val unicastResponse = (qclassRaw and 0x8000) != 0

        // Check if query matches target host (kordapp.local or kordapp)
        val target = hostname.lowercase().trimEnd('.')
        val cleanQuery = queryName.trimEnd('.')
        if (cleanQuery == target || cleanQuery == target.removeSuffix(".local")) {
            // Type A (1) or ANY (255)
            if (qtype == 1 || qtype == 255) {
                val currentIp = getIpAddress() ?: return
                val ipBytes = parseIpv4(currentIp) ?: return
                val queryId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                val responseBytes = buildMdnsResponse(queryId, target, ipBytes)

                try {
                    // Send to multicast group
                    val sendGroup = InetAddress.getByName(MDNS_GROUP)
                    val multicastPacket = DatagramPacket(responseBytes, responseBytes.size, sendGroup, MDNS_PORT)
                    s.send(multicastPacket)

                    // Also send unicast to requester if requested or from non-5353 ephemeral port
                    if (unicastResponse || packet.port != MDNS_PORT) {
                        val unicastPacket = DatagramPacket(responseBytes, responseBytes.size, packet.socketAddress)
                        s.send(unicastPacket)
                    }
                    Log.d(TAG, "Answered mDNS query for $cleanQuery -> $currentIp to ${packet.socketAddress}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send mDNS reply", e)
                }
            }
        }
    }

    internal fun buildMdnsResponse(
        queryId: Int,
        targetHost: String,
        ipBytes: ByteArray
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        // 1. Transaction ID
        bos.write((queryId shr 8) and 0xFF)
        bos.write(queryId and 0xFF)
        // 2. Flags: 0x8400 (Response + Authoritative Answer)
        bos.write(0x84)
        bos.write(0x00)
        // 3. QDCOUNT: 0
        bos.write(0x00)
        bos.write(0x00)
        // 4. ANCOUNT: 1
        bos.write(0x00)
        bos.write(0x01)
        // 5. NSCOUNT: 0
        bos.write(0x00)
        bos.write(0x00)
        // 6. ARCOUNT: 0
        bos.write(0x00)
        bos.write(0x00)

        // Answer Section:
        // QNAME: write labels for targetHost + ".local"
        val fullHost = if (targetHost.endsWith(".local")) targetHost else "$targetHost.local"
        for (label in fullHost.split('.')) {
            if (label.isNotEmpty()) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                bos.write(bytes.size)
                bos.write(bytes)
            }
        }
        bos.write(0x00)

        // TYPE: A (0x0001)
        bos.write(0x00)
        bos.write(0x01)

        // CLASS: IN with cache-flush bit (0x8001)
        bos.write(0x80)
        bos.write(0x01)

        // TTL: 120 seconds (0x00000078)
        bos.write(0x00)
        bos.write(0x00)
        bos.write(0x00)
        bos.write(0x78)

        // RDLENGTH: 4 bytes
        bos.write(0x00)
        bos.write(0x04)

        // RDATA: IPv4 bytes
        bos.write(ipBytes)

        return bos.toByteArray()
    }

    private fun parseIpv4(ip: String): ByteArray? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        val bytes = ByteArray(4)
        for (i in 0..3) {
            val num = parts[i].toIntOrNull() ?: return null
            if (num !in 0..255) return null
            bytes[i] = num.toByte()
        }
        return bytes
    }
}
