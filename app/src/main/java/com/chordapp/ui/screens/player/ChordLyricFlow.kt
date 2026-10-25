package com.chordapp.ui.screens.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.chordapp.settings.CustomPlayerManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.player.ChordParser
import com.chordapp.player.SheetLine
import com.chordapp.player.Transposer
import com.chordapp.ui.theme.ChordText
import com.chordapp.ui.theme.LocalChordAppColors
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.ui.theme.UnifiedChordGold

/**
 * One wrap unit: a lyric word (or an empty string for a chord that sits past the
 * lyric) with the chord(s) that fall over it. Rendered as a small stacked column
 * — chord on top, word below — so a whole chord+word block moves down together
 * when the row runs out of width, instead of the chord line and lyric line
 * wrapping independently and drifting apart.
 */
internal data class ChordLyricSegment(val chord: String?, val text: String)

/**
 * Splits a chord row + its lyric line into per-word segments, assigning each
 * chord to the word it sits over (a chord in the gap before a word attaches to
 * that following word — chords precede their syllable in this catalog). Chords
 * are transposed by [delta] here so the caller renders plain symbols.
 */
internal fun buildChordLyricSegments(
    chordLine: SheetLine.Chords,
    lyric: String,
    delta: Int
): List<ChordLyricSegment> {
    val tokens = chordLine.tokens.sortedBy { it.column }
    if (tokens.isEmpty()) {
        return listOf(ChordLyricSegment(null, lyric))
    }
    if (lyric.isEmpty()) {
        return tokens.map { ChordLyricSegment(Transposer.transpose(it.symbol, delta), "") }
    }

    val firstCol = tokens[0].column
    var firstColSnapped = firstCol
    var prefixEnd = 0

    if (firstCol > 0) {
        val prefixLen = minOf(firstCol, lyric.length)
        // Check if firstCol falls in the middle of a word:
        val inMiddleOfWord = firstCol < lyric.length &&
            !lyric[firstCol].isWhitespace() &&
            !lyric[firstCol - 1].isWhitespace()

        if (inMiddleOfWord) {
            val lastSpace = lyric.substring(0, prefixLen).lastIndexOf(' ')
            if (lastSpace == -1) {
                // Chord is placed over the very first word (e.g. [D] at col 2 over "Looks" or "Ain't").
                // Never chop the word! The first chord applies to the whole first word.
                prefixEnd = 0
                firstColSnapped = 0
            } else {
                prefixEnd = lastSpace + 1
                firstColSnapped = lastSpace + 1
            }
        } else {
            prefixEnd = prefixLen
            firstColSnapped = prefixLen
        }
    }

    val segments = mutableListOf<ChordLyricSegment>()
    if (prefixEnd > 0) {
        segments.add(ChordLyricSegment(null, lyric.substring(0, prefixEnd)))
    }

    for (i in tokens.indices) {
        val token = tokens[i]
        val col = if (i == 0) firstColSnapped else token.column
        val sym = Transposer.transpose(token.symbol, delta)
        val nextCol = if (i + 1 < tokens.size) tokens[i + 1].column else maxOf(lyric.length, col)

        val textSlice = if (col < lyric.length) {
            val sliceEnd = minOf(nextCol, lyric.length)
            lyric.substring(col, sliceEnd)
        } else {
            ""
        }
        segments.add(ChordLyricSegment(sym, textSlice))
    }

    return segments
}

/**
 * Renders text with optional diffuse centered ambient shadow and/or outline,
 * reacting dynamically to Custom Player styling if enabled, or falling back to theme defaults.
 */
