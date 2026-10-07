package com.chordapp.ui.screens.activation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.repositories.LicenseRepository
import com.chordapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(
    onActivationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var licenseInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val deviceId = remember { LicenseRepository.getDeviceId(context) }

    LaunchedEffect(Unit) {
        // Catat device di background saat layar dibuka
        LicenseRepository.recordDevice(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon & Title
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = AccentGold,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Kord",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryText
                )
                Text(
                    text = "App",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentGold
                )
            }

            Text(
                text = "Aktivasi Lisensi Pengguna",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Card Input Form
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Masukkan Kode Lisensi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Masukkan kode yang Anda dapatkan setelah melakukan pembelian untuk membuka 69.000+ chord offline.",
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = licenseInput,
                        onValueChange = { licenseInput = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: KORD-TEST-1234", color = MutedText.copy(alpha = 0.5f), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AccentGold)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            cursorColor = AccentGold
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Device ID Info chip
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                                Toast.makeText(context, "Device ID disalin!", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = RaisedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Device ID: $deviceId",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Pesan Feedback
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = ActiveHeart,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    successMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = GtText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Tombol Aktivasi
                    Button(
                        onClick = {
                            if (licenseInput.isBlank()) {
                                errorMessage = "Silakan masukkan kode lisensi."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            successMessage = null

                            scope.launch {
                                val (ok, msg) = LicenseRepository.activateLicense(context, licenseInput)
                                isLoading = false
                                if (ok) {
                                    successMessage = msg
                                    kotlinx.coroutines.delay(700)
                                    onActivationSuccess()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Aktivasi Sekarang", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Section Bantuan / Beli
            Text(
                text = "Belum memiliki kode lisensi?",
                color = MutedText,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    runCatching {
                        val message = "Halo Admin KordApp, saya ingin beli lisensi KordApp. Device ID saya: $deviceId"
                        val url = "https://wa.me/?text=${Uri.encode(message)}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }.onFailure {
                        Toast.makeText(context, "Tidak dapat membuka browser/WhatsApp", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold)
            ) {
                Text("Beli Lisensi / Hubungi Admin", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
