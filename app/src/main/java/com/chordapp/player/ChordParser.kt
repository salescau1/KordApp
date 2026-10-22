package com.chordapp.player

data class ChordToken(val symbol: String, val column: Int)
data class BarSegment(val text: String, val isChord: Boolean)

sealed class SheetLine {
    data class Section(val text: String) : SheetLine()
    data class Chords(
        val tokens: List<ChordToken>,
        val originalText: String,
        val bracketed: Boolean = true,
        val isBarLine: Boolean = false
    ) : SheetLine()
    data class Lyric(val text: String) : SheetLine()
    object Blank : SheetLine()
}

data class ChordSheet(
    val lines: List<SheetLine>,
    val distinctChords: List<String>,
    val sections: Int,
    val chordLineCount: Int,
    val lyricLineCount: Int
)

object ChordParser {
    private val suffixAllowed = "0123456789#+-badgijmnsu()"
    private val validLetterOnlySuffixes = setOf("", "m", "maj", "min", "dim", "aug", "sus", "add")

    fun cleanChordSymbol(symbol: String): String {
        var s = symbol.trim().trimStart('-', '–', '~', '(').trimEnd(')')
        while (s.isNotEmpty() && (s.last() == '.' || s.last() == '…' || s.last() == '~' || s.last() == '-')) {
            s = s.substring(0, s.length - 1).trim()
        }
        return s
    }

    /**
     * Strict chord-symbol test.
     *
     * A slash chord has a main part and an optional bass part (`C/E`, `C#/F`).
     * Both forms below are valid chords:
     *  - `C/E`      main chord + bass note
     *  - `/E`       bare bass note, the main chord is implied from the previous
     *                chord on the overlay (common in ChordPro sheets).
     */
    fun isChordSymbol(s: String): Boolean {
        var token = s.trim()
        if (token.isEmpty()) return false
        // Allow transition prefix like '-' or '–' or '~'
        while (token.isNotEmpty() && (token[0] == '-' || token[0] == '–' || token[0] == '~')) {
            token = token.substring(1).trim()
        }
        if (token.startsWith("(") && token.endsWith(")")) {
            token = token.substring(1, token.length - 1).trim()
        }
        // Allow trailing decorations like dots (...), ellipsis (…), tildes (~), or dashes (-)
        while (token.isNotEmpty() && (token.last() == '.' || token.last() == '…' || token.last() == '~' || token.last() == '-')) {
            token = token.substring(0, token.length - 1).trim()
        }
        if (token.isEmpty()) return false
        if (token.contains("/")) {
            val parts = token.split("/")
            if (parts.size != 2) return false
            val main = parts[0]
            val bass = parts[1]
            if (main.isEmpty()) {
                // `/E` — bare bass; valid when the bass side is a note.
                return isChordSymbol(bass)
            }
            return isChordSymbol(main) && isChordSymbol(bass)
        }
        var i = 0
        if (i < token.length && token[i] in 'A'..'G') i++ else return false
        if (i < token.length && (token[i] == '#' || token[i] == 'b')) i++
        if (i == token.length) return true
        val suffix = token.substring(i).lowercase()
        if (suffix.any { it !in suffixAllowed }) return false
        val hasMarker = suffix.any { it.isDigit() || it == '#' || it == '+' || it == '-' }
        if (!hasMarker) {
            if (suffix !in validLetterOnlySuffixes) return false
        }
        return true
    }

    /** Any single `[...]` group; only those whose inner text is a chord count. */
    private val bracketGroupRegex = Regex("\\[([^\\[\\]\\r\\n]+)\\]")

