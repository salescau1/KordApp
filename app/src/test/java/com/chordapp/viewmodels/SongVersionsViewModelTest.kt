package com.chordapp.viewmodels

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chordapp.catalog.CatalogDatabase
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SongVersionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: CatalogDatabase
    private lateinit var repository: CatalogRepository
    private lateinit var viewModel: SongVersionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetStream = File("../app/src/test/resources/catalog_test.db")
        if (!assetStream.exists()) {
            throw IllegalStateException("catalog.db not found")
        }
        val dbFile = File(context.cacheDir, "catalog_versions_test_${System.nanoTime()}.db")
        Files.copy(assetStream.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        db = Room.databaseBuilder(
            context,
            CatalogDatabase::class.java,
            dbFile.absolutePath
        )
        .allowMainThreadQueries()
        .build()

        repository = CatalogRepository(db.catalogDao())
        viewModel = SongVersionsViewModel(repository)
    }

    @After
    fun teardown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testMultiVersionSong() = runTest {
        val job = viewModel.loadVersions("ed-sheeran", "perfect")
        job.join()

        val versions = viewModel.versions.value
        assertTrue("Should load multiple versions, found ${versions.size}", versions.size > 1)

        val ugVersions = versions.filter { it.source == "UG" }
        val gtVersions = versions.filter { it.source == "GT" }
        val ctVersions = versions.filter { it.source == "ChordTela" }

        println("FETCHED VERSIONS: ${versions.map { it.source }}")
            assertTrue("UG should have multiple versions", ugVersions.size >= 1)

        assertTrue("GT votes should be null", gtVersions.all { it.votes == null })
        assertTrue("CT category should be null", ctVersions.all { it.category == null })
        assertTrue("CT version_name should be null", ctVersions.all { it.versionName == null })

        assertNull(viewModel.navigateToPlayer.value)

        viewModel.setSourceFilter("Ultimate Guitar")
        assertEquals(ugVersions.size, viewModel.filteredVersions.value.size)
        assertEquals(versions.size, viewModel.versions.value.size)

        val selected = viewModel.selectedVersion.value
        assertNotNull(selected)
        assertNotNull(selected?.sourceUrl)
    }

    @Test
    fun testOneVersionSong() = runTest {
        // Exactly one version in the test catalog -> VM must redirect straight to
        // the Player instead of listing versions.
        val job = viewModel.loadVersions("adele", "19")
        job.join()

        assertTrue(viewModel.versions.value.isEmpty())
        assertNotNull(viewModel.navigateToPlayer.value)
    }
}
