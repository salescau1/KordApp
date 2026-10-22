package com.chordapp.ui.screens.setlists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chordapp.localization.LocalAppStrings
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.SetlistSort
import com.chordapp.viewmodels.SetlistsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetlistsScreen(
    viewModel: SetlistsViewModel,
    onSetlistClick: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    val setlists by viewModel.setlists.collectAsState()
    val query by viewModel.queryText.collectAsState()
    val sort by viewModel.sortMode.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        text = strings.setlistsTitle,
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.setlistsSubtitle,
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.clickable { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = RaisedSurface,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = strings.createSetlist,
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Box {
                        Surface(
                            modifier = Modifier.clickable { showSort = true },
                            shape = RoundedCornerShape(12.dp),
                            color = RaisedSurface,
                            border = BorderStroke(1.dp, Border)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = strings.sortSetlists,
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                            DropdownMenuItem(
                                text = { Text(strings.sortLatest) },
                                onClick = { viewModel.setSort(SetlistSort.UPDATED); showSort = false },
                                leadingIcon = { RadioButton(sort == SetlistSort.UPDATED, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.sortAZ) },
                                onClick = { viewModel.setSort(SetlistSort.NAME); showSort = false },
                                leadingIcon = { RadioButton(sort == SetlistSort.NAME, null) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AccentGold,
                contentColor = Background,
                modifier = Modifier.padding(bottom = 80.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = strings.createSetlist) }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Background)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                placeholder = { Text(strings.searchSetlists) },
                shape = RoundedCornerShape(18.dp),
                trailingIcon = if (query.isNotEmpty()) {{
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = strings.clearSearch)
                    }
                }} else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = Border,
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText
                )
            )

            if (setlists.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            if (query.isBlank()) strings.noSetlistsYet else strings.noSetlistsFound,
                            color = MutedText
                        )
                        if (query.isBlank()) {
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Background)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.createSetlist)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(setlists, key = { it.id }) { setlist ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                                .clickable { onSetlistClick(setlist.id) },
                            colors = CardDefaults.cardColors(containerColor = CardSurface)
                        ) {
                            Text(
                                setlist.name,
                                color = PrimaryText,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.newSetlist) },
            text = { TextField(newName, { newName = it }, singleLine = true, placeholder = { Text(strings.setlistName) }) },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    viewModel.createSetlist(newName)
                    newName = ""
                    showCreateDialog = false
                }) { Text(strings.create) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancel) } }
        )
    }
}
