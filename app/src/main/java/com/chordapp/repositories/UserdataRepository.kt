package com.chordapp.repositories

import com.chordapp.catalog.CatalogDao
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.userdata.*
import kotlinx.coroutines.flow.Flow

sealed class ResolutionResult {
    data class Success(val song: CatalogSongEntity, val substituted: Boolean) : ResolutionResult()
    object NotFound : ResolutionResult()
}

enum class SetlistAddResult {
    Added,
    AlreadyPresent
}

data class FavoriteSongResolution(
    val favorite: FavoriteSongEntity,
    val resolution: ResolutionResult
)

data class SetlistItemResolution(
    val item: SetlistItemEntity,
    val resolution: ResolutionResult
)

class UserdataRepository(
    private val userDao: UserdataDao,
    private val catalogDao: CatalogDao
) {

    // Setlists
    fun getAllSetlists() = userDao.getAllSetlists()
    suspend fun getSetlist(id: Long) = userDao.getSetlist(id)

    suspend fun createSetlist(name: String): Long {
        val now = System.currentTimeMillis()
        return userDao.insertSetlist(SetlistEntity(
            name = name,
            note = null,
            createdAt = now,
            updatedAt = now
        ))
    }

    suspend fun renameSetlist(setlist: SetlistEntity, newName: String) {
        userDao.updateSetlist(setlist.copy(name = newName))
    }

    suspend fun deleteSetlist(setlist: SetlistEntity) {
        userDao.deleteSetlist(setlist)
    }

    // Setlist Items
    fun getSetlistItems(setlistId: Long) = userDao.getSetlistItems(setlistId)

    suspend fun addSongToSetlist(
        setlistId: Long,
        song: CatalogSongEntity,
        transpose: Int,
        capo: Int,
        note: String?
    ): SetlistAddResult {
        val currentItems = userDao.getSetlistItemsSync(setlistId)
        val duplicate = currentItems.any {
            it.sourceUrl == song.sourceUrl &&
                it.transpose == transpose &&
                it.capo == capo
        }
        if (duplicate) return SetlistAddResult.AlreadyPresent

        val item = SetlistItemEntity(
            setlistId = setlistId,
            position = currentItems.size,
            sourceUrl = song.sourceUrl,
            artistSlug = song.artistSlug,
            titleSlug = song.titleSlug,
            source = song.source,
            contentHash = song.contentHash,
            transpose = transpose,
            capo = capo,
            note = note
        )
        userDao.addSongToSetlist(item)
        return SetlistAddResult.Added
    }

    suspend fun removeSongFromSetlist(item: SetlistItemEntity) {
        userDao.removeSongFromSetlist(item)
    }

    suspend fun restoreSongToSetlist(item: SetlistItemEntity) {
        val current = userDao.getSetlistItemsSync(item.setlistId).toMutableList()
        val insertion = item.position.coerceIn(0, current.size)
        current.add(insertion, item)
        userDao.restoreSetlistItem(item, current.mapIndexed { index, value ->
            value.copy(position = index)
        })
    }

    suspend fun resolveSetlistItems(items: List<SetlistItemEntity>): List<SetlistItemResolution> =
        items.map { SetlistItemResolution(it, resolveSetlistItem(it)) }

    suspend fun reorderSetlistItems(items: List<SetlistItemEntity>) {
        val updated = items.mapIndexed { index, item -> item.copy(position = index) }
        userDao.reorderSetlistItems(updated)
    }

    // Favorites
    fun getFavoriteSongs(): Flow<List<FavoriteSongEntity>> = userDao.getFavoriteSongs()

    fun observeIsFavorite(sourceUrl: String): Flow<Boolean> = userDao.observeIsFavorite(sourceUrl)

    suspend fun addFavorite(song: CatalogSongEntity, transpose: Int, capo: Int): Boolean {
        val id = userDao.insertFavorite(
            FavoriteSongEntity(
                addedAt = System.currentTimeMillis(),
                sourceUrl = song.sourceUrl,
                artistSlug = song.artistSlug,
                titleSlug = song.titleSlug,
                source = song.source,
                contentHash = song.contentHash,
                transpose = transpose,
                capo = capo
            )
        )
        return id != -1L
    }

    suspend fun restoreFavorite(favorite: FavoriteSongEntity): Boolean =
        userDao.insertFavorite(favorite.copy(id = 0)) != -1L

    suspend fun removeFavorite(sourceUrl: String): Boolean {
        val favorite = userDao.getFavoriteBySourceUrl(sourceUrl) ?: return false
        userDao.deleteFavorite(favorite)
        return true
    }

    suspend fun toggleFavorite(song: CatalogSongEntity, transpose: Int, capo: Int): Boolean {
        val existing = userDao.getFavoriteBySourceUrl(song.sourceUrl)
        return if (existing == null) {
            addFavorite(song, transpose, capo)
            true
        } else {
            userDao.deleteFavorite(existing)
            false
        }
    }

    suspend fun resolveFavorite(favorite: FavoriteSongEntity): ResolutionResult =
        resolveCatalogReference(
            sourceUrl = favorite.sourceUrl,
            source = favorite.source,
            contentHash = favorite.contentHash,
            artistSlug = favorite.artistSlug,
            titleSlug = favorite.titleSlug
        )

    suspend fun resolveFavorites(favorites: List<FavoriteSongEntity>): List<FavoriteSongResolution> =
        favorites.map { FavoriteSongResolution(it, resolveFavorite(it)) }

    // Recently played
    fun getRecentlyPlayed(limit: Int = 20) = userDao.getRecentlyPlayed(limit)

    /**
     * Records that a catalog version was opened/played. Keyed by [CatalogSongEntity.sourceUrl],
     * so replaying the same version bumps its timestamp instead of duplicating the row.
     */
    suspend fun recordPlay(song: CatalogSongEntity) {
        userDao.recordPlay(
            PlayHistoryEntity(
                sourceUrl = song.sourceUrl,
                source = song.source,
                artist = song.artist,
                artistSlug = song.artistSlug,
                title = song.title,
                titleSlug = song.titleSlug,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    // Resolution Cascade
    suspend fun resolveSetlistItem(item: SetlistItemEntity): ResolutionResult =
        resolveCatalogReference(
            sourceUrl = item.sourceUrl,
            source = item.source,
            contentHash = item.contentHash,
            artistSlug = item.artistSlug,
            titleSlug = item.titleSlug
        )

    private suspend fun resolveCatalogReference(
        sourceUrl: String,
        source: String,
        contentHash: String,
        artistSlug: String,
        titleSlug: String
    ): ResolutionResult {
        var found = catalogDao.getBySourceUrl(sourceUrl)
        if (found != null) return ResolutionResult.Success(found, false)

        val versions = catalogDao.getSongVersions(artistSlug, titleSlug)
        found = versions.firstOrNull { it.source == source && it.contentHash == contentHash }
        if (found != null) return ResolutionResult.Success(found, false)

        found = versions.firstOrNull { it.source == source }
        if (found != null) return ResolutionResult.Success(found, false)

        found = versions.firstOrNull()
        if (found != null) return ResolutionResult.Success(found, true)

        return ResolutionResult.NotFound
    }
}