@Composable
internal fun AppSheetText(
    text: String,
    color: Color,
    fontSize: Float,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.SemiBold,
    isDark: Boolean = LocalChordAppColors.current.isDark,
    shadowEnabled: Boolean? = null,
    shadowLevel: String? = null,
    outlineEnabled: Boolean? = null,
    outlineLevel: String? = null
) {
    val customConfig by CustomPlayerManager.config.collectAsState()
    val isCustomActive = customConfig.enabled

    val effectiveShadowEnabled = shadowEnabled ?: if (isCustomActive) customConfig.shadowEnabled else true
    val effectiveShadowLevel = shadowLevel ?: if (isCustomActive) customConfig.shadowLevel else "medium"

    val effectiveOutlineEnabled = outlineEnabled ?: if (isCustomActive) customConfig.outlineEnabled else false
    val effectiveOutlineLevel = outlineLevel ?: if (isCustomActive) customConfig.outlineLevel else "medium"

    val density = LocalDensity.current

    val (wideBlurPx, coreBlurPx, shadowAlpha) = remember(density, effectiveShadowLevel, isDark) {
        with(density) {
            when (effectiveShadowLevel) {
                "thin" -> Triple(5.dp.toPx(), 2.dp.toPx(), if (isDark) 0.65f else 0.40f)
                "thick" -> Triple(16.dp.toPx(), 6.dp.toPx(), if (isDark) 1.0f else 0.85f)
                else -> Triple(10.dp.toPx(), 4.dp.toPx(), if (isDark) 0.95f else 0.70f)
            }
        }
    }

    val outlineStrokePx = remember(density, effectiveOutlineLevel) {
        with(density) {
            when (effectiveOutlineLevel) {
                "thin" -> 2.2.dp.toPx()
                "thick" -> 4.5.dp.toPx()
                else -> 3.2.dp.toPx()
            }
        }
    }

    val shadowColor = remember(isDark, color, shadowAlpha) {
        if (isDark) {
            Color.Black.copy(alpha = shadowAlpha)
        } else {
            if (color == UnifiedChordGold) Color.Black.copy(alpha = (shadowAlpha * 0.9f).coerceIn(0f, 1f))
            else Color.Black.copy(alpha = (shadowAlpha * 0.4f).coerceIn(0f, 1f))
        }
    }

    val shadowObj = if (effectiveShadowEnabled) {
        Shadow(color = shadowColor, offset = Offset.Zero, blurRadius = coreBlurPx)
    } else null

    val wideShadowObj = if (effectiveShadowEnabled) {
        Shadow(color = shadowColor, offset = Offset.Zero, blurRadius = wideBlurPx)
    } else null

    Box(modifier = modifier) {
        if (effectiveOutlineEnabled) {
            // Outline layer with stroke + outer shadow
            Text(
                text = text,
                color = Color.Black.copy(alpha = 0.95f),
                fontWeight = fontWeight,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                style = TextStyle(
                    drawStyle = Stroke(width = outlineStrokePx, join = StrokeJoin.Round),
                    shadow = wideShadowObj
                )
            )
        } else if (effectiveShadowEnabled) {
            // Wide ambient blur cloud layer
            Text(
                text = text,
                color = shadowColor,
                fontWeight = fontWeight,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                style = TextStyle(shadow = wideShadowObj)
            )
        }

        // Top Fill layer
        Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            style = TextStyle(
                drawStyle = Fill,
                shadow = if (!effectiveOutlineEnabled && effectiveShadowEnabled) shadowObj else null
            )
        )
    }
}

