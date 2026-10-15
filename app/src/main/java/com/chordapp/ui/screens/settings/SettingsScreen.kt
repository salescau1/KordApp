package com.chordapp.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
            SettingsSection(title = "MODE") {
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
                            BandMode.SERVER to "Server",
                            BandMode.NORMAL to "Normal",
                            BandMode.CLIENT to "Client"
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
                                        text = "Status Server",
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
                                            text = "Aktif (${connectedClients.size} Klien)",
                                            color = Color(0xFF22C55E),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Nama Host
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Nama Host",
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
                                    Text(text = "Proteksi PIN", color = PrimaryText, fontSize = 13.5.sp)
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
                                        placeholder = { Text("Masukkan 4 digit PIN") },
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
                                            text = "Akses Web Browser (Stage Mirror)",
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
                                        text = "Safari iPhone, Android lain, atau laptop di Wi-Fi / Hotspot:",
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
                                                    Toast.makeText(context, "Link disalin: $displayWebUrl", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Salin",
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
                                                Text("Disconnect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    else -> {
                                        Text(
                                            text = "HOST TERDETEKSI:",
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
                                                        Text("Connect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                        text = "♫ Buka Layar Standby Panggung",
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
            SettingsSection(title = "APPEARANCE") {
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
                            text = "Theme",
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
                            text = "Language",
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
            val isDownloadingCatalog by viewModel.isDownloadingCatalog.collectAsState()
            val catalogDownloadProgress by viewModel.catalogDownloadProgress.collectAsState()
            val catalogDownloadStatus by viewModel.catalogDownloadStatus.collectAsState()
            val catalogDownloadError by viewModel.catalogDownloadError.collectAsState()

            SettingsSection(title = "MUSIC & CATALOG") {
                Column {
                    // Clean rows without subtitles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Artists", color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
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
                        Text(text = "Songs", color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
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
                        Text(text = "Catalog Size", color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
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
                        Text(
                            text = "Download Catalog",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 3 Tiers Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isTierAInstalled = stats.tierASongs > 0 || (stats.songs > 0 && stats.tierBSongs == 0 && stats.tierCSongs == 0)
                            val isTierBInstalled = stats.tierBSongs > 0
                            val isTierCInstalled = stats.tierCSongs > 0

                            // Paket A (50% - Easy Chord)
                            TierCatalogCard(
                                title = "Paket A",
                                count = "79k Lagu",
                                size = "40 MB",
                                isInstalled = isTierAInstalled,
                                isDownloading = isDownloadingCatalog,
                                actionLabel = if (isTierAInstalled) "Hapus" else "Unduh",
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
                                title = "Paket B",
                                count = "47k Lagu",
                                size = "24 MB",
                                isInstalled = isTierBInstalled,
                                isDownloading = isDownloadingCatalog,
                                actionLabel = if (isTierBInstalled) "Hapus" else "Unduh",
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
                                title = "Paket C",
                                count = "32k Lagu",
                                size = "16 MB",
                                isInstalled = isTierCInstalled,
                                isDownloading = isDownloadingCatalog,
                                actionLabel = if (isTierCInstalled) "Hapus" else "Unduh",
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

                        // Progress Indicator if downloading
                        if (isDownloadingCatalog) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = catalogDownloadStatus,
                                    color = PrimaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                LinearProgressIndicator(
                                    progress = { catalogDownloadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = AccentGold,
                                    trackColor = Border
                                )
                            }
                        }

                        if (catalogDownloadError != null) {
                            Text(
                                text = catalogDownloadError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 4. LICENSE (Device ID Removed, Masked Key + Intip, Transfer License)
            // =========================================================================
            SettingsSection(title = "LICENSE & DEVICE") {
                Column {
                    // License Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "License Status",
                                color = PrimaryText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Lisensi permanen pro",
                                color = MutedText,
                                fontSize = 11.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGold.copy(alpha = 0.14f))
                                .border(1.dp, AccentGold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isLicensed) "PRO AKTIF" else "INAKTIF",
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
                            text = "License Key",
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
                                    text = if (isKeyRevealed) "Tutup" else "Lihat",
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
                                text = "Email Pemulihan",
                                color = PrimaryText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEmailVerified && !recoveryEmail.isNullOrBlank()) {
                                    LicenseRepository.maskEmail(recoveryEmail)
                                } else {
                                    "Belum ditautkan"
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
                                        text = "Terverifikasi",
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
                                    text = "Tautkan",
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
                                text = "Transfer License",
                                color = PrimaryText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Proteksi kunci layar / sidik jari",
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
                                text = "Salin Kode",
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
            SettingsSection(title = "APPLICATION") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Version", color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
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
                        Text(text = "APK Size", color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
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
                                                "Aplikasi sudah versi terbaru (v${viewModel.versionName})",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Aplikasi sudah versi terbaru",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Check Update", color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isCheckingUpdate) "Memeriksa..." else "Cek Versi",
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(text = "›", color = AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        Text(text = "About", color = PrimaryText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "›", color = MutedText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = strings.offlineDisclaimer,
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    // Dialog Transfer License
    if (showTransferDialog) {
        val transferKey = savedKey ?: "KORD-PRO-8892"
        AlertDialog(
            onDismissRequest = { if (!isTransferring) showTransferDialog = false },
            title = {
                Text(
                    text = "Transfer Lisensi",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Peringatan: Memindahkan lisensi akan menyalin kode lisensi dan melepaskan hak akses dari perangkat ini agar dapat digunakan di perangkat lain.",
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
                            Toast.makeText(context, "Kode lisensi berhasil disalin!", Toast.LENGTH_SHORT).show()

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
                        text = if (isTransferring) "Menyalin..." else "Salin Kode",
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
                    Text("Batal", color = MutedText)
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
                        text = "Tautkan Email Pemulihan",
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
                        text = "Tautkan email Anda agar Anda dapat mereset dan memindahkan lisensi secara mandiri jika perangkat ini hilang atau rusak.",
                        color = MutedText,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = bindEmailInput,
                        onValueChange = { bindEmailInput = it.trim().lowercase(); bindError = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("nama@email.com", color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
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
                            placeholder = { Text("Masukkan 6 Digit OTP", color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
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
                                bindError = "Masukkan alamat email yang valid."
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
                                    Toast.makeText(context, "Kode OTP terkirim ke $bindEmailInput (simulasi: $otp)", Toast.LENGTH_LONG).show()
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
                            Text("Kirim OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (bindOtpInput.length < 6) {
                                bindError = "Kode OTP harus 6 digit angka."
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
                                    Toast.makeText(context, "Email pemulihan berhasil ditautkan!", Toast.LENGTH_LONG).show()
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
                            Text("Verifikasi OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBindEmailDialog = false },
                    enabled = !isBindLoading
                ) {
                    Text("Batal", color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog Konfirmasi Hapus Paket Katalog
    if (deleteTierTarget != null) {
        val target = deleteTierTarget!!
        val tierTitle = when (target) {
            "A" -> "Paket A (79k Lagu Easy Chord)"
            "B" -> "Paket B (47k Lagu Penyempurnaan)"
            "C" -> "Paket C (32k Lagu Arsip)"
            else -> "Paket $target"
        }
        AlertDialog(
            onDismissRequest = { deleteTierTarget = null },
            title = {
                Text("Hapus $tierTitle?", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Lagu-lagu di dalam $tierTitle akan dihapus dari penyimpanan perangkat Anda untuk menghemat ruang memori. Paket lagu lainnya akan tetap aman dan dapat dimainkan.",
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
                        Toast.makeText(context, "$tierTitle berhasil dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTierTarget = null }) {
                    Text("Batal", color = MutedText)
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
                Text("Pembaruan Tersedia!", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Versi baru v${info.latestVersionName} telah dirilis.",
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
                        Toast.makeText(context, "Unduhan dimulai. Cek notifikasi.", Toast.LENGTH_LONG).show()
                        if (!info.isForceUpdate) {
                            showUpdateDialog = false
                            isDownloadingUpdate = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Update", color = Color(0xFF0C0E12), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Nanti", color = MutedText)
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
                    Text("Hubungkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialogForServer = null }) {
                    Text("Batal", color = MutedText)
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
    isDownloading: Boolean,
    actionLabel: String? = null,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonText = actionLabel ?: if (isInstalled) "Hapus" else "Unduh"
    val isDanger = buttonText == "Hapus"
    val isSuccess = buttonText == "Aktif"

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RaisedSurface)
            .border(
                width = 1.dp,
                color = if (isInstalled) AccentGold else Border,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            color = if (isInstalled) AccentGold else PrimaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = count,
            color = MutedText,
            fontSize = 10.5.sp
        )
        Text(
            text = size,
            color = MutedText,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isDanger -> Color(0x24EF4444)
                        isSuccess -> Color(0x1AE5A93C)
                        else -> RaisedSurface
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isDanger -> Color(0xFFEF4444)
                        isSuccess -> AccentGold.copy(alpha = 0.5f)
                        else -> Border
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = !isDownloading) { onAction() }
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = when {
                    isDanger -> Color(0xFFEF4444)
                    isSuccess -> AccentGold
                    else -> MutedText
                },
                fontSize = 10.5.sp,
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
