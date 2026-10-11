package com.chordapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.CtBg
import com.chordapp.ui.theme.CtText
import com.chordapp.ui.theme.GtBg
import com.chordapp.ui.theme.GtText
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.ui.theme.UgBg
import com.chordapp.ui.theme.UgText

import com.chordapp.ui.theme.CiBg
import com.chordapp.ui.theme.CiText

/** Label pendek yang ditampilkan untuk tiap sumber chord ("ChordTela" -> "CT", "ChordIndonesia" -> "CI", dll). */
fun sourceLabel(source: String): String = when (source) {
    "ChordTela" -> "CT"
    "ChordIndonesia", "Chord Indonesia" -> "CI"
    "GuitarTuna", "Guitar Tuna" -> "GT"
    "Ultimate Guitar", "UltimateGuitar" -> "UG"
    else -> source // UG / GT / CT / CI sudah singkat
}

/** Non-interactive badge showing a chord source name, deduplicated per source. */
@Composable
fun SourceBadge(source: String, compact: Boolean = false) {
    val norm = sourceLabel(source)
    val bg = when (norm) {
        "UG" -> UgBg
        "GT" -> GtBg
        "CT" -> CtBg
        "CI" -> CiBg
        else -> CardSurface
    }
    val textColor = when (norm) {
        "UG" -> UgText
        "GT" -> GtText
        "CT" -> CtText
        "CI" -> CiText
        else -> PrimaryText
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(
                horizontal = if (compact) 5.dp else 8.dp,
                vertical = if (compact) 2.dp else 4.dp
            )
    ) {
        Text(
            text = sourceLabel(source),
            color = textColor,
            style = if (compact) {
                MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
            } else {
                MaterialTheme.typography.labelMedium
            }
        )
    }
}
