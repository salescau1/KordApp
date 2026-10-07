package com.chordapp.ui.screens.setlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.SetlistSort
import com.chordapp.viewmodels.SetlistsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetlistsScreen(
    viewModel: SetlistsViewModel,
    onSetlistClick: (Long) -> Unit
) {
    val setlists by viewModel.setlists.collectAsState()
    val query by viewModel.queryText.collectAsState()
    val sort by viewModel.sortMode.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("SETLISTS", color = PrimaryText) },
                actions = {
                    Box {
                        IconButton(onClick = { showSort = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Urutkan setlist", tint = AccentGold)
                        }
                        DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                            DropdownMenuItem(
                                text = { Text("Terbaru") },
                                onClick = { viewModel.setSort(SetlistSort.UPDATED); showSort = false },
                                leadingIcon = { RadioButton(sort == SetlistSort.UPDATED, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Nama A–Z") },
                                onClick = { viewModel.setSort(SetlistSort.NAME); showSort = false },
                                leadingIcon = { RadioButton(sort == SetlistSort.NAME, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AccentGold,
                contentColor = Background
            ) { Icon(Icons.Filled.Add, contentDescription = "Buat setlist") }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Background),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                placeholder = { Text("Cari setlist") },
                trailingIcon = if (query.isNotEmpty()) {{
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian")
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
                    Text(
                        if (query.isBlank()) "Belum ada setlist." else "Setlist tidak ditemukan.",
                        color = MutedText
                    )
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
            title = { Text("Setlist baru") },
            text = { TextField(newName, { newName = it }, singleLine = true, placeholder = { Text("Nama setlist") }) },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    viewModel.createSetlist(newName)
                    newName = ""
                    showCreateDialog = false
                }) { Text("Buat") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Batal") } }
        )
    }
}
