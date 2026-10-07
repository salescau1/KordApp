package com.chordapp.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chordapp.catalog.CatalogDatabase
import com.chordapp.userdata.SetlistItemEntity
import com.chordapp.userdata.UserdataDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(RobolectricTestRunner::class)
class UserdataRepositoryTest {
    private lateinit var catalogDb: CatalogDatabase
    private lateinit var userDb: UserdataDatabase
    private lateinit var repository: UserdataRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Catalog DB
        val assetStream = File("../app/src/test/resources/catalog_test.db")
        val catFile = File(context.cacheDir, "catalog_user_test.db")
        Files.copy(assetStream.toPath(), catFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        catalogDb = Room.databaseBuilder(context, CatalogDatabase::class.java, catFile.absolutePath)
            .allowMainThreadQueries().build()

        // Userdata DB (in-memory for test)
        userDb = Room.inMemoryDatabaseBuilder(context, UserdataDatabase::class.java)
            .allowMainThreadQueries().build()

        repository = UserdataRepository(userDb.userdataDao(), catalogDb.catalogDao())
    }

    @After
    fun teardown() {
        catalogDb.close()
        userDb.close()
    }

    @Test
    fun flatSetlistCrud() = runBlocking {
        repository.createSetlist("My Gig")
        var lists = repository.getAllSetlists().first()
        assertEquals(1, lists.size)
        assertEquals("My Gig", lists.single().name)

        repository.renameSetlist(lists.single(), "My New Gig")
        lists = repository.getAllSetlists().first()
        assertEquals("My New Gig", lists.single().name)

        repository.deleteSetlist(lists.single())
        assertTrue(repository.getAllSetlists().first().isEmpty())
    }

    @Test
    fun createSetlistReturnsIdAndExactDuplicatesAreRejected() = runBlocking {
        val setlistId = repository.createSetlist("Player Picks")
        assertTrue(setlistId > 0)

        val catalogSong = catalogDb.catalogDao().searchSongs("perfect").first().let {
            catalogDb.catalogDao().getSongVersions(it.artistSlug, it.titleSlug).first()
        }

        assertEquals(
            SetlistAddResult.Added,
            repository.addSongToSetlist(setlistId, catalogSong, transpose = 2, capo = 0, note = null)
        )
        assertEquals(
            SetlistAddResult.AlreadyPresent,
            repository.addSongToSetlist(setlistId, catalogSong, transpose = 2, capo = 0, note = null)
        )
        assertEquals(1, repository.getSetlistItems(setlistId).first().size)

        assertEquals(
            SetlistAddResult.Added,
            repository.addSongToSetlist(setlistId, catalogSong, transpose = 3, capo = 0, note = null)
        )
        val items = repository.getSetlistItems(setlistId).first()
        assertEquals(2, items.size)
        assertEquals(listOf(0, 1), items.map { it.position })
        assertEquals(listOf(2, 3), items.map { it.transpose })
    }

    @Test
    fun testTracklistAndResolution() = runBlocking {
        repository.createSetlist("Tracklist Test")
        val setlist = repository.getAllSetlists().first().first()

        // Add Song
        val catalogSong = catalogDb.catalogDao().searchSongs("perfect").first().let { 
            catalogDb.catalogDao().getSongVersions(it.artistSlug, it.titleSlug).first()
        }

        repository.addSongToSetlist(setlist.id, catalogSong, transpose = 2, capo = 1, note = "Solo")

        var items = repository.getSetlistItems(setlist.id).first()
        assertEquals(1, items.size)
        assertEquals(0, items[0].position)
        assertEquals(2, items[0].transpose)
        assertEquals(1, items[0].capo)

        // Add another
        val catalogSong2 = catalogDb.catalogDao().searchSongs("fire").first().let { 
            catalogDb.catalogDao().getSongVersions(it.artistSlug, it.titleSlug).first()
        }
        repository.addSongToSetlist(setlist.id, catalogSong2, transpose = 0, capo = 0, note = null)

        items = repository.getSetlistItems(setlist.id).first()
        assertEquals(2, items.size)
        assertEquals(0, items[0].position)
        assertEquals(1, items[1].position)

        // Reorder
        val reversed = listOf(items[1], items[0])
        repository.reorderSetlistItems(reversed)

        items = repository.getSetlistItems(setlist.id).first()
        assertEquals(catalogSong2.sourceUrl, items[0].sourceUrl)
        assertEquals(catalogSong.sourceUrl, items[1].sourceUrl)

        // Resolution
        val resolution = repository.resolveSetlistItem(items[1])
        assertTrue(resolution is ResolutionResult.Success)
        val success = resolution as ResolutionResult.Success
        assertFalse(success.substituted) // Direct match
        assertEquals(catalogSong.id, success.song.id)

        // Delete item
        repository.removeSongFromSetlist(items[0])
        items = repository.getSetlistItems(setlist.id).first()
        assertEquals(1, items.size)
    }

