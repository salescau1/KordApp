package com.chordapp.player

import org.json.JSONObject

/** A chord symbol split into its root note and quality suffix (e.g. "C#m7"). */
data class ParsedChordSymbol(val root: String, val suffix: String)

object ChordSymbol {
    /**
     * Splits a chord symbol the same way [Transposer] does: the root is one
     * note letter, extended to two when it carries an accidental ([C#]/[Db]),
     * and everything after it is the suffix. A slash bass ("D/F#") is dropped —
     * we diagram the main chord, not the bass note.
     */
    fun parse(symbol: String): ParsedChordSymbol? {
        val s = symbol.trim()
        if (s.isEmpty()) return null
        val slash = s.indexOf('/')
        val main = if (slash > 0) s.substring(0, slash) else s
        if (main.isEmpty()) return null
        val rootLen = if (main.length > 1 && (main[1] == '#' || main[1] == 'b')) 2 else 1
        return ParsedChordSymbol(main.substring(0, rootLen), main.substring(rootLen))
    }
}

/**
 * In-memory fingering database, loaded once from the shipped asset
 * (`assets/chords_guitar.json`, chords-db "guitar.json" format).
 *
 * Asset shape: `{ "main": { …, "chords": { "<ROOT>": [ { "key", "suffix",
 * "positions": [ { "frets", "fingers", "baseFret", "barres", "midi" } ] } ] } } }`.
 * See [FretPosition] for the fret/finger semantics.
 *
 * The asset lists every known position for a chord, ordered by base fret, so
 * the first entry is frequently a shifted barre nobody actually plays. This
 * class keeps the whole ranked list: [find] returns the most playable one
 * and [findPositions] exposes the alternates for the diagram UI.
 *
 * This class never fabricates a diagram: [find] returns null for anything the
 * asset does not contain, and callers show an honest "not available" message.
 */
