package com.chordapp.ui.screens.artist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.ui.components.AppBackButton
import com.chordapp.ui.components.SourceBadge
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.ArtistDirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDirectoryScreen(
    artistSlug: String,
    viewModel: ArtistDirectoryViewModel,
    onBackClick: () -> Unit,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit
) {
    val artistName by viewModel.artistName.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(artistSlug) {
        viewModel.loadArtist(artistSlug)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        TopAppBar(
            title = { Text(artistName, color = PrimaryText) },
            navigationIcon = { AppBackButton(onBackClick) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else {
            // A-Z is the only sort mode. The catalog has no play-count/popularity data to
            // power a "Popular" order, so we never offer a fake one.
            var filter by rememberSaveable { mutableStateOf("") }
            val sortedSongs = remember(songs) {
                songs.entries.sortedBy { it.key.title.lowercase() }
            }
            val shownSongs = remember(sortedSongs, filter) {
                val query = filter.trim()
                if (query.isEmpty()) {
                    sortedSongs
                } else {
                    sortedSongs.filter { it.key.title.contains(query, ignoreCase = true) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${shownSongs.size} songs", color = MutedText)
                Text(
                    text = "A-Z",
                    color = MutedText,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Local "Filter judul": narrows the already-loaded artist songs by title.
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                placeholder = { Text(text = "Filter songs", color = MutedText) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = Border,
                    unfocusedBorderColor = Border,
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    cursorColor = AccentGold
                ),
                trailingIcon = if (filter.isNotEmpty()) {
                    {
                        Text(
                            text = "✕",
                            color = PrimaryText,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { filter = "" }
                        )
                    }
                } else null
            )

            if (shownSongs.isEmpty()) {
                Text(
                    text = if (filter.isBlank()) {
                        "No songs for this artist."
                    } else {
                        "No songs match \"${filter.trim()}\"."
                    },
                    color = MutedText,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = shownSongs,
                        key = { (group, _) -> group.titleSlug }
                    ) { (group, versions) ->
                        // The whole song card is one tap target: it opens the version
                        // picker for this song (source labels below are informational).
                        SongRow(
                            group = group,
                            versions = versions,
                            onSongClick = { onSongClick(group.artistSlug, group.titleSlug) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single song entry in the artist directory.
 *
 * The entire card is one tap target: tapping anywhere on it (title or source labels)
 * opens [SongVersions] via [onSongClick] so the user can pick which version to play.
 * The source labels are informational only — deduplicated per source (UG / GT /
 * ChordTela), with no version-specific label. Version choice happens on SongVersions.
 */
@Composable
private fun SongRow(
    group: CatalogSongGroup,
    versions: List<CatalogSongEntity>,
    onSongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = group.title,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge
            )
            if (versions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    versions.map { it.source }.distinct().forEach { source ->
                        SourceBadge(source = source)
                    }
                }
            }
        }
    }
}
