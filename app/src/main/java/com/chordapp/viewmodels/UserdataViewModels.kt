package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.repositories.UserdataRepository
import com.chordapp.userdata.SetlistEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SetlistSort { UPDATED, NAME }

class SetlistsViewModel(private val repository: UserdataRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SetlistSort.UPDATED)

    val queryText = query
    val sortMode = sort

    val setlists = combine(repository.getAllSetlists(), query, sort) { lists, text, mode ->
        val filtered = if (text.isBlank()) lists else lists.filter {
            it.name.contains(text.trim(), ignoreCase = true)
        }
        when (mode) {
            SetlistSort.UPDATED -> filtered.sortedByDescending(SetlistEntity::updatedAt)
            SetlistSort.NAME -> filtered.sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun setSort(value: SetlistSort) {
        sort.value = value
    }

    fun createSetlist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { repository.createSetlist(trimmed) }
    }

    fun renameSetlist(setlist: SetlistEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { repository.renameSetlist(setlist, trimmed) }
    }

    fun deleteSetlist(setlist: SetlistEntity) {
        viewModelScope.launch { repository.deleteSetlist(setlist) }
    }
}
