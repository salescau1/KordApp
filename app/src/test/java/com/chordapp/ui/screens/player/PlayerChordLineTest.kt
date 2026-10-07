package com.chordapp.ui.screens.player

import com.chordapp.player.ChordParser
import com.chordapp.player.SheetLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerChordLineTest {

    @Test
    fun plainChordRowPreservesStoredSpacingWithoutAddingBrackets() {
        val line = ChordParser.parse("C   A   D   G").lines.single() as SheetLine.Chords

        val (rendered, spans) = buildChordLine(line, delta = 0)

        assertFalse(line.bracketed)
        assertEquals("C   A   D   G", rendered)
        assertFalse(rendered.contains('['))
        assertEquals(listOf("C", "A", "D", "G"), spans.map { it.symbol })
    }

    @Test
    fun bracketedChordRowKeepsStoredBracketsAndSpacing() {
        val line = ChordParser.parse("[C]   [A]   [D]   [G]").lines.single() as SheetLine.Chords

        val (rendered, spans) = buildChordLine(line, delta = 0)

        assertTrue(line.bracketed)
        assertEquals("[C]   [A]   [D]   [G]", rendered)
        assertEquals(listOf("C", "A", "D", "G"), spans.map { it.symbol })
    }

    @Test
    fun transpositionNeverJoinsAdjacentPlainChordSymbols() {
        val line = ChordParser.parse("C A D G").lines.single() as SheetLine.Chords

        val (rendered, spans) = buildChordLine(line, delta = 1)

        assertEquals("C# A# D# G#", rendered)
        assertFalse(rendered.contains("C#A#"))
        assertEquals(listOf("C#", "A#", "D#", "G#"), spans.map { it.symbol })
    }

    @Test
    fun autoScrollDeltaUsesElapsedFrameTimeAndLatestSpeed() {
        assertEquals(24f, autoScrollDeltaPx(500_000_000L, 2f, 24f), 0.001f)
        assertEquals(48f, autoScrollDeltaPx(500_000_000L, 4f, 24f), 0.001f)
        assertEquals(0f, autoScrollDeltaPx(500_000_000L, 0f, 24f), 0f)
    }

    @Test
    fun nextPageStopsAtLastPageWithoutWrapping() {
        assertEquals(1, nextPage(currentPage = 0, pageCount = 3))
        assertEquals(2, nextPage(currentPage = 1, pageCount = 3))
        assertEquals(null, nextPage(currentPage = 2, pageCount = 3))
        assertEquals(null, nextPage(currentPage = 0, pageCount = 0))
    }
}
