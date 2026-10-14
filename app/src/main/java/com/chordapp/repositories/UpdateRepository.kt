package com.chordapp.repositories

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val downloadUrl: String,
    val changelog: String,
    val isForceUpdate: Boolean
)

object UpdateRepository {

    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/salescau1/KordApp-Release/releases/latest"

    /**
     * Membandingkan versi semver sederhana (misal 1.1.0 > 1.0, 1.1.1 > 1.1).
     */
    fun isNewerVersion(latest: String, current: String): Boolean {
        val lParts = latest.trimStart('v', 'V').split('.').mapNotNull { it.toIntOrNull() }
        val cParts = current.trimStart('v', 'V').split('.').mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(lParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * Memeriksa pembaruan versi APK ke GitHub Releases.
     */
    suspend fun checkAppUpdate(context: Context): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            val currentCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }
            val currentName = pInfo.versionName ?: "1.0"

            val url = URL(GITHUB_LATEST_RELEASE_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "KordApp-Android")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
            }

            if (conn.responseCode !in 200..299) {
                return@runCatching AppUpdateInfo(
                    hasUpdate = false,
                    latestVersionCode = currentCode,
                    latestVersionName = currentName,
                    downloadUrl = "",
                    changelog = "",
                    isForceUpdate = false
                )
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = JSONObject(body)

            val tagName = releaseObj.optString("tag_name", "").trim()
            val latestVersionName = tagName.trimStart('v', 'V').ifBlank { currentName }
            val changelog = releaseObj.optString("body", "Peningkatan performa dan perbaikan bug.")

            var downloadUrl = ""
            val assetsArray = releaseObj.optJSONArray("assets") ?: JSONArray()
            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }

            val hasUpdate = isNewerVersion(latestVersionName, currentName) && downloadUrl.isNotBlank()

            AppUpdateInfo(
                hasUpdate = hasUpdate,
                latestVersionCode = currentCode + (if (hasUpdate) 1 else 0),
                latestVersionName = latestVersionName,
                downloadUrl = downloadUrl,
                changelog = changelog,
                isForceUpdate = false
            )
        }
    }

    /**
     * Mendownload file APK pembaruan dan otomatis memicu intent instalasi bawaan Android setelah selesai.
     */
    fun startDownloadAndInstall(context: Context, downloadUrl: String, fileName: String = "KordApp-update.apk") {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(downloadUrl)

            val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destFile.exists()) {
                destFile.delete()
            }

            val request = DownloadManager.Request(uri).apply {
                setTitle("Mengunduh Pembaruan KordApp")
                setDescription("Sedang mengunduh versi terbaru...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            ctxt.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        installApk(ctxt, destFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    /**
     * Memicu dialog install package bawaan Android melalui FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }

    const val DEFAULT_CATALOG_URL =
        "https://github.com/salescau1/KordApp-Release/releases/download/v1.1.0/catalog.db.xz"

    /**
     * Mengunduh catalog.db.xz dari internet secara streaming dengan pelaporan progress,
     * lalu mengekstraknya langsung ke database catalog.db.
     */
    suspend fun downloadAndInstallCatalog(
        context: Context,
        catalogUrl: String = DEFAULT_CATALOG_URL,
        onProgress: (percent: Int, statusText: String) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(0, "Menghubungkan ke server...")
            val destFile = File(context.cacheDir, "catalog_downloaded.db.xz")
            if (destFile.exists()) destFile.delete()

            val url = URL(catalogUrl)
            var conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            var responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                val newUrl = conn.getHeaderField("Location")
                if (!newUrl.isNullOrBlank()) {
                    conn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 30000
                    }
                    responseCode = conn.responseCode
                }
            }

            if (responseCode !in 200..299) {
                error("Gagal mengunduh file: HTTP $responseCode")
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0 } ?: 83_726_260L
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    var lastPercent = 0
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val percent = ((downloadedBytes * 85) / totalBytes).toInt().coerceIn(0, 85)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            val mbDownloaded = downloadedBytes / (1024 * 1024)
                            val mbTotal = totalBytes / (1024 * 1024)
                            onProgress(percent, "Mengunduh database: $mbDownloaded MB / $mbTotal MB")
                        }
                    }
                }
            }

            onProgress(88, "Mengekstrak 158.000 lagu ke penyimpanan...")
            val success = com.chordapp.catalog.CatalogDatabase.importDatabaseFromXz(context, destFile)
            destFile.delete()

            if (!success) {
                error("Gagal mengekstrak database")
            }

            onProgress(100, "Selesai! 158.000 lagu siap dimainkan offline.")
            true
        }
    }

    fun getTierCatalogUrl(tier: String): String {
        val fileName = when (tier.uppercase()) {
            "A" -> "catalog_a.db.xz"
            "B" -> "catalog_b.db.xz"
            "C" -> "catalog_c.db.xz"
            else -> "catalog.db.xz"
        }
        return "https://github.com/salescau1/KordApp-Release/releases/download/v1.3.0/$fileName"
    }

    /**
     * Mengunduh dan memasang satu paket tertentu (A, B, atau C) ke katalog aktif secara modular.
     */
    suspend fun downloadAndInstallTier(
        context: Context,
        tier: String,
        onProgress: (percent: Int, statusText: String) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val urlString = getTierCatalogUrl(tier)
        val tierUpper = tier.uppercase()
        val tierLabel = when (tierUpper) {
            "A" -> "Paket A (79k Lagu)"
            "B" -> "Paket B (47k Lagu)"
            "C" -> "Paket C (32k Lagu)"
            else -> "Katalog"
        }

        runCatching {
            onProgress(0, "Menghubungkan ke server $tierLabel...")
            val destFile = File(context.cacheDir, "catalog_${tierUpper.lowercase()}.db.xz")
            if (destFile.exists()) destFile.delete()

            val url = URL(urlString)
            var conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            var responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                val newUrl = conn.getHeaderField("Location")
                if (!newUrl.isNullOrBlank()) {
                    conn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 30000
                    }
                    responseCode = conn.responseCode
                }
            }

            if (responseCode !in 200..299) {
                error("Gagal mengunduh $tierLabel: HTTP $responseCode")
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0 } ?: when (tierUpper) {
                "A" -> 40_000_000L
                "B" -> 25_000_000L
                "C" -> 16_000_000L
                else -> 84_000_000L
            }
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    var lastPercent = 0
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val percent = ((downloadedBytes * 85) / totalBytes).toInt().coerceIn(0, 85)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            val mbDownloaded = downloadedBytes / (1024 * 1024)
                            val mbTotal = totalBytes / (1024 * 1024)
                            onProgress(percent, "Mengunduh $tierLabel: $mbDownloaded MB / $mbTotal MB")
                        }
                    }
                }
            }

            onProgress(88, "Memasang $tierLabel ke database...")
            val success = com.chordapp.catalog.CatalogDatabase.importTierDatabaseFromXz(context, destFile, tierUpper)
            destFile.delete()

            if (!success) {
                error("Gagal memasang $tierLabel")
            }

            onProgress(100, "Selesai! $tierLabel siap dimainkan offline.")
            true
        }
    }
}
