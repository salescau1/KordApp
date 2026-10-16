package com.chordapp.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.localization.AppLanguage
import com.chordapp.localization.LocalAppStrings
import com.chordapp.p2p.BandMode
import com.chordapp.p2p.BandSessionManager
import com.chordapp.p2p.ClientConnectionState
import com.chordapp.p2p.DiscoveredServer
import com.chordapp.repositories.LicenseRepository
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAboutClick: () -> Unit,
    onLicenseTransferred: () -> Unit = {},
    onNavigateToStandby: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val stats by viewModel.catalogStats.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showTransferDialog by remember { mutableStateOf(false) }
    var isTransferring by remember { mutableStateOf(false) }
    var isKeyRevealed by remember { mutableStateOf(false) }
    var deleteTierTarget by remember { mutableStateOf<String?>(null) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.chordapp.repositories.AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }

    val bandMode by viewModel.bandMode.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val serverPin by viewModel.serverPin.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val connectedClients by viewModel.connectedClients.collectAsState()
    val webStageUrl by viewModel.webStageUrl.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var editingDeviceName by remember(deviceName) { mutableStateOf(deviceName) }
    var editingPin by remember(serverPin) { mutableStateOf(serverPin) }
    var showPinDialogForServer by remember { mutableStateOf<DiscoveredServer?>(null) }
    var pinDialogInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf<String?>(null) }

    val isLicensed = remember { LicenseRepository.isLicensed(context) }
    val savedKey = remember { LicenseRepository.getSavedLicenseKey(context) }
    var recoveryEmail by remember { mutableStateOf(LicenseRepository.getSavedRecoveryEmail(context)) }
    var isEmailVerified by remember { mutableStateOf(LicenseRepository.isEmailVerified(context)) }

    var showBindEmailDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var bindEmailInput by remember { mutableStateOf("") }
    var bindOtpInput by remember { mutableStateOf("") }
    var isBindOtpSent by remember { mutableStateOf(false) }
    var isBindLoading by remember { mutableStateOf(false) }
    var bindError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.settingsTitle,
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // =========================================================================
            // 1. MODE (Server | Normal | Client)
            // =========================================================================
            SettingsSection(title = strings.bandSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Switcher [ Server | Normal | Client ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RaisedSurface)
                            .border(1.dp, Border, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf(
                            BandMode.SERVER to strings.bandModeServer,
                            BandMode.NORMAL to strings.bandModeNormal,
                            BandMode.CLIENT to strings.bandModeClient
                        )
                        modes.forEach { (mode, label) ->
                            val selected = bandMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) AccentGold else Color.Transparent)
                                    .clickable { viewModel.setBandMode(mode) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color(0xFF0C0E12) else MutedText,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    when (bandMode) {
                        BandMode.NORMAL -> {
                            // Bersih tanpa subtitle / deskripsi tambahan
                        }

                        BandMode.SERVER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Status Server
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.bandServerActive,
                                        color = PrimaryText,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF22C55E), CircleShape)
                                        )
                                        Text(
                                            text = strings.bandConnectedCount(connectedClients.size),
                                            color = Color(0xFF22C55E),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Nama Host
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = strings.bandServerName,
                                        color = MutedText,
                                        fontSize = 11.5.sp
                                    )
                                    OutlinedTextField(
                                        value = editingDeviceName,
                                        onValueChange = {
                                            editingDeviceName = it
                                            viewModel.setDeviceName(it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentGold,
                                            unfocusedBorderColor = Border
                                        )
                                    )
                                }

                                // Proteksi PIN
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = strings.bandPinProtection, color = PrimaryText, fontSize = 13.5.sp)
                                    Switch(
                                        checked = isPinEnabled,
                                        onCheckedChange = { viewModel.setPinEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AccentGold,
                                            checkedTrackColor = AccentGold.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                if (isPinEnabled) {
                                    OutlinedTextField(
                                        value = editingPin,
                                        onValueChange = {
                                            if (it.length <= 4) {
                                                editingPin = it
                                                viewModel.setServerPin(it)
                                            }
                                        },
                                        placeholder = { Text(strings.bandPinPlaceholder) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentGold,
                                            unfocusedBorderColor = Border
                                        )
                                    )
                                }

                                // Link Akses Web Browser (Stage Mirror)
                                val resolvedIp = remember { BandSessionManager.getLocalIpAddress() ?: "192.168.43.1" }
                                val displayWebUrl = webStageUrl ?: "http://$resolvedIp:8990"

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(RaisedSurface)
                                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.stageMirrorTitle,
                                            color = PrimaryText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        )
                                        Text(
                                            text = "Port 8990",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = strings.stageMirrorInstruction,
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Background)
                                            .border(1.dp, Border.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = displayWebUrl,
                                            color = AccentGold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(RaisedSurface)
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Stage Mirror URL", displayWebUrl))
                                                    Toast.makeText(context, strings.linkCopiedToast(displayWebUrl), Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = strings.copyAction,
                                                color = PrimaryText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        BandMode.CLIENT -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (val state = connectionState) {
                                    is ClientConnectionState.Connected -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(RaisedSurface)
                                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFF22C55E), CircleShape)
                                                )
                                                Column {
                                                    Text(
                                                        text = state.serverName,
                                                        color = PrimaryText,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = state.host,
                                                        color = MutedText,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = { viewModel.disconnectBandClient() },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                                            ) {
                                                Text(strings.bandDisconnect, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    else -> {
                                        Text(
                                            text = strings.bandHostDetected,
                                            color = MutedText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (discoveredServers.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = strings.bandNoServersFound,
                                                    color = MutedText,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        } else {
                                            discoveredServers.forEach { srv ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(RaisedSurface)
                                                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = srv.name,
                                                                color = PrimaryText,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                            if (srv.requiresPin) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Lock,
                                                                    contentDescription = null,
                                                                    tint = AccentGold,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "${srv.host}:${srv.port}",
                                                            color = MutedText,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    Button(
                                                        onClick = {
                                                            if (srv.requiresPin) {
                                                                pinDialogInput = ""
                                                                pinDialogError = null
                                                                showPinDialogForServer = srv
                                                            } else {
                                                                viewModel.connectToBandServer(srv)
                                                                onNavigateToStandby()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = AccentGold,
                                                            contentColor = Color(0xFF0C0E12)
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(strings.bandConnect, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Tombol Buka Layar Standby Panggung
                                OutlinedButton(
                                    onClick = onNavigateToStandby,
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, AccentGold),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = AccentGold
                                    )
                                ) {
                                    Text(
                                        text = "♫ ${strings.bandOpenStandby}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. APPEARANCE (Theme Swatches & Language Capsule - No Dialogs!)
            // =========================================================================
            SettingsSection(title = strings.themeSection) {
                Column {
                    // Theme Row with 5 Circle Color Swatches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.themeLabel,
                            color = PrimaryText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 5 Circle Color Swatches: Dark, Light, Paper, Blackboard, Greenboard
                        val themeSwatches = listOf(
                            AppThemeMode.DARK to Color(0xFF0E1117),
                            AppThemeMode.LIGHT to Color(0xFFF5F6F8),
                            AppThemeMode.PAPER to Color(0xFFF4EFE6),
                            AppThemeMode.BLACKBOARD to Color(0xFF1C2228),
                            AppThemeMode.GREENBOARD to Color(0xFF14261C)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            themeSwatches.forEach { (mode, swatchColor) ->
                                val isSelected = currentTheme == mode
                                Box(
                                    modifier = Modifier
                                        .size(23.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) AccentGold else Border,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setTheme(mode) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(AccentGold, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Border)

                    // Language Row with Capsule Segment [ ID | EN ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.languageLabel,
                            color = PrimaryText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(RaisedSurface)
                                .border(1.dp, Border, RoundedCornerShape(18.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val langOptions = listOf(
                                AppLanguage.INDONESIAN to "ID",
                                AppLanguage.ENGLISH to "EN"
                            )
                            langOptions.forEach { (lang, label) ->
                                val isSelected = currentLanguage == lang
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) AccentGold else Color.Transparent)
                                        .clickable { viewModel.setLanguage(lang) }
                                        .padding(horizontal = 11.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF0C0E12) else MutedText,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. MUSIC & CATALOG (Clean stats + 3 Tiers: Paket A, Paket B, Paket C)
            // =========================================================================
            val tierDownloadState by viewModel.tierDownloadState.collectAsState()
            val isDownloadingCatalog = tierDownloadState.isDownloading
            val activeDownloadingTier = tierDownloadState.activeTier
            val catalogDownloadProgress = tierDownloadState.progress
            val catalogDownloadStatus = tierDownloadState.statusText
            val catalogDownloadError = tierDownloadState.error

            SettingsSection(title = strings.musicAndCatalogSection) {
                Column {
                    // Clean rows without subtitles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.artists, color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (stats.artists > 0) String.format(Locale.GERMAN, "%,d", stats.artists) else "0",
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    HorizontalDivider(color = Border)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.songs, color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (stats.songs > 0) String.format(Locale.GERMAN, "%,d", stats.songs) else "0",
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    HorizontalDivider(color = Border)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.catalogSize, color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatBytes(stats.catalogBytes, "0 MB"),
                            color = PrimaryText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                    }

                    HorizontalDivider(color = Border)

                    // Visualisasi 3 Tingkatan Database: Paket A, Paket B, Paket C
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = strings.downloadCatalogLabel,
                                color = PrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.tierSectionHint,
                                color = MutedText,
                                fontSize = 11.5.sp
                            )
                        }

                        // 3 Tiers Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isTierAInstalled = stats.tierASongs > 0 || (stats.songs > 0 && stats.tierBSongs == 0 && stats.tierCSongs == 0)
                            val isTierBInstalled = stats.tierBSongs > 0
                            val isTierCInstalled = stats.tierCSongs > 0

                            val isTierADownloading = isDownloadingCatalog && activeDownloadingTier == "A"
                            val isTierBDownloading = isDownloadingCatalog && activeDownloadingTier == "B"
                            val isTierCDownloading = isDownloadingCatalog && activeDownloadingTier == "C"

                            // Paket A (50% - Easy Chord)
                            TierCatalogCard(
                                title = strings.packageA,
                                count = strings.packageSongsCount("79k"),
                                size = "40 MB",
                                isInstalled = isTierAInstalled,
                                isThisTierDownloading = isTierADownloading,
                                downloadPercent = catalogDownloadProgress,
                                isAnyDownloading = isDownloadingCatalog,
                                actionLabel = if (isTierAInstalled) strings.deleteAction else strings.downloadAction,
                                onAction = {
                                    if (isTierAInstalled) {
                                        deleteTierTarget = "A"
                                    } else {
                                        viewModel.downloadTier("A")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Paket B (30% - Penyempurnaan & Pro)
                            TierCatalogCard(
                                title = strings.packageB,
                                count = strings.packageSongsCount("47k"),
                                size = "24 MB",
                                isInstalled = isTierBInstalled,
                                isThisTierDownloading = isTierBDownloading,
                                downloadPercent = catalogDownloadProgress,
                                isAnyDownloading = isDownloadingCatalog,
                                actionLabel = if (isTierBInstalled) strings.deleteAction else strings.downloadAction,
                                onAction = {
                                    if (isTierBInstalled) {
                                        deleteTierTarget = "B"
                                    } else {
                                        viewModel.downloadTier("B")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Paket C (20% - Non-Mainstream & Arsip)
                            TierCatalogCard(
                                title = strings.packageC,
                                count = strings.packageSongsCount("32k"),
                                size = "16 MB",
                                isInstalled = isTierCInstalled,
                                isThisTierDownloading = isTierCDownloading,
                                downloadPercent = catalogDownloadProgress,
                                isAnyDownloading = isDownloadingCatalog,
                                actionLabel = if (isTierCInstalled) strings.deleteAction else strings.downloadAction,
                                onAction = {
                                    if (isTierCInstalled) {
                                        deleteTierTarget = "C"
                                    } else {
                                        viewModel.downloadTier("C")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Elegant Card Progress if downloading in background
                        if (isDownloadingCatalog) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = RaisedSurface,
                                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Baris atas: Icon + Nama Paket + [ XX% ]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(15.dp),
                                                color = AccentGold,
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = strings.downloadingTierTitle(activeDownloadingTier ?: ""),
                                                color = PrimaryText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AccentGold.copy(alpha = 0.15f))
                                                .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$catalogDownloadProgress%",
                                                color = AccentGold,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    // Tengah: LinearProgressIndicator (tinggi 8.dp, rounded shape, warna AccentGold, track Border)
                                    LinearProgressIndicator(
                                        progress = { catalogDownloadProgress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = AccentGold,
                                        trackColor = Border
                                    )

                                    // Baris bawah: Teks status detail dan catatan kecil tierBackgroundNote
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (catalogDownloadStatus.isNotBlank()) {
                                            Text(
                                                text = catalogDownloadStatus,
                                                color = PrimaryText,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = strings.tierBackgroundNote,
                                            color = MutedText,
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (catalogDownloadError != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = catalogDownloadError,
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { viewModel.dismissCatalogError() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = strings.close,
                                            color = Color(0xFFEF4444),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. LICENSE (Device ID Removed, Masked Key + Intip, Transfer License)
            // =========================================================================
            SettingsSection(title = strings.licenseAndDeviceSection) {
                Column {
                    // License Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.licenseStatus,
                            color = PrimaryText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGold.copy(alpha = 0.14f))
                                .border(1.dp, AccentGold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isLicensed) strings.licenseActivePro else strings.licenseInactive,
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = Border)

                    // Masked License Key with Toggle View (Intip/Sensor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.licenseKey,
                            color = PrimaryText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val rawKey = savedKey ?: "KORD-PRO-8892"
                            val displayKey = if (isKeyRevealed) {
                                rawKey
                            } else {
                                "KORD-••••-••••"
                            }

                            Text(
                                text = displayKey,
                                color = PrimaryText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RaisedSurface)
                                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                                    .clickable { isKeyRevealed = !isKeyRevealed }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isKeyRevealed) strings.hideKey else strings.revealKey,
                                    color = PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Border)

                    // Baris Email Pemulihan (duduk tenang)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = strings.recoveryEmailTitle,
                                color = PrimaryText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEmailVerified && !recoveryEmail.isNullOrBlank()) {
                                    LicenseRepository.maskEmail(recoveryEmail)
                                } else {
                                    strings.notLinkedStatus
                                },
                                color = if (isEmailVerified) Color(0xFF22C55E) else MutedText,
                                fontSize = 11.5.sp,
                                fontFamily = if (isEmailVerified) FontFamily.Monospace else FontFamily.Default
                            )
                        }

                        if (isEmailVerified && !recoveryEmail.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF22C55E).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = strings.verifiedStatus,
                                        color = Color(0xFF22C55E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RaisedSurface)
                                    .border(1.dp, AccentGold, RoundedCornerShape(8.dp))
                                    .clickable {
                                        bindEmailInput = ""
                                        bindOtpInput = ""
                                        isBindOtpSent = false
                                        bindError = null
                                        showBindEmailDialog = true
                                    }
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = strings.linkAction,
                                    color = AccentGold,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Border)

                    // Transfer License Action with Confirmation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = strings.transferLicense,
                                color = PrimaryText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.transferLicenseDesc,
                                color = MutedText,
                                fontSize = 11.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RaisedSurface)
                                .border(1.dp, AccentGold, RoundedCornerShape(8.dp))
                                .clickable { showTransferDialog = true }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = strings.copyKeyAction,
                                color = AccentGold,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 5. APPLICATION (Version, APK Size, Check Update, About)
            // =========================================================================
            SettingsSection(title = strings.applicationSection) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.version, color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "v${viewModel.versionName}",
                            color = MutedText,
                            fontSize = 13.5.sp
                        )
                    }

                    HorizontalDivider(color = Border)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.apkSize, color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatBytes(stats.appBytes, "24.8 MB"),
                            color = MutedText,
                            fontSize = 13.5.sp
                        )
                    }

                    HorizontalDivider(color = Border)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCheckingUpdate) {
                                isCheckingUpdate = true
                                scope.launch {
                                    val result = com.chordapp.repositories.UpdateRepository.checkAppUpdate(context)
                                    isCheckingUpdate = false
                                    result.onSuccess { info ->
                                        if (info.hasUpdate) {
                                            updateInfo = info
                                            showUpdateDialog = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                strings.appUpToDate(viewModel.versionName),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            strings.appUpToDate(viewModel.versionName),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.checkUpdate, color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isCheckingUpdate) strings.checkingUpdate else strings.checkVersion,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(text = "›", color = AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Border)

                    // Masukan & Saran (Feedback)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = strings.feedbackAndSuggestions,
                                color = PrimaryText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "kekalclip@gmail.com",
                                color = MutedText,
                                fontSize = 11.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RaisedSurface)
                                .border(1.dp, AccentGold, RoundedCornerShape(8.dp))
                                .clickable { showFeedbackDialog = true }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = strings.sendEmailAction,
                                color = AccentGold,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Border)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAboutClick)
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.aboutApp, color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "›", color = MutedText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    // Dialog Feedback & Suggestions (Kirim Email)
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                    Text(
                        text = strings.feedbackDialogTitle,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val device = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                    val version = viewModel.versionName

                    val categories = listOf(
                        Triple(strings.feedbackFeatureCategory, "[KordApp-Feature] ", strings.feedbackFeatureBody(version, device)),
                        Triple(strings.feedbackBugCategory, "[KordApp-Bug] ", strings.feedbackBugBody(version, device)),
                        Triple(strings.feedbackGeneralCategory, "[KordApp-Feedback] ", strings.feedbackGeneralBody(version, device))
                    )

                    categories.forEach { (label, subjectTag, bodyTemplate) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RaisedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showFeedbackDialog = false
                                    try {
                                        val mailtoUri = Uri.parse("mailto:kekalclip@gmail.com?subject=${Uri.encode(subjectTag)}&body=${Uri.encode(bodyTemplate)}")
                                        val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                                            putExtra(Intent.EXTRA_SUBJECT, subjectTag)
                                            putExtra(Intent.EXTRA_TEXT, bodyTemplate)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, strings.noEmailAppFound, Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                color = PrimaryText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog Transfer License
    if (showTransferDialog) {
        val transferKey = savedKey ?: "KORD-PRO-8892"
        AlertDialog(
            onDismissRequest = { if (!isTransferring) showTransferDialog = false },
            title = {
                Text(
                    text = strings.transferLicense,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.transferDialogWarning,
                        color = MutedText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RaisedSurface)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = transferKey,
                            color = AccentGold,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isTransferring = true
                        scope.launch {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("KordApp License", transferKey))
                            Toast.makeText(context, strings.keyCopiedToast, Toast.LENGTH_SHORT).show()

                            val (ok, msg) = LicenseRepository.transferLicenseSelfService(context)
                            isTransferring = false
                            showTransferDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (ok) {
                                onLicenseTransferred()
                            }
                        }
                    },
                    enabled = !isTransferring,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isTransferring) strings.copyingAction else strings.copyKeyAction,
                        color = Color(0xFF0C0E12),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTransferDialog = false },
                    enabled = !isTransferring
                ) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog Tautkan Email Pemulihan dari Settings
    if (showBindEmailDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBindLoading) showBindEmailDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                    Text(
                        text = strings.bindEmailDialogTitle,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.bindEmailDialogDesc,
                        color = MutedText,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = bindEmailInput,
                        onValueChange = { bindEmailInput = it.trim().lowercase(); bindError = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(strings.recoveryEmailPlaceholder, color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AccentGold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isBindOtpSent && !isBindLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (isBindOtpSent) {
                        OutlinedTextField(
                            value = bindOtpInput,
                            onValueChange = { if (it.length <= 6) bindOtpInput = it.filter { c -> c.isDigit() }; bindError = null },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(strings.enter6DigitOtpPlaceholder, color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AccentGold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !isBindLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = Border,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    bindError?.let { err ->
                        Text(text = err, color = ActiveHeart, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                if (!isBindOtpSent) {
                    Button(
                        onClick = {
                            if (bindEmailInput.isBlank() || !bindEmailInput.contains("@")) {
                                bindError = strings.invalidEmailError
                                return@Button
                            }
                            isBindLoading = true
                            bindError = null
                            scope.launch {
                                val otp = (100000..999999).random().toString()
                                val (ok, msg) = LicenseRepository.requestEmailBindingOtp(context, bindEmailInput, generatedOtp = otp)
                                isBindLoading = false
                                if (ok) {
                                    isBindOtpSent = true
                                    Toast.makeText(context, strings.otpSentToast(bindEmailInput), Toast.LENGTH_LONG).show()
                                } else {
                                    bindError = msg
                                }
                            }
                        },
                        enabled = !isBindLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isBindLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(strings.sendOtpBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (bindOtpInput.length < 6) {
                                bindError = strings.otpLengthError
                                return@Button
                            }
                            isBindLoading = true
                            bindError = null
                            scope.launch {
                                val (ok, msg) = LicenseRepository.verifyEmailOtp(context, bindOtpInput)
                                isBindLoading = false
                                if (ok) {
                                    recoveryEmail = bindEmailInput
                                    isEmailVerified = true
                                    showBindEmailDialog = false
                                    Toast.makeText(context, strings.bindEmailSuccess, Toast.LENGTH_LONG).show()
                                } else {
                                    bindError = msg
                                }
                            }
                        },
                        enabled = !isBindLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isBindLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(strings.verifyBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBindEmailDialog = false },
                    enabled = !isBindLoading
                ) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog Konfirmasi Hapus Paket Katalog
    if (deleteTierTarget != null) {
        val target = deleteTierTarget!!
        AlertDialog(
            onDismissRequest = { deleteTierTarget = null },
            title = {
                Text(strings.deletePackageDialogTitle(target), color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = strings.deletePackageDialogDesc(target),
                    color = MutedText,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = target
                        deleteTierTarget = null
                        viewModel.deleteTier(toDelete)
                        Toast.makeText(context, strings.packageDeletedToast(toDelete), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.deleteAction, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTierTarget = null }) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog Update APK Tersedia
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = {
                if (!info.isForceUpdate && !isDownloadingUpdate) showUpdateDialog = false
            },
            title = {
                Text(strings.updateAvailableTitle, color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.newVersionAvailable(info.latestVersionName),
                        color = AccentGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = info.changelog,
                        color = MutedText,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDownloadingUpdate = true
                        com.chordapp.repositories.UpdateRepository.startDownloadAndInstall(
                            context,
                            info.downloadUrl,
                            "KordApp-v${info.latestVersionName}.apk"
                        )
                        Toast.makeText(context, strings.downloadStartedToast, Toast.LENGTH_LONG).show()
                        if (!info.isForceUpdate) {
                            showUpdateDialog = false
                            isDownloadingUpdate = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.updateAction, color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(strings.laterAction, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog PIN untuk Client Connect
    if (showPinDialogForServer != null) {
        val srv = showPinDialogForServer!!
        AlertDialog(
            onDismissRequest = { showPinDialogForServer = null },
            title = { Text(strings.bandEnterPinTitle, color = PrimaryText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.bandEnterPinPrompt, color = MutedText, fontSize = 13.sp)
                    OutlinedTextField(
                        value = pinDialogInput,
                        onValueChange = { if (it.length <= 4) pinDialogInput = it },
                        placeholder = { Text(strings.bandPinPlaceholder) },
                        singleLine = true,
                        isError = pinDialogError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border
                        )
                    )
                    if (pinDialogError != null) {
                        Text(
                            text = pinDialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.connectToBandServer(srv, pinDialogInput) { err ->
                            pinDialogError = err
                        }
                        showPinDialogForServer = null
                        onNavigateToStandby()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.bandConnect, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialogForServer = null }) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = title,
            color = MutedText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun TierCatalogCard(
    title: String,
    count: String,
    size: String,
    isInstalled: Boolean,
    isThisTierDownloading: Boolean,
    downloadPercent: Int,
    isAnyDownloading: Boolean,
    actionLabel: String? = null,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val buttonText = when {
        isThisTierDownloading -> "$downloadPercent%"
        actionLabel != null -> actionLabel
        isInstalled -> strings.deleteAction
        else -> strings.downloadAction
    }
    val isDanger = isInstalled && !isThisTierDownloading

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RaisedSurface)
            .border(
                width = if (isThisTierDownloading) 1.5.dp else 1.dp,
                color = when {
                    isThisTierDownloading -> AccentGold
                    isInstalled -> AccentGold.copy(alpha = 0.6f)
                    else -> Border
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            color = if (isInstalled || isThisTierDownloading) AccentGold else PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = count,
            color = MutedText,
            fontSize = 11.sp
        )
        Text(
            text = size,
            color = MutedText,
            fontSize = 10.5.sp
        )

        Spacer(Modifier.height(6.dp))

        // Action Button (Hapus / Unduh / XX%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        isDanger -> Color(0x24EF4444)
                        else -> AccentGold.copy(alpha = 0.15f)
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isDanger -> Color(0xFFEF4444)
                        else -> AccentGold
                    },
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(enabled = !isAnyDownloading) { onAction() }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = when {
                    isDanger -> Color(0xFFEF4444)
                    else -> AccentGold
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatBytes(bytes: Long, fallback: String): String = when {
    bytes <= 0L -> fallback
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
