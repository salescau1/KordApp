package com.chordapp.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.chordapp.localization.AppLanguageManager
import com.chordapp.p2p.BandSessionManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.R
import com.chordapp.charts.ChartPlatform
import com.chordapp.ui.components.SourceBadge
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.ActiveHeart
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedGold
import com.chordapp.ui.theme.MutedText
import com.chordapp.ui.theme.PrimaryText
import com.chordapp.viewmodels.ChartRowUi
import com.chordapp.viewmodels.ChartState
import com.chordapp.viewmodels.HomeCatalogStats
import com.chordapp.viewmodels.HomeViewModel
import com.chordapp.viewmodels.RecentSongUi
import java.util.Locale

/**
 * Offline-first Home with a real online Top 20 section.
 *
 * Pinned (spec 1 §2A): ad slot -> brand/search -> recently played -> chart header.
 * Only the chart list scrolls. Chart rows come from a real no-key source (Spotify,
 * Indonesia, via kworb mirror); nothing here is ever fabricated — offline/empty is a
 * truthful state, and rows the local catalog doesn't have are shown as unavailable.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    expanded: Boolean,
    onSearchClick: () -> Unit,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit
) {
    val recent by viewModel.recent.collectAsState()
    val catalogStats by viewModel.catalogStats.collectAsState()
    val chart by viewModel.chart.collectAsState()
    val platform by viewModel.platform.collectAsState()

    val context = LocalContext.current
    var showPinEducationDialog by remember { mutableStateOf(false) }
    var pinEduInput by remember { mutableStateOf("") }
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
    val strings = remember(currentLanguage) { AppLanguageManager.strings(currentLanguage) }

    LaunchedEffect(Unit) {
        if (!BandSessionManager.isPinPromptDismissed(context) && BandSessionManager.serverPin.value.isBlank()) {
            showPinEducationDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (expanded) {
            ExpandedHome(
                recent = recent,
                catalogStats = catalogStats,
                chart = chart,
                platform = platform,
                onSearchClick = onSearchClick,
                onSongClick = onSongClick,
                onSelectPlatform = viewModel::selectPlatform,
                onRetry = viewModel::retry,
                onRefresh = viewModel::refresh,
                onToggleFavorite = viewModel::toggleFavorite,
                onDownloadSong = viewModel::downloadSong
            )
        } else {
            CompactHome(
                recent = recent.take(1),
                chart = chart,
                platform = platform,
                onSearchClick = onSearchClick,
                onSongClick = onSongClick,
                onSelectPlatform = viewModel::selectPlatform,
                onRetry = viewModel::retry,
                onRefresh = viewModel::refresh,
                onToggleFavorite = viewModel::toggleFavorite,
                onDownloadSong = viewModel::downloadSong
            )
        }

        if (showPinEducationDialog) {
            AlertDialog(
                onDismissRequest = {
                    BandSessionManager.setPinPromptDismissed(context, true)
                    showPinEducationDialog = false
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                        Text(
                            text = strings.pinSetupEduTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = strings.pinSetupEduDesc,
                            fontSize = 12.5.sp,
                            color = MutedText,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = pinEduInput,
                            onValueChange = { input ->
                                if (input.length <= 4) {
                                    pinEduInput = input.filter { it.isDigit() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("4 Digit PIN (misal: 1234)", color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AccentGold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = Border,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinEduInput.length == 4) {
                                BandSessionManager.setPin(pinEduInput)
                                BandSessionManager.setPinEnabled(true)
                                BandSessionManager.setPinPromptDismissed(context, true)
                                showPinEducationDialog = false
                                Toast.makeText(context, strings.pinSavedToast, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = pinEduInput.length == 4,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0C0E12)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(strings.savePinBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            BandSessionManager.setPinPromptDismissed(context, true)
                            showPinEducationDialog = false
                        }
                    ) {
                        Text(strings.skipPinBtn, color = MutedText)
                    }
                },
                containerColor = CardSurface
            )
        }
    }
}

@Composable
private fun CompactHome(
    recent: List<RecentSongUi>,
    chart: ChartState,
    platform: ChartPlatform,
    onSearchClick: () -> Unit,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit,
    onSelectPlatform: (ChartPlatform) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (ChartRowUi) -> Unit,
    onDownloadSong: (ChartRowUi) -> Unit
) {
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val topPadding = if (headerHeightDp > 0.dp) headerHeightDp + 6.dp else 216.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Only the chart area scrolls across the full background
        ChartBody(
            modifier = Modifier.fillMaxSize(),
            state = chart,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 96.dp),
            onRetry = onRetry,
            onRefresh = onRefresh,
            onSongClick = onSongClick,
            onToggleFavorite = onToggleFavorite,
            onDownloadSong = onDownloadSong
        )

        // Frosted floating header at top: blur effect when list scrolls underneath
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    headerHeightPx = coordinates.size.height
                }
                .background(
                    Brush.verticalGradient(
                        0.0f to Background,
                        0.80f to Background.copy(alpha = 0.96f),
                        0.95f to Background.copy(alpha = 0.85f),
                        1.0f to Color.Transparent
                    )
                )
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            TopAdSlot()

            // App Brand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kord",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "App",
                    color = AccentGold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Floating Search Pill (search)
            FloatingSearchBar(onSearchClick = onSearchClick)

            Spacer(modifier = Modifier.height(8.dp))

            // Floating Recently Played Pill
            FloatingRecentPill(recent = recent.firstOrNull(), onSongClick = onSongClick)

            Spacer(modifier = Modifier.height(8.dp))

            // (top 50 Spotify) (top 50 YouTube)
            ChartHeader(
                platform = platform,
                onSelectPlatform = onSelectPlatform
            )
        }
    }
}

@Composable
private fun ExpandedHome(
    recent: List<RecentSongUi>,
    catalogStats: HomeCatalogStats?,
    chart: ChartState,
    platform: ChartPlatform,
    onSearchClick: () -> Unit,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit,
    onSelectPlatform: (ChartPlatform) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (ChartRowUi) -> Unit,
    onDownloadSong: (ChartRowUi) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(336.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            TopAdSlot()
            HomeHeader(onSearchClick = onSearchClick)
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                RecentlyPlayedSection(
                    recent = recent.take(5),
                    onSongClick = onSongClick
                )
                Spacer(modifier = Modifier.height(20.dp))
                CatalogInfoSection(stats = catalogStats)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ChartHeader(platform = platform, onSelectPlatform = onSelectPlatform)
                Spacer(modifier = Modifier.height(8.dp))
            }
            ChartBody(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = chart,
                onRetry = onRetry,
                onRefresh = onRefresh,
                onSongClick = onSongClick,
                onToggleFavorite = onToggleFavorite,
                onDownloadSong = onDownloadSong
            )
        }
    }
}

@Composable
private fun CatalogInfoSection(stats: HomeCatalogStats?) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    Text(
        text = strings.offlineDatabase,
        color = PrimaryText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, shape)
            .border(1.dp, Border, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CatalogInfoRow(strings.artists, stats?.artists?.toString() ?: strings.calculating)
        CatalogInfoRow(strings.songs, stats?.songs?.toString() ?: strings.calculating)
        CatalogInfoRow(strings.size, stats?.catalogBytes?.let { formatBytes(it, strings.calculating) } ?: strings.calculating)
    }
}

@Composable
private fun CatalogInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MutedText, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatBytes(bytes: Long, calculatingText: String): String = when {
    bytes <= 0L -> calculatingText
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

/** Reserved ad slot pinned at the very top, above the brand row. Renders nothing (zero height) until a real ad source exists. */
@Composable
private fun TopAdSlot(adContent: (@Composable () -> Unit)? = null) {
    adContent?.invoke()
}

