package com.chordapp.charts

import android.content.Context

/** Persists the last chart platform without coupling HomeViewModel to Android APIs. */
interface ChartPlatformStore {
    fun read(): ChartPlatform
    fun write(platform: ChartPlatform)

    object Default : ChartPlatformStore {
        override fun read(): ChartPlatform = ChartPlatform.SPOTIFY
        override fun write(platform: ChartPlatform) = Unit
    }
}

class SharedPreferencesChartPlatformStore(context: Context) : ChartPlatformStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "chart_preferences",
        Context.MODE_PRIVATE
    )

    override fun read(): ChartPlatform = runCatching {
        ChartPlatform.valueOf(
            preferences.getString(KEY_PLATFORM, ChartPlatform.SPOTIFY.name)
                ?: ChartPlatform.SPOTIFY.name
        )
    }.getOrDefault(ChartPlatform.SPOTIFY)

    override fun write(platform: ChartPlatform) {
        preferences.edit().putString(KEY_PLATFORM, platform.name).apply()
    }

    private companion object {
        const val KEY_PLATFORM = "last_platform"
    }
}
