package com.chordapp.scraper

import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.catalogSlug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Native Android scraper and normalizer for GuitarTuna (GT).
 * 1:1 port of tools/crawler/parsers/guitartuna.py & batch_crawler.py.
 */
object GtSongScraper {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
    private const val APPLICATION_NAME = "GuitarTunaSongsSite"

    private val TITLE_PATTERN = Pattern.compile("^(.*?)\\s+Chords\\s+by\\s+(.*?)(?:\\s+-\\s+|\\s*\\|\\s*GuitarTuna)", Pattern.CASE_INSENSITIVE)
    private val VERSION_SUFFIX_PATTERN = Pattern.compile("\\s*\\(\\s*(?:official|original|easy)\\s+version\\s*\\)", Pattern.CASE_INSENSITIVE)
    private val CHORD_BRACKET_PATTERN = Pattern.compile(
        "\\[([A-G][b#]?(?:m|maj7|maj9|maj|min|7|9|11|13|sus2|sus4|sus|dim7|dim|aug|add9|m7b5|m7|mM7)?(?:/[A-G][b#]?)?)\\]",
        Pattern.CASE_INSENSITIVE
    )

    data class GtSongVersionItem(
        val title: String,
        val artist: String,
        val versionName: String,
        val rating: Float?,
        val votes: Int?,
        val keyBase: String?,
        val tabUrl: String
    )

    suspend fun searchSongVersions(query: String): List<GtSongVersionItem> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@withContext emptyList()

                val searchUrl = "https://gtweb-api-prod.guitartuna.com/songs/search?query=${URLEncoder.encode(trimmed, "UTF-8")}&limit=30&trim_transcription=false"
                val jsonText = fetchHttp(searchUrl, isJsonApi = true) ?: return@withContext emptyList()

                val root = JSONObject(jsonText)
                val songs = root.optJSONArray("songs") ?: return@withContext emptyList()

                val list = mutableListOf<GtSongVersionItem>()
                for (i in 0 until songs.length()) {
                    val item = songs.optJSONObject(i) ?: continue
                    val rawId = item.optString("_id")
                    val hexId = rawId.removePrefix("songid_")
                    if (hexId.isBlank()) continue

                    val rawSongTitle = item.optString("title").trim()
                    val rawArtistName = item.optString("artist_name").trim()
                    if (rawSongTitle.isBlank() || rawArtistName.isBlank()) continue

                    val (artistName, songTitle) = CiSongScraper.normalizeFeatArtistAndTitle(rawArtistName, rawSongTitle)

                    val slugTitle = catalogSlug(songTitle)
                    val slugArtist = catalogSlug(artistName)
                    val baseSlug = "$slugTitle-$slugArtist"

                    val rating = if (item.has("rating")) item.optDouble("rating").toFloat() else 4.8f

                    // 1. Easy Chords
                    list.add(
                        GtSongVersionItem(
                            title = songTitle,
                            artist = artistName,
                            versionName = "Easy Chords",
                            rating = rating,
                            votes = 120,
                            keyBase = null,
                            tabUrl = "https://guitartuna.com/chords/$baseSlug-easy-guitar-chords-$hexId"
                        )
                    )

                    // 2. Original Version
                    list.add(
                        GtSongVersionItem(
                            title = songTitle,
                            artist = artistName,
                            versionName = "Original Version",
                            rating = rating,
                            votes = 85,
                            keyBase = null,
                            tabUrl = "https://guitartuna.com/chords/$baseSlug-original-guitar-chords-$hexId"
                        )
                    )
                }

                list
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun scrapeUrl(url: String, artistHint: String = "", titleHint: String = ""): CatalogSongEntity? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchHttp(url, isJsonApi = false) ?: return@withContext null
                val doc = Jsoup.parse(html)

                var title = titleHint
                var artist = artistHint

                val titleElem = doc.selectFirst("title")
                if (titleElem != null && (title.isBlank() || artist.isBlank())) {
                    val raw = cleanText(titleElem.text())
                    val m = TITLE_PATTERN.matcher(raw)
                    if (m.find()) {
                        if (title.isBlank()) title = m.group(1)?.trim().orEmpty()
                        if (artist.isBlank()) artist = m.group(2)?.trim().orEmpty()
                    }
                }

                if (artist.isNotBlank()) {
                    artist = VERSION_SUFFIX_PATTERN.matcher(artist).replaceAll("").trim()
                }

                if (title.isBlank()) title = "Unknown Title"
                if (artist.isBlank()) artist = "Unknown Artist"

                val (cleanArtist, cleanTitle) = CiSongScraper.normalizeFeatArtistAndTitle(artist, title)
                artist = cleanArtist
                title = cleanTitle

                var versionName = "Chords"
                val urlLower = url.lowercase(Locale.US)
                if (urlLower.contains("easy")) {
                    versionName = "Easy Chords"
                } else if (urlLower.contains("original") || urlLower.contains("official")) {
                    versionName = "Original Version"
                }

                val elements = doc.select(".part_type, .line")
                if (elements.isEmpty()) return@withContext null

                val out = mutableListOf<String>()

