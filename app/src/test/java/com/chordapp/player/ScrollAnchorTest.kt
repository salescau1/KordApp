package com.chordapp.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure scroll re-anchoring: after a version switch the Player should land back
 * on the same section that was on screen.
 */
class ScrollAnchorTest {

    private val old = listOf(
        SheetLine.Section("[Intro]"),
        SheetLine.Lyric("na na na"),
        SheetLine.Section("[Verse]"),
        SheetLine.Lyric("line one"),
        SheetLine.Lyric("line two"),
        SheetLine.Section("[Chorus]"),
        SheetLine.Lyric("chorus!")
    )

    @Test
    fun `nearestSection returns the heading at or above the index`() {
        assertEquals("[Intro]", ScrollAnchor.nearestSection(old, 0))
        assertEquals("[Verse]", ScrollAnchor.nearestSection(old, 4))
        assertEquals("[Chorus]", ScrollAnchor.nearestSection(old, 6))
        assertEquals("[Intro]", ScrollAnchor.nearestSection(old, 1))
    }

    @Test
    fun `anchor lands on the first occurrence of the same section in the new sheet`() {
        val shifted = listOf(
            SheetLine.Section("[Chorus]"),
            SheetLine.Lyric("c"),
            SheetLine.Section("[Verse]"),
            SheetLine.Lyric("v"),
            SheetLine.Section("[Intro]"),
            SheetLine.Lyric("i")
        )
        // Old viewport was in [Chorus]; new sheet puts [Chorus] at 0.
        assertEquals(0, ScrollAnchor.anchorIndex(old, 6, shifted))
        // Old viewport in [Verse]; new sheet puts [Verse] at 2.
        assertEquals(2, ScrollAnchor.anchorIndex(old, 3, shifted))
    }

    @Test
    fun `anchor falls back to clamped old index when the section is gone`() {
        val noSections = listOf(SheetLine.Lyric("a"), SheetLine.Lyric("b"), SheetLine.Lyric("c"))
        assertEquals(2, ScrollAnchor.anchorIndex(old, 6, noSections))
        assertEquals(0, ScrollAnchor.anchorIndex(old, 0, noSections))
    }

    @Test
    fun `empty new sheet anchors to zero`() {
        assertEquals(0, ScrollAnchor.anchorIndex(old, 4, emptyList()))
    }

    @Test
    fun `no section above the viewport falls back instead of guessing`() {
        val lyricOnly = listOf(
            SheetLine.Lyric("top"),
            SheetLine.Lyric("mid"),
            SheetLine.Lyric("low"),
            SheetLine.Section("[Verse]")
        )
        assertEquals(2, ScrollAnchor.anchorIndex(lyricOnly, 2, old))
    }

    @Test
    fun `nearestSection over an empty list is null`() {
        assertNull(ScrollAnchor.nearestSection(emptyList(), 0))
    }
}