    fun unescapeHtml(text: String): String {
        return text
            .replace(Regex("&#0*39;"), "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .replace("&#x27;", "'", ignoreCase = true)
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")
            .replace("&#x2F;", "/", ignoreCase = true)
            .replace("&hellip;", "...")
    }

    fun parse(content: String): ChordSheet {
        val rawLines = unescapeHtml(content).lines()
        val sheetLines = mutableListOf<SheetLine>()
        val chordSet = mutableSetOf<String>()
        var sectionCount = 0
        var chordLineCount = 0
        var lyricLineCount = 0

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                sheetLines.add(SheetLine.Blank)
                continue
            }

            // ChordPro notation `[Chord]` is how this catalog stores chords. A
            // group whose inner text parses as a chord is a chord token; other
            // brackets (`[Chorus]`, `[Verse 1]`) are structure, not chords.
            val chordGroups = bracketGroupRegex.findAll(line)
                .map { it to isChordSymbol(it.groupValues[1]) }
                .toList()

            if (chordGroups.any { it.second }) {
                val bracketChords = chordGroups.filter { it.second }
                val chordSpans = bracketChords.map { it.first.range }

                if (isBarNotationLine(line, chordSpans)) {
                    // Bar / instrumental notation line: `| [C] [G] | [Am]  | [Dm]  | [G]   |`
                    // Kept intact on one row — never split into chords above and `|` as lyrics below.
                    val tokens = bracketChords.map { ChordToken(it.first.groupValues[1].trim(), it.first.range.first) }
                    chordSet.addAll(tokens.map { cleanChordSymbol(it.symbol) })
                    sheetLines.add(SheetLine.Chords(tokens, line, bracketed = true, isBarLine = true))
                    chordLineCount++
                } else {
                    // Blank bracketed chords from a scratch buffer to check for unbracketed transition chords (e.g. -G7)
                    val sb = StringBuilder(line)
                    for (r in chordSpans) {
                        for (i in r) sb.setCharAt(i, ' ')
                    }
                    val extraChordTokens = mutableListOf<Pair<String, IntRange>>()
                    val wordRegex = Regex("""\S+""")
                    for (m in wordRegex.findAll(sb.toString())) {
                        if (isChordSymbol(m.value)) {
                            extraChordTokens.add(m.value to m.range)
                            for (i in m.range) sb.setCharAt(i, ' ')
                        }
                    }

                    if (isOverlayOnly(sb)) {
                        // A pure overlay line: brackets and any unbracketed transition chords (e.g. -G7).
                        // Columns are taken from the original monospace position compensating for brackets.
                        val allRaw = mutableListOf<Pair<String, Int>>()
                        for (bc in bracketChords) {
                            allRaw.add(bc.first.groupValues[1].trim() to bc.first.range.first)
                        }
                        for (ec in extraChordTokens) {
                            allRaw.add(ec.first to ec.second.first)
                        }
                        allRaw.sortBy { it.second }

                        val tokens = allRaw.map { (sym, startPos) ->
                            ChordToken(sym, startPos)
                        }
                        chordSet.addAll(tokens.map { cleanChordSymbol(it.symbol) })
                        sheetLines.add(SheetLine.Chords(tokens, line, bracketed = true))
                        chordLineCount++
                    } else {
                        // Inline ChordPro: chords embedded in the same physical line as
                        // lyrics (`[C]I heard [Am]the news`). Split it into a chord
                        // overlay row plus a lyric row with the brackets removed, both
                        // column-aligned in monospace.
                        val (tokens, lyricText) = splitInline(line, bracketChords.map { it.first })
                        chordSet.addAll(tokens.map { cleanChordSymbol(it.symbol) })
                        sheetLines.add(SheetLine.Chords(tokens, line))
                        chordLineCount++
                        if (lyricText.isNotBlank() && lyricText.any { it.isLetter() }) {
                            sheetLines.add(SheetLine.Lyric(lyricText))
                            lyricLineCount++
                        }
                    }
                }
                continue
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                sheetLines.add(SheetLine.Section(trimmed))
                sectionCount++
                continue
            }

            val tokens = extractTokens(line)
            if (isChordLine(tokens)) {
                val chordTokens = tokens.map {
                    chordSet.add(it.symbol)
                    it
                }
                // Plain chord row: the source wrote these chords bare
                // (`C  G/B`) - mark unbracketed so the renderer does not invent
                // `[ ]` around symbols the site never bracketed.
                val isBar = line.contains("|")
                sheetLines.add(SheetLine.Chords(chordTokens, line, bracketed = false, isBarLine = isBar))
                chordLineCount++
            } else if (line.contains("|") && isPlainBarLine(line, tokens)) {
                val chordTokens = tokens.filter { isChordSymbol(it.symbol) }
                chordSet.addAll(chordTokens.map { it.symbol })
                sheetLines.add(SheetLine.Chords(tokens, line, bracketed = false, isBarLine = true))
                chordLineCount++
            } else if (isInlinePlainOverlay(tokens)) {
                // Plain inline overlay: a run of leading chord tokens followed by
                // lyric words on the SAME physical line (`C# Fm mu aku bahagia`).
                // The 50%-chord test above cannot see this, so without a special
                // case the chords would render as lyric text. Split the leading
                // chord run into an overlay row that aligns with the lyric below.
                val splitIndex = leadingChordRun(tokens)
                val chordTokens = tokens.take(splitIndex)
                val lyricTokens = tokens.drop(splitIndex)
                val chordCol = chordTokens.first().column
                val overlay = chordTokens.map { ChordToken(it.symbol, it.column - chordCol) }
                chordSet.addAll(overlay.map { it.symbol })
                val lyricText = lyricTokens.joinToString(" ") { it.symbol }.trim()
                sheetLines.add(SheetLine.Chords(overlay, line, bracketed = false))
                chordLineCount++
                if (lyricText.isNotBlank()) {
                    // The lyric row starts at the same column as the lyric's first
                    // word in the source line, so each word lands under its chord.
                    val indent = " ".repeat((lyricTokens.first().column - chordCol).coerceAtLeast(0))
                    sheetLines.add(SheetLine.Lyric(indent + lyricText))
                    lyricLineCount++
                }
            } else {
                sheetLines.add(SheetLine.Lyric(line))
                lyricLineCount++
            }
        }

