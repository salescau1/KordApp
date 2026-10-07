package com.chordapp.viewmodels

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chordapp.catalog.CatalogDatabase
import com.chordapp.player.Transposer
import com.chordapp.repositories.CatalogRepository
import com.chordapp.repositories.UserdataRepository
import com.chordapp.settings.PlayerPreferences
import com.chordapp.userdata.UserdataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: CatalogDatabase
    private lateinit var userDb: UserdataDatabase
    private lateinit var repository: CatalogRepository
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetStream = File("../app/src/test/resources/catalog_test.db")
        if (!assetStream.exists()) {
            throw IllegalStateException("catalog.db not found")
        }
        val dbFile = File(context.cacheDir, "catalog_player_test_${System.nanoTime()}.db")
        Files.copy(assetStream.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        db = Room.databaseBuilder(
            context,
            CatalogDatabase::class.java,
            dbFile.absolutePath
        )
        .allowMainThreadQueries()
        .build()

        userDb = Room.inMemoryDatabaseBuilder(context, UserdataDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = CatalogRepository(db.catalogDao())
        viewModel = PlayerViewModel(repository, UserdataRepository(userDb.userdataDao(), db.catalogDao()))
    }

    @After
    fun teardown() {
        db.close()
        userDb.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testPlayerStateTransitions() = runTest {
        // Find a song dynamically
        val group = repository.searchSongs("perfect").first()
        val versions = repository.getSongVersions(group.artistSlug, group.titleSlug)
        val version1 = versions[0]
        val version2 = versions.getOrNull(1)

        // Note: Our ViewModel uses URLDecoder, but in the test we can just pass the raw sourceUrl
        // because URLDecoder.decode of a normal URL without % might just return the URL or we pass the hash/encoded.
        val encodedUrl = java.net.URLEncoder.encode(version1.sourceUrl, "UTF-8")

        val job = viewModel.loadSong(encodedUrl)
        job.join()

        assertNotNull(viewModel.song.value)
        assertNotNull(viewModel.sheet.value)
        assertTrue(viewModel.song.value!!.content.isNotBlank())
        assertEquals(
            com.chordapp.player.ChordParser.parse(version1.content),
            viewModel.sheet.value
        )

        // Transpose
        assertEquals(0, viewModel.transposeDelta.value)
        viewModel.transposeUp()
        assertEquals(1, viewModel.transposeDelta.value)
        viewModel.transposeDown()
        assertEquals(0, viewModel.transposeDelta.value)
        viewModel.transposeDown()
        assertEquals(-1, viewModel.transposeDelta.value)
        viewModel.resetTranspose()
        assertEquals(0, viewModel.transposeDelta.value)

        // Font Size
        val initialFont = viewModel.fontSize.value
        viewModel.increaseFontSize()
        assertTrue(viewModel.fontSize.value > initialFont)
        viewModel.decreaseFontSize()
        assertEquals(initialFont, viewModel.fontSize.value)

        // Modes
        assertFalse(viewModel.isPageFlip.value)
        assertEquals(0f, viewModel.autoScrollSpeed.value)

        viewModel.toggleAutoScroll()
        assertTrue(viewModel.autoScrollSpeed.value > 0f)

        // Page flip must stop auto scroll
        viewModel.togglePageFlip()
        assertTrue(viewModel.isPageFlip.value)
        assertEquals(0f, viewModel.autoScrollSpeed.value)

        // Version switching preserves state
        if (version2 != null) {
            viewModel.transposeUp()
            assertEquals(1, viewModel.transposeDelta.value)

            val switchJob = viewModel.switchVersion(version2.sourceUrl)
            switchJob.join()

            // Should have the new song but same transpose
            assertEquals(version2.id, viewModel.song.value?.id)
            assertEquals(1, viewModel.transposeDelta.value)
            assertTrue(viewModel.isPageFlip.value) // Mode preserved
        }
    }

    @Test
    fun addCurrentSongUsesTransposeAndRejectsExactDuplicate() = runTest {
        val setlistId = userDb.userdataDao().insertSetlist(
            com.chordapp.userdata.SetlistEntity(
                name = "Player Picks",
                note = null,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        val version = repository.searchSongs("perfect").first().let {
            repository.getSongVersions(it.artistSlug, it.titleSlug).first()
        }
        viewModel.loadSong(java.net.URLEncoder.encode(version.sourceUrl, "UTF-8")).join()
        viewModel.transposeUp()

        val firstEvent = async { viewModel.setlistEvents.first() }
        viewModel.addCurrentSongToSetlist(setlistId)
        advanceUntilIdle()
        assertTrue(firstEvent.await() is PlayerSetlistEvent.Added)

        var items = userDb.userdataDao().getSetlistItemsSync(setlistId)
        assertEquals(1, items.size)
        assertEquals(1, items.single().transpose)
        assertEquals(0, items.single().capo)

        val duplicateEvent = async { viewModel.setlistEvents.first() }
        viewModel.addCurrentSongToSetlist(setlistId)
        advanceUntilIdle()
        assertTrue(duplicateEvent.await() is PlayerSetlistEvent.AlreadyPresent)
        items = userDb.userdataDao().getSetlistItemsSync(setlistId)
        assertEquals(1, items.size)
    }

    @Test
    fun createSetlistAddsCurrentSong() = runTest {
        val version = repository.searchSongs("perfect").first().let {
            repository.getSongVersions(it.artistSlug, it.titleSlug).first()
        }
        viewModel.loadSong(java.net.URLEncoder.encode(version.sourceUrl, "UTF-8")).join()

        val event = async { viewModel.setlistEvents.first() }
        viewModel.createSetlistAndAddCurrentSong("  New List  ")
        advanceUntilIdle()
        assertEquals(PlayerSetlistEvent.Added("New List"), event.await())

        val lists = userDb.userdataDao().getAllSetlists().first()
        assertEquals(1, lists.size)
        assertEquals("New List", lists.single().name)
        val items = userDb.userdataDao().getSetlistItemsSync(lists.single().id)
        assertEquals(version.sourceUrl, items.single().sourceUrl)
    }

    @Test
    fun playerUsesAndPersistsFunctionalPreferences() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = PlayerPreferences(context).apply {
            keepScreenAwake = false
            defaultScrollSpeed = 2.5f
        }
        val preferredViewModel = PlayerViewModel(
            repository,
            UserdataRepository(userDb.userdataDao(), db.catalogDao()),
            preferences
        )

        assertFalse(preferredViewModel.keepScreenAwake.value)
        preferredViewModel.toggleKeepScreenAwake()
        assertTrue(preferredViewModel.keepScreenAwake.value)
        assertTrue(PlayerPreferences(context).keepScreenAwake)
        preferredViewModel.toggleAutoScroll()
        assertEquals(2.5f, preferredViewModel.autoScrollSpeed.value)

        preferredViewModel.setAutoScrollSpeed(3.5f)
        assertEquals(3.5f, PlayerPreferences(context).defaultScrollSpeed)
    }

    @Test
    fun favoriteToggleStoresCurrentTransposeAndCapo() = runTest {
        val version = repository.searchSongs("perfect").first().let {
            repository.getSongVersions(it.artistSlug, it.titleSlug).first()
        }
        viewModel.loadSong(java.net.URLEncoder.encode(version.sourceUrl, "UTF-8"), 2, 3).join()
        advanceUntilIdle()
        assertEquals(2, viewModel.transposeDelta.value)
        assertEquals(3, viewModel.capo.value)

        val added = async { viewModel.favoriteEvents.first() }
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertEquals(PlayerFavoriteEvent.Changed(true), added.await())
        val favorite = userDb.userdataDao().getFavoriteBySourceUrl(version.sourceUrl)
        assertNotNull(favorite)
        assertEquals(2, favorite!!.transpose)
        assertEquals(3, favorite.capo)

        val removed = async { viewModel.favoriteEvents.first() }
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertEquals(PlayerFavoriteEvent.Changed(false), removed.await())
        assertNull(userDb.userdataDao().getFavoriteBySourceUrl(version.sourceUrl))
    }

    @Test
    fun transposeClampsAtBounds() {
        repeat(12) { viewModel.transposeUp() }
        assertEquals(11, viewModel.transposeDelta.value)
        repeat(24) { viewModel.transposeDown() }
        assertEquals(-11, viewModel.transposeDelta.value)
        viewModel.resetTranspose()
        assertEquals(0, viewModel.transposeDelta.value)
    }

    @Test
    fun autoScrollSpeedClampsAndPauseResumeKeepsLastSpeed() {
        viewModel.toggleAutoScroll()
        assertEquals(1f, viewModel.autoScrollSpeed.value, 0f) // resumes the default last speed
        viewModel.setAutoScrollSpeed(10f)
        assertEquals(PlayerViewModel.MAX_AUTO_SCROLL_SPEED, viewModel.autoScrollSpeed.value, 0f)
        viewModel.toggleAutoScroll() // pause
        assertEquals(0f, viewModel.autoScrollSpeed.value, 0f)
        viewModel.toggleAutoScroll() // resume keeps the remembered cadence
        assertEquals(PlayerViewModel.MAX_AUTO_SCROLL_SPEED, viewModel.autoScrollSpeed.value, 0f)
        viewModel.setAutoScrollSpeed(0.05f)
        assertEquals(PlayerViewModel.MIN_AUTO_SCROLL_SPEED, viewModel.autoScrollSpeed.value, 0f)
    }

    @Test
    fun pageFlipAndAutoScrollAreMutuallyExclusive() {
        viewModel.toggleAutoScroll()
        viewModel.setAutoScrollSpeed(2.5f)
        assertEquals(2.5f, viewModel.autoScrollSpeed.value, 0f)

        viewModel.togglePageFlip()
        assertTrue(viewModel.isPageFlip.value)
        assertEquals(0f, viewModel.autoScrollSpeed.value, 0f)

        // Starting auto-scroll again must turn Page Flip off (mutual exclusion).
        viewModel.toggleAutoScroll()
        assertFalse(viewModel.isPageFlip.value)
        assertEquals(2.5f, viewModel.autoScrollSpeed.value, 0f)
    }

    @Test
    fun effectiveKeyFollowsTransposeForAKeyedSong() = runTest {
        val group = repository.searchSongs("perfect").first()
        val version = repository.getSongVersions(group.artistSlug, group.titleSlug)
            .firstOrNull { it.keyBase != null } ?: return@runTest
        val encoded = java.net.URLEncoder.encode(version.sourceUrl, "UTF-8")

        viewModel.loadSong(encoded).join()
        val base = version.keyBase!!
        assertEquals(base, viewModel.effectiveKey())

        viewModel.transposeUp()
        viewModel.transposeUp()
        assertEquals(Transposer.transpose(base, 2), viewModel.effectiveKey())
        viewModel.transposeDown()
        assertEquals(Transposer.transpose(base, 1), viewModel.effectiveKey())
        viewModel.resetTranspose()
        assertEquals(base, viewModel.effectiveKey())
    }
}
