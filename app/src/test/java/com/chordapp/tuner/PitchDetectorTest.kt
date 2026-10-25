package com.chordapp.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class PitchDetectorTest {

    private lateinit var detector: PitchDetector

    @Before
    fun setUp() {
        detector = PitchDetector()
    }

    @Test
    fun `test B string (string 2) matches B3 and NEVER matches E2 string 6`() {
        val strings = TunerPresets.GUITAR_STRINGS
        val bStringFreq = 246.94f // String 2 (B3)

        val match = detector.resolveStringMatch(
            pitch = bStringFreq,
            activeStrings = strings,
            isAuto = true,
            manualTarget = null
        )

        assertNotNull("Match should not be null", match)
        assertEquals("Should match string number 2 (B3)", 2, match!!.targetString.stringNumber)
        assertEquals("Should match note B", "B", match.targetString.noteName)
        assertNotEquals("Must NEVER match string number 6 (E2)", 6, match.targetString.stringNumber)
        assertEquals(0f, match.centsDeviation, 0.5f)
    }

    @Test
    fun `test B string subharmonic 123Hz and 2nd harmonic 494Hz fold cleanly to string 2 without jumping`() {
        val strings = TunerPresets.GUITAR_STRINGS
        val bString = strings.find { it.stringNumber == 2 }!!

        // 1. Subharmonic (123.47 Hz)
        val subMatch = detector.resolveStringMatch(
            pitch = 123.47f,
            activeStrings = strings,
            isAuto = true,
            manualTarget = bString
        )
        assertNotNull(subMatch)
        assertEquals("Subharmonic must match string 2", 2, subMatch!!.targetString.stringNumber)
        assertEquals(0f, subMatch.centsDeviation, 0.5f)
        assertEquals(246.94f, subMatch.normalizedFrequency, 1.0f)

        // 2. 2nd Harmonic (493.88 Hz)
        val harm2Match = detector.resolveStringMatch(
            pitch = 493.88f,
            activeStrings = strings,
            isAuto = true,
            manualTarget = bString
        )
        assertNotNull(harm2Match)
        assertEquals("2nd harmonic must match string 2", 2, harm2Match!!.targetString.stringNumber)
        assertEquals(0f, harm2Match.centsDeviation, 0.5f)
        assertEquals(246.94f, harm2Match.normalizedFrequency, 1.0f)

        // 3. 3rd Harmonic transient (370.41 Hz = F# transient)
        val harm3Match = detector.resolveStringMatch(
            pitch = 370.41f,
            activeStrings = strings,
            isAuto = true,
            manualTarget = bString
        )
        assertNotNull(harm3Match)
        assertEquals("3rd harmonic transient must fold to string 2", 2, harm3Match!!.targetString.stringNumber)
        assertEquals(0f, harm3Match.centsDeviation, 1.0f)

        // 4. Detuned flat (-35 cents) at subharmonic, fundamental, and 2nd harmonic
        val bFundFlat = 246.94f * (2.0.pow(-35.0 / 1200.0)).toFloat() // ~241.99 Hz
        val bSubFlat = bFundFlat / 2f                                  // ~121.00 Hz
        val bHarmFlat = bFundFlat * 2f                                 // ~483.98 Hz

        val matchFund = detector.resolveStringMatch(bFundFlat, strings, true, bString)
        val matchSubFlat = detector.resolveStringMatch(bSubFlat, strings, true, bString)
        val matchHarmFlat = detector.resolveStringMatch(bHarmFlat, strings, true, bString)

        assertNotNull(matchFund)
        assertNotNull(matchSubFlat)
        assertNotNull(matchHarmFlat)

        assertEquals(-35f, matchFund!!.centsDeviation, 0.5f)
        assertEquals(-35f, matchSubFlat!!.centsDeviation, 0.5f)
        assertEquals(-35f, matchHarmFlat!!.centsDeviation, 0.5f)
    }

    @Test
    fun `test all 6 guitar strings match correctly at fundamental`() {
        val strings = TunerPresets.GUITAR_STRINGS
        val expected = mapOf(
            82.41f to 6,   // E2
            110.00f to 5,  // A2
            146.83f to 4,  // D3
            196.00f to 3,  // G3
            246.94f to 2,  // B3
            329.63f to 1   // E4
        )

        for ((freq, expectedStrNum) in expected) {
            val match = detector.resolveStringMatch(
                pitch = freq,
                activeStrings = strings,
                isAuto = true,
                manualTarget = null
            )
            assertNotNull("Match for $freq Hz should not be null", match)
            assertEquals("Pitch $freq Hz should match string $expectedStrNum", expectedStrNum, match!!.targetString.stringNumber)
            assertEquals(0f, abs(match.centsDeviation), 1.0f)
        }
    }

    @Test
    fun `test B string detuned flat and sharp still matches string 2`() {
        val strings = TunerPresets.GUITAR_STRINGS
        val bFreq = 246.94f

        // -30 cents flat
        val flatPitch = bFreq * (2.0.pow(-30.0 / 1200.0)).toFloat()
        val flatMatch = detector.resolveStringMatch(
            pitch = flatPitch,
            activeStrings = strings,
            isAuto = true,
            manualTarget = null
        )
        assertNotNull(flatMatch)
        assertEquals(2, flatMatch!!.targetString.stringNumber)
        assertEquals(-30f, flatMatch.centsDeviation, 1.0f)

        // +30 cents sharp
        val sharpPitch = bFreq * (2.0.pow(30.0 / 1200.0)).toFloat()
        val sharpMatch = detector.resolveStringMatch(
            pitch = sharpPitch,
            activeStrings = strings,
            isAuto = true,
            manualTarget = null
        )
        assertNotNull(sharpMatch)
        assertEquals(2, sharpMatch!!.targetString.stringNumber)
        assertEquals(30f, sharpMatch.centsDeviation, 1.0f)
    }

    @Test
    fun `test bass 4-strings resolve accurately`() {
        val bassStrings = TunerPresets.BASS_STRINGS
        val expected = mapOf(
            41.20f to 4,  // E1
            55.00f to 3,  // A1
            73.42f to 2,  // D2
            98.00f to 1   // G2
        )

        for ((freq, expectedStrNum) in expected) {
            val match = detector.resolveStringMatch(
                pitch = freq,
                activeStrings = bassStrings,
                isAuto = true,
                manualTarget = null
            )
            assertNotNull("Bass $freq Hz should match", match)
            assertEquals(expectedStrNum, match!!.targetString.stringNumber)
        }
    }

    @Test
    fun `test ukulele strings resolve accurately`() {
        val ukeStrings = TunerPresets.UKULELE_STRINGS
        val expected = mapOf(
            392.00f to 4, // G4
            261.63f to 3, // C4
            329.63f to 2, // E4
            440.00f to 1  // A4
        )

        for ((freq, expectedStrNum) in expected) {
            val match = detector.resolveStringMatch(
                pitch = freq,
                activeStrings = ukeStrings,
                isAuto = true,
                manualTarget = null
            )
            assertNotNull("Ukulele $freq Hz should match", match)
            assertEquals(expectedStrNum, match!!.targetString.stringNumber)
        }
    }

    @Test
    fun `test low E string 6 harmonics fold to string 6 and never confuse with string 2 or string 1`() {
        val strings = TunerPresets.GUITAR_STRINGS
        val eString = strings.find { it.stringNumber == 6 }!!

        // 2nd Harmonic (164.82 Hz)
        val harm2 = detector.resolveStringMatch(164.82f, strings, isAuto = true, manualTarget = eString)
        assertNotNull(harm2)
        assertEquals(6, harm2!!.targetString.stringNumber)
        assertEquals(82.41f, harm2.normalizedFrequency, 0.5f)
        assertEquals(0f, harm2.centsDeviation, 0.5f)

        // 3rd Harmonic (247.23 Hz) - identical to String 2 B3 frequency, must stay on String 6!
        val harm3 = detector.resolveStringMatch(247.23f, strings, isAuto = true, manualTarget = eString)
        assertNotNull(harm3)
        assertEquals(6, harm3!!.targetString.stringNumber)
        assertEquals(82.41f, harm3.normalizedFrequency, 0.5f)
        assertEquals(0f, harm3.centsDeviation, 0.5f)

        // 4th Harmonic (329.64 Hz) - identical to String 1 E4 frequency, must stay on String 6!
        val harm4 = detector.resolveStringMatch(329.64f, strings, isAuto = true, manualTarget = eString)
        assertNotNull(harm4)
        assertEquals(6, harm4!!.targetString.stringNumber)
        assertEquals(82.41f, harm4.normalizedFrequency, 0.5f)
        assertEquals(0f, harm4.centsDeviation, 0.5f)
    }

    @Test
    fun `test low A string 5 3rd harmonic 330Hz folds to string 5 and never jumps to string 1`() {
        val strings = TunerPresets.GUITAR_STRINGS
        val aString = strings.find { it.stringNumber == 5 }!!

        val harm3 = detector.resolveStringMatch(330.00f, strings, isAuto = true, manualTarget = aString)
        assertNotNull(harm3)
        assertEquals(5, harm3!!.targetString.stringNumber)
        assertEquals(110.00f, harm3.normalizedFrequency, 0.5f)
        assertEquals(0f, harm3.centsDeviation, 0.5f)
    }
}
