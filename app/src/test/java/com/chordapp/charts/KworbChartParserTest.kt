package com.chordapp.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the shared kworb chart parser (Spotify country-daily and
 * YouTube country insights use the same row shape).
 *
 * Rows look like
 *   <tr><td class="np">1</td><td class="np">=</td><td class="text mp"><div>Artist - Title</div></td>...</tr>
 * with the artist/title anchors being optional. Nothing is invented when the page
 * doesn't match — malformed rows are skipped and an empty result is returned
 * (callers treat that as failure, never as fake rankings).
 */
class KworbChartParserTest {

    /** A realistic Spotify-style snippet (3 real rows + 2 rows that must be skipped). */
    private val sample = """
        <html><body>
        <table>
        <tr><td class="np">Pos</td><td class="np">Mov</td><td class="text mp">Song</td><td class="text">Streams</td></tr>
        <tr><td class="np">2</td><td class="np">new</td><td class="text mp"><div><a href="/a/tulus">Tulus &amp; Friends</a> - <a href="/s/hati">Hati-Hati di Jalan</a></div></td><td class="text">999</td></tr>
        <tr><td class="np">1</td><td class="np">=</td><td class="text mp"><div><a href="/a/dewa19">Dewa 19</a> - <a href="/s/kirana">Kirana</a></div></td><td class="text">1200</td></tr>
        <tr><td class="np">3</td><td class="np">-1</td><td class="text mp"><div><a href="/a/noah">Noah</a> - <a href="/s/kau">Kau &#39;Kan Datang</a></div></td><td class="text">800</td></tr>
        <tr><td class="np">junk</td><td class="np">=</td><td class="text mp"><div><a href="/a/x">Bad Rank</a> - <a href="/s/y">Skipped</a></div></td></tr>
        <tr><td class="text mp"><div><a href="/a/y">No Rank Cell</a> - <a href="/s/z">Skipped Too</a></div></td></tr>
        </table>
        </body></html>
    """.trimIndent()

    @Test
    fun `parses rows sorted by rank and decodes entities`() {
        val entries = KworbChartParser.parse(sample)
        assertEquals(3, entries.size)

        assertEquals(1, entries[0].rank)
        assertEquals("Dewa 19", entries[0].artist)
        assertEquals("Kirana", entries[0].title)

        assertEquals(2, entries[1].rank)
        assertEquals("Tulus & Friends", entries[1].artist) // &amp; decoded
        assertEquals("Hati-Hati di Jalan", entries[1].title)

        assertEquals(3, entries[2].rank)
        assertEquals("Noah", entries[2].artist)
        assertEquals("Kau 'Kan Datang", entries[2].title) // &#39; decoded
    }

    @Test
    fun `parses YouTube-style rows without anchors`() {
        // kworb YouTube insights renders the track cell as a plain <div>Artist - Title</div>.
        val html = """
            <table>
            <tr><td class="np">1</td><td class="np">=</td><td class="text mp"><div>Tulus - Teh Hijau</div></td></tr>
            <tr><td class="np">2</td><td class="np">NEW</td><td class="text mp"><div>Ari Lesmana - Sesi Potret</div></td></tr>
            </table>
        """.trimIndent()
        val entries = KworbChartParser.parse(html)
        assertEquals(2, entries.size)
        assertEquals(ChartEntry(1, "Teh Hijau", "Tulus"), entries[0])
        assertEquals(ChartEntry(2, "Sesi Potret", "Ari Lesmana"), entries[1])
    }

    @Test
    fun `caps at top 50 and keeps ascending ranks`() {
        val many = buildString {
            append("<table>")
            // Deliberately shuffled so output order must come from sorting.
            for (i in 50 downTo 1) {
                append(
                    "<tr><td class=\"np\">$i</td><td class=\"np\">=</td>" +
                        "<td class=\"text mp\"><div><a href=\"/a/a$i\">Artist $i</a> - <a href=\"/s/s$i\">Song $i</a></div></td></tr>"
                )
            }
            append("</table>")
        }
        val entries = KworbChartParser.parse(many)
        assertEquals(50, entries.size)
        assertEquals((1..50).toList(), entries.map { it.rank })
    }

    @Test
    fun `empty or unparseable input yields empty list (never fabricates)`() {
        assertTrue(KworbChartParser.parse("").isEmpty())
        assertTrue(KworbChartParser.parse("<html><body>no table here</body></html>").isEmpty())
        assertTrue(KworbChartParser.parse("<tr><td>1</td><td>plain cells, wrong classes</td></tr>").isEmpty())
    }
}
