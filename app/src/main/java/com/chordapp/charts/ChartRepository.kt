package com.chordapp.charts

/** Outcome of trying to load a chart. Never fabricates entries. */
sealed interface ChartResult {
    /** Just fetched from the network and persisted. */
    data class Fresh(val snapshot: ChartSnapshot) : ChartResult

    /** Network failed; returned the last successfully fetched snapshot from cache. */
    data class FromCache(val snapshot: ChartSnapshot) : ChartResult

    /** Network failed and there is no cache: show a truthful empty/unavailable state. */
    data object NoData : ChartResult
}

interface ChartRepository {
    /** Last valid local snapshot, returned without network access. */
    fun cached(platform: ChartPlatform): ChartSnapshot? = null

    fun cachedResolved(platform: ChartPlatform): List<com.chordapp.viewmodels.ChartRowUi>? = null

    fun saveCachedResolved(platform: ChartPlatform, rows: List<com.chordapp.viewmodels.ChartRowUi>) {}

    suspend fun load(platform: ChartPlatform): ChartResult
}
