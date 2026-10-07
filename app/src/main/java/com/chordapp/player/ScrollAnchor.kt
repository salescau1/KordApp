package com.chordapp.player

/**
 * Preserves the user's place in the sheet when a version switch swaps the
 * content: remember the section ([Verse]/[Chorus]/…) under the viewport and
 * re-anchor to the same section in the new sheet.
 */
object ScrollAnchor {

    /** The section heading at or above [index], or null when none is above. */
    fun nearestSection(lines: List<SheetLine>, index: Int): String? {
        for (i in index downTo 0) {
            val line = lines.getOrNull(i) ?: continue
            if (line is SheetLine.Section) return line.text
        }
        return null
    }

    /**
     * Index in [newLines] to land on after the content changed. Anchors to the
     * first occurrence of the section that was on screen; falls back to a
     * clamped version of the old index when the section is gone.
     */
    fun anchorIndex(oldLines: List<SheetLine>, oldIndex: Int, newLines: List<SheetLine>): Int {
        if (newLines.isEmpty()) return 0
        val fallback = oldIndex.coerceIn(0, newLines.size - 1)
        val section = nearestSection(oldLines, oldIndex) ?: return fallback
        val found = newLines.indexOfFirst { it is SheetLine.Section && it.text == section }
        return if (found >= 0) found else fallback
    }
}
