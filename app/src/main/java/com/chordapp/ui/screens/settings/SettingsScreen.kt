package com.chordapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.localization.AppLanguage
import com.chordapp.localization.LocalAppStrings
import com.chordapp.repositories.LicenseRepository
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAboutClick: () -> Unit,
    onLicenseTransferred: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val stats by viewModel.catalogStats.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var isTransferring by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = strings.settingsTitle,
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )

        // Language Section
        SettingsSection(strings.languageSection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.appLanguageLabel, color = PrimaryText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentLanguage.displayName, color = AccentGold, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text("›", color = AccentGold)
                }
            }
        }

        // Offline Catalog Section
        SettingsSection(strings.offlineCatalogSection) {
            InfoRow(strings.artists, stats.artists.toString())
            HorizontalDivider(color = Border)
            InfoRow(strings.songs, stats.songs.toString())
            HorizontalDivider(color = Border)
            InfoRow(strings.catalogSize, formatBytes(stats.catalogBytes, strings.calculating))
        }

        val isLicensed = remember { LicenseRepository.isLicensed(context) }
        val savedKey = remember { LicenseRepository.getSavedLicenseKey(context) }
        val deviceId = remember { LicenseRepository.getDeviceId(context) }

        // License & Device Section
        SettingsSection(strings.licenseAndDeviceSection) {
            InfoRow(strings.licenseStatus, if (isLicensed) strings.licenseActivePro else strings.licenseInactive)
            if (isLicensed && savedKey != null) {
                HorizontalDivider(color = Border)
                InfoRow(strings.licenseKey, savedKey)
                HorizontalDivider(color = Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTransferDialog = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.transferLicenseToOtherPhone, color = AccentGold, fontWeight = FontWeight.Medium)
                    Text("›", color = AccentGold)
                }
            }
            HorizontalDivider(color = Border)
            InfoRow(strings.deviceId, deviceId)
        }

        if (showTransferDialog && savedKey != null) {
            AlertDialog(
                onDismissRequest = { if (!isTransferring) showTransferDialog = false },
                title = { Text(strings.transferLicenseTitle, color = PrimaryText, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = strings.transferLicenseMessage(savedKey),
                        color = MutedText,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isTransferring = true
                            scope.launch {
                                val (ok, msg) = LicenseRepository.transferLicenseSelfService(context)
                                isTransferring = false
                                showTransferDialog = false
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                if (ok) {
                                    onLicenseTransferred()
                                }
                            }
                        },
                        enabled = !isTransferring,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Text(
                            text = if (isTransferring) strings.releasingLicense else strings.releaseLicenseAction,
                            color = Color.Black,
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

        // Language Picker Dialog
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(strings.selectLanguage, color = PrimaryText, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = currentLanguage == lang
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setLanguage(lang)
                                        showLanguageDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = lang.displayName,
                                    color = if (isSelected) AccentGold else PrimaryText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (isSelected) {
                                    Text("✓", color = AccentGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(strings.cancel, color = MutedText)
                    }
                },
                containerColor = CardSurface
            )
        }

        // Application Section
        SettingsSection(strings.applicationSection) {
            InfoRow(strings.version, viewModel.versionName)
            HorizontalDivider(color = Border)
            InfoRow(strings.apkSize, formatBytes(stats.appBytes, strings.calculating))
            HorizontalDivider(color = Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAboutClick)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.aboutApp, color = PrimaryText)
                Text("›", color = AccentGold)
            }
        }

        Text(
            text = strings.offlineDisclaimer,
            color = MutedText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = AccentGold, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium)
        Column(
            modifier = Modifier.fillMaxWidth().background(CardSurface, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp)
        ) { content() }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PrimaryText)
        Text(value, color = MutedText)
    }
}

private fun formatBytes(bytes: Long, calculatingText: String): String = when {
    bytes <= 0L -> calculatingText
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
