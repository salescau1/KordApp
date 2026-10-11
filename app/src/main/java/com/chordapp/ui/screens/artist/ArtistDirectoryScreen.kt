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
import com.chordapp.ui.components.RegionBadge
import com.chordapp.ui.components.SourceBadge
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.ArtistDirectoryViewModel
import com.chordapp.viewmodels.ArtistSongSortOrder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDirectoryScreen(
    artistSlug: String,
    viewModel: ArtistDirectoryViewModel,
    onBackClick: () -> Unit,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    val artistName by viewModel.artistName.collectAsState()
    val region by viewModel.region.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(artistName, color = PrimaryText)
                    region?.let { reg ->
                        Spacer(modifier = Modifier.width(8.dp))
                        RegionBadge(region = reg)
                    }
                }
            },
            navigationIcon = { AppBackButton(onBackClick) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else {
            var filter by rememberSaveable { mutableStateOf("") }
            val shownSongs = remember(songs, filter) {
                val query = filter.trim()
                if (query.isEmpty()) {
                    songs.entries.toList()
                } else {
                    songs.entries.filter { it.key.title.contains(query, ignoreCase = true) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.songsCount(shownSongs.size),
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                ArtistSortSegmentedControl(
                    selectedOrder = sortOrder,
                    onSelectOrder = { viewModel.setSortOrder(it) },
                    strings = strings
                )
            }

            // Local "Filter judul": narrows the already-loaded artist songs by title.
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                placeholder = { Text(text = strings.filterSongs, color = MutedText) },
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
                        strings.noSongsForArtist
                    } else {
                        strings.noSongsMatch(filter.trim())
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        versions.map { it.source }.distinct().forEach { source ->
                            SourceBadge(source = source)
                        }
                    }

                    val maxVotes = versions.maxOfOrNull { it.votes ?: 0 } ?: 0
                    val maxRating = versions.maxOfOrNull { it.rating ?: 0f } ?: 0f
                    if (maxVotes > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RaisedSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Border.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "★",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (maxRating % 1f == 0f) maxRating.toInt().toString() else String.format(java.util.Locale.US, "%.1f", maxRating),
                                    color = PrimaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${formatVoteCount(maxVotes)})",
                                    color = MutedText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatVoteCount(votes: Int): String = when {
    votes >= 1000 -> String.format(java.util.Locale.US, "%.1fk", votes / 1000f)
    else -> votes.toString()
}

@Composable
private fun ArtistSortSegmentedControl(
    selectedOrder: ArtistSongSortOrder,
    onSelectOrder: (ArtistSongSortOrder) -> Unit,
    strings: com.chordapp.localization.AppStrings
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortSegmentItem(
                label = strings.sortAZ,
                icon = null,
                selected = selectedOrder == ArtistSongSortOrder.ALPHABETICAL,
                onClick = { onSelectOrder(ArtistSongSortOrder.ALPHABETICAL) }
            )
            SortSegmentItem(
                label = strings.sortPopular,
                icon = "🔥",
                selected = selectedOrder == ArtistSongSortOrder.POPULAR,
                onClick = { onSelectOrder(ArtistSongSortOrder.POPULAR) }
            )
        }
    }
}

@Composable
private fun SortSegmentItem(
    label: String,
    icon: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) AccentGold else Color.Transparent
    val textColor = if (selected) Background else MutedText
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = fontWeight
            )
        }
    }
}
