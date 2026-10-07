package com.chordapp.userdata

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserdataDao {

    // Setlists
    @Query("SELECT * FROM setlists ORDER BY updated_at DESC")
    fun getAllSetlists(): Flow<List<SetlistEntity>>

    @Query("SELECT * FROM setlists WHERE id = :setlistId LIMIT 1")
    suspend fun getSetlist(setlistId: Long): SetlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlist(setlist: SetlistEntity): Long

    @Update
    suspend fun updateSetlist(setlist: SetlistEntity)

    @Delete
    suspend fun deleteSetlist(setlist: SetlistEntity)

    // Setlist Items
    @Query("SELECT * FROM setlist_items WHERE setlist_id = :setlistId ORDER BY position ASC")
    fun getSetlistItems(setlistId: Long): Flow<List<SetlistItemEntity>>

    @Query("SELECT * FROM setlist_items WHERE setlist_id = :setlistId ORDER BY position ASC")
    suspend fun getSetlistItemsSync(setlistId: Long): List<SetlistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlistItem(item: SetlistItemEntity): Long

    @Update
    suspend fun updateSetlistItems(items: List<SetlistItemEntity>)

    @Delete
    suspend fun deleteSetlistItem(item: SetlistItemEntity)

    @Query("UPDATE setlists SET updated_at = :timestamp WHERE id = :setlistId")
    suspend fun updateSetlistTimestamp(setlistId: Long, timestamp: Long)

    // Favorites
    @Query("SELECT * FROM favorite_songs ORDER BY added_at DESC")
    fun getFavoriteSongs(): Flow<List<FavoriteSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE source_url = :sourceUrl)")
    fun observeIsFavorite(sourceUrl: String): Flow<Boolean>

    @Query("SELECT * FROM favorite_songs WHERE source_url = :sourceUrl LIMIT 1")
    suspend fun getFavoriteBySourceUrl(sourceUrl: String): FavoriteSongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(favorite: FavoriteSongEntity): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteSongEntity)

    // Recently played
    @Query("SELECT * FROM play_history ORDER BY played_at DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlay(entry: PlayHistoryEntity)

    @Transaction
    suspend fun addSongToSetlist(item: SetlistItemEntity) {
        insertSetlistItem(item)
        updateSetlistTimestamp(item.setlistId, System.currentTimeMillis())
    }

    @Transaction
    suspend fun removeSongFromSetlist(item: SetlistItemEntity) {
        deleteSetlistItem(item)
        val remaining = getSetlistItemsSync(item.setlistId)
            .mapIndexed { index, value -> value.copy(position = index) }
        if (remaining.isNotEmpty()) updateSetlistItems(remaining)
        touchSetlist(item.setlistId)
    }

    @Transaction
    suspend fun restoreSetlistItem(
        item: SetlistItemEntity,
        orderedItems: List<SetlistItemEntity>
    ) {
        insertSetlistItem(item)
        updateSetlistItems(orderedItems)
        touchSetlist(item.setlistId)
    }

    @Transaction
    suspend fun reorderSetlistItems(items: List<SetlistItemEntity>) {
        updateSetlistItems(items)
        if (items.isNotEmpty()) touchSetlist(items.first().setlistId)
    }

    private suspend fun touchSetlist(setlistId: Long) {
        updateSetlistTimestamp(setlistId, System.currentTimeMillis())
    }
}
