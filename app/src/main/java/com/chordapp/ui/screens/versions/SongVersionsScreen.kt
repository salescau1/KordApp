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
import androidx.compose.ui.text.font.FontWeight
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
    val strings = com.chordapp.localization.LocalAppStrings.current
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
            Text(strings.noVersionsFound, color = PrimaryText)
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
                Text(text = strings.versionsCount(versions.size), color = AccentGold, style = MaterialTheme.typography.bodySmall)
                firstVersion.keyBase?.let {
                    Text(text = strings.keyLabel(it), color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text(text = strings.chordsCount(firstVersion.chordCount), color = MutedText, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Filters
        val filterOptions = listOf(
            "All Sources" to strings.allSources,
            "UG" to "UG",
            "GT" to "GT",
            "CT" to "CT",
            "CI" to "CI"
        )
        val counts = filterOptions.associate { (id, _) ->
            val count = if (id == "All Sources") versions.size
            else versions.count { sourceLabel(it.source) == id }
            id to count
        }

        ScrollableTabRow(
            selectedTabIndex = filterOptions.indexOfFirst { it.first == sourceFilter }.coerceAtLeast(0),
            containerColor = CardSurface,
            contentColor = PrimaryText,
            edgePadding = 16.dp,
            indicator = { /* No default indicator */ },
            divider = { }
        ) {
            filterOptions.forEach { (id, label) ->
                val count = counts[id] ?: 0
                val isSelected = sourceFilter == id

                val bg = if (isSelected) AccentGold else Color.Transparent
                val tc = if (isSelected) Background else (if (count > 0) PrimaryText else MutedText)

                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setSourceFilter(id) },
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(16.dp))
                            .border(1.dp, if (isSelected) AccentGold else Border, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = "$label ($count)", color = tc, style = MaterialTheme.typography.labelMedium)
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
    val strings = com.chordapp.localization.LocalAppStrings.current
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
                    val normSource = sourceLabel(version.source)
                    val sourceBg = when(normSource) {
                        "UG" -> UgBg
                        "GT" -> GtBg
                        "CT" -> CtBg
                        "CI" -> CiBg
                        else -> CardSurface
                    }
                    val sourceTc = when(normSource) {
                        "UG" -> UgText
                        "GT" -> GtText
                        "CT" -> CtText
                        "CI" -> CiText
                        else -> PrimaryText
                    }
                    Box(
                        modifier = Modifier
                            .background(sourceBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(normSource, color = sourceTc, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    val titleText = version.versionName?.takeIf { it.isNotBlank() } ?: "Ver. $rankInSong"
                    Text(
                        text = titleText,
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (version.votes != null && version.votes > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "★ ${version.rating ?: 0f}", color = AccentGold, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "(${version.votes})", color = MutedText, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row
            val meta = mutableListOf<String>()
            version.difficulty?.let { meta.add(it) }
            version.keyBase?.let { meta.add(strings.keyLabel(it)) }
            meta.add(strings.chordsCount(version.chordCount))
            meta.add(strings.linesCount(version.lineCount))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                meta.forEach { text ->
                    Text(text = text, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
