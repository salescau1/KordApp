package com.chordapp.charts

import org.json.JSONObject

/** Parses the unauthenticated Spotify embed payload for Top 50 - Indonesia. */
object SpotifyPlaylistParser {
    private const val NEXT_DATA_OPEN =
        "<script id=\"__NEXT_DATA__\" type=\"application/json\">"
    private const val NEXT_DATA_CLOSE = "</script>"
    private const val TOP_N = 50

    fun parse(html: String): List<ChartEntry> {
        val open = html.indexOf(NEXT_DATA_OPEN)
        if (open < 0) return emptyList()
        val jsonStart = open + NEXT_DATA_OPEN.length
        val jsonEnd = html.indexOf(NEXT_DATA_CLOSE, jsonStart)
        if (jsonEnd < 0) return emptyList()

        return runCatching {
            val tracks = JSONObject(html.substring(jsonStart, jsonEnd))
                .getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("state")
                .getJSONObject("data")
                .getJSONObject("entity")
                .getJSONArray("trackList")
            buildList {
                for (index in 0 until minOf(tracks.length(), TOP_N)) {
                    val track = tracks.optJSONObject(index) ?: continue
                    val title = track.optString("title").trim()
                    val artist = track.optString("subtitle").trim()
                    if (title.isEmpty() || artist.isEmpty()) continue
                    add(ChartEntry(rank = size + 1, title = title, artist = artist))
                }
            }
        }.getOrDefault(emptyList())
    }
}
