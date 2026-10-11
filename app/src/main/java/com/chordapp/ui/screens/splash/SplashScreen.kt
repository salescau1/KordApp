package com.chordapp.ui.screens.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.chordapp.R
import com.chordapp.ui.navigation.Screen
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * KordApp splash (versi ringkas, pasti compile):
 *  1. Icon (pick + K) fade + scale-in di kiri.
 *  2. Wordmark "KordApp" muncul di kanan — "Kord" putih, "App" emas.
 *  3. Tiga kata "Music / Offline / Database" muncul bergantian, fade dari
 *     bawah ke atas, ~1 detik per item.
 *  4. Masuk ke [Screen.Home].
 *
 * Timing dikumpulkan di [Timing] biar gampang di-tuning.
 */
private object Timing {
    const val ICON_IN_MS = 420
    const val WORD_IN_MS = 380
    const val TAG_HOLD_MS = 1000L
    const val EXIT_HOLD_MS = 250L
}

object SplashSession {
    var hasShownSplash: Boolean = false
}

@Composable
fun SplashScreen(navController: NavHostController) {
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.72f) }
    val wordAlpha = remember { Animatable(0f) }

    val tags = remember { listOf("Music", "Offline", "Database") }
    var tagIndex by remember { mutableIntStateOf(-1) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        if (SplashSession.hasShownSplash) {
            val isLicensed = com.chordapp.repositories.LicenseRepository.isLicensed(context)
            val destination = if (isLicensed) Screen.Home.route else Screen.Activation.route
            navController.navigate(destination) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        SplashSession.hasShownSplash = true

        // Lapor device di background
        launch { com.chordapp.repositories.LicenseRepository.recordDevice(context) }

        // 1. icon in (alpha + scale bareng)
        launch { iconAlpha.animateTo(1f, tween(Timing.ICON_IN_MS)) }
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        // 2. wordmark in
        wordAlpha.animateTo(1f, tween(Timing.WORD_IN_MS))
        // 3. tiga kata bergantian
        for (i in tags.indices) {
            tagIndex = i
            delay(Timing.TAG_HOLD_MS)
        }
        // 4. keluar: sinkronisasi lisensi jika online & cek status
        delay(Timing.EXIT_HOLD_MS)
        com.chordapp.repositories.LicenseRepository.syncLicenseStatusIfOnline(context)
        val isLicensed = com.chordapp.repositories.LicenseRepository.isLicensed(context)
        val destination = if (isLicensed) Screen.Home.route else Screen.Activation.route
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "KordApp",
                modifier = Modifier
                    .size(152.dp)
                    .alpha(iconAlpha.value)
                    .scale(iconScale.value)
            )
            // Rapatkan ke icon: tarik teks ke kiri untuk menutup padding transparan
            // bawaan foreground (art pick ada di dalam safe-zone 108vp).
            Column(modifier = Modifier.offset(x = (-18).dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(wordAlpha.value)
                ) {
                    Text(
                        text = "Kord",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryText
                    )
                    Text(
                        text = "App",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold
                    )
                }
                Spacer(Modifier.height(3.dp))
                // Area tagline dengan tinggi tetap supaya layout tidak loncat.
                Box(Modifier.height(26.dp)) {
                    AnimatedContent(
                        targetState = tagIndex,
                        transitionSpec = {
                            (fadeIn(tween(320)) + slideInVertically(tween(320)) { full -> full })
                                .togetherWith(fadeOut(tween(220)))
                        },
                        label = "tagline"
                    ) { idx ->
                        if (idx in tags.indices) {
                            Text(
                                text = tags[idx],
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryText
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "By kekalclip",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MutedText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(wordAlpha.value)
        )
    }
}
