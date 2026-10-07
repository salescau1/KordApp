package com.chordapp.ui.screens.setlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chordapp.localization.LocalAppStrings
import com.chordapp.ui.components.AppBackButton
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.SetlistTrackUi
import com.chordapp.viewmodels.SetlistTracklistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetlistTracklistScreen(
    setlistId: Long,
    viewModel: SetlistTracklistViewModel,
    onBackClick: () -> Unit,
    onSongClick: (sourceUrl: String, transpose: Int, capo: Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val items by viewModel.items.collectAsState()
    val setlist by viewModel.setlist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberSaveable(setlistId, saver = LazyListState.Saver) { LazyListState() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(setlistId) { viewModel.loadSetlist(setlistId) }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TracklistTopBar(
                title = setlist?.name ?: strings.navSetlists,
                menuExpanded = menuExpanded,
                onMenuChange = { menuExpanded = it },
                onBackClick = onBackClick,
                onRename = { showRename = true },
                onDelete = { showDelete = true }
            )
        }
    ) { padding ->
        TracklistBody(
            modifier = Modifier.padding(padding),
            items = items,
            isLoading = isLoading,
            listState = listState,
            onSongClick = onSongClick,
            onMove = viewModel::move,
            onDelete = { item ->
                viewModel.deleteItem(item.stored)
                scope.launch {
                    val result = snackbar.showSnackbar(strings.songRemoved, strings.undo)
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreItem(item.stored)
                }
            }
        )
    }
    if (showRename) {
        NameDialog(
            title = strings.renameSetlist,
            initial = setlist?.name.orEmpty(),
            onDismiss = { showRename = false },
            onConfirm = {
                viewModel.renameSetlist(it)
                showRename = false
            }
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(strings.deleteSetlistConfirmTitle) },
            text = { Text(strings.deleteSetlistConfirmMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteSetlist(onBackClick)
                }) { Text(strings.delete, color = ActiveHeart) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TextField(value, { value = it }, singleLine = true) },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value) }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TracklistTopBar(
    title: String,
    menuExpanded: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    TopAppBar(
        title = { Text(title, color = PrimaryText) },
        navigationIcon = { AppBackButton(onBackClick) },
        actions = {
            IconButton(onClick = { onMenuChange(true) }) {
                Icon(Icons.Filled.MoreVert, strings.setlistOptions, tint = PrimaryText)
            }
            DropdownMenu(menuExpanded, { onMenuChange(false) }) {
                DropdownMenuItem(text = { Text(strings.renameSetlist) }, onClick = {
                    onMenuChange(false)
                    onRename()
                })
                DropdownMenuItem(text = { Text(strings.delete, color = ActiveHeart) }, onClick = {
                    onMenuChange(false)
                    onDelete()
                })
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
    )
}

@Composable
private fun TracklistBody(
    modifier: Modifier,
    items: List<SetlistTrackUi>,
    isLoading: Boolean,
    listState: LazyListState,
    onSongClick: (String, Int, Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: (SetlistTrackUi) -> Unit
) {
    val strings = LocalAppStrings.current
    when {
        isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentGold)
        }
        items.isEmpty() -> Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(strings.noSongsInSetlist, color = MutedText)
        }
        else -> LazyColumn(modifier.fillMaxSize(), state = listState) {
            itemsIndexed(items, key = { _, item -> item.stored.id }) { index, item ->
                TracklistRow(index, item, items.size, onSongClick, onMove, onDelete)
                HorizontalDivider(color = Border)
            }
        }
    }
}

@Composable
private fun TracklistRow(
    index: Int,
    item: SetlistTrackUi,
    itemCount: Int,
    onSongClick: (String, Int, Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: (SetlistTrackUi) -> Unit
) {
    val strings = LocalAppStrings.current
    var dragTotal by remember { mutableStateOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
            .clickable(enabled = item.available) {
                item.resolvedSourceUrl?.let { url ->
                    onSongClick(url, item.stored.transpose, item.stored.capo)
                }
            }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${index + 1}", color = MutedText, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = if (item.available) PrimaryText else MutedText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(item.artist)
                    append(if (item.available) " • ${item.source}" else " • ${strings.unavailable}")
                    if (item.substituted) append(" ${strings.alternateVersion}")
                    if (item.stored.transpose != 0) append(" • Transpose ${item.stored.transpose}")
                    if (item.stored.capo != 0) append(" • Capo ${item.stored.capo}")
                },
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { onDelete(item) }) {
            Icon(Icons.Filled.Delete, strings.deleteSong, tint = ActiveHeart)
        }
        Icon(
            Icons.Filled.DragHandle,
            strings.dragToReorder,
            tint = MutedText,
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp)
                .pointerInput(index, itemCount) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            val target = when {
                                dragTotal < -48f -> index - 1
                                dragTotal > 48f -> index + 1
                                else -> index
                            }.coerceIn(0, itemCount - 1)
                            onMove(index, target)
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            dragTotal += amount.y
                        }
                    )
                }
        )
    }
}
