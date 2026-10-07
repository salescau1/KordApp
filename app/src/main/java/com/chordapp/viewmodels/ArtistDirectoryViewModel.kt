package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ArtistSongSortOrder {
    ALPHABETICAL,
    POPULAR
}

class ArtistDirectoryViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _artistName = MutableStateFlow("")
    val artistName: StateFlow<String> = _artistName.asStateFlow()

    private val _region = MutableStateFlow<String?>(null)
    val region: StateFlow<String?> = _region.asStateFlow()

    private val _rawSongs = MutableStateFlow<Map<CatalogSongGroup, List<CatalogSongEntity>>>(emptyMap())

    private val _sortOrder = MutableStateFlow(ArtistSongSortOrder.ALPHABETICAL)
    val sortOrder: StateFlow<ArtistSongSortOrder> = _sortOrder.asStateFlow()

    val songs: StateFlow<Map<CatalogSongGroup, List<CatalogSongEntity>>> = combine(_rawSongs, _sortOrder) { rawMap, order ->
        sortSongs(rawMap, order)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentArtistSlug: String? = null

    fun setSortOrder(order: ArtistSongSortOrder) {
        _sortOrder.value = order
    }

    fun loadArtist(artistSlug: String) {
        if (currentArtistSlug == artistSlug && _rawSongs.value.isNotEmpty()) {
            return
        }
        currentArtistSlug = artistSlug
        viewModelScope.launch {
            _isLoading.value = true
            val info = repository.getArtistInfo(artistSlug)
            if (info != null) {
                _artistName.value = info.artist
                _region.value = info.region
            }
            _rawSongs.value = repository.getArtistSongs(artistSlug)
            _isLoading.value = false
        }
    }

    private fun sortSongs(
        rawMap: Map<CatalogSongGroup, List<CatalogSongEntity>>,
        order: ArtistSongSortOrder
    ): Map<CatalogSongGroup, List<CatalogSongEntity>> {
        val entries = rawMap.entries.toList()
        val sorted = when (order) {
            ArtistSongSortOrder.ALPHABETICAL -> {
                entries.sortedBy { it.key.title.lowercase() }
            }
            ArtistSongSortOrder.POPULAR -> {
                entries.sortedWith(
                    compareByDescending<Map.Entry<CatalogSongGroup, List<CatalogSongEntity>>> { entry ->
                        entry.value.maxOfOrNull { it.votes ?: 0 } ?: 0
                    }.thenByDescending { entry ->
                        entry.value.maxOfOrNull { it.rating ?: 0f } ?: 0f
                    }.thenByDescending { entry ->
                        entry.key.versionCount
                    }.thenBy { entry ->
                        entry.key.title.lowercase()
                    }
                )
            }
        }
        val result = LinkedHashMap<CatalogSongGroup, List<CatalogSongEntity>>()
        for (entry in sorted) {
            result[entry.key] = entry.value
        }
        return result
    }
}
