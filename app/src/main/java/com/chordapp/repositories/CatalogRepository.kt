package com.chordapp.repositories

import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogDao
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.CatalogSongGroup

class CatalogRepository(private val dao: CatalogDao) {

    suspend fun searchSongs(query: String, limit: Int = 50): List<CatalogSongGroup> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        val vars = com.chordapp.catalog.generateSearchVariants(trimmed)
        val q1 = vars.getOrElse(0) { "" }
        val q2 = vars.getOrElse(1) { "" }
        val q3 = vars.getOrElse(2) { "" }
        val q4 = vars.getOrElse(3) { "" }
        val q5 = vars.getOrElse(4) { "" }
        return dao.searchSongsVariants(q1, q2, q3, q4, q5, limit = limit)
    }

    suspend fun searchSongsByGt(query: String, limit: Int = 50): List<CatalogSongGroup> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        val vars = com.chordapp.catalog.generateSearchVariants(trimmed)
        val q1 = vars.getOrElse(0) { "" }
        val q2 = vars.getOrElse(1) { "" }
        val q3 = vars.getOrElse(2) { "" }
        val q4 = vars.getOrElse(3) { "" }
        val q5 = vars.getOrElse(4) { "" }
        return dao.searchSongsByGtVariants(q1, q2, q3, q4, q5, limit = limit)
    }

    suspend fun searchArtists(query: String, limit: Int = 5): List<CatalogArtistInfo> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        val vars = com.chordapp.catalog.generateSearchVariants(trimmed)
        val q1 = vars.getOrElse(0) { "" }
        val q2 = vars.getOrElse(1) { "" }
        val q3 = vars.getOrElse(2) { "" }
        val q4 = vars.getOrElse(3) { "" }
        val q5 = vars.getOrElse(4) { "" }
        return dao.searchArtistsVariants(q1, q2, q3, q4, q5, limit = limit)
    }

    suspend fun searchArtistsByGt(query: String, limit: Int = 5): List<CatalogArtistInfo> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        val vars = com.chordapp.catalog.generateSearchVariants(trimmed)
        val q1 = vars.getOrElse(0) { "" }
        val q2 = vars.getOrElse(1) { "" }
        val q3 = vars.getOrElse(2) { "" }
        val q4 = vars.getOrElse(3) { "" }
        val q5 = vars.getOrElse(4) { "" }
        return dao.searchArtistsByGtVariants(q1, q2, q3, q4, q5, limit = limit)
    }
    
    suspend fun getArtistInfo(artistSlug: String): CatalogSongEntity? {
        return dao.getArtistInfo(artistSlug)
    }

    suspend fun getArtistSongs(artistSlug: String): Map<CatalogSongGroup, List<CatalogSongEntity>> {
        val rawSongs = dao.getArtistSongsRaw(artistSlug)
        val grouped = rawSongs.groupBy { it.titleSlug }
        val result = mutableMapOf<CatalogSongGroup, List<CatalogSongEntity>>()
        
        for ((_, versions) in grouped) {
            val first = versions.first()
            val group = CatalogSongGroup(
                artistName = first.artist,
                artistSlug = first.artistSlug,
                title = first.title,
                titleSlug = first.titleSlug,
                versionCount = versions.size,
                chordCount = versions.minByOrNull { it.chordCount }?.chordCount ?: 0
            )
            result[group] = versions
        }
        return result
    }
    
    /**
     * Daftar artis untuk layar Explore. Katalog read-only, jadi hasilnya di-cache
     * di memori level-proses: buka Explore -> pindah -> balik = instan (tanpa query
     * ulang ke SQLite). Cache reset saat proses mati (cold start).
     */
    suspend fun getAllArtists(): List<CatalogArtistInfo> {
        cachedArtists?.let { return it }
        return dao.getAllArtists().also { cachedArtists = it }
    }

    suspend fun getUniqueArtistCount() = dao.getUniqueArtistCount()

    suspend fun getUniqueSongCount() = dao.getUniqueSongCount()

    suspend fun getBySourceUrl(sourceUrl: String): CatalogSongEntity? {
        return dao.getBySourceUrl(sourceUrl)
    }

    suspend fun getOstSongs(): List<CatalogSongGroup> {
        return dao.getOstSongs()
    }

    suspend fun getSongVersions(artistSlug: String, titleSlug: String): List<CatalogSongEntity> {
        return dao.getSongVersions(artistSlug, titleSlug)
    }

    /** Pencocokan longgar (prefix) untuk chart: hiasan bisa ada di sisi katalog. */
    suspend fun getSongVersionsByPrefix(artistPrefix: String, titlePrefix: String): List<CatalogSongEntity> {
        return dao.getSongVersionsByPrefix(artistPrefix, titlePrefix)
    }

    /** Exact (trimmed, case-insensitive) lookup of one logical song by its displayed name. */
    suspend fun findSongByExact(artist: String, title: String): CatalogSongGroup? {
        return dao.findSongByExact(artist, title)
    }

    suspend fun insertSong(song: CatalogSongEntity): Long {
        cachedArtists = null
        dao.deleteBySourceUrl(song.sourceUrl)
        return dao.insertSong(song)
    }

    suspend fun insertSongs(songs: List<CatalogSongEntity>): List<Long> {
        cachedArtists = null
        for (song in songs) {
            dao.deleteBySourceUrl(song.sourceUrl)
        }
        return dao.insertSongs(songs)
    }

    suspend fun pruneDatabase(
        artistSlugs: List<String>,
        songKeys: List<Pair<String, String>>,
        versionIds: List<Long>
    ) {
        cachedArtists = null
        if (artistSlugs.isNotEmpty()) {
            dao.deleteArtistsBySlugs(artistSlugs)
        }
        for ((artistSlug, titleSlug) in songKeys) {
            dao.deleteSongBySlug(artistSlug, titleSlug)
        }
        if (versionIds.isNotEmpty()) {
            dao.deleteSongsByIds(versionIds)
        }
    }

    companion object {
        /** Cache proses daftar artis Explore. Reset saat proses mati (cold start). */
        @Volatile
        private var cachedArtists: List<CatalogArtistInfo>? = null
    }
}
