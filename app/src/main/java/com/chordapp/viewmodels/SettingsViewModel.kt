package com.chordapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.repositories.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import com.chordapp.catalog.CatalogDatabase
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
    companion object {
        private val _sharedCatalogStats = MutableStateFlow(CatalogStats())
        @Volatile private var hasInitialStatsLoaded = false
    }

    private val appContext = context.applicationContext

    val catalogStats: StateFlow<CatalogStats> = _sharedCatalogStats.asStateFlow()

    val versionName: String = appContext.packageManager
        .getPackageInfo(appContext.packageName, 0)
        .versionName ?: "Unknown"

    val userCatalogRepository = repository.userCatalogRepo ?: com.chordapp.usercatalog.UserCatalogRepository(appContext)

    val scrapedCount: StateFlow<Int> = userCatalogRepository.getScrapedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun clearAllScraped() {
        viewModelScope.launch(Dispatchers.IO) {
            userCatalogRepository.clearAllScraped()
            repository.notifyCatalogUpdated()
        }
    }

    fun exportScraped(onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songs = userCatalogRepository.getAllScrapedSongsList()
                if (songs.isEmpty()) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onError("Katalog Scraped kosong, tidak ada lagu untuk diekspor")
                    }
                    return@launch
                }
                val file = com.chordapp.usercatalog.KordPackageSerializer.exportScrapedToFile(appContext, songs)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onSuccess(file)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Gagal mengekspor file .kord")
                }
            }
        }
    }

    fun importScrapedFromUri(uri: android.net.Uri, onResult: (Int, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = appContext.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                if (content.isNullOrBlank()) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onResult(0, "File kosong atau tidak dapat dibaca")
                    }
                    return@launch
                }
                val songs = com.chordapp.usercatalog.KordPackageSerializer.deserializeScrapedSongs(content)
                if (songs.isEmpty()) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onResult(0, "Tidak ada lagu yang ditemukan dalam file")
                    }
                    return@launch
                }
                userCatalogRepository.insertScrapedSongs(songs.map { it.toCatalogSongEntity() })
                repository.notifyCatalogUpdated()
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResult(songs.size, null)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResult(0, e.localizedMessage ?: "Format file .kord tidak valid")
                }
            }
        }
    }

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
        CatalogDownloadManager.downloadTier(appContext, tier) {
            refreshCatalogStats(force = true)
        }
    }

    fun deleteTier(tier: String) {
        viewModelScope.launch {
            val success = CatalogDatabase.deleteTier(appContext, tier)
            if (success) {
                refreshCatalogStats(force = true)
            }
        }
    }

    fun dismissCatalogError() {
        CatalogDownloadManager.dismissError()
    }

    fun loadCatalogStats(force: Boolean = false) {
        refreshCatalogStats(force)
    }

    fun refreshCatalogStats(force: Boolean = false) {
        if (!force && hasInitialStatsLoaded && _sharedCatalogStats.value.songs > 0) {
            // Sudah ada di memory cache, tidak perlu query ulang agar tidak ada transisi visual
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val tierA = repository.getSongCountByTier("A")
            val tierB = repository.getSongCountByTier("B")
            val tierC = repository.getSongCountByTier("C")
            val artists = repository.getUniqueArtistCount()
            val songs = repository.getUniqueSongCount()
            val catalogBytes = appContext.getDatabasePath("catalog.db").takeIf(File::exists)?.length() ?: 0L
            val appBytes = File(appContext.applicationInfo.sourceDir).length()

            _sharedCatalogStats.value = CatalogStats(
                artists = artists,
                songs = songs,
                catalogBytes = catalogBytes,
                appBytes = appBytes,
                tierASongs = tierA,
                tierBSongs = tierB,
                tierCSongs = tierC
            )
            hasInitialStatsLoaded = true
        }
    }

    val bandMode = com.chordapp.p2p.BandSessionManager.currentMode
    val deviceName = com.chordapp.p2p.BandSessionManager.deviceName
    val serverPin = com.chordapp.p2p.BandSessionManager.serverPin
    val isPinEnabled = com.chordapp.p2p.BandSessionManager.isPinEnabled
    val connectedClients = com.chordapp.p2p.BandSessionManager.connectedClients
    val webStageUrl = com.chordapp.p2p.BandSessionManager.webStageUrl
    val webStageIpUrl = com.chordapp.p2p.BandSessionManager.webStageIpUrl
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
        loadCatalogStats(force = false)
        viewModelScope.launch {
            repository.catalogUpdateFlow.collect {
                refreshCatalogStats(force = true)
            }
        }
        viewModelScope.launch {
            CatalogDownloadManager.downloadState.collect { state ->
                if (!state.isDownloading && state.progress == 100) {
                    refreshCatalogStats(force = true)
                }
            }
        }
    }
}
