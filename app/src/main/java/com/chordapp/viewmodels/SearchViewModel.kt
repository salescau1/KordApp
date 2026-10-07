package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _artistResults = MutableStateFlow<List<CatalogArtistInfo>>(emptyList())
    val artistResults: StateFlow<List<CatalogArtistInfo>> = _artistResults.asStateFlow()

    private val _songResults = MutableStateFlow<List<CatalogSongGroup>>(emptyList())
    val songResults: StateFlow<List<CatalogSongGroup>> = _songResults.asStateFlow()

    // Backwards compatibility
    val results: StateFlow<List<CatalogSongGroup>> = _songResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        val trimmed = newQuery.trim()
        if (trimmed.isBlank()) {
            _artistResults.value = emptyList()
            _songResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(250) // debounce
            _artistResults.value = repository.searchArtists(trimmed, limit = 5)
            _songResults.value = repository.searchSongs(trimmed, limit = 50)
            _isSearching.value = false
        }
    }
}
