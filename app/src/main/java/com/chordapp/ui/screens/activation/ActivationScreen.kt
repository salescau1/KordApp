package com.chordapp.ui.screens.activation

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.localization.AppLanguage
import com.chordapp.localization.AppLanguageManager
import com.chordapp.localization.LocalAppStrings
import com.chordapp.p2p.BandSessionManager
import com.chordapp.repositories.LicenseRepository
import com.chordapp.ui.components.LicenseKeyVisualTransformation
import com.chordapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(
    onActivationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
    val strings = remember(currentLanguage) { AppLanguageManager.strings(currentLanguage) }

    var licenseInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Dialog & Modal States
    var showQrisDialog by remember { mutableStateOf(false) }
    var showResetModal by remember { mutableStateOf(false) }
    var showBindEducationDialog by remember { mutableStateOf(false) }
    var showPinEducationDialog by remember { mutableStateOf(false) }
    var pinEduInput by remember { mutableStateOf("") }

    fun proceedAfterActivationOrEmail() {
        if (!LicenseRepository.isSecurityPinPromptDismissed(context) && !LicenseRepository.isSecurityPinEnabled(context)) {
            showPinEducationDialog = true
        } else {
            onActivationSuccess()
        }
    }

    // State for Reset Modal
    var resetEmail by remember { mutableStateOf("") }
    var resetOtp by remember { mutableStateOf("") }
    var isResetOtpSent by remember { mutableStateOf(false) }
    var isResetLoading by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var resetSuccess by remember { mutableStateOf<String?>(null) }

    // State for Bind Education Dialog
    var bindEmail by remember { mutableStateOf("") }
    var bindOtp by remember { mutableStateOf("") }
    var isBindOtpSent by remember { mutableStateOf(false) }
    var isBindLoading by remember { mutableStateOf(false) }
    var bindError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Catat device di background saat layar dibuka
        LicenseRepository.recordDevice(context)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
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
                                .clickable { AppLanguageManager.setLanguage(context, lang) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF0C0E12) else MutedText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Center Content Scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
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
                text = strings.activationHeader,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
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
                        text = strings.enterLicenseKey,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.activationDesc,
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(18.dp))

                    val visualTransformation = remember { LicenseKeyVisualTransformation() }

                    OutlinedTextField(
                        value = licenseInput,
                        onValueChange = { input ->
                            licenseInput = LicenseKeyVisualTransformation.sanitize(input, maxLength = 16)
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("KORD-XXXX-XXXX-XXXX", color = MutedText.copy(alpha = 0.5f), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AccentGold)
                        },
                        trailingIcon = {
                            if (licenseInput.isNotEmpty()) {
                                IconButton(onClick = { licenseInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = MutedText, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        visualTransformation = visualTransformation,
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

                    Spacer(Modifier.height(16.dp))

                    // Feedback Message
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = ActiveHeart,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    successMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = GtText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    // 3. Solid Gold Main Button: AKTIVASI SEKARANG
                    Button(
                        onClick = {
                            if (licenseInput.isBlank()) {
                                errorMessage = strings.pleaseEnterLicenseKey
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            successMessage = null

                            scope.launch {
                                val keyToActivate = LicenseKeyVisualTransformation.formatWithDashes(licenseInput)
                                val (ok, msg) = LicenseRepository.activateLicense(context, keyToActivate)
                                isLoading = false
                                if (ok) {
                                    successMessage = msg
                                    delay(600)
                                    if (LicenseRepository.isEmailVerified(context) || LicenseRepository.isBindEmailPromptDismissed(context)) {
                                        proceedAfterActivationOrEmail()
                                    } else {
                                        showBindEducationDialog = true
                                    }
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color(0xFF0C0E12)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF0C0E12),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "🚀  ${strings.activateNow.uppercase()}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 4. Garis Pemisah & Grid 2 Kolom Seimbang di Bagian Bawah
            HorizontalDivider(color = Border.copy(alpha = 0.8f), thickness = 1.dp)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Sisi Kiri: Beli Lisensi (WA / TG / QR)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = strings.buyLicenseLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedText,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // [ WA ] Button
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    runCatching {
                                        val message = strings.waOrderMessage("")
                                        val url = "https://wa.me/?text=${Uri.encode(message)}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }.onFailure {
                                        Toast.makeText(context, strings.unableToOpenBrowserOrWa, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = RaisedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("WA", color = Color(0xFF25D366), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }

                        // [ TG ] Button
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    runCatching {
                                        val url = "https://t.me/kordapp"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }.onFailure {
                                        Toast.makeText(context, strings.unableToOpenBrowserOrWa, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = RaisedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("TG", color = Color(0xFF229ED9), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }

                        // [ QR ] Button (QRIS Modal)
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { showQrisDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = RaisedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("QR", color = AccentGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Sisi Kanan: Lost Access (Reset Key)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = strings.lostAccessLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedText,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                resetEmail = ""
                                resetOtp = ""
                                isResetOtpSent = false
                                resetError = null
                                resetSuccess = null
                                showResetModal = true
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = RaisedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset Key",
                                tint = AccentGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = strings.resetKeyAction,
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 5. MODAL IN-APP RESET KEY (HP LAMA HILANG / GANTI DEVICE)
    // =========================================================================
    if (showResetModal) {
        AlertDialog(
            onDismissRequest = { if (!isResetLoading) showResetModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                    Text(
                        text = strings.resetKeyDialogTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.resetKeyDialogDesc,
                        fontSize = 12.5.sp,
                        color = MutedText,
                        lineHeight = 17.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RaisedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Text(
                            text = strings.resetCooldownNote,
                            color = AccentGold,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    // Step 1: Input Email
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it.trim().lowercase(); resetError = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(strings.recoveryEmailPlaceholder, color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AccentGold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isResetOtpSent && !isResetLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Step 2: Input 6-Digit OTP (muncul jika OTP sudah terkirim)
                    if (isResetOtpSent) {
                        OutlinedTextField(
                            value = resetOtp,
                            onValueChange = { if (it.length <= 6) resetOtp = it.filter { c -> c.isDigit() }; resetError = null },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(strings.enterResetOtpTitle, color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AccentGold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !isResetLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = Border,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    resetError?.let { err ->
                        Text(text = err, color = ActiveHeart, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    resetSuccess?.let { succ ->
                        Text(text = succ, color = GtText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                if (!isResetOtpSent) {
                    Button(
                        onClick = {
                            if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                                resetError = strings.invalidEmailError
                                return@Button
                            }
                            isResetLoading = true
                            resetError = null
                            scope.launch {
                                val otp = (100000..999999).random().toString()
                                val (ok, msg) = LicenseRepository.requestLicenseResetByEmail(resetEmail, resetToken = otp)
                                isResetLoading = false
                                if (ok) {
                                    isResetOtpSent = true
                                    resetSuccess = strings.otpSentToast(resetEmail)
                                } else {
                                    resetError = msg
                                }
                            }
                        },
                        enabled = !isResetLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(strings.sendResetVerificationBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (resetOtp.length < 6) {
                                resetError = strings.otpLengthError
                                return@Button
                            }
                            isResetLoading = true
                            resetError = null
                            scope.launch {
                                val (ok, licenseKey, msg) = LicenseRepository.confirmLicenseResetByEmailOtp(resetEmail, resetOtp)
                                isResetLoading = false
                                if (ok) {
                                    resetSuccess = msg
                                    licenseInput = LicenseKeyVisualTransformation.sanitize(licenseKey)
                                    delay(900)
                                    showResetModal = false
                                    Toast.makeText(context, strings.resetSuccessMsg, Toast.LENGTH_LONG).show()
                                } else {
                                    resetError = msg
                                }
                            }
                        },
                        enabled = !isResetLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(strings.confirmResetBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetModal = false },
                    enabled = !isResetLoading
                ) {
                    Text(strings.cancel, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // =========================================================================
    // 6. POP-UP EDUKASI BIND EMAIL (MUNCUL HANYA 1 KALI SETELAH AKTIVASI SUKSES)
    // =========================================================================
    if (showBindEducationDialog) {
        AlertDialog(
            onDismissRequest = {
                LicenseRepository.setBindEmailPromptDismissed(context, true)
                showBindEducationDialog = false
                proceedAfterActivationOrEmail()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                    Text(
                        text = strings.bindEmailEduTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.bindEmailEduDesc,
                        fontSize = 12.5.sp,
                        color = MutedText,
                        lineHeight = 17.sp
                    )

                    // Input Email
                    OutlinedTextField(
                        value = bindEmail,
                        onValueChange = { bindEmail = it.trim().lowercase(); bindError = null },
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

                    // Input OTP jika sudah request
                    if (isBindOtpSent) {
                        OutlinedTextField(
                            value = bindOtp,
                            onValueChange = { if (it.length <= 6) bindOtp = it.filter { c -> c.isDigit() }; bindError = null },
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
                            if (bindEmail.isBlank() || !bindEmail.contains("@")) {
                                bindError = strings.invalidEmailError
                                return@Button
                            }
                            isBindLoading = true
                            bindError = null
                            scope.launch {
                                val otp = (100000..999999).random().toString()
                                val (ok, msg) = LicenseRepository.requestEmailBindingOtp(context, bindEmail, generatedOtp = otp)
                                isBindLoading = false
                                if (ok) {
                                    isBindOtpSent = true
                                    Toast.makeText(context, strings.otpSentToast(bindEmail), Toast.LENGTH_LONG).show()
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
                            Text(strings.bindEmailNowBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (bindOtp.length < 6) {
                                bindError = strings.otpLengthError
                                return@Button
                            }
                            isBindLoading = true
                            bindError = null
                            scope.launch {
                                val (ok, msg) = LicenseRepository.verifyEmailOtp(context, bindOtp)
                                isBindLoading = false
                                if (ok) {
                                    LicenseRepository.setBindEmailPromptDismissed(context, true)
                                    showBindEducationDialog = false
                                    Toast.makeText(context, strings.bindEmailSuccess, Toast.LENGTH_LONG).show()
                                    proceedAfterActivationOrEmail()
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
                            Text(strings.verifyAndContinueBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        LicenseRepository.setBindEmailPromptDismissed(context, true)
                        showBindEducationDialog = false
                        proceedAfterActivationOrEmail()
                    },
                    enabled = !isBindLoading
                ) {
                    Text(strings.setupLaterBtn, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // =========================================================================
    // 7. POP-UP EDUKASI PIN PANGGUNG (MUNCUL SETELAH AKTIVASI / SETELAH BIND EMAIL)
    // =========================================================================
    if (showPinEducationDialog) {
        AlertDialog(
            onDismissRequest = {
                LicenseRepository.setSecurityPinPromptDismissed(context, true)
                showPinEducationDialog = false
                onActivationSuccess()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                    Text(
                        text = strings.licensePinSecurityTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.licensePinSecurityDesc,
                        fontSize = 12.5.sp,
                        color = MutedText,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = pinEduInput,
                        onValueChange = { input ->
                            if (input.length <= 4) {
                                pinEduInput = input.filter { it.isDigit() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("4 Digit PIN (misal: 1234)", color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AccentGold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinEduInput.length == 4) {
                            LicenseRepository.setSecurityPin(context, pinEduInput)
                            showPinEducationDialog = false
                            Toast.makeText(context, strings.pinSetupSuccessToast, Toast.LENGTH_SHORT).show()
                            onActivationSuccess()
                        }
                    },
                    enabled = pinEduInput.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.saveAndLockLicenseBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        LicenseRepository.setSecurityPinPromptDismissed(context, true)
                        showPinEducationDialog = false
                        onActivationSuccess()
                    }
                ) {
                    Text(strings.skipPinBtn, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }

    // =========================================================================
    // 7. MODAL DIALOG QRIS RESMI KORDAPP
    // =========================================================================
    if (showQrisDialog) {
        AlertDialog(
            onDismissRequest = { showQrisDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                    Text(
                        text = strings.qrisDialogTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // QR Graphic Box
                    Surface(
                        modifier = Modifier
                            .size(210.dp)
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, AccentGold)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "QRIS STANDAR NASIONAL",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = "QRIS Code",
                                tint = Color.Black,
                                modifier = Modifier.size(120.dp)
                            )
                            Text(
                                text = "NMID: ID1020304050607\nKordApp Official Store",
                                color = Color(0xFF4B5563),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = strings.qrisDialogDesc,
                        fontSize = 12.sp,
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.5.sp
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = RaisedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Text(
                            text = "BCA • MANDIRI • BRI • BNI • GOPAY • OVO • DANA • SHOPEEPAY",
                            color = AccentGold,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        runCatching {
                            val url = "https://wa.me/?text=${Uri.encode("Halo Admin KordApp, saya sudah scan QRIS dan ingin konfirmasi pembelian lisensi.")}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.confirmToWhatsApp, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrisDialog = false }) {
                    Text(strings.close, color = MutedText)
                }
            },
            containerColor = CardSurface
        )
    }
}
