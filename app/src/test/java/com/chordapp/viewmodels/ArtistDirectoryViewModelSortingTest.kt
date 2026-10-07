package com.chordapp.viewmodels

import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogDao
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDirectoryViewModelSortingTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSong(
        title: String,
        titleSlug: String,
        votes: Int?,
        rating: Float?
    ) = CatalogSongEntity(
        id = 1L,
        source = "UG",
        sourceUrl = "https://example.com/$titleSlug",
        title = title,
        artist = "Test Artist",
        titleSlug = titleSlug,
        artistSlug = "test-artist",
        versionName = null,
        rating = rating,
        votes = votes,
        keyBase = "C",
        difficulty = "novice",
        content = "Test content",
        chordCount = 4,
        lineCount = 20,
        category = null,
        region = "EN"
    )

    private val fakeDao = object : CatalogDao {
        override suspend fun getArtistSongs(artistSlug: String): List<CatalogSongGroup> = emptyList()

        override suspend fun getArtistSongsRaw(artistSlug: String): List<CatalogSongEntity> {
            return listOf(
                createSong("Yellow", "yellow", votes = 500, rating = 4.8f),
                createSong("A Song", "a-song", votes = 10, rating = 3.5f),
                createSong("Best Hit", "best-hit", votes = 9999, rating = 4.9f),
                createSong("Middle Song", "middle-song", votes = 2000, rating = 4.7f)
            )
        }

        override suspend fun getAllArtists(): List<CatalogArtistInfo> = emptyList()
        override suspend fun getArtistCount(): Int = 0
        override suspend fun getUniqueSongCount(): Int = 0
        override suspend fun getArtists(limit: Int, offset: Int): List<CatalogArtistInfo> = emptyList()
        override suspend fun searchArtists(query: String, limit: Int): List<CatalogArtistInfo> = emptyList()
        override suspend fun searchSongs(query: String, limit: Int): List<CatalogSongGroup> = emptyList()
        override suspend fun getArtistInfo(artistSlug: String): CatalogSongEntity? = createSong("Best Hit", "best-hit", 9999, 4.9f)
        override suspend fun getSongsForArtistAndTitle(artistSlug: String, titleSlug: String): List<CatalogSongEntity> = emptyList()
        override suspend fun getSongBySourceUrl(sourceUrl: String): CatalogSongEntity? = null
        override suspend fun getSongBySlug(artistSlug: String, titleSlug: String): CatalogSongEntity? = null
    }

    @Test
    fun testAlphabeticalAndPopularSorting() = runTest(testDispatcher) {
        val repository = CatalogRepository(fakeDao)
        val viewModel = ArtistDirectoryViewModel(repository)

        viewModel.loadArtist("test-artist")
        testDispatcher.scheduler.advanceUntilIdle()

        // Default sort order is ALPHABETICAL
        assertEquals(ArtistSongSortOrder.ALPHABETICAL, viewModel.sortOrder.value)
        val alphaTitles = viewModel.songs.value.keys.map { it.title }
        assertEquals(listOf("A Song", "Best Hit", "Middle Song", "Yellow"), alphaTitles)

        // Switch to POPULAR
        viewModel.setSortOrder(ArtistSongSortOrder.POPULAR)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ArtistSongSortOrder.POPULAR, viewModel.sortOrder.value)
        val popularTitles = viewModel.songs.value.keys.map { it.title }
        assertEquals(listOf("Best Hit", "Middle Song", "Yellow", "A Song"), popularTitles)

        // Switch back to ALPHABETICAL
        viewModel.setSortOrder(ArtistSongSortOrder.ALPHABETICAL)
        testDispatcher.scheduler.advanceUntilIdle()

        val reAlphaTitles = viewModel.songs.value.keys.map { it.title }
        assertEquals(listOf("A Song", "Best Hit", "Middle Song", "Yellow"), reAlphaTitles)
    }
}
