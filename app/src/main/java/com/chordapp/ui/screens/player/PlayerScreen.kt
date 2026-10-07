package com.chordapp.ui.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chordapp.player.ChordFingeringDb
import com.chordapp.player.ChordParser
import com.chordapp.player.ChordSheet
import com.chordapp.player.ScrollAnchor
import com.chordapp.player.SheetLine
import com.chordapp.player.SheetPaginator
import com.chordapp.player.Transposer
import com.chordapp.ui.components.AppBackButton
import com.chordapp.ui.theme.*
import com.chordapp.viewmodels.PlayerFavoriteEvent
import com.chordapp.viewmodels.PlayerSetlistEvent
import com.chordapp.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch

/** Maps a tap offset inside the chord line back to the chord under it. */
internal data class ChordSpan(val start: Int, val length: Int, val symbol: String)

/**
 * Renders one chord row exactly from its stored delimiter and token columns:
 * bracketed rows keep brackets; plain rows remain plain. Transposition may
 * widen a symbol, so subsequent chords retain their source column when
 * possible and otherwise receive at least one separating space.
 */
internal fun buildChordLine(chords: SheetLine.Chords, delta: Int): Pair<String, List<ChordSpan>> {
    val sb = StringBuilder()
    val spans = mutableListOf<ChordSpan>()
    for (token in chords.tokens) {
        val isChord = chords.bracketed || ChordParser.isChordSymbol(token.symbol)
        val transposed = Transposer.transpose(token.symbol, delta)
        val bracket = chords.bracketed && !token.symbol.startsWith("-") && !token.symbol.startsWith("~")
        val rendered = if (bracket) "[$transposed]" else transposed
        val target = if (sb.isEmpty()) token.column else maxOf(token.column, sb.length + 1)
        while (sb.length < target) sb.append(' ')
        val symStart = sb.length + (if (bracket) 1 else 0)
        sb.append(rendered)
        if (isChord) spans.add(ChordSpan(symStart, transposed.length, transposed))
    }
    return sb.toString() to spans
}

private fun chordAt(offset: Int, spans: List<ChordSpan>): String? =
    spans.lastOrNull { offset >= it.start && offset < it.start + it.length }?.symbol

internal fun autoScrollDeltaPx(elapsedNanos: Long, linesPerSecond: Float, lineHeightPx: Float): Float {
    if (elapsedNanos <= 0L || linesPerSecond <= 0f || lineHeightPx <= 0f) return 0f
    return elapsedNanos / 1_000_000_000f * linesPerSecond * lineHeightPx
}

internal fun nextPage(currentPage: Int, pageCount: Int): Int? =
    (currentPage + 1).takeIf { it < pageCount }

private fun speedLabel(speed: Float): String {
    val v = if (speed % 1f == 0f) speed.toInt().toString() else String.format("%.1f", speed)
    return "$v×"
}

/**
 * Renders one line of a locked page. Every line occupies a fixed slot so the
 * pages produced by [SheetPaginator] always fit their viewport exactly.
 */
@Composable
private fun PageLine(
    line: SheetLine,
    slotHeight: Dp,
    fontSize: Float,
    transposeDelta: Int,
    requestDiagram: (String) -> Unit
) {
    when (line) {
        is SheetLine.Blank -> Spacer(modifier = Modifier.height(slotHeight))
        is SheetLine.Section -> Box(
            modifier = Modifier.fillMaxWidth().height(slotHeight * 2f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = line.text,
                color = AccentGold,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp
            )
        }
        is SheetLine.Lyric -> Box(modifier = Modifier.fillMaxWidth().height(slotHeight)) {
            Text(
                text = line.text,
                color = PrimaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp
            )
        }
        is SheetLine.Chords -> {
            if (line.isBarLine) {
                Box(modifier = Modifier.fillMaxWidth().height(slotHeight), contentAlignment = Alignment.CenterStart) {
                    BarLineFlow(
                        barLine = line,
                        fontSize = fontSize,
                        transposeDelta = transposeDelta,
                        requestDiagram = requestDiagram
                    )
                }
            } else {
                val (text, spans) = buildChordLine(line, transposeDelta)
                val mono = TextStyle(
                    color = CtText,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp
                )
                Box(modifier = Modifier.fillMaxWidth().height(slotHeight)) {
                    ClickableText(
                        text = AnnotatedString(text),
                        style = mono,
                        onClick = { offset -> chordAt(offset, spans)?.let { requestDiagram(ChordParser.cleanChordSymbol(it)) } }
                    )
                }
            }
        }
    }
}

