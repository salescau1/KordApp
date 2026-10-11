package com.chordapp.scraper

import com.chordapp.catalog.CatalogSongEntity
import com.chordapp.catalog.catalogSlug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Native Android scraper and normalizer for CI (ChordIndonesia).
 * Parses chord sheets from https://www.chordindonesia.com
 */
object CiSongScraper {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    private val TITLE_PATTERN = Pattern.compile(
        "^Kunci\\s+Gitar\\s+(.*?)\\s+-\\s+(.*?)(?:\\s+Chord.*|$)",
        Pattern.CASE_INSENSITIVE
    )

    private val CHORD_BRACKET_PATTERN = Pattern.compile(
        "\\[([A-G][b#]?(?:m|maj7|maj9|maj|min|7|9|11|13|sus2|sus4|sus|dim7|dim|aug|add9|m7b5|m7|mM7)?(?:/[A-G][b#]?)?)\\]",
        Pattern.CASE_INSENSITIVE
    )

    data class CiSongVersionItem(
        val title: String,
        val artist: String,
        val versionName: String,
        val rating: Float?,
        val votes: Int?,
        val keyBase: String?,
        val tabUrl: String
    )

    /**
     * Search song versions from CI.
     * Checks artist page `https://www.chordindonesia.com/chord/{artistSlug}`
     * and direct slug URLs.
     */
    suspend fun searchSongVersions(query: String): List<CiSongVersionItem> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@withContext emptyList()

                val results = mutableListOf<CiSongVersionItem>()
                val seenUrls = mutableSetOf<String>()

                val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val querySlug = catalogSlug(trimmed)
                val foldedQuerySlug = catalogSlug(com.chordapp.catalog.foldDiacritics(trimmed))

                // 1. Try artist page for the full query slug (and folded ascii version)
                val candidateSlugs = mutableListOf<String>()
                candidateSlugs.add(querySlug)
                if (foldedQuerySlug.isNotEmpty() && !candidateSlugs.contains(foldedQuerySlug)) {
                    candidateSlugs.add(foldedQuerySlug)
                }
                for (t in tokens) {
                    val s = catalogSlug(t)
                    val fs = catalogSlug(com.chordapp.catalog.foldDiacritics(t))
                    if (s.length >= 2 && !candidateSlugs.contains(s)) candidateSlugs.add(s)
                    if (fs.length >= 2 && !candidateSlugs.contains(fs)) candidateSlugs.add(fs)
                }
                if (tokens.size >= 2) {
                    val firstTwo = catalogSlug("${tokens[0]} ${tokens[1]}")
                    val foldedFirstTwo = catalogSlug(com.chordapp.catalog.foldDiacritics("${tokens[0]} ${tokens[1]}"))
                    if (!candidateSlugs.contains(firstTwo)) candidateSlugs.add(firstTwo)
                    if (!candidateSlugs.contains(foldedFirstTwo)) candidateSlugs.add(foldedFirstTwo)
                }

