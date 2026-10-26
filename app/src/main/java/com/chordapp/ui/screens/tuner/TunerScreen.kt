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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import com.chordapp.R
import com.chordapp.localization.LocalAppStrings
import kotlin.math.abs
import kotlin.math.roundToInt

private val TunerDarkBg = Color(0xFF0F1115)
private val TunerCardBg = Color(0xFF181C22)
private val TunerCardBorder = Color(0xFF262C36)
private val TunerGold = Color(0xFFFFB74D)
private val TunerEmerald = Color(0xFF00E676)
private val TunerMuted = Color(0xFF8895A5)
private val TunerTextPrimary = Color.White

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
    var instrumentMenuExpanded by remember { mutableStateOf(false) }

    val reading by pitchDetector.pitchState.collectAsState()

    val strings = remember(selectedInstrument) {
        TunerPresets.getStringsFor(selectedInstrument)
    }

    // Strictly ordered sequence 6 -> 5 -> 4 -> 3 -> 2 -> 1
    val sortedStrings = remember(strings) {
        strings.sortedByDescending { it.stringNumber }
    }

    var tunedStrings by remember { mutableStateOf(setOf<Int>()) }
    var currentTargetNumber by remember {
        mutableIntStateOf(sortedStrings.firstOrNull()?.stringNumber ?: 6)
    }

    val allTuned = remember(tunedStrings, strings) {
        strings.isNotEmpty() && strings.all { it.stringNumber in tunedStrings }
    }

    val currentTargetString = remember(currentTargetNumber, sortedStrings) {
        sortedStrings.find { it.stringNumber == currentTargetNumber } ?: sortedStrings.firstOrNull()
    }

    // Keep detector target synchronized
    LaunchedEffect(currentTargetString, isAutoMode) {
        pitchDetector.targetString = currentTargetString
        pitchDetector.isAutoMode = isAutoMode
    }

    // Reset when instrument changes
    LaunchedEffect(selectedInstrument) {
        tunedStrings = emptySet()
        val firstStr = TunerPresets.getStringsFor(selectedInstrument).maxOfOrNull { it.stringNumber } ?: 1
        currentTargetNumber = firstStr
        pitchDetector.currentInstrument = selectedInstrument
    }

    // When Auto mode is toggled ON: advance to first untuned string in sequence
    LaunchedEffect(isAutoMode) {
        if (isAutoMode) {
            val nextUntuned = sortedStrings.firstOrNull { it.stringNumber !in tunedStrings }
            if (nextUntuned != null) {
                currentTargetNumber = nextUntuned.stringNumber
            }
        }
    }

    // In-tune stability & Auto sequence progression 6 -> 5 -> 4 -> 3 -> 2 -> 1
    // In both Auto and Manual: string turns green ONLY when tuner reaches sempurna
    LaunchedEffect(reading.isInTune, reading.isSilent, reading.matchedString?.stringNumber) {
        if (reading.isInTune && !reading.isSilent && reading.matchedString != null) {
            val matchedNum = reading.matchedString!!.stringNumber
            if (matchedNum == currentTargetNumber) {
                delay(350L)
                if (reading.isInTune && !reading.isSilent && reading.matchedString?.stringNumber == matchedNum) {
                    if (matchedNum !in tunedStrings) {
                        audioPlayer.playInTuneChime()
                        tunedStrings = tunedStrings + matchedNum

                        if (isAutoMode) {
                            // Advance to next string in sequence 6 -> 5 -> 4 -> 3 -> 2 -> 1
                            val curIdx = sortedStrings.indexOfFirst { it.stringNumber == matchedNum }
                            if (curIdx >= 0 && curIdx + 1 < sortedStrings.size) {
                                currentTargetNumber = sortedStrings[curIdx + 1].stringNumber
                            }
                        }
                    }
                }
            }
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

    val headstockDrawable = when (selectedInstrument) {
        TunerInstrumentType.GUITAR_6 -> com.chordapp.R.drawable.guitar_headstock
        TunerInstrumentType.BASS_4 -> com.chordapp.R.drawable.bass_headstock
        TunerInstrumentType.UKULELE -> com.chordapp.R.drawable.ukulele_headstock
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TunerDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            // Mic permission warning banner (if denied)
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

                // (b) Top Fade-Out Gradient Line: seamless into TunerDarkBg
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(TunerDarkBg, Color.Transparent)
                            )
                        )
                )

                // (c) Bottom Fade-Out Gradient Line: seamless into TunerDarkBg
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(56.dp)
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
                        isSilent = reading.isSilent,
                        targetString = currentTargetString,
                        isAutoMode = isAutoMode,
                        allTuned = allTuned
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
                        isSilent = reading.isSilent,
                        targetString = currentTargetString,
                        isAutoMode = isAutoMode,
                        allTuned = allTuned,
                        onReset = {
                            tunedStrings = emptySet()
                            currentTargetNumber = sortedStrings.firstOrNull()?.stringNumber ?: 6
                        },
                        showReset = tunedStrings.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Horizontal String Selector Row (6 -> 5 -> 4 -> 3 -> 2 -> 1)
                    TunerStringsRow(
                        strings = strings,
                        currentTargetNumber = currentTargetNumber,
                        tunedStrings = tunedStrings,
                        onSelect = { str ->
                            audioPlayer.playStringNote(selectedInstrument, str.stringNumber)
                            currentTargetNumber = str.stringNumber
                            // When tapped, it is NOT green until tuned to perfection:
                            tunedStrings = tunedStrings - str.stringNumber
                        }
                    )
                }
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
    val strings = LocalAppStrings.current
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
                    .border(1.dp, TunerCardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.close,
                    tint = TunerTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Kord",
                    color = TunerTextPrimary,
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
                    text = " " + strings.tunerTitle.lowercase().replaceFirstChar { it.uppercase() },
                    color = TunerMuted,
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
                .border(1.dp, TunerCardBorder, RoundedCornerShape(20.dp))
                .padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = strings.tunerAutoMode.uppercase(),
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
                    uncheckedTrackColor = Color(0xFF1A1F26),
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
                .border(1.dp, TunerCardBorder, RoundedCornerShape(10.dp))
                .clickable { onExpandChange(true) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentInstrument.displayName,
                        color = TunerTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
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
                .border(1.dp, TunerCardBorder, RoundedCornerShape(8.dp))
        ) {
            TunerInstrumentType.values().forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = type.displayName,
                                color = if (type == currentInstrument) TunerGold else TunerTextPrimary,
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
    isSilent: Boolean,
    targetString: TunerString?,
    isAutoMode: Boolean,
    allTuned: Boolean
) {
    val isInTune = reading.isInTune && !isSilent
    val noteColor by animateColorAsState(
        targetValue = when {
            allTuned -> TunerEmerald
            isSilent -> TunerMuted
            isInTune -> TunerEmerald
            else -> TunerTextPrimary
        },
        label = "noteColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isAutoMode && allTuned) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PAS",
                    color = TunerEmerald,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        } else if (!isSilent) {
            val stringNum = reading.matchedString?.stringNumber ?: targetString?.stringNumber
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = reading.noteName.ifEmpty { "--" },
                    color = noteColor,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                if (stringNum != null) {
                    Text(
                        text = "$stringNum",
                        color = noteColor.copy(alpha = 0.7f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                }
            }
        } else {
            // Silent state: show guided or manual target note faintly
            val targetNote = targetString
            if (targetNote != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = targetNote.noteName,
                        color = TunerGold.copy(alpha = 0.45f),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "${targetNote.stringNumber}",
                        color = TunerGold.copy(alpha = 0.30f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "--",
                    color = noteColor,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
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
        absDev <= 2.0f -> 0f
        absDev <= 5.0f -> {
            val t = (absDev - 2.0f) / 3.0f
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

    // GuitarTuna Grade Calm Analog Needle:
    // Critically damped (dampingRatio = 1.0f, no bounce) with low stiffness (140f)
    // gives that signature smooth, heavy, luxury VU-meter glide.
    val animatedCents by animateFloatAsState(
        targetValue = targetCents,
        animationSpec = spring(
            dampingRatio = 1.0f,
            stiffness = 140f
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
                            text = "0",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
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
    isSilent: Boolean,
    targetString: TunerString?,
    isAutoMode: Boolean,
    allTuned: Boolean,
    onReset: () -> Unit,
    showReset: Boolean
) {
    val strings = LocalAppStrings.current
    val isInTune = reading.isInTune && !isSilent

    val statusText = when {
        isAutoMode && allTuned -> strings.tunerAllInTune
        isSilent -> {
            if (targetString != null) {
                strings.tunerPluckTarget(targetString.stringNumber, targetString.noteName)
            } else {
                strings.tunerPluckString
            }
        }
        isInTune -> {
            val str = reading.matchedString
            if (str != null) {
                strings.tunerInTune(str.stringNumber, str.noteName)
            } else {
                "PAS! ${reading.noteName}"
            }
        }
        reading.centsDeviation < -4f -> {
            val str = reading.matchedString
            if (str != null) {
                strings.tunerTooLow(str.stringNumber, kotlin.math.abs(reading.centsDeviation.roundToInt()))
            } else {
                "${reading.noteName}: (${reading.centsDeviation.roundToInt()} cents)"
            }
        }
        else -> {
            val str = reading.matchedString
            if (str != null) {
                strings.tunerTooHigh(str.stringNumber, kotlin.math.abs(reading.centsDeviation.roundToInt()))
            } else {
                "${reading.noteName}: (+${reading.centsDeviation.roundToInt()} cents)"
            }
        }
    }

    val statusColor = when {
        isAutoMode && allTuned -> TunerEmerald
        isSilent -> if (targetString != null) TunerGold else TunerMuted
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
        if (showReset) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(TunerCardBg)
                    .border(1.dp, TunerCardBorder, RoundedCornerShape(6.dp))
                    .clickable { onReset() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = strings.tunerReset,
                    color = TunerMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HORIZONTAL STRINGS ROW (CLEAN BORDERLESS PILLS)
// ---------------------------------------------------------------------------
@Composable
private fun TunerStringsRow(
    strings: List<TunerString>,
    currentTargetNumber: Int,
    tunedStrings: Set<Int>,
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
            val isTuned = stringItem.stringNumber in tunedStrings
            val isCurrent = stringItem.stringNumber == currentTargetNumber

            val badgeBg by animateColorAsState(
                targetValue = when {
                    isTuned -> TunerEmerald.copy(alpha = 0.20f)
                    isCurrent -> TunerGold.copy(alpha = 0.20f)
                    else -> Color(0xFF14171C)
                },
                label = "pillBg"
            )

            val badgeBorderColor by animateColorAsState(
                targetValue = when {
                    isTuned -> TunerEmerald
                    isCurrent -> TunerGold
                    else -> TunerCardBorder
                },
                label = "pillBorder"
            )

            val badgeTextColor by animateColorAsState(
                targetValue = when {
                    isTuned -> TunerEmerald
                    isCurrent -> TunerGold
                    else -> TunerTextPrimary
                },
                label = "pillText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeBg)
                    .border(
                        border = BorderStroke(
                            width = if (isTuned || isCurrent) 1.5.dp else 1.dp,
                            color = badgeBorderColor
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(stringItem) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringItem.noteName,
                        color = badgeTextColor,
                        fontSize = 18.sp,
                        fontWeight = if (isTuned || isCurrent) FontWeight.ExtraBold else FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${stringItem.stringNumber}",
                        color = if (isTuned) TunerEmerald.copy(alpha = 0.8f) else TunerMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
