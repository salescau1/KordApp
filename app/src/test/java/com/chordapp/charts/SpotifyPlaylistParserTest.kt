package com.chordapp.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpotifyPlaylistParserTest {
    @Test
    fun `parses ordered tracks from Spotify embed payload and caps at fifty`() {
        val tracks = (1..60).joinToString(",") { index ->
            """{"title":"Song $index","subtitle":"Artist $index","uri":"spotify:track:$index"}"""
        }
        val html = """
            <html><script id="__NEXT_DATA__" type="application/json">
            {"props":{"pageProps":{"state":{"data":{"entity":{"trackList":[$tracks]}}}}}}
            </script></html>
        """.trimIndent()

        val parsed = SpotifyPlaylistParser.parse(html)

        assertEquals(50, parsed.size)
        assertEquals(ChartEntry(1, "Song 1", "Artist 1"), parsed.first())
        assertEquals(ChartEntry(50, "Song 50", "Artist 50"), parsed.last())
    }

    @Test
    fun `missing or malformed payload returns no invented rows`() {
        assertTrue(SpotifyPlaylistParser.parse("<html></html>").isEmpty())
        assertTrue(
            SpotifyPlaylistParser.parse(
                """<script id="__NEXT_DATA__" type="application/json">{}</script>"""
            ).isEmpty()
        )
    }
}
