package com.chordapp.ui.screens.tuner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.chordapp.tuner.InstrumentAudioPlayer
import com.chordapp.tuner.PitchDetector
import com.chordapp.tuner.PitchReading
import com.chordapp.tuner.TunerInstrumentType
import com.chordapp.tuner.TunerPresets
import com.chordapp.tuner.TunerString
import kotlin.math.abs
import kotlin.math.roundToInt

private val TunerDarkBg = Color(0xFF0F1115)
private val TunerCardBg = Color(0xFF181C22)
private val TunerGold = Color(0xFFFFB74D)
private val TunerEmerald = Color(0xFF00E676)
private val TunerMuted = Color(0xFF8895A5)

@Composable
fun TunerScreen(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val audioPlayer = remember { InstrumentAudioPlayer(context) }
    val pitchDetector = remember { PitchDetector() }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            pitchDetector.start()
        }
    }

    // Automatically prompt for mic permission if not granted yet
    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var selectedInstrument by remember { mutableStateOf(TunerInstrumentType.GUITAR_6) }
    var isAutoMode by remember { mutableStateOf(true) }
    var manualSelectedString by remember { mutableStateOf<TunerString?>(null) }
    var instrumentMenuExpanded by remember { mutableStateOf(false) }

    val reading by pitchDetector.pitchState.collectAsState()

    // Chime feedback when in tune
    var lastChimeTime by remember { mutableLongStateOf(0L) }
    var wasInTune by remember { mutableStateOf(false) }

    LaunchedEffect(reading.isInTune, reading.isSilent) {
        val now = System.currentTimeMillis()
        if (reading.isInTune && !reading.isSilent && !wasInTune && (now - lastChimeTime > 1200L)) {
            lastChimeTime = now
            audioPlayer.playInTuneChime()
        }
        wasInTune = reading.isInTune && !reading.isSilent
    }

    // Detector updates
    LaunchedEffect(selectedInstrument) {
        pitchDetector.currentInstrument = selectedInstrument
        manualSelectedString = TunerPresets.getStringsFor(selectedInstrument).firstOrNull()
        pitchDetector.targetString = manualSelectedString
    }

    LaunchedEffect(isAutoMode) {
        pitchDetector.isAutoMode = isAutoMode
    }

    LaunchedEffect(manualSelectedString) {
        if (!isAutoMode) {
            pitchDetector.targetString = manualSelectedString
        }
    }

    // AudioRecord lifecycle
    DisposableEffect(hasMicPermission) {
        if (hasMicPermission) {
            pitchDetector.start()
        }
        onDispose {
            pitchDetector.stop()
            audioPlayer.release()
        }
    }

    val strings = remember(selectedInstrument) {
        TunerPresets.getStringsFor(selectedInstrument)
    }

    val activeHighlightString = if (isAutoMode) {
        reading.matchedString
    } else {
        manualSelectedString
    }

    val headstockDrawable = when (selectedInstrument) {
        TunerInstrumentType.GUITAR_6 -> com.chordapp.R.drawable.guitar_headstock
        TunerInstrumentType.BASS_4 -> com.chordapp.R.drawable.bass_headstock
        TunerInstrumentType.UKULELE -> com.chordapp.R.drawable.ukulele_headstock
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TunerDarkBg)
            .statusBarsPadding()
    ) {
        // =======================================================================
        // 1. TOP BAR: (x) KordApp Tuner            Auto (0-0)
        // =======================================================================
        TunerTopBar(
            isAutoMode = isAutoMode,
            onAutoToggle = { isAutoMode = it },
            onClose = onClose
        )

        // =======================================================================
        // 2. INSTRUMENT SELECTOR: Guitar 6 Strings ▾
        // =======================================================================
        InstrumentSelectorRow(
            currentInstrument = selectedInstrument,
            isMenuExpanded = instrumentMenuExpanded,
            onExpandChange = { instrumentMenuExpanded = it },
            onSelectInstrument = {
                selectedInstrument = it
                instrumentMenuExpanded = false
            }
        )

        // Mic permission banner if denied
        AnimatedVisibility(
            visible = !hasMicPermission,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MicPermissionBanner(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // =======================================================================
        // 3. CENTER DIAGRAM AREA WITH BACKGROUND GUITAR & FADE-OUT GRADIENTS
        // =======================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // (a) Background Guitar/Bass/Ukulele Graphic - Centered and subtle (samar-samar)
            Image(
                painter = painterResource(id = headstockDrawable),
                contentDescription = "Instrument Visual",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .width(220.dp)
                    .alpha(0.20f)
            )

            // (b) Top Fade-Out Gradient Line
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(TunerDarkBg, Color.Transparent)
                        )
                    )
            )

            // (c) Bottom Fade-Out Gradient Line
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, TunerDarkBg)
                        )
                    )
            )

            // (d) Main Tuning Diagram & Meter (Border-free, Centered)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Giant Active Note Readout (e.g. E, A, D, G, B)
                BigNoteDisplay(
                    reading = reading,
                    isSilent = reading.isSilent
                )

                Spacer(modifier = Modifier.height(18.dp))

                // The Tuning Diagram Gauge (Tick marks, needle bubble, flat/sharp)
                TunerDiagramMeter(
                    reading = reading,
                    isSilent = reading.isSilent
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status message & Frequency
                TunerStatusReadout(
                    reading = reading,
                    isSilent = reading.isSilent
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Horizontal String Selector Row (Clean borderless pills)
                TunerStringsRow(
                    strings = strings,
                    activeString = activeHighlightString,
                    isInTune = reading.isInTune && !reading.isSilent,
                    onSelect = { str ->
                        audioPlayer.playStringNote(selectedInstrument, str.stringNumber)
                        if (!isAutoMode) {
                            manualSelectedString = str
                        }
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. TOP BAR
// ---------------------------------------------------------------------------
@Composable
private fun TunerTopBar(
    isAutoMode: Boolean,
    onAutoToggle: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Close Button (x) + Brand Text
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TunerCardBg)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tuner",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Kord",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "App",
                    color = TunerGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = " Tuner",
                    color = Color(0xFFD0D7E2),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right: AUTO toggle switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(TunerCardBg)
                .padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = "AUTO",
                color = if (isAutoMode) TunerEmerald else TunerMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = isAutoMode,
                onCheckedChange = onAutoToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TunerEmerald,
                    uncheckedThumbColor = TunerMuted,
                    uncheckedTrackColor = Color(0xFF20252C),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. INSTRUMENT SELECTOR ROW
// ---------------------------------------------------------------------------
@Composable
private fun InstrumentSelectorRow(
    currentInstrument: TunerInstrumentType,
    isMenuExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelectInstrument: (TunerInstrumentType) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(TunerCardBg)
                .clickable { onExpandChange(true) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentInstrument.displayName,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pilih Instrumen",
                        tint = TunerMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    text = currentInstrument.subtitle,
                    color = TunerMuted,
                    fontSize = 10.5.sp
                )
            }
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier
                .background(TunerCardBg)
        ) {
            TunerInstrumentType.values().forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = type.displayName,
                                color = if (type == currentInstrument) TunerGold else Color.White,
                                fontWeight = if (type == currentInstrument) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = type.subtitle,
                                color = TunerMuted,
                                fontSize = 11.sp
                            )
                        }
                    },
                    onClick = { onSelectInstrument(type) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MIC PERMISSION BANNER
// ---------------------------------------------------------------------------
@Composable
private fun MicPermissionBanner(onRequestPermission: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF28241B))
            .clickable { onRequestPermission() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = TunerGold,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Izinkan Akses Mikrofon",
                color = TunerGold,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
            Text(
                text = "Ketuk untuk memberikan izin mikrofon agar tuner dapat mendeteksi nada.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.5.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// BIG NOTE DISPLAY
// ---------------------------------------------------------------------------
@Composable
private fun BigNoteDisplay(
    reading: PitchReading,
    isSilent: Boolean
) {
    val isInTune = reading.isInTune && !isSilent
    val noteColor by animateColorAsState(
        targetValue = when {
            isSilent -> Color(0xFF6B7280)
            isInTune -> TunerEmerald
            else -> Color.White
        },
        label = "noteColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isSilent) "--" else reading.noteName.ifEmpty { "--" },
                color = noteColor,
                fontSize = 68.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            if (!isSilent && reading.octave > 0) {
                Text(
                    text = "${reading.octave}",
                    color = noteColor.copy(alpha = 0.7f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, start = 2.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TUNER DIAGRAM METER (BORDER-FREE)
// ---------------------------------------------------------------------------
@Composable
private fun TunerDiagramMeter(
    reading: PitchReading,
    isSilent: Boolean
) {
    val rawDev = reading.centsDeviation
    val absDev = abs(rawDev)
    val displayedCents = when {
        absDev <= 2.5f -> 0f
        absDev <= 6.0f -> {
            val t = (absDev - 2.5f) / 3.5f
            val smoothT = t * t * (3f - 2f * t)
            kotlin.math.sign(rawDev) * (absDev * smoothT)
        }
        else -> rawDev
    }

    var lastHeldCents by remember { mutableFloatStateOf(0f) }
    if (!isSilent) {
        lastHeldCents = displayedCents
    }
    val targetCents = if (isSilent) lastHeldCents else displayedCents

    val animatedCents by animateFloatAsState(
        targetValue = targetCents,
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = 220f
        ),
        label = "diagramCents"
    )

    val isInTune = reading.isInTune && !isSilent
    val needleColor by animateColorAsState(
        targetValue = when {
            isSilent -> Color(0xFF5A6472)
            isInTune -> TunerEmerald
            else -> TunerGold
        },
        label = "needleColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Meter Scale Line & Graduations (Drawn without outer box border)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2f

            // Baseline guide (subtle glowing center track)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x35FFFFFF),
                        Color(0x80FFFFFF),
                        Color(0x35FFFFFF),
                        Color.Transparent
                    )
                ),
                start = Offset(w * 0.08f, cy),
                end = Offset(w * 0.92f, cy),
                strokeWidth = 1.5f
            )

            // Tick marks
            val totalTicks = 20
            val tickSpan = w * 0.76f
            val leftX = (w - tickSpan) / 2f
            for (i in 0..totalTicks) {
                val tx = leftX + (i / totalTicks.toFloat()) * tickSpan
                val isCenter = i == totalTicks / 2
                val isMajor = i % 5 == 0

                val tickH = when {
                    isCenter -> 32f
                    isMajor -> 20f
                    else -> 10f
                }

                val tickColor = when {
                    isCenter && isInTune -> TunerEmerald
                    isCenter -> Color(0xCCFFFFFF)
                    isMajor -> Color(0x60FFFFFF)
                    else -> Color(0x25FFFFFF)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(tx, cy - tickH / 2f),
                    end = Offset(tx, cy + tickH / 2f),
                    strokeWidth = if (isCenter) 2.5f else 1f
                )
            }
        }

        // Flat 'b' on left, Sharp '#' on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "♭",
                color = if (animatedCents < -3f && !isSilent) TunerGold else TunerMuted.copy(alpha = 0.6f),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "♯",
                color = if (animatedCents > 3f && !isSilent) TunerGold else TunerMuted.copy(alpha = 0.6f),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Sliding Droplet Indicator (No border box)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val maxOffsetPx = widthPx * 0.38f
            val offsetPx = (animatedCents / 50f).coerceIn(-1f, 1f) * maxOffsetPx

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(offsetPx.roundToInt(), 0) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circle droplet pointer
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(if (isInTune) 14.dp else 4.dp, CircleShape, spotColor = needleColor)
                        .background(needleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isInTune) {
                        Text(
                            text = "✓",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    } else if (!isSilent) {
                        Text(
                            text = "${abs(animatedCents).roundToInt()}",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TUNER STATUS & FREQUENCY READOUT
// ---------------------------------------------------------------------------
@Composable
private fun TunerStatusReadout(
    reading: PitchReading,
    isSilent: Boolean
) {
    val isInTune = reading.isInTune && !isSilent
    val statusText = when {
        isSilent -> "Petik senar..."
        isInTune -> "✓ PAS! (In Tune)"
        reading.centsDeviation < -4f -> "Terlalu Rendah (${reading.centsDeviation.roundToInt()} cents)"
        else -> "Terlalu Tinggi (+${reading.centsDeviation.roundToInt()} cents)"
    }

    val statusColor = when {
        isSilent -> TunerMuted
        isInTune -> TunerEmerald
        else -> TunerGold
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        if (!isSilent && reading.frequencyHz > 0f) {
            Text(
                text = " • ${String.format("%.1f", reading.frequencyHz)} Hz",
                color = TunerMuted,
                fontSize = 12.5.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// HORIZONTAL STRINGS ROW (CLEAN BORDERLESS PILLS)
// ---------------------------------------------------------------------------
@Composable
private fun TunerStringsRow(
    strings: List<TunerString>,
    activeString: TunerString?,
    isInTune: Boolean,
    onSelect: (TunerString) -> Unit
) {
    // Sort strings logically from lowest string (6) to highest (1)
    val sortedStrings = remember(strings) {
        strings.sortedByDescending { it.stringNumber }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        sortedStrings.forEach { stringItem ->
            val isCurrent = activeString?.stringIndex == stringItem.stringIndex

            val badgeBg by animateColorAsState(
                targetValue = when {
                    isCurrent && isInTune -> TunerEmerald.copy(alpha = 0.30f)
                    isCurrent -> TunerCardBg
                    else -> Color(0xFF14171C)
                },
                label = "pillBg"
            )

            val badgeTextColor by animateColorAsState(
                targetValue = when {
                    isCurrent && isInTune -> TunerEmerald
                    isCurrent -> TunerGold
                    else -> Color(0xFFA0ACBC)
                },
                label = "pillText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(stringItem) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringItem.noteName,
                        color = badgeTextColor,
                        fontSize = 18.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold
                    )
                    Text(
                        text = "${stringItem.stringNumber}",
                        color = TunerMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
