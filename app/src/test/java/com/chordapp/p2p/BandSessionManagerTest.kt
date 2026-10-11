package com.chordapp.p2p

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BandSessionManagerTest {

    @Test
    fun testLiveSongPayloadSerialization() {
        val payload = LiveSongPayload(
            title = "Kangen",
            artist = "Dewa 19",
            artistSlug = "dewa-19",
            titleSlug = "kangen",
            source = "UG",
            sourceUrl = "https://tabs.ultimate-guitar.com/tab/dewa-19/kangen",
            versionName = "UG v1",
            effectiveKey = "D",
            transpose = 2,
            capo = 0,
            rawContent = "[Intro]\r\nD  F#m  G  A\r\n\r\nKut'rima suratmu..."
        )

        val json = payload.toJson()
        val parsed = LiveSongPayload.fromJson(json)

        assertEquals(payload.title, parsed.title)
        assertEquals(payload.artist, parsed.artist)
        assertEquals(payload.artistSlug, parsed.artistSlug)
        assertEquals(payload.titleSlug, parsed.titleSlug)
        assertEquals(payload.source, parsed.source)
        assertEquals(payload.sourceUrl, parsed.sourceUrl)
        assertEquals(payload.versionName, parsed.versionName)
        assertEquals(payload.effectiveKey, parsed.effectiveKey)
        assertEquals(payload.transpose, parsed.transpose)
        assertEquals(payload.capo, parsed.capo)
        assertEquals(payload.rawContent, parsed.rawContent)
    }

    @Test
    fun testDiscoveredServerData() {
        val server = DiscoveredServer(
            name = "Gitaris 1",
            host = "192.168.43.1",
            port = 8989,
            requiresPin = true
        )

        assertEquals("Gitaris 1", server.name)
        assertEquals("192.168.43.1", server.host)
        assertEquals(8989, server.port)
        assertTrue(server.requiresPin)
    }
}
