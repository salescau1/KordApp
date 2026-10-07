package com.chordapp.catalog

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CatalogDao {

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getTotalSongs(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE source = :source")
    suspend fun getSourceCount(source: String): Int

    @Query("SELECT * FROM songs WHERE source_url = :sourceUrl LIMIT 1")
    suspend fun getBySourceUrl(sourceUrl: String): CatalogSongEntity?

    @Query("SELECT * FROM songs WHERE artist_slug = :artistSlug AND title_slug = :titleSlug ORDER BY source ASC, version_name ASC")
    suspend fun getSongVersions(artistSlug: String, titleSlug: String): List<CatalogSongEntity>

    /**
     * Pencocokan longgar untuk chart Spotify: hiasan ("feat. …", "(Beda)", " - Live")
     * bisa muncul di SISI KATALOG maupun sisi Spotify, jadi slug bersih dari chart
     * dipakai sebagai PREFIX. Slug hanya berisi [huruf/angka/'-'] tanpa '%'/'_',
     * jadi aman dipakai langsung di LIKE. Gabungan artis+judul menekan false-positive.
     */
    @Query("SELECT * FROM songs WHERE artist_slug LIKE :artistPrefix || '%' AND title_slug LIKE :titlePrefix || '%' ORDER BY source ASC, version_name ASC")
    suspend fun getSongVersionsByPrefix(artistPrefix: String, titlePrefix: String): List<CatalogSongEntity>

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE artist_slug = :artistSlug
        GROUP BY artist_slug, title_slug
        ORDER BY title ASC
    """)
    suspend fun getArtistSongs(artistSlug: String): List<CatalogSongGroup>

    @Query("SELECT artist, artist_slug, COUNT(DISTINCT title_slug) as song_count FROM songs GROUP BY artist_slug ORDER BY artist COLLATE NOCASE ASC")
    suspend fun getAllArtists(): List<CatalogArtistInfo>

    @Query("SELECT COUNT(DISTINCT artist_slug) FROM songs")
    suspend fun getUniqueArtistCount(): Int

    @Query("SELECT COUNT(*) FROM (SELECT 1 FROM songs GROUP BY artist_slug, title_slug)")
    suspend fun getUniqueSongCount(): Int

    @Query("SELECT artist, artist_slug, COUNT(DISTINCT title_slug) as song_count FROM songs GROUP BY artist_slug ORDER BY artist ASC LIMIT :limit OFFSET :offset")
    suspend fun getArtists(limit: Int = 100, offset: Int = 0): List<CatalogArtistInfo>

    @Query("""
        SELECT artist, artist_slug, COUNT(DISTINCT title_slug) as song_count
        FROM songs
        WHERE artist LIKE '%' || :query || '%'
        GROUP BY artist_slug
        ORDER BY 
            CASE 
                WHEN lower(artist) = lower(:query) THEN 1
                WHEN lower(artist) LIKE lower(:query) || '%' THEN 2
                ELSE 3 
            END,
            song_count DESC
        LIMIT :limit
    """)
    suspend fun searchArtists(query: String, limit: Int = 5): List<CatalogArtistInfo>

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'
        GROUP BY artist_slug, title_slug
        ORDER BY title ASC
        LIMIT :limit
    """)
    suspend fun searchSongs(query: String, limit: Int = 50): List<CatalogSongGroup>

    @Query("SELECT * FROM songs WHERE artist_slug = :artistSlug ORDER BY title ASC, source ASC")
    suspend fun getArtistSongsRaw(artistSlug: String): List<CatalogSongEntity>

    @Query("SELECT * FROM songs WHERE artist_slug = :artistSlug LIMIT 1")
    suspend fun getArtistInfo(artistSlug: String): CatalogSongEntity?

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE lower(title) LIKE '%(ost%' OR lower(title) LIKE '%[ost%'
           OR lower(title) LIKE '%ost.%' OR lower(title) LIKE '%soundtrack%'
           OR lower(source_url) LIKE '%-ost-%'
        GROUP BY artist_slug, title_slug
        ORDER BY title ASC
    """)
    suspend fun getOstSongs(): List<CatalogSongGroup>

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE lower(trim(artist)) = lower(trim(:artist))
          AND lower(trim(title)) = lower(trim(:title))
        GROUP BY artist_slug, title_slug
        LIMIT 1
    """)
    suspend fun findSongByExact(artist: String, title: String): CatalogSongGroup?

}
