package com.chordapp.promo

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

fun interface PromoRemoteSource {
    fun fetch(): String
}

/**
 * Menarik `promo.json` (teks JSON) dari repo GitHub publik lewat
 * raw.githubusercontent.com — disajikan sebagai `text/plain; charset=utf-8`.
 * Guard sama seperti About: HTTPS wajib, batas ukuran, UTF-8 ketat. Kegagalan
 * apa pun dilempar sebagai exception (dipetakan ke "tidak ada popup" di repo).
 */
class HttpPromoRemoteSource(
    private val initialUrl: String = PROMO_URL,
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    },
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000,
    private val maxBytes: Int = MAX_RESPONSE_BYTES,
    private val maxRedirects: Int = 3
) : PromoRemoteSource {
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
                check(text.isNotEmpty()) { "Empty promo content" }
                return text
            } finally {
                connection.disconnect()
            }
        }
        error("Unable to load promo content")
    }

    private fun checkedHttpsUrl(value: String): URL = URL(value).also {
        require(it.protocol.equals("https", ignoreCase = true)) { "HTTPS required" }
    }

    companion object {
        const val PROMO_URL =
            "https://raw.githubusercontent.com/salescau1/KordApp-Content/main/promo.json"
        const val MAX_RESPONSE_BYTES = 256 * 1024
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
