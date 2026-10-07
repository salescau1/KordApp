package com.chordapp.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Parses an inline snapshot of the chords-db "guitar.json" shape (org.json only
 * works under Robolectric) and checks lookup: literal roots, enharmonic
 * fallback, slash-bass dropping, and honest null for missing symbols.
 */
@RunWith(RobolectricTestRunner::class)
class ChordFingeringDbTest {

    private fun db(): ChordFingeringDb {
        val json = """
        {"main":{"chords":{
          "C":[{"key":"C","suffix":"major","positions":[{"frets":[-1,3,2,0,1,0],"fingers":[0,3,2,0,1,0],"baseFret":1,"barres":[]}]}],
          "Bb":[{"key":"Bb","suffix":"major","positions":[{"frets":[-1,-1,3,3,3,1],"fingers":[0,0,3,2,1,1],"baseFret":1,"barres":[1]}]}],
          "D":[{"key":"D","suffix":"major","positions":[{"frets":[-1,-1,0,2,3,2],"fingers":[0,0,0,2,3,1],"baseFret":1,"barres":[]}]},
                {"key":"D","suffix":"minor","positions":[{"frets":[-1,-1,0,2,3,1],"fingers":[0,0,0,2,3,1],"baseFret":1,"barres":[]}]}]
        }}}
        """.trimIndent()
        return ChordFingeringDb.fromJson(json)
    }

    @Test
    fun `parse splits root and suffix with accidental and slash bass`() {
        val m7 = ChordSymbol.parse("C#m7")
        assertNotNull(m7)
        assertEquals("C#", m7!!.root)
        assertEquals("m7", m7.suffix)

        val slash = ChordSymbol.parse("D/F#")
        assertNotNull(slash)
        assertEquals("D", slash!!.root)
        assertEquals("", slash.suffix)

        val flat = ChordSymbol.parse("Bb")
        assertNotNull(flat)
        assertEquals("Bb", flat!!.root)
        assertEquals("", flat.suffix)

        assertNull(ChordSymbol.parse("   "))
        assertNull(ChordSymbol.parse(""))
    }

    @Test
    fun `literal root returns its stored shape`() {
        val c = db().find("C")
        assertNotNull(c)
        assertEquals(listOf(-1, 3, 2, 0, 1, 0), c!!.frets)
    }

    @Test
    fun `empty suffix resolves to the stored major shape`() {
        val d = db().find("D")
        assertNotNull(d)
        assertEquals(listOf(-1, -1, 0, 2, 3, 2), d!!.frets)
    }

    @Test
    fun `slash bass diagrams the main chord only`() {
        val db = db()
        assertSame("D/F# must chart the same D shape as D", db.find("D"), db.find("D/F#"))
    }

    @Test
    fun `slash chord prefers the exact over-bass voicing when the asset stores it`() {
        val json = """
        {"chords":{
          "D":[
            {"key":"D","suffix":"major","positions":[{"frets":[-1,-1,0,2,3,2],"baseFret":1,"barres":[]}]},
            {"key":"D","suffix":"/F#","positions":[{"frets":[2,-1,0,2,3,2],"baseFret":1,"barres":[]}]},
            {"key":"D","suffix":"m/F","positions":[{"frets":[1,-1,0,2,3,1],"baseFret":1,"barres":[]}]}
          ]
        }}
        """.trimIndent()
        val db = ChordFingeringDb.fromJson(json)
        // D/F#: the real over-bass shape (low string fretted), not plain D major.
        assertEquals(listOf(2, -1, 0, 2, 3, 2), db.find("D/F#")!!.frets)
        // Minor slash: Dm/F resolves via the "m/F" suffix key.
        assertEquals(listOf(1, -1, 0, 2, 3, 1), db.find("Dm/F")!!.frets)
    }

    @Test
    fun `slash chord falls back to the main chord when the exact voicing is absent`() {
        val json = """
        {"chords":{
          "D":[{"key":"D","suffix":"major","positions":[{"frets":[-1,-1,0,2,3,2],"baseFret":1,"barres":[]}]}]
        }}
        """.trimIndent()
        val db = ChordFingeringDb.fromJson(json)
        // No "/A" suffix stored -> chart the main D shape rather than nothing.
        assertEquals(db.find("D"), db.find("D/A"))
    }

    @Test
    fun `slash bass enharmonic spelling still resolves`() {
        // Asset stores "/F#"; a sheet writing the enharmonic "/Gb" must still hit it.
        val json = """
        {"chords":{
          "D":[{"key":"D","suffix":"/F#","positions":[{"frets":[2,-1,0,2,3,2],"baseFret":1,"barres":[]}]}]
        }}
        """.trimIndent()
        val db = ChordFingeringDb.fromJson(json)
        assertEquals(listOf(2, -1, 0, 2, 3, 2), db.find("D/Gb")!!.frets)
    }

