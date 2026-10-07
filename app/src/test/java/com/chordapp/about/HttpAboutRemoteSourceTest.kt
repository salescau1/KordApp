package com.chordapp.about

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class HttpAboutRemoteSourceTest {
    @Test
    fun `valid plain UTF-8 response is returned`() {
        val source = sourceWith(FakeConnection(200, "text/plain; charset=utf-8", "Halo aplikasi".toByteArray()))
        assertEquals("Halo aplikasi", source.fetch())
    }

    @Test
    fun `rejects insecure initial URL`() {
        assertFailsWith<IllegalArgumentException> {
            sourceWith(FakeConnection(200, "text/plain", byteArrayOf()), "http://example.test/about").fetch()
        }
    }

    @Test
    fun `rejects oversized response`() {
        val source = HttpAboutRemoteSource(
            connectionFactory = { FakeConnection(200, "text/plain", ByteArray(9)) },
            maxBytes = 8
        )
        assertFailsWith<IllegalStateException> { source.fetch() }
    }

    @Test
    fun `rejects non plain content`() {
        val source = sourceWith(FakeConnection(200, "text/html", "nope".toByteArray()))
        assertFailsWith<IllegalStateException> { source.fetch() }
    }

    @Test
    fun `rejects HTTPS redirect to HTTP`() {
        val connection = FakeConnection(302, "text/plain", byteArrayOf(), "http://example.test/about")
        val source = sourceWith(connection)
        assertFailsWith<IllegalArgumentException> { source.fetch() }
    }

    @Test
    fun `follows bounded HTTPS redirect and applies timeouts`() {
        val redirect = FakeConnection(302, "text/plain", byteArrayOf(), "/next")
        val success = FakeConnection(200, "text/plain", "fresh".toByteArray())
        val connections = ArrayDeque(listOf(redirect, success))
        val source = HttpAboutRemoteSource(
            initialUrl = "https://example.test/about",
            connectionFactory = { connections.removeFirst() },
            connectTimeoutMs = 1234,
            readTimeoutMs = 5678,
            maxRedirects = 1
        )

        assertEquals("fresh", source.fetch())
        assertEquals(1234, redirect.connectTimeout)
        assertEquals(5678, redirect.readTimeout)
        assertEquals(1234, success.connectTimeout)
        assertEquals(5678, success.readTimeout)
    }

    @Test
    fun `rejects redirects beyond configured bound`() {
        val redirects = ArrayDeque(
            listOf(
                FakeConnection(302, "text/plain", byteArrayOf(), "/one"),
                FakeConnection(302, "text/plain", byteArrayOf(), "/two")
            )
        )
        val source = HttpAboutRemoteSource(
            initialUrl = "https://example.test/about",
            connectionFactory = { redirects.removeFirst() },
            maxRedirects = 1
        )

        assertFailsWith<IllegalStateException> { source.fetch() }
    }

    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected ${T::class.java.simpleName}")
        } catch (error: Throwable) {
            if (error !is T) throw error
        }
    }

    private fun sourceWith(
        connection: HttpURLConnection,
        url: String = "https://example.test/about"
    ) = HttpAboutRemoteSource(url, connectionFactory = { connection })

    private class FakeConnection(
        private val status: Int,
        private val type: String,
        private val body: ByteArray,
        private val location: String? = null
    ) : HttpURLConnection(URL("https://example.test/about")) {
        override fun getResponseCode(): Int = status
        override fun getContentType(): String = type
        override fun getContentLengthLong(): Long = body.size.toLong()
        override fun getInputStream(): InputStream = ByteArrayInputStream(body)
        override fun getHeaderField(name: String?): String? = if (name == "Location") location else null
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun connect() = Unit
    }
}
