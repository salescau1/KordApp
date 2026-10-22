package com.chordapp.ui.screens.explore

import androidx.compose.ui.draw.clip

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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.ui.components.RegionBadge
import com.chordapp.ui.components.SourceBadge
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.ExploreViewModel

private val DeleteRed = Color(0xFFE53935)

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

    val artistSearchMap = remember(artists) {
        artists.map { artist ->
            artist to com.chordapp.catalog.foldDiacritics(artist.artistName).lowercase(java.util.Locale.ROOT)
        }
    }

    val shownArtists = remember(artists, query, selectedLetter, selectedGenre, artistSearchMap) {
        when {
            query.isNotBlank() -> {
                val q = query.trim()
                val normQ = com.chordapp.catalog.foldDiacritics(q).lowercase(java.util.Locale.ROOT)
                val matches = ArrayList<CatalogArtistInfo>()
                for ((artist, normName) in artistSearchMap) {
                    if (normName.contains(normQ)) {
                        matches.add(artist)
                    }
                }
                matches
            }
            selectedGenre != null && !selectedGenre!!.isSongs -> {
                val slugs = selectedGenre!!.artistSlugs
                artists.filter { it.artistSlug in slugs }
            }
            selectedLetter != null -> {
                if (selectedLetter == '#') {
                    artists.filter {
                        val ch = com.chordapp.catalog.foldDiacritics(it.artistName.trim().take(1)).firstOrNull()?.uppercaseChar()
                        ch != null && ch !in 'A'..'Z'
                    }
                } else {
                    artists.filter {
                        val ch = com.chordapp.catalog.foldDiacritics(it.artistName.trim().take(1)).firstOrNull()?.uppercaseChar()
                        ch == selectedLetter
                    }
                }
            }
            else -> emptyList()
        }
    }

    val isEditMode by viewModel.isEditMode.collectAsState()
    val deletedArtists by viewModel.deletedArtistSlugs.collectAsState()
    val deletedSongs by viewModel.deletedSongs.collectAsState()
    val deletedVersions by viewModel.deletedVersionIds.collectAsState()
    val editingArtist by viewModel.editingArtist.collectAsState()
    val editingArtistSongs by viewModel.editingArtistSongs.collectAsState()
    var showApplyDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val hasPendingDeletions = (deletedArtists.size + deletedSongs.size + deletedVersions.size) > 0

    BackHandler(enabled = isEditMode) {
        if (editingArtist != null) {
            viewModel.closeArtistInEditMode()
        } else if (hasPendingDeletions) {
            showDiscardDialog = true
        } else {
            viewModel.cancelEditMode()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            val strings = com.chordapp.localization.LocalAppStrings.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditMode) "EDIT MODE" else strings.exploreTitle,
                        color = if (isEditMode) AccentGold else PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEditMode) "Customize Your Database" else strings.exploreSubtitle,
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!isEditMode) {
                    Surface(
                        modifier = Modifier.clickable { viewModel.setEditMode(true) },
                        shape = RoundedCornerShape(12.dp),
                        color = RaisedSurface,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Mode",
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.clickable {
                            if (hasPendingDeletions) {
                                showDiscardDialog = true
                            } else {
                                viewModel.cancelEditMode()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = RaisedSurface,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Keluar Edit Mode",
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (editingArtist != null) {
                // View: Artist song inspection in Edit Mode
                EditModeArtistSongDetailView(
                    artist = editingArtist!!,
                    songs = editingArtistSongs,
                    deletedSongs = deletedSongs,
                    deletedVersions = deletedVersions,
                    onToggleSong = viewModel::toggleSongDeletion,
                    onToggleVersion = viewModel::toggleVersionDeletion,
                    onBack = viewModel::closeArtistInEditMode
                )
            } else {
                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    placeholder = { Text(strings.searchArtistPlaceholder, color = MutedText) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = AccentGold)
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = strings.clearSearch, tint = MutedText)
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
                        isEditMode = isEditMode,
                        deletedArtistSlugs = deletedArtists,
                        onToggleArtistDelete = viewModel::toggleArtistDeletion,
                        onArtistClick = { artist ->
                            if (isEditMode) viewModel.openArtistInEditMode(artist) else onArtistClick(artist.artistSlug)
                        }
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
                            isEditMode = isEditMode,
                            deletedArtistSlugs = deletedArtists,
                            onToggleArtistDelete = viewModel::toggleArtistDeletion,
                            onBack = { viewModel.clearGenre() },
                            onArtistClick = { artist ->
                                if (isEditMode) viewModel.openArtistInEditMode(artist) else onArtistClick(artist.artistSlug)
                            }
                        )
                    }
                } else if (selectedLetter != null) {
                    // View: Letter Selected
                    LetterDetailView(
                        selectedLetter = selectedLetter!!,
                        catalogItems = catalogItems,
                        letterCountMap = letterCountMap,
                        artists = shownArtists,
                        isEditMode = isEditMode,
                        deletedArtistSlugs = deletedArtists,
                        onToggleArtistDelete = viewModel::toggleArtistDeletion,
                        onSelectLetter = { item ->
                            if (item == '★') viewModel.clearLetter() else viewModel.selectLetter(item)
                        },
                        onBack = { viewModel.clearLetter() },
                        onArtistClick = { artist ->
                            if (isEditMode) viewModel.openArtistInEditMode(artist) else onArtistClick(artist.artistSlug)
                        }
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

        // Floating Action Bar in Edit Mode
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.30f to Background.copy(alpha = 0.65f),
                            0.65f to Background.copy(alpha = 0.90f),
                            1.0f to Background
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${deletedArtists.size} Artists • ${deletedSongs.size} Songs • ${deletedVersions.size} Versions",
                            color = PrimaryText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { showApplyDialog = true },
                            enabled = hasPendingDeletions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeleteRed,
                                disabledContainerColor = DeleteRed.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Apply",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Dialog Konfirmasi Apply
        if (showApplyDialog) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                title = {
                    Text("Konfirmasi Hapus Database", color = PrimaryText, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Tindakan ini akan menghapus ${deletedArtists.size} Artis, ${deletedSongs.size} Lagu, dan ${deletedVersions.size} Versi secara permanen dari database lokal perangkat.",
                        color = MutedText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyDeletions()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeleteRed)
                    ) {
                        Text("Hapus Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApplyDialog = false }) {
                        Text("Batal", color = AccentGold)
                    }
                },
                containerColor = CardSurface
            )
        }

        // Dialog Safeguard saat Batal / Keluar Edit Mode jika ada seleksi
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = {
                    Text("Keluar dari Edit Mode?", color = PrimaryText, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Pilihan penghapusan yang belum di-Apply tidak akan dihapus dari database.",
                        color = MutedText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDiscardDialog = false
                            viewModel.cancelEditMode()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaisedSurface)
                    ) {
                        Text("Keluar", color = PrimaryText, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Lanjut Edit", color = AccentGold)
                    }
                },
                containerColor = CardSurface
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
    val strings = com.chordapp.localization.LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Section 1: Genre & Kategori
        Text(
            text = strings.genresAndCategoriesSection,
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
                text = strings.alphabeticalCatalogSection,
                color = AccentGold,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = strings.selectCharacter,
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
                                        text = if (item == '★') strings.all else count.toString(),
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
                val strings = com.chordapp.localization.LocalAppStrings.current
                Text(
                    text = if (genre.isSongs) "${OstCatalogData.FRANCHISES.size} Franchises" else strings.artistsCount(genre.artistSlugs.size),
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
    isEditMode: Boolean = false,
    deletedArtistSlugs: Set<String> = emptySet(),
    onToggleArtistDelete: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    onArtistClick: (CatalogArtistInfo) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
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
                        contentDescription = strings.back,
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.backToExplore,
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
                    text = strings.artistsFound(artists.size),
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
            contentPadding = PaddingValues(bottom = if (isEditMode) 110.dp else 96.dp)
        ) {
            items(artists, key = { it.artistSlug }) { artist ->
                ArtistCard(
                    artist = artist,
                    isEditMode = isEditMode,
                    isDeleted = deletedArtistSlugs.contains(artist.artistSlug),
                    onToggleDelete = { onToggleArtistDelete?.invoke(artist.artistSlug) },
                    onClick = { onArtistClick(artist) }
                )
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
    val strings = com.chordapp.localization.LocalAppStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
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
                        contentDescription = strings.backToFranchises,
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.backToFranchises,
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
                    text = "${franchise.category} • ${strings.songsCount(franchise.songs.size)}",
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
    val strings = com.chordapp.localization.LocalAppStrings.current
    val filteredFranchises = remember(franchises, selectedCategory, searchQuery) {
        franchises.filter { f ->
            val matchCategory = selectedCategory == "Semua" || selectedCategory == strings.all || f.category == selectedCategory
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
                onClick = onBack,
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
                        contentDescription = strings.backToExplore,
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.backToExplore,
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
                    text = "${filteredFranchises.size} franchises",
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
            placeholder = { Text(strings.searchOstPlaceholder, color = MutedText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = strings.clearSearch, tint = MutedText)
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
            if (otherOstSongs.isNotEmpty() && searchQuery.isBlank() && (selectedCategory == "Semua" || selectedCategory == strings.all)) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.otherSoundtracksSection,
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
    val strings = com.chordapp.localization.LocalAppStrings.current
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
                            text = strings.songsCount(franchise.songs.size),
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
            val strings = com.chordapp.localization.LocalAppStrings.current
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
                    text = "${song.artistName} • ${strings.chordVersionsCount(song.versionCount)}",
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
    isEditMode: Boolean = false,
    deletedArtistSlugs: Set<String> = emptySet(),
    onToggleArtistDelete: ((String) -> Unit)? = null,
    onSelectLetter: (Char) -> Unit,
    onBack: () -> Unit,
    onArtistClick: (CatalogArtistInfo) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
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
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    color = RaisedSurface,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Text(
                        text = strings.allBack,
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
                text = "${strings.navArtist.uppercase()} ${if (selectedLetter == '#') "#" else selectedLetter.toString()}",
                color = AccentGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = strings.artistsFound(artists.size),
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
            contentPadding = PaddingValues(bottom = if (isEditMode) 110.dp else 96.dp)
        ) {
            items(artists, key = { it.artistSlug }) { artist ->
                ArtistCard(
                    artist = artist,
                    isEditMode = isEditMode,
                    isDeleted = deletedArtistSlugs.contains(artist.artistSlug),
                    onToggleDelete = { onToggleArtistDelete?.invoke(artist.artistSlug) },
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    results: List<CatalogArtistInfo>,
    isEditMode: Boolean = false,
    deletedArtistSlugs: Set<String> = emptySet(),
    onToggleArtistDelete: ((String) -> Unit)? = null,
    onArtistClick: (CatalogArtistInfo) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.searchResults,
                color = AccentGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = strings.artistsCount(results.size),
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
                        text = strings.noArtistsFound(query),
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.tryDifferentArtistKeyword,
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
                contentPadding = PaddingValues(bottom = if (isEditMode) 110.dp else 96.dp)
            ) {
                items(results, key = { it.artistSlug }) { artist ->
                    ArtistCard(
                        artist = artist,
                        isEditMode = isEditMode,
                        isDeleted = deletedArtistSlugs.contains(artist.artistSlug),
                        onToggleDelete = { onToggleArtistDelete?.invoke(artist.artistSlug) },
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: CatalogArtistInfo,
    isEditMode: Boolean = false,
    isDeleted: Boolean = false,
    onToggleDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    val shape = RoundedCornerShape(14.dp)
    val cardAlpha = if (isEditMode && isDeleted) 0.55f else 1.0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !(isEditMode && isDeleted)) { onClick() },
        shape = shape,
        color = CardSurface.copy(alpha = cardAlpha),
        border = BorderStroke(
            1.dp,
            if (isEditMode && isDeleted) Color(0xFFC62828).copy(alpha = 0.5f) else Border
        )
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
                    color = if (isEditMode && isDeleted) MutedText else AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artist.artistName,
                        color = if (isEditMode && isDeleted) MutedText else PrimaryText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    artist.region?.let { reg ->
                        Spacer(modifier = Modifier.width(6.dp))
                        RegionBadge(region = reg, compact = true)
                    }
                }
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
                        text = strings.songsCount(artist.songCount),
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (isEditMode) {
                ColorCircleSwitch(
                    isKeep = !isDeleted,
                    onToggle = { onToggleDelete?.invoke() }
                )
            } else {
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
}

/**
 * Switcher geser kanan-kiri bulat sederhana:
 * Hijau (Keep) = Bulatan di kanan, warna hijau terang.
 * Merah (Hapus) = Bulatan di kiri, warna merah terang.
 */
@Composable
private fun ColorCircleSwitch(
    isKeep: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackBg by animateColorAsState(if (isKeep) Color(0xFF143823) else Color(0xFF3D1616), label = "trackBg")
    val trackBorder by animateColorAsState(if (isKeep) Color(0xFF2E7D32) else Color(0xFFC62828), label = "trackBorder")
    val knobColor by animateColorAsState(if (isKeep) Color(0xFF4CAF50) else Color(0xFFE53935), label = "knobColor")
    val alignment = if (isKeep) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .width(46.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(trackBg)
            .border(1.dp, trackBorder, CircleShape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 3.dp),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(knobColor, CircleShape)
        )
    }
}

/**
 * Level 2: Tampilan inspeksi dan pruning lagu artis dalam Edit Mode (Alphabetical A-Z).
 */
@Composable
private fun EditModeArtistSongDetailView(
    artist: CatalogArtistInfo,
    songs: Map<CatalogSongGroup, List<com.chordapp.catalog.CatalogSongEntity>>,
    deletedSongs: Set<Pair<String, String>>,
    deletedVersions: Set<Long>,
    onToggleSong: (artistSlug: String, titleSlug: String) -> Unit,
    onToggleVersion: (versionId: Long) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                color = RaisedSurface,
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                        text = "Kembali ke Artis",
                        color = PrimaryText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.artistName,
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Pruning lagu & versi (A–Z)",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CardSurface,
                border = BorderStroke(1.dp, Border)
            ) {
                Text(
                    text = "${songs.size} Lagu",
                    color = AccentGold,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(songs.entries.toList(), key = { it.key.titleSlug }) { entry ->
                    val songGroup = entry.key
                    val versions = entry.value
                    EditModeSongRow(
                        artistSlug = artist.artistSlug,
                        songGroup = songGroup,
                        versions = versions,
                        isSongDeleted = deletedSongs.contains(Pair(artist.artistSlug, songGroup.titleSlug)),
                        deletedVersions = deletedVersions,
                        onToggleSong = { onToggleSong(artist.artistSlug, songGroup.titleSlug) },
                        onToggleVersion = onToggleVersion
                    )
                }
            }
        }
    }
}

@Composable
private fun EditModeSongRow(
    artistSlug: String,
    songGroup: CatalogSongGroup,
    versions: List<com.chordapp.catalog.CatalogSongEntity>,
    isSongDeleted: Boolean,
    deletedVersions: Set<Long>,
    onToggleSong: () -> Unit,
    onToggleVersion: (Long) -> Unit
) {
    val isMultiVersion = versions.size > 1
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardSurface.copy(alpha = if (isSongDeleted) 0.55f else 1.0f),
        border = BorderStroke(
            1.dp,
            if (isSongDeleted) Color(0xFFC62828).copy(alpha = 0.5f) else Border
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Main Song Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isSongDeleted) MutedText else AccentGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songGroup.title,
                        color = if (isSongDeleted) MutedText else PrimaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isMultiVersion) "${versions.size} Versi chord" else "${versions.firstOrNull()?.source ?: "Katalog"} • Key ${versions.firstOrNull()?.keyBase ?: "-"}",
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isMultiVersion) {
                    // Expand/Collapse Badge
                    Surface(
                        modifier = Modifier.clickable { isExpanded = !isExpanded },
                        shape = RoundedCornerShape(8.dp),
                        color = RaisedSurface,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${versions.size} Versi",
                                color = AccentGold,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Whole-song delete toggle
                ColorCircleSwitch(
                    isKeep = !isSongDeleted,
                    onToggle = onToggleSong
                )
            }

            // Expanded Sub-Versions List
            if (isMultiVersion && isExpanded && !isSongDeleted) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                versions.forEachIndexed { idx, ver ->
                    val isVerDeleted = ver.id != null && deletedVersions.contains(ver.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${ver.source} ${ver.versionName ?: "v${idx + 1}"}",
                                    color = if (isVerDeleted) MutedText else PrimaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                SourceBadge(source = ver.source)
                            }
                            Text(
                                text = "Key: ${ver.keyBase ?: "-"} • ⭐${ver.rating ?: 0f} (${ver.votes ?: 0} votes)",
                                color = MutedText,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Version delete toggle
                        ColorCircleSwitch(
                            isKeep = !isVerDeleted,
                            onToggle = { ver.id?.let { onToggleVersion(it) } }
                        )
                    }
                    if (idx < versions.lastIndex) {
                        HorizontalDivider(
                            color = Border.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
