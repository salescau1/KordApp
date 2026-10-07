package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.repositories.CatalogRepository
import com.chordapp.ui.screens.explore.ExploreGenre
import com.chordapp.ui.screens.explore.OstFranchise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExploreViewModel(private val repository: CatalogRepository) : ViewModel() {
    private val _artists = MutableStateFlow<List<CatalogArtistInfo>>(emptyList())
    val artists: StateFlow<List<CatalogArtistInfo>> = _artists.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedLetter = MutableStateFlow<Char?>(null)
    val selectedLetter: StateFlow<Char?> = _selectedLetter.asStateFlow()

    private val _selectedGenre = MutableStateFlow<ExploreGenre?>(null)
    val selectedGenre: StateFlow<ExploreGenre?> = _selectedGenre.asStateFlow()

    private val _ostSongs = MutableStateFlow<List<CatalogSongGroup>>(emptyList())
    val ostSongs: StateFlow<List<CatalogSongGroup>> = _ostSongs.asStateFlow()

    private val _selectedOstFranchise = MutableStateFlow<OstFranchise?>(null)
    val selectedOstFranchise: StateFlow<OstFranchise?> = _selectedOstFranchise.asStateFlow()

    private val _selectedOstCategory = MutableStateFlow("Semua")
    val selectedOstCategory: StateFlow<String> = _selectedOstCategory.asStateFlow()

    private val _ostSearchQuery = MutableStateFlow("")
    val ostSearchQuery: StateFlow<String> = _ostSearchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _artists.value = repository.getAllArtists()
            _isLoading.value = false
        }
    }

    fun setQuery(value: String) {
        _query.value = value
        if (value.isNotBlank()) {
            _selectedLetter.value = null
            _selectedGenre.value = null
            _selectedOstFranchise.value = null
        }
    }

    fun selectLetter(letter: Char) {
        _query.value = ""
        _selectedGenre.value = null
        _selectedOstFranchise.value = null
        _selectedLetter.value = letter.uppercaseChar()
    }

    fun clearLetter() {
        _selectedLetter.value = null
    }

    fun selectGenre(genre: ExploreGenre) {
        _query.value = ""
        _selectedLetter.value = null
        _selectedOstFranchise.value = null
        _selectedGenre.value = genre
        if (genre.isSongs && _ostSongs.value.isEmpty()) {
            viewModelScope.launch {
                _ostSongs.value = repository.getOstSongs()
            }
        }
    }

    fun clearGenre() {
        _selectedGenre.value = null
        _selectedOstFranchise.value = null
        _ostSearchQuery.value = ""
    }

    fun selectOstFranchise(franchise: OstFranchise) {
        _selectedOstFranchise.value = franchise
    }

    fun clearOstFranchise() {
        _selectedOstFranchise.value = null
    }

    fun setOstCategory(category: String) {
        _selectedOstCategory.value = category
    }

    fun setOstSearchQuery(query: String) {
        _ostSearchQuery.value = query
    }
}
