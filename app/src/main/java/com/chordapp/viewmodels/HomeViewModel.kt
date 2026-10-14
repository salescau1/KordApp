package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.charts.ChartEntry
import com.chordapp.charts.ChartPlatform
import com.chordapp.charts.ChartPlatformStore
import com.chordapp.charts.ChartRepository
import com.chordapp.charts.ChartResult
import com.chordapp.catalog.catalogSlug
import com.chordapp.repositories.CatalogRepository
import com.chordapp.repositories.UserdataRepository
import com.chordapp.scraper.UgSongScraper
import com.chordapp.userdata.PlayHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A recently played song shown on Home, resolved against the current catalog. */
data class RecentSongUi(
    val artist: String,
    val title: String,
    val source: String,
    val artistSlug: String,
    val titleSlug: String,
    /** True if the version (or at least this logical song) still exists in the catalog. */
    val available: Boolean
)

/** Runtime catalog facts displayed in expanded Home. */
data class HomeCatalogStats(
    val artists: Int,
    val songs: Int,
    val catalogBytes: Long
)

/** One chart row on Home, after resolving whether the local catalog has the song. */
data class ChartRowUi(
    val rank: Int,
    val title: String,
    val artist: String,
    /** True only if the (trimmed, case-insensitive) exact song exists in the local catalog. */
    val available: Boolean,
    val artistSlug: String? = null,
    val titleSlug: String? = null,
    /** Chord sources present in the catalog for this song (deduped); empty when unavailable. */
    val sources: List<String> = emptyList(),
    /** Deterministic catalog version used for sourceUrl-based favorite persistence. */
    val favoriteSourceUrl: String? = null,
    val isFavorite: Boolean = false
)

/** Chart section state. Never contains fabricated rankings. */
sealed interface ChartState {
    data object Loading : ChartState
    data class Loaded(
        val rows: List<ChartRowUi>,
        val fromCache: Boolean,
        val isRefreshing: Boolean = false
    ) : ChartState
    data object NoData : ChartState
}

/**
 * Feeds the Home "Recently Played" + "Top 20" sections.
 *
 * Recent: up to five newest plays from userdata, resolved against the catalog
 * (exact source_url first, then any version of the same logical song; unresolvable
 * rows stay visible but marked unavailable). Compact Home displays only the newest.
 *
 * Chart: loads a real no-key source via [ChartRepository]; each row is resolved to
 * the local catalog by exact artist/title match so "chords available" is always
 * truthful. Network failure falls back to the last cached snapshot; with neither,
 * [ChartState.NoData] is shown instead of fake rankings.
 */
