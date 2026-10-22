package com.chordapp.usercatalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapedSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: ScrapedSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<ScrapedSongEntity>): List<Long>

    @Query("SELECT * FROM scraped_songs WHERE source_url = :sourceUrl LIMIT 1")
    suspend fun getBySourceUrl(sourceUrl: String): ScrapedSongEntity?

    @Query("SELECT * FROM scraped_songs WHERE artist_slug = :artistSlug AND title_slug = :titleSlug LIMIT 1")
    suspend fun getBySlugs(artistSlug: String, titleSlug: String): ScrapedSongEntity?

    @Query("SELECT * FROM scraped_songs ORDER BY created_at DESC")
    fun getAllScrapedSongs(): Flow<List<ScrapedSongEntity>>

    @Query("SELECT * FROM scraped_songs ORDER BY created_at DESC")
    suspend fun getAllScrapedSongsList(): List<ScrapedSongEntity>

    @Query("SELECT COUNT(*) FROM scraped_songs")
    fun getScrapedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scraped_songs")
    suspend fun getScrapedCount(): Int

    @Query("DELETE FROM scraped_songs WHERE source_url = :sourceUrl")
    suspend fun deleteBySourceUrl(sourceUrl: String): Int

    @Query("DELETE FROM scraped_songs")
    suspend fun clearAll(): Int

    @Query("""
        SELECT * FROM scraped_songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR (artist || ' ' || title) LIKE '%' || :query || '%' 
           OR (title || ' ' || artist) LIKE '%' || :query || '%' 
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 20): List<ScrapedSongEntity>
}
