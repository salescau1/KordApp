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
        val song = CiSongScraper.scrapeUrl(url)

        assertNotNull("Song should not be null", song)
        assertEquals("CI", song?.source)
        assertTrue("Title should contain Sesi Potret", song?.title?.contains("Sesi Potret", ignoreCase = true) == true)
        assertTrue("Artist should contain enau", song?.artist?.contains("enau", ignoreCase = true) == true)
        assertTrue("Content should have [Intro]", song?.content?.contains("[Intro]") == true)
        assertTrue("Content should have [Chorus]", song?.content?.contains("[Chorus]") == true)
        assertTrue("Should have distinct chords", (song?.chordCount ?: 0) > 4)
    }

    @Test
    fun testSearchEnauSongs() = runBlocking {
        val results = CiSongScraper.searchSongVersions("enau")
        assertTrue("Should find enau songs from artist page", results.isNotEmpty())
        val hasSesiPotret = results.any { it.title.contains("Sesi Potret", ignoreCase = true) }
        assertTrue("Should contain Sesi Potret in results", hasSesiPotret)
    }
}
