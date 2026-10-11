package com.chordapp.player

import kotlin.math.ceil

/**
 * Splits a sheet into discrete, whole-viewport "pages" for Page Flip mode.
 *
 * A page is a contiguous slice of lines whose total height never exceeds
 * [capacity] "slots". One slot is one monospace line. Section headings count
 * as two slots (they are rendered taller); everything else counts as one.
 *
 * The split never separates a [SheetLine.Chords] line from the lyric/chord
 * line directly beneath it, so a page never starts with a lyric whose chords
 * are on the previous page.
 *
 * Also accounts for visual line wrapping: long chord/lyric lines wrap in
 * Compose FlowRow, so their slot height is multiplied by the number of visual rows
 * they occupy on screens with [charsPerLine] columns.
 *
 * To avoid orphan section headings at the bottom of a page, a section header
 * is kept together with its first following line.
 */
object SheetPaginator {

    fun unitCount(line: SheetLine): Int = when (line) {
        is SheetLine.Section -> 2
        else -> 1
    }

    private fun lineWrapCount(lineLen: Int, charsPerLine: Int): Int {
        if (charsPerLine <= 0 || lineLen <= charsPerLine) return 1
        return ceil(lineLen.toFloat() / charsPerLine.toFloat()).toInt().coerceAtLeast(1)
    }

    /**
     * Determines visual character length of a chord line, taking into account
     * that chords attach to words in Compose rather than preserving
     * raw whitespace padding from the source text.
     */
    internal fun visualLineLength(chordLine: SheetLine.Chords?, lyricLine: SheetLine.Lyric?): Int {
        val lyricLen = lyricLine?.text?.trimEnd()?.length ?: 0
        if (chordLine == null) return lyricLen
        if (lyricLen == 0) {
            return chordLine.tokens.sumOf { it.symbol.length + 3 }
        }
        val lastToken = chordLine.tokens.maxByOrNull { it.column }
        val chordEnd = if (lastToken != null) lastToken.column + lastToken.symbol.length else 0
        return maxOf(lyricLen, chordEnd)
    }

    /**
     * Measures the slot height and the next index for the atomic content unit starting at [index].
     */
    private fun itemSlotHeight(
        lines: List<SheetLine>,
        index: Int,
        charsPerLine: Int
    ): Pair<Int, Int> {
        val line = lines[index]
        return when (line) {
            is SheetLine.Blank -> 1 to (index + 1)
            is SheetLine.Section -> 2 to (index + 1)
            is SheetLine.Chords -> {
                val next = index + 1
                if (!line.isBarLine && next < lines.size && lines[next] is SheetLine.Lyric) {
                    val lyric = lines[next] as SheetLine.Lyric
                    val effLen = visualLineLength(line, lyric)
                    val wrap = lineWrapCount(effLen, charsPerLine)
                    (2 * wrap) to (next + 1)
                } else {
                    val effLen = visualLineLength(line, null)
                    val wrap = lineWrapCount(effLen, charsPerLine)
                    wrap to next
                }
            }
            is SheetLine.Lyric -> {
                val effLen = line.text.trimEnd().length
                val wrap = lineWrapCount(effLen, charsPerLine)
                wrap to (index + 1)
            }
        }
    }

    fun paginate(
        lines: List<SheetLine>,
        capacity: Int,
        charsPerLine: Int = 40
    ): List<List<SheetLine>> {
        if (lines.isEmpty()) return emptyList()
        val cap = capacity.coerceAtLeast(1)

        val pages = mutableListOf<List<SheetLine>>()
        val current = mutableListOf<SheetLine>()
        var used = 0

        var i = 0
        while (i < lines.size) {
            // Drop leading blank lines on a new page so every page starts on real content.
            if (current.isEmpty() && lines[i] is SheetLine.Blank) {
                i++
                continue
            }

            val line = lines[i]

            // Orphan heading check for Section:
            // If current page is not empty, ensure Section has enough headroom
            // to include at least its first following content line.
            if (line is SheetLine.Section && current.isNotEmpty()) {
                var probe = i + 1
                var sectionBudget = 2 // Section heading takes 2 slots
                if (probe < lines.size && lines[probe] is SheetLine.Blank) {
                    sectionBudget += 1
                    probe++
                }
                if (probe < lines.size) {
                    val (contentHeight, _) = itemSlotHeight(lines, probe, charsPerLine)
                    sectionBudget += contentHeight
                }
                // If it cannot fit Section + its first content line on the current page,
                // push Section to a fresh page to avoid an orphan header.
                if (used + sectionBudget > cap) {
                    while (current.isNotEmpty() && current.last() is SheetLine.Blank) {
                        current.removeAt(current.size - 1)
                    }
                    if (current.isNotEmpty()) {
                        pages.add(current.toList())
                        current.clear()
                        used = 0
                    }
                    continue
                }
            }

            val (height, nextIdx) = itemSlotHeight(lines, i, charsPerLine)

            // If the item doesn't fit on the current page:
            if (current.isNotEmpty() && used + height > cap) {
                while (current.isNotEmpty() && current.last() is SheetLine.Blank) {
                    current.removeAt(current.size - 1)
                }
                if (current.isNotEmpty()) {
                    pages.add(current.toList())
                    current.clear()
                    used = 0
                }
                continue
            }

            // Add the item (could be 1 line or a paired Chord+Lyric)
            for (k in i until nextIdx) {
                current.add(lines[k])
            }
            used += height
            i = nextIdx
        }

        while (current.isNotEmpty() && current.last() is SheetLine.Blank) {
            current.removeAt(current.size - 1)
        }
        if (current.isNotEmpty()) {
            pages.add(current.toList())
        }

        return pages
    }
}
