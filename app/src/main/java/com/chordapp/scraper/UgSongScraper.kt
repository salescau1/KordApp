package com.chordapp.scraper

import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.catalogSlug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Native Android scraper and normalizer for Ultimate Guitar (UG).
 * Matches 100% of the specifications from KordApp-Data (tools/crawler/parsers/ultimate_guitar.py).
 */
object UgSongScraper {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    private val JS_STORE_PATTERN = Pattern.compile("class=\"js-store\"\\s+data-content=\"([^\"]+)\"")
    private val CHORD_TAG_PATTERN = Pattern.compile("\\[ch\\](.*?)\\[/ch\\]", Pattern.CASE_INSENSITIVE)
    private val TAB_TAG_PATTERN = Pattern.compile("\\[/?tab\\]", Pattern.CASE_INSENSITIVE)
    private val CHORD_BRACKET_PATTERN = Pattern.compile(
        "\\[([A-G][b#]?(?:m|maj7|maj9|maj|min|7|9|11|13|sus2|sus4|sus|dim7|dim|aug|add9|m7b5|m7|mM7)?(?:/[A-G][b#]?)?)\\]",
        Pattern.CASE_INSENSITIVE
    )

    suspend fun searchAndScrapeSong(artist: String, title: String): CatalogSongEntity? =
        withContext(Dispatchers.IO) {
            try {
                val qArtist = primaryArtist(artist)
                val qTitle = cleanTitle(title)
                val primaryQuery = "$qArtist $qTitle".trim()

                // 1. Try search with primary artist + clean title
                var searchUrl = "https://www.ultimate-guitar.com/search.php?title=${URLEncoder.encode(primaryQuery, "UTF-8")}&type=300"
                var html = fetchHtml(searchUrl)
                var bestTabUrl = html?.let { findBestChordTabUrl(it, targetArtist = artist, targetTitle = title) }

                // 2. Fallback: If no match and artist or title was complex, search by clean title alone
                if (bestTabUrl == null) {
                    val fallbackQuery = qTitle.trim()
                    if (fallbackQuery.isNotBlank() && fallbackQuery != primaryQuery) {
                        searchUrl = "https://www.ultimate-guitar.com/search.php?title=${URLEncoder.encode(fallbackQuery, "UTF-8")}&type=300"
                        html = fetchHtml(searchUrl)
                        bestTabUrl = html?.let { findBestChordTabUrl(it, targetArtist = artist, targetTitle = title) }
                    }
                }

                if (bestTabUrl == null) return@withContext null
                scrapeUrl(bestTabUrl, artistHint = artist, titleHint = title)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun searchAndScrapeSongByQuery(query: String): CatalogSongEntity? =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@withContext null

                if (trimmed.contains(" - ")) {
                    val parts = trimmed.split(" - ", limit = 2)
                    val result = searchAndScrapeSong(parts[0].trim(), parts[1].trim())
                    if (result != null) return@withContext result
                }

                val searchUrl = "https://www.ultimate-guitar.com/search.php?title=${URLEncoder.encode(trimmed, "UTF-8")}&type=300"
                val html = fetchHtml(searchUrl) ?: return@withContext null
                val bestTabUrl = findBestChordTabUrlForQuery(html, trimmed) ?: return@withContext null
                scrapeUrl(bestTabUrl, titleHint = trimmed)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun scrapeArtistAllSongs(
        artistName: String,
        maxPages: Int = 15,
        onProgress: (current: Int, total: Int, songTitle: String) -> Unit
    ): List<CatalogSongEntity> =
        withContext(Dispatchers.IO) {
            try {
                val normTargetArtist = normalizeForMatching(artistName)
                val encodedArtist = URLEncoder.encode(artistName.trim(), "UTF-8")
                val allCandidateTabs = mutableListOf<CandidateTab>()
                var currentPage = 1
                var totalPages = 1

                while (currentPage <= totalPages && currentPage <= maxPages) {
                    val url = "https://www.ultimate-guitar.com/search.php?title=$encodedArtist&type=300&page=$currentPage"
                    val html = fetchHtml(url) ?: break
                    val matcher = JS_STORE_PATTERN.matcher(html)
                    if (!matcher.find()) break
                    val rawGroup = matcher.group(1) ?: break
                    val rawJson = unescapeHtml(rawGroup)
                    val root = JSONObject(rawJson)
                    val pageData = root.optJSONObject("store")?.optJSONObject("page")?.optJSONObject("data") ?: break
                    val results = pageData.optJSONArray("results") ?: break
                    val pagination = pageData.optJSONObject("pagination")
                    totalPages = pagination?.optInt("total", 1) ?: 1

                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val type = item.optString("type")
                        val tabUrl = item.optString("tab_url")
                        if (type != "Chords" && !tabUrl.contains("-chords-") && !tabUrl.contains("/tab/")) continue
                        if (tabUrl.contains("/pro/")) continue

                        val itemArtist = item.optString("artist_name")
                        val normItemArtist = normalizeForMatching(itemArtist)
                        val isArtistMatch = normItemArtist == normTargetArtist ||
                                normItemArtist.contains(normTargetArtist) ||
                                normTargetArtist.contains(normItemArtist)
                        if (!isArtistMatch) continue

                        val songName = item.optString("song_name").trim()
                        if (songName.isEmpty()) continue
                        val votes = item.optInt("votes", 0)
                        val rating = item.optDouble("rating", 0.0)

                        allCandidateTabs.add(CandidateTab(songName, itemArtist, tabUrl, votes, rating))
                    }

                    currentPage++
                }

                // Group by normalized song title and pick up to 2 versions per song (Top Votes + Best Rating)
                val bySong = allCandidateTabs.groupBy { normalizeForMatching(cleanTitle(it.songName)) }
                val selectedTabs = mutableListOf<CandidateTab>()
                for ((_, tabs) in bySong) {
                    if (tabs.isEmpty()) continue
                    // 1. Top votes
                    val sortedByVotes = tabs.sortedByDescending { it.votes }
                    val topVotes = sortedByVotes.first()
                    selectedTabs.add(topVotes)

                    // 2. Best rating among remaining (min 3 votes to prevent anomalies)
                    val remaining = tabs.filter { it.tabUrl != topVotes.tabUrl }
                    if (remaining.isNotEmpty()) {
                        val qualified = remaining.filter { it.votes >= 3 }
                        val candidates = if (qualified.isNotEmpty()) qualified else remaining
                        val bestRating = candidates.maxByOrNull { it.rating }
                        if (bestRating != null) {
                            selectedTabs.add(bestRating)
                        }
                    }
                }

                if (selectedTabs.isEmpty()) return@withContext emptyList()

                val scrapedList = mutableListOf<CatalogSongEntity>()
                val total = selectedTabs.size

                for (idx in selectedTabs.indices) {
                    val tab = selectedTabs[idx]
                    onProgress(idx + 1, total, tab.songName)
                    val entity = scrapeUrl(tab.tabUrl, artistHint = tab.artistName, titleHint = tab.songName)
                    if (entity != null) {
                        scrapedList.add(entity)
                    }
                    kotlinx.coroutines.delay(200)
                }

                scrapedList
            } catch (_: Exception) {
                emptyList()
            }
        }

    data class UgSongVersionItem(
        val title: String,
        val artist: String,
        val versionName: String,
        val rating: Float?,
        val votes: Int?,
        val keyBase: String?,
        val tabUrl: String
    )

    data class UgSearchResult(
        val items: List<UgSongVersionItem>,
        val currentPage: Int,
        val totalPages: Int
    )

    suspend fun searchSongVersionsPaged(query: String, page: Int = 1): UgSearchResult =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@withContext UgSearchResult(emptyList(), 1, 1)
                val url = "https://www.ultimate-guitar.com/search.php?title=${URLEncoder.encode(trimmed, "UTF-8")}&type=300&page=$page"
                val html = fetchHtml(url) ?: return@withContext UgSearchResult(emptyList(), page, 1)
                val matcher = JS_STORE_PATTERN.matcher(html)
                if (!matcher.find()) return@withContext UgSearchResult(emptyList(), page, 1)
                val rawGroup = matcher.group(1) ?: return@withContext UgSearchResult(emptyList(), page, 1)
                val rawJson = unescapeHtml(rawGroup)
                val root = JSONObject(rawJson)
                val pageData = root.optJSONObject("store")?.optJSONObject("page")?.optJSONObject("data")
                    ?: return@withContext UgSearchResult(emptyList(), page, 1)
                val results = pageData.optJSONArray("results") ?: return@withContext UgSearchResult(emptyList(), page, 1)
                val pagination = pageData.optJSONObject("pagination")
                val totalPages = pagination?.optInt("total", 1) ?: 1
                val currentPage = pagination?.optInt("current", page) ?: page

                val list = mutableListOf<UgSongVersionItem>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val type = item.optString("type")
                    val tabUrl = item.optString("tab_url")
                    if (type != "Chords" && !tabUrl.contains("-chords-") && !tabUrl.contains("/tab/")) continue
                    if (tabUrl.contains("/pro/")) continue

                    val rawSongName = item.optString("song_name").trim()
                    val rawArtistName = item.optString("artist_name").trim()
                    val (artistName, songName) = CiSongScraper.normalizeFeatArtistAndTitle(rawArtistName, rawSongName)

                    val versionNum = item.optInt("version", 1)
                    val versionName = if (versionNum > 0) "Ver $versionNum" else "Chords"
                    val rating = if (item.has("rating")) item.optDouble("rating").toFloat() else null
                    val votes = if (item.has("votes")) item.optInt("votes") else null
                    val key = item.optString("tonality_name").ifBlank { null }

                    list.add(
                        UgSongVersionItem(
                            title = songName,
                            artist = artistName,
                            versionName = versionName,
                            rating = rating,
                            votes = votes,
                            keyBase = key,
                            tabUrl = tabUrl
                        )
                    )
                }
                UgSearchResult(list, currentPage, totalPages)
            } catch (_: Exception) {
                UgSearchResult(emptyList(), page, 1)
            }
        }

    suspend fun searchSongVersions(query: String): List<UgSongVersionItem> {
        return searchSongVersionsPaged(query, page = 1).items
    }

    suspend fun fetchSheetPreview(tabUrl: String): CatalogSongEntity? =
        withContext(Dispatchers.IO) {
            scrapeUrl(tabUrl)
        }

    suspend fun scrapeUrl(url: String, artistHint: String = "", titleHint: String = ""): CatalogSongEntity? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchHtml(url) ?: return@withContext null
                parseUgHtml(html, url, artistHint, titleHint)
            } catch (_: Exception) {
                null
            }
        }

    private fun fetchHtml(urlStr: String): String? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 12_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json")
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()
            text
        } catch (_: Exception) {
            null
        }
    }

    internal fun findBestChordTabUrl(
        html: String,
        targetArtist: String,
        targetTitle: String
    ): String? {
        val matcher = JS_STORE_PATTERN.matcher(html)
        if (!matcher.find()) return null
        val rawGroup = matcher.group(1) ?: return null
        val rawJson = unescapeHtml(rawGroup)
        return try {
            val root = JSONObject(rawJson)
            val results = root.optJSONObject("store")
                ?.optJSONObject("page")
                ?.optJSONObject("data")
                ?.optJSONArray("results") ?: return null

            val normTargetTitle = normalizeForMatching(cleanTitle(targetTitle))
            val normTargetArtist = normalizeForMatching(primaryArtist(targetArtist))

            var bestUrl: String? = null
            var bestScore = -1.0
            var bestTier = -1 // 3: Title exact & Artist match, 2: Title exact, 1: Title partial

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val type = item.optString("type")
                val tabUrl = item.optString("tab_url")
                if (type != "Chords" && !tabUrl.contains("-chords-") && !tabUrl.contains("/tab/")) continue
                if (tabUrl.contains("/pro/")) continue

                val itemTitle = item.optString("song_name")
                val itemArtist = item.optString("artist_name")
                val normItemTitle = normalizeForMatching(cleanTitle(itemTitle))
                val normItemArtist = normalizeForMatching(primaryArtist(itemArtist))

                val isTitleExact = normItemTitle.isNotEmpty() && normItemTitle == normTargetTitle
                val isTitlePartial = normItemTitle.isNotEmpty() && (
                    normItemTitle.startsWith(normTargetTitle) ||
                    normTargetTitle.startsWith(normItemTitle) ||
                    normItemTitle.contains(normTargetTitle) ||
                    normTargetTitle.contains(normItemTitle)
                )

                // Must have at least a title match; unrelated titles are rejected immediately
                if (!isTitleExact && !isTitlePartial) continue

                val isArtistMatch = normTargetArtist.isNotEmpty() && (
                    normItemArtist == normTargetArtist ||
                    normItemArtist.contains(normTargetArtist) ||
                    normTargetArtist.contains(normItemArtist) ||
                    normalizeForMatching(targetArtist).contains(normItemArtist)
                )

                val tier = when {
                    isTitleExact && isArtistMatch -> 3
                    isTitleExact -> 2
                    isTitlePartial && isArtistMatch -> 2
                    else -> 1
                }

                val rating = item.optDouble("rating", 0.0)
                val votes = item.optInt("votes", 0)
                val score = rating * (votes + 1)

                if (tier > bestTier || (tier == bestTier && score > bestScore)) {
                    bestTier = tier
                    bestScore = score
                    bestUrl = tabUrl
                }
            }
            bestUrl
        } catch (_: Exception) {
            null
        }
    }

    internal data class CandidateTab(
        val songName: String,
        val artistName: String,
        val tabUrl: String,
        val votes: Int,
        val rating: Double
    )

    internal fun findBestChordTabUrlForQuery(
        html: String,
        query: String
    ): String? {
        val matcher = JS_STORE_PATTERN.matcher(html)
        if (!matcher.find()) return null
        val rawGroup = matcher.group(1) ?: return null
        val rawJson = unescapeHtml(rawGroup)
        return try {
            val root = JSONObject(rawJson)
            val results = root.optJSONObject("store")
                ?.optJSONObject("page")
                ?.optJSONObject("data")
                ?.optJSONArray("results") ?: return null

            val normQuery = normalizeForMatching(query)
            var bestUrl: String? = null
            var bestScore = -1.0
            var bestTier = -1

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val type = item.optString("type")
                val tabUrl = item.optString("tab_url")
                if (type != "Chords" && !tabUrl.contains("-chords-") && !tabUrl.contains("/tab/")) continue
                if (tabUrl.contains("/pro/")) continue

                val itemTitle = item.optString("song_name")
                val itemArtist = item.optString("artist_name")
                val normItemTitle = normalizeForMatching(cleanTitle(itemTitle))
                val normItemArtist = normalizeForMatching(primaryArtist(itemArtist))

                val tier = when {
                    normItemTitle == normQuery -> 4
                    normItemTitle.startsWith(normQuery) || normQuery.startsWith(normItemTitle) -> 3
                    normItemTitle.contains(normQuery) || normQuery.contains(normItemTitle) -> 2
                    normItemArtist == normQuery || normItemArtist.contains(normQuery) -> 2
                    else -> 1
                }

                val rating = item.optDouble("rating", 0.0)
                val votes = item.optInt("votes", 0)
                val score = rating * (votes + 1)

                if (tier > bestTier || (tier == bestTier && score > bestScore)) {
                    bestTier = tier
                    bestScore = score
                    bestUrl = tabUrl
                }
            }
            bestUrl
        } catch (_: Exception) {
            null
        }
    }

    internal fun normalizeForMatching(str: String): String {
        val n = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return n.lowercase(Locale.ROOT)
            .replace("'", "")
            .replace("’", "")
            .replace(Regex("[^a-z0-9]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    internal fun cleanTitle(raw: String): String {
        var s = raw.replace(Regex("\\([^)]*\\)"), " ").replace(Regex("\\[[^]]*]"), " ")
        val dash = s.indexOf(" - ")
        if (dash >= 0) s = s.substring(0, dash)
        return s.trim().ifEmpty { raw }
    }

    internal fun primaryArtist(raw: String): String {
        var s = raw
        for (sep in listOf(" feat.", " feat ", " ft.", " ft ", " featuring ")) {
            val i = s.lowercase(Locale.ROOT).indexOf(sep)
            if (i >= 0) s = s.substring(0, i)
        }
        return s.split(",", "&", " x ", " X ").firstOrNull()?.trim().orEmpty().ifEmpty { raw }
    }

    internal fun cleanArtistName(raw: String, hint: String): String {
        val withoutParens = raw.replace(Regex("\\([^)]*\\)"), "").trim()
        if (hint.isNotBlank()) {
            val normHint = normalizeForMatching(primaryArtist(hint))
            val normRaw = normalizeForMatching(withoutParens)
            if (normHint == normRaw) return primaryArtist(hint)
        }
        return withoutParens.ifBlank { raw }
    }

    internal fun parseUgHtml(htmlText: String, url: String, artistHint: String, titleHint: String): CatalogSongEntity? {
        val matcher = JS_STORE_PATTERN.matcher(htmlText)
        if (!matcher.find()) return null
        val rawGroup = matcher.group(1) ?: return null
        val rawJson = unescapeHtml(rawGroup)

        return try {
            val root = JSONObject(rawJson)
            val pageData = root.optJSONObject("store")?.optJSONObject("page")?.optJSONObject("data") ?: return null
            val tabView = pageData.optJSONObject("tab_view") ?: return null
            val rawContent = unescapeHtml(tabView.optJSONObject("wiki_tab")?.optString("content").orEmpty())
            if (rawContent.isBlank()) return null

            val tab = pageData.optJSONObject("tab") ?: return null
            val rawTitle = tab.optString("song_name").ifBlank { titleHint }.trim()
            val rawArtist = tab.optString("artist_name").ifBlank { artistHint }.trim()
            val initialTitle = cleanTitle(rawTitle).ifBlank { rawTitle }
            val initialArtist = cleanArtistName(rawArtist, artistHint)

            val (artist, title) = CiSongScraper.normalizeFeatArtistAndTitle(initialArtist, initialTitle)
            if (title.isBlank() || artist.isBlank()) return null

            val versionNum = tab.optInt("version", 1)
            val versionName = "Ver $versionNum"
            val rating = if (tab.has("rating")) tab.optDouble("rating").toFloat() else null
            val votes = if (tab.has("votes")) tab.optInt("votes") else null
            val keyBase = tab.optString("tonality_name").ifBlank { null }
            val difficulty = tab.optString("difficulty").ifBlank { null }

            // Clean BBCode tags:
            // 1. Strip [tab] and [/tab]
            var cleaned = TAB_TAG_PATTERN.matcher(rawContent).replaceAll("")
            // 2. Convert [ch]Chord[/ch] -> [Chord]
            cleaned = CHORD_TAG_PATTERN.matcher(cleaned).replaceAll("[$1]")

            val sheetContent = normalizeSheet(unescapeHtml(cleaned))
            if (sheetContent.isBlank()) return null

            val uniqueChords = extractUniqueChords(sheetContent)
            if (uniqueChords.isEmpty()) return null

            val lines = sheetContent.split("\n")
            val lineCount = lines.count { it.isNotBlank() }
            val chordsString = uniqueChords.joinToString(",")
            val contentHash = sha256(sheetContent)
            val scrapedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

            CatalogSongEntity(
                id = null,
                source = "UG",
                sourceUrl = url,
                title = title,
                artist = artist,
                titleSlug = catalogSlug(title.replace("(?i)\\s*\\((?:feat\\.?|ft\\.?|featuring)\\s+[^)]+\\)".toRegex(), "").trim()),
                artistSlug = catalogSlug(artist),
                versionName = versionName,
                rating = rating,
                votes = votes,
                keyBase = keyBase,
                difficulty = difficulty,
                category = "BARAT",
                region = "BARAT",
                chords = chordsString,
                chordCount = uniqueChords.size,
                lineCount = lineCount,
                content = sheetContent,
                contentHash = contentHash,
                scrapedAt = scrapedAt
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeSheet(content: String): String {
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val processed = mutableListOf<String>()
        var lastBlank = false

        for (line in lines) {
            val trimmedRight = line.trimEnd()
            val isBlank = trimmedRight.isBlank()
            if (isBlank) {
                if (!lastBlank && processed.isNotEmpty()) {
                    processed.add("")
                    lastBlank = true
                }
            } else {
                // Ensure touching chords [C][Am] are separated to [C] [Am]
                val spaced = trimmedRight.replace("][", "] [")
                processed.add(spaced)
                lastBlank = false
            }
        }
        return processed.joinToString("\n").trim()
    }

    private fun extractUniqueChords(content: String): List<String> {
        val matcher = CHORD_BRACKET_PATTERN.matcher(content)
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        while (matcher.find()) {
            val c = matcher.group(1)?.trim().orEmpty()
            if (c.isNotEmpty()) {
                val canonical = c.take(1).uppercase(Locale.US) + c.drop(1)
                if (seen.add(canonical)) {
                    result.add(canonical)
                }
            }
        }
        return result
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    internal fun unescapeHtml(text: String): String {
        return text
            .replace(Regex("&#0*39;"), "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .replace("&#x27;", "'", ignoreCase = true)
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")
            .replace("&#x2F;", "/", ignoreCase = true)
            .replace("&hellip;", "...")
    }
}
