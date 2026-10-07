package com.chordapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CatalogStats(
    val artists: Int = 0,
    val songs: Int = 0,
    val catalogBytes: Long = 0,
    val appBytes: Long = 0
)

class SettingsViewModel(
    context: Context,
    private val repository: CatalogRepository
) : ViewModel() {
    private val appContext = context.applicationContext

    private val _catalogStats = MutableStateFlow(CatalogStats())
    val catalogStats: StateFlow<CatalogStats> = _catalogStats.asStateFlow()

    val versionName: String = appContext.packageManager
        .getPackageInfo(appContext.packageName, 0)
        .versionName ?: "Unknown"

    val currentLanguage: StateFlow<com.chordapp.localization.AppLanguage> =
        com.chordapp.localization.AppLanguageManager.currentLanguage

    fun setLanguage(language: com.chordapp.localization.AppLanguage) {
        com.chordapp.localization.AppLanguageManager.setLanguage(appContext, language)
    }

    init {
        viewModelScope.launch {
            _catalogStats.value = CatalogStats(
                artists = repository.getUniqueArtistCount(),
                songs = repository.getUniqueSongCount(),
                catalogBytes = appContext.getDatabasePath("catalog.db").takeIf(File::exists)?.length() ?: 0L,
                appBytes = File(appContext.applicationInfo.sourceDir).length()
            )
        }
    }
}
