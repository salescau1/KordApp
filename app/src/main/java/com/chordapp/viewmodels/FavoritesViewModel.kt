package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.repositories.FavoriteSongResolution
import com.chordapp.repositories.ResolutionResult
import com.chordapp.repositories.UserdataRepository
import com.chordapp.userdata.FavoriteSongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FavoriteSort { DATE, TITLE, ARTIST }

data class FavoriteSongUi(
    val favorite: FavoriteSongEntity,
    val title: String,
    val artist: String,
    val source: String,
    val resolvedSourceUrl: String?,
    val substituted: Boolean,
    val available: Boolean
)

class FavoritesViewModel(private val repository: UserdataRepository) : ViewModel() {
    val favorites = repository.getFavoriteSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(FavoriteSort.DATE)
    val sort: StateFlow<FavoriteSort> = _sort.asStateFlow()

    private val _items = MutableStateFlow<List<FavoriteSongUi>>(emptyList())
    val items: StateFlow<List<FavoriteSongUi>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            favorites.collectLatest { stored ->
                _isLoading.value = true
                val resolved = repository.resolveFavorites(stored).map(::toUi)
                _items.value = filterAndSort(resolved, _query.value, _sort.value)
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            query.collectLatest { value ->
                val resolved = repository.resolveFavorites(favorites.value).map(::toUi)
                _items.value = filterAndSort(resolved, value, _sort.value)
            }
        }
        viewModelScope.launch {
            sort.collectLatest { value ->
                val resolved = repository.resolveFavorites(favorites.value).map(::toUi)
                _items.value = filterAndSort(resolved, _query.value, value)
            }
        }
    }

    fun setQuery(value: String) { _query.value = value }
    fun setSort(value: FavoriteSort) { _sort.value = value }

    fun remove(favorite: FavoriteSongEntity) {
        viewModelScope.launch { repository.removeFavorite(favorite.sourceUrl) }
    }

    fun restore(favorite: FavoriteSongEntity) {
        viewModelScope.launch { repository.restoreFavorite(favorite) }
    }

    private fun toUi(item: FavoriteSongResolution): FavoriteSongUi {
        val success = item.resolution as? ResolutionResult.Success
        val song = success?.song
        return FavoriteSongUi(
            favorite = item.favorite,
            title = song?.title ?: item.favorite.titleSlug.replace('-', ' '),
            artist = song?.artist ?: item.favorite.artistSlug.replace('-', ' '),
            source = song?.source ?: item.favorite.source,
            resolvedSourceUrl = song?.sourceUrl,
            substituted = success?.substituted == true,
            available = success != null
        )
    }

    private fun filterAndSort(
        items: List<FavoriteSongUi>,
        query: String,
        sort: FavoriteSort
    ): List<FavoriteSongUi> {
        val filtered = if (query.isBlank()) items else items.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
        return when (sort) {
            FavoriteSort.DATE -> filtered.sortedByDescending { it.favorite.addedAt }
            FavoriteSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            FavoriteSort.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
        }
    }
}
