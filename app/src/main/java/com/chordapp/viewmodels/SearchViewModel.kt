package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.catalog.CatalogArtistInfo
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.CatalogSongGroup
import com.chordapp.repositories.CatalogRepository
import com.chordapp.scraper.CiSongScraper
import com.chordapp.scraper.GtSongScraper
import com.chordapp.scraper.UgSongScraper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchSource {
    LOCAL,
    UG,
    GT,
    CI
}

class SearchViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedSource = MutableStateFlow(SearchSource.LOCAL)
    val selectedSource: StateFlow<SearchSource> = _selectedSource.asStateFlow()

    private val _artistResults = MutableStateFlow<List<CatalogArtistInfo>>(emptyList())
    val artistResults: StateFlow<List<CatalogArtistInfo>> = _artistResults.asStateFlow()

    private val _songResults = MutableStateFlow<List<CatalogSongGroup>>(emptyList())
    val songResults: StateFlow<List<CatalogSongGroup>> = _songResults.asStateFlow()

    // Backwards compatibility
    val results: StateFlow<List<CatalogSongGroup>> = _songResults.asStateFlow()

    private val _ugVersionResults = MutableStateFlow<List<UgSongScraper.UgSongVersionItem>>(emptyList())
    val ugVersionResults: StateFlow<List<UgSongScraper.UgSongVersionItem>> = _ugVersionResults.asStateFlow()

    private val _gtVersionResults = MutableStateFlow<List<GtSongScraper.GtSongVersionItem>>(emptyList())
    val gtVersionResults: StateFlow<List<GtSongScraper.GtSongVersionItem>> = _gtVersionResults.asStateFlow()

    private val _ciVersionResults = MutableStateFlow<List<CiSongScraper.CiSongVersionItem>>(emptyList())
    val ciVersionResults: StateFlow<List<CiSongScraper.CiSongVersionItem>> = _ciVersionResults.asStateFlow()

    private val _ugCurrentPage = MutableStateFlow(1)
    val ugCurrentPage: StateFlow<Int> = _ugCurrentPage.asStateFlow()

    private val _ugTotalPages = MutableStateFlow(1)
    val ugTotalPages: StateFlow<Int> = _ugTotalPages.asStateFlow()

    private val _isLoadingMoreUg = MutableStateFlow(false)
    val isLoadingMoreUg: StateFlow<Boolean> = _isLoadingMoreUg.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _previewSong = MutableStateFlow<CatalogSongEntity?>(null)
    val previewSong: StateFlow<CatalogSongEntity?> = _previewSong.asStateFlow()

    private val _isPreviewLoading = MutableStateFlow(false)
    val isPreviewLoading: StateFlow<Boolean> = _isPreviewLoading.asStateFlow()

    private var searchJob: Job? = null

    fun setSource(source: SearchSource) {
        if (_selectedSource.value == source) return
        _selectedSource.value = source
        executeSearch(_query.value)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        executeSearch(newQuery)
    }

    private fun executeSearch(rawQuery: String) {
        searchJob?.cancel()

        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) {
            _artistResults.value = emptyList()
            _songResults.value = emptyList()
            _ugVersionResults.value = emptyList()
            _gtVersionResults.value = emptyList()
            _ciVersionResults.value = emptyList()
            _ugCurrentPage.value = 1
            _ugTotalPages.value = 1
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            when (_selectedSource.value) {
                SearchSource.LOCAL -> {
                    delay(250) // debounce
                    _artistResults.value = repository.searchArtists(trimmed, limit = 5)
                    _songResults.value = repository.searchSongs(trimmed, limit = 50)
                }
                SearchSource.GT -> {
                    delay(400) // debounce online GT
                    _gtVersionResults.value = GtSongScraper.searchSongVersions(trimmed)
                }
                SearchSource.UG -> {
                    delay(400) // debounce online UG
                    _ugCurrentPage.value = 1
                    _ugTotalPages.value = 1
                    val paged = UgSongScraper.searchSongVersionsPaged(trimmed, page = 1)
                    _ugVersionResults.value = paged.items
                    _ugCurrentPage.value = paged.currentPage
                    _ugTotalPages.value = paged.totalPages
                }
                SearchSource.CI -> {
                    delay(400) // debounce online CI
                    _ciVersionResults.value = CiSongScraper.searchSongVersions(trimmed)
                }
            }
            _isSearching.value = false
        }
    }

    fun loadMoreUg() {
        val trimmed = _query.value.trim()
        if (trimmed.isEmpty() || _isLoadingMoreUg.value) return
        val nextPage = _ugCurrentPage.value + 1
        if (nextPage > _ugTotalPages.value) return

        viewModelScope.launch {
            _isLoadingMoreUg.value = true
            try {
                val paged = UgSongScraper.searchSongVersionsPaged(trimmed, page = nextPage)
                if (paged.items.isNotEmpty()) {
                    val current = _ugVersionResults.value.toMutableList()
                    val existingUrls = current.map { it.tabUrl }.toSet()
                    val newItems = paged.items.filter { it.tabUrl !in existingUrls }
                    current.addAll(newItems)
                    _ugVersionResults.value = current
                    _ugCurrentPage.value = paged.currentPage
                    _ugTotalPages.value = paged.totalPages
                }
            } catch (_: Exception) {
                // Keep current results if network fails during pagination
            } finally {
                _isLoadingMoreUg.value = false
            }
        }
    }

    private val _scrapingState = MutableStateFlow<ScrapingState>(ScrapingState.Idle)
    val scrapingState: StateFlow<ScrapingState> = _scrapingState.asStateFlow()

    private var scrapingJob: Job? = null

    fun openPreview(tabUrl: String, artistHint: String = "", titleHint: String = "") {
        viewModelScope.launch {
            _isPreviewLoading.value = true
            val song = when {
                tabUrl.contains("guitartuna.com") -> GtSongScraper.fetchSheetPreview(tabUrl)
                tabUrl.contains("chordindonesia.com") -> CiSongScraper.fetchSheetPreview(tabUrl, artistHint = artistHint, titleHint = titleHint)
                else -> UgSongScraper.fetchSheetPreview(tabUrl)
            }
            _previewSong.value = song
            _isPreviewLoading.value = false
        }
    }

    fun closePreview() {
        _previewSong.value = null
    }

    fun downloadPreviewedSong() {
        val song = _previewSong.value ?: return
        viewModelScope.launch {
            repository.insertSong(song)
            _previewSong.value = null
            _scrapingState.value = ScrapingState.SongReady(song.artistSlug, song.titleSlug)
        }
    }

    fun downloadUgVersion(versionItem: UgSongScraper.UgSongVersionItem) {
        scrapingJob?.cancel()
        scrapingJob = viewModelScope.launch {
            _scrapingState.value = ScrapingState.LoadingSong(versionItem.title)
            try {
                val song = UgSongScraper.scrapeUrl(
                    versionItem.tabUrl,
                    artistHint = versionItem.artist,
                    titleHint = versionItem.title
                )
                if (song != null) {
                    repository.insertSong(song)
                    _scrapingState.value = ScrapingState.SongReady(song.artistSlug, song.titleSlug)
                } else {
                    _scrapingState.value = ScrapingState.Error("Gagal mengunduh versi ${versionItem.versionName}.")
                }
            } catch (e: Exception) {
                _scrapingState.value = ScrapingState.Error("Gagal mengunduh lagu: ${e.localizedMessage ?: "Koneksi terganggu"}")
            }
        }
    }

    fun downloadGtVersion(versionItem: GtSongScraper.GtSongVersionItem) {
        scrapingJob?.cancel()
        scrapingJob = viewModelScope.launch {
            _scrapingState.value = ScrapingState.LoadingSong(versionItem.title)
            try {
                val song = GtSongScraper.scrapeUrl(
                    versionItem.tabUrl,
                    artistHint = versionItem.artist,
                    titleHint = versionItem.title
                )
                if (song != null) {
                    repository.insertSong(song)
                    _scrapingState.value = ScrapingState.SongReady(song.artistSlug, song.titleSlug)
                } else {
                    _scrapingState.value = ScrapingState.Error("Gagal mengunduh versi ${versionItem.versionName}.")
                }
            } catch (e: Exception) {
                _scrapingState.value = ScrapingState.Error("Gagal mengunduh lagu: ${e.localizedMessage ?: "Koneksi terganggu"}")
            }
        }
    }

    fun downloadCiVersion(versionItem: CiSongScraper.CiSongVersionItem) {
        scrapingJob?.cancel()
        scrapingJob = viewModelScope.launch {
            _scrapingState.value = ScrapingState.LoadingSong(versionItem.title)
            try {
                val song = CiSongScraper.scrapeUrl(
                    versionItem.tabUrl,
                    artistHint = versionItem.artist,
                    titleHint = versionItem.title
                )
                if (song != null) {
                    repository.insertSong(song)
                    _scrapingState.value = ScrapingState.SongReady(song.artistSlug, song.titleSlug)
                } else {
                    _scrapingState.value = ScrapingState.Error("Gagal mengunduh versi ${versionItem.versionName}.")
                }
            } catch (e: Exception) {
                _scrapingState.value = ScrapingState.Error("Gagal mengunduh lagu: ${e.localizedMessage ?: "Koneksi terganggu"}")
            }
        }
    }

    fun scrapeArtistCi(artistName: String) {
        val trimmed = artistName.trim()
        if (trimmed.isBlank()) return
        scrapingJob?.cancel()
        scrapingJob = viewModelScope.launch {
            _scrapingState.value = ScrapingState.LoadingArtist(
                artist = trimmed,
                phase = "Mencari daftar lagu di CI..."
            )
            try {
                val songs = CiSongScraper.scrapeArtistAllSongs(
                    artistQuery = trimmed,
                    onProgress = { current, total, songTitle ->
                        _scrapingState.value = ScrapingState.LoadingArtist(
                            artist = trimmed,
                            phase = "Mengunduh chord CI",
                            current = current,
                            total = total,
                            currentSongTitle = songTitle
                        )
                    }
                )
                if (songs.isNotEmpty()) {
                    repository.insertSongs(songs)
                    val artistSlug = songs.first().artistSlug
                    _scrapingState.value = ScrapingState.ArtistReady(artistSlug, songs.size)
                } else {
                    _scrapingState.value = ScrapingState.Error("Tidak ada lagu yang ditemukan untuk \"$trimmed\" di CI.")
                }
            } catch (e: Exception) {
                _scrapingState.value = ScrapingState.Error("Gagal mengunduh artis: ${e.localizedMessage ?: "Koneksi terganggu"}")
            }
        }
    }

    fun scrapeArtistGt(artistName: String) {
        val trimmed = artistName.trim()
        if (trimmed.isBlank()) return
        scrapingJob?.cancel()
        scrapingJob = viewModelScope.launch {
            _scrapingState.value = ScrapingState.LoadingArtist(
                artist = trimmed,
                phase = "Mencari daftar lagu di GT..."
            )
            try {
                val songs = GtSongScraper.scrapeArtistAllSongs(
                    artistName = trimmed,
                    onProgress = { current, total, songTitle ->
                        _scrapingState.value = ScrapingState.LoadingArtist(
                            artist = trimmed,
                            phase = "Mengunduh chord GT (2 versi/lagu)",
                            current = current,
                            total = total,
                            currentSongTitle = songTitle
                        )
                    }
                )
                if (songs.isNotEmpty()) {
                    repository.insertSongs(songs)
                    val artistSlug = songs.first().artistSlug
                    _scrapingState.value = ScrapingState.ArtistReady(artistSlug, songs.size)
                } else {
                    _scrapingState.value = ScrapingState.Error("Tidak ada lagu yang ditemukan untuk \"$trimmed\" di GT.")
                }
            } catch (e: Exception) {
                _scrapingState.value = ScrapingState.Error("Gagal mengunduh artis: ${e.localizedMessage ?: "Koneksi terganggu"}")
            }
        }
    }

    fun scrapeSong(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        scrapingJob?.cancel()
        scrapingJob = viewModelScope.launch {
            _scrapingState.value = ScrapingState.LoadingSong(trimmed)
            try {
                val song = UgSongScraper.searchAndScrapeSongByQuery(trimmed)
                if (song != null) {
                    repository.insertSong(song)
                    _artistResults.value = repository.searchArtists(_query.value.trim(), limit = 5)
                    _songResults.value = repository.searchSongs(_query.value.trim(), limit = 50)
                    _scrapingState.value = ScrapingState.SongReady(song.artistSlug, song.titleSlug)
                } else {
                    _scrapingState.value = ScrapingState.Error("Lagu \"$trimmed\" tidak ditemukan di UG.")
                }
            } catch (e: Exception) {
                _scrapingState.value = ScrapingState.Error("Gagal mengunduh lagu: ${e.localizedMessage ?: "Koneksi terganggu"}")
            }
        }
    }

    fun scrapeArtist(artistName: String) {
        val trimmed = artistName.trim()
        if (trimmed.isBlank()) return
        scrapingJob?.cancel()
        scrapingJob = viewModelScope.launch {
            _scrapingState.value = ScrapingState.LoadingArtist(
                artist = trimmed,
                phase = "Mencari daftar lagu di UG..."
            )
            try {
                val songs = UgSongScraper.scrapeArtistAllSongs(
                    artistName = trimmed,
                    maxPages = 15,
                    onProgress = { current, total, songTitle ->
                        _scrapingState.value = ScrapingState.LoadingArtist(
                            artist = trimmed,
                            phase = "Mengunduh chord UG (2 versi/lagu)",
                            current = current,
                            total = total,
                            currentSongTitle = songTitle
                        )
                    }
                )
                if (songs.isNotEmpty()) {
                    repository.insertSongs(songs)
                    val artistSlug = songs.first().artistSlug
                    _scrapingState.value = ScrapingState.ArtistReady(artistSlug, songs.size)
                } else {
                    _scrapingState.value = ScrapingState.Error("Tidak ada chord ditemukan untuk artis \"$trimmed\" di UG.")
                }
            } catch (e: Exception) {
                _scrapingState.value = ScrapingState.Error("Gagal mengunduh artis: ${e.localizedMessage ?: "Koneksi terganggu"}")
            }
        }
    }

    fun cancelScraping() {
        scrapingJob?.cancel()
        scrapingJob = null
        _scrapingState.value = ScrapingState.Idle
    }

    fun dismissScrapingState() {
        _scrapingState.value = ScrapingState.Idle
    }
}

sealed class ScrapingState {
    object Idle : ScrapingState()
    data class LoadingSong(val query: String) : ScrapingState()
    data class LoadingArtist(
        val artist: String,
        val phase: String,
        val current: Int = 0,
        val total: Int = 0,
        val currentSongTitle: String = ""
    ) : ScrapingState()
    data class SongReady(val artistSlug: String, val titleSlug: String) : ScrapingState()
    data class ArtistReady(val artistSlug: String, val songsCount: Int) : ScrapingState()
    data class Error(val message: String) : ScrapingState()
}
