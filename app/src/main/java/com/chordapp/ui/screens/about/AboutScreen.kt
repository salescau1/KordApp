package com.chordapp.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chordapp.ui.components.AppBackButton
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.viewmodels.AboutViewModel

@Composable
fun AboutScreen(
    viewModel: AboutViewModel,
    onBackClick: () -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    val content by viewModel.content.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppBackButton(onBackClick)
            Text(
                strings.aboutApp.uppercase(),
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 4.dp),
                    color = AccentGold,
                    strokeWidth = 2.dp
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(viewModel.metadata.name, color = PrimaryText, style = MaterialTheme.typography.headlineSmall)
            Text(
                strings.versionWithCode(viewModel.metadata.versionName, viewModel.metadata.versionCode),
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        HorizontalDivider(color = Border)
        Text(
            text = content,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
