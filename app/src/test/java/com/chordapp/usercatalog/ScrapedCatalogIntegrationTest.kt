package com.chordapp.usercatalog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chordapp.catalog.CatalogDatabase
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.repositories.CatalogRepository
import com.chordapp.repositories.ResolutionResult
import com.chordapp.repositories.UserdataRepository
import com.chordapp.userdata.UserdataDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScrapedCatalogIntegrationTest {
    private lateinit var catalogDb: CatalogDatabase
    private lateinit var userCatalogDb: UserCatalogDatabase
    private lateinit var userdataDb: UserdataDatabase

    private lateinit var userCatalogRepo: UserCatalogRepository
    private lateinit var catalogRepo: CatalogRepository
    private lateinit var userdataRepo: UserdataRepository

    private val testScrapedSong = CatalogSongEntity(
        id = null,
        source = "chordtela",
        sourceUrl = "https://www.chordtela.com/2023/niki-high-school-in-jakarta",
        title = "High School in Jakarta",
        artist = "NIKI",
        titleSlug = "high-school-in-jakarta",
        artistSlug = "niki",
        versionName = null,
        rating = null,
        votes = null,
        keyBase = "C",
        difficulty = null,
        category = null,
        region = "ID",
        chords = "C Am F G",
        chordCount = 4,
        lineCount = 10,
        content = "[C] High school in [Am] Jakarta",
        contentHash = "hash123",
        scrapedAt = "2026-10-23"
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        catalogDb = Room.inMemoryDatabaseBuilder(context, CatalogDatabase::class.java)
            .allowMainThreadQueries().build()
        userCatalogDb = Room.inMemoryDatabaseBuilder(context, UserCatalogDatabase::class.java)
            .allowMainThreadQueries().build()
        userdataDb = Room.inMemoryDatabaseBuilder(context, UserdataDatabase::class.java)
            .allowMainThreadQueries().build()

        userCatalogRepo = UserCatalogRepository(userCatalogDb)
        catalogRepo = CatalogRepository(
            daoProvider = { catalogDb.catalogDao() },
            userCatalogRepoProvider = { userCatalogRepo }
        )
        userdataRepo = UserdataRepository(
            userDao = userdataDb.userdataDao(),
            catalogDaoProvider = { catalogDb.catalogDao() },
            userCatalogRepoProvider = { userCatalogRepo }
        )
    }

    @After
    fun teardown() {
        catalogDb.close()
        userCatalogDb.close()
        userdataDb.close()
    }

    @Test
    fun testScrapedArtistAppearsInExploreAndSearch() = runBlocking {
        // Initially empty
        assertTrue(catalogRepo.getAllArtists().isEmpty())
        assertTrue(catalogRepo.searchArtists("NIKI").isEmpty())

        // Insert scraped song
        catalogRepo.insertSong(testScrapedSong)

        // 1. Appears in getAllArtists (Explore screen)
        val allArtists = catalogRepo.getAllArtists()
        assertEquals(1, allArtists.size)
        assertEquals("NIKI", allArtists[0].artistName)
        assertEquals("niki", allArtists[0].artistSlug)
        assertEquals(1, allArtists[0].songCount)

        // 2. Appears in searchArtists (Search screen "ARTIS" section)
        val searchArtists = catalogRepo.searchArtists("NIKI")
        assertEquals(1, searchArtists.size)
        assertEquals("NIKI", searchArtists[0].artistName)

        // Partial search
        val partialSearch = catalogRepo.searchArtists("nik")
        assertEquals(1, partialSearch.size)
        assertEquals("NIKI", partialSearch[0].artistName)

        // 3. Artist Directory (Artist info & Artist songs)
        val artistInfo = catalogRepo.getArtistInfo("niki")
        assertNotNull(artistInfo)
        assertEquals("NIKI", artistInfo?.artist)

        val artistSongs = catalogRepo.getArtistSongs("niki")
        assertEquals(1, artistSongs.size)
        val group = artistSongs.keys.first()
        assertEquals("High School in Jakarta", group.title)
        assertEquals("niki", group.artistSlug)

        // 4. Song versions
        val versions = catalogRepo.getSongVersions("niki", "high-school-in-jakarta")
        assertEquals(1, versions.size)
        assertEquals(testScrapedSong.sourceUrl, versions[0].sourceUrl)
    }

    @Test
    fun testScrapedSongResolvesInFavoritesAndSetlists() = runBlocking {
        // Insert scraped song into user catalog
        catalogRepo.insertSong(testScrapedSong)

        // Add to favorites
        val addedFav = userdataRepo.addFavorite(testScrapedSong, transpose = 0, capo = 0)
        assertTrue(addedFav)

        val favs = userdataRepo.getFavoriteSongs().first()
        assertEquals(1, favs.size)

        // Resolve favorites - should NOT be NotFound, should be Success
        val resolvedFavs = userdataRepo.resolveFavorites(favs)
        assertEquals(1, resolvedFavs.size)
        val favRes = resolvedFavs[0].resolution
        assertTrue("Favorite should resolve successfully", favRes is ResolutionResult.Success)
        val favSong = (favRes as ResolutionResult.Success).song
        assertEquals("High School in Jakarta", favSong.title)
        assertEquals("NIKI", favSong.artist)

        // Add to setlist
        val setlistId = userdataRepo.createSetlist("Setlist 1")
        userdataRepo.addSongToSetlist(setlistId, testScrapedSong, transpose = 0, capo = 0, note = null)

        val setlistItems = userdataRepo.getSetlistItems(setlistId).first()
        assertEquals(1, setlistItems.size)

        // Resolve setlist items - should NOT be NotFound, should be Success
        val resolvedItems = userdataRepo.resolveSetlistItems(setlistItems)
        assertEquals(1, resolvedItems.size)
        val itemRes = resolvedItems[0].resolution
        assertTrue("Setlist item should resolve successfully", itemRes is ResolutionResult.Success)
        val itemSong = (itemRes as ResolutionResult.Success).song
        assertEquals("High School in Jakarta", itemSong.title)
        assertEquals("NIKI", itemSong.artist)
    }
}
