package com.chordapp.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** Page Flip pagination: whole-viewport pages, never splitting a chord from its lyric. */
class SheetPaginatorTest {

    private fun chord(symbol: String = "C") = SheetLine.Chords(listOf(ChordToken(symbol, 0)), symbol)

    @Test
    fun `fills pages up to capacity`() {
        val lines = listOf(SheetLine.Lyric("1"), SheetLine.Lyric("2"), SheetLine.Lyric("3"), SheetLine.Lyric("4"))
        val pages = SheetPaginator.paginate(lines, capacity = 2)
        assertEquals(2, pages.size)
        assertEquals(listOf("1", "2"), pages[0].map { (it as SheetLine.Lyric).text })
        assertEquals(listOf("3", "4"), pages[1].map { (it as SheetLine.Lyric).text })
    }

    @Test
    fun `never splits a chord line from its lyric`() {
        val l0 = SheetLine.Lyric("first words")
        val l1 = SheetLine.Lyric("second words")
        val lines = listOf(chord("G"), l0, l1)
        val pages = SheetPaginator.paginate(lines, capacity = 2)
        // chord("G") + l0 form one atomic block that fills the page.
        assertEquals(2, pages.size)
        assertEquals(listOf<SheetLine>(chord("G"), l0), pages[0])
        assertEquals(listOf<SheetLine>(l1), pages[1])
    }

    @Test
    fun `section heading plus a short verse fit one page when tall enough`() {
        val lines = listOf(SheetLine.Section("[Verse]"), SheetLine.Lyric("walking"))
        val pages = SheetPaginator.paginate(lines, capacity = 3)
        assertEquals(1, pages.size)
        assertEquals(2, pages[0].size)
    }

    @Test
    fun `section takes two slots so a full sheet wraps pages`() {
        val lines = listOf(
            SheetLine.Section("[Chorus]"),
            chord("C"), SheetLine.Lyric("aa"),
            chord("G"), SheetLine.Lyric("bb")
        )
        val pages = SheetPaginator.paginate(lines, capacity = 3)
        // Blocks: [Chorus](2), [C+aa](2), [G+bb](2). First page holds Chorus only.
        assertEquals(3, pages.size)
        assertEquals(SheetLine.Section("[Chorus]"), pages[0].single())
        assertEquals(listOf<SheetLine>(chord("C"), SheetLine.Lyric("aa")), pages[1])
    }

    @Test
    fun `degenerate capacities never crash`() {
        val lines = listOf(chord("D"), SheetLine.Lyric("lyric"), SheetLine.Lyric("more"))
        val pages = SheetPaginator.paginate(lines, capacity = 0)
        // capacity is coerced to 1: the chord+lyric block (2 slots) can't share a
        // page with the trailing lyric, so it splits into two pages.
        assertEquals(2, pages.size)
        assertEquals(listOf<SheetLine>(chord("D"), SheetLine.Lyric("lyric")), pages[0])
        assertEquals(listOf<SheetLine>(SheetLine.Lyric("more")), pages[1])
    }

    @Test
    fun `empty sheet yields no pages`() {
        assertEquals(0, SheetPaginator.paginate(emptyList(), capacity = 5).size)
    }

    @Test
    fun `long chord and lyric line accounts for wrapping`() {
        // Line with 50 characters: on charsPerLine = 25, it wraps into 2 rows (4 slots).
        val longChord = SheetLine.Chords(listOf(ChordToken("C", 0), ChordToken("G", 45)), "C" + " ".repeat(44) + "G")
        val longLyric = SheetLine.Lyric("a".repeat(50))
        val shortLyric = SheetLine.Lyric("short")
        val lines = listOf(longChord, longLyric, shortLyric)
        // With capacity = 4, the longChord + longLyric (4 slots) fills the entire page.
        // The shortLyric must be pushed to page 2.
        val pages = SheetPaginator.paginate(lines, capacity = 4, charsPerLine = 25)
        assertEquals(2, pages.size)
        assertEquals(listOf<SheetLine>(longChord, longLyric), pages[0])
        assertEquals(listOf<SheetLine>(shortLyric), pages[1])
    }

    @Test
    fun `close your eyes never cuts off bridge or separates bridge lyrics`() {
        val stream = javaClass.classLoader!!.getResourceAsStream("close_your_eyes.txt")!!
        val content = stream.bufferedReader().readText()
        val sheet = ChordParser.parse(content)
        // Test over typical capacity ranges (25 to 50) and mobile charsPerLine (35 to 55)
        for (cap in 25..50) {
            for (chars in listOf(35, 40, 45, 50)) {
                val pages = SheetPaginator.paginate(sheet.lines, capacity = cap, charsPerLine = chars)
                for (pIdx in pages.indices) {
                    val page = pages[pIdx]
                    val hasBridge = page.any { it is SheetLine.Section && it.text.contains("Bridge") }
                    if (hasBridge) {
                        // Bridge must never be the last line on a page without its lyrics
                        val bridgeIdx = page.indexOfFirst { it is SheetLine.Section && it.text.contains("Bridge") }
                        val linesAfterBridge = page.drop(bridgeIdx + 1).filter { it !is SheetLine.Blank }
                        org.junit.Assert.assertTrue(
                            "Bridge heading at cap=$cap chars=$chars on page ${pIdx+1} must be followed by content, not orphaned",
                            linesAfterBridge.isNotEmpty()
                        )
                        // Specifically, When your love pours down on me should follow Bridge
                        val hasWhen = linesAfterBridge.any { it is SheetLine.Lyric && it.text.contains("When your love") }
                        org.junit.Assert.assertTrue(
                            "Bridge on page ${pIdx+1} must contain 'When your love pours down on me' when Bridge is on this page",
                            hasWhen
                        )
                    }
                }
            }
        }
    }
}
