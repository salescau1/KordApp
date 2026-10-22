package com.chordapp.usercatalog

import android.content.Context
import com.chordapp.catalog.CatalogSongEntity
import kotlinx.coroutines.flow.Flow

class UserCatalogRepository(
    private val database: UserCatalogDatabase
) {
    constructor(context: Context) : this(UserCatalogDatabase.getDatabase(context))

    private val scrapedDao get() = database.scrapedSongDao()
    private val creatorDao get() = database.creatorSongDao()

    suspend fun insertScrapedSong(song: CatalogSongEntity): Long {
        val entity = ScrapedSongEntity.fromCatalogSongEntity(song)
        return scrapedDao.insert(entity)
    }

    suspend fun insertScrapedSongs(songs: List<CatalogSongEntity>): List<Long> {
        val entities = songs.map { ScrapedSongEntity.fromCatalogSongEntity(it) }
        return scrapedDao.insertAll(entities)
    }

    suspend fun getScrapedSongByUrl(url: String): CatalogSongEntity? {
        return scrapedDao.getBySourceUrl(url)?.toCatalogSongEntity()
    }

    suspend fun getScrapedSongBySlugs(artistSlug: String, titleSlug: String): CatalogSongEntity? {
        return scrapedDao.getBySlugs(artistSlug, titleSlug)?.toCatalogSongEntity()
    }

    suspend fun searchScrapedSongs(query: String, limit: Int = 20): List<CatalogSongEntity> {
        val variants = com.chordapp.catalog.generateSearchVariants(query)
        val seen = HashSet<String>()
        val results = mutableListOf<CatalogSongEntity>()
        for (v in variants) {
            val list = scrapedDao.search(v, limit)
            for (item in list) {
                if (seen.add("${item.artistSlug}/${item.titleSlug}")) {
                    results.add(item.toCatalogSongEntity())
                    if (results.size >= limit) return results
                }
            }
        }
        return results
    }

    fun getScrapedCountFlow(): Flow<Int> = scrapedDao.getScrapedCountFlow()

    suspend fun getScrapedCount(): Int = scrapedDao.getScrapedCount()

    suspend fun deleteScrapedByUrl(sourceUrl: String): Int = scrapedDao.deleteBySourceUrl(sourceUrl)

    suspend fun clearAllScraped(): Int = scrapedDao.clearAll()

    suspend fun getAllScrapedSongsList(): List<ScrapedSongEntity> = scrapedDao.getAllScrapedSongsList()

    fun getCreatorCountFlow(): Flow<Int> = creatorDao.getCreatorCountFlow()

    suspend fun getCreatorCount(): Int = creatorDao.getCreatorCount()
}
