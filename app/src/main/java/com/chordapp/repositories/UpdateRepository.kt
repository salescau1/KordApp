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

    private const val SUPABASE_URL = "https://rhgcsgrdqbhimpdloxgo.supabase.co/rest/v1"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJoZ2NzZ3JkcWJoaW1wZGxveGdvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEzMDAzNjksImV4cCI6MjEwNjg3NjM2OX0.JKlFUjdom0M0WV62xYvCn9vCq23O9UPisXdPdCCHGtk"

    /**
     * Memeriksa pembaruan versi APK ke tabel app_updates di Supabase.
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

            val url = URL("$SUPABASE_URL/app_updates?select=*&order=latest_app_version_code.desc&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
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
            val array = JSONArray(body)
            if (array.length() == 0) {
                return@runCatching AppUpdateInfo(
                    hasUpdate = false,
                    latestVersionCode = currentCode,
                    latestVersionName = currentName,
                    downloadUrl = "",
                    changelog = "",
                    isForceUpdate = false
                )
            }

            val item = array.getJSONObject(0)
            val latestCode = item.optInt("latest_app_version_code", currentCode)
            val latestName = item.optString("latest_app_version_name", currentName)
            val downloadUrl = item.optString("app_download_url", "")
            val changelog = item.optString("app_changelog", "Peningkatan performa dan perbaikan bug.")
            val isForce = item.optBoolean("is_force_app_update", false)

            val hasUpdate = latestCode > currentCode && downloadUrl.isNotBlank()

            AppUpdateInfo(
                hasUpdate = hasUpdate,
                latestVersionCode = latestCode,
                latestVersionName = latestName,
                downloadUrl = downloadUrl,
                changelog = changelog,
                isForceUpdate = isForce
            )
        }
    }

    /**
     * Mendownload file APK pembaruan dan otomatis memicu intent instalasi bawaan Android setelah selesai.
     */
    fun startDownloadAndInstall(context: Context, downloadUrl: String, fileName: String = "KordApp-update.apk") {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)

        // Hapus file lama jika ada di folder Download publik
        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destFile.exists()) {
            destFile.delete()
        }

        val request = DownloadManager.Request(uri).apply {
            setTitle("Mengunduh Pembaruan KordApp")
            setDescription("Sedang mengunduh versi terbaru...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destFile))
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

            onProgress(88, "Mengekstrak 157.000 lagu ke penyimpanan...")
            val success = com.chordapp.catalog.CatalogDatabase.importDatabaseFromXz(context, destFile)
            destFile.delete()

            if (!success) {
                error("Gagal mengekstrak database")
            }

            onProgress(100, "Selesai! 157.000 lagu siap dimainkan offline.")
            true
        }
    }
}
