package com.chordapp.ui.theme

import android.content.Context
import com.chordapp.settings.PlayerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val code: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    companion object {
        fun fromCode(code: String): AppThemeMode {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DARK
        }
    }
}

object AppThemeManager {
    private val _currentTheme = MutableStateFlow(AppThemeMode.DARK)
    val currentTheme: StateFlow<AppThemeMode> = _currentTheme.asStateFlow()

    fun initialize(context: Context) {
        val prefs = PlayerPreferences(context)
        _currentTheme.value = AppThemeMode.fromCode(prefs.appTheme)
    }

    fun setTheme(context: Context, mode: AppThemeMode) {
        val prefs = PlayerPreferences(context)
        prefs.appTheme = mode.code
        _currentTheme.value = mode
    }
}
