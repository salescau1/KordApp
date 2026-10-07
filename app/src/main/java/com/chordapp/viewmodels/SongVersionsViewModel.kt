package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongVersionsViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _versions = MutableStateFlow<List<CatalogSongEntity>>(emptyList())
    val versions: StateFlow<List<CatalogSongEntity>> = _versions.asStateFlow()

    private val _filteredVersions = MutableStateFlow<List<CatalogSongEntity>>(emptyList())
    val filteredVersions: StateFlow<List<CatalogSongEntity>> = _filteredVersions.asStateFlow()

    private val _selectedVersion = MutableStateFlow<CatalogSongEntity?>(null)
    val selectedVersion: StateFlow<CatalogSongEntity?> = _selectedVersion.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sourceFilter = MutableStateFlow("All Sources")
    val sourceFilter: StateFlow<String> = _sourceFilter.asStateFlow()

    private val _navigateToPlayer = MutableStateFlow<String?>(null)
    val navigateToPlayer: StateFlow<String?> = _navigateToPlayer.asStateFlow()

    fun loadVersions(artistSlug: String, titleSlug: String): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            _isLoading.value = true
            val rawVersions = repository.getSongVersions(artistSlug, titleSlug)
        println("LOAD_VERSIONS: $artistSlug / $titleSlug -> size: ${rawVersions.size}")

            // Sort versions neutrally: source ASC, versionName ASC
            val sortedVersions = rawVersions.sortedWith(compareBy<CatalogSongEntity> { it.source }
                .thenBy { it.versionName ?: "" })

            if (sortedVersions.size == 1) {
                _navigateToPlayer.value = sortedVersions.first().sourceUrl
            } else {
                _versions.value = sortedVersions
                applyFilter("All Sources")
            }
            _isLoading.value = false
        }
    }

    fun setSourceFilter(filter: String) {
        applyFilter(filter)
    }

    private fun applyFilter(filter: String) {
        _sourceFilter.value = filter
        val currentVersions = _versions.value
        val dbSource = when (filter) {
            "Ultimate Guitar" -> "UG"
            "GuitarTuna" -> "GT"
            "ChordTela" -> "ChordTela"
            else -> filter
        }
        val filtered = if (dbSource == "All Sources") {
            currentVersions
        } else {
            currentVersions.filter { it.source == dbSource }
        }
        _filteredVersions.value = filtered

        // Automatically select the best version in the current filter if selected is no longer valid
        if (filtered.isNotEmpty() && !filtered.contains(_selectedVersion.value)) {
            _selectedVersion.value = filtered.first()
        } else if (filtered.isEmpty()) {
            _selectedVersion.value = null
        }
    }

    fun selectVersion(version: CatalogSongEntity) {
        _selectedVersion.value = version
    }

    fun clearNavigation() {
        _navigateToPlayer.value = null
    }
}
