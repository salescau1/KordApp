package com.chordapp.catalog

import androidx.room.ColumnInfo

data class CatalogArtistInfo(
    @ColumnInfo(name = "artist")
    val artistName: String,
    @ColumnInfo(name = "artist_slug")
    val artistSlug: String,
    @ColumnInfo(name = "song_count")
    val songCount: Int
)
