package com.chordapp.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UgSongScraperTest {

    @Test
    fun `parses UG html with js-store content and normalizes chords`() {
        val wikiTab = org.json.JSONObject()
            .put("content", "[tab][Verse 1]\r\n[ch]C[/ch]               [ch]G[/ch]\r\nBelum ada satu bulan\r\n[ch]Am[/ch]              [ch]F[/ch]\r\nKudengar kabar yang baru[/tab]")
        val tabView = org.json.JSONObject().put("wiki_tab", wikiTab)
        val tab = org.json.JSONObject()
            .put("song_name", "Satu Bulan")
            .put("artist_name", "Bernadya")
            .put("version", 1)
            .put("rating", 4.92)
            .put("votes", 120)
            .put("tonality_name", "C")
            .put("difficulty", "novice")

        val data = org.json.JSONObject()
            .put("tab", tab)
            .put("tab_view", tabView)

        val root = org.json.JSONObject()
            .put("store", org.json.JSONObject().put("page", org.json.JSONObject().put("data", data)))

        val escaped = root.toString()
            .replace("&", "&amp;")
            .replace("\"", "&quot;")

        val html = """<div class="js-store" data-content="$escaped"></div>"""

        val parsed = UgSongScraper.parseUgHtml(
            htmlText = html,
            url = "https://tabs.ultimate-guitar.com/tab/bernadya/satu-bulan-chords-12345",
            artistHint = "",
            titleHint = ""
        )

        assertNotNull(parsed)
        assertEquals("Satu Bulan", parsed!!.title)
        assertEquals("Bernadya", parsed.artist)
        assertEquals("satu-bulan", parsed.titleSlug)
        assertEquals("bernadya", parsed.artistSlug)
        assertEquals("Ver 1", parsed.versionName)
        assertEquals(4.92f, parsed.rating)
        assertEquals(120, parsed.votes)
        assertEquals("C", parsed.keyBase)
        assertEquals("UG", parsed.source)

        // Verify chords converted from [ch]C[/ch] to [C] and [tab] stripped
        assertTrue(parsed.content.contains("[C]"))
        assertTrue(parsed.content.contains("[G]"))
        assertTrue(parsed.content.contains("[Am]"))
        assertTrue(parsed.content.contains("[F]"))
        assertTrue(!parsed.content.contains("[ch]"))
        assertTrue(!parsed.content.contains("[tab]"))

        // Verify unique chords
        assertEquals("C,G,Am,F", parsed.chords)
        assertEquals(4, parsed.chordCount)
    }

    @Test
    fun `findBestChordTabUrl prioritizes title and artist match over higher-voted unrelated songs`() {
        val resultsArray = org.json.JSONArray()

        // Unrelated popular song with massive votes (e.g. Alec Benjamin)
        val popularWrong = org.json.JSONObject()
            .put("song_name", "If I Killed Someone For You")
            .put("artist_name", "Alec Benjamin")
            .put("type", "Chords")
            .put("tab_url", "https://tabs.ultimate-guitar.com/tab/alec-benjamin/if-i-killed-someone-for-you-chords-2518968")
            .put("rating", 4.90)
            .put("votes", 1245)
        resultsArray.put(popularWrong)

        // Exact match with small votes (Alex Crichton - What If I Call)
        val targetSong = org.json.JSONObject()
            .put("song_name", "What If I Call")
            .put("artist_name", "Alex Crichton")
            .put("type", "Chords")
            .put("tab_url", "https://tabs.ultimate-guitar.com/tab/alex-crichton/what-if-i-call-chords-6561233")
            .put("rating", 4.71)
            .put("votes", 6)
        resultsArray.put(targetSong)

        // Same artist but different song with more votes
        val wrongSongSameArtist = org.json.JSONObject()
            .put("song_name", "Merry Christmas I Miss You")
            .put("artist_name", "Alex Crichton")
            .put("type", "Chords")
            .put("tab_url", "https://tabs.ultimate-guitar.com/tab/alex-crichton/merry-christmas-i-miss-you-chords-5559966")
            .put("rating", 4.91)
            .put("votes", 28)
        resultsArray.put(wrongSongSameArtist)

        val page = org.json.JSONObject().put("data", org.json.JSONObject().put("results", resultsArray))
        val store = org.json.JSONObject().put("page", page)
        val root = org.json.JSONObject().put("store", store)
        val escaped = root.toString().replace("&", "&amp;").replace("\"", "&quot;")
        val html = """<div class="js-store" data-content="$escaped"></div>"""

        val selectedUrl = UgSongScraper.findBestChordTabUrl(
            html = html,
            targetArtist = "Alex Crichton",
            targetTitle = "What If I Call"
        )

        assertEquals("https://tabs.ultimate-guitar.com/tab/alex-crichton/what-if-i-call-chords-6561233", selectedUrl)
    }

    @Test
    fun `normalizeForMatching strips punctuation, apostrophes, and diacritics`() {
        assertEquals("merry christmas please dont call", UgSongScraper.normalizeForMatching("Merry Christmas, Please Don't Call"))
        assertEquals("enau", UgSongScraper.normalizeForMatching("eńau"))
        assertEquals("what if i call", UgSongScraper.normalizeForMatching("What If I Call"))
    }

    @Test
    fun `findBestChordTabUrlForQuery picks best chord candidate for free query`() {
        val resultsArray = org.json.JSONArray()

        val song1 = org.json.JSONObject()
            .put("song_name", "Are You Gonna Be My Girl")
            .put("artist_name", "Jet")
            .put("type", "Chords")
            .put("tab_url", "https://tabs.ultimate-guitar.com/tab/jet/are-you-gonna-be-my-girl-chords-1058317")
            .put("rating", 4.80)
            .put("votes", 903)
        resultsArray.put(song1)

        val song2 = org.json.JSONObject()
            .put("song_name", "Jet")
            .put("artist_name", "Paul McCartney & Wings")
            .put("type", "Chords")
            .put("tab_url", "https://tabs.ultimate-guitar.com/tab/paul-mccartney/jet-chords-12345")
            .put("rating", 4.90)
            .put("votes", 210)
        resultsArray.put(song2)

        val page = org.json.JSONObject().put("data", org.json.JSONObject().put("results", resultsArray))
        val store = org.json.JSONObject().put("page", page)
        val root = org.json.JSONObject().put("store", store)
        val escaped = root.toString().replace("&", "&amp;").replace("\"", "&quot;")
        val html = """<div class="js-store" data-content="$escaped"></div>"""

        // When query is "Jet", exact title "Jet" is tier 4, beating partial matches
        val selectedJet = UgSongScraper.findBestChordTabUrlForQuery(html, "Jet")
        assertEquals("https://tabs.ultimate-guitar.com/tab/paul-mccartney/jet-chords-12345", selectedJet)

        // When query is "Are You Gonna Be My Girl", it selects Jet's song
        val selectedGirl = UgSongScraper.findBestChordTabUrlForQuery(html, "Are You Gonna Be My Girl")
        assertEquals("https://tabs.ultimate-guitar.com/tab/jet/are-you-gonna-be-my-girl-chords-1058317", selectedGirl)
    }
}
