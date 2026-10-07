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

            // Build one atomic block.
            var height: Int
            var j = i + 1

            when (val line = lines[i]) {
                is SheetLine.Blank -> {
                    height = 1
                }
                is SheetLine.Section -> {
                    // Section header: takes 2 slots.
                    // Group with following blank (if any) and first content line to avoid orphan heading.
                    var secHeight = 2
                    var nextIdx = j
                    if (nextIdx < lines.size && lines[nextIdx] is SheetLine.Blank) {
                        secHeight += 1
                        nextIdx++
                    }
                    if (nextIdx < lines.size) {
                        val contentLine = lines[nextIdx]
                        when (contentLine) {
                            is SheetLine.Chords -> {
                                var pairNext = nextIdx + 1
                                if (!contentLine.isBarLine && pairNext < lines.size && lines[pairNext] is SheetLine.Lyric) {
                                    val effLen = maxOf(
                                        contentLine.originalText.trimEnd().length,
                                        (lines[pairNext] as SheetLine.Lyric).text.trimEnd().length
                                    )
                                    val wrap = lineWrapCount(effLen, charsPerLine)
                                    secHeight += 2 * wrap
                                    pairNext++
                                } else {
                                    val effLen = contentLine.originalText.trimEnd().length
                                    secHeight += lineWrapCount(effLen, charsPerLine)
                                }
                                j = pairNext
                            }
                            is SheetLine.Lyric -> {
                                val effLen = contentLine.text.trimEnd().length
                                secHeight += lineWrapCount(effLen, charsPerLine)
                                j = nextIdx + 1
                            }
                            else -> {
                                j = nextIdx
                            }
                        }
                    }

                    // If the grouped block cannot fit within the entire page capacity,
                    // unbind to let Section stand on its own slot.
                    if (secHeight > cap) {
                        height = 2
                        j = i + 1
                    } else {
                        height = secHeight
                    }
                }
                is SheetLine.Chords -> {
                    if (!line.isBarLine && j < lines.size && lines[j] is SheetLine.Lyric) {
                        val chordLen = line.originalText.trimEnd().length
                        val lyricLen = (lines[j] as SheetLine.Lyric).text.trimEnd().length
                        val effLen = maxOf(chordLen, lyricLen)
                        val wrap = lineWrapCount(effLen, charsPerLine)
                        height = 2 * wrap
                        j++
                    } else {
                        val effLen = line.originalText.trimEnd().length
                        height = lineWrapCount(effLen, charsPerLine)
                    }
                }
                is SheetLine.Lyric -> {
                    val effLen = line.text.trimEnd().length
                    height = lineWrapCount(effLen, charsPerLine)
                }
            }

            if (current.isNotEmpty() && used + height > cap) {
                // Trim trailing blank lines from current page before finalizing.
                while (current.isNotEmpty() && current.last() is SheetLine.Blank) {
                    current.removeAt(current.size - 1)
                }
                if (current.isNotEmpty()) {
                    pages.add(current.toList())
                    current.clear()
                    used = 0
                }
                // Do not increment i; re-evaluate from i on the new clean page.
                continue
            }

            for (k in i until j) {
                current.add(lines[k])
            }
            used += height
            i = j
        }

        while (current.isNotEmpty() && current.last() is SheetLine.Blank) {
            current.removeAt(current.size - 1)
        }
        if (current.isNotEmpty()) pages.add(current.toList())
        return pages
    }
}
