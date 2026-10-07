package com.chordapp.userdata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "setlists")
data class SetlistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "setlist_items",
    foreignKeys = [
        ForeignKey(
            entity = SetlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["setlist_id"],
            onDelete = ForeignKey.CASCADE // If setlist is deleted, items go away.
        )
    ],
    indices = [Index("setlist_id")]
)
data class SetlistItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "setlist_id")
    val setlistId: Long,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String,

    @ColumnInfo(name = "artist_slug")
    val artistSlug: String,

    @ColumnInfo(name = "title_slug")
    val titleSlug: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "content_hash")
    val contentHash: String,

    @ColumnInfo(name = "transpose")
    val transpose: Int,

    @ColumnInfo(name = "capo")
    val capo: Int,

    @ColumnInfo(name = "note")
    val note: String?
)

/**
 * Recently played history (user data). One row per catalog version a user opened,
 * keyed by [sourceUrl] (the primary catalog reference — never [catalog songs.id]).
 *
 * Only references + a display snapshot (title/artist/source) are stored here, never
 * chord content. Replaying the same version refreshes [playedAt] and moves it to the
 * top of the history.
 */
@Entity(
    tableName = "favorite_songs",
    indices = [Index(value = ["source_url"], unique = true)]
)
data class FavoriteSongEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "added_at")
    val addedAt: Long,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String,

    @ColumnInfo(name = "artist_slug")
    val artistSlug: String,

    @ColumnInfo(name = "title_slug")
    val titleSlug: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "content_hash")
    val contentHash: String,

    @ColumnInfo(name = "transpose")
    val transpose: Int,

    @ColumnInfo(name = "capo")
    val capo: Int
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_url")
    val sourceUrl: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "artist")
    val artist: String,

    @ColumnInfo(name = "artist_slug")
    val artistSlug: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "title_slug")
    val titleSlug: String,

    @ColumnInfo(name = "played_at")
    val playedAt: Long
)
