package com.chordapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.catalogSlug
import com.chordapp.scraper.CiSongScraper
import com.chordapp.player.ChordParser
import com.chordapp.player.ChordSheet
import com.chordapp.player.Transposer
import com.chordapp.repositories.CatalogRepository
import com.chordapp.repositories.SetlistAddResult
import com.chordapp.repositories.UserdataRepository
import com.chordapp.settings.PlayerPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class PlayerSetlistEvent {
    data class Added(val setlistName: String) : PlayerSetlistEvent()
    data class AlreadyPresent(val setlistName: String) : PlayerSetlistEvent()
    object Error : PlayerSetlistEvent()
}

sealed class PlayerFavoriteEvent {
    data class Changed(val isFavorite: Boolean) : PlayerFavoriteEvent()
    object Error : PlayerFavoriteEvent()
}

class PlayerViewModel(
    private val repository: CatalogRepository,
    private val userdataRepository: UserdataRepository,
    private val preferences: PlayerPreferences? = null
) : ViewModel() {

    private val _song = MutableStateFlow<CatalogSongEntity?>(null)
    val song: StateFlow<CatalogSongEntity?> = _song.asStateFlow()

    private val _sheet = MutableStateFlow<ChordSheet?>(null)
    val sheet: StateFlow<ChordSheet?> = _sheet.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _transposeDelta = MutableStateFlow(0)
    val transposeDelta: StateFlow<Int> = _transposeDelta.asStateFlow()

    private val _capo = MutableStateFlow(0)
    val capo: StateFlow<Int> = _capo.asStateFlow()

    private val _fontSize = MutableStateFlow(14f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _autoScrollSpeed = MutableStateFlow(0f) // 0 means off
    val autoScrollSpeed: StateFlow<Float> = _autoScrollSpeed.asStateFlow()

    private val _lastAutoScrollSpeed = MutableStateFlow(
        preferences?.defaultScrollSpeed?.coerceIn(MIN_AUTO_SCROLL_SPEED, MAX_AUTO_SCROLL_SPEED) ?: 1f
    ) // resumes Play at this speed

    private val _keepScreenAwake = MutableStateFlow(preferences?.keepScreenAwake ?: true)
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    private val _isPageFlip = MutableStateFlow(false)
    val isPageFlip: StateFlow<Boolean> = _isPageFlip.asStateFlow()

    private val _availableVersions = MutableStateFlow<List<CatalogSongEntity>>(emptyList())
    val availableVersions: StateFlow<List<CatalogSongEntity>> = _availableVersions.asStateFlow()

    val setlists = userdataRepository.getAllSetlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isAddingToSetlist = MutableStateFlow(false)
    val isAddingToSetlist: StateFlow<Boolean> = _isAddingToSetlist.asStateFlow()

    private val setlistEventChannel = Channel<PlayerSetlistEvent>(Channel.BUFFERED)
    val setlistEvents = setlistEventChannel.receiveAsFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isUpdatingFavorite = MutableStateFlow(false)
    val isUpdatingFavorite: StateFlow<Boolean> = _isUpdatingFavorite.asStateFlow()

    private val favoriteEventChannel = Channel<PlayerFavoriteEvent>(Channel.BUFFERED)
    val favoriteEvents = favoriteEventChannel.receiveAsFlow()

    private var favoriteObservation: Job? = null

    private suspend fun sanitizeEntity(rawEntity: CatalogSongEntity): CatalogSongEntity {
        if (!rawEntity.artist.equals("CI Artist", ignoreCase = true) && !rawEntity.artist.contains("(?i)\\bfeat".toRegex())) {
            return rawEntity
        }

        val (fixedArtist, fixedTitle) = if (rawEntity.artist.equals("CI Artist", ignoreCase = true)) {
            val (pArtist, pTitle) = CiSongScraper.parseArtistAndTitleFromText(rawEntity.title, fallbackArtist = "")
            if (pArtist.isNotBlank()) CiSongScraper.normalizeFeatArtistAndTitle(pArtist, pTitle)
            else Pair(rawEntity.artist, rawEntity.title)
        } else {
            CiSongScraper.normalizeFeatArtistAndTitle(rawEntity.artist, rawEntity.title)
        }

        return if (fixedArtist != rawEntity.artist || fixedTitle != rawEntity.title) {
            val updated = rawEntity.copy(
                artist = fixedArtist,
                title = fixedTitle,
                artistSlug = catalogSlug(fixedArtist),
                titleSlug = catalogSlug(fixedTitle.replace("(?i)\\s*\\((?:feat\\.?|ft\\.?|featuring)\\s+[^)]+\\)".toRegex(), "").trim())
            )
            repository.insertSong(updated)
            updated
        } else {
            rawEntity
        }
    }

    fun loadSong(
        sourceUrlHash: String,
        initialTranspose: Int = 0,
        initialCapo: Int = 0
    ): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            _isLoading.value = true
            val t0 = System.currentTimeMillis()
            _transposeDelta.value = initialTranspose.coerceIn(-11, 11)
            _capo.value = initialCapo.coerceIn(0, 12)
            val url = java.net.URLDecoder.decode(sourceUrlHash, "UTF-8")
            val rawEntity = repository.getBySourceUrl(url)
            val t1 = System.currentTimeMillis()
            if (rawEntity != null) {
                val entity = sanitizeEntity(rawEntity)
                val t2 = System.currentTimeMillis()
                _song.value = entity
                _sheet.value = ChordParser.parse(entity.content)
                observeFavorite(entity.sourceUrl)

                // Record this play so Home "Recently Played" reflects real history.
                userdataRepository.recordPlay(entity)

                // Fetch curated versions
                val rawVersions = repository.getSongVersions(entity.artistSlug, entity.titleSlug)
                val t3 = System.currentTimeMillis()
                _availableVersions.value = com.chordapp.catalog.SongVersionCurator.curate(rawVersions)
                android.util.Log.d("PlayerLoadPerf", "loadSong: getBySourceUrl=${t1-t0}ms, sanitize=${t2-t1}ms, getVersions=${t3-t2}ms, total=${System.currentTimeMillis()-t0}ms")
            }
            _isLoading.value = false
        }
    }

    fun switchVersion(newVersionUrl: String): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            val rawEntity = repository.getBySourceUrl(newVersionUrl)
            if (rawEntity != null) {
                val entity = sanitizeEntity(rawEntity)
                _song.value = entity
                _sheet.value = ChordParser.parse(entity.content)
                observeFavorite(entity.sourceUrl)
                // The user actively chose to play this version — record it too.
                userdataRepository.recordPlay(entity)
                // Transpose and scroll state naturally preserved because they are separate StateFlows
                // We don't reset _transposeDelta!
            }
        }
    }

    private fun observeFavorite(sourceUrl: String) {
        favoriteObservation?.cancel()
        favoriteObservation = viewModelScope.launch {
            userdataRepository.observeIsFavorite(sourceUrl).collect { favorite ->
                if (_song.value?.sourceUrl == sourceUrl) _isFavorite.value = favorite
            }
        }
    }

    fun toggleFavorite() {
        val currentSong = _song.value ?: return
        if (_isUpdatingFavorite.value) return
        _isUpdatingFavorite.value = true
        val transpose = _transposeDelta.value
        val capo = _capo.value
        viewModelScope.launch {
            try {
                val favorite = userdataRepository.toggleFavorite(
                    song = currentSong,
                    transpose = transpose,
                    capo = capo
                )
                _isFavorite.value = favorite
                favoriteEventChannel.send(PlayerFavoriteEvent.Changed(favorite))
            } catch (_: Exception) {
                favoriteEventChannel.send(PlayerFavoriteEvent.Error)
            } finally {
                _isUpdatingFavorite.value = false
            }
        }
    }

    fun transposeUp() {
        _transposeDelta.value = (_transposeDelta.value + 1).coerceAtMost(11)
    }

    fun transposeDown() {
        _transposeDelta.value = (_transposeDelta.value - 1).coerceAtLeast(-11)
    }

    fun resetTranspose() {
        _transposeDelta.value = 0
    }

    fun capoUp() {
        _capo.value = (_capo.value + 1).coerceAtMost(12)
    }

    fun capoDown() {
        _capo.value = (_capo.value - 1).coerceAtLeast(0)
    }

    fun resetCapo() {
        _capo.value = 0
    }

    fun addCurrentSongToSetlist(setlistId: Long) {
        val currentSong = _song.value ?: return
        val setlistName = setlists.value.firstOrNull { it.id == setlistId }?.name ?: "setlist"
        addSongToSetlist(currentSong, setlistId, setlistName)
    }

    fun createSetlistAndAddCurrentSong(name: String) {
        val currentSong = _song.value ?: return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || _isAddingToSetlist.value) return

        _isAddingToSetlist.value = true
        val transpose = _transposeDelta.value
        viewModelScope.launch {
            try {
                val setlistId = userdataRepository.createSetlist(trimmedName)
                val result = userdataRepository.addSongToSetlist(
                    setlistId = setlistId,
                    song = currentSong,
                    transpose = transpose,
                    capo = _capo.value,
                    note = null
                )
                emitSetlistResult(result, trimmedName)
            } catch (_: Exception) {
                setlistEventChannel.send(PlayerSetlistEvent.Error)
            } finally {
                _isAddingToSetlist.value = false
            }
        }
    }

    private fun addSongToSetlist(
        currentSong: CatalogSongEntity,
        setlistId: Long,
        setlistName: String
    ) {
        if (_isAddingToSetlist.value) return
        _isAddingToSetlist.value = true
        val transpose = _transposeDelta.value
        viewModelScope.launch {
            try {
                val result = userdataRepository.addSongToSetlist(
                    setlistId = setlistId,
                    song = currentSong,
                    transpose = transpose,
                    capo = _capo.value,
                    note = null
                )
                emitSetlistResult(result, setlistName)
            } catch (_: Exception) {
                setlistEventChannel.send(PlayerSetlistEvent.Error)
            } finally {
                _isAddingToSetlist.value = false
            }
        }
    }

    private suspend fun emitSetlistResult(result: SetlistAddResult, setlistName: String) {
        val event = when (result) {
            SetlistAddResult.Added -> PlayerSetlistEvent.Added(setlistName)
            SetlistAddResult.AlreadyPresent -> PlayerSetlistEvent.AlreadyPresent(setlistName)
        }
        setlistEventChannel.send(event)
    }

    fun increaseFontSize() {
        _fontSize.value = (_fontSize.value + 2f).coerceAtMost(24f)
    }

    fun decreaseFontSize() {
        _fontSize.value = (_fontSize.value - 2f).coerceAtLeast(10f)
    }

    fun resetFontSize() {
        _fontSize.value = 14f
    }

    fun effectiveKey(): String? {
        val base = _song.value?.keyBase?.trim()
        if (base.isNullOrEmpty()) return null
        val delta = _transposeDelta.value
        if (delta == 0) return base
        return Transposer.transpose(base, delta)
    }

    fun toggleKeepScreenAwake() {
        val enabled = !_keepScreenAwake.value
        _keepScreenAwake.value = enabled
        preferences?.keepScreenAwake = enabled
    }

    fun toggleAutoScroll() {
        if (_autoScrollSpeed.value > 0f) {
            // Running -> pause, remembering the cadence so Play resumes it.
            _lastAutoScrollSpeed.value = _autoScrollSpeed.value
            _autoScrollSpeed.value = 0f
        } else {
            // Paused/off -> Play at the last remembered speed.
            // Auto-Scroll and Page Flip are mutually exclusive.
            _isPageFlip.value = false
            _autoScrollSpeed.value = _lastAutoScrollSpeed.value.coerceIn(MIN_AUTO_SCROLL_SPEED, MAX_AUTO_SCROLL_SPEED)
        }
    }

    /** Stops auto-scroll (e.g. reached end of sheet) while keeping the speed to resume later. */
    fun pauseAutoScroll() {
        if (_autoScrollSpeed.value > 0f) {
            _lastAutoScrollSpeed.value = _autoScrollSpeed.value
            _autoScrollSpeed.value = 0f
        }
    }

    fun setAutoScrollSpeed(speed: Float) {
        val clamped = speed.coerceIn(MIN_AUTO_SCROLL_SPEED, MAX_AUTO_SCROLL_SPEED)
        _lastAutoScrollSpeed.value = clamped
        preferences?.defaultScrollSpeed = clamped
        if (_autoScrollSpeed.value > 0f) {
            _autoScrollSpeed.value = clamped // live cadence update
        }
    }

    fun togglePageFlip() {
        _isPageFlip.value = !_isPageFlip.value
        if (_isPageFlip.value) {
            // Page Flip stops auto-scroll (mutually exclusive); remember speed to resume later.
            if (_autoScrollSpeed.value > 0f) {
                _lastAutoScrollSpeed.value = _autoScrollSpeed.value
            }
            _autoScrollSpeed.value = 0f
        }
    }

    companion object {
        /** Auto-scroll cadence bounds, in sheet lines per second. */
        const val MIN_AUTO_SCROLL_SPEED = 0.5f
        const val MAX_AUTO_SCROLL_SPEED = 4f
    }
}
