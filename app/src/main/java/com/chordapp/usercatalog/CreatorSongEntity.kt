package com.chordapp.usercatalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "creator_songs",
    indices = [
        Index(value = ["creator_email"], name = "idx_creator_email"),
        Index(value = ["title_slug"], name = "idx_creator_title_slug"),
        Index(value = ["artist_slug"], name = "idx_creator_artist_slug")
    ]
)
data class CreatorSongEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,

    @ColumnInfo(name = "creator_email")
    val creatorEmail: String,

    @ColumnInfo(name = "creator_name")
    val creatorName: String? = null,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist")
    val artist: String,

    @ColumnInfo(name = "title_slug")
    val titleSlug: String,

    @ColumnInfo(name = "artist_slug")
    val artistSlug: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "key_base")
    val keyBase: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
