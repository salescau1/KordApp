package com.chordapp.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private enum class SecurityPinAction {
    REVEAL_LICENSE,
    TRANSFER_LICENSE,
    DISABLE_PIN
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAboutClick: () -> Unit,
    onLicenseTransferred: () -> Unit = {},
    onNavigateToStandby: () -> Unit = {}
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val strings = remember(currentLanguage) { com.chordapp.localization.AppLanguageManager.strings(currentLanguage) }
    val currentTheme by viewModel.currentTheme.collectAsState()
    val stats by viewModel.catalogStats.collectAsState()
    val scrapedCount by viewModel.scrapedCount.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importKordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importScrapedFromUri(it) { count, error ->
                if (error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Berhasil mengimpor $count lagu ke Scraped", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showTransferDialog by remember { mutableStateOf(false) }
    var isTransferring by remember { mutableStateOf(false) }
    var isKeyRevealed by remember { mutableStateOf(false) }
    var deleteTierTarget by remember { mutableStateOf<String?>(null) }
    var showClearScrapedDialog by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.chordapp.repositories.AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }

    val bandMode by viewModel.bandMode.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val connectedClients by viewModel.connectedClients.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val serverPin by viewModel.serverPin.collectAsState()
    val isServerPinEnabled by viewModel.isPinEnabled.collectAsState()

    var showRenameDeviceDialog by remember { mutableStateOf(false) }
    var renameDeviceInput by remember { mutableStateOf("") }
    var showServerPinDialog by remember { mutableStateOf(false) }
    var serverPinInput by remember { mutableStateOf("") }
    var showPinDialogForServer by remember { mutableStateOf<DiscoveredServer?>(null) }
    var pinDialogInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf<String?>(null) }

    var isSecurityPinEnabled by remember { mutableStateOf(LicenseRepository.isSecurityPinEnabled(context)) }
    var hasSecurityPin by remember { mutableStateOf(LicenseRepository.getSecurityPin(context).isNotEmpty()) }

    var pendingSecurityPinAction by remember { mutableStateOf<SecurityPinAction?>(null) }
    var verifyPinInput by remember { mutableStateOf("") }
    var verifyPinError by remember { mutableStateOf<String?>(null) }

    var showSetupSecurityPinDialog by remember { mutableStateOf(false) }
    var setupSecurityPinInput by remember { mutableStateOf("") }

    var showChangeSecurityPinDialog by remember { mutableStateOf(false) }
    var changeOldPinInput by remember { mutableStateOf("") }
    var changeNewPinInput by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf<String?>(null) }

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
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.settingsTitle,
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Personal Configuration",
                    color = MutedText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Global Language Switch [ ID | EN ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(RaisedSurface)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
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
                            .padding(horizontal = 10.dp, vertical = 4.dp),
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
                                // 0 Connected
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
                                        text = "${connectedClients.size} Connected",
                                        color = Color(0xFF22C55E),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Device Name
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Device Name",
                                        color = MutedText,
                                        fontSize = 11.5.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = RaisedSurface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 14.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = deviceName,
                                                    color = PrimaryText,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                renameDeviceInput = deviceName
                                                showRenameDeviceDialog = true
                                            },
                                            modifier = Modifier.height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentGold,
                                                contentColor = Color(0xFF0C0E12)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Text(
                                                text = "Rename",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                }

                                // PIN Protection
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "PIN Protection", color = PrimaryText, fontSize = 13.5.sp)
                                        if (isServerPinEnabled && serverPin.isNotBlank()) {
                                            Text(
                                                text = "[$serverPin]",
                                                color = AccentGold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    serverPinInput = serverPin
                                                    showServerPinDialog = true
                                                }
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (serverPin.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(RaisedSurface)
                                                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        serverPinInput = serverPin
                                                        showServerPinDialog = true
                                                    }
                                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Edit",
                                                    color = PrimaryText,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isServerPinEnabled,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (serverPin.length == 4) {
                                                        viewModel.setPinEnabled(true)
                                                    } else {
                                                        serverPinInput = serverPin.ifBlank { "" }
                                                        showServerPinDialog = true
                                                    }
                                                } else {
                                                    viewModel.setPinEnabled(false)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = AccentGold,
                                                checkedTrackColor = AccentGold.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                }

                                // WEB Access (Rapet / Compact, Pure IP Address with Refresh Button)
                                var resolvedIp by remember { mutableStateOf(BandSessionManager.getLocalIpAddress() ?: "192.168.43.1") }
                                val ipUrl = "http://$resolvedIp:8990"

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(RaisedSurface)
                                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Header Row: WEB Access + Logo Refresh
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "WEB Access",
                                                color = PrimaryText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Work on same network!",
                                                color = MutedText,
                                                fontSize = 10.5.sp
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(CardSurface)
                                                .border(0.8.dp, Border, CircleShape)
                                                .clickable {
                                                    resolvedIp = BandSessionManager.getLocalIpAddress() ?: "192.168.43.1"
                                                    Toast.makeText(context, "IP Address: $resolvedIp", Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh IP",
                                                tint = AccentGold,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    // IP Address Row (Rapet)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Background)
                                            .border(1.dp, Border.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 9.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "IP Address",
                                                color = MutedText,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = ipUrl,
                                                color = AccentGold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(RaisedSurface)
                                                .border(0.8.dp, Border.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Stage Mirror IP", ipUrl))
                                                    Toast.makeText(context, strings.linkCopiedToast(ipUrl), Toast.LENGTH_SHORT).show()
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
            // 2. APPEARANCE (Theme Card Slider - [blackboard][leaves]<terang>[paper])
            // =========================================================================
            SettingsSection(title = strings.themeSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    InfiniteThemeSlider(
                        currentTheme = currentTheme,
                        onThemeSelect = { viewModel.setTheme(it) }
                    )
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

            SettingsSection(title = "KORDAPP CATALOG") {
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
            // 3.5. USER CATALOG (Dua Card Terpisah: Scraped & Created)
            // =========================================================================
            SettingsSection(title = "USER CATALOG") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Scraped
                        UserCatalogModuleCard(
                            title = "Scraped",
                            subtitle = "Comotan Web",
                            countText = if (scrapedCount > 0) "$scrapedCount Lagu" else "0 Lagu",
                            headerIcon = Icons.Default.Public,
                            headerIconColor = Color(0xFF38BDF8),
                            actions = listOf(
                                UserCatalogActionItem(Icons.Default.Sync, "Sync P2P", PrimaryText) {
                                    Toast.makeText(context, "Sync P2P (Scraped)", Toast.LENGTH_SHORT).show()
                                },
                                UserCatalogActionItem(Icons.Default.FileUpload, "Export", PrimaryText) {
                                    viewModel.exportScraped(
                                        onSuccess = { file ->
                                            try {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/octet-stream"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    putExtra(Intent.EXTRA_SUBJECT, file.name)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Bagikan file .kord"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal membagikan: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                UserCatalogActionItem(Icons.Default.FileDownload, "Import", PrimaryText) {
                                    importKordLauncher.launch(arrayOf("*/*"))
                                },
                                UserCatalogActionItem(Icons.Default.DeleteOutline, "Hapus", Color(0xFFEF4444)) {
                                    if (scrapedCount > 0) {
                                        showClearScrapedDialog = true
                                    } else {
                                        Toast.makeText(context, "Katalog Scraped masih kosong", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Card 2: Creator (Disabled / Gray / Coming Soon)
                        UserCatalogModuleCard(
                            title = "Creator",
                            subtitle = "Karya & Aransemen",
                            countText = "",
                            headerIcon = Icons.Default.Create,
                            headerIconColor = MutedText,
                            isComingSoon = true,
                            modifier = Modifier.weight(1f)
                        )
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
                                    .clickable {
                                        if (isKeyRevealed) {
                                            isKeyRevealed = false
                                        } else {
                                            if (isSecurityPinEnabled) {
                                                verifyPinInput = ""
                                                verifyPinError = null
                                                pendingSecurityPinAction = SecurityPinAction.REVEAL_LICENSE
                                            } else {
                                                isKeyRevealed = true
                                            }
                                        }
                                    }
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

                    // Baris PIN Keamanan Lisensi (Melindungi intip kode lisensi & transfer lisensi)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.licensePinSecurityTitle,
                                color = PrimaryText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isSecurityPinEnabled) strings.licensePinActiveStatus else strings.licensePinInactiveStatus,
                                color = if (isSecurityPinEnabled) Color(0xFF22C55E) else MutedText,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSecurityPinEnabled) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RaisedSurface)
                                    .border(1.dp, if (!hasSecurityPin) AccentGold else Border, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!hasSecurityPin) {
                                            setupSecurityPinInput = ""
                                            showSetupSecurityPinDialog = true
                                        } else {
                                            changeOldPinInput = ""
                                            changeNewPinInput = ""
                                            changePinError = null
                                            showChangeSecurityPinDialog = true
                                        }
                                    }
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (!hasSecurityPin) strings.setPinAction else strings.changePinAction,
                                    color = if (!hasSecurityPin) AccentGold else PrimaryText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Switch(
                                checked = isSecurityPinEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (hasSecurityPin) {
                                            LicenseRepository.setSecurityPinEnabled(context, true)
                                            isSecurityPinEnabled = true
                                            Toast.makeText(context, strings.pinSetupSuccessToast, Toast.LENGTH_SHORT).show()
                                        } else {
                                            setupSecurityPinInput = ""
                                            showSetupSecurityPinDialog = true
                                        }
                                    } else {
                                        // Matikan PIN harus diverifikasi dengan PIN saat ini agar peminjam HP tidak bisa sembarangan switch off
                                        verifyPinInput = ""
                                        verifyPinError = null
                                        pendingSecurityPinAction = SecurityPinAction.DISABLE_PIN
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentGold,
                                    checkedTrackColor = AccentGold.copy(alpha = 0.4f)
                                )
                            )
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
                                .clickable {
                                    if (isSecurityPinEnabled) {
                                        verifyPinInput = ""
                                        verifyPinError = null
                                        pendingSecurityPinAction = SecurityPinAction.TRANSFER_LICENSE
                                    } else {
                                        showTransferDialog = true
                                    }
                                }
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
                            onValueChange = { if (it.length <= 8) bindOtpInput = it.filter { c -> c.isDigit() }; bindError = null },
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

    // Dialog Konfirmasi Hapus Katalog Scraped
    if (showClearScrapedDialog) {
        AlertDialog(
            onDismissRequest = { showClearScrapedDialog = false },
            title = {
                Text(
                    text = "Hapus Semua Lagu Scraped?",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Seluruh lagu hasil comotan web ($scrapedCount lagu) akan dihapus dari penyimpanan perangkat. Database resmi dan data lainnya tetap aman.",
                    color = MutedText,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllScraped()
                        showClearScrapedDialog = false
                        Toast.makeText(context, "Katalog Scraped berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(strings.deleteAction, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearScrapedDialog = false }) {
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

    // Dialog Verifikasi PIN Keamanan Lisensi
    if (pendingSecurityPinAction != null) {
        val action = pendingSecurityPinAction!!
        val titleText = when (action) {
            SecurityPinAction.REVEAL_LICENSE -> strings.enterPinToRevealTitle
            SecurityPinAction.TRANSFER_LICENSE -> strings.enterPinToTransferTitle
            SecurityPinAction.DISABLE_PIN -> strings.enterPinToDisableTitle
        }

        AlertDialog(
            onDismissRequest = {
                pendingSecurityPinAction = null
                verifyPinError = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                    Text(
                        text = titleText,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.enterCurrentPinPrompt,
                        color = MutedText,
                        fontSize = 12.5.sp
                    )

                    OutlinedTextField(
                        value = verifyPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                verifyPinInput = it.filter { c -> c.isDigit() }
                                verifyPinError = null
                            }
                        },
                        placeholder = { Text("••••", color = MutedText.copy(alpha = 0.5f), letterSpacing = 4.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = verifyPinError != null,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            cursorColor = AccentGold
                        )
                    )

                    if (verifyPinError != null) {
                        Text(
                            text = verifyPinError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (LicenseRepository.verifySecurityPin(context, verifyPinInput)) {
                            val act = action
                            pendingSecurityPinAction = null
                            verifyPinError = null
                            when (act) {
                                SecurityPinAction.REVEAL_LICENSE -> {
                                    isKeyRevealed = true
                                }
                                SecurityPinAction.TRANSFER_LICENSE -> {
                                    showTransferDialog = true
                                }
                                SecurityPinAction.DISABLE_PIN -> {
                                    LicenseRepository.setSecurityPinEnabled(context, false)
                                    isSecurityPinEnabled = false
                                    Toast.makeText(context, strings.pinDisabledSuccessToast, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            verifyPinError = strings.wrongPinError
                        }
                    },
                    enabled = verifyPinInput.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingSecurityPinAction = null
                        verifyPinError = null
                    }
                ) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog Setup PIN Baru untuk Keamanan Lisensi
    if (showSetupSecurityPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetupSecurityPinDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                    Text(
                        text = strings.licensePinSecurityTitle,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.licensePinSecurityDesc,
                        color = MutedText,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = setupSecurityPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                setupSecurityPinInput = it.filter { c -> c.isDigit() }
                            }
                        },
                        placeholder = { Text("4 Digit Angka", color = MutedText.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            cursorColor = AccentGold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (setupSecurityPinInput.length == 4) {
                            LicenseRepository.setSecurityPin(context, setupSecurityPinInput)
                            hasSecurityPin = true
                            isSecurityPinEnabled = true
                            showSetupSecurityPinDialog = false
                            Toast.makeText(context, strings.pinSetupSuccessToast, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = setupSecurityPinInput.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.saveAndLockLicenseBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetupSecurityPinDialog = false }) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // Dialog Ganti PIN Keamanan Lisensi
    if (showChangeSecurityPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangeSecurityPinDialog = false
                changePinError = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                    Text(
                        text = strings.changePinAction,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.enterCurrentPinPrompt,
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = changeOldPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                changeOldPinInput = it.filter { c -> c.isDigit() }
                                changePinError = null
                            }
                        },
                        placeholder = { Text("PIN Lama (4 Digit)", color = MutedText.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            cursorColor = AccentGold
                        )
                    )

                    Text(
                        text = strings.enterNewPinPrompt,
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = changeNewPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                changeNewPinInput = it.filter { c -> c.isDigit() }
                                changePinError = null
                            }
                        },
                        placeholder = { Text("PIN Baru (4 Digit)", color = MutedText.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            cursorColor = AccentGold
                        )
                    )

                    if (changePinError != null) {
                        Text(
                            text = changePinError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (LicenseRepository.verifySecurityPin(context, changeOldPinInput)) {
                            LicenseRepository.setSecurityPin(context, changeNewPinInput)
                            hasSecurityPin = true
                            isSecurityPinEnabled = true
                            showChangeSecurityPinDialog = false
                            changePinError = null
                            Toast.makeText(context, strings.pinSetupSuccessToast, Toast.LENGTH_SHORT).show()
                        } else {
                            changePinError = strings.wrongPinError
                        }
                    },
                    enabled = changeOldPinInput.length == 4 && changeNewPinInput.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.save, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangeSecurityPinDialog = false
                        changePinError = null
                    }
                ) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    if (showRenameDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDeviceDialog = false },
            title = {
                Text(
                    text = "Rename Device",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Masukkan nama perangkat untuk Server Mode:",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = renameDeviceInput,
                        onValueChange = { renameDeviceInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            cursorColor = AccentGold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameDeviceInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.setDeviceName(trimmed)
                        }
                        showRenameDeviceDialog = false
                    },
                    enabled = renameDeviceInput.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDeviceDialog = false }) {
                    Text(text = "Cancel", color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    if (showServerPinDialog) {
        AlertDialog(
            onDismissRequest = { showServerPinDialog = false },
            title = {
                Text(
                    text = "PIN Protection",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Masukkan 4 karakter PIN untuk mengamankan koneksi band:",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = serverPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                serverPinInput = it
                            }
                        },
                        placeholder = { Text("4 Karakter (misal: 1234)", color = MutedText.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            cursorColor = AccentGold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setServerPin(serverPinInput)
                        viewModel.setPinEnabled(true)
                        showServerPinDialog = false
                    },
                    enabled = serverPinInput.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerPinDialog = false }) {
                    Text(text = "Cancel", color = MutedText)
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
            .padding(vertical = 9.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title,
                color = if (isInstalled || isThisTierDownloading) AccentGold else PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 15.sp
            )
            Text(
                text = count,
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
            Text(
                text = size,
                color = MutedText,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        }

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

private data class UserCatalogActionItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
private fun UserCatalogModuleCard(
    title: String,
    subtitle: String,
    countText: String,
    headerIcon: androidx.compose.ui.graphics.vector.ImageVector,
    headerIconColor: Color,
    actions: List<UserCatalogActionItem> = emptyList(),
    isComingSoon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isComingSoon) RaisedSurface.copy(alpha = 0.5f) else RaisedSurface)
            .border(
                1.dp,
                if (isComingSoon) Border.copy(alpha = 0.4f) else Border,
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Baris Header: Icon + Title + Count Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isComingSoon) Border.copy(alpha = 0.2f) else headerIconColor.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isComingSoon) Border.copy(alpha = 0.4f) else headerIconColor.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = if (isComingSoon) MutedText else headerIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        color = if (isComingSoon) MutedText else PrimaryText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = MutedText.copy(alpha = 0.6f),
                        fontSize = 10.5.sp
                    )
                }
            }

            if (!isComingSoon && countText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Border.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = countText,
                        color = PrimaryText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        HorizontalDivider(
            color = if (isComingSoon) Border.copy(alpha = 0.3f) else Border.copy(alpha = 0.5f),
            thickness = 0.8.dp
        )

        if (isComingSoon) {
            // Pengganti logo/toolbar: Tulisan (Coming Soon)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(0.8.dp, Border.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "(Coming Soon)",
                    color = MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
            }
        } else {
            // Baris Toolbar Aksi: 4 Icon dalam 1 baris seimbang
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(0.8.dp, Border.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                            .clickable { action.onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = action.tint,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class ThemeSliderCard(
    val mode: AppThemeMode,
    val name: String,
    val themeType: String,
    val drawableRes: Int?,
    val fallbackBg: Color,
    val fallbackBorder: Color,
    val textColor: Color
)

@Composable
private fun DockedThemeCard(
    card: ThemeSliderCard,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(card.fallbackBg)
            .then(
                if (isActive) Modifier.border(1.5.dp, AccentGold) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Authentic Texture Image Background if available
        if (card.drawableRes != null) {
            Image(
                painter = painterResource(id = card.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark Scrim: active is clear/subtle, inactive is heavily dimmed
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isActive)
                        Color.Black.copy(alpha = 0.22f)
                    else
                        Color.Black.copy(alpha = 0.65f)
                )
        )

        // Centered Theme Name ONLY
        Text(
            text = card.name.uppercase(),
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = if (isActive) 12.5.sp else 11.sp,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.50f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun InfiniteThemeSlider(
    currentTheme: AppThemeMode,
    onThemeSelect: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeCards = remember {
        listOf(
            ThemeSliderCard(
                mode = AppThemeMode.GREENBOARD,
                name = "Green Board",
                themeType = "Papan Tulis Hijau",
                drawableRes = com.chordapp.R.drawable.texture_greenboard,
                fallbackBg = Color(0xFF121C16),
                fallbackBorder = Color(0xFF23362B),
                textColor = Color(0xFFEAE6DF)
            ),
            ThemeSliderCard(
                mode = AppThemeMode.BLACKBOARD,
                name = "Blackboard",
                themeType = "Papan Tulis Hitam",
                drawableRes = com.chordapp.R.drawable.texture_blackboard,
                fallbackBg = Color(0xFF111417),
                fallbackBorder = Color(0xFF2C353F),
                textColor = Color(0xFFF1F5F9)
            ),
            ThemeSliderCard(
                mode = AppThemeMode.LEATHER,
                name = "Leather",
                themeType = "Vintage Leather",
                drawableRes = com.chordapp.R.drawable.texture_leather,
                fallbackBg = Color(0xFF1B130E),
                fallbackBorder = Color(0xFF4A3528),
                textColor = Color(0xFFFDF8F2)
            ),
            ThemeSliderCard(
                mode = AppThemeMode.PAPER,
                name = "Paper",
                themeType = "Vintage Paper",
                drawableRes = com.chordapp.R.drawable.texture_paper,
                fallbackBg = Color(0xFFF2EBD9),
                fallbackBorder = Color(0xFFD6C8AE),
                textColor = Color(0xFF221C14)
            ),
            ThemeSliderCard(
                mode = AppThemeMode.CRIMSON_SHADOW,
                name = "Crimson Shadow",
                themeType = "Blood Moon",
                drawableRes = com.chordapp.R.drawable.texture_crimson_shadow,
                fallbackBg = Color(0xFF120D12),
                fallbackBorder = Color(0xFF4A1E27),
                textColor = Color(0xFFF4E9DC)
            ),
            ThemeSliderCard(
                mode = AppThemeMode.DARK,
                name = "Dark",
                themeType = "Obsidian Dark",
                drawableRes = null,
                fallbackBg = Color(0xFF0E1117),
                fallbackBorder = Color(0xFF242C3D),
                textColor = Color(0xFFF5F8FF)
            )
        )
    }

    val currentIndex = remember(currentTheme) {
        val idx = themeCards.indexOfFirst { it.mode == currentTheme }
        if (idx >= 0) idx else 0
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 28f

    val leftItem = themeCards[Math.floorMod(currentIndex - 1, themeCards.size)]
    val centerItem = themeCards[currentIndex]
    val rightItem = themeCards[Math.floorMod(currentIndex + 1, themeCards.size)]

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .pointerInput(currentTheme) {
                    detectHorizontalDragGestures(
                        onDragEnd = { dragAccumulator = 0f },
                        onDragCancel = { dragAccumulator = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount
                            if (dragAccumulator > dragThreshold) {
                                // Swiped Right -> previous
                                val prevIdx = Math.floorMod(currentIndex - 1, themeCards.size)
                                onThemeSelect(themeCards[prevIdx].mode)
                                dragAccumulator = 0f
                            } else if (dragAccumulator < -dragThreshold) {
                                // Swiped Left -> next
                                val nextIdx = Math.floorMod(currentIndex + 1, themeCards.size)
                                onThemeSelect(themeCards[nextIdx].mode)
                                dragAccumulator = 0f
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Card: offset -1 (dimmed)
            DockedThemeCard(
                card = leftItem,
                isActive = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onThemeSelect(leftItem.mode) }
            )

            // Divider
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Border))

            // Center Card: active (<terang>)
            DockedThemeCard(
                card = centerItem,
                isActive = true,
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
            )

            // Divider
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Border))

            // Right Card: offset +1 (dimmed)
            DockedThemeCard(
                card = rightItem,
                isActive = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onThemeSelect(rightItem.mode) }
            )
        }

        // OVERLAY tulisan SKIN berada di kiri atas pilihan temanya
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AccentGold.copy(alpha = 0.28f))
                .border(0.8.dp, AccentGold, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = "SKIN",
                color = AccentGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.5.sp,
                letterSpacing = 1.2.sp,
                maxLines = 1
            )
        }
    }
}

