package com.chordapp.usercatalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chordapp.catalog.CatalogSongEntity

@Entity(
    tableName = "scraped_songs",
    indices = [
        Index(value = ["source_url"], unique = true, name = "idx_scraped_source_url"),
        Index(value = ["title_slug"], name = "idx_scraped_title_slug"),
        Index(value = ["artist_slug"], name = "idx_scraped_artist_slug")
    ]
)
data class ScrapedSongEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,

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
    val versionName: String? = null,

    @ColumnInfo(name = "rating")
    val rating: Float? = null,

    @ColumnInfo(name = "votes")
    val votes: Int? = null,

    @ColumnInfo(name = "key_base")
    val keyBase: String? = null,

    @ColumnInfo(name = "difficulty")
    val difficulty: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "region")
    val region: String = "ID",

    @ColumnInfo(name = "chords")
    val chords: String = "",

    @ColumnInfo(name = "chord_count")
    val chordCount: Int = 0,

    @ColumnInfo(name = "line_count")
    val lineCount: Int = 0,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "content_hash")
    val contentHash: String = "",

    @ColumnInfo(name = "scraped_at")
    val scrapedAt: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toCatalogSongEntity(): CatalogSongEntity = CatalogSongEntity(
        id = id,
        source = source,
        sourceUrl = sourceUrl,
        title = title,
        artist = artist,
        titleSlug = titleSlug,
        artistSlug = artistSlug,
        versionName = versionName,
        rating = rating,
        votes = votes,
        keyBase = keyBase,
        difficulty = difficulty,
        category = category,
        region = region,
        chords = chords,
        chordCount = chordCount,
        lineCount = lineCount,
        content = content,
        contentHash = contentHash,
        scrapedAt = scrapedAt,
        tier = "SCRAPED"
    )

    companion object {
        fun fromCatalogSongEntity(entity: CatalogSongEntity): ScrapedSongEntity = ScrapedSongEntity(
            id = null,
            source = entity.source,
            sourceUrl = entity.sourceUrl,
            title = entity.title,
            artist = entity.artist,
            titleSlug = entity.titleSlug,
            artistSlug = entity.artistSlug,
            versionName = entity.versionName,
            rating = entity.rating,
            votes = entity.votes,
            keyBase = entity.keyBase,
            difficulty = entity.difficulty,
            category = entity.category,
            region = entity.region,
            chords = entity.chords,
            chordCount = entity.chordCount,
            lineCount = entity.lineCount,
            content = entity.content,
            contentHash = entity.contentHash,
            scrapedAt = entity.scrapedAt
        )
    }
}
