package com.chordapp.player

import org.junit.Assert.*
import org.junit.Test

class PlayerLogicTest {

    @Test
    fun testTransposer() {
        assertEquals("D", Transposer.transpose("C", 2))
        assertEquals("F#m7", Transposer.transpose("Em7", 2))
        assertEquals("Bb", Transposer.transpose("Bb", 0))
        assertEquals("C", Transposer.transpose("Bb", 2)) // Bb -> B -> C
        assertEquals("A/C#", Transposer.transpose("G/B", 2))
        assertEquals("D/F#", Transposer.transpose("C/E", 2))

        // Down transposition
        assertEquals("C", Transposer.transpose("D", -2))
        assertEquals("Em7", Transposer.transpose("F#m7", -2))

        // Safe transposition guards against blank / malformed chords
        assertEquals("", Transposer.transpose("", 1))
        assertEquals("   ", Transposer.transpose("   ", 2))
        assertEquals("C/", Transposer.transpose("C/", 1))

        // Transition prefixes like -, ~, and ()
        assertEquals("-A7", Transposer.transpose("-G7", 2))
        assertEquals("-(A7)", Transposer.transpose("-(G7)", 2))
        assertEquals("~Bm", Transposer.transpose("~Am", 2))
        assertEquals("-E/G#", Transposer.transpose("-D/F#", 2))
    }

    @Test
    fun testChordParser() {
        val content = """
            [Verse 1]
            C        G
            I found a love for me
            Am       F
            Darling, just dive right in

            [Chorus]
            C/E      Fmaj7
            Barefoot on the grass
        """.trimIndent()

        val sheet = ChordParser.parse(content)

        assertEquals(2, sheet.sections)
        assertEquals(3, sheet.chordLineCount)
        assertEquals(3, sheet.lyricLineCount) // including blank lines? No, Blank is separate

        // Check lines
        assertTrue(sheet.lines[0] is SheetLine.Section)
        assertTrue(sheet.lines[1] is SheetLine.Chords)
        assertTrue(sheet.lines[2] is SheetLine.Lyric)

        // Column detection
        val chordsLine = sheet.lines[1] as SheetLine.Chords
        assertEquals(2, chordsLine.tokens.size)
        assertEquals("C", chordsLine.tokens[0].symbol)
        assertEquals(0, chordsLine.tokens[0].column)
        assertEquals("G", chordsLine.tokens[1].symbol)
        assertEquals(9, chordsLine.tokens[1].column)
    }

    @Test
    fun testChordParserBracketChordPro() {
        // Real catalog shape: `[Chord]` overlay lines sit above lyric lines.
        // Columns below are counted from the opening `[`, which the data confirms
        // is where the chord belongs for lyric alignment.
        val overlay1 = "  [C]" + "    " + "[Am]" // '[' at 2 and at 9
        val overlay2 = "     [C]"               // '[' at 5
        val content = "[Verse 1]\n$overlay1\nI heard there was a secret chord\n$overlay2\nThat David played and it pleased the Lord"

        val sheet = ChordParser.parse(content)

        assertEquals(1, sheet.sections)
        assertTrue(sheet.lines[0] is SheetLine.Section)

        // First chord overlay: C at column 2, Am at column 9.
        val c1 = sheet.lines[1] as SheetLine.Chords
        assertEquals(listOf("C", "Am"), c1.tokens.map { it.symbol })
        assertEquals(listOf(2, 9), c1.tokens.map { it.column })
        // The lyric line below is preserved verbatim.
        assertEquals("I heard there was a secret chord", (sheet.lines[2] as SheetLine.Lyric).text)

        // Second overlay has a single chord C at column 5.
        val c2 = sheet.lines[3] as SheetLine.Chords
        assertEquals(listOf("C"), c2.tokens.map { it.symbol })
        assertEquals(listOf(5), c2.tokens.map { it.column })
    }

    @Test
    fun testChordParserBracketOnlyLineIsChordNotSection() {
        // `[C] [Am]` (trimmed) starts '[' and ends ']', but is chords, not a heading.
        val sheet = ChordParser.parse("[C] [Am]\nI heard")
        assertEquals(0, sheet.sections)
        assertTrue(sheet.lines[0] is SheetLine.Chords)
        assertEquals(listOf("C", "Am"), (sheet.lines[0] as SheetLine.Chords).tokens.map { it.symbol })
        assertTrue(sheet.lines[1] is SheetLine.Lyric)
    }

    @Test
    fun testChordParserInlineChordsSplitIntoLyricAndOverlay() {
        // `[C]I heard [Am]the news`: C over "I"(0); [Am] begins at 11, minus the
        // 3 chars of `[C]` -> col 8, exactly above "the".
        val sheet = ChordParser.parse("[C]I heard [Am]the news\nnext line")
        val overlay = sheet.lines[0] as SheetLine.Chords
        assertEquals(listOf("C", "Am"), overlay.tokens.map { it.symbol })
        assertEquals(listOf(0, 8), overlay.tokens.map { it.column })
        assertEquals("I heard the news", (sheet.lines[1] as SheetLine.Lyric).text)
    }

    @Test
    fun testChordParserBracketDistinctChords() {
        val sheet = ChordParser.parse("[C] [G/B]\nlyric\n[Am7] [D/F#]\nmore")
        assertEquals(listOf("Am7", "C", "D/F#", "G/B"), sheet.distinctChords)
        assertEquals(2, sheet.chordLineCount)
    }

    @Test
    fun testChordParserBracketedFlagMatchesSourceFormat() {
        val sheet = ChordParser.parse("""[C]  [G/B]
lyric one
C  G/B  C
lyric two""")
        val bracketed = sheet.lines[0] as SheetLine.Chords
        assertTrue(bracketed.bracketed)
        val plain = sheet.lines[2] as SheetLine.Chords
        assertFalse(plain.bracketed)
        assertEquals(listOf("C", "G/B", "C"), plain.tokens.map { it.symbol })
    }

    @Test
    fun testTransitionChordOnOverlayLine() {
        val input = """
            [Am]      [D]             [G]    -G7
             dan hampir tak dapat bicara..
        """.trimIndent()

        val sheet = ChordParser.parse(input)
        assertEquals(1, sheet.chordLineCount)
        assertEquals(1, sheet.lyricLineCount)

        val chordLine = sheet.lines[0] as SheetLine.Chords
        assertEquals(listOf("Am", "D", "G", "-G7"), chordLine.tokens.map { it.symbol })

        val lyricLine = sheet.lines[1] as SheetLine.Lyric
        assertEquals(" dan hampir tak dapat bicara..", lyricLine.text)

        // distinct chords clean the prefix
        assertTrue(sheet.distinctChords.contains("G7"))
    }

    @Test
    fun testCleanChordSymbol() {
        assertEquals("G7", ChordParser.cleanChordSymbol("-G7"))
        assertEquals("G7", ChordParser.cleanChordSymbol("-(G7)"))
        assertEquals("Am", ChordParser.cleanChordSymbol("~Am"))
        assertEquals("C", ChordParser.cleanChordSymbol("C"))
    }
}
