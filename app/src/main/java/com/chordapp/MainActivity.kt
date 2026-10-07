package com.chordapp

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.chordapp.localization.AppLanguageManager
import com.chordapp.localization.LocalAppStrings
import com.chordapp.ui.navigation.AppNavigation
import com.chordapp.ui.theme.AppThemeManager
import com.chordapp.ui.theme.ChordAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        optimizeRefreshRate()
        AppLanguageManager.initialize(this)
        AppThemeManager.initialize(this)
        setContent {
            val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
            val currentTheme by AppThemeManager.currentTheme.collectAsState()
            val strings = remember(currentLanguage) { AppLanguageManager.strings(currentLanguage) }

            CompositionLocalProvider(LocalAppStrings provides strings) {
                ChordAppTheme(themeMode = currentTheme) {
                    AppNavigation()
                }
            }
        }
    }

    private fun optimizeRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val currentDisplay = display
            if (currentDisplay != null) {
                val maxMode = currentDisplay.supportedModes.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate > 60f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val defaultDisplay = windowManager.defaultDisplay
            if (defaultDisplay != null) {
                val maxMode = defaultDisplay.supportedModes.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate > 60f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            }
        }
    }
}
