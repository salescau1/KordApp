package com.chordapp.about

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

fun interface AboutRemoteSource {
    fun fetch(): String
}

class HttpAboutRemoteSource(
    private val initialUrl: String = ABOUT_URL,
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    },
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000,
    private val maxBytes: Int = MAX_RESPONSE_BYTES,
    private val maxRedirects: Int = 3
) : AboutRemoteSource {
    override fun fetch(): String {
        var url = checkedHttpsUrl(initialUrl)
        repeat(maxRedirects + 1) { redirectCount ->
            val connection = connectionFactory(url).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Accept", "text/plain")
            }
            try {
                val status = connection.responseCode
                if (status in REDIRECT_CODES) {
                    check(redirectCount < maxRedirects) { "Too many redirects" }
                    val location = connection.getHeaderField("Location")
                        ?: error("Redirect without location")
                    url = checkedHttpsUrl(URL(url, location).toString())
                    return@repeat
                }
                check(status in 200..299) { "Unexpected HTTP status" }
                val contentType = connection.contentType.orEmpty().lowercase()
                check(contentType.startsWith("text/plain")) { "Unexpected content type" }
                val length = connection.contentLengthLong
                check(length < 0 || length <= maxBytes) { "Response too large" }
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= maxBytes) { "Response too large" }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
                    .trim()
                check(text.isNotEmpty()) { "Empty About content" }
                return text
            } finally {
                connection.disconnect()
            }
        }
        error("Unable to load About content")
    }

    private fun checkedHttpsUrl(value: String): URL = URL(value).also {
        require(it.protocol.equals("https", ignoreCase = true)) { "HTTPS required" }
    }

    companion object {
        // Sumber konten About: file .txt polos di repo GitHub PUBLIK
        // (salescau1/KordApp-Content), disajikan via raw.githubusercontent.com
        // sebagai `text/plain; charset=utf-8` sehingga lolos guard HTTPS +
        // content-type + UTF-8 di fetch(). Repo ini juga menyimpan promo.json
        // untuk popup promosi (satu repo konten remote). Edit about.txt di repo
        // itu lalu commit; app menariknya saat refresh (cache-first). Repo harus
        // tetap publik (raw dari repo privat butuh token dan akan gagal guard).
        const val ABOUT_URL =
            "https://raw.githubusercontent.com/salescau1/KordApp-Content/main/about.txt"
        const val MAX_RESPONSE_BYTES = 256 * 1024
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
