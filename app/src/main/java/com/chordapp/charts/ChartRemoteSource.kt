package com.chordapp.charts

/** Fetches the current Top-20 for [platform]. Throws on any failure. */
interface ChartRemoteSource {
    suspend fun fetchTop20(platform: ChartPlatform): List<ChartEntry>
}