class ChordFingeringDb private constructor(
    private val chords: Map<String, Map<String, List<FretPosition>>>,
) {
    /** Most playable position for [symbol], or null when the asset has none. */
    fun find(symbol: String): FretPosition? = findPositions(symbol).firstOrNull()

    /** All positions for [symbol], most playable first; empty when absent. */
    fun findPositions(symbol: String): List<FretPosition> {
        // 1. Slash chord ("D/F#", "Dm/F#"): the asset stores the real "over bass"
        // shape as a suffix like "/F#" or "m/F#". Try that exact voicing first so
        // the diagram matches the written chord instead of just its main triad.
        slashPositions(symbol)?.let { return it }

        // 2. Plain chord (or a slash whose exact voicing is not in the asset):
        // diagram the main chord. ChordSymbol.parse drops any bass note here.
        val parsed = ChordSymbol.parse(symbol) ?: return emptyList()
        for (root in rootCandidates(parsed.root)) {
            val bySuffix = chords[root] ?: continue
            for (suffix in suffixCandidates(parsed.suffix)) {
                bySuffix[suffix]?.let { return rank(it) }
            }
        }
        return emptyList()
    }

    /**
     * Exact slash-chord voicing from the asset, or null when the symbol is not a
     * slash chord or that specific "over bass" shape is not stored. The asset
     * keeps the main suffix as written plus the bass note ("" -> "/F#", "m" ->
     * "m/F#"), so we rebuild that key and try both spellings of the bass note.
     */
    private fun slashPositions(symbol: String): List<FretPosition>? {
        val s = symbol.trim()
        val slash = s.indexOf('/')
        if (slash <= 0 || slash >= s.length - 1) return null
        val bass = s.substring(slash + 1).trim()
        val parsedMain = ChordSymbol.parse(s.substring(0, slash)) ?: return null
        for (root in rootCandidates(parsedMain.root)) {
            val bySuffix = chords[root] ?: continue
            for (mainSuffix in suffixCandidates(parsedMain.suffix)) {
                // suffixCandidates may alias "" -> "major"; the slash key uses the
                // raw main suffix, so skip an aliased spelling that is not empty.
                val raw = if (mainSuffix == "major") "" else if (mainSuffix == "minor") "m" else mainSuffix
                for (bassSpelling in bassCandidates(bass)) {
                    bySuffix["$raw/$bassSpelling"]?.let { positions ->
                        val ranked = rank(positions)
                        // Some chords-db slash voicings are very sparse partials
                        // (e.g. E/G# = [-1,-1,-1,1,0,0], only 3 strings) that read
                        // as a lone fret. Only use the exact voicing when its best
                        // shape actually sounds a real chord; otherwise fall through
                        // and let the caller diagram the fuller main-chord shape.
                        val best = ranked.firstOrNull()
                        if (best != null && best.frets.count { it >= 0 } >= 4) return ranked
                    }
                }
            }
        }
        return null
    }

    private fun rank(positions: List<FretPosition>): List<FretPosition> =
        positions.sortedBy { playabilityScore(it) }

    /**
     * Lower is easier to play. An open shape (frets touching the nut) always
     * outranks a shifted shape. Within open shapes, prefer fewer fretted
     * strings, no barre, and a narrower span. Within shifted shapes, prefer
     * the lowest position on the neck, fewer fretted strings, no barre, and a
     * narrower span.
     */
    private fun playabilityScore(p: FretPosition): Int {
        val fretted = p.frets.count { it > 0 }
        val open = p.frets.any { it == 0 }
        val values = p.frets.filter { it > 0 }
        val span = if (values.size > 1) values.max() - values.min() else 0
        val barre = if (p.barres.isNotEmpty()) 5 else 0
        return if (open) fretted + span * 2 + barre else 100 + (p.baseFret - 1) * 10 + fretted * 8 + span * 6 + barre
    }

    companion object {
        // Asset root spellings: 12 canonical keys (C Csharp D Eb E F Fsharp G Ab A Bb B).
        // A sheet sharp or flat resolves to that canonical key (F#/Gb -> Fsharp).
        private val ASSET_KEYS = mapOf(
            "C" to "C", "C#" to "Csharp", "Db" to "Csharp",
            "D" to "D", "D#" to "Eb", "Eb" to "Eb",
            "E" to "E",
            "F" to "F", "F#" to "Fsharp", "Gb" to "Fsharp",
            "G" to "G", "G#" to "Ab", "Ab" to "Ab",
            "A" to "A", "A#" to "Bb", "Bb" to "Bb",
            "B" to "B"
        )

        /**
         * Sheet spellings that differ from the asset's lowercase literals.
         * Most suffixes (m7, maj7, sus4, add9, …) already match exactly.
         */
        private val SUFFIX_ALIASES = mapOf(
            "" to listOf("major"),
            "M" to listOf("major"),
            "maj" to listOf("major"),
            "m" to listOf("minor"),
            "min" to listOf("minor"),
            "minor" to listOf("minor"),
            "M7" to listOf("maj7"),
            "M9" to listOf("maj9"),
            "M11" to listOf("maj11"),
            "M13" to listOf("maj13")
        )

        private fun rootCandidates(root: String): List<String> = buildList {
            add(root)
            ASSET_KEYS[root]?.let { if (it != root) add(it) }
        }

        private fun suffixCandidates(suffix: String): List<String> {
            SUFFIX_ALIASES[suffix]?.let { return it }
            // Asset literals are lowercase; sheets usually already are too.
            return if (suffix == suffix.lowercase()) listOf(suffix) else listOf(suffix, suffix.lowercase())
        }

        // Bass-note spellings the slash suffixes use in the asset (sharps for
        // C#/D#/F#/G#, flat for Bb). A sheet may write the enharmonic twin, so we
        // try the note as written first, then the asset's preferred spelling.
        private val BASS_ENHARMONIC = mapOf(
            "Db" to "C#", "Eb" to "D#", "Gb" to "F#", "Ab" to "G#", "A#" to "Bb"
        )

        private fun bassCandidates(bass: String): List<String> = buildList {
            add(bass)
            BASS_ENHARMONIC[bass]?.let { if (it != bass) add(it) }
        }

        /** Parses the shipped asset text. Returns a DB with whatever is present. */
        fun fromJson(text: String): ChordFingeringDb {
            val root = JSONObject(text)
            // The shipped chords-db asset keeps the root->array map at the TOP
            // level under a top-level chords object; main is only metadata (strings, name, ...).
            // The legacy main -> chords nesting is kept for the test fixtures.
            val chordsHolder = when {
                root.optJSONObject("chords") != null -> root.getJSONObject("chords")
                else -> {
                    val main = if (root.has("main")) root.getJSONObject("main") else root
                    if (main.has("chords")) main.getJSONObject("chords") else main
                }
            }

            val map = HashMap<String, HashMap<String, MutableList<FretPosition>>>()
            val keys = chordsHolder.keys()
            while (keys.hasNext()) {
                val rootName = keys.next()
                val arr = chordsHolder.optJSONArray(rootName) ?: continue
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    if (obj.isNull("suffix")) continue
                    val suffix = obj.optString("suffix")
                    if (suffix.isEmpty()) continue
                    val positions = obj.optJSONArray("positions")
                    if (positions == null || positions.length() == 0) continue
                    val parsed = (0 until positions.length())
                        .mapNotNull { parsePosition(positions.optJSONObject(it)) }
                    if (parsed.isEmpty()) continue
                    map.getOrPut(rootName) { HashMap() }[suffix] = parsed.toMutableList()
                }
            }
            return ChordFingeringDb(map)
        }

        private fun parsePosition(pos: JSONObject): FretPosition? {
            val fretsArr = pos.optJSONArray("frets") ?: return null
            val frets = IntArray(fretsArr.length()) { fretsArr.getInt(it) }.toList()
            if (frets.size < 6) return null // 6-string asset
            val fingersArr = pos.optJSONArray("fingers")
            val fingers = if (fingersArr == null) List(frets.size) { 0 } else {
                IntArray(fingersArr.length()) { fingersArr.getInt(it) }.toList()
            }
            val barresArr = pos.optJSONArray("barres")
            val barres = if (barresArr == null) emptyList() else {
                IntArray(barresArr.length()) { barresArr.getInt(it) }.toList()
            }
            val baseFret = if (pos.isNull("baseFret")) 1 else pos.optInt("baseFret", 1).coerceAtLeast(1)
            return FretPosition(frets, fingers, baseFret, barres)
        }
    }
}
