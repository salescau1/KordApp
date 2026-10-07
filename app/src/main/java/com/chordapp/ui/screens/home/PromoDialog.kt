package com.chordapp.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chordapp.promo.Promo
import com.chordapp.promo.PromoAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Gold = Color(0xFFE6B450)
private val YouTubeRed = Color(0xFFFF0033)
private val SpotifyGreen = Color(0xFF1DB954)

/**
 * Popup promo band. Hanya dipanggil bila ada promo aktif valid (fetch sukses).
 *
 * - Gambar full 1:1 (center-crop), tanpa teks judul/subjudul.
 * - Tombol X translucent di pojok kanan atas.
 * - Countdown mengunci semua interaksi (X jadi angka + cincin gold) sampai habis.
 * - Tombol YouTube (kiri) & Spotify (kanan) di ujung bawah, masing-masing opsional:
 *   satu kosong -> tombol sisanya full-width; dua kosong -> seluruh gambar jadi area tap.
 */
@Composable
fun PromoDialog(
    promo: Promo,
    onDismiss: () -> Unit,
    onAction: (PromoAction) -> Unit
) {
    val total = promo.countdownSeconds.coerceIn(0, 15)
    var remaining by remember(promo.id) { mutableIntStateOf(total) }
    val locked = remaining > 0
    val ring = remember(promo.id) { Animatable(if (total == 0) 1f else 0f) }

    LaunchedEffect(promo.id) {
        if (total > 0) {
            launch { ring.animateTo(1f, tween(total * 1000, easing = LinearEasing)) }
            for (s in total downTo 1) {
                remaining = s
                delay(1000L)
            }
            remaining = 0
        }
    }

    val hasYoutube = !promo.youtubeUrl.isNullOrBlank()
    val hasSpotify = !promo.spotifyUrl.isNullOrBlank()
    val showButtons = hasYoutube || hasSpotify
    val imageTappable = !showButtons && promo.action != null

    Dialog(
        onDismissRequest = { if (!locked) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !locked,
            dismissOnClickOutside = !locked
        )
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = promo.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!locked && imageTappable) {
                                Modifier.clickable {
                                    promo.action?.let(onAction)
                                    onDismiss()
                                }
                            } else Modifier
                        )
                )

                // Tombol stream di ujung bawah (di atas scrim gradasi).
                if (showButtons) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.85f)
                                )
                            )
                            .padding(top = 40.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (hasYoutube) {
                                StreamButton(
                                    label = "YouTube",
                                    icon = Icons.Filled.PlayArrow,
                                    container = YouTubeRed,
                                    content = Color.White,
                                    enabled = !locked,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    promo.youtubeUrl?.let { onAction(PromoAction.External(it)) }
                                    onDismiss()
                                }
                            }
                            if (hasSpotify) {
                                StreamButton(
                                    label = "Spotify",
                                    icon = Icons.Filled.MusicNote,
                                    container = SpotifyGreen,
                                    content = Color(0xFF04220F),
                                    enabled = !locked,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    promo.spotifyUrl?.let { onAction(PromoAction.External(it)) }
                                    onDismiss()
                                }
                            }
                        }
                    }
                }

                // Tombol tutup / countdown di pojok kanan atas.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .then(if (!locked) Modifier.clickable { onDismiss() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (locked) {
                        CircularProgressIndicator(
                            progress = ring.value,
                            modifier = Modifier.size(34.dp),
                            color = Gold,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = remaining.toString(),
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (enabled) container else container.copy(alpha = 0.45f),
        contentColor = content,
        shape = RoundedCornerShape(11.dp),
        modifier = modifier
            .height(44.dp)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
