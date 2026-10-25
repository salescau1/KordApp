package com.chordapp.settings

import android.content.Context

class PlayerPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("player_preferences", Context.MODE_PRIVATE)

    var keepScreenAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_AWAKE, true)
        set(value) { prefs.edit().putBoolean(KEY_KEEP_AWAKE, value).apply() }

    var defaultScrollSpeed: Float
        get() = prefs.getFloat(KEY_SCROLL_SPEED, 1f).coerceIn(0.5f, 4f)
        set(value) { prefs.edit().putFloat(KEY_SCROLL_SPEED, value.coerceIn(0.5f, 4f)).apply() }

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"
        set(value) { prefs.edit().putString(KEY_APP_LANGUAGE, value).apply() }

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "dark") ?: "dark"
        set(value) { prefs.edit().putString(KEY_APP_THEME, value).apply() }

    var customPlayerConfig: CustomPlayerConfig
        get() = CustomPlayerConfig(
            enabled = prefs.getBoolean("cp_enabled", false),
            chordHue = prefs.getFloat("cp_chord_hue", 48f),
            chordBrightness = prefs.getFloat("cp_chord_brightness", 0.95f),
            lyricHue = prefs.getFloat("cp_lyric_hue", 210f),
            lyricBrightness = prefs.getFloat("cp_lyric_brightness", 0.95f),
            sectionCustom = prefs.getBoolean("cp_section_custom", false),
            sectionHue = prefs.getFloat("cp_section_hue", 48f),
            sectionBrightness = prefs.getFloat("cp_section_brightness", 0.95f),
            shadowEnabled = prefs.getBoolean("cp_shadow_enabled", true),
            shadowLevel = prefs.getString("cp_shadow_level", "medium") ?: "medium",
            outlineEnabled = prefs.getBoolean("cp_outline_enabled", false),
            outlineLevel = prefs.getString("cp_outline_level", "medium") ?: "medium",
            bgEnabled = prefs.getBoolean("cp_bg_enabled", false),
            bgPath = prefs.getString("cp_bg_path", null),
            bgDimmer = prefs.getFloat("cp_bg_dimmer", 0.45f),
            textureEnabled = prefs.getBoolean("cp_texture_enabled", false),
            textureType = prefs.getString("cp_texture_type", "paper") ?: "paper"
        )
        set(value) {
            prefs.edit()
                .putBoolean("cp_enabled", value.enabled)
                .putFloat("cp_chord_hue", value.chordHue)
                .putFloat("cp_chord_brightness", value.chordBrightness)
                .putFloat("cp_lyric_hue", value.lyricHue)
                .putFloat("cp_lyric_brightness", value.lyricBrightness)
                .putBoolean("cp_section_custom", value.sectionCustom)
                .putFloat("cp_section_hue", value.sectionHue)
                .putFloat("cp_section_brightness", value.sectionBrightness)
                .putBoolean("cp_shadow_enabled", value.shadowEnabled)
                .putString("cp_shadow_level", value.shadowLevel)
                .putBoolean("cp_outline_enabled", value.outlineEnabled)
                .putString("cp_outline_level", value.outlineLevel)
                .putBoolean("cp_bg_enabled", value.bgEnabled)
                .putString("cp_bg_path", value.bgPath)
                .putFloat("cp_bg_dimmer", value.bgDimmer)
                .putBoolean("cp_texture_enabled", value.textureEnabled)
                .putString("cp_texture_type", value.textureType)
                .apply()
        }

    var dontShowUpdateAgain: Boolean
        get() = prefs.getBoolean(KEY_DONT_SHOW_UPDATE, false)
        set(value) { prefs.edit().putBoolean(KEY_DONT_SHOW_UPDATE, value).apply() }

    var lastDismissedUpdateVersion: String
        get() = prefs.getString(KEY_DISMISSED_UPDATE_VERSION, "") ?: ""
        set(value) { prefs.edit().putString(KEY_DISMISSED_UPDATE_VERSION, value).apply() }

    companion object {
        private const val KEY_KEEP_AWAKE = "keep_screen_awake"
        private const val KEY_SCROLL_SPEED = "default_scroll_speed"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_APP_THEME = "app_theme"
        const val KEY_DONT_SHOW_UPDATE = "dont_show_update_again"
        const val KEY_DISMISSED_UPDATE_VERSION = "dismissed_update_version"
    }
}
