package com.chordapp.viewmodels

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chordapp.catalog.CatalogDatabase
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.charts.ChartEntry
import com.chordapp.charts.ChartPlatform
import com.chordapp.charts.ChartRepository
import com.chordapp.charts.ChartResult
import com.chordapp.charts.ChartSnapshot
import com.chordapp.repositories.CatalogRepository
import com.chordapp.repositories.UserdataRepository
import com.chordapp.userdata.PlayHistoryEntity
import com.chordapp.userdata.UserdataDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * HomeViewModel: chart rows resolve against the real catalog (exact artist/title),
 * so "chords available" is truthful; unavailable chart entries stay visible but not
 * playable; recent plays resolve and never fabricate. ChartRepository is faked;
 * catalog + userdata DBs are real (the asset catalog, matching production).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var catalogDb: CatalogDatabase
    private lateinit var userDb: UserdataDatabase
    private lateinit var catalogRepository: CatalogRepository
    private lateinit var userRepository: UserdataRepository

    @Before
    fun setup() {
        HomeViewModel.resetTestCache()
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()

        val assetStream = File("../app/src/test/resources/catalog_test.db")
        if (!assetStream.exists()) throw IllegalStateException("catalog.db not found")
        val catFile = File(context.cacheDir, "catalog_home_test_${System.nanoTime()}.db")
        Files.copy(assetStream.toPath(), catFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        catalogDb = Room.databaseBuilder(context, CatalogDatabase::class.java, catFile.absolutePath)
            .allowMainThreadQueries().build()
        userDb = Room.inMemoryDatabaseBuilder(context, UserdataDatabase::class.java)
            .allowMainThreadQueries().build()

        catalogRepository = CatalogRepository(catalogDb.catalogDao())
        userRepository = UserdataRepository(userDb.userdataDao(), catalogDb.catalogDao())
    }

    @After
    fun teardown() {
        catalogDb.close()
        userDb.close()
        HomeViewModel.resetTestCache()
        Dispatchers.resetMain()
    }

    private class FakeChartRepository(
        private val result: ChartResult,
        private val cached: Map<ChartPlatform, ChartSnapshot> = emptyMap()
    ) : ChartRepository {
        override fun cached(platform: ChartPlatform): ChartSnapshot? = cached[platform]
        override suspend fun load(platform: ChartPlatform): ChartResult = result
    }

    private class DeferredChartRepository(
        private val cachedSnapshots: Map<ChartPlatform, ChartSnapshot>,
        private val deferredResults: Map<ChartPlatform, CompletableDeferred<ChartResult>>
    ) : ChartRepository {
        override fun cached(platform: ChartPlatform): ChartSnapshot? = cachedSnapshots[platform]

        override suspend fun load(platform: ChartPlatform): ChartResult =
            deferredResults.getValue(platform).await()
    }

    private class FakePlatformStore(initial: ChartPlatform) : com.chordapp.charts.ChartPlatformStore {
        var stored = initial
        override fun read(): ChartPlatform = stored
        override fun write(platform: ChartPlatform) { stored = platform }
    }

    private fun viewModel(
        result: ChartResult,
        cached: Map<ChartPlatform, ChartSnapshot> = emptyMap(),
        store: com.chordapp.charts.ChartPlatformStore = com.chordapp.charts.ChartPlatformStore.Default
    ): HomeViewModel = HomeViewModel(
        userRepository,
        catalogRepository,
        FakeChartRepository(result, cached),
        store,
        catalogSizeBytes = { 297_267_200L }
    )

    private fun viewModel(
        repository: ChartRepository,
        store: com.chordapp.charts.ChartPlatformStore = com.chordapp.charts.ChartPlatformStore.Default
    ): HomeViewModel = HomeViewModel(
        userRepository,
        catalogRepository,
        repository,
        store,
        catalogSizeBytes = { 297_267_200L }
    )

    private fun pumpUntil(message: String, predicate: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end) {
            dispatcher.scheduler.advanceUntilIdle()
            if (predicate()) return
            Thread.sleep(5)
        }
        fail("Timed out: $message")
    }

    @Test
    fun `last platform is restored and a new selection is persisted`() {
        val store = FakePlatformStore(ChartPlatform.YOUTUBE)
        val vm = viewModel(ChartResult.NoData, store = store)
        assertEquals(ChartPlatform.YOUTUBE, vm.platform.value)

        vm.selectPlatform(ChartPlatform.SPOTIFY)
        assertEquals(ChartPlatform.SPOTIFY, vm.platform.value)
        assertEquals(ChartPlatform.SPOTIFY, store.stored)
    }

    @Test
    fun `cached platform renders before a failed refresh and remains visible`() {
        val snapshot = ChartSnapshot(
            entries = listOf(ChartEntry(1, "Cached Song", "Cached Artist")),
            fetchedAtEpochMs = 1_600_000_000_000L
        )
        val vm = viewModel(
            result = ChartResult.NoData,
            cached = mapOf(ChartPlatform.SPOTIFY to snapshot)
        )

        pumpUntil("cached chart should render") { vm.chart.value is ChartState.Loaded }
        val state = vm.chart.value as ChartState.Loaded
        assertTrue(state.fromCache)
        assertEquals("Cached Song", state.rows.single().title)
    }

    @Test
    fun `cached rows remain visible while refresh is pending`() {
        val cached = ChartSnapshot(
            entries = listOf(ChartEntry(1, "Cached Song", "Cached Artist")),
            fetchedAtEpochMs = 1_600_000_000_000L
        )
        val refresh = CompletableDeferred<ChartResult>()
        val vm = viewModel(
            DeferredChartRepository(
                cachedSnapshots = mapOf(ChartPlatform.SPOTIFY to cached),
                deferredResults = mapOf(ChartPlatform.SPOTIFY to refresh)
            )
        )

        pumpUntil("cached rows should render before refresh completes") {
            vm.chart.value is ChartState.Loaded
        }

        vm.refresh()

        val state = vm.chart.value as ChartState.Loaded
        assertEquals("Cached Song", state.rows.single().title)
        assertTrue(state.fromCache)
        assertTrue(state.isRefreshing)
        assertFalse(vm.chart.value is ChartState.Loading)

        refresh.complete(ChartResult.Fresh(cached.copy(entries = listOf(ChartEntry(1, "Fresh Song", "Fresh Artist")))))
        pumpUntil("fresh chart should replace cached rows") {
            (vm.chart.value as? ChartState.Loaded)?.rows?.single()?.title == "Fresh Song"
        }

        val fresh = vm.chart.value as ChartState.Loaded
        assertEquals("Fresh Song", fresh.rows.single().title)
        assertFalse(fresh.fromCache)
        assertFalse(fresh.isRefreshing)
    }

    @Test
    fun `failed refresh keeps cached rows and ends background refresh`() {
        val cached = ChartSnapshot(
            entries = listOf(ChartEntry(1, "Cached Song", "Cached Artist")),
            fetchedAtEpochMs = 1_600_000_000_000L
        )
        val refresh = CompletableDeferred<ChartResult>()
        val vm = viewModel(
            DeferredChartRepository(
                cachedSnapshots = mapOf(ChartPlatform.SPOTIFY to cached),
                deferredResults = mapOf(ChartPlatform.SPOTIFY to refresh)
            )
        )

        pumpUntil("cached rows should render before failed refresh completes") {
            vm.chart.value is ChartState.Loaded
        }
        vm.refresh()
        refresh.complete(ChartResult.NoData)
        pumpUntil("cached chart should finish refreshing") {
            (vm.chart.value as? ChartState.Loaded)?.isRefreshing == false
        }

        val state = vm.chart.value as ChartState.Loaded
        assertEquals("Cached Song", state.rows.single().title)
        assertTrue(state.fromCache)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `fresh chart row that exists locally is available and playable`() {
        // A song we know exists in the shipped catalog, with its canonical displayed name.
        val known = runBlocking { catalogRepository.findSongByExact("Ed Sheeran", "Perfect") }
        assertNotNull("Ed Sheeran / Perfect should exist in catalog.db", known)

        val vm = viewModel(
            ChartResult.Fresh(
                ChartSnapshot(
                    entries = listOf(
                        ChartEntry(1, known!!.title, known.artistName),
                        ChartEntry(2, "A Song That Is Definitely Not In The Catalog", "Zz Nobody")
                    ),
                    fetchedAtEpochMs = 1_700_000_000_000L
                )
            )
        )

        pumpUntil("chart should leave Loading") { vm.chart.value !is ChartState.Loading }

        val state = vm.chart.value as ChartState.Loaded
        assertFalse("fresh load must not be flagged fromCache", state.fromCache)
        assertEquals(2, state.rows.size)

        val local = state.rows[0]
        assertTrue(local.available)
        assertEquals(known.artistSlug, local.artistSlug)
        assertEquals(known.titleSlug, local.titleSlug)
        assertTrue("local song should expose its real sources", local.sources.isNotEmpty())
        val expectedVersion = runBlocking {
            catalogRepository.getSongVersions(known.artistSlug, known.titleSlug).first()
        }
        assertEquals(expectedVersion.sourceUrl, local.favoriteSourceUrl)
        assertFalse(local.isFavorite)
        assertEquals(1, local.rank)

        val missing = state.rows[1]
        assertFalse(missing.available)
        assertNull(missing.artistSlug)
        assertNull(missing.titleSlug)
        assertNull(missing.favoriteSourceUrl)
        assertFalse(missing.isFavorite)
        assertTrue(missing.sources.isEmpty())
    }

    @Test
    fun `chart favorite toggles deterministic source url and updates row`() {
        val known = runBlocking { catalogRepository.findSongByExact("Ed Sheeran", "Perfect") }!!
        val expectedVersion = runBlocking {
            catalogRepository.getSongVersions(known.artistSlug, known.titleSlug).first()
        }
        val vm = viewModel(
            ChartResult.Fresh(
                ChartSnapshot(
                    entries = listOf(ChartEntry(1, known.title, known.artistName)),
                    fetchedAtEpochMs = 1_700_000_000_000L
                )
            )
        )
        pumpUntil("chart should load") { vm.chart.value is ChartState.Loaded }

        val row = (vm.chart.value as ChartState.Loaded).rows.single()
        assertEquals(expectedVersion.sourceUrl, row.favoriteSourceUrl)
        vm.toggleFavorite(row)
        pumpUntil("favorite should update chart row") {
            (vm.chart.value as? ChartState.Loaded)?.rows?.single()?.isFavorite == true
        }

        val stored = runBlocking { userRepository.getFavoriteSongs().first() }
        assertEquals(expectedVersion.sourceUrl, stored.single().sourceUrl)
        assertEquals(0, stored.single().transpose)
        assertEquals(0, stored.single().capo)
    }

    @Test
    fun `unavailable chart favorite toggle is a no-op`() {
        val vm = viewModel(
            ChartResult.Fresh(
                ChartSnapshot(
                    entries = listOf(ChartEntry(1, "Missing Song", "Missing Artist")),
                    fetchedAtEpochMs = 1_700_000_000_000L
                )
            )
        )
        pumpUntil("chart should load") { vm.chart.value is ChartState.Loaded }

        vm.toggleFavorite((vm.chart.value as ChartState.Loaded).rows.single())
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(runBlocking { userRepository.getFavoriteSongs().first() }.isEmpty())
    }

    @Test
    fun `cache-backed load reports fromCache`() {
        val vm = viewModel(
            ChartResult.FromCache(
                ChartSnapshot(
                    entries = listOf(ChartEntry(1, "A Song That Is Definitely Not In The Catalog", "Zz Cache Artist")),
                    fetchedAtEpochMs = 1_600_000_000_000L
                )
            )
        )
        pumpUntil("chart should leave Loading") { vm.chart.value !is ChartState.Loading }

        val state = vm.chart.value as ChartState.Loaded
        assertTrue("offline fallback must be flagged", state.fromCache)
        assertEquals(1, state.rows.size)
        assertFalse(state.rows[0].available)
    }

    @Test
    fun `no data (offline and no cache) shows truthful NoData`() {
        val vm = viewModel(ChartResult.NoData)
        pumpUntil("chart should settle to NoData") { vm.chart.value is ChartState.NoData }
    }

    @Test
    fun `catalog stats use runtime repository counts and size provider`() {
        val vm = viewModel(ChartResult.NoData)

        pumpUntil("catalog stats should load") { vm.catalogStats.value != null }
        val stats = vm.catalogStats.value!!
        assertEquals(runBlocking { catalogRepository.getUniqueArtistCount() }, stats.artists)
        assertEquals(runBlocking { catalogRepository.getUniqueSongCount() }, stats.songs)
        assertEquals(297_267_200L, stats.catalogBytes)
        assertTrue(stats.artists > 0)
        assertTrue(stats.songs > 0)
    }

    @Test
    fun `recent history exposes only five newest resolved rows`() {
        val knownVersions = runBlocking {
            listOf(
                catalogRepository.getSongVersions("ed-sheeran", "perfect").first(),
                catalogRepository.getSongVersions("adele", "hello").first(),
                catalogRepository.getSongVersions("coldplay", "yellow").first(),
                catalogRepository.getSongVersions("radiohead", "creep").first(),
                catalogRepository.getSongVersions("oasis", "wonderwall").first(),
                catalogRepository.getSongVersions("ed-sheeran", "photograph").first()
            )
        }
        runBlocking {
            knownVersions.forEachIndexed { index, song ->
                userDb.userdataDao().recordPlay(
                    PlayHistoryEntity(
                        sourceUrl = song.sourceUrl,
                        source = song.source,
                        artist = song.artist,
                        artistSlug = song.artistSlug,
                        title = song.title,
                        titleSlug = song.titleSlug,
                        playedAt = index.toLong()
                    )
                )
            }
        }

        val vm = viewModel(ChartResult.NoData)
        pumpUntil("five recent rows should resolve") { vm.recent.value.size == 5 }

        assertEquals(knownVersions.drop(1).reversed().map { it.sourceUrl }.size, vm.recent.value.size)
        assertEquals(knownVersions.last().titleSlug, vm.recent.value.first().titleSlug)
        assertEquals(knownVersions[1].titleSlug, vm.recent.value.last().titleSlug)
        assertTrue(vm.recent.value.all { it.available })
        assertFalse(vm.recent.value.any { it.titleSlug == knownVersions.first().titleSlug })
    }

    @Test
    fun `delayed previous platform cannot overwrite current platform`() {
        val spotifyRefresh = CompletableDeferred<ChartResult>()
        val youtubeRefresh = CompletableDeferred<ChartResult>()
        val vm = viewModel(
            DeferredChartRepository(
                cachedSnapshots = emptyMap(),
                deferredResults = mapOf(
                    ChartPlatform.SPOTIFY to spotifyRefresh,
                    ChartPlatform.YOUTUBE to youtubeRefresh
                )
            )
        )

        vm.selectPlatform(ChartPlatform.YOUTUBE)
        youtubeRefresh.complete(
            ChartResult.Fresh(
                ChartSnapshot(listOf(ChartEntry(1, "YouTube Song", "Artist")), 2L)
            )
        )
        pumpUntil("YouTube result should render") {
            (vm.chart.value as? ChartState.Loaded)?.rows?.single()?.title == "YouTube Song"
        }

        spotifyRefresh.complete(
            ChartResult.Fresh(
                ChartSnapshot(listOf(ChartEntry(1, "Late Spotify Song", "Artist")), 3L)
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChartPlatform.YOUTUBE, vm.platform.value)
        assertEquals("YouTube Song", (vm.chart.value as ChartState.Loaded).rows.single().title)
    }

    @Test
    fun `recent play that still exists resolves to a playable row`() {
        runBlocking {
            val version = catalogRepository.getSongVersions("ed-sheeran", "perfect").first()
            userRepository.recordPlay(version)
        }

        val vm = viewModel(ChartResult.NoData)
        pumpUntil("recent should show the recorded play") { vm.recent.value.size == 1 }

        val recent = vm.recent.value.first()
        assertTrue(recent.available)
        assertEquals("ed-sheeran", recent.artistSlug)
        assertEquals("perfect", recent.titleSlug)
    }

    @Test
    fun `recent play whose song left the catalog stays visible but unavailable`() {
        runBlocking {
            userRepository.recordPlay(
                CatalogSongEntity(
                    id = null,
                    source = "UG",
                    sourceUrl = "https://tabs.ultimate-guitar.com/tab/zz-removed-song-9999999",
                    title = "Gone Song",
                    artist = "Ghost Artist",
                    titleSlug = "zz-gone-song",
                    artistSlug = "zz-ghost-artist",
                    versionName = null,
                    rating = null,
                    votes = null,
                    keyBase = null,
                    difficulty = null,
                    category = null,
                    region = "global",
                    chords = "",
                    chordCount = 0,
                    lineCount = 0,
                    content = "",
                    contentHash = "zz",
                    scrapedAt = "2020-01-01"
                )
            )
        }

        val vm = viewModel(ChartResult.NoData)
        pumpUntil("recent should show the recorded play") { vm.recent.value.size == 1 }

        val recent = vm.recent.value.first()
        assertFalse("row must not be playable when catalog no longer has it", recent.available)
        assertEquals("Gone Song", recent.title)
        assertEquals("Ghost Artist", recent.artist)
    }
}
