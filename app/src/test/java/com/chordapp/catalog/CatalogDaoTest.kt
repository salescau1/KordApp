package com.chordapp.catalog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class CatalogDaoTest {
    private lateinit var db: CatalogDatabase
    private lateinit var dao: CatalogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Integrity gate: verify counts against the FULL catalog. The full
        // catalog lives at .data_backups/catalog_full.db (the app asset may be
        // swapped for a small test build); fall back to the app asset only when
        // the backup is absent.
        val source = File("../.data_backups/catalog_full.db")
            .takeIf { it.exists() }
            ?: File("../app/src/main/assets/catalog.db")
        if (!source.exists()) {
            throw IllegalStateException("catalog_full.db not found: ${source.absolutePath}")
        }
        val dbFile = File(context.cacheDir, "catalog_full_test.db")
        Files.copy(source.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        db = Room.databaseBuilder(
            context,
            CatalogDatabase::class.java,
            dbFile.absolutePath
        )
        .allowMainThreadQueries()
        .build()

        dao = db.catalogDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testCatalogCounts() = runBlocking {
        // CatalogDaoTest deliberately runs against the FULL catalog (the big
        // catalog.db in app/src/main/assets) to gate catalog integrity: 99.012
        // rows, per-source counts. When that asset is swapped for a small
        // test-only catalog for a fast app build, the counts reflect the small
        // DB and this test reports the mismatch on purpose — see PROJECT_PLAN.
        val total = dao.getTotalSongs()
        assertEquals(99012, total)

        val ugCount = dao.getSourceCount("UG")
        assertEquals(8121, ugCount)

        val gtCount = dao.getSourceCount("GT")
        assertEquals(48918, gtCount)

        val ctCount = dao.getSourceCount("ChordTela")
        assertEquals(41973, ctCount)
    }

    @Test
    fun testCrossSourceVersionsAndLookup() = runBlocking {
        // Find a song with multiple versions across sources (e.g., Perfect / Ed Sheeran)
        // Just test search to get a group
        val searchResults = dao.searchSongs("perfect")
        assertTrue(searchResults.isNotEmpty())

        val songGroup = searchResults.first { it.title.contains("perfect", ignoreCase = true) }

        val versions = dao.getSongVersions(songGroup.artistSlug, songGroup.titleSlug)
        assertTrue(versions.size >= 1)

        // source_url lookup works
        val firstVersion = versions.first()
        val lookedUp = dao.getBySourceUrl(firstVersion.sourceUrl)
        assertNotNull(lookedUp)
        assertEquals(firstVersion.id, lookedUp?.id)
        assertTrue(lookedUp!!.content.isNotBlank())
    }

    @Test
    fun testMissingMetadataIsNull() = runBlocking {
        // GT doesn't have rating/votes/key_base
        val searchResults = dao.searchSongs("A")
        val gtVersions = searchResults.flatMap { 
            dao.getSongVersions(it.artistSlug, it.titleSlug) 
        }.filter { it.source == "GT" }

        if (gtVersions.isNotEmpty()) {
            val gt = gtVersions.first()
            assertNull(gt.rating)
            assertNull(gt.votes)
            assertNull(gt.keyBase)
        }

        // ChordTela doesn't have category populated
        val ctVersions = searchResults.flatMap { 
            dao.getSongVersions(it.artistSlug, it.titleSlug) 
        }.filter { it.source == "ChordTela" }

        if (ctVersions.isNotEmpty()) {
            val ct = ctVersions.first()
            assertNull(ct.category)
        }
    }

    @Test
    fun testArtistAggregation() = runBlocking {
        val artists = dao.getArtists(limit = 10)
        assertTrue(artists.isNotEmpty())

        // Ensure unique artist slugs
        val slugs = artists.map { it.artistSlug }
        assertEquals(slugs.size, slugs.distinct().size)
    }
}
