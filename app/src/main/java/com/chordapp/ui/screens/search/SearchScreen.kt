package com.chordapp.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.ui.components.RegionBadge
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.ScrapingState
import com.chordapp.viewmodels.SearchSource
import com.chordapp.viewmodels.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onSongClick: (String, String) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    val query by viewModel.query.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val songResults by viewModel.songResults.collectAsState()
    val ugVersionResults by viewModel.ugVersionResults.collectAsState()
    val gtVersionResults by viewModel.gtVersionResults.collectAsState()
    val ciVersionResults by viewModel.ciVersionResults.collectAsState()
    val ugCurrentPage by viewModel.ugCurrentPage.collectAsState()
    val ugTotalPages by viewModel.ugTotalPages.collectAsState()
    val isLoadingMoreUg by viewModel.isLoadingMoreUg.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val scrapingState by viewModel.scrapingState.collectAsState()
    val previewSong by viewModel.previewSong.collectAsState()
    val isPreviewLoading by viewModel.isPreviewLoading.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(scrapingState) {
        when (val s = scrapingState) {
            is ScrapingState.SongReady -> {
                onSongClick(s.artistSlug, s.titleSlug)
                viewModel.dismissScrapingState()
            }
            is ScrapingState.ArtistReady -> {
                onArtistClick(s.artistSlug)
                viewModel.dismissScrapingState()
            }
            else -> {}
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = true) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Search bar with separate circular back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .background(Background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular back button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CardSurface, CircleShape)
                        .border(1.dp, Border, CircleShape)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = PrimaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Search pill
                BasicTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = PrimaryText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(AccentGold),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CardSurface, RoundedCornerShape(24.dp))
                                .border(1.dp, Border, RoundedCornerShape(24.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = strings.searchPlaceholder,
                                        color = MutedText,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                                    )
                                }
                                innerTextField()
                            }
                            if (query.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { viewModel.onQueryChange("") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = strings.clearSearch,
                                        tint = MutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // Source Selector Pills: [ Local ] [ UG ] [ GT ] [ CI ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchSourcePill(
                label = "Local",
                badge = "Offline",
                isSelected = selectedSource == SearchSource.LOCAL,
                onClick = { viewModel.setSource(SearchSource.LOCAL) }
            )
            SearchSourcePill(
                label = "UG",
                badge = "Online",
                isSelected = selectedSource == SearchSource.UG,
                onClick = { viewModel.setSource(SearchSource.UG) }
            )
            SearchSourcePill(
                label = "GT",
                badge = "Online",
                isSelected = selectedSource == SearchSource.GT,
                onClick = { viewModel.setSource(SearchSource.GT) }
            )
            SearchSourcePill(
                label = "CI",
                badge = "Online",
                isSelected = selectedSource == SearchSource.CI,
                onClick = { viewModel.setSource(SearchSource.CI) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = Border, thickness = 1.dp)

        // Results Content Area
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                when (selectedSource) {
                    SearchSource.LOCAL -> {
                        // Section: ARTIS
                        if (artistResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = strings.artistsHeader,
                                    color = AccentGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(artistResults, key = { "artist_${it.artistSlug}" }) { artist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable { onArtistClick(artist.artistSlug) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(CardSurface, RoundedCornerShape(21.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("👤", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = artist.artistName,
                                                    color = PrimaryText,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                artist.region?.let { reg ->
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    RegionBadge(region = reg, compact = true)
                                                }
                                            }
                                            Text(
                                                text = strings.artistSongsBadge(artist.songCount),
                                                color = MutedText,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Section: LAGU
                        if (songResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = strings.songsHeader,
                                    color = MutedText,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(songResults, key = { "song_${it.artistSlug}_${it.titleSlug}" }) { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSongClick(song.artistSlug, song.titleSlug) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("♪", color = AccentGold, modifier = Modifier.padding(end = 16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = song.title, color = PrimaryText, style = MaterialTheme.typography.bodyLarge)
                                        Text(text = song.artistName, color = MutedText, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                HorizontalDivider(color = Border)
                            }
                        }

                        // Empty State for Local
                        if (artistResults.isEmpty() && songResults.isEmpty() && query.isNotBlank()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = strings.noResultsFor(query.trim()),
                                        color = MutedText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.setSource(SearchSource.UG) },
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold)
                                        ) {
                                            Text("Cari di UG (Online)", color = AccentGold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.setSource(SearchSource.GT) },
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold)
                                        ) {
                                            Text("Cari di GT (Online)", color = AccentGold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SearchSource.UG -> {
                        // UG ONLINE MODE
                        if (query.isBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Ketik judul lagu atau nama artis untuk mencari di UG (Online).",
                                        color = MutedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        } else {
                            // Card Action: Tarik Semua Lagu Artis
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp)
                                        .clickable {
                                            keyboardController?.hide()
                                            viewModel.scrapeArtist(query.trim())
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(21.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎙️", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Tarik Semua Lagu Artis \"${query.trim()}\" dari UG",
                                                color = PrimaryText,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Unduh semua lagu artis ini dari UG (2 versi: Top Votes & Rating)",
                                                color = MutedText,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = null,
                                            tint = AccentGold
                                        )
                                    }
                                }
                            }

                            // Header Versi Lagu
                            item {
                                Text(
                                    text = "DAFTAR VERSI CHORD DI UG",
                                    color = AccentGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )
                            }

                            if (ugVersionResults.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Tidak ada chord ditemukan untuk \"${query.trim()}\" di UG.", color = MutedText)
                                    }
                                }
                            } else {
                                items(ugVersionResults) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        color = PrimaryText,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = item.artist,
                                                        color = MutedText,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                // Version badge
                                                Surface(
                                                    color = AccentGold.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = item.versionName,
                                                        color = AccentGold,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Rating & Key info
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                item.rating?.let { r ->
                                                    Text(
                                                        text = "⭐️ ${String.format("%.2f", r)} (${item.votes ?: 0} votes)",
                                                        color = MutedText,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                item.keyBase?.let { k ->
                                                    Text(
                                                        text = "• Kunci: $k",
                                                        color = MutedText,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                Text(
                                                    text = "• UG (Online)",
                                                    color = MutedText,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Buttons: Preview & Unduh
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.openPreview(item.tabUrl, artistHint = item.artist, titleHint = item.title) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Visibility,
                                                        contentDescription = null,
                                                        tint = PrimaryText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Preview", color = PrimaryText, fontSize = 13.sp)
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Button(
                                                    onClick = { viewModel.downloadUgVersion(item) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = null,
                                                        tint = Background,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Unduh", color = Background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (ugCurrentPage < ugTotalPages) {
                                    item {
                                        OutlinedButton(
                                            onClick = { viewModel.loadMoreUg() },
                                            enabled = !isLoadingMoreUg,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold)
                                        ) {
                                            if (isLoadingMoreUg) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = AccentGold,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Memuat halaman berikutnya...", color = AccentGold, fontSize = 13.sp)
                                            } else {
                                                Text(
                                                    "▼  Muat Lagu Lainnya (Halaman $ugCurrentPage dari $ugTotalPages)",
                                                    color = AccentGold,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SearchSource.GT -> {
                        // GT ONLINE MODE
                        if (query.isBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Ketik judul lagu atau nama artis untuk mencari di GT (Online).",
                                        color = MutedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        } else {
                            // Card Action: Tarik Semua Lagu Artis GT
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp)
                                        .clickable {
                                            keyboardController?.hide()
                                            viewModel.scrapeArtistGt(query.trim())
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(21.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎸", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Tarik Semua Lagu Artis \"${query.trim()}\" dari GT",
                                                color = PrimaryText,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Unduh semua lagu artis ini dari GT (2 versi: Easy & Original)",
                                                color = MutedText,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = null,
                                            tint = AccentGold
                                        )
                                    }
                                }
                            }

                            // Header Versi Lagu
                            item {
                                Text(
                                    text = "DAFTAR VERSI CHORD DI GT",
                                    color = AccentGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )
                            }

                            if (gtVersionResults.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Tidak ada chord ditemukan untuk \"${query.trim()}\" di GT.", color = MutedText)
                                    }
                                }
                            } else {
                                items(gtVersionResults) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        color = PrimaryText,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = item.artist,
                                                        color = MutedText,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                // Version badge
                                                Surface(
                                                    color = AccentGold.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = item.versionName,
                                                        color = AccentGold,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Rating & Source info
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                item.rating?.let { r ->
                                                    Text(
                                                        text = "⭐️ ${String.format("%.2f", r)}",
                                                        color = MutedText,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                Text(
                                                    text = "• GT (Online)",
                                                    color = MutedText,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Buttons: Preview & Unduh
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.openPreview(item.tabUrl, artistHint = item.artist, titleHint = item.title) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Visibility,
                                                        contentDescription = null,
                                                        tint = PrimaryText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Preview", color = PrimaryText, fontSize = 13.sp)
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Button(
                                                    onClick = { viewModel.downloadGtVersion(item) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = null,
                                                        tint = Background,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Unduh", color = Background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SearchSource.CI -> {
                        // CI ONLINE MODE
                        if (query.isBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Ketik nama artis atau judul lagu untuk mencari di CI (Online).",
                                        color = MutedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        } else {
                            // Card Action: Tarik Semua Lagu Artis CI
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp)
                                        .clickable {
                                            keyboardController?.hide()
                                            viewModel.scrapeArtistCi(query.trim())
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(21.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🇮🇩", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Tarik Semua Lagu Artis \"${query.trim()}\" dari CI",
                                                color = PrimaryText,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Unduh semua lagu artis ini dari katalog CI",
                                                color = MutedText,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = null,
                                            tint = AccentGold
                                        )
                                    }
                                }
                            }

                            // Header Versi Lagu
                            item {
                                Text(
                                    text = "DAFTAR VERSI CHORD DI CI",
                                    color = AccentGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )
                            }

                            if (ciVersionResults.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Tidak ada chord ditemukan untuk \"${query.trim()}\" di CI.", color = MutedText)
                                    }
                                }
                            } else {
                                items(ciVersionResults) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        color = PrimaryText,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = item.artist,
                                                        color = MutedText,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                // Version badge
                                                Surface(
                                                    color = AccentGold.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = item.versionName,
                                                        color = AccentGold,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Rating & Source info
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                item.rating?.let { r ->
                                                    Text(
                                                        text = "⭐️ ${String.format("%.2f", r)}",
                                                        color = MutedText,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                Text(
                                                    text = "• CI (Online)",
                                                    color = MutedText,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Buttons: Preview & Unduh
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.openPreview(item.tabUrl, artistHint = item.artist, titleHint = item.title) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Visibility,
                                                        contentDescription = null,
                                                        tint = PrimaryText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Preview", color = PrimaryText, fontSize = 13.sp)
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Button(
                                                    onClick = { viewModel.downloadCiVersion(item) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = null,
                                                        tint = Background,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Unduh", color = Background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog Preview Chord Sheet
    if (isPreviewLoading) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = RaisedSurface,
            title = { Text("Memuat Preview...", color = PrimaryText) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentGold)
                }
            },
            confirmButton = {}
        )
    }

    previewSong?.let { song ->
        AlertDialog(
            onDismissRequest = { viewModel.closePreview() },
            containerColor = RaisedSurface,
            titleContentColor = PrimaryText,
            title = {
                Column {
                    Text(text = song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${song.artist} • ${song.versionName} • Kunci: ${song.keyBase ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .background(Background, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = song.content,
                        color = PrimaryText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closePreview() }) {
                    Text("Tutup", color = MutedText)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadPreviewedSong() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Unduh & Mainkan", color = Background, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialogs for scraping (Artist & Song)
    when (val state = scrapingState) {
        is ScrapingState.LoadingSong -> {
            AlertDialog(
                onDismissRequest = { viewModel.cancelScraping() },
                containerColor = RaisedSurface,
                titleContentColor = PrimaryText,
                textContentColor = MutedText,
                title = { Text("Menarik Lagu dari UG") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Mencari & mengunduh \"${state.query}\"...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        CircularProgressIndicator(color = AccentGold)
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelScraping() }) {
                        Text(strings.cancel, color = AccentGold)
                    }
                }
            )
        }
        is ScrapingState.LoadingArtist -> {
            AlertDialog(
                onDismissRequest = { viewModel.cancelScraping() },
                containerColor = RaisedSurface,
                titleContentColor = PrimaryText,
                textContentColor = MutedText,
                title = { Text("Menarik Katalog Artis") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Artis: ${state.artist}",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.total == 0) {
                            Text(
                                text = state.phase,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = AccentGold,
                                trackColor = Border
                            )
                        } else {
                            Text(
                                text = "Mengunduh ${state.current} dari ${state.total} lagu:\n${state.currentSongTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryText
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            LinearProgressIndicator(
                                progress = { state.current.toFloat() / state.total },
                                modifier = Modifier.fillMaxWidth(),
                                color = AccentGold,
                                trackColor = Border
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelScraping() }) {
                        Text(strings.cancel, color = AccentGold)
                    }
                }
            )
        }
        is ScrapingState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissScrapingState() },
                containerColor = RaisedSurface,
                titleContentColor = PrimaryText,
                textContentColor = MutedText,
                title = { Text("Pemberitahuan") },
                text = {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissScrapingState() }) {
                        Text(strings.close, color = AccentGold)
                    }
                }
            )
        }
        else -> {}
    }
}

@Composable
private fun SearchSourcePill(
    label: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AccentGold.copy(alpha = 0.15f) else CardSurface
    val borderColor = if (isSelected) AccentGold else Border
    val textColor = if (isSelected) AccentGold else PrimaryText
    val badgeColor = if (isSelected) AccentGold.copy(alpha = 0.8f) else MutedText

    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AccentGold, CircleShape)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor
        )
        Text(
            text = "($badge)",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = badgeColor
        )
    }
}
