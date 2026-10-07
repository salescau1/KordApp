package com.chordapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.chordapp.localization.LocalAppStrings
import com.chordapp.ui.theme.PrimaryText

@Composable
fun AppBackButton(onClick: () -> Unit) {
    val strings = LocalAppStrings.current
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = strings.back,
            tint = PrimaryText
        )
    }
}
