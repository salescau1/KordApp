package com.chordapp.ui.screens.settings

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chordapp.R
import com.chordapp.settings.CustomPlayerConfig
import com.chordapp.settings.CustomPlayerManager
import com.chordapp.ui.screens.player.AppSheetText
import com.chordapp.ui.theme.*
import java.io.File
import kotlin.math.roundToInt

@Composable
fun CustomPlayerDialog(
    initialConfig: CustomPlayerConfig,
    onDismiss: () -> Unit,
    onApply: (CustomPlayerConfig) -> Unit
) {
    val context = LocalContext.current

    // Working state inside the dialog
    var chordHue by remember { mutableFloatStateOf(initialConfig.chordHue) }
    var chordBrightness by remember { mutableFloatStateOf(initialConfig.chordBrightness) }

    var lyricHue by remember { mutableFloatStateOf(initialConfig.lyricHue) }
    var lyricBrightness by remember { mutableFloatStateOf(initialConfig.lyricBrightness) }

    var sectionCustom by remember { mutableStateOf(initialConfig.sectionCustom) }
    var sectionHue by remember { mutableFloatStateOf(initialConfig.sectionHue) }
    var sectionBrightness by remember { mutableFloatStateOf(initialConfig.sectionBrightness) }

    var shadowEnabled by remember { mutableStateOf(initialConfig.shadowEnabled) }
    var shadowLevel by remember { mutableStateOf(initialConfig.shadowLevel) }

    var outlineEnabled by remember { mutableStateOf(initialConfig.outlineEnabled) }
    var outlineLevel by remember { mutableStateOf(initialConfig.outlineLevel) }

    var bgEnabled by remember { mutableStateOf(initialConfig.bgEnabled) }
    var bgPath by remember { mutableStateOf(initialConfig.bgPath) }
    var bgDimmer by remember { mutableFloatStateOf(initialConfig.bgDimmer) }

    var textureEnabled by remember { mutableStateOf(initialConfig.textureEnabled) }
    var textureType by remember { mutableStateOf(initialConfig.textureType) }

    // Launcher for image import from gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val destFile = File(context.filesDir, "custom_player_bg.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                bgPath = destFile.absolutePath
                bgEnabled = true
                Toast.makeText(context, "Background berhasil dipilih", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamic color calculations for live preview
    val currentChordColor = remember(chordHue, chordBrightness) {
        CustomPlayerConfig.hslToColor(chordHue, chordBrightness)
    }
    val currentLyricColor = remember(lyricHue, lyricBrightness) {
        CustomPlayerConfig.hslToColor(lyricHue, lyricBrightness)
    }
    val currentSectionColor = remember(sectionCustom, sectionHue, sectionBrightness, currentLyricColor) {
        if (sectionCustom) CustomPlayerConfig.hslToColor(sectionHue, sectionBrightness) else currentLyricColor
    }

    Dialog(
        onDismissRequest = { /* Modal terkunci: tidak bisa ditutup kecuali Batal/Terapkan */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Kustom Player)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kustom Player",
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MutedText
                        )
                    }
                }

                HorizontalDivider(color = Border, thickness = 0.8.dp)

                // =====================================================================
                // 1. STICKY PREVIEW (BAKU DI ATAS - TIDAK TERPENGARUH SCROLL)
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(135.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Background),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Layer Background Foto (Fill / ContentScale.Crop)
                            if (bgEnabled && bgPath != null && File(bgPath!!).exists()) {
                                AsyncImage(
                                    model = File(bgPath!!),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Default background texture based on active theme
                                val activeTexture = TextureType
                                if (activeTexture != ThemeTextureType.NONE) {
                                    val resId = when (activeTexture) {
                                        ThemeTextureType.LEATHER -> R.drawable.texture_leather
                                        ThemeTextureType.PARCHMENT -> R.drawable.texture_paper
                                        ThemeTextureType.CHALKBOARD -> R.drawable.texture_blackboard
                                        ThemeTextureType.GREENBOARD -> R.drawable.texture_greenboard
                                        ThemeTextureType.CRIMSON_SHADOW -> R.drawable.texture_crimson_shadow
                                        else -> null
                                    }
                                    if (resId != null) {
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alpha = 0.5f
                                        )
                                    }
                                }
                            }

                            // Layer Dimmer (Kegelapan Background)
                            if (bgEnabled && bgPath != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = bgDimmer))
                                )
                            }

                            // Layer Filter Tekstur Serat Foto (Jika ON)
                            if (bgEnabled && textureEnabled) {
                                when (textureType) {
                                    "paper" -> {
                                        Image(
                                            painter = painterResource(id = R.drawable.texture_paper),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alpha = 0.40f
                                        )
                                    }
                                    "canvas" -> {
                                        Image(
                                            painter = painterResource(id = R.drawable.texture_leather),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alpha = 0.40f
                                        )
                                    }
                                    "halftone" -> {
                                        // Halftone retro dot matrix overlay
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val dotSpacing = 16f
                                            val dotRadius = 2.2f
                                            var y = 0f
                                            while (y < size.height) {
                                                var x = 0f
                                                while (x < size.width) {
                                                    drawCircle(
                                                        color = Color.Black.copy(alpha = 0.45f),
                                                        radius = dotRadius,
                                                        center = Offset(x, y)
                                                    )
                                                    x += dotSpacing
                                                }
                                                y += dotSpacing
                                            }
                                        }
                                    }
                                }
                            }

                            // Live Text Rendering (Am \n Lyrics \n [Chorus])
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Chord "Am"
                                AppSheetText(
                                    text = "Am",
                                    color = currentChordColor,
                                    fontSize = 16f,
                                    fontWeight = FontWeight.Bold,
                                    shadowEnabled = shadowEnabled,
                                    shadowLevel = shadowLevel,
                                    outlineEnabled = outlineEnabled,
                                    outlineLevel = outlineLevel
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Lyric "Lyrics"
                                AppSheetText(
                                    text = "Lyrics",
                                    color = currentLyricColor,
                                    fontSize = 15f,
                                    fontWeight = FontWeight.SemiBold,
                                    shadowEnabled = shadowEnabled,
                                    shadowLevel = shadowLevel,
                                    outlineEnabled = outlineEnabled,
                                    outlineLevel = outlineLevel
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Section "[Chorus]"
                                AppSheetText(
                                    text = "[Chorus]",
                                    color = currentSectionColor,
                                    fontSize = 14f,
                                    fontWeight = FontWeight.Bold,
                                    shadowEnabled = shadowEnabled,
                                    shadowLevel = shadowLevel,
                                    outlineEnabled = outlineEnabled,
                                    outlineLevel = outlineLevel
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Border, thickness = 0.8.dp)

                // =====================================================================
                // 2. SCROLLABLE CONTROLS
                // =====================================================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    // ---- 1. CHORD ----
                    ColorControlGroup(
                        title = "Chord",
                        currentColor = currentChordColor,
                        hue = chordHue,
                        onHueChange = { chordHue = it },
                        brightness = chordBrightness,
                        onBrightnessChange = { chordBrightness = it }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // ---- 2. LIRIK ----
                    ColorControlGroup(
                        title = "Lirik",
                        currentColor = currentLyricColor,
                        hue = lyricHue,
                        onHueChange = { lyricHue = it },
                        brightness = lyricBrightness,
                        onBrightnessChange = { lyricBrightness = it }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // ---- 3. [CHORUS] ----
                    ColorControlGroup(
                        title = "[Chorus]",
                        currentColor = currentSectionColor,
                        hue = sectionHue,
                        onHueChange = { sectionHue = it },
                        brightness = sectionBrightness,
                        onBrightnessChange = { sectionBrightness = it },
                        enabled = sectionCustom,
                        trailingContent = {
                            Switch(
                                checked = sectionCustom,
                                onCheckedChange = { sectionCustom = it }
                            )
                        }
                    )
                    if (!sectionCustom) {
                        Text(
                            text = "Mengikuti warna lirik",
                            color = MutedText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ---- 4. SHADOW ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shadow",
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = shadowEnabled,
                            onCheckedChange = { shadowEnabled = it }
                        )
                    }
                    if (shadowEnabled) {
                        SegmentedOptionRow(
                            options = listOf("thin" to "Tipis", "medium" to "Sedang", "thick" to "Tebal"),
                            selected = shadowLevel,
                            onSelect = { shadowLevel = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ---- 5. OUTLINE ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Outline",
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = outlineEnabled,
                            onCheckedChange = { outlineEnabled = it }
                        )
                    }
                    if (outlineEnabled) {
                        SegmentedOptionRow(
                            options = listOf("thin" to "Tipis", "medium" to "Sedang", "thick" to "Tebal"),
                            selected = outlineLevel,
                            onSelect = { outlineLevel = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ---- 6. BACKGROUND ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Background",
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = bgEnabled,
                            onCheckedChange = { bgEnabled = it }
                        )
                    }

                    if (bgEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        // Tombol Pilih Foto
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BrandGold)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (bgPath != null) "Ganti Background (Galeri)" else "+ Pilih Background (Galeri)",
                                color = BrandGold,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kegelapan (${(bgDimmer * 100).toInt()}%)",
                                color = MutedText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(110.dp)
                            )
                            UnifiedGradientSlider(
                                value = bgDimmer,
                                onValueChange = { bgDimmer = it },
                                valueRange = 0.0f..0.85f,
                                gradientBrush = remember {
                                    Brush.horizontalGradient(listOf(Color(0xFF777777), Color.Black))
                                },
                                thumbColor = Color(0xFF222222),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tekstur",
                                color = PrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = textureEnabled,
                                onCheckedChange = { textureEnabled = it }
                            )
                        }

                        if (textureEnabled) {
                            SegmentedOptionRow(
                                options = listOf("paper" to "Kertas", "canvas" to "Kanvas", "halftone" to "Halftone"),
                                selected = textureType,
                                onSelect = { textureType = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                HorizontalDivider(color = Border, thickness = 0.8.dp)

                // =====================================================================
                // 3. STICKY ACTION BUTTONS (BATAL & TERAPKAN)
                // =====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Text(text = "Batal", color = MutedText, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val updated = CustomPlayerConfig(
                                enabled = true,
                                chordHue = chordHue,
                                chordBrightness = chordBrightness,
                                lyricHue = lyricHue,
                                lyricBrightness = lyricBrightness,
                                sectionCustom = sectionCustom,
                                sectionHue = sectionHue,
                                sectionBrightness = sectionBrightness,
                                shadowEnabled = shadowEnabled,
                                shadowLevel = shadowLevel,
                                outlineEnabled = outlineEnabled,
                                outlineLevel = outlineLevel,
                                bgEnabled = bgEnabled,
                                bgPath = bgPath,
                                bgDimmer = bgDimmer,
                                textureEnabled = textureEnabled,
                                textureType = textureType
                            )
                            onApply(updated)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGold)
                    ) {
                        Text(text = "Terapkan", color = Color(0xFF1B130E), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedGradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    gradientBrush: Brush,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(valueRange) {
                val thumbRadiusPx = 11.dp.toPx()
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val usableWidth = (width - thumbRadiusPx * 2).coerceAtLeast(1f)
                    val clampedX = (offset.x - thumbRadiusPx).coerceIn(0f, usableWidth)
                    val newFraction = clampedX / usableWidth
                    onValueChange(valueRange.start + newFraction * rangeSpan)
                }
            }
            .pointerInput(valueRange) {
                val thumbRadiusPx = 11.dp.toPx()
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat()
                        val usableWidth = (width - thumbRadiusPx * 2).coerceAtLeast(1f)
                        val clampedX = (change.position.x - thumbRadiusPx).coerceIn(0f, usableWidth)
                        val newFraction = clampedX / usableWidth
                        onValueChange(valueRange.start + newFraction * rangeSpan)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val thumbRadiusPx = with(density) { 11.dp.toPx() }
        val usableWidth = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)
        val thumbOffsetPx = fraction * usableWidth

        // 1. The Gradient Track (Warna langsung menyatu sebagai track slider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(gradientBrush)
                .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(7.dp))
        )

        // 2. Thumb: Handle bulat langsung bergeser di atas gradient track
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                .size(22.dp)
                .shadow(5.dp, CircleShape)
                .background(thumbColor, CircleShape)
                .border(2.5.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun ColorControlGroup(
    title: String,
    currentColor: Color,
    hue: Float,
    onHueChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val rainbowBrush = remember {
        Brush.horizontalGradient(
            listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red
            )
        )
    }

    // Dynamic brightness gradient corresponding to this exact hue
    val brightnessBrush = remember(hue) {
        val pureColor = CustomPlayerConfig.hslToColor(hue, 0.50f)
        Brush.horizontalGradient(
            listOf(
                Color.Black,
                pureColor,
                Color.White
            )
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Warna (Rainbow Track Slider Langsung)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Warna",
                    color = MutedText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(72.dp)
                )
                UnifiedGradientSlider(
                    value = hue,
                    onValueChange = onHueChange,
                    valueRange = 0f..360f,
                    gradientBrush = rainbowBrush,
                    thumbColor = currentColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Kecerahan (Brightness Track Slider Langsung)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kecerahan",
                    color = MutedText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(72.dp)
                )
                UnifiedGradientSlider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.15f..1.0f,
                    gradientBrush = brightnessBrush,
                    thumbColor = currentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SegmentedOptionRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            val containerColor = if (isSelected) BrandGold else CardSurface
            val textColor = if (isSelected) Color(0xFF1B130E) else PrimaryText
            val border = if (isSelected) BorderStroke(1.dp, BrandGold) else BorderStroke(1.dp, Border)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerColor)
                    .border(border, RoundedCornerShape(8.dp))
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
