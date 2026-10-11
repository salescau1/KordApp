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
    chordText = Color(0xFFB45309),
    isDark = false
)

val PaperColors = ChordAppColors(
    background = Color(0xFFF4EFE6),
    cardSurface = Color(0xFFFAF6F0),
    raisedSurface = Color(0xFFEAE3D5),
    border = Color(0xFFDED5C4),
    accentGold = Color(0xFFB45309),
    mutedGold = Color(0xFFFEF3C7),
    primaryText = Color(0xFF1E242B),
    mutedText = Color(0xFF6B7280),
    activeHeart = Color(0xFFDC2626),
    chordText = Color(0xFFB45309), // Warm amber burnt orange on parchment paper
    isDark = false
)

val BlackboardColors = ChordAppColors(
    background = Color(0xFF13171A),
    cardSurface = Color(0xFF1C2228),
    raisedSurface = Color(0xFF242C35),
    border = Color(0xFF2D3742),
    accentGold = Color(0xFFF59E0B),
    mutedGold = Color(0xFF2E2716),
    primaryText = Color(0xFFECEFF4),
    mutedText = Color(0xFF94A3B8),
    activeHeart = Color(0xFFFF4D6D),
    chordText = Color(0xFFFCD34D), // Golden yellow chalk on slate
    isDark = true
)

val GreenboardColors = ChordAppColors(
    background = Color(0xFF14241B),
    cardSurface = Color(0xFF1D3527),
    raisedSurface = Color(0xFF264634),
    border = Color(0xFF2E533E),
    accentGold = Color(0xFFEAB308),
    mutedGold = Color(0xFF243622),
    primaryText = Color(0xFFF0FDF4),
    mutedText = Color(0xFF86A789),
    activeHeart = Color(0xFFF43F5E),
    chordText = Color(0xFFFDE047), // Vibrant lemon chalk on vintage chalkboard green
    isDark = true
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
val CiText = Color(0xFFA855F7) // Purple / Violet
val CiBg = Color(0xFF2E1065)


// Region Badges
val RegionIdText = Color(0xFF22C55E) // Hijau Emerald
val RegionIdBg = Color(0xFF0D281E)
val RegionEnText = Color(0xFFA78BFA) // Lavender / Slate Purple
val RegionEnBg = Color(0xFF231F3D)
val RegionKrText = Color(0xFF38BDF8) // Sky Blue / Cyan
val RegionKrBg = Color(0xFF0C2340)
val RegionJpText = Color(0xFFFB7185) // Rose / Coral
val RegionJpBg = Color(0xFF37121E)
