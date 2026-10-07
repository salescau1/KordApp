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

    companion object {
        private const val KEY_KEEP_AWAKE = "keep_screen_awake"
        private const val KEY_SCROLL_SPEED = "default_scroll_speed"
    }
}
