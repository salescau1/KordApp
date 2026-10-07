package com.chordapp.ui.screens.player

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chordapp.player.ChordFingeringDb
import com.chordapp.player.FretPosition
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lazily loads the shipped fingering asset once (in-memory singleton). The asset
 * is deflated inside the APK, so this is cheap even though the JSON is large.
 */
object ChordDiagramCache {
    @Volatile
    private var cached: ChordFingeringDb? = null

    suspend fun get(context: Context): ChordFingeringDb {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            cached ?: run {
                val text = context.assets.open("chords_guitar.json").bufferedReader().use { it.readText() }
                ChordFingeringDb.fromJson(text).also { cached = it }
            }
        }
    }
}

/**
 * Compact fretboard card pinned to the top-right of the sheet (QA-5b). Unlike
 * the earlier centered dialog it is small and drawn inside the reading area, so
 * the lyrics stay visible. [db] may still be loading on the first chord tap — a
 * spinner is shown until it is available. When the symbol is genuinely absent
 * from the asset we say so honestly instead of drawing a made-up shape. The
 * fretboard height follows the shape (row count), so it stays proportional.
 */
@Composable
fun ChordDiagramCard(
    symbol: String,
    db: ChordFingeringDb?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val positions = db?.findPositions(symbol)
    val count = positions?.size ?: 0
    var index by remember(symbol) { mutableIntStateOf(0) }
    val pos = positions?.getOrNull(index.coerceIn(0, (count - 1).coerceAtLeast(0)))
    Surface(
        modifier = modifier.widthIn(min = 110.dp, max = 134.dp),
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, Border),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    symbol,
                    color = AccentGold,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                db == null -> Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(24.dp))
                }
                pos == null -> Text(
                    "Diagram belum tersedia untuk $symbol di data fingering.",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
                else -> {
                    FretboardCanvas(
                        position = pos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.82f)
                    )
                    if (count > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { index = (index - 1 + count) % count },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Bentuk sebelumnya",
                                    tint = MutedText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                "${index + 1}/$count",
                                color = MutedText,
                                style = MaterialTheme.typography.labelSmall
                            )
                            IconButton(
                                onClick = { index = (index + 1) % count },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ArrowForward,
                                    contentDescription = "Bentuk berikutnya",
                                    tint = MutedText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws one real fingering diagram from the chords-db asset.
 *
 * Rendering rules (spec §8, no tutorial):
 *  - six vertical strings, horizontal fret lines; a thick nut line when the
 *    shape starts at the nut, otherwise a thin top fret labelled [baseFret];
 *  - open string = ring above the top line; muted string = ×;
 *  - a pressed string draws a dot on row `frets-1` with the finger number
 *    inside when the asset provides one;
 *  - a barre is drawn as a thick line across the strings it covers.
 */
@Composable
fun FretboardCanvas(position: FretPosition, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val stringStroke = with(density) { 1.4f.dp.toPx() }
    val fretStroke = with(density) { 1.8f.dp.toPx() }
    val nutStroke = with(density) { 4.dp.toPx() }
    val gridColor = PrimaryText.copy(alpha = 0.82f)
    val markerColor = Color(0xFFF5F5F5)
    val fingerStyle = TextStyle(color = Background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val labelStyle = TextStyle(color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val left = w * 0.16f
        val right = w * 0.94f
        val topPad = h * 0.13f
        val bottomPad = h * 0.12f
        val topY = topPad
        val bottomY = h - bottomPad
        val xGap = (right - left) / 5f

        fun xFor(k: Int) = left + k * xGap

        val maxVal = (position.frets.filter { it > 0 }.maxOrNull() ?: 1).coerceIn(1, 6)
        val rowH = (bottomY - topY) / maxVal
        val dotR = minOf(xGap, rowH) * 0.24f

        val nutAtTop = position.baseFret <= 1

        // Vertical strings.
        for (k in 0..5) {
            drawLine(
                color = gridColor,
                start = Offset(xFor(k), topY),
                end = Offset(xFor(k), bottomY),
                strokeWidth = stringStroke
            )
        }

        // Horizontal fret lines. The top line is the nut (thick) or fret baseFret.
        for (r in 0..maxVal) {
            val y = topY + r * rowH
            val isTop = r == 0
            if (isTop && nutAtTop) {
                drawLine(AccentGold, Offset(left, y), Offset(right, y), nutStroke)
            } else {
                drawLine(gridColor, Offset(left, y), Offset(right, y), fretStroke)
            }
        }

        if (!nutAtTop) {
            val label = textMeasurer.measure(AnnotatedString(position.baseFret.toString()), labelStyle)
            drawText(
                textLayoutResult = label,
                color = MutedText,
                topLeft = Offset(left * 0.12f, topY + rowH * 0.5f - label.size.height / 2f)
            )
        }

        val markerY = topY - rowH * 0.4f
        // Markers for open (ring) and muted (×) strings.
        for (k in 0..5) {
            val f = position.frets.getOrNull(k) ?: continue
            val x = xFor(k)
            when {
                f == 0 -> drawCircle(
                    color = markerColor,
                    radius = dotR * 0.9f,
                    center = Offset(x, markerY),
                    style = Stroke(width = fretStroke)
                )
                f < 0 -> {
                    val half = dotR * 0.8f
                    drawLine(markerColor, Offset(x - half, markerY - half), Offset(x + half, markerY + half), fretStroke)
                    drawLine(markerColor, Offset(x - half, markerY + half), Offset(x + half, markerY - half), fretStroke)
                }
            }
        }

        // Pressed-string dots (skip a string that is covered by a barre).
        for (k in 0..5) {
            val f = position.frets.getOrNull(k) ?: continue
            if (f <= 0) continue
            if (position.barres.any { it == f }) continue
            val x = xFor(k)
            val cy = topY + (f - 1) * rowH + rowH * 0.5f
            drawCircle(color = AccentGold, radius = dotR, center = Offset(x, cy))
            val finger = position.fingers.getOrNull(k) ?: 0
            if (finger > 0) {
                val digit = textMeasurer.measure(AnnotatedString(finger.toString()), fingerStyle)
                drawText(
                    textLayoutResult = digit,
                    color = Background,
                    topLeft = Offset(x - digit.size.width / 2f, cy - digit.size.height / 2f)
                )
            }
        }

        // Barres on top of the dots they cover.
        for (b in position.barres) {
            val covered = position.frets.indices.filter { position.frets[it] == b }
            if (covered.isEmpty()) continue
            val y = topY + (b - 1) * rowH + rowH * 0.5f
            drawLine(
                color = PrimaryText,
                start = Offset(xFor(covered.first()), y),
                end = Offset(xFor(covered.last()), y),
                strokeWidth = (rowH * 0.3f).coerceAtLeast(stringStroke * 2f),
                cap = StrokeCap.Round
            )
        }
    }
}
