package com.chordapp.p2p

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@RunWith(RobolectricTestRunner::class)
class MdnsResponderTest {

    @Test
    fun testBuildDnsResponse() {
        val responder = MdnsResponder("kordapp.local") { "192.168.1.21" }
        val response = responder.buildDnsResponse(
            queryId = 0x1234,
            targetHost = "kordapp.local",
            ipBytes = byteArrayOf(192.toByte(), 168.toByte(), 1.toByte(), 21.toByte()),
            isLlmnr = false
        )

        // 1. Transaction ID
        assertEquals(0x12.toByte(), response[0])
        assertEquals(0x34.toByte(), response[1])

        // 2. Flags: 0x8400
        assertEquals(0x84.toByte(), response[2])
        assertEquals(0x00.toByte(), response[3])

        // 3. QDCOUNT: 1, ANCOUNT: 1
        assertEquals(0x00.toByte(), response[4])
        assertEquals(0x01.toByte(), response[5])
        assertEquals(0x00.toByte(), response[6])
        assertEquals(0x01.toByte(), response[7])

        // Verify IP bytes at the end
        val len = response.size
        assertEquals(192.toByte(), response[len - 4])
        assertEquals(168.toByte(), response[len - 3])
        assertEquals(1.toByte(), response[len - 2])
        assertEquals(21.toByte(), response[len - 1])
    }

    @Test
    fun testHandleQueryPacket() {
        var answered = false
        val responder = MdnsResponder("kordapp.local") { "192.168.1.21" }

        // Construct mock query packet for "kordapp.local"
        val bos = ByteArrayOutputStream()
        bos.write(byteArrayOf(0x00, 0x00)) // ID
        bos.write(byteArrayOf(0x00, 0x00)) // Flags: standard query
        bos.write(byteArrayOf(0x00, 0x01)) // QDCOUNT: 1
        bos.write(byteArrayOf(0x00, 0x00)) // ANCOUNT: 0
        bos.write(byteArrayOf(0x00, 0x00)) // NSCOUNT: 0
        bos.write(byteArrayOf(0x00, 0x00)) // ARCOUNT: 0

        // QNAME: 7kordapp 5local 0
        bos.write(7)
        bos.write("kordapp".toByteArray(Charsets.US_ASCII))
        bos.write(5)
        bos.write("local".toByteArray(Charsets.US_ASCII))
        bos.write(0)

        // QTYPE: 1 (A), QCLASS: 0x8001 (IN, unicast response requested)
        bos.write(byteArrayOf(0x00, 0x01))
        bos.write(byteArrayOf(0x80.toByte(), 0x01.toByte()))

        val queryBytes = bos.toByteArray()
        val queryPacket = DatagramPacket(queryBytes, queryBytes.size, InetAddress.getByName("127.0.0.1"), 54321)

        val mockSocket = object : DatagramSocket() {
            override fun send(p: DatagramPacket?) {
                if (p != null && p.length > 0) {
                    answered = true
                }
            }
        }

        responder.handlePacket(queryPacket, mockSocket, MdnsResponder.MDNS_PORT, false)
        mockSocket.close()

        assertTrue("Responder should answer mDNS query for kordapp.local", answered)
    }
}
