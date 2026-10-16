package com.chordapp.repositories

import android.content.Context
import android.util.Log
import com.chordapp.catalog.CatalogDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TierDownloadState(
    val isDownloading: Boolean = false,
    val activeTier: String? = null, // "A", "B", "C", atau null
    val progress: Int = 0,
    val statusText: String = "",
    val error: String? = null
)

object CatalogDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadState = MutableStateFlow(TierDownloadState())
    val downloadState: StateFlow<TierDownloadState> = _downloadState.asStateFlow()

    fun downloadTier(context: Context, tier: String, onFinished: (() -> Unit)? = null) {
        if (_downloadState.value.isDownloading) return
        val appContext = context.applicationContext
        val tierUpper = tier.uppercase()

        _downloadState.value = TierDownloadState(
            isDownloading = true,
            activeTier = tierUpper,
            progress = 0,
            statusText = "Menghubungkan...",
            error = null
        )

        scope.launch {
            val result = UpdateRepository.downloadAndInstallTier(
                context = appContext,
                tier = tierUpper,
                onProgress = { percent, status ->
                    _downloadState.value = _downloadState.value.copy(
                        progress = percent,
                        statusText = status
                    )
                }
            )

            result.onSuccess {
                CatalogDatabase.notifyDatabaseReloaded(appContext)
                _downloadState.value = TierDownloadState(
                    isDownloading = false,
                    activeTier = null,
                    progress = 100,
                    statusText = "Selesai",
                    error = null
                )
                onFinished?.invoke()
            }.onFailure { err ->
                Log.e("CatalogDownloadManager", "Download tier $tierUpper failed", err)
                _downloadState.value = TierDownloadState(
                    isDownloading = false,
                    activeTier = null,
                    progress = 0,
                    statusText = "",
                    error = err.localizedMessage ?: "Gagal mengunduh paket $tierUpper"
                )
            }
        }
    }

    fun dismissError() {
        _downloadState.value = _downloadState.value.copy(error = null)
    }
}
