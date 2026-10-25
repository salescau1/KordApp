package com.chordapp.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CustomPlayerConfig(
    val enabled: Boolean = false,
    val chordHue: Float = 48f,            // 0..360 (Kuning Emas)
    val chordBrightness: Float = 0.95f,   // 0..1
    val lyricHue: Float = 210f,           // 0..360 (Putih Tulang)
    val lyricBrightness: Float = 0.95f,   // 0..1
    val sectionCustom: Boolean = false,   // false = sama dengan lirik
    val sectionHue: Float = 48f,
    val sectionBrightness: Float = 0.95f,
    val shadowEnabled: Boolean = true,
    val shadowLevel: String = "medium",   // "thin", "medium", "thick"
    val outlineEnabled: Boolean = false,
    val outlineLevel: String = "medium",  // "thin", "medium", "thick"
    val bgEnabled: Boolean = false,
    val bgPath: String? = null,
    val bgDimmer: Float = 0.45f,          // 0.0 .. 0.90
    val textureEnabled: Boolean = false,
    val textureType: String = "paper"     // "paper", "canvas", "halftone"
) {
    fun getChordColor(): Color = hslToColor(chordHue, chordBrightness)
    fun getLyricColor(): Color = hslToColor(lyricHue, lyricBrightness)
    fun getSectionColor(): Color = if (sectionCustom) hslToColor(sectionHue, sectionBrightness) else getLyricColor()

    companion object {
        fun hslToColor(hue: Float, brightness: Float): Color {
            val h = (hue % 360f + 360f) % 360f
            val l = brightness.coerceIn(0f, 1f)
            val s = if (l >= 0.90f) {
                ((1f - (l - 0.90f) / 0.10f) * 0.95f).coerceIn(0f, 1f)
            } else {
                0.95f
            }
            val c = (1f - abs(2f * l - 1f)) * s
            val x = c * (1f - abs((h / 60f) % 2f - 1f))
            val m = l - c / 2f
            val (r1, g1, b1) = when {
                h < 60f -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return Color(
                red = (r1 + m).coerceIn(0f, 1f),
                green = (g1 + m).coerceIn(0f, 1f),
                blue = (b1 + m).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
    }
}

object CustomPlayerManager {
    private val _config = MutableStateFlow(CustomPlayerConfig())
    val config: StateFlow<CustomPlayerConfig> = _config.asStateFlow()

    fun initialize(context: Context) {
        val prefs = PlayerPreferences(context)
        _config.value = prefs.customPlayerConfig
    }

    fun updateConfig(context: Context, newConfig: CustomPlayerConfig) {
        val prefs = PlayerPreferences(context)
        prefs.customPlayerConfig = newConfig
        _config.value = newConfig
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val updated = _config.value.copy(enabled = enabled)
        updateConfig(context, updated)
    }
}
