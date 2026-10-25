package com.chordapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chordapp.localization.LocalAppStrings
import com.chordapp.repositories.AppUpdateInfo
import com.chordapp.repositories.UpdateRepository
import com.chordapp.settings.PlayerPreferences
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText

@Composable
fun UpdateNotificationDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val prefs = remember { PlayerPreferences(context) }
    var dontShowAgain by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate) {
                if (dontShowAgain) {
                    prefs.dontShowUpdateAgain = true
                    prefs.lastDismissedUpdateVersion = updateInfo.latestVersionName
                }
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate,
            dismissOnClickOutside = !updateInfo.isForceUpdate
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardSurface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header with update icon & version info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.updateAvailableTitle,
                            color = PrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.newVersionAvailable(updateInfo.latestVersionName),
                            color = AccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Background.copy(alpha = 0.6f))
                        .border(1.dp, Border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = updateInfo.changelog.ifBlank { "Peningkatan performa, kestabilan tuner, dan perbaikan antarmuka." },
                        color = MutedText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Checkbox: [ ] Jangan tampilkan lagi / Don't show this again
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentGold,
                            checkmarkColor = Color.Black,
                            uncheckedColor = MutedText
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.dontShowAgain,
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons: Update Now / Later
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!updateInfo.isForceUpdate) {
                        OutlinedButton(
                            onClick = {
                                if (dontShowAgain) {
                                    prefs.dontShowUpdateAgain = true
                                    prefs.lastDismissedUpdateVersion = updateInfo.latestVersionName
                                }
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MutedText
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Text(
                                text = strings.laterAction,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (dontShowAgain) {
                                prefs.dontShowUpdateAgain = true
                                prefs.lastDismissedUpdateVersion = updateInfo.latestVersionName
                            }
                            UpdateRepository.startDownloadAndInstall(
                                context,
                                updateInfo.downloadUrl,
                                "KordApp-v${updateInfo.latestVersionName}.apk"
                            )
                            Toast.makeText(context, strings.downloadStartedToast, Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color(0xFF0C0E12)
                        )
                    ) {
                        Text(
                            text = strings.updateAction,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
