package com.chordapp.charts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

/**
 * Fallback behavior of ChartRepositoryImpl: fresh fetch wins and is cached; a failing
 * fetch falls back to the last snapshot; with neither, NoData — never invented data.
 */
@RunWith(RobolectricTestRunner::class)
class ChartRepositoryImplTest {

    private val entries = listOf(
        ChartEntry(1, "Kirana", "Dewa 19"),
        ChartEntry(2, "Hati-Hati di Jalan", "Tulus & Friends")
    )

    private lateinit var dir: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dir = File(context.cacheDir, "chart_repo_test_${System.nanoTime()}")
    }

    private class FakeRemote(
        private val result: List<ChartEntry>? // null => throws
    ) : ChartRemoteSource {
        override suspend fun fetchTop20(platform: ChartPlatform): List<ChartEntry> {
            return result ?: throw IOException("connection refused")
        }
    }

    @Test
    fun `fresh fetch wins and is persisted for later offline fallback`() = runBlocking {
        val cache = ChartCacheStore(dir)
        val repo = ChartRepositoryImpl(remote = FakeRemote(entries), cache = cache)

        val first = repo.load(ChartPlatform.SPOTIFY)
        assertTrue("expected Fresh, got $first", first is ChartResult.Fresh)
        assertEquals(entries, (first as ChartResult.Fresh).snapshot.entries)

        // Now the network dies; the second load must come back from the cache we just wrote.
        val offline = ChartRepositoryImpl(remote = FakeRemote(null), cache = cache)
        val second = offline.load(ChartPlatform.SPOTIFY)
        assertTrue("expected FromCache, got $second", second is ChartResult.FromCache)
        assertEquals(entries, (second as ChartResult.FromCache).snapshot.entries)
    }

    @Test
    fun `failure with no cache yields NoData`() = runBlocking {
        val repo = ChartRepositoryImpl(remote = FakeRemote(null), cache = ChartCacheStore(dir))
        val result = repo.load(ChartPlatform.SPOTIFY)
        assertEquals(ChartResult.NoData, result)
    }

    @Test
    fun `empty remote parse is treated as failure, falls back to cache`() = runBlocking {
        val cache = ChartCacheStore(dir)
        val ok = ChartRepositoryImpl(remote = FakeRemote(entries), cache = cache)
        assertTrue(ok.load(ChartPlatform.SPOTIFY) is ChartResult.Fresh)

        // A "successful" HTTP 200 whose HTML parsed to nothing must NOT produce a Fresh
        // empty snapshot — that would look like an empty chart. It should fall back.
        val emptyHtml = ChartRepositoryImpl(remote = FakeRemote(emptyList()), cache = cache)
        val result = emptyHtml.load(ChartPlatform.SPOTIFY)
        assertTrue("expected FromCache (previous snapshot), got $result", result is ChartResult.FromCache)
        assertEquals(entries, (result as ChartResult.FromCache).snapshot.entries)
    }
}