@Composable
private fun HomeHeader(onSearchClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    ) {
        // App brand — wordmark sama seperti splash: "Kord" putih, "App" emas. Tanpa icon.
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kord",
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "App",
                color = AccentGold,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar. Tapping anywhere opens the dedicated Search screen.
        val searchShape = RoundedCornerShape(8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(CardSurface, searchShape)
                .border(1.dp, Border, searchShape)
                .clickable { onSearchClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val strings = com.chordapp.localization.LocalAppStrings.current
            Text(
                text = strings.searchPlaceholderHome,
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = strings.searchButton,
                color = AccentGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FloatingSearchBar(onSearchClick: () -> Unit) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    val searchShape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(CardSurface.copy(alpha = 0.92f), searchShape)
            .border(1.dp, Border, searchShape)
            .clickable { onSearchClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = AccentGold,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = strings.searchPlaceholderHome,
            color = MutedText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = strings.searchButton,
            color = AccentGold,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FloatingRecentPill(
    recent: RecentSongUi?,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val baseModifier = Modifier
        .fillMaxWidth()
        .background(CardSurface.copy(alpha = 0.92f), shape)
        .border(1.dp, Border, shape)
        .padding(horizontal = 14.dp, vertical = 8.dp)

    val clickableModifier = if (recent != null && recent.available) {
        baseModifier.clickable { onSongClick(recent.artistSlug, recent.titleSlug) }
    } else {
        baseModifier
    }

    val strings = com.chordapp.localization.LocalAppStrings.current
    Row(
        modifier = clickableModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.padding(end = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = strings.recentlyHeader1,
                color = MutedText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
            Text(
                text = strings.recentlyHeader2,
                color = AccentGold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(26.dp)
                .background(Border)
        )

        Spacer(modifier = Modifier.width(12.dp))

        if (recent != null) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recent.title,
                    color = if (recent.available) PrimaryText else MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (recent.available) "${recent.artist} • ${recent.source}" else "${recent.artist} • ${strings.unavailable}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (recent.available) {
                Text(
                    text = "›",
                    color = AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.nothingPlayedYet,
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = strings.songsPlayedAppearHere,
                    color = MutedText.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    recent: List<RecentSongUi>,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    Text(
        text = strings.recentlyPlayedHeader,
        color = PrimaryText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (recent.isEmpty()) {
        // Real history is empty until the user actually plays something.
        InfoCard(
            leading = "♪",
            title = strings.nothingPlayedYet,
            message = strings.songsPlayedAppearHere
        )
    } else {
        recent.forEach { song ->
            RecentSongRow(song = song, onSongClick = onSongClick)
        }
    }
}

/**
 * One recently played song. The whole row is tappable when the song is still
 * resolvable in the catalog; otherwise it stays visible but disabled (the song
 * may have been removed from the catalog after it was played).
 */
@Composable
private fun RecentSongRow(
    song: RecentSongUi,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    val shape = RoundedCornerShape(8.dp)
    val rowModifier = Modifier
        .fillMaxWidth()
        .background(CardSurface, shape)
        .border(1.dp, Border, shape)
        .padding(12.dp)
    val clickableModifier = if (song.available) {
        rowModifier.clickable { onSongClick(song.artistSlug, song.titleSlug) }
    } else {
        rowModifier
    }

    Row(
        modifier = clickableModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MutedGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                color = AccentGold,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (song.available) PrimaryText else MutedText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (song.available) "${song.artist} • ${song.source}" else "${song.artist} • ${strings.unavailable}",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (song.available) {
            Text(text = "›", color = AccentGold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Section header and source selector for the Top 20 list. */
@Composable
private fun ChartHeader(
    platform: ChartPlatform,
    onSelectPlatform: (ChartPlatform) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlatformButton(
            platform = ChartPlatform.SPOTIFY,
            selected = platform == ChartPlatform.SPOTIFY,
            modifier = Modifier.weight(1f),
            onClick = onSelectPlatform
        )
        PlatformButton(
            platform = ChartPlatform.YOUTUBE,
            selected = platform == ChartPlatform.YOUTUBE,
            modifier = Modifier.weight(1f),
            onClick = onSelectPlatform
        )
    }
}

@Composable
private fun PlatformButton(
    platform: ChartPlatform,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (ChartPlatform) -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .height(42.dp)
            .background(if (selected) MutedGold else CardSurface.copy(alpha = 0.92f), shape)
            .border(1.dp, if (selected) AccentGold else Border, shape)
            .clickable { onClick(platform) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TOP 50",
            color = if (selected) AccentGold else PrimaryText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Image(
            painter = painterResource(
                if (platform == ChartPlatform.SPOTIFY) {
                    R.drawable.spotify_mark
                } else {
                    R.drawable.youtube_logo
                }
            ),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = platform.label,
            color = if (selected) AccentGold else PrimaryText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * The scrollable chart body. [modifier] carries the caller's column weight so this
 * helper never needs ColumnScope; only this area scrolls (header stays pinned).
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ChartBody(
    modifier: Modifier,
    state: ChartState,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit,
    onToggleFavorite: (ChartRowUi) -> Unit,
    onDownloadSong: (ChartRowUi) -> Unit = {}
) {
    when (state) {
        ChartState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        }

        ChartState.NoData -> {
            val strings = com.chordapp.localization.LocalAppStrings.current
            Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                InfoCard(
                    leading = "—",
                    title = strings.chartsNeedInternetTitle,
                    message = strings.chartsNeedInternetDesc
                )
                Text(
                    text = strings.retry,
                    color = AccentGold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 12.dp)
                        .clickable { onRetry() }
                )
            }
        }

        is ChartState.Loaded -> {
            val rows = state.rows
            val items = remember(rows) {
                buildList {
                    rows.forEachIndexed { index, row ->
                        add(RowItem(row))
                        // Native-ad slot after rank #3 (collapses to nothing while empty).
                        if (index == 2) add(AdItem)
                    }
                }
            }
            val pullState = rememberPullRefreshState(
                refreshing = state.isRefreshing,
                onRefresh = onRefresh
            )
            Box(modifier = modifier.fillMaxSize().pullRefresh(pullState)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items) { item ->
                        when (item) {
                            is RowItem -> ChartRow(
                                row = item.row,
                                onSongClick = onSongClick,
                                onToggleFavorite = onToggleFavorite,
                                onDownloadSong = onDownloadSong
                            )
                            AdItem -> NativeAdSlot()
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = state.isRefreshing,
                    state = pullState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentPadding.calculateTopPadding()),
                    backgroundColor = CardSurface,
                    contentColor = AccentGold
                )
            }
        }
    }
}

/** Reserved native-ad slot after rank #3. Renders nothing (zero height) while empty. */
@Composable
private fun NativeAdSlot(adContent: (@Composable () -> Unit)? = null) {
    adContent?.invoke()
}

/**
 * A single chart row. Left: rank (#1–#3 in accent gold). Middle: title, artist, and
 * either the real chord-source badges of the local song or a muted "Chords not
 * available". Right: play affordance. The whole card opens the version picker only
 * when the song exists locally.
 */
@Composable
private fun ChartRow(
    row: ChartRowUi,
    onSongClick: (artistSlug: String, titleSlug: String) -> Unit,
    onToggleFavorite: (ChartRowUi) -> Unit,
    onDownloadSong: (ChartRowUi) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    val shape = RoundedCornerShape(12.dp)

    val canOpen = row.available && row.artistSlug != null && row.titleSlug != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (canOpen) {
                    onSongClick(row.artistSlug!!, row.titleSlug!!)
                } else {
                    onDownloadSong(row)
                }
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.rank.toString(),
                color = if (row.rank <= 3) AccentGold else MutedText,
                style = if (row.rank <= 3) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(34.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    color = if (row.available) PrimaryText else MutedText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = row.artist,
                        color = MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (row.available) {
                        row.sources.take(2).forEach { source ->
                            SourceBadge(source = source, compact = true)
                        }
                    } else {
                        Text(
                            text = strings.notAvailable,
                            color = MutedText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            if (row.available) {
                IconButton(
                    onClick = { onToggleFavorite(row) },
                    enabled = row.favoriteSourceUrl != null,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (row.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (row.isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                        tint = when {
                            row.isFavorite -> ActiveHeart
                            row.favoriteSourceUrl != null -> PrimaryText
                            else -> MutedText
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { onDownloadSong(row) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Unduh Chord",
                        tint = AccentGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/** Truthful empty/unavailable placeholder card used when a section has no offline data. */
@Composable
private fun InfoCard(
    leading: String,
    title: String,
    message: String
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, shape)
            .border(1.dp, Border, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MutedGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leading,
                color = AccentGold,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private sealed interface ChartListItem
private data class RowItem(val row: ChartRowUi) : ChartListItem
private data object AdItem : ChartListItem
