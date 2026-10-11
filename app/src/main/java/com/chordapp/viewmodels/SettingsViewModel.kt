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

    val currentTheme: StateFlow<com.chordapp.ui.theme.AppThemeMode> =
        com.chordapp.ui.theme.AppThemeManager.currentTheme

    fun setTheme(theme: com.chordapp.ui.theme.AppThemeMode) {
        com.chordapp.ui.theme.AppThemeManager.setTheme(appContext, theme)
    }

    private val _isDownloadingCatalog = MutableStateFlow(false)
    val isDownloadingCatalog: StateFlow<Boolean> = _isDownloadingCatalog.asStateFlow()

    private val _catalogDownloadProgress = MutableStateFlow(0)
    val catalogDownloadProgress: StateFlow<Int> = _catalogDownloadProgress.asStateFlow()

    private val _catalogDownloadStatus = MutableStateFlow("")
    val catalogDownloadStatus: StateFlow<String> = _catalogDownloadStatus.asStateFlow()

    private val _catalogDownloadError = MutableStateFlow<String?>(null)
    val catalogDownloadError: StateFlow<String?> = _catalogDownloadError.asStateFlow()

    fun downloadCatalog() {
        if (_isDownloadingCatalog.value) return
        viewModelScope.launch {
            _isDownloadingCatalog.value = true
            _catalogDownloadError.value = null
            _catalogDownloadProgress.value = 0
            _catalogDownloadStatus.value = "Menghubungkan..."

            val result = com.chordapp.repositories.UpdateRepository.downloadAndInstallCatalog(
                context = appContext,
                onProgress = { percent, status ->
                    _catalogDownloadProgress.value = percent
                    _catalogDownloadStatus.value = status
                }
            )

            result.onSuccess {
                refreshCatalogStats()
            }.onFailure { err ->
                _catalogDownloadError.value = err.localizedMessage ?: "Gagal mengunduh katalog"
            }

            _isDownloadingCatalog.value = false
        }
    }

    fun dismissCatalogError() {
        _catalogDownloadError.value = null
    }

    fun refreshCatalogStats() {
        viewModelScope.launch {
            _catalogStats.value = CatalogStats(
                artists = repository.getUniqueArtistCount(),
                songs = repository.getUniqueSongCount(),
                catalogBytes = appContext.getDatabasePath("catalog.db").takeIf(File::exists)?.length() ?: 0L,
                appBytes = File(appContext.applicationInfo.sourceDir).length()
            )
        }
    }

    val bandMode = com.chordapp.p2p.BandSessionManager.currentMode
    val deviceName = com.chordapp.p2p.BandSessionManager.deviceName
    val serverPin = com.chordapp.p2p.BandSessionManager.serverPin
    val isPinEnabled = com.chordapp.p2p.BandSessionManager.isPinEnabled
    val connectedClients = com.chordapp.p2p.BandSessionManager.connectedClients
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
        refreshCatalogStats()
    }
}