                for (el in elements) {
                    if (el.hasClass("part_type")) {
                        val secName = el.text().trim()
                        if (secName.isNotEmpty()) {
                            if (out.isNotEmpty() && out.last().isNotEmpty()) {
                                out.add("")
                            }
                            val cleanSec = secName.trim('[', ']')
                            out.add("[$cleanSec]")
                        }
                    } else if (el.hasClass("line")) {
                        val lineChords = mutableListOf<String>()
                        for (cEl in el.select("[data-chord]")) {
                            val c = cEl.text().trim()
                            if (c.isNotEmpty()) lineChords.add(c)
                        }

                        var lyricDiv: org.jsoup.nodes.Element? = null
                        for (child in el.children()) {
                            if (child.tagName() == "div" && !child.hasClass("beat_info")) {
                                lyricDiv = child
                                break
                            }
                        }

                        val srSpans = lyricDiv?.select(".sr-only") ?: org.jsoup.select.Elements()

                        var textWithoutSr = ""
                        if (lyricDiv != null) {
                            val cloned = lyricDiv.clone()
                            cloned.select(".sr-only").remove()
                            textWithoutSr = cloned.text().trim()
                        }

                        if (textWithoutSr.isEmpty() && lineChords.isNotEmpty()) {
                            val chordLine = lineChords.joinToString("   ") { "[$it]" }
                            out.add(chordLine)
                        } else if (lyricDiv != null && textWithoutSr.isNotEmpty()) {
                            for (idx in srSpans.indices) {
                                val chord = if (idx < lineChords.size) lineChords[idx] else ""
                                val replacement = if (chord.isNotEmpty()) "[$chord]" else ""
                                srSpans[idx].replaceWith(org.jsoup.nodes.TextNode(replacement))
                            }
                            val rawText = lyricDiv.text()
                            val spacedLine = ensureChordSpacing(rawText.trim())
                            out.add(spacedLine)
                        }
                    }
                }

                val sheetContent = normalizeSheet(out.joinToString("\n"))
                if (sheetContent.isBlank()) return@withContext null

                val uniqueChords = extractUniqueChords(sheetContent)
                if (uniqueChords.isEmpty()) return@withContext null

                val lines = sheetContent.split("\n")
                val lineCount = lines.count { it.isNotBlank() }
                val chordsString = uniqueChords.joinToString(",")
                val contentHash = sha256(sheetContent)
                val scrapedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                CatalogSongEntity(
                    id = null,
                    source = "GT",
                    sourceUrl = url,
                    title = title,
                    artist = artist,
                    titleSlug = catalogSlug(title.replace("(?i)\\s*\\((?:feat\\.?|ft\\.?|featuring)\\s+[^)]+\\)".toRegex(), "").trim()),
                    artistSlug = catalogSlug(artist),
                    versionName = versionName,
                    rating = if (versionName == "Easy Chords") 4.8f else 4.7f,
                    votes = 100,
                    keyBase = null,
                    difficulty = if (versionName == "Easy Chords") "Beginner" else "Intermediate",
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

    suspend fun fetchSheetPreview(url: String): CatalogSongEntity? = scrapeUrl(url)

    suspend fun scrapeArtistAllSongs(
        artistName: String,
        onProgress: (current: Int, total: Int, songTitle: String) -> Unit
    ): List<CatalogSongEntity> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = artistName.trim()
                if (trimmed.isEmpty()) return@withContext emptyList()

                val searchUrl = "https://gtweb-api-prod.guitartuna.com/songs/search?query=${URLEncoder.encode(trimmed, "UTF-8")}&limit=50&trim_transcription=false"
                val jsonText = fetchHttp(searchUrl, isJsonApi = true) ?: return@withContext emptyList()

                val root = JSONObject(jsonText)
                val songs = root.optJSONArray("songs") ?: return@withContext emptyList()

                val normTarget = catalogSlug(trimmed)
                val candidatePairs = mutableListOf<Triple<String, String, String>>() // (url, artist, title)

                for (i in 0 until songs.length()) {
                    val item = songs.optJSONObject(i) ?: continue
                    val songArtist = item.optString("artist_name").trim()
                    val songTitle = item.optString("title").trim()
                    val rawId = item.optString("_id")
                    val hexId = rawId.removePrefix("songid_")
                    if (hexId.isBlank() || songTitle.isBlank()) continue

                    val normArtist = catalogSlug(songArtist)
                    if (!normArtist.contains(normTarget) && !normTarget.contains(normArtist)) continue

                    val slugTitle = catalogSlug(songTitle)
                    val baseSlug = "$slugTitle-$normArtist"

                    // 1. Easy Chords
                    candidatePairs.add(
                        Triple(
                            "https://guitartuna.com/chords/$baseSlug-easy-guitar-chords-$hexId",
                            songArtist,
                            songTitle
                        )
                    )
                    // 2. Original Version
                    candidatePairs.add(
                        Triple(
                            "https://guitartuna.com/chords/$baseSlug-original-guitar-chords-$hexId",
                            songArtist,
                            songTitle
                        )
                    )
                }

                if (candidatePairs.isEmpty()) return@withContext emptyList()

                val scrapedList = mutableListOf<CatalogSongEntity>()
                val total = candidatePairs.size

                for (idx in candidatePairs.indices) {
                    val (url, artist, title) = candidatePairs[idx]
                    onProgress(idx + 1, total, title)
                    val entity = scrapeUrl(url, artistHint = artist, titleHint = title)
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

    private fun fetchHttp(urlString: String, isJsonApi: Boolean): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (isJsonApi) {
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-Application-Name", APPLICATION_NAME)
            }
            conn.instanceFollowRedirects = true

            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun cleanText(text: String): String {
        return text
            .replace("\u2018", "'")
            .replace("\u2019", "'")
            .replace("\u201c", "\"")
            .replace("\u201d", "\"")
            .replace("\u2013", "-")
            .replace("\u2014", "--")
            .replace("\u00a0", " ")
    }

    private fun ensureChordSpacing(line: String): String = line.replace("][", "] [")

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
                val spaced = ensureChordSpacing(trimmedRight)
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
}
