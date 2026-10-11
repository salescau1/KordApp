package com.chordapp.charts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Combines fetch + cache: try a fresh fetch (persist on success); on any failure
 * fall back to the last cached snapshot, and if there is none, [ChartResult.NoData].
 */
class ChartRepositoryImpl(
    private val remote: ChartRemoteSource,
    private val cache: ChartCacheStore
) : ChartRepository {

    override fun cached(platform: ChartPlatform): ChartSnapshot? = cache.read(platform)
    override fun cachedResolved(platform: ChartPlatform): List<com.chordapp.viewmodels.ChartRowUi>? =
        cache.readResolved(platform)
    override fun saveCachedResolved(platform: ChartPlatform, rows: List<com.chordapp.viewmodels.ChartRowUi>) {
        cache.writeResolved(platform, rows)
    }

    override suspend fun load(platform: ChartPlatform): ChartResult =
        withContext(Dispatchers.IO) {
            val fresh = try {
                val entries = remote.fetchTop20(platform)
                if (entries.isEmpty()) throw IOException("empty chart")
                val snapshot = ChartSnapshot(entries.take(50), System.currentTimeMillis())
                cache.write(platform, snapshot)
                snapshot
            } catch (_: Exception) {
                null
            }

            when {
                fresh != null -> ChartResult.Fresh(fresh)
                cache.read(platform) != null -> ChartResult.FromCache(cache.read(platform)!!)
                else -> ChartResult.NoData
            }
        }
}
