package com.chordapp.about

import android.content.Context
import android.os.Build
import java.nio.charset.StandardCharsets

const val DEFAULT_ABOUT_TEXT = "KordApp adalah aplikasi chord & lirik offline untuk musisi. Dikembangkan oleh g0dzella di bawah bendera kekalclip."

fun bundledAboutText(context: Context): String = runCatching {
    context.assets.open("about_fallback.txt").bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        .trim()
        .takeIf(String::isNotEmpty)
}.getOrNull() ?: DEFAULT_ABOUT_TEXT

data class AppMetadata(
    val name: String,
    val versionName: String,
    val versionCode: Long
)

fun runtimeAppMetadata(context: Context): AppMetadata {
    val appContext = context.applicationContext
    val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    val name = appContext.applicationInfo.loadLabel(appContext.packageManager).toString()
    return AppMetadata(
        name = name,
        versionName = packageInfo.versionName ?: "Unknown",
        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    )
}
