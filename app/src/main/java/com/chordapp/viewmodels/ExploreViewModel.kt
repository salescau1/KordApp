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

    // Database Edit Mode State
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _deletedArtistSlugs = MutableStateFlow<Set<String>>(emptySet())
    val deletedArtistSlugs: StateFlow<Set<String>> = _deletedArtistSlugs.asStateFlow()

    private val _deletedSongs = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    val deletedSongs: StateFlow<Set<Pair<String, String>>> = _deletedSongs.asStateFlow()

    private val _deletedVersionIds = MutableStateFlow<Set<Long>>(emptySet())
    val deletedVersionIds: StateFlow<Set<Long>> = _deletedVersionIds.asStateFlow()

    // Level 2: Artist song inspection in Edit Mode
    private val _editingArtist = MutableStateFlow<CatalogArtistInfo?>(null)
    val editingArtist: StateFlow<CatalogArtistInfo?> = _editingArtist.asStateFlow()

    private val _editingArtistSongs = MutableStateFlow<Map<CatalogSongGroup, List<com.chordapp.catalog.CatalogSongEntity>>>(emptyMap())
    val editingArtistSongs: StateFlow<Map<CatalogSongGroup, List<com.chordapp.catalog.CatalogSongEntity>>> = _editingArtistSongs.asStateFlow()

    private val _isPruning = MutableStateFlow(false)
    val isPruning: StateFlow<Boolean> = _isPruning.asStateFlow()

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

    fun setEditMode(enabled: Boolean) {
        if (!enabled) {
            cancelEditMode()
        } else {
            _isEditMode.value = true
        }
    }

    fun toggleEditMode() {
        if (_isEditMode.value) {
            cancelEditMode()
        } else {
            _isEditMode.value = true
        }
    }

    fun cancelEditMode() {
        _isEditMode.value = false
        _deletedArtistSlugs.value = emptySet()
        _deletedSongs.value = emptySet()
        _deletedVersionIds.value = emptySet()
        _editingArtist.value = null
        _editingArtistSongs.value = emptyMap()
    }

    fun toggleArtistDeletion(artistSlug: String) {
        val current = _deletedArtistSlugs.value.toMutableSet()
        if (current.contains(artistSlug)) {
            current.remove(artistSlug)
        } else {
            current.add(artistSlug)
            _deletedSongs.value = _deletedSongs.value.filter { it.first != artistSlug }.toSet()
        }
        _deletedArtistSlugs.value = current
    }

    fun openArtistInEditMode(artist: CatalogArtistInfo) {
        _editingArtist.value = artist
        viewModelScope.launch {
            val songs = repository.getArtistSongs(artist.artistSlug)
            val sorted = songs.toSortedMap(compareBy { it.title.lowercase() })
            _editingArtistSongs.value = sorted
        }
    }

    fun closeArtistInEditMode() {
        _editingArtist.value = null
        _editingArtistSongs.value = emptyMap()
    }

    fun toggleSongDeletion(artistSlug: String, titleSlug: String) {
        val key = Pair(artistSlug, titleSlug)
        val current = _deletedSongs.value.toMutableSet()
        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
            val songVersions = _editingArtistSongs.value.entries.find { it.key.titleSlug == titleSlug }?.value ?: emptyList()
            val versionIds = songVersions.mapNotNull { it.id }.toSet()
            _deletedVersionIds.value = _deletedVersionIds.value - versionIds
        }
        _deletedSongs.value = current
    }

    fun toggleVersionDeletion(versionId: Long) {
        val current = _deletedVersionIds.value.toMutableSet()
        if (current.contains(versionId)) {
            current.remove(versionId)
        } else {
            current.add(versionId)
        }
        _deletedVersionIds.value = current
    }

    fun applyDeletions(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isPruning.value = true
            repository.pruneDatabase(
                artistSlugs = _deletedArtistSlugs.value.toList(),
                songKeys = _deletedSongs.value.toList(),
                versionIds = _deletedVersionIds.value.toList()
            )
            _artists.value = repository.getAllArtists()
            cancelEditMode()
            _isPruning.value = false
            onComplete()
        }
    }
}
