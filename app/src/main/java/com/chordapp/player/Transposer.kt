package com.chordapp.player

object Transposer {
    private val notesSharp = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val notesFlat = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    fun transpose(chord: String, delta: Int): String {
        if (delta == 0 || chord.isBlank()) return chord

        // Preserve leading transition prefixes like '-' or '–' or '~' or '('
        val prefix = chord.takeWhile { it in "-–~(" }
        val suffixParen = if (chord.endsWith(")") && prefix.contains('(')) ")" else ""
        val core = chord.substring(prefix.length, chord.length - suffixParen.length).trim()
        if (core.isEmpty()) return chord

        // Handle slash chords like D/F# or -D/F#
        if (core.contains("/")) {
            val parts = core.split("/")
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                val main = transposeSingle(parts[0].trim(), delta)
                val bass = transposeSingle(parts[1].trim(), delta)
                return "$prefix$main/$bass$suffixParen"
            }
            return chord
        }
        return "$prefix${transposeSingle(core, delta)}$suffixParen"
    }

    private fun transposeSingle(chord: String, delta: Int): String {
        if (chord.isEmpty()) return chord

        val rootLen = when {
            chord.length > 1 && (chord[1] == '#' || chord[1] == 'b') -> 2
            else -> 1
        }

        if (chord.length < rootLen) return chord

        val root = chord.substring(0, rootLen)
        val suffix = chord.substring(rootLen)

        val isFlat = root.contains("b")
        val noteArray = if (isFlat) notesFlat else notesSharp
        val index = noteArray.indexOfFirst { it.equals(root, ignoreCase = true) }

        if (index == -1) return chord // Not recognized, return as is

        var newIndex = (index + delta) % 12
        if (newIndex < 0) newIndex += 12

        // Heuristic for output: use sharp by default, except for keys where flat is common, but keeping it simple for now
        // We use sharp array for output unless input was flat
        val outArray = if (isFlat) notesFlat else notesSharp
        return outArray[newIndex] + suffix
    }
}
