package com.chordapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import com.chordapp.repositories.CatalogDownloadManager
import com.chordapp.repositories.TierDownloadState
import com.chordapp.localization.AppLanguage
import com.chordapp.localization.AppLanguageManager
import com.chordapp.ui.theme.AppThemeMode
import com.chordapp.ui.theme.AppThemeManager

data class CatalogStats(
    val artists: Int = 0,
    val songs: Int = 0,
    val catalogBytes: Long = 0,
    val appBytes: Long = 0,
    val tierASongs: Int = 0,
    val tierBSongs: Int = 0,
    val tierCSongs: Int = 0
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

    val currentLanguage: StateFlow<AppLanguage> =
        AppLanguageManager.currentLanguage

    fun setLanguage(language: AppLanguage) {
        AppLanguageManager.setLanguage(appContext, language)
    }

    val currentTheme: StateFlow<AppThemeMode> =
        AppThemeManager.currentTheme

    fun setTheme(theme: AppThemeMode) {
        AppThemeManager.setTheme(appContext, theme)
    }

    val tierDownloadState: StateFlow<TierDownloadState> =
        CatalogDownloadManager.downloadState

    val isDownloadingCatalog: StateFlow<Boolean> = CatalogDownloadManager.downloadState
        .map { it.isDownloading }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CatalogDownloadManager.downloadState.value.isDownloading)

    val catalogDownloadProgress: StateFlow<Int> = CatalogDownloadManager.downloadState
        .map { it.progress }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CatalogDownloadManager.downloadState.value.progress)

    val catalogDownloadStatus: StateFlow<String> = CatalogDownloadManager.downloadState
        .map { it.statusText }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CatalogDownloadManager.downloadState.value.statusText)

    val catalogDownloadError: StateFlow<String?> = CatalogDownloadManager.downloadState
        .map { it.error }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CatalogDownloadManager.downloadState.value.error)

    fun downloadCatalog() {
        downloadTier("A")
    }

    fun downloadTier(tier: String) {
        com.chordapp.repositories.CatalogDownloadManager.downloadTier(appContext, tier) {
            loadCatalogStats()
        }
    }

    fun deleteTier(tier: String) {
        viewModelScope.launch {
            com.chordapp.catalog.CatalogDatabase.deleteTier(appContext, tier)
            loadCatalogStats()
        }
    }

    fun dismissCatalogError() {
        com.chordapp.repositories.CatalogDownloadManager.dismissError()
    }

    fun loadCatalogStats() {
        refreshCatalogStats()
    }

    fun refreshCatalogStats() {
        viewModelScope.launch {
            val tierA = repository.getSongCountByTier("A")
            val tierB = repository.getSongCountByTier("B")
            val tierC = repository.getSongCountByTier("C")
            _catalogStats.value = CatalogStats(
                artists = repository.getUniqueArtistCount(),
                songs = repository.getUniqueSongCount(),
                catalogBytes = appContext.getDatabasePath("catalog.db").takeIf(File::exists)?.length() ?: 0L,
                appBytes = File(appContext.applicationInfo.sourceDir).length(),
                tierASongs = tierA,
                tierBSongs = tierB,
                tierCSongs = tierC
            )
        }
    }

    val bandMode = com.chordapp.p2p.BandSessionManager.currentMode
    val deviceName = com.chordapp.p2p.BandSessionManager.deviceName
    val serverPin = com.chordapp.p2p.BandSessionManager.serverPin
    val isPinEnabled = com.chordapp.p2p.BandSessionManager.isPinEnabled
    val connectedClients = com.chordapp.p2p.BandSessionManager.connectedClients
    val webStageUrl = com.chordapp.p2p.BandSessionManager.webStageUrl
    val discoveredServers = com.chordapp.p2p.BandSessionManager.discoveredServers
    val connectionState = com.chordapp.p2p.BandSessionManager.connectionState

    fun setBandMode(mode: com.chordapp.p2p.BandMode) = com.chordapp.p2p.BandSessionManager.setMode(mode)
    fun setDeviceName(name: String) = com.chordapp.p2p.BandSessionManager.setDeviceName(name)
    fun setServerPin(pin: String) = com.chordapp.p2p.BandSessionManager.setPin(pin)
    fun setPinEnabled(enabled: Boolean) = com.chordapp.p2p.BandSessionManager.setPinEnabled(enabled)
    fun connectToBandServer(server: com.chordapp.p2p.DiscoveredServer, pin: String? = null, onFailed: (String) -> Unit = {}) =
        com.chordapp.p2p.BandSessionManager.connectToServer(server, pin, onFailed)
    fun disconnectBandClient() = com.chordapp.p2p.BandSessionManager.disconnectClient()

    init {
        loadCatalogStats()
        viewModelScope.launch {
            repository.catalogUpdateFlow.collect {
                loadCatalogStats()
            }
        }
        viewModelScope.launch {
            com.chordapp.repositories.CatalogDownloadManager.downloadState.collect { state ->
                if (!state.isDownloading && state.progress == 100) {
                    loadCatalogStats()
                }
            }
        }
    }
}