        return ChordSheet(
            lines = sheetLines,
            distinctChords = chordSet.toList().sorted(),
            sections = sectionCount,
            chordLineCount = chordLineCount,
            lyricLineCount = lyricLineCount
        )
    }

    /**
     * True when the line opens with a run of 2+ chord-looking tokens followed
     * by non-chord words: an inline plain-chord overlay that [isChordLine]
     * misses because the lyric words outnumber the chords.
     */
    private fun isInlinePlainOverlay(tokens: List<ChordToken>): Boolean {
        val run = leadingChordRun(tokens)
        if (run < 2 || run == tokens.size) return false
        return tokens.drop(run).any { !isChordSymbol(it.symbol) }
    }

    private fun leadingChordRun(tokens: List<ChordToken>): Int {
        var n = 0
        for (t in tokens) {
            if (isChordSymbol(t.symbol)) n++ else break
        }
        return n
    }

    /** True when, once the chord-bracket spans and extra chord tokens are blanked, only spaces or non-lyric punctuation remain. */
    private fun isOverlayOnly(sb: StringBuilder): Boolean {
        if (sb.contains('|')) return false
        val remaining = sb.toString()
            .replace(Regex("""(?i)(?:N\.C\.?|NC|\b(?:[xX]\d+|\d+[xX]|riff|fill|solo|stop|hold)\b)"""), " ")
            .replace(Regex("""[|\s:.\-_/\\()%+*~^0-9]"""), "")
        return remaining.isEmpty() || remaining.all { !it.isLetter() }
    }

    /**
     * True when the non-chord portion of a line consists solely of bar separators (`|`),
     * punctuation, measure repeats, or rests — meaning this is an instrumental / bar notation
     * row (`| [C] [G] | [Am] |`), not an inline lyric line.
     */
    fun isBarNotationLine(line: String, chordSpans: List<IntRange>): Boolean {
        if (!line.contains('|')) return false
        val sb = StringBuilder(line)
        for (r in chordSpans) {
            for (i in r) sb.setCharAt(i, ' ')
        }
        val remaining = sb.toString()
            .replace(Regex("""(?i)(?:N\.C\.?|NC|\b(?:[xX]\d+|\d+[xX]|riff|fill|solo|stop)\b)"""), " ")
            .replace(Regex("""[|\s:.\-_/\\()%+*~^0-9]"""), "")
        return remaining.isEmpty() || remaining.all { !it.isLetter() }
    }

    private fun isPlainBarLine(line: String, tokens: List<ChordToken>): Boolean {
        if (!line.contains('|')) return false
        val chords = tokens.filter { isChordSymbol(it.symbol) }
        if (chords.isEmpty()) return false
        val chordSpans = chords.map { it.column until (it.column + it.symbol.length) }
        return isBarNotationLine(line, chordSpans)
    }

    /**
     * Decomposes a bar notation line into alternating text segments and chord tokens
     * so that the bar structure (`|`, measure spaces, brackets removed) renders on
     * a single line with chords highlighted and transposed.
     */
    fun buildBarSegments(line: SheetLine.Chords): List<BarSegment> {
        if (!line.isBarLine) return emptyList()
        val segments = mutableListOf<BarSegment>()
        if (line.bracketed) {
            val matches = bracketGroupRegex.findAll(line.originalText)
                .filter { isChordSymbol(it.groupValues[1]) }
                .toList()
            var cursor = 0
            for (m in matches) {
                if (m.range.first > cursor) {
                    segments.add(BarSegment(line.originalText.substring(cursor, m.range.first), false))
                }
                segments.add(BarSegment(m.groupValues[1].trim(), true))
                cursor = m.range.last + 1
            }
            if (cursor < line.originalText.length) {
                segments.add(BarSegment(line.originalText.substring(cursor), false))
            }
        } else {
            val chordTokens = line.tokens.filter { isChordSymbol(it.symbol) }
            var cursor = 0
            for (t in chordTokens) {
                if (t.column > cursor) {
                    segments.add(BarSegment(line.originalText.substring(cursor, t.column), false))
                }
                segments.add(BarSegment(t.symbol, true))
                cursor = t.column + t.symbol.length
            }
            if (cursor < line.originalText.length) {
                segments.add(BarSegment(line.originalText.substring(cursor), false))
            }
        }
        return segments
    }

    /**
     * Splits one inline-ChordPro line into chord tokens and the lyric text with
     * the bracketed chords removed. Chord columns are shifted left by the width
     * of every earlier chord span so a chord still lands exactly above the lyric
     * word that follows it in the same monospace column.
     */
    private fun splitInline(line: String, groups: List<MatchResult>): Pair<List<ChordToken>, String> {
        val tokens = mutableListOf<ChordToken>()
        val lyric = StringBuilder()
        var removedBefore = 0
        var cursor = 0
        for (g in groups) {
            val r = g.range
            val col = r.first - removedBefore
            tokens.add(ChordToken(g.groupValues[1].trim(), col))
            lyric.append(line, cursor, r.first)
            cursor = r.last + 1
            removedBefore += r.last - r.first + 1
        }
        lyric.append(line, cursor, line.length)
        return tokens to lyric.toString()
    }

    private fun extractTokens(line: String): List<ChordToken> {
        val tokens = mutableListOf<ChordToken>()
        var i = 0
        while (i < line.length) {
            if (!line[i].isWhitespace()) {
                val start = i
                while (i < line.length && !line[i].isWhitespace()) {
                    i++
                }
                tokens.add(ChordToken(line.substring(start, i), start))
            } else {
                i++
            }
        }
        return tokens
    }

    private fun isChordLine(tokens: List<ChordToken>): Boolean {
        if (tokens.isEmpty()) return false
        var chordCount = 0
        for (t in tokens) {
            if (isChordSymbol(t.symbol)) {
                chordCount++
            } else {
                // if it contains something that is definitely not a chord (like long words), penalize heavily
                if (t.symbol.length > 7 && !t.symbol.contains("/")) return false
            }
        }
        // More than 50% of tokens must look like chords to be considered a chord line
        return chordCount.toFloat() / tokens.size > 0.5f
    }
}
