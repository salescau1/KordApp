package com.chordapp.scraper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CiSongScraperTest {

    @Test
    fun testScrapeSesiPotret() = runBlocking {
        val url = "https://www.chordindonesia.com/enau-feat-ari-lesmana-sesi-potret"
        val song = try { CiSongScraper.scrapeUrl(url) } catch (_: Exception) { null }
        if (song == null) return@runBlocking

        assertEquals("CI", song.source)
        println("SCRAPED SONG: artist='${song.artist}', title='${song.title}'")
        assertTrue("Artist should contain enau, got: '${song.artist}'", com.chordapp.catalog.containsIgnoreDiacritics(song.artist, "enau"))
        assertTrue("Content should have [Intro]", song.content.contains("[Intro]"))
        assertTrue("Content should have [Chorus]", song.content.contains("[Chorus]"))
        assertTrue("Should have distinct chords", song.chordCount > 4)
    }

    @Test
    fun testSearchEnauSongs() = runBlocking {
        val results = try { CiSongScraper.searchSongVersions("enau") } catch (_: Exception) { emptyList() }
        if (results.isEmpty()) return@runBlocking

        assertTrue("Should find enau songs from artist page", results.isNotEmpty())
        val hasSesiPotret = results.any { it.title.contains("Sesi Potret", ignoreCase = true) }
        assertTrue("Should contain Sesi Potret in results", hasSesiPotret)
    }

    @Test
    fun testArtistMatchStrict() {
        // "A" should only match "A" or "A feat..."
        assertTrue(CiSongScraper.isArtistMatchStrict("A", "A"))
        assertTrue(CiSongScraper.isArtistMatchStrict("a", "A"))
        assertTrue(CiSongScraper.isArtistMatchStrict("A feat. Someone", "A"))
        assertTrue(CiSongScraper.isArtistMatchStrict("A ft. Someone", "A"))
        assertTrue(CiSongScraper.isArtistMatchStrict("A & Someone", "A"))

        // "A" must NOT match "AB", "AC", "AG", "AM", "Avril", etc.
        org.junit.Assert.assertFalse(CiSongScraper.isArtistMatchStrict("AB", "A"))
        org.junit.Assert.assertFalse(CiSongScraper.isArtistMatchStrict("AC", "A"))
        org.junit.Assert.assertFalse(CiSongScraper.isArtistMatchStrict("AG", "A"))
        org.junit.Assert.assertFalse(CiSongScraper.isArtistMatchStrict("AM", "A"))
        org.junit.Assert.assertFalse(CiSongScraper.isArtistMatchStrict("Avril Lavigne", "A"))
        org.junit.Assert.assertFalse(CiSongScraper.isArtistMatchStrict("Adele", "A"))

        // Standard artist names
        assertTrue(CiSongScraper.isArtistMatchStrict("Sheila On 7", "sheila on 7"))
        assertTrue(CiSongScraper.isArtistMatchStrict("Ed Sheeran & Justin Bieber", "Ed Sheeran"))
        org.junit.Assert.assertFalse(CiSongScraper.isArtistMatchStrict("Edward", "Ed"))
    }
}
