package com.chordapp.ui.screens.player

import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import com.chordapp.settings.CustomPlayerManager
import java.io.File

import com.chordapp.R
import com.chordapp.ui.theme.ThemeTextureType

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import kotlinx.coroutines.*

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
    val isDark = LocalChordAppColors.current.isDark
    val textShadow = if (isDark) {
        Shadow(
            color = Color.Black.copy(alpha = 0.70f),
            offset = Offset(1.5f, 1.5f),
            blurRadius = 3f
        )
    } else null

    when (line) {
        is SheetLine.Blank -> Spacer(modifier = Modifier.height(slotHeight))
        is SheetLine.Section -> Box(
            modifier = Modifier.fillMaxWidth().height(slotHeight * 2f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = line.text,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                style = TextStyle(shadow = textShadow)
            )
        }
        is SheetLine.Lyric -> Box(modifier = Modifier.fillMaxWidth().height(slotHeight)) {
            Text(
                text = line.text,
                color = PrimaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                style = TextStyle(shadow = textShadow)
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
                    fontSize = fontSize.sp,
                    shadow = textShadow
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
    // Slot height in Dp. Accurately matches ChordLyricFlow slot height (1.35x font size).
    val slotDp = with(density) {
        val spDp = (fontSize * 1.35f).sp.toDp()
        val minDp = (fontSize * 1.35f).dp
        if (spDp > minDp) spDp else minDp
    }
    val slotPx = with(density) { slotDp.toPx() }

    var areaHeightPx by remember { mutableStateOf(0) }
    var areaWidthPx by remember { mutableStateOf(0) }

    // Column has padding(top = 6.dp, bottom = 6.dp) = 12.dp.
    val verticalPaddingPx = with(density) { 12.dp.toPx() }
    val usableHeightPx = (areaHeightPx - verticalPaddingPx).coerceAtLeast(0f)
    val capacity = if (usableHeightPx > 0f && slotPx > 0f) {
        (usableHeightPx / slotPx).toInt().coerceAtLeast(1)
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
            val customConfig by CustomPlayerManager.config.collectAsState()
            val sectionColor = if (customConfig.enabled) customConfig.getSectionColor() else PrimaryText
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
                        top = 6.dp,
                        bottom = 6.dp
                    )
            ) {
                page.forEachIndexed { idx, line ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when (line) {
                            is SheetLine.Blank -> {
                                Spacer(modifier = Modifier.height((fontSize * 0.75f).dp))
                            }
                            is SheetLine.Section -> {
                                AppSheetText(
                                    text = line.text,
                                    color = sectionColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSize,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 2.dp)
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
            val trackBorder = Border
            val trackAccent = AccentGold
            val thumbSurface = CardSurface
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                val left = (size.width - stroke) / 2f
                // Background inactive track (Border gray)
                drawRoundRect(
                    color = trackBorder,
                    topLeft = Offset(left, 0f),
                    size = Size(stroke, size.height),
                    cornerRadius = CornerRadius(stroke / 2f)
                )
                // Active track (AccentGold yellow) - Fills from clamped thumb down to bottom
                val filledHeight = size.height - clampedThumbY
                if (filledHeight > 0f) {
                    drawRoundRect(
                        color = trackAccent,
                        topLeft = Offset(left, clampedThumbY),
                        size = Size(stroke, filledHeight),
                        cornerRadius = CornerRadius(stroke / 2f)
                    )
                }
                // Thumb circle with contrasting center dot
                val cy = clampedThumbY.coerceIn(stroke, size.height - stroke)
                drawCircle(trackAccent, radius = stroke * 1.25f, center = Offset(size.width / 2f, cy))
                drawCircle(thumbSurface, radius = stroke * 0.45f, center = Offset(size.width / 2f, cy))
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

/** Floating circular action button (used for Back, Favorite, Add to Setlist). */
@Composable
private fun CircularActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryText,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = CardSurface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Border),
        shadowElevation = 4.dp,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/** Symbolic icon button for floating dock controls (Auto-Scroll, Page Flip, Keep Screen). */
@Composable
private fun SymbolicIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    active: Boolean = false,
    activeColor: Color = AccentGold,
    onClick: () -> Unit
) {
    val bg = if (active) activeColor.copy(alpha = 0.22f) else Color.Transparent
    val tint = if (active) activeColor else PrimaryText.copy(alpha = 0.85f)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Transpose segment: [ − | delta | + ].
 * Tapping the middle value restores default (+0).
 */
@Composable
private fun TransposeSegment(
    delta: Int,
    onDown: () -> Unit,
    onReset: () -> Unit,
    onUp: () -> Unit
) {
    val active = delta != 0
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .border(1.dp, if (active) AccentGold.copy(alpha = 0.7f) else Border, shape)
            .background(if (active) AccentGold.copy(alpha = 0.18f) else Border.copy(alpha = 0.22f), shape)
            .clip(shape)
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 34.dp)
                .clickable(onClick = onDown),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "−",
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Box(modifier = Modifier.width(1.dp).height(18.dp).background(Border.copy(alpha = 0.7f)))

        Box(
            modifier = Modifier
                .widthIn(min = 28.dp)
                .height(34.dp)
                .padding(horizontal = 4.dp)
                .clickable(onClick = onReset),
            contentAlignment = Alignment.Center
        ) {
            val label = if (delta > 0) "+$delta" else "$delta"
            Text(
                text = label,
                color = if (active) AccentGold else MutedText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Box(modifier = Modifier.width(1.dp).height(18.dp).background(Border.copy(alpha = 0.7f)))

        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 34.dp)
                .clickable(onClick = onUp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Font size segment: [ a | 0 | A ].
 * a kecil decreases, A besar increases, tapping 0 resets to default 14sp.
 */
@Composable
private fun FontSizeSegment(
    fontSize: Float,
    onDecrease: () -> Unit,
    onReset: () -> Unit,
    onIncrease: () -> Unit
) {
    val isModified = fontSize != 14f
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .border(1.dp, if (isModified) AccentGold.copy(alpha = 0.7f) else Border, shape)
            .background(if (isModified) AccentGold.copy(alpha = 0.18f) else Border.copy(alpha = 0.22f), shape)
            .clip(shape)
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 34.dp)
                .clickable(onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "a",
                color = PrimaryText,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }

        Box(modifier = Modifier.width(1.dp).height(18.dp).background(Border.copy(alpha = 0.7f)))

        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 34.dp)
                .clickable(onClick = onReset),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "0",
                color = if (isModified) AccentGold else MutedText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Box(modifier = Modifier.width(1.dp).height(18.dp).background(Border.copy(alpha = 0.7f)))

        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 34.dp)
                .clickable(onClick = onIncrease),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "A",
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    sourceUrlHash: String,
    initialTranspose: Int = 0,
    initialCapo: Int = 0,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onSearchClick: (query: String?, source: String?) -> Unit = { _, _ -> }
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
            if (event is PlayerFavoriteEvent.Error) {
                snackbarHostState.showSnackbar(strings.failedToUpdateFavorites)
            }
        }
    }
    // Keep the screen awake only while this Player is visible and the tile is ON.
    val view = LocalView.current
    DisposableEffect(view, keepScreenAwake) {
        view.keepScreenOn = keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }

    val currentActivity = view.context as? android.app.Activity
    val isDarkTheme = com.chordapp.ui.theme.LocalChordAppColors.current.isDark
    if (currentActivity != null) {
        DisposableEffect(currentActivity, isDarkTheme) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(currentActivity.window, view)
            val prevLightBars = insetsController.isAppearanceLightStatusBars
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            onDispose {
                insetsController.isAppearanceLightStatusBars = prevLightBars
            }
        }
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

    if (isLoading && song == null) {
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
        while (currentAutoScrollSpeed > 0f && !currentPageFlip && isActive) {
            // If user is actively touching or dragging the list, pause temporarily until finger released
            if (listState.isScrollInProgress) {
                while (listState.isScrollInProgress && isActive && currentAutoScrollSpeed > 0f) {
                    delay(50)
                }
                previousFrame = withFrameNanos { it }
                remainder = 0f
                continue
            }

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

            try {
                val consumed = listState.scrollBy(toScroll)
                val isAtBottom = !listState.canScrollForward &&
                    listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == (itemCount - 1).coerceAtLeast(0)
                if (consumed < toScroll * 0.5f && isAtBottom) {
                    viewModel.pauseAutoScroll()
                    break
                }
            } catch (e: CancellationException) {
                if (!isActive || currentAutoScrollSpeed <= 0f) throw e
                // Touch gesture or UI interruption: wait until user releases finger and resume smoothly
                while (listState.isScrollInProgress && isActive && currentAutoScrollSpeed > 0f) {
                    delay(50)
                }
                previousFrame = withFrameNanos { it }
                remainder = 0f
            }
        }
    }

    val effectiveKey = viewModel.effectiveKey()
    val versionIndex = availableVersions.indexOfFirst { it.sourceUrl == currentSong.sourceUrl }
    val sourceTag = if (currentSong.source.contains("Ultimate Guitar", ignoreCase = true)) "UG" else currentSong.source
    val versionLabel = if (versionIndex >= 0) "$sourceTag v${versionIndex + 1} ▾" else "$sourceTag ▾"

    val bandMode by com.chordapp.p2p.BandSessionManager.currentMode.collectAsState()
    val connectedClients by com.chordapp.p2p.BandSessionManager.connectedClients.collectAsState()
    val isBroadcastPaused by com.chordapp.p2p.BandSessionManager.isBroadcastPaused.collectAsState()

    // Server Mode: Broadcast current song only when song or broadcast pause changes
    LaunchedEffect(currentSong.sourceUrl, isBroadcastPaused) {
        if (bandMode == com.chordapp.p2p.BandMode.SERVER && hasContent && !isBroadcastPaused) {
            val rawContent = currentSong.content.ifBlank {
                com.chordapp.p2p.BandSessionManager.getCachedRawSheet(currentSong.sourceUrl) ?: ""
            }
            val payload = com.chordapp.p2p.LiveSongPayload(
                title = currentSong.title,
                artist = currentSong.artist,
                artistSlug = currentSong.artistSlug,
                titleSlug = currentSong.titleSlug,
                source = currentSong.source,
                sourceUrl = currentSong.sourceUrl,
                versionName = currentSong.versionName,
                effectiveKey = effectiveKey,
                transpose = transposeDelta,
                capo = capo,
                rawContent = rawContent
            )
            com.chordapp.p2p.BandSessionManager.broadcastSong(payload)
        }
    }

    // Server Mode: Broadcast transpose changes
    LaunchedEffect(transposeDelta) {
        if (bandMode == com.chordapp.p2p.BandMode.SERVER) {
            com.chordapp.p2p.BandSessionManager.broadcastTranspose(transposeDelta)
        }
    }

    // Server Mode: Broadcast standby when leaving PlayerScreen
    DisposableEffect(bandMode) {
        onDispose {
            if (bandMode == com.chordapp.p2p.BandMode.SERVER) {
                com.chordapp.p2p.BandSessionManager.broadcastStandby()
            }
        }
    }

    val connectionState by com.chordapp.p2p.BandSessionManager.connectionState.collectAsState()
    val isConnectedClient = bandMode == com.chordapp.p2p.BandMode.CLIENT && connectionState is com.chordapp.p2p.ClientConnectionState.Connected

    // Client Mode: Follow server song changes (only when connected and URL is different)
    LaunchedEffect(isConnectedClient) {
        if (isConnectedClient) {
            com.chordapp.p2p.BandSessionManager.incomingSong.collect { payload ->
                if (song?.sourceUrl != payload.sourceUrl) {
                    val safeHash = java.net.URLEncoder.encode(payload.sourceUrl, "UTF-8")
                    viewModel.loadSong(safeHash, payload.transpose, payload.capo)
                }
            }
        }
    }

    // Client Mode: Follow server transpose (only when connected)
    LaunchedEffect(isConnectedClient) {
        if (isConnectedClient) {
            com.chordapp.p2p.BandSessionManager.incomingTranspose.collect { delta ->
                viewModel.setTranspose(delta)
            }
        }
    }

    // Client Mode: Follow server standby (only when connected)
    LaunchedEffect(isConnectedClient) {
        if (isConnectedClient) {
            com.chordapp.p2p.BandSessionManager.standbyTrigger.collect {
                onBackClick()
            }
        }
    }

    val customConfig by CustomPlayerManager.config.collectAsState()
    val isCustomActive = customConfig.enabled

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        // Custom background photo or themed textured layer behind sheet
        if (isCustomActive && customConfig.bgEnabled && customConfig.bgPath != null && File(customConfig.bgPath!!).exists()) {
            AsyncImage(
                model = File(customConfig.bgPath!!),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = customConfig.bgDimmer))
            )
            if (customConfig.textureEnabled) {
                when (customConfig.textureType) {
                    "paper" -> {
                        Image(
                            painter = painterResource(id = R.drawable.texture_paper),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            alpha = 0.40f
                        )
                    }
                    "canvas" -> {
                        Image(
                            painter = painterResource(id = R.drawable.texture_leather),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            alpha = 0.40f
                        )
                    }
                    "halftone" -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dotSpacing = 16f
                            val dotRadius = 2.2f
                            var y = 0f
                            while (y < size.height) {
                                var x = 0f
                                while (x < size.width) {
                                    drawCircle(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        radius = dotRadius,
                                        center = Offset(x, y)
                                    )
                                    x += dotSpacing
                                }
                                y += dotSpacing
                            }
                        }
                    }
                }
            }
        } else {
            PlayerThemedTexture(textureType = com.chordapp.ui.theme.LocalChordAppColors.current.textureType)
        }

        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val sheetTopPadding = if (isPageFlip) statusBarTop + 60.dp else statusBarTop + 76.dp
        val sheetBottomPadding = if (isPageFlip) navBarBottom + 64.dp else navBarBottom + 88.dp

        // Sheet reading area
        val lines = if (hasContent) currentSheet!!.lines else emptyList()
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = sheetTopPadding, bottom = sheetBottomPadding)
                ) {
                    PagedSheet(
                        lines = lines,
                        fontSize = fontSize,
                        transposeDelta = transposeDelta,
                        contentKey = currentSong.sourceUrl,
                        requestDiagram = requestDiagram
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = sheetTopPadding,
                        bottom = sheetBottomPadding,
                        start = 16.dp,
                        end = 16.dp
                    )
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
                                    val sectionColor = if (isCustomActive) customConfig.getSectionColor() else PrimaryText
                                    AppSheetText(
                                        text = line.text,
                                        color = sectionColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = fontSize,
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
                    item { Spacer(modifier = Modifier.height(30.dp)) }
                }

                if (autoScrollSpeed > 0f) {
                    AutoScrollRail(speed = autoScrollSpeed) { viewModel.setAutoScrollSpeed(it) }
                }
            }
        }

        // Top Fadeout Scrim: Universal dark vignette so lyrics scroll out cleanly across all themes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarTop + 72.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.85f),
                        0.55f to Color.Black.copy(alpha = 0.45f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // Top Floating Islands: (<) (Title • Version ▾) (Love) (Setlist)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back circular button
            CircularActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick
            )

            // Title + Version floating capsule
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(23.dp),
                    color = CardSurface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Border),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .combinedClickable(
                            onClick = {
                                showVersionMenu = true
                            },
                            onLongClick = {
                                onSearchClick(null, "LOCAL")
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Baris 1: Judul Lagu (Tepat di tengah, bersih tanpa versi)
                        Text(
                            text = currentSong.title,
                            color = PrimaryText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Baris 2: Artis - Versi (Tanpa Key: C)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentSong.artist,
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = " - ",
                                color = MutedText.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                            )
                            Text(
                                text = versionLabel,
                                color = AccentGold,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (bandMode == com.chordapp.p2p.BandMode.SERVER) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isBroadcastPaused) Color(0xFFFF9800).copy(alpha = 0.25f) else Color(0xFF4CAF50).copy(alpha = 0.25f))
                                        .clickable {
                                            com.chordapp.p2p.BandSessionManager.toggleBroadcastPause()
                                            val msg = if (!isBroadcastPaused) "Siaran ke band di-pause" else "Siaran ke band aktif"
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBroadcastPaused) "PAUSED" else "LIVE (${connectedClients.size})",
                                        color = if (isBroadcastPaused) Color(0xFFFF9800) else Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            } else if (isConnectedClient) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "SYNC",
                                        color = AccentGold,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showVersionMenu,
                    onDismissRequest = { showVersionMenu = false }
                ) {
                    availableVersions.forEachIndexed { index, v ->
                        val itemTag = if (v.source.contains("Ultimate Guitar", ignoreCase = true)) "UG" else v.source
                        val label = if (!v.versionName.isNullOrBlank()) "$itemTag ${v.versionName}" else "$itemTag v${index + 1}"
                        val isSelected = v.sourceUrl == currentSong.sourceUrl
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (isSelected) PrimaryText else MutedText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = AccentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                showVersionMenu = false
                                if (v.sourceUrl != currentSong.sourceUrl) {
                                    viewModel.switchVersion(v.sourceUrl)
                                }
                            }
                        )
                    }
                    if (availableVersions.isNotEmpty()) {
                        HorizontalDivider(color = Border)
                    }
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                tint = AccentGold
                            )
                        },
                        text = {
                            Text(
                                strings.addAnotherVersion,
                                color = AccentGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            showVersionMenu = false
                            onSearchClick("${currentSong.artist} ${currentSong.title}", "UG")
                        }
                    )
                }
            }

            // Favorite circular button
            CircularActionButton(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                tint = if (isFavorite) ActiveHeart else PrimaryText,
                contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                enabled = !isUpdatingFavorite,
                onClick = { viewModel.toggleFavorite() }
            )

            // Add to setlist circular button
            CircularActionButton(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                tint = PrimaryText,
                contentDescription = strings.addSongToSetlist,
                enabled = !isAddingToSetlist,
                onClick = { showSetlistPicker = true }
            )
        }

        // Bottom Fadeout Scrim: Universal dark vignette so lyrics scroll out cleanly across all themes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(navBarBottom + 80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.45f),
                        1.0f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        // Bottom Floating Symbolic Dock: (scroll flip transpose size lampu)
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = CardSurface.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, Border),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Auto Scroll (▶ / ⏸)
                SymbolicIconButton(
                    icon = if (autoScrollSpeed > 0f) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Auto Scroll",
                    active = autoScrollSpeed > 0f,
                    onClick = { viewModel.toggleAutoScroll() }
                )

                // Page Flip (📖)
                SymbolicIconButton(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Page Flip",
                    active = isPageFlip,
                    onClick = { viewModel.togglePageFlip() }
                )

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Border.copy(alpha = 0.7f)))

                // Transpose (− 0 +) - tap 0 resets to 0
                TransposeSegment(
                    delta = transposeDelta,
                    onDown = { viewModel.transposeDown() },
                    onReset = { viewModel.resetTranspose() },
                    onUp = { viewModel.transposeUp() }
                )

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Border.copy(alpha = 0.7f)))

                // Font Size (a 0 A) - tap 0 resets to default (14sp)
                FontSizeSegment(
                    fontSize = fontSize,
                    onDecrease = { viewModel.decreaseFontSize() },
                    onReset = { viewModel.resetFontSize() },
                    onIncrease = { viewModel.increaseFontSize() }
                )

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Border.copy(alpha = 0.7f)))

                // Keep Screen Awake (💡 Lampu identik warna kuning/emas terang di semua tema)
                SymbolicIconButton(
                    icon = if (keepScreenAwake) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                    contentDescription = "Keep Screen On",
                    active = keepScreenAwake,
                    activeColor = Color(0xFFFFD54F), // Lampu Kuning/Emas Terang Otentik
                    onClick = { viewModel.toggleKeepScreenAwake() }
                )
            }
        }

        // Tapped-chord diagram: a small fretboard card pinned to the top-right
        // of the sheet. Drawn at the very top of the Box to ensure highest Z-order.
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
                    .padding(top = statusBarTop + 66.dp, end = 14.dp)
            )
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

@Composable
private fun PlayerThemedTexture(textureType: ThemeTextureType) {
    if (textureType == ThemeTextureType.NONE) return

    val resId = when (textureType) {
        ThemeTextureType.LEATHER -> R.drawable.texture_leather
        ThemeTextureType.PARCHMENT -> R.drawable.texture_paper
        ThemeTextureType.CHALKBOARD -> R.drawable.texture_blackboard
        ThemeTextureType.GREENBOARD -> R.drawable.texture_greenboard
        ThemeTextureType.CRIMSON_SHADOW -> R.drawable.texture_crimson_shadow
        ThemeTextureType.NONE -> return
    }

    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}
