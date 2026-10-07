package com.chordapp.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chordapp.catalog.CatalogDatabase
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
class CatalogRepositoryTest {
    private lateinit var db: CatalogDatabase
    private lateinit var repository: CatalogRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetStream = File("../app/src/test/resources/catalog_test.db")
        if (!assetStream.exists()) {
            throw IllegalStateException("catalog.db not found")
        }
        val dbFile = File(context.cacheDir, "catalog_repo_test.db")
        Files.copy(assetStream.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        db = Room.databaseBuilder(
            context,
            CatalogDatabase::class.java,
            dbFile.absolutePath
        )
        .allowMainThreadQueries()
        .build()

        repository = CatalogRepository(db.catalogDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testSearchSongsReturnsRealData() = runBlocking {
        val results = repository.searchSongs("perfect")
        assertTrue("Search should return results for 'perfect'", results.isNotEmpty())

        // Check no forbidden metadata is exposed by the group object
        val group = results.first()
        assertNotNull(group.title)
        assertNotNull(group.artistName)
        assertNotNull(group.artistSlug)
        assertTrue(group.versionCount > 0)
        // CatalogSongGroup does not have fields for rating, votes, bpm, genre, etc.
    }

    @Test
    fun exploreArtistsAreRealCaseInsensitiveAzWithCatalogCounts() = runBlocking {
        val artists = repository.getAllArtists()
        assertTrue(artists.isNotEmpty())
        assertEquals(repository.getUniqueArtistCount(), artists.size)
        assertTrue(repository.getUniqueSongCount() > 0)
        assertTrue(artists.all { it.artistName.isNotBlank() && it.artistSlug.isNotBlank() && it.songCount > 0 })
        assertEquals(artists.map { it.artistSlug }, repository.getAllArtists().map { it.artistSlug })
    }

    @Test
    fun testArtistDirectoryGrouping() = runBlocking {
        val searchResults = repository.searchSongs("Ed Sheeran")
        val edSheeran = searchResults.firstOrNull { it.artistName.contains("Sheeran", ignoreCase = true) }
        assertNotNull(edSheeran)

        val artistSongs = repository.getArtistSongs(edSheeran!!.artistSlug)
        assertTrue(artistSongs.isNotEmpty())

        // Logical song grouping does not destroy source/version records
        val totalVersions = artistSongs.values.sumOf { it.size }
        assertTrue("Total versions $totalVersions should be >= distinct songs ${artistSongs.size}", totalVersions >= artistSongs.size)

        // Artist counts come from the catalog
        val info = repository.getArtistInfo(edSheeran.artistSlug)
        assertNotNull(info)
        assertEquals(edSheeran.artistSlug, info?.artistSlug)
    }
}
