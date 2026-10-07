package com.chordapp.ui.screens.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.ui.components.SourceBadge
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.ExploreViewModel

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onArtistClick: (String) -> Unit,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit = { _, _ -> }
) {
    val artists by viewModel.artists.collectAsState()
    val query by viewModel.query.collectAsState()
    val selectedLetter by viewModel.selectedLetter.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val ostSongs by viewModel.ostSongs.collectAsState()
    val selectedOstFranchise by viewModel.selectedOstFranchise.collectAsState()
    val selectedOstCategory by viewModel.selectedOstCategory.collectAsState()
    val ostSearchQuery by viewModel.ostSearchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 28 items: '#' (non-alpha), 'A'..'Z' (26 letters), and '★' (All/Semua)
    // 7 columns x 4 rows = exactly 28 cells without any hanging trailing element!
    val catalogItems = remember {
        listOf('#') + ('A'..'Z').toList() + listOf('★')
    }

    val letterCountMap = remember(artists) {
        val map = mutableMapOf<Char, Int>()
        var nonAlpha = 0
        for (artist in artists) {
            val ch = artist.artistName.trim().firstOrNull()?.uppercaseChar()
            if (ch != null) {
                if (ch in 'A'..'Z') {
                    map[ch] = (map[ch] ?: 0) + 1
                } else {
                    nonAlpha++
                }
            }
        }
        map['#'] = nonAlpha
        map['★'] = artists.size
        map
    }

    val shownArtists = remember(artists, query, selectedLetter, selectedGenre) {
        when {
            query.isNotBlank() -> artists.filter {
                it.artistName.contains(query.trim(), ignoreCase = true)
            }
            selectedGenre != null && !selectedGenre!!.isSongs -> {
                val slugs = selectedGenre!!.artistSlugs
                artists.filter { it.artistSlug in slugs }
            }
            selectedLetter != null -> {
                if (selectedLetter == '#') {
                    artists.filter {
                        val ch = it.artistName.trim().firstOrNull()?.uppercaseChar()
                        ch != null && ch !in 'A'..'Z'
                    }
                } else {
                    artists.filter {
                        it.artistName.trim().firstOrNull()?.uppercaseChar() == selectedLetter
                    }
                }
            }
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EXPLORE",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Katalog Artis & Genre Musik",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (artists.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RaisedSurface,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Text(
                        text = "${artists.size} Artis",
                        color = AccentGold,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text("Cari nama artis atau band...", color = MutedText) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AccentGold)
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian", tint = MutedText)
                    }
                }
            } else null,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText,
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = Border,
                cursorColor = AccentGold
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (query.isNotBlank()) {
            // View: Search Results
            SearchResultsView(
                query = query,
                results = shownArtists,
                onArtistClick = onArtistClick
            )
        } else if (selectedGenre != null) {
            if (selectedGenre!!.isSongs) {
                // View: OST Franchise-based Architecture
                OstDetailView(
                    franchises = OstCatalogData.FRANCHISES,
                    selectedFranchise = selectedOstFranchise,
                    selectedCategory = selectedOstCategory,
                    searchQuery = ostSearchQuery,
                    otherOstSongs = ostSongs,
                    onCategorySelect = viewModel::setOstCategory,
                    onSearchQueryChange = viewModel::setOstSearchQuery,
                    onFranchiseSelect = viewModel::selectOstFranchise,
                    onBackFranchise = viewModel::clearOstFranchise,
                    onBackGenre = viewModel::clearGenre,
                    onSongClick = onSongClick
                )
            } else {
                // View: Genre Artists
                GenreDetailView(
                    genre = selectedGenre!!,
                    artists = shownArtists,
                    onBack = { viewModel.clearGenre() },
                    onArtistClick = onArtistClick
                )
            }
        } else if (selectedLetter != null) {
            // View: Letter Selected
            LetterDetailView(
                selectedLetter = selectedLetter!!,
                catalogItems = catalogItems,
                letterCountMap = letterCountMap,
                artists = shownArtists,
                onSelectLetter = { item ->
                    if (item == '★') viewModel.clearLetter() else viewModel.selectLetter(item)
                },
                onBack = { viewModel.clearLetter() },
                onArtistClick = onArtistClick
            )
        } else {
            // View: Main Explore Overview (Genres + 7-Column Compact Non-alphabetical to Z Grid)
            ExploreMainOverview(
                catalogItems = catalogItems,
                letterCountMap = letterCountMap,
                onGenreClick = { genre -> viewModel.selectGenre(genre) },
                onLetterClick = { item ->
                    if (item != '★') viewModel.selectLetter(item)
                }
            )
        }
    }
}