                for (slug in candidateSlugs) {
                    val artistUrl = "https://www.chordindonesia.com/chord/$slug"
                    val html = fetchHtml(artistUrl) ?: continue
                    val doc = Jsoup.parse(html)
                    val h1 = doc.selectFirst("h1")?.text()?.trim() ?: ""
                    if (h1.contains("Tidak Ditemukan", ignoreCase = true) || h1.contains("404")) continue

                    val pageArtist = h1.removePrefix("Kumpulan Chord").removeSuffix("@ Chordindonesia.com").trim()

                    for (a in doc.select("a[href]")) {
                        val href = a.attr("href").trim()
                        val text = a.text().trim()
                        if (href.startsWith("https://www.chordindonesia.com/") && text.length > 2) {
                            if (!href.contains("/chord/") &&
                                !href.contains("/kumpulan-kunci-gitar/") &&
                                !href.contains("/category/") &&
                                !href.contains("/tag/") &&
                                !href.contains("/page/") &&
                                !href.contains("#") &&
                                !href.contains("tentang-kami") &&
                                !href.contains("contact") &&
                                !href.contains("disclaimer") &&
                                !href.contains("privacy") &&
                                !href.contains("transpose") &&
                                !href.contains("tuner") &&
                                !href.contains("daftar-isi") &&
                                !href.contains("0-9") &&
                                href != "https://www.chordindonesia.com/" &&
                                href != "https://www.chordindonesia.com"
                            ) {
                                if (seenUrls.add(href)) {
                                    val (parsedArtist, parsedTitle) = parseArtistAndTitleFromText(text, fallbackArtist = pageArtist)
                                    // If query had multiple tokens, filter loosely
                                    val match = if (tokens.size > 1 && slug != querySlug) {
                                        tokens.any { text.contains(it, ignoreCase = true) }
                                    } else {
                                        true
                                    }
                                    if (match) {
                                        results.add(
                                            CiSongVersionItem(
                                                title = parsedTitle,
                                                artist = parsedArtist,
                                                versionName = "CI Standard",
                                                rating = 4.8f,
                                                votes = 50,
                                                keyBase = null,
                                                tabUrl = href
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (results.isNotEmpty() && slug == querySlug) break
                }

                // 2. Try direct slug match: https://www.chordindonesia.com/{querySlug}
                val directUrl = "https://www.chordindonesia.com/$querySlug"
                if (!seenUrls.contains(directUrl)) {
                    val directHtml = fetchHtml(directUrl)
                    if (directHtml != null) {
                        val doc = Jsoup.parse(directHtml)
                        val contentPre = doc.selectFirst("div.entry-content pre")
                        if (contentPre != null) {
                            val directCategoryArtist = doc.select("a[href*='/chord/']").firstOrNull { a ->
                                val href = a.attr("href")
                                !href.contains("/page/") && !href.contains("?") && !href.contains("#") &&
                                    a.text().isNotBlank() && !a.text().contains("Daftar", ignoreCase = true)
                            }?.text()?.trim() ?: ""
                            val h1 = doc.selectFirst("h1")?.text()?.trim() ?: ""
                            val (parsedArtist, parsedTitle) = parseArtistAndTitleFromH1(h1, fallbackArtist = directCategoryArtist)
                            val finalArtist = if (parsedArtist.isNotBlank() && !parsedArtist.equals("CI Artist", ignoreCase = true)) parsedArtist
                                else directCategoryArtist.ifBlank { "CI Artist" }
                            if (parsedTitle.isNotBlank()) {
                                results.add(
                                    0,
                                    CiSongVersionItem(
                                        title = parsedTitle,
                                        artist = finalArtist,
                                        versionName = "CI Standard",
                                        rating = 4.9f,
                                        votes = 100,
                                        keyBase = null,
                                        tabUrl = directUrl
                                    )
                                )
                            }
                        }
                    }
                }

                results
            } catch (_: Exception) {
                emptyList()
            }
        }

    /**
     * Scrape a single song URL from CI into a CatalogSongEntity.
     */
    suspend fun scrapeUrl(url: String, artistHint: String = "", titleHint: String = ""): CatalogSongEntity? =
        withContext(Dispatchers.IO) {
            try {
                val html = fetchHtml(url) ?: return@withContext null
                val doc = Jsoup.parse(html)

                val contentPre = doc.selectFirst("div.entry-content pre") ?: return@withContext null

                // Replace chord spans with [Chord] notation
                for (span in contentPre.select("span.showTip")) {
                    val chord = span.text().trim()
                    if (chord.isNotEmpty()) {
                        span.replaceWith(org.jsoup.nodes.TextNode("[$chord]"))
                    }
                }

                var rawText = contentPre.wholeText()

                // Normalize section headers: "Intro :" -> "[Intro]"
                rawText = rawText.replace("(?im)^(Intro|Chorus|Int\\.?|Outro|Bridge|Verse|Reff|Bait)\\s*:\\s*".toRegex(), "[$1]\n")
                rawText = rawText.replace("[Int.]", "[Interlude]")
                rawText = rawText.replace("[Int]", "[Interlude]")
                rawText = rawText.replace("[Reff]", "[Chorus]")
                rawText = rawText.replace("[Bait]", "[Verse]")

                // Normalize transition chords like -[Fm], ([G]-[Bb]), [C]..
                val lines = rawText.lines()
                val cleanLines = mutableListOf<String>()
                for (l in lines) {
                    var line = l
                    // Clean trailing unneeded tokens at the end of chord lines if present
                    cleanLines.add(line)
                }

                val finalContent = cleanLines.joinToString("\n").trim()
                if (finalContent.length < 50) return@withContext null

                // 1. Try to extract artist from category link in HTML
                val categoryArtist = doc.select("a[href*='/chord/']").firstOrNull { a ->
                    val href = a.attr("href")
                    !href.contains("/page/") && !href.contains("?") && !href.contains("#") &&
                        a.text().isNotBlank() && !a.text().contains("Daftar", ignoreCase = true)
                }?.text()?.trim() ?: ""

                // 2. Parse Title & Artist from H1
                val h1Text = doc.selectFirst("h1")?.text()?.trim() ?: ""
                val (parsedArtist, parsedTitle) = parseArtistAndTitleFromH1(h1Text, fallbackArtist = categoryArtist)

                val rawTitle = when {
                    titleHint.isNotBlank() -> titleHint
                    parsedTitle.isNotBlank() -> parsedTitle
                    else -> cleanTitleFromUrl(url)
                }

                val rawArtist = when {
                    artistHint.isNotBlank() && !artistHint.equals("CI Artist", ignoreCase = true) -> artistHint
                    categoryArtist.isNotBlank() -> categoryArtist
                    parsedArtist.isNotBlank() && !parsedArtist.equals("CI Artist", ignoreCase = true) -> parsedArtist
                    else -> "CI Artist"
                }

                val (artist, title) = normalizeFeatArtistAndTitle(rawArtist, rawTitle)
                val cleanTitleBase = title.replace("(?i)\\s*\\((?:feat\\.?|ft\\.?|featuring)\\s+[^)]+\\)".toRegex(), "").trim()
                val titleSlug = catalogSlug(cleanTitleBase)
                val artistSlug = catalogSlug(artist)

                // Distinct chords
                val distinctChords = mutableListOf<String>()
                val matcher = CHORD_BRACKET_PATTERN.matcher(finalContent)
                while (matcher.find()) {
                    val chord = matcher.group(1)?.trim() ?: continue
                    if (chord.isNotEmpty() && !distinctChords.contains(chord)) {
                        distinctChords.add(chord)
                    }
                }

                val hash = sha1Hex("$artistSlug:$titleSlug:ci")
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                CatalogSongEntity(
                    id = null,
                    source = "CI",
                    sourceUrl = url,
                    title = title,
                    artist = artist,
                    titleSlug = titleSlug,
                    artistSlug = artistSlug,
                    versionName = "CI Standard",
                    rating = 4.8f,
                    votes = 50,
                    keyBase = distinctChords.firstOrNull(),
                    difficulty = "Novice",
                    category = "INDONESIA",
                    region = "INDONESIA",
                    chords = distinctChords.joinToString(","),
                    chordCount = distinctChords.size,
                    lineCount = cleanLines.size,
                    content = finalContent,
                    contentHash = hash,
                    scrapedAt = nowStr
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun fetchSheetPreview(url: String, artistHint: String = "", titleHint: String = ""): CatalogSongEntity? =
        scrapeUrl(url, artistHint = artistHint, titleHint = titleHint)

    /**
     * Scrape all songs by an artist from CI.
     */
    suspend fun scrapeArtistAllSongs(
        artistQuery: String,
        onProgress: (current: Int, total: Int, songTitle: String) -> Unit = { _, _, _ -> }
    ): List<CatalogSongEntity> =
        withContext(Dispatchers.IO) {
            val trimmed = artistQuery.trim()
            if (trimmed.isEmpty()) return@withContext emptyList()

            val artistSlug = catalogSlug(trimmed)
            val foldedSlug = catalogSlug(com.chordapp.catalog.foldDiacritics(trimmed))
            var artistUrl = "https://www.chordindonesia.com/chord/$artistSlug"
            var html = fetchHtml(artistUrl)
            if (html == null && foldedSlug != artistSlug) {
                artistUrl = "https://www.chordindonesia.com/chord/$foldedSlug"
                html = fetchHtml(artistUrl)
            }
            if (html == null) return@withContext emptyList()

            val doc = Jsoup.parse(html)
            val songUrls = mutableListOf<String>()
            val seen = mutableSetOf<String>()

            for (a in doc.select("a[href]")) {
                val href = a.attr("href").trim()
                val text = a.text().trim()
                if (href.startsWith("https://www.chordindonesia.com/") && text.length > 2) {
                    if (!href.contains("/chord/") &&
                        !href.contains("/kumpulan-kunci-gitar/") &&
                        !href.contains("/category/") &&
                        !href.contains("/tag/") &&
                        !href.contains("/page/") &&
                        !href.contains("#") &&
                        !href.contains("tentang-kami") &&
                        !href.contains("contact") &&
                        !href.contains("disclaimer") &&
                        !href.contains("privacy") &&
                        !href.contains("transpose") &&
                        !href.contains("tuner") &&
                        !href.contains("daftar-isi") &&
                        !href.contains("0-9") &&
                        href != "https://www.chordindonesia.com/" &&
                        href != "https://www.chordindonesia.com"
                    ) {
                        if (seen.add(href)) {
                            songUrls.add(href)
                        }
                    }
                }
            }

            val scraped = mutableListOf<CatalogSongEntity>()
            val total = songUrls.size
            for ((index, songUrl) in songUrls.withIndex()) {
                val song = scrapeUrl(songUrl, artistHint = trimmed)
                if (song != null) {
                    scraped.add(song)
                    onProgress(index + 1, total, song.title)
                }
            }

            scraped
        }

    private fun normalizeText(s: String): String {
        val nfd = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return nfd.replace("\\p{M}".toRegex(), "").trim()
    }

    /**
     * Memindahkan part kolaborasi (feat. / ft. / featuring) dari nama artis ke belakang judul lagu.
     * Contoh:
     *   artist: "Gloria Jessica feat Ade Govinda", title: "Terbelah Jadi Dua"
     *   -> artist: "Gloria Jessica", title: "Terbelah Jadi Dua (feat. Ade Govinda)"
     */
    fun normalizeFeatArtistAndTitle(rawArtist: String, rawTitle: String): Pair<String, String> {
        var cleanArtist = rawArtist.trim()
        var cleanTitle = rawTitle.trim()

        // Deteksi pola feat / ft / featuring di nama artis
        val featRegex = "(?i)\\s+(?:feat\\.?|ft\\.?|featuring)\\s+(.*)".toRegex()
        val match = featRegex.find(cleanArtist)
        if (match != null) {
            val featuredArtist = match.groupValues[1].trim().trimEnd('.', ')')
            cleanArtist = cleanArtist.substring(0, match.range.first).trim()
            
            // Tambahkan ke judul lagu jika di judul belum ada tag feat
            if (featuredArtist.isNotEmpty() && !cleanTitle.contains("(?i)\\bfeat".toRegex())) {
                cleanTitle = "$cleanTitle (feat. $featuredArtist)"
            }
        }

        return Pair(cleanArtist, cleanTitle)
    }

    fun parseArtistAndTitleFromH1(h1: String, fallbackArtist: String = ""): Pair<String, String> {
        // Bersihkan prefix umum: "Chord Kunci Gitar", "Chord Gitar", "Kunci Gitar", "Chord"
        var clean = h1.replace("(?i)^(?:Chord\\s+Kunci\\s+Gitar|Chord\\s+Gitar|Kunci\\s+Gitar|Chord)\\s+".toRegex(), "").trim()
        
        // Bersihkan suffix umum: "Kunci Gitar untuk Pemula", "Chord Mudah", "Kunci Dasar", "Chord"
        clean = clean.replace("(?i)\\s+(?:Kunci\\s+Gitar.*|Chord\\s+Mudah.*|Kunci\\s+Dasar.*|Chord.*)$".toRegex(), "").trim()

        val (rawArtist, rawTitle) = if (clean.contains(" - ")) {
            val parts = clean.split(" - ", limit = 2)
            Pair(normalizeText(parts[0]), normalizeText(parts[1]))
        } else if (fallbackArtist.isNotBlank() && clean.startsWith(fallbackArtist, ignoreCase = true)) {
            val rem = clean.substring(fallbackArtist.length).trim().removePrefix("-").trim()
            Pair(normalizeText(fallbackArtist), normalizeText(rem))
        } else {
            Pair(normalizeText(fallbackArtist), normalizeText(clean))
        }

        return normalizeFeatArtistAndTitle(rawArtist, rawTitle)
    }

    fun parseArtistAndTitleFromText(text: String, fallbackArtist: String): Pair<String, String> {
        val (rawArtist, rawTitle) = if (text.contains(" - ")) {
            val parts = text.split(" - ", limit = 2)
            Pair(normalizeText(parts[0]), normalizeText(parts[1]))
        } else if (fallbackArtist.isNotBlank() && text.startsWith(fallbackArtist, ignoreCase = true)) {
            val rem = text.substring(fallbackArtist.length).trim().removePrefix("-").trim()
            Pair(normalizeText(fallbackArtist), normalizeText(rem))
        } else {
            Pair(normalizeText(fallbackArtist), normalizeText(text))
        }

        return normalizeFeatArtistAndTitle(rawArtist, rawTitle)
    }

    private fun cleanTitleFromUrl(url: String): String {
        val lastSegment = url.trimEnd('/').substringAfterLast('/')
        return lastSegment.replace('-', ' ').split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun fetchHtml(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            conn.instanceFollowRedirects = true

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun sha1Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
