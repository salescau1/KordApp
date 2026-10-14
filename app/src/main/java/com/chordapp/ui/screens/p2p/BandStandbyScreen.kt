package com.chordapp.ui.screens.p2p

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.p2p.BandSessionManager
import com.chordapp.p2p.ClientConnectionState
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.ui.theme.RaisedSurface

@Composable
fun BandStandbyScreen(
    onCloseClick: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onDisconnectClick: () -> Unit = onCloseClick
) {
    val connectionState by BandSessionManager.connectionState.collectAsState()
    val context = LocalContext.current

    // Keep screen on during stage standby
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Triple Radar Pulse concentric rings animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF151A24),
                        Color(0xFF0C0E12)
                    ),
                    radius = 900f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Header 3 Kolom Sesuai Arahan Boss: [ ✕ Close ] [ Title ] [ Live Sync 🟢 ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom Kiri: [ ✕ Close ]
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CardSurface)
                    .border(1.dp, Border, CircleShape)
                    .clickable { onCloseClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Kolom Tengah: Judul
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Standby Panggung",
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Kolom Kanan: [ 🟢 Live Sync ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1F22C55E))
                    .border(1.dp, Color(0x6622C55E), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                )
                Text(
                    text = "Live Sync",
                    color = Color(0xFF22C55E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        // 2. Di Tengah Layar: Resonansi Not Balok Musik Ganda (♫) & Status Koneksi
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Radar Rings & Double Musical Note Core (♫)
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Ring 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale1)
                            .border(1.5.dp, AccentGold.copy(alpha = pulseAlpha1), CircleShape)
                    )
                    // Outer Ring 2
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale2)
                            .border(1.5.dp, AccentGold.copy(alpha = pulseAlpha2), CircleShape)
                    )
                    // Core Circle with Musical Note (♫)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.14f))
                            .border(2.dp, AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♫",
                            color = AccentGold,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Connection Status Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (val state = connectionState) {
                        is ClientConnectionState.Connected -> {
                            Text(
                                text = "Connected to ${state.serverName}",
                                color = PrimaryText,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Waiting for lyrics from stage leader...",
                                color = MutedText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        is ClientConnectionState.Connecting -> {
                            Text(
                                text = "Connecting to ${state.serverName}...",
                                color = PrimaryText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Menyambungkan ke pemandu panggung...",
                                color = MutedText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        is ClientConnectionState.Error -> {
                            Text(
                                text = "Koneksi Terputus",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Mencari pemandu musik kembali di panggung...",
                                color = MutedText,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        ClientConnectionState.Disconnected -> {
                            Text(
                                text = "Standby Panggung",
                                color = PrimaryText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Waiting for lyrics from stage leader...",
                                color = MutedText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Sub-Indicator Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(RaisedSurface)
                        .border(1.dp, Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                    Text(
                        text = "Standby Panggung (Dock Disembunyikan)",
                        color = Color(0xFF22C55E),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
