package com.chordapp.player

/**
 * One playable fingering position, matching the chords-db (guitar.json) asset
 * format. [frets] and [fingers] are indexed by string, string 0 = low E …
 * string 5 = high e.
 *
 * A fret value is the position counted from the top of the diagram, **not** an
 * absolute fret on the neck:
 *  - 0  -> open string
 *  - -1 -> muted / not played
 *  - >0 -> press the [value]th fret down from the top line; value 1 sits at
 *          [baseFret] (actual fret on the neck = baseFret + value - 1)
 *
 * [barres] lists barre positions with the same value convention (drawn as a
 * thick line over the fretted strings). [baseFret] is 1 for open shapes (the
 * top line is the nut) and higher for shifted shapes (the top line is a plain
 * fret and the shape is labelled with [baseFret]).
 */
data class FretPosition(
    val frets: List<Int>,
    val fingers: List<Int>,
    val baseFret: Int,
    val barres: List<Int>,
)