/**
 * Main Overview:
 * 1. "JELAJAH GENRE & KATEGORI" (Kartu Rock 90s, OST, Pop Indo, Dangdut, dll.)
 * 2. "KATALOG ABJAD (# - Z)" (Grid 7 Kolom x 4 Baris compact, tidak ada yang menggantung)
 */
@Composable
private fun ExploreMainOverview(
    catalogItems: List<Char>,
    letterCountMap: Map<Char, Int>,
    onGenreClick: (ExploreGenre) -> Unit,
    onLetterClick: (Char) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Section 1: Genre & Kategori
        Text(
            text = "JELAJAH GENRE & KATEGORI",
            color = AccentGold,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Grid 2 Kolom untuk Kartu Genre
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val genres = ExploreGenres.ALL
            for (i in genres.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val g1 = genres[i]
                    val g2 = if (i + 1 < genres.size) genres[i + 1] else null

                    GenreCard(
                        genre = g1,
                        modifier = Modifier.weight(1f),
                        onClick = { onGenreClick(g1) }
                    )
                    if (g2 != null) {
                        GenreCard(
                            genre = g2,
                            modifier = Modifier.weight(1f),
                            onClick = { onGenreClick(g2) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 2: Katalog Abjad (# - Z)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KATALOG ABJAD (# — Z)",
                color = AccentGold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Pilih karakter",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 7 Kolom x 4 Baris: Tepat 28 kotak, kompak dan tanpa elemen menggantung
        val shape = RoundedCornerShape(10.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        if (index < catalogItems.size) {
                            val item = catalogItems[index]
                            val count = letterCountMap[item] ?: 0
                            val isEnabled = count > 0

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable(enabled = isEnabled) { onLetterClick(item) },
                                shape = shape,
                                color = if (isEnabled) CardSurface else Background,
                                border = BorderStroke(
                                    1.dp,
                                    if (item == '★') AccentGold.copy(alpha = 0.5f) else Border
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = item.toString(),
                                        color = if (isEnabled) {
                                            if (item == '★') AccentGold else PrimaryText
                                        } else MutedText.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (item == '★') "Semua" else count.toString(),
                                        color = if (isEnabled) MutedText else MutedText.copy(alpha = 0.3f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun GenreCard(
    genre: ExploreGenre,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
        shape = shape,
        color = CardSurface,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MutedGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (genre.isSongs) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = genre.title,
                    color = PrimaryText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (genre.isSongs) "${OstCatalogData.FRANCHISES.size} Franchise • Film & Anime" else "${genre.artistSlugs.size} Artis",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GenreDetailView(
    genre: ExploreGenre,
    artists: List<CatalogArtistInfo>,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable { onBack() },
                shape = RoundedCornerShape(10.dp),
                color = RaisedSurface,
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kembali ke Jelajah",
                        color = PrimaryText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = genre.title,
                    color = AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${artists.size} artis ditemukan",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Artists in Genre
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(artists, key = { it.artistSlug }) { artist ->
                ArtistCard(artist = artist, onClick = { onArtistClick(artist.artistSlug) })
            }
        }
    }
}

@Composable
private fun OstDetailView(
    franchises: List<OstFranchise>,
    selectedFranchise: OstFranchise?,
    selectedCategory: String,
    searchQuery: String,
    otherOstSongs: List<CatalogSongGroup>,
    onCategorySelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFranchiseSelect: (OstFranchise) -> Unit,
    onBackFranchise: () -> Unit,
    onBackGenre: () -> Unit,
    onSongClick: (String, String) -> Unit
) {
    if (selectedFranchise != null) {
        OstFranchiseDetailView(
            franchise = selectedFranchise,
            onBack = onBackFranchise,
            onSongClick = onSongClick
        )
    } else {
        OstFranchisesOverview(
            franchises = franchises,
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            otherOstSongs = otherOstSongs,
            onCategorySelect = onCategorySelect,
            onSearchQueryChange = onSearchQueryChange,
            onFranchiseSelect = onFranchiseSelect,
            onBack = onBackGenre,
            onSongClick = onSongClick
        )
    }
}

@Composable
private fun OstFranchiseDetailView(
    franchise: OstFranchise,
    onBack: () -> Unit,
    onSongClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable { onBack() },
                shape = RoundedCornerShape(10.dp),
                color = RaisedSurface,
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Franchise",
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kembali ke Franchise",
                        color = PrimaryText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = franchise.title,
                    color = AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${franchise.category} • ${franchise.songs.size} Soundtrack",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(franchise.songs, key = { "${it.artistSlug}_${it.titleSlug}_${it.role.orEmpty()}" }) { song ->
                OstFranchiseSongCard(
                    song = song,
                    onClick = { onSongClick(song.artistSlug, song.titleSlug) }
                )
            }
        }
    }
}

@Composable
private fun OstFranchisesOverview(
    franchises: List<OstFranchise>,
    selectedCategory: String,
    searchQuery: String,
    otherOstSongs: List<CatalogSongGroup>,
    onCategorySelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFranchiseSelect: (OstFranchise) -> Unit,
    onBack: () -> Unit,
    onSongClick: (String, String) -> Unit
) {
    val filteredFranchises = remember(franchises, selectedCategory, searchQuery) {
        franchises.filter { f ->
            val matchCategory = selectedCategory == "Semua" || f.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                f.title.contains(searchQuery, ignoreCase = true) ||
                f.subtitle.contains(searchQuery, ignoreCase = true) ||
                f.songs.any { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
            matchCategory && matchSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable { onBack() },
                shape = RoundedCornerShape(10.dp),
                color = RaisedSurface,
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Jelajah",
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kembali ke Jelajah",
                        color = PrimaryText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "OST & Soundtrack",
                    color = AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredFranchises.size} franchise ditemukan",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Field within OST
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text("Cari franchise atau lagu soundtrack...", color = MutedText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian", tint = MutedText)
                    }
                }
            } else null,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText,
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = Border,
                cursorColor = AccentGold
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(OstCatalogData.CATEGORIES) { cat ->
                val isSelected = cat == selectedCategory
                Surface(
                    modifier = Modifier.clickable { onCategorySelect(cat) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AccentGold.copy(alpha = 0.2f) else CardSurface,
                    border = BorderStroke(1.dp, if (isSelected) AccentGold else Border)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) AccentGold else PrimaryText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(filteredFranchises, key = { it.id }) { franchise ->
                OstFranchiseCard(
                    franchise = franchise,
                    onClick = { onFranchiseSelect(franchise) }
                )
            }
            if (otherOstSongs.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "Semua") {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LAGU SOUNDTRACK LAINNYA",
                        color = AccentGold,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                items(otherOstSongs, key = { "other_${it.artistSlug}_${it.titleSlug}" }) { song ->
                    OstSongCard(
                        song = song,
                        onClick = { onSongClick(song.artistSlug, song.titleSlug) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OstFranchiseCard(
    franchise: OstFranchise,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = shape,
        color = CardSurface,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MutedGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = franchise.title,
                        color = PrimaryText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RaisedSurface,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Text(
                            text = "${franchise.songs.size} Lagu",
                            color = AccentGold,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${franchise.category} • ${franchise.subtitle}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "›",
                color = AccentGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OstFranchiseSongCard(
    song: OstSongItem,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = shape,
        color = CardSurface,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MutedGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        color = PrimaryText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!song.role.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = song.role,
                                color = AccentGold,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = song.artist,
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "›",
                color = AccentGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OstSongCard(
    song: CatalogSongGroup,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = shape,
        color = CardSurface,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MutedGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artistName} • ${song.versionCount} versi chord",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "›",
                color = AccentGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LetterDetailView(
    selectedLetter: Char,
    catalogItems: List<Char>,
    letterCountMap: Map<Char, Int>,
    artists: List<CatalogArtistInfo>,
    onSelectLetter: (Char) -> Unit,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Alphabet Scroller
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Surface(
                    modifier = Modifier.clickable { onBack() },
                    shape = RoundedCornerShape(10.dp),
                    color = RaisedSurface,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Text(
                        text = "‹ Semua",
                        color = AccentGold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            items(catalogItems) { item ->
                if (item != '★') {
                    val isSelected = item == selectedLetter
                    val count = letterCountMap[item] ?: 0
                    val isEnabled = count > 0

                    Surface(
                        modifier = Modifier.clickable(enabled = isEnabled) { onSelectLetter(item) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AccentGold else if (isEnabled) CardSurface else Background,
                        border = BorderStroke(1.dp, if (isSelected) AccentGold else Border)
                    ) {
                        Text(
                            text = item.toString(),
                            color = if (isSelected) Background else if (isEnabled) PrimaryText else MutedText.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ARTIS ${if (selectedLetter == '#') "NON-ALFABET (#)" else selectedLetter.toString()}",
                color = AccentGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "${artists.size} artis ditemukan",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(artists, key = { it.artistSlug }) { artist ->
                ArtistCard(artist = artist, onClick = { onArtistClick(artist.artistSlug) })
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    results: List<CatalogArtistInfo>,
    onArtistClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HASIL PENCARIAN",
                color = AccentGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "${results.size} artis",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada artis \"$query\"",
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coba gunakan kata kunci nama artis lainnya.",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(results, key = { it.artistSlug }) { artist ->
                    ArtistCard(artist = artist, onClick = { onArtistClick(artist.artistSlug) })
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: CatalogArtistInfo,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = shape,
        color = CardSurface,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = artist.artistName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "♪"
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(RaisedSurface, CircleShape)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.artistName,
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${artist.songCount} lagu",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(RaisedSurface, CircleShape)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    color = AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
