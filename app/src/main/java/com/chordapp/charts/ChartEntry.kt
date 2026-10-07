package com.chordapp.charts

/** One row of a chart: real rank/title/artist taken from the source (never fabricated). */
data class ChartEntry(
    val rank: Int,
    val title: String,
    val artist: String
)
