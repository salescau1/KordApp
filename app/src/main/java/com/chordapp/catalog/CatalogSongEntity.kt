package com.chordapp.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mirrors the `songs` table shipped in assets/catalog.db.
 *
 * The three indices below are REQUIRED: Room validates a createFromAsset
 * database's schema (including indices) against this entity before use, and
 * the prepackaged table already contains idx_source / idx_title_slug /
 * idx_artist_slug. Without declaring them here the database fails to open
 * with "Pre-packaged database has an invalid schema".
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["source"], name = "idx_source"),
        Index(value = ["title_slug"], name = "idx_title_slug"),
        Index(value = ["artist_slug"], name = "idx_artist_slug")
    ]
)
data class CatalogSongEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long?,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "source_url")
    val sourceUrl: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist")
    val artist: String,

    @ColumnInfo(name = "title_slug")
    val titleSlug: String,

    @ColumnInfo(name = "artist_slug")
    val artistSlug: String,

    @ColumnInfo(name = "version_name")
    val versionName: String?,

    @ColumnInfo(name = "rating")
    val rating: Float?,

    @ColumnInfo(name = "votes")
    val votes: Int?,

    @ColumnInfo(name = "key_base")
    val keyBase: String?,

    @ColumnInfo(name = "difficulty")
    val difficulty: String?,

    @ColumnInfo(name = "category")
    val category: String?,

    @ColumnInfo(name = "region")
    val region: String,

    @ColumnInfo(name = "chords")
    val chords: String,

    @ColumnInfo(name = "chord_count")
    val chordCount: Int,

    @ColumnInfo(name = "line_count")
    val lineCount: Int,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "content_hash")
    val contentHash: String,

    @ColumnInfo(name = "scraped_at")
    val scrapedAt: String
)
