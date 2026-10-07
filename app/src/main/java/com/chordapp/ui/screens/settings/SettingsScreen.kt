package com.chordapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.viewmodels.SettingsViewModel
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAboutClick: () -> Unit,
    onLicenseTransferred: () -> Unit = {}
) {
    val stats by viewModel.catalogStats.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var showTransferDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var isTransferring by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SETTINGS", color = PrimaryText, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall)

        SettingsSection("KATALOG OFFLINE") {
            InfoRow("Artis", stats.artists.toString())
            HorizontalDivider(color = Border)
            InfoRow("Lagu", stats.songs.toString())
            HorizontalDivider(color = Border)
            InfoRow("Ukuran katalog", formatBytes(stats.catalogBytes))
        }

        val isLicensed = androidx.compose.runtime.remember { com.chordapp.repositories.LicenseRepository.isLicensed(context) }
        val savedKey = androidx.compose.runtime.remember { com.chordapp.repositories.LicenseRepository.getSavedLicenseKey(context) }
        val deviceId = androidx.compose.runtime.remember { com.chordapp.repositories.LicenseRepository.getDeviceId(context) }

        SettingsSection("LISENSI & PERANGKAT") {
            InfoRow("Status Lisensi", if (isLicensed) "Aktif (PRO)" else "Belum Aktif")
            if (isLicensed && savedKey != null) {
                HorizontalDivider(color = Border)
                InfoRow("Kode Lisensi", savedKey)
                HorizontalDivider(color = Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTransferDialog = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Pindahkan Lisensi ke HP Lain", color = AccentGold, fontWeight = FontWeight.Medium)
                    Text("›", color = AccentGold)
                }
            }
            HorizontalDivider(color = Border)
            InfoRow("Device ID", deviceId)
        }

        if (showTransferDialog && savedKey != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { if (!isTransferring) showTransferDialog = false },
                title = { Text("Pindahkan Lisensi?", color = PrimaryText, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Kode lisensi Anda: $savedKey\n\nPastikan Anda sudah menyalin kode di atas. Setelah dilepas, aplikasi di HP ini akan terkunci kembali dan kode bisa langsung digunakan di HP baru Anda.",
                        color = MutedText,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            isTransferring = true
                            scope.launch {
                                val (ok, msg) = com.chordapp.repositories.LicenseRepository.transferLicenseSelfService(context)
                                isTransferring = false
                                showTransferDialog = false
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                if (ok) {
                                    onLicenseTransferred()
                                }
                            }
                        },
                        enabled = !isTransferring,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Text(if (isTransferring) "Melepas..." else "Ya, Lepas Lisensi", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showTransferDialog = false },
                        enabled = !isTransferring
                    ) {
                        Text("Batal", color = MutedText)
                    }
                },
                containerColor = CardSurface
            )
        }

        SettingsSection("APLIKASI") {
            InfoRow("Versi", viewModel.versionName)
            HorizontalDivider(color = Border)
            InfoRow("Ukuran APK", formatBytes(stats.appBytes))
            HorizontalDivider(color = Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAboutClick)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("About Application", color = PrimaryText)
                Text("›", color = AccentGold)
            }
        }

        Text(
            "Katalog, favorit, setlist, dan pengaturan Player tersedia secara offline.",
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

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0L -> "Menghitung…"
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
