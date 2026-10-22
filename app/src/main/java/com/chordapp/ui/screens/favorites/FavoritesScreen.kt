package com.chordapp.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chordapp.localization.LocalAppStrings
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.FavoriteSort
import com.chordapp.viewmodels.FavoritesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onSongClick: (sourceUrl: String, transpose: Int, capo: Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val stored by viewModel.favorites.collectAsState()
    val items by viewModel.items.collectAsState()
    val query by viewModel.query.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.favoritesTitle,
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.savedSongsCount(stored.size),
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Background)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                placeholder = { Text(strings.searchFavoritesPlaceholder) },
                trailingIcon = if (query.isNotEmpty()) {{
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = strings.clearSearch)
                    }
                }} else null,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                )
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoriteSort.values().forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { viewModel.setSort(option) },
                        label = { Text(when (option) {
                            FavoriteSort.DATE -> strings.sortLatest
                            FavoriteSort.TITLE -> strings.sortTitle
                            FavoriteSort.ARTIST -> strings.sortArtist
                        }) }
                    )
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
                stored.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(strings.noFavoritesYet, color = MutedText)
                }
                items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.noMatchingFavorites, color = MutedText)
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    itemsIndexed(items, key = { _, item -> item.favorite.id }) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.available) {
                                    item.resolvedSourceUrl?.let {
                                        onSongClick(it, item.favorite.transpose, item.favorite.capo)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", color = MutedText, modifier = Modifier.width(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = if (item.available) PrimaryText else MutedText,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    buildString {
                                        append(item.artist)
                                        if (item.available) append(" • ${item.source}") else append(" • ${strings.unavailable}")
                                        if (item.substituted) append(" ${strings.alternateVersion}")
                                    },
                                    color = MutedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                val removed = item.favorite
                                viewModel.remove(removed)
                                scope.launch {
                                    val result = snackbar.showSnackbar(strings.removedFromFavorites, strings.undo)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(removed)
                                }
                            }) {
                                Icon(Icons.Filled.Favorite, strings.removeFromFavorites, tint = ActiveHeart)
                            }
                        }
                        HorizontalDivider(color = Border)
                    }
                }
            }
        }
    }
}
