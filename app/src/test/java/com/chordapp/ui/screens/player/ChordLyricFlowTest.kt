package com.chordapp.ui.screens.player

import com.chordapp.player.ChordToken
import com.chordapp.player.SheetLine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies chord+lyric segmentation: each chord attaches to the word it sits
 * over so the pair wraps together, chords transpose, gaps and instrumental
 * (chord-only) lines are handled. No Android/Robolectric needed — pure Kotlin.
 */
class ChordLyricFlowTest {

    private fun chords(vararg pairs: Pair<Int, String>) =
        SheetLine.Chords(
            tokens = pairs.map { ChordToken(it.second, it.first) },
            originalText = "",
            bracketed = false
        )

    @Test
    fun `each chord attaches to the word it sits over`() {
        // "C" over "Aku ", "G" over "cinta".
        val line = chords(0 to "C", 4 to "G")
        val segs = buildChordLyricSegments(line, "Aku cinta", 0)
        assertEquals(listOf("C" to "Aku ", "G" to "cinta"), segs.map { it.chord to it.text })
    }

    @Test
    fun `chord in the gap before a word attaches to that following word`() {
        // Column 3 is the space; the chord belongs to " cinta", not "Aku".
        val line = chords(3 to "G")
        val segs = buildChordLyricSegments(line, "Aku cinta", 0)
        assertEquals(null, segs[0].chord)
        assertEquals("Aku", segs[0].text)
        assertEquals("G", segs[1].chord)
        assertEquals(" cinta", segs[1].text)
    }

    @Test
    fun `transpose is applied to every chord`() {
        val line = chords(0 to "C", 4 to "G")
        val segs = buildChordLyricSegments(line, "Aku cinta", 2)
        assertEquals(listOf("D", "A"), segs.mapNotNull { it.chord })
    }

    @Test
    fun `two chords over one word stay separate and tappable`() {
        // "C" at 0 over "A", "G" at 1 over "ku" -> together "Aku".
        val line = chords(0 to "C", 1 to "G")
        val segs = buildChordLyricSegments(line, "Aku", 0)
        assertEquals(2, segs.size)
        assertEquals("C" to "A", segs[0].chord to segs[0].text)
        assertEquals("G" to "ku", segs[1].chord to segs[1].text)
    }

    @Test
    fun `mid-word chords like Scaramouche align without extra spaces`() {
        val line = chords(0 to "A", 5 to "D", 13 to "A", 18 to "D")
        val segs = buildChordLyricSegments(line, "Scaramouche, Scaramouche,", 0)
        assertEquals(
            listOf("A" to "Scara", "D" to "mouche, ", "A" to "Scara", "D" to "mouche,"),
            segs.map { it.chord to it.text }
        )
    }

    @Test
    fun `chord past the lyric end becomes a trailing chord-only segment`() {
        val line = chords(0 to "C", 20 to "G")
        val segs = buildChordLyricSegments(line, "Aku", 0)
        assertEquals("C", segs[0].chord)
        assertEquals("Aku", segs[0].text)
        assertEquals("G", segs[1].chord)
        assertEquals("", segs[1].text)
    }

    @Test
    fun `instrumental chord-only line yields one segment per chord`() {
        val line = chords(0 to "C", 5 to "G", 10 to "Am")
        val segs = buildChordLyricSegments(line, "", 0)
        assertEquals(listOf("C", "G", "Am"), segs.mapNotNull { it.chord })
        assertEquals(3, segs.size)
        assertEquals(listOf("", "", ""), segs.map { it.text })
    }
}
