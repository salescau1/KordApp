package com.chordapp.p2p

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class WebStageServerTest {

    @Test
    fun testWebStageServerLifecycleAndEndpoints() {
        val testPort = 18990
        val server = WebStageServer(testPort)
        try {
            server.start()

            // Test GET / returns HTML player page
            val htmlUrl = URL("http://127.0.0.1:$testPort/")
            val htmlConn = htmlUrl.openConnection() as HttpURLConnection
            htmlConn.connectTimeout = 3000
            htmlConn.readTimeout = 3000
            assertEquals(200, htmlConn.responseCode)
            assertTrue(htmlConn.contentType.contains("text/html"))
            val htmlBody = htmlConn.inputStream.bufferedReader().use { it.readText() }
            assertTrue(htmlBody.contains("Web Stage Mirror"))
            assertTrue(htmlBody.contains("Standby Panggung"))
            assertTrue(htmlBody.contains("id=\"btnScroll\""))
            assertTrue(htmlBody.contains("id=\"dockSpeed\""))
            assertTrue(htmlBody.contains("id=\"btnPageFlip\""))
            assertTrue(htmlBody.contains("id=\"speedRail\""))
            assertTrue(htmlBody.contains("id=\"speedRailTrack\""))
            assertTrue(htmlBody.contains("id=\"pageFlipRail\""))
            assertTrue(htmlBody.contains("cycleSpeedPreset"))
            assertTrue(htmlBody.contains("togglePageFlip"))
            assertTrue(htmlBody.contains("updatePageFlipRailUI"))

            // Test GET /api/current in standby
            val apiUrl = URL("http://127.0.0.1:$testPort/api/current")
            val apiConn = apiUrl.openConnection() as HttpURLConnection
            apiConn.connectTimeout = 3000
            apiConn.readTimeout = 3000
            assertEquals(200, apiConn.responseCode)
            val apiBody = apiConn.inputStream.bufferedReader().use { it.readText() }
            assertTrue(apiBody.contains("\"state\":\"standby\""))

            // Broadcast song and test /api/current updates
            val payload = LiveSongPayload(
                title = "Separuh Nafas",
                artist = "Dewa 19",
                artistSlug = "dewa-19",
                titleSlug = "separuh-nafas",
                source = "ChordTela",
                sourceUrl = "https://chordtela.com/dewa-19-separuh-nafas",
                versionName = "Original",
                effectiveKey = "Dm",
                transpose = 0,
                capo = 0,
                rawContent = "Dm  Bb  F  C\r\nSeparuh nafasku terbang..."
            )
            server.broadcastSong(payload)

            val apiSongConn = URL("http://127.0.0.1:$testPort/api/current").openConnection() as HttpURLConnection
            apiSongConn.connectTimeout = 3000
            apiSongConn.readTimeout = 3000
            assertEquals(200, apiSongConn.responseCode)
            val songBody = apiSongConn.inputStream.bufferedReader().use { it.readText() }
            assertTrue(songBody.contains("\"state\":\"song\""))
            assertTrue(songBody.contains("Separuh Nafas"))
            assertTrue(songBody.contains("Dewa 19"))

            // Test broadcast standby
            server.broadcastStandby()
            val apiStandbyConn = URL("http://127.0.0.1:$testPort/api/current").openConnection() as HttpURLConnection
            apiStandbyConn.connectTimeout = 3000
            apiStandbyConn.readTimeout = 3000
            val standbyBody = apiStandbyConn.inputStream.bufferedReader().use { it.readText() }
            assertTrue(standbyBody.contains("\"state\":\"standby\""))

        } finally {
            server.stop()
        }
    }
}
