package com.chordapp.charts

/**
 * A snapshot of a chart that was successfully fetched at [fetchedAtEpochMs].
 *
 * Snapshots are cached to disk so that going offline after a successful fetch
 * still shows the last real data (marked "hasil sinkron terakhir" in the UI).
 */
data class ChartSnapshot(
    val entries: List<ChartEntry>,
    val fetchedAtEpochMs: Long
)
