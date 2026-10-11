package com.chordapp.ui.screens.p2p

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.localization.LocalAppStrings
import com.chordapp.p2p.BandSessionManager
import com.chordapp.p2p.ClientConnectionState
import com.chordapp.p2p.LiveSongPayload
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BandStandbyScreen(
    onDisconnectClick: () -> Unit
) {
    val strings = LocalAppStrings.current
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

    // Live clock for stage timer
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTime = formatter.format(Date())
            delay(1000)
        }
    }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Background,
                        CardSurface.copy(alpha = 0.5f),
                        Background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Digital Clock at top
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                color = AccentGold,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = strings.bandStandbyTitle,
                color = MutedText,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Center: Radar indicator & Connection info
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .border(2.dp, AccentGold.copy(alpha = 0.6f), CircleShape)
                    .background(AccentGold.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            when (val state = connectionState) {
                is ClientConnectionState.Connected -> {
                    Text(
                        text = strings.bandConnectedTo(state.serverName),
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.bandWaitingForLeader,
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is ClientConnectionState.Connecting -> {
                    Text(
                        text = "Menyambungkan ke ${state.serverName}...",
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                is ClientConnectionState.Error -> {
                    Text(
                        text = "Koneksi terputus: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = strings.bandSearchingServers,
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ClientConnectionState.Disconnected -> {
                    Text(
                        text = strings.bandSearchingServers,
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Bottom action button: Disconnect
        Button(
            onClick = onDisconnectClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CardSurface,
                contentColor = PrimaryText
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = strings.bandDisconnect,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
