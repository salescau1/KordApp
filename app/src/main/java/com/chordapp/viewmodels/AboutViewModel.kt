package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.about.AboutRepository
import com.chordapp.about.AppMetadata
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutViewModel(
    val metadata: AppMetadata,
    private val repository: AboutRepository,
    private val refreshDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _content = MutableStateFlow(repository.initialContent())
    val content: StateFlow<String> = _content.asStateFlow()

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch(refreshDispatcher) {
            repository.refresh().onSuccess { _content.value = it }
            _isRefreshing.value = false
        }
    }
}
