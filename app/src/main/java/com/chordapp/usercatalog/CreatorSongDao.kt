package com.chordapp.usercatalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreatorSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: CreatorSongEntity): Long

    @Query("SELECT COUNT(*) FROM creator_songs")
    fun getCreatorCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM creator_songs")
    suspend fun getCreatorCount(): Int

    @Query("SELECT * FROM creator_songs ORDER BY updated_at DESC")
    fun getAllCreatorSongs(): Flow<List<CreatorSongEntity>>

    @Query("DELETE FROM creator_songs WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