class HomeViewModel(
    private val userdataRepository: UserdataRepository,
    private val catalogRepository: CatalogRepository,
    private val chartRepository: ChartRepository,
    private val platformStore: ChartPlatformStore = ChartPlatformStore.Default,
    private val catalogSizeBytes: () -> Long = { 0L }
) : ViewModel() {

    companion object {
        private val resolvedRows: MutableMap<ChartPlatform, List<ChartRowUi>> = mutableMapOf()
        private var cachedStats: HomeCatalogStats? = null

        internal fun resetTestCache() {
            resolvedRows.clear()
            cachedStats = null
        }
    }

    private val _recent = MutableStateFlow<List<RecentSongUi>>(emptyList())
    val recent: StateFlow<List<RecentSongUi>> = _recent.asStateFlow()

    private val _catalogStats = MutableStateFlow<HomeCatalogStats?>(cachedStats)
    val catalogStats: StateFlow<HomeCatalogStats?> = _catalogStats.asStateFlow()

    private val initialPlatform = platformStore.read()
    private val _chart = MutableStateFlow<ChartState>(
        resolvedRows[initialPlatform]?.let { rows ->
            ChartState.Loaded(rows = rows, fromCache = true, isRefreshing = false)
        } ?: chartRepository.cachedResolved(initialPlatform)?.let { rows ->
            resolvedRows[initialPlatform] = rows
            ChartState.Loaded(rows = rows, fromCache = true, isRefreshing = false)
        } ?: ChartState.Loading
    )
    val chart: StateFlow<ChartState> = _chart.asStateFlow()

    private val _platform = MutableStateFlow(initialPlatform)
    val platform: StateFlow<ChartPlatform> = _platform.asStateFlow()

    private var refreshJob: kotlinx.coroutines.Job? = null
    private var favoriteUrls: Set<String> = emptySet()
    private var lastHistory: List<PlayHistoryEntity> = emptyList()

    init {
        viewModelScope.launch {
            // Expanded Home shows the five newest plays. Compact Home deliberately
            // renders only the first row to retain its existing visual hierarchy.
            userdataRepository.getRecentlyPlayed(limit = 5).collect { history ->
                lastHistory = history
                _recent.value = history.map { resolve(it) }
            }
        }
        if (cachedStats == null) {
            viewModelScope.launch {
                val stats = HomeCatalogStats(
                    artists = catalogRepository.getUniqueArtistCount(),
                    songs = catalogRepository.getUniqueSongCount(),
                    catalogBytes = catalogSizeBytes()
                )
                cachedStats = stats
                _catalogStats.value = stats
            }
        }
        viewModelScope.launch {
            userdataRepository.getFavoriteSongs().collect { favorites ->
                favoriteUrls = favorites.map { it.sourceUrl }.toSet()
                val loaded = _chart.value as? ChartState.Loaded ?: return@collect
                _chart.value = loaded.copy(
                    rows = loaded.rows.map { row ->
                        row.copy(isFavorite = row.favoriteSourceUrl in favoriteUrls)
                    }
                )
            }
        }
        viewModelScope.launch {
            catalogRepository.catalogUpdateFlow.collect {
                // 1. Invalidate & refresh stats
                cachedStats = null
                val stats = HomeCatalogStats(
                    artists = catalogRepository.getUniqueArtistCount(),
                    songs = catalogRepository.getUniqueSongCount(),
                    catalogBytes = catalogSizeBytes()
                )
                cachedStats = stats
                _catalogStats.value = stats

                // 2. Invalidate chart caches & re-resolve current rows if loaded
                resolvedRows.clear()
                val currentLoaded = _chart.value as? ChartState.Loaded
                if (currentLoaded != null) {
                    val updatedRows = currentLoaded.rows.map { row ->
                        toRowUi(ChartEntry(row.rank, row.title, row.artist))
                    }
                    resolvedRows[_platform.value] = updatedRows
                    chartRepository.saveCachedResolved(_platform.value, updatedRows)
                    _chart.value = currentLoaded.copy(rows = updatedRows)
                }

                // 3. Re-resolve recently played songs against updated catalog
                if (lastHistory.isNotEmpty()) {
                    _recent.value = lastHistory.map { resolve(it) }
                }
            }
        }
        showCached(_platform.value)
    }

    fun retry() = manualRefresh()

    /** Pull-to-refresh: satu-satunya jalur yang menyentuh jaringan atas aksi user. */
    fun refresh() = manualRefresh()

    fun selectPlatform(platform: ChartPlatform) {
        if (_platform.value == platform) return
        _platform.value = platform
        platformStore.write(platform)
        showCached(platform)
    }

    fun toggleFavorite(row: ChartRowUi) {
        val sourceUrl = row.favoriteSourceUrl ?: return
        viewModelScope.launch {
            val song = catalogRepository.getBySourceUrl(sourceUrl) ?: return@launch
            userdataRepository.toggleFavorite(song, transpose = 0, capo = 0)
        }
    }

    fun downloadSong(row: ChartRowUi) {
        viewModelScope.launch(Dispatchers.IO) {
            val scraped = UgSongScraper.searchAndScrapeSong(
                artist = row.artist,
                title = row.title
            )
            if (scraped != null) {
                catalogRepository.insertSong(scraped)
                val updated = toRowUi(ChartEntry(row.rank, row.title, row.artist))
                val currentLoaded = _chart.value as? ChartState.Loaded ?: return@launch
                val platform = _platform.value
                val updatedRows = currentLoaded.rows.map { r ->
                    if (r.rank == row.rank && r.title == row.title) {
                        updated.copy(isFavorite = updated.favoriteSourceUrl in favoriteUrls)
                    } else {
                        r
                    }
                }
                resolvedRows[platform] = updatedRows
                chartRepository.saveCachedResolved(platform, updatedRows)
                _chart.value = currentLoaded.copy(rows = updatedRows)
            }
        }
    }

    /**
     * Tampilkan cache SEGERA tanpa jaringan: rows in-memory sesi ini kalau ada,
     * lalu cache disk resolved. Tampil seketika tanpa loading spinner atau jeda.
     * Tidak ada background update: murni pull-to-refresh sesuai aksi pengguna.
     */
    private fun showCached(platform: ChartPlatform) {
        refreshJob?.cancel()

        val remembered = resolvedRows[platform]
        if (remembered != null) {
            chartRepository.saveCachedResolved(platform, remembered)
            _chart.value = ChartState.Loaded(
                rows = remembered.map { it.copy(isFavorite = it.favoriteSourceUrl in favoriteUrls) },
                fromCache = true,
                isRefreshing = false
            )
            return
        }

        val cachedResolved = chartRepository.cachedResolved(platform)
        if (cachedResolved != null) {
            resolvedRows[platform] = cachedResolved
            _chart.value = ChartState.Loaded(
                rows = cachedResolved.map { it.copy(isFavorite = it.favoriteSourceUrl in favoriteUrls) },
                fromCache = true,
                isRefreshing = false
            )
            return
        }

        val cached = chartRepository.cached(platform)
        if (cached == null) {
            manualRefresh()
            return
        }

        // Tampilkan data cache langsung tanpa jeda Loading spinner
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            val cachedRows = cached.entries.map { toRowUi(it) }
            if (_platform.value != platform) return@launch
            resolvedRows[platform] = cachedRows
            chartRepository.saveCachedResolved(platform, cachedRows)
            _chart.value = ChartState.Loaded(
                rows = cachedRows.map { it.copy(isFavorite = it.favoriteSourceUrl in favoriteUrls) },
                fromCache = true,
                isRefreshing = false
            )
        }
    }

    /** Ambil data terbaru dari jaringan untuk platform aktif (retry / pull-to-refresh). */
    private fun manualRefresh() {
        val platform = _platform.value
        refreshJob?.cancel()

        _chart.value = when (val current = _chart.value) {
            is ChartState.Loaded -> current.copy(isRefreshing = true)
            else -> ChartState.Loading
        }

        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = chartRepository.load(platform)) {
                is ChartResult.Fresh -> {
                    val rows = result.snapshot.entries.map { toRowUi(it) }
                    resolvedRows[platform] = rows
                    chartRepository.saveCachedResolved(platform, rows)
                    if (_platform.value == platform) {
                        _chart.value = ChartState.Loaded(rows, fromCache = false, isRefreshing = false)
                    }
                }
                is ChartResult.FromCache -> {
                    val rows = result.snapshot.entries.map { toRowUi(it) }
                    resolvedRows[platform] = rows
                    chartRepository.saveCachedResolved(platform, rows)
                    if (_platform.value == platform) {
                        _chart.value = ChartState.Loaded(rows, fromCache = true, isRefreshing = false)
                    }
                }
                ChartResult.NoData -> {
                    if (_platform.value != platform) return@launch
                    val fallback = resolvedRows[platform] ?: chartRepository.cachedResolved(platform)
                    _chart.value = if (fallback != null) {
                        ChartState.Loaded(
                            rows = fallback.map { it.copy(isFavorite = it.favoriteSourceUrl in favoriteUrls) },
                            fromCache = true,
                            isRefreshing = false
                        )
                    } else {
                        ChartState.NoData
                    }
                }
            }
        }
    }

    private suspend fun toRowUi(entry: ChartEntry): ChartRowUi {
        // Cocokkan lewat kolom ber-index (artist_slug/title_slug), bukan
        // lower(trim(...)) yang memaksa full-table scan — cepat di katalog besar.
        // Teks Spotify membawa hiasan yang tak ada di slug katalog: subtitle sering
        // memuat banyak artis ("A, B feat. C") dan title punya "(feat...)", " - Remaster",
        // dsb. Cocok-persis-dua-slug jadi gagal walau lagunya ADA. Coba beberapa
        // kandidat (mentah -> artis-utama + judul-bersih), pakai yang pertama ketemu.
        val artistCandidates = listOf(entry.artist, primaryArtist(entry.artist)).distinct()
        val titleCandidates = listOf(entry.title, cleanTitle(entry.title)).distinct()
        var artistSlug = ""
        var titleSlug = ""
        var versions = emptyList<com.chordapp.catalog.CatalogSongEntity>()
        outer@ for (a in artistCandidates) {
            val aSlug = catalogSlug(a)
            if (aSlug.isEmpty()) continue
            for (t in titleCandidates) {
                val tSlug = catalogSlug(t)
                if (tSlug.isEmpty()) continue
                val found = catalogRepository.getSongVersions(aSlug, tSlug)
                if (found.isNotEmpty()) {
                    artistSlug = aSlug; titleSlug = tSlug; versions = found
                    break@outer
                }
            }
        }
        if (versions.isEmpty()) {
            // Fallback longgar: hiasan ("feat…", "(Beda)", " - Live") bisa ada di SISI
            // KATALOG (mis. artist_slug "enau-feat-ari-lesmana", title_slug
            // "jangan-paksa-rindu-beda"). Pakai slug bersih dari chart sebagai prefix.
            val aPrefix = catalogSlug(primaryArtist(entry.artist))
            val tPrefix = catalogSlug(cleanTitle(entry.title))
            // Sebagian baris katalog menyimpan diakritik dalam bentuk terurai
            // sehingga tandanya rontok jadi ASCII (mis. "eńau" -> "enau"), tapi
            // Spotify bisa mengirim bentuk tergabung ("ń"). Coba juga varian
            // ASCII-fold agar keduanya bertemu. urutan: slug apa adanya dulu.
            val artistPrefixes = listOf(aPrefix, asciiSlug(primaryArtist(entry.artist))).filter { it.isNotEmpty() }.distinct()
            val titlePrefixes = listOf(tPrefix, asciiSlug(cleanTitle(entry.title))).filter { it.isNotEmpty() }.distinct()
            loop@ for (ap in artistPrefixes) {
                for (tp in titlePrefixes) {
                    val found = catalogRepository.getSongVersionsByPrefix(ap, tp)
                    if (found.isNotEmpty()) {
                        artistSlug = found.first().artistSlug
                        titleSlug = found.first().titleSlug
                        versions = found
                        break@loop
                    }
                }
            }
        }
        if (versions.isEmpty()) {
            return ChartRowUi(entry.rank, entry.title, entry.artist, available = false)
        }
        val sources = versions.map { it.source }.distinct()
        val favoriteVersion = versions.firstOrNull()
        return ChartRowUi(
            rank = entry.rank,
            title = entry.title,
            artist = entry.artist,
            available = favoriteVersion != null,
            artistSlug = artistSlug,
            titleSlug = titleSlug,
            sources = sources,
            favoriteSourceUrl = favoriteVersion?.sourceUrl,
            isFavorite = favoriteVersion?.sourceUrl in favoriteUrls
        )
    }

    /** Slug dengan diakritik dilipat ke ASCII (NFD lalu buang tanda gabung). */
    private fun asciiSlug(raw: String): String {
        val n = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return catalogSlug(n)
    }

    /**
     * Artis utama dari string chart Spotify: subtitle sering "A, B feat. C" / "A & B" /
     * "A x B". Katalog menyimpan lagu di bawah artis utamanya, jadi ambil token pertama.
     */
    private fun primaryArtist(raw: String): String {
        var s = raw
        for (sep in listOf(" feat.", " feat ", " ft.", " ft ", " featuring ")) {
            val i = s.lowercase().indexOf(sep)
            if (i >= 0) s = s.substring(0, i)
        }
        return s.split(",", "&", " x ", " X ").firstOrNull()?.trim().orEmpty().ifEmpty { raw }
    }

    /**
     * Judul bersih: buang embel-embel "(feat. …)"/"[…]" dan sufiks " - Remastered/Live/…"
     * yang tidak ada di title_slug katalog.
     */
    private fun cleanTitle(raw: String): String {
        var s = raw.replace(Regex("\\([^)]*\\)"), " ").replace(Regex("\\[[^]]*]"), " ")
        val dash = s.indexOf(" - ")
        if (dash >= 0) s = s.substring(0, dash)
        return s.trim().ifEmpty { raw }
    }

    private suspend fun resolve(entry: PlayHistoryEntity): RecentSongUi {        // 1. Exact version reference.
        val exact = catalogRepository.getBySourceUrl(entry.sourceUrl)
        if (exact != null) {
            return RecentSongUi(
                artist = exact.artist,
                title = exact.title,
                source = exact.source,
                artistSlug = exact.artistSlug,
                titleSlug = exact.titleSlug,
                available = true
            )
        }

        // 2. Any version of the same logical song (catalog rebuild may have moved it).
        val fallback = catalogRepository.getSongVersions(entry.artistSlug, entry.titleSlug).firstOrNull()
        if (fallback != null) {
            return RecentSongUi(
                artist = fallback.artist,
                title = fallback.title,
                source = fallback.source,
                artistSlug = fallback.artistSlug,
                titleSlug = fallback.titleSlug,
                available = true
            )
        }

        // 3. Not in the current catalog — keep the snapshot visible but unavailable.
        return RecentSongUi(
            artist = entry.artist,
            title = entry.title,
            source = entry.source,
            artistSlug = entry.artistSlug,
            titleSlug = entry.titleSlug,
            available = false
        )
    }
}
