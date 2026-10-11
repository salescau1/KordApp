package com.chordapp.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordParserTest {

    @Test
    fun bracketColumnsTrackMonospacePosition() {
        // Overlay chords sit above the lyric. Columns match the opening bracket position
        // in the monospace string:
        //   [B]   [Bb]   [A]   [Bb]
        //   0     6      13    19
        val bracketed = ChordParser.parse("[B]   [Bb]   [A]   [Bb]")
            .lines.single() as SheetLine.Chords
        val columns = bracketed.tokens.map { it.column }
        val symbols = bracketed.tokens.map { it.symbol }
        assertEquals(listOf("B", "Bb", "A", "Bb"), symbols)
        assertEquals(listOf(0, 6, 13, 19), columns)
        // columns are strictly ascending
        for (i in 1 until columns.size) assertTrue(columns[i] > columns[i - 1])
    }

    @Test
    fun plainChordColumnsTrackTokenStart() {
        // Same symbol sequence with no brackets: columns are the exact token start
        // indices in the monospace string.
        //   B   Bb   A   Bb
        //   0123 45  6789 01234
        val plain = ChordParser.parse("B   Bb   A   Bb")
            .lines.single() as SheetLine.Chords
        assertEquals(listOf("B", "Bb", "A", "Bb"), plain.tokens.map { it.symbol })
        assertEquals(listOf(0, 4, 9, 13), plain.tokens.map { it.column })
    }

    @Test
    fun bareSlashBassTokensAreRecognisedAsChords() {
        // GT source lines can merge a chord overlay with its lyric into one
        // physical line: `/E /Eb Didn't mean to make you cry`. The leading bare
        // slash-bass tokens (`/E`, `/Eb`) must be recognised as chords so the
        // parser splits them off instead of rendering them as lyric text.
        assertTrue(ChordParser.isChordSymbol("/E"))
        assertTrue(ChordParser.isChordSymbol("/Eb"))
        // a bare slash with a non-note bass side is not a chord
        assertTrue(!ChordParser.isChordSymbol("/"))
    }

    @Test
    fun mergedChordLyricLineIsSplitIntoOverlayAndLyric() {
        // Mirrors the GT bohemian-rhapsody line where `/E /Eb` sits on the same
        // physical line as its lyric.
        val lines = ChordParser.parse("/E /Eb Didn't mean to make you cry").lines
        val chords = lines.filterIsInstance<SheetLine.Chords>()
        val lyrics = lines.filterIsInstance<SheetLine.Lyric>()

        assertEquals(1, chords.size)
        assertEquals(listOf("/E", "/Eb"), chords[0].tokens.map { it.symbol })
        assertEquals(1, lyrics.size)
        // Lyric is indented to its original column so the overlay stays aligned.
        assertEquals("       Didn't mean to make you cry", lyrics[0].text)
    }

    @Test
    fun chordAndLyricRowsAreCountedSeparately() {
        val content = """
            [Intro]
            [B]   [Bb]   [A]   [Bb]
            Because I'm easy come, easy go
            [Bb]   [Eb]
            Look up to the skies and see
        """.trimIndent()

        val lines = ChordParser.parse(content).lines
        val chordRows = lines.filterIsInstance<SheetLine.Chords>()
        val lyricRows = lines.filterIsInstance<SheetLine.Lyric>()

        assertEquals(2, chordRows.size)
        assertEquals(2, lyricRows.size)

        // Each chord row's tokens are all within the column span of the lyric below
        // it — the core invariant that makes the overlay render correctly.
        assertEquals(
            listOf(0, 6, 13, 19),
            chordRows[0].tokens.map { it.column }
        )
        assertEquals(
            listOf(0, 7),
            chordRows[1].tokens.map { it.column }
        )
        chordRows.forEachIndexed { i, row ->
            val lyricLen = lyricRows[i].text.length
            row.tokens.forEach { token ->
                assertTrue(
                    "chord token at col ${token.column} must be within lyric length $lyricLen",
                    token.column < lyricLen
                )
            }
        }
    }

    @Test
    fun barLinesWithBracketsAreParsedAsBarLinesAndNotSplitIntoLyrics() {
        val content = """
            | [C] [G] | [Am]  | [Dm]  | [G]   |
            | [C] [G] | [Am]  | [Dm]  | [Bb]  [Bb] [A] [G#] [G] | [F#]   |
        """.trimIndent()

        val parsed = ChordParser.parse(content)
        val lyrics = parsed.lines.filterIsInstance<SheetLine.Lyric>()
        val chords = parsed.lines.filterIsInstance<SheetLine.Chords>()

        // Must NOT generate dummy lyric rows with '|   |   |'
        assertEquals(0, lyrics.size)
        assertEquals(2, chords.size)
        assertTrue(chords[0].isBarLine)
        assertTrue(chords[1].isBarLine)

        // Verify segments preserve the clean preview string without brackets
        val segs0 = ChordParser.buildBarSegments(chords[0])
        val rendered0 = segs0.joinToString("") { it.text }
        assertEquals("| C G | Am  | Dm  | G   |", rendered0)

        val segs1 = ChordParser.buildBarSegments(chords[1])
        val rendered1 = segs1.joinToString("") { it.text }
        assertEquals("| C G | Am  | Dm  | Bb  Bb A G# G | F#   |", rendered1)
    }

    @Test
    fun barLineSegmentsTransposeChordsCleanly() {
        val line = ChordParser.parse("| [C] [G] | [Am]  | [Dm]  | [G]   |")
            .lines.single() as SheetLine.Chords

        val segs = ChordParser.buildBarSegments(line)
        val transposed = segs.joinToString("") {
            if (it.isChord) Transposer.transpose(it.text, delta = 2) else it.text
        }
        assertEquals("| D A | Bm  | Em  | A   |", transposed)
    }

    @Test
    fun unbracketedBarLineIsDetectedAsBarLine() {
        val line = ChordParser.parse("| C G | Am  | Dm  | G   |")
            .lines.single() as SheetLine.Chords

        assertTrue(line.isBarLine)
        val segs = ChordParser.buildBarSegments(line)
        val rendered = segs.joinToString("") { it.text }
        assertEquals("| C G | Am  | Dm  | G   |", rendered)
    }

    @Test
    fun barLineWithNCAnnotationIsRecognisedAsBarLine() {
        val line = ChordParser.parse("|([C])  | [C]   | [C]   | [D]   | N.C.|")
            .lines.single() as SheetLine.Chords

        assertTrue(line.isBarLine)
        val segs = ChordParser.buildBarSegments(line)
        val rendered = segs.joinToString("") { it.text }
        assertEquals("|(C)  | C   | C   | D   | N.C.|", rendered)
    }

    @Test
    fun parenthesizedChordsDoNotGenerateDummyLyric() {
        val parsed = ChordParser.parse("([C])  [Em]   [F]   [G]")
        val lyrics = parsed.lines.filterIsInstance<SheetLine.Lyric>()
        val chords = parsed.lines.filterIsInstance<SheetLine.Chords>()

        assertEquals(0, lyrics.size)
        assertEquals(1, chords.size)
        assertEquals(listOf("C", "Em", "F", "G"), chords[0].tokens.map { it.symbol })
    }

    @Test
    fun htmlEntitiesInLyricsAreUnescaped() {
        val parsed = ChordParser.parse("slamin&#039; doors and you&#039;re lonely")
        val lyrics = parsed.lines.filterIsInstance<SheetLine.Lyric>()
        assertEquals(1, lyrics.size)
        assertEquals("slamin' doors and you're lonely", lyrics[0].text)
    }

    @Test
    fun chordIndonesiaSesiPotretParsesCorrectly() {
        val sample = """
            [Intro]
            [C] [F] [C] [F] -[Fm]

            [F]        [G]           [C]
             tahun lalu berjuta alasanku..
            [F]         [G]
             maaf tak bisa pulang
                [C]
            penghasilanku pas-pasan
            [F]      [G]          [C]
             kali ini sudah lumayan..

            [Interlude]
            [C] [D] [F] [Em]
            [G] [D] [Dm] -[G] [C]..
        """.trimIndent()

        val parsed = ChordParser.parse(sample)
        val sections = parsed.lines.filterIsInstance<SheetLine.Section>()
        val chords = parsed.lines.filterIsInstance<SheetLine.Chords>()
        val lyrics = parsed.lines.filterIsInstance<SheetLine.Lyric>()

        assertEquals(2, sections.size)
        assertEquals("[Intro]", sections[0].text)
        assertEquals("[Interlude]", sections[1].text)
        assertTrue(parsed.distinctChords.contains("C"))
        assertTrue(parsed.distinctChords.contains("Fm"))
        assertTrue(parsed.distinctChords.contains("G"))
        assertTrue(chords.isNotEmpty())
        assertTrue(lyrics.isNotEmpty())
    }
}


