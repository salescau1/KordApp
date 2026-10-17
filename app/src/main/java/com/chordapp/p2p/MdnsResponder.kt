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
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket

/**
 * Embedded mDNS and LLMNR A-record responder.
 * Listens on:
 * - mDNS: 224.0.0.251:5353 (IPv4) & [FF02::FB]:5353 (IPv6)
 * - LLMNR: 224.0.0.252:5355 (IPv4) & [FF02::1:3]:5355 (IPv6)
 *
 * Automatically answers queries for [hostname] (e.g. "kordapp.local" or "kordapp")
 * directly to the device's current local IP address, enabling Windows, macOS, iOS,
 * Android, Chrome, and Brave to connect via http://kordapp.local:8990.
 */
class MdnsResponder(
    val hostname: String = "kordapp.local",
    private val getIpAddress: () -> String?
) {
    companion object {
        private const val TAG = "MdnsResponder"
        const val MDNS_GROUP_IPV4 = "224.0.0.251"
        const val MDNS_GROUP_IPV6 = "FF02::FB"
        const val MDNS_PORT = 5353

        const val LLMNR_GROUP_IPV4 = "224.0.0.252"
        const val LLMNR_GROUP_IPV6 = "FF02::1:3"
        const val LLMNR_PORT = 5355
    }

    private var mdnsJob: Job? = null
    private var llmnrJob: Job? = null
    private var mdnsSocket: MulticastSocket? = null
    private var llmnrSocket: MulticastSocket? = null

    fun start(scope: CoroutineScope) {
        stop()
        mdnsJob = scope.launch(Dispatchers.IO) {
            runSocketLoop(
                port = MDNS_PORT,
                groups = listOf(MDNS_GROUP_IPV4, MDNS_GROUP_IPV6),
                isLlmnr = false
            ) { s -> mdnsSocket = s }
        }

        llmnrJob = scope.launch(Dispatchers.IO) {
            runSocketLoop(
                port = LLMNR_PORT,
                groups = listOf(LLMNR_GROUP_IPV4, LLMNR_GROUP_IPV6),
                isLlmnr = true
            ) { s -> llmnrSocket = s }
        }
    }

    fun stop() {
        mdnsJob?.cancel()
        mdnsJob = null
        llmnrJob?.cancel()
        llmnrJob = null

        try { mdnsSocket?.close() } catch (_: Exception) {}
        mdnsSocket = null

        try { llmnrSocket?.close() } catch (_: Exception) {}
        llmnrSocket = null
    }

    private suspend fun runSocketLoop(
        port: Int,
        groups: List<String>,
        isLlmnr: Boolean,
        onSocketCreated: (MulticastSocket) -> Unit
    ) {
        var s: MulticastSocket? = null
        try {
            s = MulticastSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(port))
                for (group in groups) {
                    try {
                        joinGroup(InetAddress.getByName(group))
                    } catch (e: Exception) {
                        Log.w(TAG, "joinGroup $group:$port failed: ${e.message}")
                    }
                }
                soTimeout = 0
            }
            onSocketCreated(s)
            Log.d(TAG, "Responder active on port $port for $hostname (LLMNR=$isLlmnr)")

            val buffer = ByteArray(1500)
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                s.receive(packet)
                handlePacket(packet, s, port, isLlmnr)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Socket loop error on port $port: ${e.message}")
        } finally {
            try { s?.close() } catch (_: Exception) {}
        }
    }

    internal fun handlePacket(
        packet: DatagramPacket,
        s: DatagramSocket,
        port: Int = MDNS_PORT,
        isLlmnr: Boolean = false
    ) {
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

        // Check if query matches target host (kordapp.local or kordapp)
        val target = hostname.lowercase().trimEnd('.')
        val cleanQuery = queryName.trimEnd('.')
        val matches = cleanQuery == target ||
                cleanQuery == target.removeSuffix(".local") ||
                "$cleanQuery.local" == target

        if (matches && (qtype == 1 || qtype == 255)) {
            val currentIp = getIpAddress() ?: return
            val ipBytes = parseIpv4(currentIp) ?: return
            val queryId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)

            val matchedHost = if (cleanQuery.endsWith(".local") || !isLlmnr) {
                if (target.endsWith(".local")) target else "$target.local"
            } else {
                target.removeSuffix(".local")
            }

            val responseBytes = buildDnsResponse(
                queryId = queryId,
                targetHost = matchedHost,
                ipBytes = ipBytes,
                isLlmnr = isLlmnr
            )

            // 1. ALWAYS send unicast directly back to the requester
            try {
                val unicastPacket = DatagramPacket(responseBytes, responseBytes.size, packet.socketAddress)
                s.send(unicastPacket)
            } catch (e: Exception) {
                Log.w(TAG, "Unicast reply failed to ${packet.socketAddress}: ${e.message}")
            }

            // 2. Also send multicast to the matching address family
            try {
                val mcastHost = if (isLlmnr) {
                    if (packet.address is Inet6Address) LLMNR_GROUP_IPV6 else LLMNR_GROUP_IPV4
                } else {
                    if (packet.address is Inet6Address) MDNS_GROUP_IPV6 else MDNS_GROUP_IPV4
                }
                val mcastDest = InetAddress.getByName(mcastHost)
                val mcastPacket = DatagramPacket(responseBytes, responseBytes.size, mcastDest, port)
                s.send(mcastPacket)
            } catch (e: Exception) {
                Log.w(TAG, "Multicast reply failed on port $port: ${e.message}")
            }

            Log.d(TAG, "Answered DNS query ($port) for $cleanQuery -> $currentIp to ${packet.socketAddress}")
        }
    }

    internal fun buildDnsResponse(
        queryId: Int,
        targetHost: String,
        ipBytes: ByteArray,
        isLlmnr: Boolean = false
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        // 1. Transaction ID
        bos.write((queryId shr 8) and 0xFF)
        bos.write(queryId and 0xFF)

        // 2. Flags: 0x8400 (Response + Authoritative Answer)
        bos.write(0x84)
        bos.write(0x00)

        // 3. QDCOUNT: 1 (Echo the question so Windows resolver matches immediately)
        bos.write(0x00)
        bos.write(0x01)

        // 4. ANCOUNT: 1
        bos.write(0x00)
        bos.write(0x01)

        // 5. NSCOUNT: 0
        bos.write(0x00)
        bos.write(0x00)

        // 6. ARCOUNT: 0
        bos.write(0x00)
        bos.write(0x00)

        // --- QUESTION SECTION ---
        val cleanHost = targetHost.trimEnd('.')
        for (label in cleanHost.split('.')) {
            if (label.isNotEmpty()) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                bos.write(bytes.size)
                bos.write(bytes)
            }
        }
        bos.write(0x00) // End of name
        // QTYPE: A (1)
        bos.write(0x00)
        bos.write(0x01)
        // QCLASS: IN (1)
        bos.write(0x00)
        bos.write(0x01)

        // --- ANSWER SECTION ---
        // Pointer to QNAME at byte 12: 0xC00C
        bos.write(0xC0)
        bos.write(0x0C)

        // TYPE: A (0x0001)
        bos.write(0x00)
        bos.write(0x01)

        // CLASS: IN (1) with cache-flush bit (0x8001) for mDNS, or plain IN (1) for LLMNR
        if (isLlmnr) {
            bos.write(0x00)
            bos.write(0x01)
        } else {
            bos.write(0x80)
            bos.write(0x01)
        }

        // TTL: 120 seconds for mDNS, 30 seconds for LLMNR
        val ttl = if (isLlmnr) 30 else 120
        bos.write((ttl shr 24) and 0xFF)
        bos.write((ttl shr 16) and 0xFF)
        bos.write((ttl shr 8) and 0xFF)
        bos.write(ttl and 0xFF)

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
