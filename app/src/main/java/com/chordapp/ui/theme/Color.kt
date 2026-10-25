package com.chordapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeTextureType {
    NONE,
    LEATHER,
    PARCHMENT,
    CHALKBOARD,
    GREENBOARD,
    CRIMSON_SHADOW
}

// Brand signature colors - NEVER change across themes
val BrandGold = Color(0xFFE5A93C)
val UnifiedChordGold = Color(0xFFFDE047) // Universal bright chord color across all themes

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
    val isDark: Boolean,
    val textureType: ThemeTextureType = ThemeTextureType.NONE
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
    chordText = UnifiedChordGold,
    isDark = true,
    textureType = ThemeTextureType.NONE
)

val LeatherColors = ChordAppColors(
    background = Color(0xFF1B130E),
    cardSurface = Color(0xFF261B14),
    raisedSurface = Color(0xFF33241B),
    border = Color(0xFF4A3528),
    accentGold = Color(0xFFE5A93C),
    mutedGold = Color(0xFF352014),
    primaryText = Color(0xFFFDF8F2),
    mutedText = Color(0xFFA89485),
    activeHeart = Color(0xFFFF5252),
    chordText = UnifiedChordGold,
    isDark = true,
    textureType = ThemeTextureType.LEATHER
)

val PaperColors = ChordAppColors(
    background = Color(0xFFF2EBD9),
    cardSurface = Color(0xFFFAF6EB),
    raisedSurface = Color(0xFFE5DCC6),
    border = Color(0xFFD6C8AE),
    accentGold = Color(0xFF9A4E08),
    mutedGold = Color(0xFFF5E4C3),
    primaryText = Color(0xFF221C14),
    mutedText = Color(0xFF6B5E4F),
    activeHeart = Color(0xFFC0392B),
    chordText = UnifiedChordGold,
    isDark = false,
    textureType = ThemeTextureType.PARCHMENT
)

val BlackboardColors = ChordAppColors(
    background = Color(0xFF111417),
    cardSurface = Color(0xFF1A1F24),
    raisedSurface = Color(0xFF222930),
    border = Color(0xFF2C353F),
    accentGold = Color(0xFFFBBF24),
    mutedGold = Color(0xFF242216),
    primaryText = Color(0xFFF1F5F9),
    mutedText = Color(0xFF94A3B8),
    activeHeart = Color(0xFFFB7185),
    chordText = UnifiedChordGold,
    isDark = true,
    textureType = ThemeTextureType.CHALKBOARD
)

val GreenboardColors = ChordAppColors(
    background = Color(0xFF121C16),       // Deep dark vintage chalkboard slate (doff & matte)
    cardSurface = Color(0xFF18261E),      // Darker slate surface (solid 100%)
    raisedSurface = Color(0xFF203228),    // Elevated slate
    border = Color(0xFF23362B),           // Subtle chalk tray / slate seam
    accentGold = Color(0xFFE5A93C),       // Brand Gold KordApp (NO NEON GREEN)
    mutedGold = Color(0xFF1E2D24),
    primaryText = Color(0xFFEAE6DF),      // Kapur Tulis Putih (Soft Chalk White)
    mutedText = Color(0xFF8A998F),        // Debu Bekas Hapus Kapur (Chalk Dust Muted)
    activeHeart = Color(0xFFE05A47),      // Kapur Tulis Merah Pudar
    chordText = UnifiedChordGold,
    isDark = true,
    textureType = ThemeTextureType.GREENBOARD
)

val CrimsonShadowColors = ChordAppColors(
    background = Color(0xFF120D12),
    cardSurface = Color(0xFF1E141C),
    raisedSurface = Color(0xFF281B26),
    border = Color(0xFF4A1E27),
    accentGold = Color(0xFFC72428), // Blood Moon Crimson
    mutedGold = Color(0xFF331218),
    primaryText = Color(0xFFF4E9DC), // Porcelain Mask Ivory
    mutedText = Color(0xFF9E8A86), // Muted Burlap
    activeHeart = Color(0xFFE63946),
    chordText = UnifiedChordGold,
    isDark = true,
    textureType = ThemeTextureType.CRIMSON_SHADOW
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

val TextureType: ThemeTextureType
    @Composable
    @ReadOnlyComposable
    get() = LocalChordAppColors.current.textureType

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
