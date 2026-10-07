package com.chordapp.catalog

import androidx.room.ColumnInfo

data class CatalogSongGroup(
    @ColumnInfo(name = "artist")
    val artistName: String,
    @ColumnInfo(name = "artist_slug")
    val artistSlug: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "title_slug")
    val titleSlug: String,
    @ColumnInfo(name = "version_count")
    val versionCount: Int,
    @ColumnInfo(name = "chord_count")
    val chordCount: Int
)
