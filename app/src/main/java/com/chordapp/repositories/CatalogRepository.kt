package com.chordapp.repositories

import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogDao
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.CatalogSongGroup
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CatalogRepository(
    private val daoProvider: () -> CatalogDao,
    private val userCatalogRepoProvider: (() -> com.chordapp.usercatalog.UserCatalogRepository)? = null
) {

    constructor(dao: CatalogDao) : this({ dao }, null)
    constructor(dao: CatalogDao, userCatalogRepo: com.chordapp.usercatalog.UserCatalogRepository) : this({ dao }, { userCatalogRepo })

    private val dao: CatalogDao get() = daoProvider()
    val userCatalogRepo: com.chordapp.usercatalog.UserCatalogRepository? get() = userCatalogRepoProvider?.invoke()

    val catalogUpdateFlow: SharedFlow<Long> get() = Companion.catalogUpdateFlow

    fun notifyCatalogUpdated() {
        Companion.notifyCatalogUpdated()
    }

    suspend fun searchSongs(query: String, limit: Int = 50): List<CatalogSongGroup> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        val scrapedResults = userCatalogRepo?.searchScrapedSongs(trimmed, limit = 10)?.map {
            CatalogSongGroup(
                artistName = it.artist,
                artistSlug = it.artistSlug,
                title = it.title,
                titleSlug = it.titleSlug,
                versionCount = 1,
                chordCount = it.chordCount
            )
        } ?: emptyList()

        val vars = com.chordapp.catalog.generateSearchVariants(trimmed)
        val q1 = vars.getOrElse(0) { "" }
        val q2 = vars.getOrElse(1) { "" }
        val q3 = vars.getOrElse(2) { "" }
        val q4 = vars.getOrElse(3) { "" }
        val q5 = vars.getOrElse(4) { "" }
        val officialResults = dao.searchSongsVariants(q1, q2, q3, q4, q5, limit = limit)

        val seen = HashSet<String>()
        val combined = ArrayList<CatalogSongGroup>()
        for (item in scrapedResults) {
            val key = "${item.artistSlug}/${item.titleSlug}"
            if (seen.add(key)) {
                combined.add(item)
            }
        }
        for (item in officialResults) {
            val key = "${item.artistSlug}/${item.titleSlug}"
            if (seen.add(key)) {
                combined.add(item)
            }
        }
        return combined.take(limit)
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
        val userSong = userCatalogRepo?.getScrapedSongByUrl(sourceUrl)
        if (userSong != null) return userSong
        return dao.getBySourceUrl(sourceUrl)
    }

    suspend fun getOstSongs(): List<CatalogSongGroup> {
        return dao.getOstSongs()
    }

    suspend fun getSongVersions(artistSlug: String, titleSlug: String): List<CatalogSongEntity> {
        val userSong = userCatalogRepo?.getScrapedSongBySlugs(artistSlug, titleSlug)
        val officialVersions = dao.getSongVersions(artistSlug, titleSlug)
        return if (userSong != null) {
            listOf(userSong) + officialVersions.filter { it.sourceUrl != userSong.sourceUrl }
        } else {
            officialVersions
        }
    }

    /** Pencocokan longgar (prefix) untuk chart: hiasan bisa ada di sisi katalog. */
    suspend fun getSongVersionsByPrefix(artistPrefix: String, titlePrefix: String): List<CatalogSongEntity> {
        return dao.getSongVersionsByPrefix(artistPrefix, titlePrefix)
    }

    /** Exact (trimmed, case-insensitive) lookup of one logical song by its displayed name. */
    suspend fun findSongByExact(artist: String, title: String): CatalogSongGroup? {
        val official = dao.findSongByExact(artist, title)
        if (official != null) return official
        val scraped = userCatalogRepo?.searchScrapedSongs(title, limit = 10)?.firstOrNull {
            it.artist.equals(artist, ignoreCase = true) && it.title.equals(title, ignoreCase = true)
        }
        return scraped?.let {
            CatalogSongGroup(
                artistName = it.artist,
                artistSlug = it.artistSlug,
                title = it.title,
                titleSlug = it.titleSlug,
                versionCount = 1,
                chordCount = it.chordCount
            )
        }
    }

    suspend fun insertSong(song: CatalogSongEntity): Long {
        cachedArtists = null
        if (userCatalogRepo != null) {
            val id = userCatalogRepo!!.insertScrapedSong(song)
            notifyCatalogUpdated()
            return id
        }
        dao.deleteBySourceUrl(song.sourceUrl)
        val id = dao.insertSong(song)
        notifyCatalogUpdated()
        return id
    }

    suspend fun insertSongs(songs: List<CatalogSongEntity>): List<Long> {
        cachedArtists = null
        if (userCatalogRepo != null) {
            val ids = userCatalogRepo!!.insertScrapedSongs(songs)
            notifyCatalogUpdated()
            return ids
        }
        for (song in songs) {
            dao.deleteBySourceUrl(song.sourceUrl)
        }
        val ids = dao.insertSongs(songs)
        notifyCatalogUpdated()
        return ids
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
        notifyCatalogUpdated()
    }

    suspend fun getSongCountByTier(tier: String): Int = dao.getSongCountByTier(tier)

    suspend fun deleteSongsByTier(tier: String): Int {
        cachedArtists = null
        val deleted = dao.deleteSongsByTier(tier)
        notifyCatalogUpdated()
        return deleted
    }

    companion object {
        /** Cache proses daftar artis Explore. Reset saat proses mati (cold start) atau saat katalog di-update. */
        @Volatile
        private var cachedArtists: List<CatalogArtistInfo>? = null

        private val _catalogUpdateFlow = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 64)
        val catalogUpdateFlow: SharedFlow<Long> = _catalogUpdateFlow.asSharedFlow()

        fun notifyCatalogUpdated() {
            cachedArtists = null
            _catalogUpdateFlow.tryEmit(System.currentTimeMillis())
        }
    }
}
