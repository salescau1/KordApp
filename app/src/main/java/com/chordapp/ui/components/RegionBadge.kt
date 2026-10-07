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
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.ui.theme.RegionEnBg
import com.chordapp.ui.theme.RegionEnText
import com.chordapp.ui.theme.RegionIdBg
import com.chordapp.ui.theme.RegionIdText
import com.chordapp.ui.theme.RegionJpBg
import com.chordapp.ui.theme.RegionJpText
import com.chordapp.ui.theme.RegionKrBg
import com.chordapp.ui.theme.RegionKrText

/** Label kode 2-huruf untuk wilayah/region lagu & artis. */
fun regionLabel(region: String?): String = when (region?.uppercase()) {
    "INDO", "INDONESIA" -> "ID"
    "BARAT" -> "EN"
    "KOREA" -> "KR"
    "JAPAN" -> "JP"
    "ASIA" -> "ASIA"
    else -> region?.uppercase() ?: ""
}

/** Plat warna (Badge) untuk menampilkan asal region artis / lagu. */
@Composable
fun RegionBadge(region: String?, compact: Boolean = false) {
    val label = regionLabel(region)
    if (label.isEmpty()) return

    val bg = when (label) {
        "ID" -> RegionIdBg
        "EN" -> RegionEnBg
        "KR" -> RegionKrBg
        "JP" -> RegionJpBg
        else -> CardSurface
    }
    val textColor = when (label) {
        "ID" -> RegionIdText
        "EN" -> RegionEnText
        "KR" -> RegionKrText
        "JP" -> RegionJpText
        else -> PrimaryText
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(
                horizontal = if (compact) 5.dp else 6.dp,
                vertical = if (compact) 1.dp else 2.dp
            )
    ) {
        Text(
            text = label,
            color = textColor,
            style = if (compact) {
                MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
            } else {
                MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp)
            }
        )
    }
}
