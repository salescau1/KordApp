package com.chordapp

import android.content.Intent
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
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {
            finish()
            return
        }
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
            val currentDisplay = display ?: return
            val currentMode = currentDisplay.mode ?: return
            // Pastikan hanya memilih mode dengan resolusi yang SAMA persis dengan resolusi aktif saat ini
            // agar TIDAK memaksa layar berpindah resolusi (misal dari 1080p ke 1440p di Google Pixel / Samsung)
            val bestMode = currentDisplay.supportedModes
                .filter { mode ->
                    (mode.physicalWidth == currentMode.physicalWidth && mode.physicalHeight == currentMode.physicalHeight) ||
                    (mode.physicalWidth == currentMode.physicalHeight && mode.physicalHeight == currentMode.physicalWidth)
                }
                .maxByOrNull { it.refreshRate }

            if (bestMode != null && bestMode.refreshRate > currentMode.refreshRate) {
                val params = window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                window.attributes = params
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val defaultDisplay = windowManager.defaultDisplay ?: return
            val currentMode = defaultDisplay.mode ?: return
            val bestMode = defaultDisplay.supportedModes
                .filter { mode ->
                    (mode.physicalWidth == currentMode.physicalWidth && mode.physicalHeight == currentMode.physicalHeight) ||
                    (mode.physicalWidth == currentMode.physicalHeight && mode.physicalHeight == currentMode.physicalWidth)
                }
                .maxByOrNull { it.refreshRate }

            if (bestMode != null && bestMode.refreshRate > currentMode.refreshRate) {
                val params = window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                window.attributes = params
            }
        }
    }
}
