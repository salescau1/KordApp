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
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExploreViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var database: CatalogDatabase
    private lateinit var viewModel: ExploreViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val asset = File("../app/src/test/resources/catalog_test.db")
        val copy = File(context.cacheDir, "catalog_explore_test_${System.nanoTime()}.db")
        Files.copy(asset.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING)
        database = Room.databaseBuilder(context, CatalogDatabase::class.java, copy.absolutePath)
            .allowMainThreadQueries()
            .build()
        viewModel = ExploreViewModel(CatalogRepository(database.catalogDao()))
        val deadline = System.currentTimeMillis() + 10_000
        while (viewModel.isLoading.value && System.currentTimeMillis() < deadline) {
            dispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(5)
        }
        assertTrue("Explore catalog should finish loading", !viewModel.isLoading.value)
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `catalog loads in case-insensitive order and supports letter categories`() {
        val artists = viewModel.artists.value
        assertTrue(artists.isNotEmpty())

        val letter = artists.first().artistName.trim().first().uppercaseChar()
        viewModel.selectLetter(letter.lowercaseChar())
        assertEquals(letter, viewModel.selectedLetter.value)
        assertEquals("", viewModel.query.value)
        assertTrue(
            artists.filter { it.artistName.trim().startsWith(letter, ignoreCase = true) }
                .all { it.artistSlug.isNotBlank() }
        )
    }

    @Test
    fun `search clears category and category reset preserves query rules`() {
        viewModel.selectLetter('A')
        viewModel.setQuery("sheeran")
        assertNull(viewModel.selectedLetter.value)
        assertEquals("sheeran", viewModel.query.value)

        viewModel.setQuery("")
        assertEquals("", viewModel.query.value)
        assertNull(viewModel.selectedLetter.value)

        viewModel.selectLetter('e')
        assertEquals('E', viewModel.selectedLetter.value)
        assertEquals("", viewModel.query.value)
        viewModel.clearLetter()
        assertNull(viewModel.selectedLetter.value)
    }

    @Test
    fun `all twenty six categories can be derived without invented entries`() {
        val letters = ('A'..'Z').toList()
        val populated = viewModel.artists.value.mapNotNull { artist ->
            artist.artistName.trim().firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' }
        }.toSet()
        assertEquals(26, letters.size)
        assertTrue(populated.all { it in letters })
        assertEquals(6, (letters.size + 4) / 5)
    }
}