    @Test
    fun deletingSetlistCascadesItsItems() = runBlocking {
        val id = repository.createSetlist("Delete Me")
        val song = catalogDb.catalogDao().searchSongs("perfect").first().let {
            catalogDb.catalogDao().getSongVersions(it.artistSlug, it.titleSlug).first()
        }
        repository.addSongToSetlist(id, song, transpose = 0, capo = 0, note = null)
        assertEquals(1, repository.getSetlistItems(id).first().size)

        repository.deleteSetlist(repository.getSetlist(id)!!)
        assertTrue(userDb.userdataDao().getSetlistItemsSync(id).isEmpty())
    }

    @Test
    fun favoriteTogglePersistsPlayerStateAndRejectsDuplicates() = runBlocking {
        val song = catalogDb.catalogDao().searchSongs("perfect").first().let {
            catalogDb.catalogDao().getSongVersions(it.artistSlug, it.titleSlug).first()
        }

        assertTrue(repository.toggleFavorite(song, transpose = 3, capo = 2))
        var favorites = repository.getFavoriteSongs().first()
        assertEquals(1, favorites.size)
        assertEquals(song.sourceUrl, favorites.single().sourceUrl)
        assertEquals(3, favorites.single().transpose)
        assertEquals(2, favorites.single().capo)
        assertTrue(repository.observeIsFavorite(song.sourceUrl).first())

        assertFalse(repository.addFavorite(song, transpose = 0, capo = 0))
        assertEquals(1, repository.getFavoriteSongs().first().size)

        val resolution = repository.resolveFavorite(favorites.single())
        assertTrue(resolution is ResolutionResult.Success)
        assertFalse((resolution as ResolutionResult.Success).substituted)

        assertFalse(repository.toggleFavorite(song, transpose = 3, capo = 2))
        favorites = repository.getFavoriteSongs().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun testRecentlyPlayedOrderAndDedupe() = runBlocking {
        val songA = catalogDb.catalogDao().searchSongs("perfect").first().let {
            catalogDb.catalogDao().getSongVersions(it.artistSlug, it.titleSlug).first()
        }
        val songB = catalogDb.catalogDao().searchSongs("fire").first().let {
            catalogDb.catalogDao().getSongVersions(it.artistSlug, it.titleSlug).first()
        }
        assertNotEquals(songA.sourceUrl, songB.sourceUrl)

        // Play B, then A, then replay B. History must be newest-first and B deduped
        // to a single row (its timestamp bumped), not duplicated.
        repository.recordPlay(songB)
        Thread.sleep(5)
        repository.recordPlay(songA)
        Thread.sleep(5)
        repository.recordPlay(songB)

        val history = repository.getRecentlyPlayed().first()
        assertEquals(2, history.size)
        assertEquals(songB.sourceUrl, history[0].sourceUrl)
        assertEquals(songA.sourceUrl, history[1].sourceUrl)

        // LIMIT is honored.
        val limited = repository.getRecentlyPlayed(limit = 1).first()
        assertEquals(1, limited.size)
        assertEquals(songB.sourceUrl, limited[0].sourceUrl)
    }
}