/**
 * Renders a chord row glued to its lyric so each chord stays above its own word
 * and a chord+word block wraps down as a unit when the line runs out of width —
 * instead of the chord line and the lyric line wrapping independently and
 * sliding out of alignment. Pass [chordLine] = null for a plain lyric line, or
 * [lyric] = null for an instrumental chord-only line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChordLyricFlow(
    chordLine: SheetLine.Chords?,
    lyric: String?,
    fontSize: Float,
    transposeDelta: Int,
    requestDiagram: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalChordAppColors.current.isDark
    val customConfig by CustomPlayerManager.config.collectAsState()
    val isCustomActive = customConfig.enabled

    val chordColor = if (isCustomActive) customConfig.getChordColor() else ChordText
    val lyricColor = if (isCustomActive) customConfig.getLyricColor() else PrimaryText

    if (chordLine == null) {
        AppSheetText(
            text = lyric ?: "",
            color = lyricColor,
            fontSize = fontSize,
            isDark = isDark,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    // Instrumental / chord-only row (Solo, Intro, Outro chords without lyrics):
    // Rendered compactly as a single horizontal row of chord chips without artificial blank lines below.
    if (lyric.isNullOrBlank()) {
        FlowRow(modifier = modifier.fillMaxWidth()) {
            chordLine.tokens.sortedBy { it.column }.forEach { token ->
                val transposed = Transposer.transpose(token.symbol, transposeDelta)
                AppSheetText(
                    text = transposed + "   ",
                    color = chordColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    isDark = isDark,
                    modifier = Modifier.pointerInput(transposed) {
                        detectTapGestures {
                            requestDiagram(ChordParser.cleanChordSymbol(transposed))
                        }
                    }
                )
            }
        }
        return
    }

    val segments = remember(chordLine, lyric, transposeDelta) {
        buildChordLyricSegments(chordLine, lyric, transposeDelta)
    }

    val chordSlotHeight = with(LocalDensity.current) { (fontSize * 1.35f).sp.toDp() }

    FlowRow(modifier = modifier.fillMaxWidth()) {
        segments.forEach { seg ->
            Column(horizontalAlignment = Alignment.Start) {
                Box(
                    modifier = Modifier.height(chordSlotHeight),
                    contentAlignment = Alignment.BottomStart
                ) {
                    if (seg.chord != null) {
                        val chordText = if (seg.text.isEmpty() || seg.chord.length >= seg.text.length) {
                            seg.chord + " "
                        } else {
                            seg.chord
                        }
                        AppSheetText(
                            text = chordText,
                            color = chordColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            isDark = isDark,
                            modifier = Modifier.pointerInput(seg.chord) {
                                detectTapGestures {
                                    requestDiagram(ChordParser.cleanChordSymbol(seg.chord.trim().substringBefore(' ')))
                                }
                            }
                        )
                    }
                }

                if (seg.text.isNotEmpty()) {
                    AppSheetText(
                        text = seg.text,
                        color = lyricColor,
                        fontSize = fontSize,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

/**
 * Renders an instrumental / bar notation line (`| C G | Am  | Dm  | G   |`)
 * as a single monospace row. Chords are highlighted, bold, clickable, and transposed,
 * while bar delimiters (`|`), measure spaces, and performance annotations remain
 * in plain text on the same row with consistent outline and ambient shadow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BarLineFlow(
    barLine: SheetLine.Chords,
    fontSize: Float,
    transposeDelta: Int,
    requestDiagram: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(barLine) {
        ChordParser.buildBarSegments(barLine)
    }
    val isDark = LocalChordAppColors.current.isDark
    val customConfig by CustomPlayerManager.config.collectAsState()
    val isCustomActive = customConfig.enabled

    val chordColor = if (isCustomActive) customConfig.getChordColor() else ChordText
    val textColor = if (isCustomActive) customConfig.getLyricColor() else PrimaryText

    FlowRow(modifier = modifier.fillMaxWidth()) {
        segments.forEach { seg ->
            if (seg.isChord) {
                val transposed = remember(seg.text, transposeDelta) {
                    Transposer.transpose(seg.text, transposeDelta)
                }
                AppSheetText(
                    text = transposed,
                    color = chordColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    isDark = isDark,
                    modifier = Modifier.pointerInput(transposed) {
                        detectTapGestures {
                            requestDiagram(ChordParser.cleanChordSymbol(transposed))
                        }
                    }
                )
            } else {
                AppSheetText(
                    text = seg.text,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize,
                    isDark = isDark
                )
            }
        }
    }
}
