package com.chordapp.catalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogDao {

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getTotalSongs(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE source = :source")
    suspend fun getSourceCount(source: String): Int

    @Query("SELECT * FROM songs WHERE source_url = :sourceUrl ORDER BY id DESC LIMIT 1")
    suspend fun getBySourceUrl(sourceUrl: String): CatalogSongEntity?

    @Query("DELETE FROM songs WHERE source_url = :sourceUrl")
    suspend fun deleteBySourceUrl(sourceUrl: String): Int

    @Query("""
        SELECT * FROM songs 
        WHERE artist_slug = :artistSlug 
          AND (
            title_slug = :titleSlug 
            OR title_slug LIKE :titleSlug || '-feat-%' 
            OR (instr(:titleSlug, '-feat-') > 0 AND title_slug = substr(:titleSlug, 1, instr(:titleSlug, '-feat-') - 1))
          )
        ORDER BY source ASC, version_name ASC
    """)
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

    @Query("SELECT artist, artist_slug, MAX(region) as region, COUNT(DISTINCT title_slug) as song_count FROM songs GROUP BY artist_slug ORDER BY artist COLLATE NOCASE ASC")
    suspend fun getAllArtists(): List<CatalogArtistInfo>

    @Query("SELECT COUNT(DISTINCT artist_slug) FROM songs")
    suspend fun getUniqueArtistCount(): Int

    @Query("SELECT COUNT(*) FROM (SELECT 1 FROM songs GROUP BY artist_slug, title_slug)")
    suspend fun getUniqueSongCount(): Int

    @Query("SELECT artist, artist_slug, MAX(region) as region, COUNT(DISTINCT title_slug) as song_count FROM songs GROUP BY artist_slug ORDER BY artist ASC LIMIT :limit OFFSET :offset")
    suspend fun getArtists(limit: Int = 100, offset: Int = 0): List<CatalogArtistInfo>

    @Query("""
        SELECT artist, artist_slug, MAX(region) as region, COUNT(DISTINCT title_slug) as song_count
        FROM songs
        WHERE (:q1 != '' AND (artist LIKE '%' || :q1 || '%' OR artist_slug LIKE '%' || :q1 || '%'))
           OR (:q2 != '' AND (artist LIKE '%' || :q2 || '%' OR artist_slug LIKE '%' || :q2 || '%'))
           OR (:q3 != '' AND (artist LIKE '%' || :q3 || '%' OR artist_slug LIKE '%' || :q3 || '%'))
           OR (:q4 != '' AND (artist LIKE '%' || :q4 || '%' OR artist_slug LIKE '%' || :q4 || '%'))
           OR (:q5 != '' AND (artist LIKE '%' || :q5 || '%' OR artist_slug LIKE '%' || :q5 || '%'))
        GROUP BY artist_slug
        ORDER BY 
            CASE 
                WHEN lower(artist) = lower(:q1) OR lower(artist) = lower(:q2) THEN 1
                WHEN lower(artist) LIKE lower(:q1) || '%' OR lower(artist) LIKE lower(:q2) || '%' THEN 2
                ELSE 3 
            END,
            song_count DESC
        LIMIT :limit
    """)
    suspend fun searchArtistsVariants(
        q1: String,
        q2: String = "",
        q3: String = "",
        q4: String = "",
        q5: String = "",
        limit: Int = 5
    ): List<CatalogArtistInfo>

    @Query("""
        SELECT artist, artist_slug, MAX(region) as region, COUNT(DISTINCT title_slug) as song_count
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
        SELECT artist, artist_slug, MAX(region) as region, COUNT(DISTINCT title_slug) as song_count
        FROM songs
        WHERE source = 'GT' AND (
            (:q1 != '' AND (artist LIKE '%' || :q1 || '%' OR artist_slug LIKE '%' || :q1 || '%'))
            OR (:q2 != '' AND (artist LIKE '%' || :q2 || '%' OR artist_slug LIKE '%' || :q2 || '%'))
            OR (:q3 != '' AND (artist LIKE '%' || :q3 || '%' OR artist_slug LIKE '%' || :q3 || '%'))
            OR (:q4 != '' AND (artist LIKE '%' || :q4 || '%' OR artist_slug LIKE '%' || :q4 || '%'))
            OR (:q5 != '' AND (artist LIKE '%' || :q5 || '%' OR artist_slug LIKE '%' || :q5 || '%'))
        )
        GROUP BY artist_slug
        ORDER BY 
            CASE 
                WHEN lower(artist) = lower(:q1) OR lower(artist) = lower(:q2) THEN 1
                WHEN lower(artist) LIKE lower(:q1) || '%' OR lower(artist) LIKE lower(:q2) || '%' THEN 2
                ELSE 3 
            END,
            song_count DESC
        LIMIT :limit
    """)
    suspend fun searchArtistsByGtVariants(
        q1: String,
        q2: String = "",
        q3: String = "",
        q4: String = "",
        q5: String = "",
        limit: Int = 5
    ): List<CatalogArtistInfo>

    @Query("""
        SELECT artist, artist_slug, MAX(region) as region, COUNT(DISTINCT title_slug) as song_count
        FROM songs
        WHERE source = 'GT' AND artist LIKE '%' || :query || '%'
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
    suspend fun searchArtistsByGt(query: String, limit: Int = 5): List<CatalogArtistInfo>

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE (:q1 != '' AND (title LIKE '%' || :q1 || '%' OR artist LIKE '%' || :q1 || '%' OR artist_slug LIKE '%' || :q1 || '%' OR title_slug LIKE '%' || :q1 || '%'))
           OR (:q2 != '' AND (title LIKE '%' || :q2 || '%' OR artist LIKE '%' || :q2 || '%' OR artist_slug LIKE '%' || :q2 || '%' OR title_slug LIKE '%' || :q2 || '%'))
           OR (:q3 != '' AND (title LIKE '%' || :q3 || '%' OR artist LIKE '%' || :q3 || '%' OR artist_slug LIKE '%' || :q3 || '%' OR title_slug LIKE '%' || :q3 || '%'))
           OR (:q4 != '' AND (title LIKE '%' || :q4 || '%' OR artist LIKE '%' || :q4 || '%' OR artist_slug LIKE '%' || :q4 || '%' OR title_slug LIKE '%' || :q4 || '%'))
           OR (:q5 != '' AND (title LIKE '%' || :q5 || '%' OR artist LIKE '%' || :q5 || '%' OR artist_slug LIKE '%' || :q5 || '%' OR title_slug LIKE '%' || :q5 || '%'))
        GROUP BY artist_slug, title_slug
        ORDER BY 
            CASE
                WHEN lower(title) = lower(:q1) OR lower(title) = lower(:q2) THEN 1
                WHEN lower(title) LIKE lower(:q1) || '%' OR lower(title) LIKE lower(:q2) || '%' THEN 2
                ELSE 3
            END,
            title ASC
        LIMIT :limit
    """)
    suspend fun searchSongsVariants(
        q1: String,
        q2: String = "",
        q3: String = "",
        q4: String = "",
        q5: String = "",
        limit: Int = 50
    ): List<CatalogSongGroup>

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'
        GROUP BY artist_slug, title_slug
        ORDER BY title ASC
        LIMIT :limit
    """)
    suspend fun searchSongs(query: String, limit: Int = 50): List<CatalogSongGroup>

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE source = 'GT' AND (
            (:q1 != '' AND (title LIKE '%' || :q1 || '%' OR artist LIKE '%' || :q1 || '%' OR artist_slug LIKE '%' || :q1 || '%' OR title_slug LIKE '%' || :q1 || '%'))
            OR (:q2 != '' AND (title LIKE '%' || :q2 || '%' OR artist LIKE '%' || :q2 || '%' OR artist_slug LIKE '%' || :q2 || '%' OR title_slug LIKE '%' || :q2 || '%'))
            OR (:q3 != '' AND (title LIKE '%' || :q3 || '%' OR artist LIKE '%' || :q3 || '%' OR artist_slug LIKE '%' || :q3 || '%' OR title_slug LIKE '%' || :q3 || '%'))
            OR (:q4 != '' AND (title LIKE '%' || :q4 || '%' OR artist LIKE '%' || :q4 || '%' OR artist_slug LIKE '%' || :q4 || '%' OR title_slug LIKE '%' || :q4 || '%'))
            OR (:q5 != '' AND (title LIKE '%' || :q5 || '%' OR artist LIKE '%' || :q5 || '%' OR artist_slug LIKE '%' || :q5 || '%' OR title_slug LIKE '%' || :q5 || '%'))
        )
        GROUP BY artist_slug, title_slug
        ORDER BY 
            CASE
                WHEN lower(title) = lower(:q1) OR lower(title) = lower(:q2) THEN 1
                WHEN lower(title) LIKE lower(:q1) || '%' OR lower(title) LIKE lower(:q2) || '%' THEN 2
                ELSE 3
            END,
            title ASC
        LIMIT :limit
    """)
    suspend fun searchSongsByGtVariants(
        q1: String,
        q2: String = "",
        q3: String = "",
        q4: String = "",
        q5: String = "",
        limit: Int = 50
    ): List<CatalogSongGroup>

    @Query("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) as version_count, MIN(chord_count) as chord_count
        FROM songs
        WHERE source = 'GT' AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%')
        GROUP BY artist_slug, title_slug
        ORDER BY title ASC
        LIMIT :limit
    """)
    suspend fun searchSongsByGt(query: String, limit: Int = 50): List<CatalogSongGroup>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: CatalogSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<CatalogSongEntity>): List<Long>

    @Query("DELETE FROM songs WHERE artist_slug IN (:artistSlugs)")
    suspend fun deleteArtistsBySlugs(artistSlugs: List<String>): Int

    @Query("DELETE FROM songs WHERE artist_slug = :artistSlug AND title_slug = :titleSlug")
    suspend fun deleteSongBySlug(artistSlug: String, titleSlug: String): Int

    @Query("DELETE FROM songs WHERE id IN (:songIds)")
    suspend fun deleteSongsByIds(songIds: List<Long>): Int

    @Query("SELECT COUNT(*) FROM songs WHERE tier = :tier")
    suspend fun getSongCountByTier(tier: String): Int

    @Query("DELETE FROM songs WHERE tier = :tier")
    suspend fun deleteSongsByTier(tier: String): Int
}
