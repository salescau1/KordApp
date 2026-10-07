package com.chordapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ChordAppColors(
    val background: Color,
    val cardSurface: Color,
    val raisedSurface: Color,
    val border: Color,
    val accentGold: Color,
    val mutedGold: Color,
    val primaryText: Color,
    val mutedText: Color,
    val activeHeart: Color,
    val chordText: Color,
    val isDark: Boolean
)

val DarkColors = ChordAppColors(
    background = Color(0xFF0E1117),
    cardSurface = Color(0xFF161B22),
    raisedSurface = Color(0xFF1A202C),
    border = Color(0xFF242C3D),
    accentGold = Color(0xFFE5A93C),
    mutedGold = Color(0xFF2A210C),
    primaryText = Color(0xFFF5F8FF),
    mutedText = Color(0xFF8E9AA8),
    activeHeart = Color(0xFFFF4D6D),
    chordText = Color(0xFFFFB74D),
    isDark = true
)

val LightColors = ChordAppColors(
    background = Color(0xFFF8F9FA),
    cardSurface = Color(0xFFFFFFFF),
    raisedSurface = Color(0xFFF1F3F5),
    border = Color(0xFFE2E8F0),
    accentGold = Color(0xFFD97706),
    mutedGold = Color(0xFFFEF3C7),
    primaryText = Color(0xFF0F172A),
    mutedText = Color(0xFF64748B),
    activeHeart = Color(0xFFE11D48),
    chordText = Color(0xFFB45309), // Rich warm amber on light paper
    isDark = false
)

val LocalChordAppColors = staticCompositionLocalOf { DarkColors }

val Background: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.background

val CardSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.cardSurface

val RaisedSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.raisedSurface

val Border: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.border

val AccentGold: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.accentGold

val MutedGold: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.mutedGold

val PrimaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.primaryText

val MutedText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.mutedText

val ActiveHeart: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.activeHeart

val ChordText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.chordText

// Badges
val UgText = Color(0xFFF59E0B)
val UgBg = Color(0xFF2A210C)
val GtText = Color(0xFF22C55E)
val GtBg = Color(0xFF0D281E)
val CtText = Color(0xFF38BDF8)
val CtBg = Color(0xFF0C2340)

// Region Badges
val RegionIdText = Color(0xFF22C55E) // Hijau Emerald
val RegionIdBg = Color(0xFF0D281E)
val RegionEnText = Color(0xFFA78BFA) // Lavender / Slate Purple
val RegionEnBg = Color(0xFF231F3D)
val RegionKrText = Color(0xFF38BDF8) // Sky Blue / Cyan
val RegionKrBg = Color(0xFF0C2340)
val RegionJpText = Color(0xFFFB7185) // Rose / Coral
val RegionJpBg = Color(0xFF37121E)
