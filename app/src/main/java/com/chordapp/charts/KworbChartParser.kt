package com.chordapp.charts

/**
 * Parser for kworb country chart HTML (Spotify country-daily and YouTube country
 * insights share the same row shape). Pure Kotlin (no Android deps) so it is
 * unit-testable on the JVM. Each data row is
 *
 *   <tr>
 *     <td class="np">1</td>                      rank
 *     <td class="np">=</td>                       movement (ignored)
 *     <td class="text mp"><div>Artist - Title</div></td>   (anchors optional)
 *     ...
 *   </tr>
 *
 * Rows that don't match this shape are skipped; if nothing parses, an empty list is
 * returned (the caller treats that as a failure and falls back to cache/no-data —
 * never to invented rankings).
 */
object KworbChartParser {

    private val ROW = Regex("<tr>(.*?)</tr>", setOf(RegexOption.DOT_MATCHES_ALL))
    private val RANK = Regex("""<td[^>]*class="np"[^>]*>(\d+)</td>""")
    private val TITLE_CELL =
        Regex("""<td[^>]*class="text mp"[^>]*>(.*?)</td>""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val TAG = Regex("<[^>]+>")
    private const val SEPARATOR = " - "
    private const val TOP_N = 50

    fun parse(html: String): List<ChartEntry> {
        val out = ArrayList<ChartEntry>(TOP_N)
        for (row in ROW.findAll(html)) {
            if (out.size >= TOP_N) break
            val rank = RANK.find(row.value)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val cell = TITLE_CELL.find(row.value)?.groupValues?.get(1) ?: continue
            val text = decodeEntities(TAG.replace(cell, "")).trim()
            if (text.isEmpty()) continue
            val sep = text.indexOf(SEPARATOR)
            if (sep <= 0) continue
            val artist = text.substring(0, sep).trim()
            val title = text.substring(sep + SEPARATOR.length).trim()
            if (artist.isEmpty() || title.isEmpty()) continue
            out.add(ChartEntry(rank = rank, title = title, artist = artist))
        }
        return out.sortedBy { it.rank }.take(TOP_N)
    }

    /** Decodes the few HTML entities that can appear in titles/artists. */
    private fun decodeEntities(s: String): String = s
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
}
