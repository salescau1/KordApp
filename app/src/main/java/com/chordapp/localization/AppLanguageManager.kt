package com.chordapp.localization

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.chordapp.settings.PlayerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun initialize(context: Context) {
        val prefs = PlayerPreferences(context)
        _currentLanguage.value = AppLanguage.fromCode(prefs.appLanguage)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        val prefs = PlayerPreferences(context)
        prefs.appLanguage = language.code
        _currentLanguage.value = language
    }

    fun strings(language: AppLanguage = _currentLanguage.value): AppStrings {
        return when (language) {
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.INDONESIAN -> IndonesianStrings
        }
    }
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
