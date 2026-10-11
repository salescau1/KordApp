package com.chordapp.charts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Round-trip + corruption behavior of the file-backed snapshot cache.
 * (Robolectric provides a real org.json, which plain JVM unit tests stub out.)
 */
@RunWith(RobolectricTestRunner::class)
class ChartCacheStoreTest {

    private lateinit var cache: ChartCacheStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "chart_cache_test_${System.nanoTime()}")
        cache = ChartCacheStore(dir)
    }

    @Test
    fun `write then read preserves snapshot`() {
        val snapshot = ChartSnapshot(
            entries = listOf(
                ChartEntry(1, "Kirana", "Dewa 19"),
                ChartEntry(2, "Hati-Hati di Jalan", "Tulus & Friends")
            ),
            fetchedAtEpochMs = 1_700_000_000_123L
        )
        cache.write(ChartPlatform.SPOTIFY, snapshot)

        val back = cache.read(ChartPlatform.SPOTIFY)
        assertEquals(snapshot.entries, back?.entries)
        assertEquals(snapshot.fetchedAtEpochMs, back?.fetchedAtEpochMs)
    }

    @Test
    fun `missing file reads null`() {
        assertNull(cache.read(ChartPlatform.SPOTIFY))
    }

    @Test
    fun `corrupt file reads null instead of crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(File(context.cacheDir, "chart_cache_test_${System.nanoTime()}"), ChartPlatform.SPOTIFY.cacheFile)
        file.parentFile?.mkdirs()
        file.writeText("{ this is not valid json !!!")
        val corrupt = ChartCacheStore(file.parentFile!!)

        assertNull(corrupt.read(ChartPlatform.SPOTIFY))
    }

    @Test
    fun `writeResolved then readResolved preserves rows`() {
        val rows = listOf(
            com.chordapp.viewmodels.ChartRowUi(
                rank = 1,
                title = "Kirana",
                artist = "Dewa 19",
                available = true,
                artistSlug = "dewa-19",
                titleSlug = "kirana",
                sources = listOf("UG", "CI"),
                favoriteSourceUrl = "https://example.com/kirana",
                isFavorite = false
            )
        )
        cache.writeResolved(ChartPlatform.SPOTIFY, rows)
        val back = cache.readResolved(ChartPlatform.SPOTIFY)
        assertEquals(rows, back)
    }
}
