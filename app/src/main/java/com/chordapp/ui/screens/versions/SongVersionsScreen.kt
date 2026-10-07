package com.chordapp.ui.screens.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.ui.components.AppBackButton
import com.chordapp.ui.components.sourceLabel
import androidx.compose.ui.text.style.TextOverflow
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.SongVersionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongVersionsScreen(
    artistSlug: String,
    titleSlug: String,
    viewModel: SongVersionsViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val versions by viewModel.versions.collectAsState()
    val filteredVersions by viewModel.filteredVersions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sourceFilter by viewModel.sourceFilter.collectAsState()
    val navigateToPlayer by viewModel.navigateToPlayer.collectAsState()

    LaunchedEffect(artistSlug, titleSlug) {
        viewModel.loadVersions(artistSlug, titleSlug)
    }

    LaunchedEffect(navigateToPlayer) {
        navigateToPlayer?.let { sourceUrl ->
            viewModel.clearNavigation()
            // Hash it to safely pass in route argument if needed, though route argument handles hashing locally in graph
            onNavigateToPlayer(sourceUrl)
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentGold)
        }
        return
    }

    // If versions is empty, might be an error. If 1, it navigated away.
    if (versions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            Text("No versions found", color = PrimaryText)
        }
        return
    }

    val firstVersion = versions.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        TopAppBar(
            title = { },
            navigationIcon = { AppBackButton(onBackClick) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        // Header
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = firstVersion.title, color = PrimaryText, style = MaterialTheme.typography.headlineMedium)
            Text(text = firstVersion.artist, color = MutedText, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Top level stats based on first/best version
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "${versions.size} Versions", color = AccentGold, style = MaterialTheme.typography.bodySmall)
                firstVersion.keyBase?.let {
                    Text(text = "Key: $it", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text(text = "${firstVersion.chordCount} Chords", color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Filters
        val filterOptions = listOf("All Sources", "Ultimate Guitar", "GuitarTuna", "ChordTela")
        val counts = filterOptions.associateWith { name ->
            if (name == "All Sources") versions.size
            else versions.count { it.source == (if (name == "Ultimate Guitar") "UG" else if (name == "GuitarTuna") "GT" else "ChordTela") }
        }

        ScrollableTabRow(
            selectedTabIndex = filterOptions.indexOf(sourceFilter).coerceAtLeast(0),
            containerColor = CardSurface,
            contentColor = PrimaryText,
            edgePadding = 16.dp,
            indicator = { /* No default indicator */ },
            divider = { }
        ) {
            filterOptions.forEach { option ->
                val count = counts[option] ?: 0
                val isSelected = sourceFilter == option

                val bg = if (isSelected) AccentGold else Color.Transparent
                val tc = if (isSelected) Background else (if (count > 0) PrimaryText else MutedText)

                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setSourceFilter(option) },
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(16.dp))
                            .border(1.dp, if (isSelected) AccentGold else Border, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = "$option ($count)", color = tc, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // List of versions
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(filteredVersions) { index, version ->
                VersionCard(
                    version = version,
                    rankInSong = index + 1,
                    onClick = { onNavigateToPlayer(version.sourceUrl) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun VersionCard(
    version: CatalogSongEntity,
    rankInSong: Int,
    onClick: () -> Unit
) {
    val bg = CardSurface
    val borderColor = Border

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rankInSong == 1) {
                        Box(
                            modifier = Modifier
                                .background(AccentGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("BEST", color = Background, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val sourceBg = when(version.source) {
                        "UG" -> UgBg
                        "GT" -> GtBg
                        "ChordTela" -> CtBg
                        else -> CardSurface
                    }
                    val sourceTc = when(version.source) {
                        "UG" -> UgText
                        "GT" -> GtText
                        "ChordTela" -> CtText
                        else -> PrimaryText
                    }
                    Box(
                        modifier = Modifier
                            .background(sourceBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(sourceLabel(version.source), color = sourceTc, style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    // version_name di katalog sebenarnya teks "about" dari situs
                    // (mis. UG) — bukan nama versi. Jadi judul pakai nomor urut yang
                    // rapi, dan teks about ditaruh sbagai catatan kecil di bawah.
                    Text(text = "Ver. $rankInSong", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
                }

                if (version.votes != null && version.votes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "★ ${version.rating ?: 0f}", color = AccentGold, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "(${version.votes})", color = MutedText, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Catatan "about" dari situs sumber (disimpan di version_name). Tampilkan
            // kecil & terpotong, bukan sebagai judul versi.
            version.versionName?.takeIf { it.isNotBlank() }?.let { about ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = about,
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row
            val meta = mutableListOf<String>()
            version.difficulty?.let { meta.add(it) }
            version.keyBase?.let { meta.add("Key: $it") }
            meta.add("${version.chordCount} Chords")
            meta.add("${version.lineCount} Lines")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                meta.forEach { text ->
                    Text(text = text, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