/**
 * Vertical pagination indicator rail along the right edge of the sheet,
 * positioned at the exact same location as [AutoScrollRail].
 * Displays vertical dots/pills top-to-bottom for the pages.
 */
@Composable
private fun BoxScope.PageFlipRail(
    currentPage: Int,
    pageCount: Int,
    onPageSelect: (Int) -> Unit
) {
    if (pageCount <= 1) return
    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(PaddingValues(start = 0.dp, top = 8.dp, end = 2.dp, bottom = 8.dp))
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardSurface.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (pageCount > 10) 4.dp else 6.dp)
            ) {
                for (i in 0 until pageCount) {
                    val isSelected = i == currentPage
                    val dotHeight by animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 8.dp,
                        animationSpec = tween(durationMillis = 200),
                        label = "dotHeight"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) AccentGold else Border,
                        animationSpec = tween(durationMillis = 200),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPageSelect(i) }
                            .padding(vertical = 2.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = dotHeight)
                                .background(
                                    color = dotColor,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
        Text(
            text = "${currentPage + 1}/$pageCount",
            color = AccentGold,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Page Flip content: the sheet sliced into whole-viewport slides via
 * [SheetPaginator]. One slide cannot be scrolled vertically — every line sits in
 * a fixed slot grid — and slides are turned by swiping up/down
 * ([VerticalPager] snaps and animates the turn) or tapping the page. A vertical
 * pagination rail along the right edge displays dots top-to-bottom matching
 * the auto-scroll slider position.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedSheet(
    lines: List<SheetLine>,
    fontSize: Float,
    transposeDelta: Int,
    contentKey: String,
    requestDiagram: (String) -> Unit
) {
    val density = LocalDensity.current
    // Slot height in Dp. Ensure it never undershoots minimum monospace line height (1.6x font size).
    val slotDp = with(density) {
        val spDp = (fontSize * 1.6f).sp.toDp()
        val minDp = (fontSize * 1.6f).dp
        if (spDp > minDp) spDp else minDp
    }
    val slotPx = with(density) { slotDp.toPx() }

    var areaHeightPx by remember { mutableStateOf(0) }
    var areaWidthPx by remember { mutableStateOf(0) }

    // Column has padding(top = 8.dp, bottom = 8.dp).
    // Reserve an extra 24.dp safety buffer so pages are guaranteed never to exceed the viewport.
    val verticalPaddingPx = with(density) { 40.dp.toPx() }
    val usableHeightPx = (areaHeightPx - verticalPaddingPx).coerceAtLeast(0f)
    // Coerce capacity and subtract 1 slot headroom so page content strictly stays within the screen.
    val capacity = if (usableHeightPx > 0f && slotPx > 0f) {
        ((usableHeightPx / slotPx).toInt() - 1).coerceAtLeast(1)
    } else 1

    // Horizontal padding: 16.dp start, 36.dp end (for pagination rail).
    // Monospace char width is ~0.6 * fontSize.
    val horizontalPaddingPx = with(density) { 52.dp.toPx() }
    val usableWidthPx = (areaWidthPx - horizontalPaddingPx).coerceAtLeast(0f)
    val charWidthPx = with(density) { (fontSize * 0.6f).sp.toPx() }
    val charsPerLine = if (charWidthPx > 0f && usableWidthPx > 0f) {
        (usableWidthPx / charWidthPx).toInt().coerceIn(20, 120)
    } else {
        40
    }

    val pages = remember(lines, capacity, charsPerLine) {
        SheetPaginator.paginate(lines, capacity, charsPerLine)
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // A different song/version restarts on page one; font/size changes keep place.
    LaunchedEffect(contentKey) {
        if (pagerState.currentPage != 0) pagerState.scrollToPage(0)
    }
    // Larger font means fewer pages; never let currentPage point past the end.
    LaunchedEffect(pages.size) {
        if (pagerState.currentPage >= pages.size && pages.isNotEmpty()) {
            pagerState.scrollToPage((pages.size - 1).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                areaHeightPx = it.height
                areaWidthPx = it.width
            }
    ) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@VerticalPager
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        nextPage(pagerState.currentPage, pages.size)?.let { next ->
                            scope.launch { pagerState.animateScrollToPage(next) }
                        }
                    }
                    .padding(
                        start = 16.dp,
                        end = if (pages.size > 1) 36.dp else 16.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    )
            ) {
                page.forEachIndexed { idx, line ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when (line) {
                            is SheetLine.Blank -> {
                                Spacer(modifier = Modifier.height((fontSize * 1.2).dp))
                            }
                            is SheetLine.Section -> {
                                Text(
                                    text = line.text,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp)
                                )
                            }
                            is SheetLine.Lyric -> {
                                val prev = page.getOrNull(idx - 1)
                                val pairedWithPrev = prev is SheetLine.Chords && !prev.isBarLine
                                if (!pairedWithPrev) {
                                    ChordLyricFlow(
                                        chordLine = null,
                                        lyric = line.text,
                                        fontSize = fontSize,
                                        transposeDelta = transposeDelta,
                                        requestDiagram = requestDiagram
                                    )
                                }
                            }
                            is SheetLine.Chords -> {
                                if (line.isBarLine) {
                                    BarLineFlow(
                                        barLine = line,
                                        fontSize = fontSize,
                                        transposeDelta = transposeDelta,
                                        requestDiagram = requestDiagram
                                    )
                                } else {
                                    ChordLyricFlow(
                                        chordLine = line,
                                        lyric = (page.getOrNull(idx + 1) as? SheetLine.Lyric)?.text,
                                        fontSize = fontSize,
                                        transposeDelta = transposeDelta,
                                        requestDiagram = requestDiagram
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        PageFlipRail(
            currentPage = pagerState.currentPage,
            pageCount = pages.size,
            onPageSelect = { page ->
                scope.launch { pagerState.animateScrollToPage(page) }
            }
        )
    }
}

/**
 * Vertical auto-scroll speed slider, shown only while auto-scroll is running.
 * Appears along the right edge of the sheet: drag/tap higher = faster, lower =
 * slower. A custom Canvas slider keeps the drag axis unambiguous (no rotation).
 */
@Composable
private fun BoxScope.AutoScrollRail(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val minSpeed = 0.5f
    val maxSpeed = 4f
    var trackPx by remember { mutableStateOf(1f) }
    val railDp = 200.dp

    fun valueAt(yPx: Float): Float {
        val h = trackPx
        if (h <= 0f) return speed
        val f = (yPx.coerceIn(0f, h)) / h // 0 top .. 1 bottom
        return maxSpeed - f * (maxSpeed - minSpeed)
    }

    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(PaddingValues(start = 0.dp, top = 8.dp, end = 2.dp, bottom = 8.dp))
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(railDp)
                .onSizeChanged { trackPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> onSpeedChange(valueAt(offset.y)) }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        onSpeedChange(valueAt(change.position.y))
                    }
                }
        ) {
            val frac = ((speed - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0f, 1f)
            val clampedThumbY = ((1f - frac) * trackPx).coerceIn(0f, trackPx)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                val left = (size.width - stroke) / 2f
                // Background inactive track (Border gray)
                drawRoundRect(
                    color = Border,
                    topLeft = Offset(left, 0f),
                    size = Size(stroke, size.height),
                    cornerRadius = CornerRadius(stroke / 2f)
                )
                // Active track (AccentGold yellow) - Fills from clamped thumb down to bottom
                val filledHeight = size.height - clampedThumbY
                if (filledHeight > 0f) {
                    drawRoundRect(
                        color = AccentGold,
                        topLeft = Offset(left, clampedThumbY),
                        size = Size(stroke, filledHeight),
                        cornerRadius = CornerRadius(stroke / 2f)
                    )
                }
                // Thumb circle with contrasting center dot
                val cy = clampedThumbY.coerceIn(stroke, size.height - stroke)
                drawCircle(AccentGold, radius = stroke * 1.25f, center = Offset(size.width / 2f, cy))
                drawCircle(CardSurface, radius = stroke * 0.45f, center = Offset(size.width / 2f, cy))
            }
        }
        Text(
            speedLabel(speed),
            color = AccentGold,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** One tap cell of the transpose stepper (1× reference size). */
@Composable
private fun SegmentButton(label: String, active: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                active -> AccentGold
                enabled -> PrimaryText
                else -> MutedText
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SegmentDividerV() {
    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Border))
}

/**
 * Transpose group at the 1× reference size (the smallest group in the control
 * band): the word "transpose" sits above an explicit `− reset +` row. The
 * reset cell is always visible and tapping it restores +0 — an explicit
 * button, not a merged tap-the-value reset (QA-2).
 */
@Composable
private fun TransposeGroup(
    delta: Int,
    onDown: () -> Unit,
    onUp: () -> Unit,
    onReset: () -> Unit
) {
    val active = delta != 0
    val shape = RoundedCornerShape(8.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "transpose",
            color = MutedText,
            style = MaterialTheme.typography.labelSmall
        )
        Row(
            modifier = Modifier
                .padding(top = 2.dp)
                .border(1.dp, if (active) AccentGold else Border, shape)
                .background(if (active) AccentGold.copy(alpha = 0.10f) else RaisedSurface, shape)
        ) {
            SegmentButton("−", onClick = onDown)
            SegmentDividerV()
            SegmentButton("reset", active = active, onClick = onReset)
            SegmentDividerV()
            SegmentButton("+", onClick = onUp)
        }
    }
}

/** Small bordered tap target used across the Player control bars. */
@Composable
private fun PillButton(
    label: String,
    active: Boolean = false,
    large: Boolean = false,
    modifier: Modifier = Modifier,
    contentColor: Color = PrimaryText,
    onClick: () -> Unit
) {
    val bg = if (active) AccentGold else RaisedSurface
    val tc = if (active) Background else contentColor
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, if (active) AccentGold else Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (large) 12.dp else 10.dp,
                vertical = if (large) 10.dp else 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = tc,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KeepScreenButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val background = if (enabled) AccentGold else CardSurface
    val foreground = if (enabled) Background else MutedText
    Column(
        modifier = Modifier
            .background(background, shape)
            .border(1.dp, if (enabled) AccentGold else Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Keep Screen", color = foreground, style = MaterialTheme.typography.labelSmall)
        Text(
            if (enabled) "ON" else "OFF",
            color = foreground,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    sourceUrlHash: String,
    initialTranspose: Int = 0,
    initialCapo: Int = 0,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val song by viewModel.song.collectAsState()
    val sheet by viewModel.sheet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val transposeDelta by viewModel.transposeDelta.collectAsState()
    val capo by viewModel.capo.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val isPageFlip by viewModel.isPageFlip.collectAsState()
    val autoScrollSpeed by viewModel.autoScrollSpeed.collectAsState()
    val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
    val availableVersions by viewModel.availableVersions.collectAsState()
    val setlists by viewModel.setlists.collectAsState()
    val isAddingToSetlist by viewModel.isAddingToSetlist.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isUpdatingFavorite by viewModel.isUpdatingFavorite.collectAsState()

    val strings = com.chordapp.localization.LocalAppStrings.current
    var showSetlistPicker by remember { mutableStateOf(false) }
    var showCreateSetlist by remember { mutableStateOf(false) }
    var newSetlistName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, strings) {
        viewModel.setlistEvents.collect { event ->
            val message = when (event) {
                is PlayerSetlistEvent.Added -> strings.addedToSetlist(event.setlistName)
                is PlayerSetlistEvent.AlreadyPresent -> strings.alreadyInSetlist(event.setlistName)
                PlayerSetlistEvent.Error -> strings.failedToAddToSetlist
            }
            snackbarHostState.showSnackbar(message)
        }
    }
    LaunchedEffect(viewModel, strings) {
        viewModel.favoriteEvents.collect { event ->
            val message = when (event) {
                is PlayerFavoriteEvent.Changed -> if (event.isFavorite) strings.addedToFavorites else strings.removedFromFavorites
                PlayerFavoriteEvent.Error -> strings.failedToUpdateFavorites
            }
            snackbarHostState.showSnackbar(message)
        }
    }
    // Keep the screen awake only while this Player is visible and the tile is ON.
    val view = LocalView.current
    DisposableEffect(view, keepScreenAwake) {
        view.keepScreenOn = keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(sourceUrlHash, initialTranspose, initialCapo) {
        viewModel.loadSong(sourceUrlHash, initialTranspose, initialCapo)
    }

    var showVersionMenu by remember { mutableStateOf(false) }
    var diagramSymbol by remember { mutableStateOf<String?>(null) }
    var fingeringDb by remember { mutableStateOf<ChordFingeringDb?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val requestDiagram: (String) -> Unit = { symbol ->
        diagramSymbol = symbol
        if (fingeringDb == null) {
            scope.launch {
                if (fingeringDb == null) {
                    val loaded = ChordDiagramCache.get(context)
                    fingeringDb = loaded
                }
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentGold)
        }
        return
    }

    val currentSong = song ?: return
    val currentSheet = sheet
    val hasContent = currentSheet != null && currentSheet.lines.isNotEmpty()

    val listState = rememberLazyListState()
    val itemCount = if (hasContent) currentSheet!!.lines.size + 1 else 0 // +1 bottom spacer

    // Re-anchor to the same section when a version switch swaps the content.
    var lastUrl by remember { mutableStateOf<String?>(null) }
    var lastLines by remember { mutableStateOf<List<SheetLine>>(emptyList()) }
    var lastIndex by remember { mutableStateOf(0) }

    LaunchedEffect(currentSong.sourceUrl) {
        val newLines = sheet?.lines ?: emptyList()
        if (lastUrl != null && lastUrl != currentSong.sourceUrl && newLines.isNotEmpty() && !isPageFlip) {
            val target = ScrollAnchor.anchorIndex(lastLines, lastIndex, newLines)
            listState.scrollToItem(target.coerceIn(0, itemCount - 1))
        }
        lastUrl = currentSong.sourceUrl
        lastLines = newLines
    }
    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { lastIndex = it }
    }

    // Frame-based continuous scrolling. Speed changes are read from updated state,
    // so changing the rail does not restart or jump the sheet.
    val currentAutoScrollSpeed by rememberUpdatedState(autoScrollSpeed)
    val currentPageFlip by rememberUpdatedState(isPageFlip)
    val lineHeightPx = with(LocalDensity.current) { (fontSize * 1.5f).sp.toPx() }
    LaunchedEffect(autoScrollSpeed > 0f, isPageFlip, currentSheet?.lines?.size, lineHeightPx) {
        if (autoScrollSpeed <= 0f || isPageFlip) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        var remainder = 0f
        while (currentAutoScrollSpeed > 0f && !currentPageFlip) {
            val frame = withFrameNanos { it }
            val rawDelta = autoScrollDeltaPx(
                elapsedNanos = frame - previousFrame,
                linesPerSecond = currentAutoScrollSpeed,
                lineHeightPx = lineHeightPx
            ) + remainder
            previousFrame = frame
            if (rawDelta <= 0f) continue
            val toScroll = if (rawDelta < 0.5f) {
                remainder = rawDelta
                continue
            } else {
                remainder = 0f
                rawDelta
            }
            val consumed = listState.scrollBy(toScroll)
            if (consumed < toScroll * 0.5f && !listState.canScrollForward) {
                viewModel.pauseAutoScroll()
                break
            }
        }
    }

    val effectiveKey = viewModel.effectiveKey()
    val versionIndex = availableVersions.indexOfFirst { it.sourceUrl == currentSong.sourceUrl }
    val versionLabel = if (versionIndex >= 0) "v${versionIndex + 1}/${availableVersions.size} ▾" else null

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar - Centered Title between Back and (Favorite + Setlist)
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        currentSong.title,
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        currentSong.artist,
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = { AppBackButton(onBackClick) },
            actions = {
                IconButton(
                    enabled = !isUpdatingFavorite,
                    onClick = { viewModel.toggleFavorite() }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                        tint = if (isFavorite) ActiveHeart else PrimaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(36.dp)
                        .background(AccentGold, CircleShape)
                        .clickable(enabled = !isAddingToSetlist) { showSetlistPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = strings.addSongToSetlist,
                        tint = Background,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CardSurface)
        )

        // Meta strip: real metadata only (never a placeholder dash); version chip right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            effectiveKey?.takeIf { it.isNotBlank() }?.let { Text("Key: $it", color = AccentGold, style = MaterialTheme.typography.bodySmall) }
            if (capo > 0) Text("Capo: $capo", color = AccentGold, style = MaterialTheme.typography.bodySmall)
            currentSong.difficulty?.let { Text(it, color = MutedText, style = MaterialTheme.typography.bodySmall) }
            Spacer(modifier = Modifier.weight(1f))
            if (versionLabel != null) {
                Box {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .clickable { showVersionMenu = true }
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            versionLabel,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    DropdownMenu(expanded = showVersionMenu, onDismissRequest = { showVersionMenu = false }) {
                        availableVersions.forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        v.versionName ?: v.source,
                                        color = if (v.sourceUrl == currentSong.sourceUrl) AccentGold else PrimaryText
                                    )
                                },
                                onClick = {
                                    showVersionMenu = false
                                    if (v.sourceUrl != currentSong.sourceUrl) viewModel.switchVersion(v.sourceUrl)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sheet. One Box owns the whole reading area so the tapped-chord diagram
        // card can float in its top-right corner (QA-5b), over whichever mode is
        // active, instead of blocking the sheet from a centered dialog.
        val lines = if (hasContent) currentSheet!!.lines else emptyList()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                !hasContent -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.noSheetContent,
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                isPageFlip -> {
                    // Discrete full-screen "slides": vertical scroll is locked and
                    // pages change by swiping left/right; dots row below the sheet.
                    PagedSheet(
                        lines = lines,
                        fontSize = fontSize,
                        transposeDelta = transposeDelta,
                        contentKey = currentSong.sourceUrl,
                        requestDiagram = requestDiagram
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(
                            items = lines,
                            key = { idx, _ -> idx },
                            contentType = { _, line ->
                                when (line) {
                                    is SheetLine.Blank -> 0
                                    is SheetLine.Section -> 1
                                    is SheetLine.Lyric -> 2
                                    is SheetLine.Chords -> if (line.isBarLine) 3 else 4
                                }
                            }
                        ) { idx, line ->
                            // Chord+lyric wrap sebagai satu blok: chord tetap di atas
                            // katanya dan turun bersama katanya ketika baris tidak muat.
                            // Index tetap 1:1 dengan `lines` supaya anchor scroll saat
                            // ganti versi tetap valid.
                            Box(modifier = Modifier.fillMaxWidth()) {
                                when (line) {
                                    is SheetLine.Blank -> {
                                        Spacer(modifier = Modifier.height((fontSize * 1.5).dp))
                                    }
                                    is SheetLine.Section -> {
                                        Text(
                                            text = line.text,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = fontSize.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, bottom = 4.dp)
                                        )
                                    }
                                    is SheetLine.Lyric -> {
                                        // Lirik yang sudah dipasangkan ke baris chord di
                                        // atasnya dirender oleh chord itu; jangan dobel.
                                        val prev = lines.getOrNull(idx - 1)
                                        val pairedWithPrev = prev is SheetLine.Chords && !prev.isBarLine
                                        if (!pairedWithPrev) {
                                            ChordLyricFlow(
                                                chordLine = null,
                                                lyric = line.text,
                                                fontSize = fontSize,
                                                transposeDelta = transposeDelta,
                                                requestDiagram = requestDiagram
                                            )
                                        }
                                    }
                                    is SheetLine.Chords -> {
                                        if (line.isBarLine) {
                                            BarLineFlow(
                                                barLine = line,
                                                fontSize = fontSize,
                                                transposeDelta = transposeDelta,
                                                requestDiagram = requestDiagram
                                            )
                                        } else {
                                            ChordLyricFlow(
                                                chordLine = line,
                                                lyric = (lines.getOrNull(idx + 1) as? SheetLine.Lyric)?.text,
                                                fontSize = fontSize,
                                                transposeDelta = transposeDelta,
                                                requestDiagram = requestDiagram
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(120.dp)) }
                    }

                    if (autoScrollSpeed > 0f) {
                        AutoScrollRail(speed = autoScrollSpeed) { viewModel.setAutoScrollSpeed(it) }
                    }
                }
            }

            // Tapped-chord diagram: a small fretboard card pinned to the top-right
            // of the sheet. The overlay consumes taps before the page-turn handler.
            if (diagramSymbol != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { diagramSymbol = null }
                )
                ChordDiagramCard(
                    symbol = diagramSymbol!!,
                    db = fingeringDb,
                    onDismiss = { diagramSymbol = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 8.dp)
                )
            }
        }

        // Controls: one aligned control band (QA-2). Left→right, all
        // center-aligned on one row: [Auto Scroll] & [Page Flip] at 2× ·
        // transpose group at 1× · A− A+ at normal size (far right).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RaisedSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillButton("Auto Scroll", active = autoScrollSpeed > 0f, large = true) { viewModel.toggleAutoScroll() }
                PillButton("Page Flip", active = isPageFlip, large = true) { viewModel.togglePageFlip() }
                TransposeGroup(
                    delta = transposeDelta,
                    onDown = { viewModel.transposeDown() },
                    onUp = { viewModel.transposeUp() },
                    onReset = { viewModel.resetTranspose() }
                )
                PillButton("A−") { viewModel.decreaseFontSize() }
                PillButton("A+") { viewModel.increaseFontSize() }
                Spacer(modifier = Modifier.weight(1f))
                KeepScreenButton(enabled = keepScreenAwake) { viewModel.toggleKeepScreenAwake() }
            }
        }
    }

    if (showSetlistPicker) {
        AlertDialog(
            onDismissRequest = { if (!isAddingToSetlist) showSetlistPicker = false },
            title = { Text(strings.addToSetlistDialogTitle) },
            text = {
                if (setlists.isEmpty()) {
                    Text(strings.noSetlistsPrompt)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(setlists, key = { it.id }) { setlist ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAddingToSetlist) {
                                        showSetlistPicker = false
                                        viewModel.addCurrentSongToSetlist(setlist.id)
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(setlist.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isAddingToSetlist,
                    onClick = {
                        showSetlistPicker = false
                        showCreateSetlist = true
                    }
                ) { Text(strings.createSetlist) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isAddingToSetlist,
                    onClick = { showSetlistPicker = false }
                ) { Text(strings.cancel) }
            }
        )
    }

    if (showCreateSetlist) {
        AlertDialog(
            onDismissRequest = { if (!isAddingToSetlist) showCreateSetlist = false },
            title = { Text(strings.newSetlist) },
            text = {
                TextField(
                    value = newSetlistName,
                    onValueChange = { newSetlistName = it },
                    enabled = !isAddingToSetlist,
                    singleLine = true,
                    placeholder = { Text(strings.setlistName) }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newSetlistName.isNotBlank() && !isAddingToSetlist,
                    onClick = {
                        viewModel.createSetlistAndAddCurrentSong(newSetlistName)
                        newSetlistName = ""
                        showCreateSetlist = false
                    }
                ) { Text(strings.createAndAdd) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isAddingToSetlist,
                    onClick = {
                        newSetlistName = ""
                        showCreateSetlist = false
                    }
                ) { Text(strings.cancel) }
            }
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}
