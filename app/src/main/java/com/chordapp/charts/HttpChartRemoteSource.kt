package com.chordapp.charts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * GET-only, no-API-key remote source for the accepted public playlists.
 *
 * Spotify uses its unauthenticated embed document because the Web API requires
 * credentials. YouTube uses the kworb Indonesia YouTube insights page, whose row
 * shape matches the kworb Spotify chart (parsed by [KworbChartParser]). A parser
 * returning no rows is treated as failure so an old cache is never replaced by
 * emptiness.
 */
class HttpChartRemoteSource(
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val connectTimeoutMs: Int = 12_000,
    private val readTimeoutMs: Int = 15_000
) : ChartRemoteSource {

    override suspend fun fetchTop20(platform: ChartPlatform): List<ChartEntry> =
        withContext(Dispatchers.IO) {
            val url = when (platform) {
                ChartPlatform.SPOTIFY ->
                    "https://open.spotify.com/embed/playlist/37i9dQZEVXbObFQZ3JLcXt"
                ChartPlatform.YOUTUBE ->
                    "https://kworb.net/youtube/insights/id.html"
            }
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = connectTimeoutMs
                connection.readTimeout = readTimeoutMs
                connection.setRequestProperty("User-Agent", userAgent)
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")

                val code = connection.responseCode
                if (code !in 200..299) throw IOException("Chart source HTTP $code")

                val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val entries = when (platform) {
                    ChartPlatform.SPOTIFY -> SpotifyPlaylistParser.parse(html)
                    ChartPlatform.YOUTUBE -> KworbChartParser.parse(html)
                }
                if (entries.isEmpty()) throw IOException("Chart source returned no rows")
                entries
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
    }
}
