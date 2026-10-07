package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArtistDirectoryViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _artistName = MutableStateFlow("")
    val artistName: StateFlow<String> = _artistName.asStateFlow()

    private val _region = MutableStateFlow<String?>(null)
    val region: StateFlow<String?> = _region.asStateFlow()

    private val _songs = MutableStateFlow<Map<CatalogSongGroup, List<CatalogSongEntity>>>(emptyMap())
    val songs: StateFlow<Map<CatalogSongGroup, List<CatalogSongEntity>>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentArtistSlug: String? = null

    fun loadArtist(artistSlug: String) {
        if (currentArtistSlug == artistSlug && _songs.value.isNotEmpty()) {
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
            _songs.value = repository.getArtistSongs(artistSlug)
            _isLoading.value = false
        }
    }
}