    @Test
    fun `sparse slash voicing is skipped in favor of the fuller main chord`() {
        // E/G# in chords-db is [-1,-1,-1,1,0,0] — only 3 strings, reads as a lone
        // fret. The DB must skip it and chart the full E major shape instead.
        val json = """
        {"chords":{
          "E":[
            {"key":"E","suffix":"major","positions":[{"frets":[0,2,2,1,0,0],"baseFret":1,"barres":[]}]},
            {"key":"E","suffix":"/G#","positions":[{"frets":[-1,-1,-1,1,0,0],"baseFret":1,"barres":[]}]}
          ]
        }}
        """.trimIndent()
        val db = ChordFingeringDb.fromJson(json)
        assertEquals(listOf(0, 2, 2, 1, 0, 0), db.find("E/G#")!!.frets)
    }

    @Test
    fun `enharmonic root falls back to the stored spelling`() {
        // Asset stores Bb; a sharp-spelled A# must still resolve.
        val aSharp = db().find("A#")
        assertNotNull(aSharp)
        assertEquals(listOf(-1, -1, 3, 3, 3, 1), aSharp!!.frets)
    }

    @Test
    fun `m short alias resolves to the minor shape`() {
        val dm = db().find("Dm")
        assertNotNull(dm)
        assertEquals(listOf(-1, -1, 0, 2, 3, 1), dm!!.frets)
    }

    @Test
    fun `absent chord returns null instead of a made-up diagram`() {
        assertNull(db().find("C7"))
        assertNull(db().find("F#m"))
        assertNull(db().find("E/B"))
    }

    @Test
    fun `flattened two-level asset shape is tolerated`() {
        val flat = ChordFingeringDb.fromJson(
            """{"C":[{"key":"C","suffix":"major","positions":[{"frets":[-1,3,2,0,1,0]}]}]}"""
        )
        val c = flat.find("C")
        assertNotNull(c)
        assertTrue(c!!.frets.size == 6)
        assertEquals(1, c.baseFret) // default when absent
    }

    @Test
    fun bestPositionRanksOpenShapeAboveShiftedBarre() {
        // Asset order is by baseFret: a shifted barre can come first. The DB
        // must return the open shape as the primary diagram.
        val json = """{"main":{"chords":{"C":[{"key":"C","suffix":"major","positions":[
          {"frets":[-1,3,3,3,3,1],"fingers":[0,2,3,4,3,3],"baseFret":3,"barres":[1]},
          {"frets":[-1,3,2,0,1,0],"fingers":[0,3,2,0,1,0],"baseFret":1,"barres":[]}
        ]}]}}}""".trimIndent()
        val db = ChordFingeringDb.fromJson(json)
        val all = db.findPositions("C")
        assertEquals(2, all.size)
        assertEquals(listOf(-1, 3, 2, 0, 1, 0), db.find("C")!!.frets)
        assertEquals(all.first(), db.find("C"))
    }

    @Test
    fun findPositionsRanksLowestFretFirstWhenNoOpenShape() {
        // All shifted: prefer the lowest baseFret, and among equal-fret copies
        // the barre-free shape must outrank the barre one.
        val json = """{"main":{"chords":{"A":[{"key":"A","suffix":"minor","positions":[
          {"frets":[-1,-1,3,4,4,3],"fingers":[0,0,2,3,4,3],"baseFret":5,"barres":[1]},
          {"frets":[-1,-1,3,4,4,3],"fingers":[0,0,2,3,4,3],"baseFret":7,"barres":[1]},
          {"frets":[-1,-1,2,2,2,2],"fingers":[0,0,2,3,2,2],"baseFret":5,"barres":[]}
        ]}]}}}""".trimIndent()
        val db = ChordFingeringDb.fromJson(json)
        val all = db.findPositions("Am")
        assertEquals(3, all.size)
        assertEquals(listOf(-1, -1, 2, 2, 2, 2), db.find("Am")!!.frets)
        assertEquals(listOf(5, 5, 7), all.map { it.baseFret })
    }

    @Test
    fun realAssetShapeTopLevelChordsParses() {
        // Mirrors chords_guitar.json: main is only metadata; the root->array map
        // is at TOP level under chords. Regression for the diagram-not-loading bug
        // where fromJson looked inside main and built an empty map.
        val db = ChordFingeringDb.fromJson(
            """{"main":{"name":"guitar"},"chords":{"C":[{"key":"C","suffix":"major","positions":[{"frets":[-1,3,2,0,1,0],"baseFret":1}]}]}}"""
        )
        val c = db.find("C")
        assertNotNull(c)
        assertEquals(listOf(-1, 3, 2, 0, 1, 0), c!!.frets)
        assertNull(db.find("C7"))
    }

    @Test
    fun sharpAndFlatSpellingsResolveToStoredSharpRoot() {
        // The asset stores F# under Fsharp and has no Db/Gb keys, so F#m (sheet)
        // and Gbm (enharmonic) must both resolve to the Fsharp minor shape.
        val db = ChordFingeringDb.fromJson(
            """{"chords":{"Fsharp":[{"key":"Fsharp","suffix":"minor","positions":[{"frets":[-1,-1,3,3,3,2],"fingers":[0,0,3,2,1,4],"baseFret":2,"barres":[3]}]}]}}"""
        )
        val fSharpM = db.find("F#m")
        assertNotNull(fSharpM)
        assertEquals(listOf(-1, -1, 3, 3, 3, 2), fSharpM!!.frets)
        // Gb minor is the same pitch as F# minor -> same stored shape.
        assertEquals(fSharpM, db.find("Gbm"))
    }
}
