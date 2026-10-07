package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.repositories.ResolutionResult
import com.chordapp.repositories.SetlistItemResolution
import com.chordapp.repositories.UserdataRepository
import com.chordapp.userdata.SetlistEntity
import com.chordapp.userdata.SetlistItemEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Collections

data class SetlistTrackUi(
    val stored: SetlistItemEntity,
    val title: String,
    val artist: String,
    val source: String,
    val resolvedSourceUrl: String?,
    val substituted: Boolean,
    val available: Boolean
)

class SetlistTracklistViewModel(private val repository: UserdataRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<SetlistTrackUi>>(emptyList())
    val items: StateFlow<List<SetlistTrackUi>> = _items.asStateFlow()

    private val _setlist = MutableStateFlow<SetlistEntity?>(null)
    val setlist: StateFlow<SetlistEntity?> = _setlist.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentSetlistId: Long = -1
    private var observation: Job? = null

    fun loadSetlist(id: Long) {
        if (currentSetlistId == id && observation?.isActive == true) return
        currentSetlistId = id
        observation?.cancel()
        observation = viewModelScope.launch {
            _isLoading.value = true
            _setlist.value = repository.getSetlist(id)
            repository.getSetlistItems(id).collectLatest { stored ->
                _items.value = repository.resolveSetlistItems(stored).map(::toUi)
                _isLoading.value = false
            }
        }
    }

    fun renameSetlist(name: String) {
        val current = _setlist.value ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank() || trimmed == current.name) return
        viewModelScope.launch {
            repository.renameSetlist(current, trimmed)
            _setlist.value = current.copy(name = trimmed)
        }
    }

    fun deleteSetlist(onDeleted: () -> Unit) {
        val current = _setlist.value ?: return
        viewModelScope.launch {
            repository.deleteSetlist(current)
            onDeleted()
        }
    }

    fun deleteItem(item: SetlistItemEntity) {
        viewModelScope.launch { repository.removeSongFromSetlist(item) }
    }

    fun restoreItem(item: SetlistItemEntity) {
        viewModelScope.launch { repository.restoreSongToSetlist(item) }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val stored = _items.value.map { it.stored }.toMutableList()
        if (fromIndex !in stored.indices || toIndex !in stored.indices || fromIndex == toIndex) return
        Collections.swap(stored, fromIndex, toIndex)
        viewModelScope.launch { repository.reorderSetlistItems(stored) }
    }

    private fun toUi(item: SetlistItemResolution): SetlistTrackUi {
        val success = item.resolution as? ResolutionResult.Success
        val song = success?.song
        return SetlistTrackUi(
            stored = item.item,
            title = song?.title ?: item.item.titleSlug.replace('-', ' '),
            artist = song?.artist ?: item.item.artistSlug.replace('-', ' '),
            source = song?.source ?: item.item.source,
            resolvedSourceUrl = song?.sourceUrl,
            substituted = success?.substituted == true,
            available = success != null
        )
    }
}
