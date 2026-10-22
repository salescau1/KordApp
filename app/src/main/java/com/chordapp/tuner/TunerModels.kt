package com.chordapp.tuner

enum class TunerInstrumentType(val displayName: String, val subtitle: String) {
    GUITAR_6("Guitar 6-string", "Standard"),
    BASS_4("Bass 4-string", "Standard"),
    UKULELE("Ukulele", "Standard")
}

data class TunerString(
    val stringIndex: Int, // 0-based
    val stringNumber: Int, // 1 to 6
    val noteName: String,
    val octave: Int,
    val frequencyHz: Float,
    val isLeft: Boolean,
    val verticalOrder: Int // 0 is top, 1 is middle, 2 is bottom
)

object TunerPresets {
    val GUITAR_STRINGS = listOf(
        // Left side (top to bottom: D4, A5, E6)
        TunerString(stringIndex = 3, stringNumber = 4, noteName = "D", octave = 3, frequencyHz = 146.83f, isLeft = true, verticalOrder = 0),
        TunerString(stringIndex = 4, stringNumber = 5, noteName = "A", octave = 2, frequencyHz = 110.00f, isLeft = true, verticalOrder = 1),
        TunerString(stringIndex = 5, stringNumber = 6, noteName = "E", octave = 2, frequencyHz = 82.41f, isLeft = true, verticalOrder = 2),
        // Right side (top to bottom: G3, B2, E1)
        TunerString(stringIndex = 2, stringNumber = 3, noteName = "G", octave = 3, frequencyHz = 196.00f, isLeft = false, verticalOrder = 0),
        TunerString(stringIndex = 1, stringNumber = 2, noteName = "B", octave = 3, frequencyHz = 246.94f, isLeft = false, verticalOrder = 1),
        TunerString(stringIndex = 0, stringNumber = 1, noteName = "E", octave = 4, frequencyHz = 329.63f, isLeft = false, verticalOrder = 2)
    )

    val BASS_STRINGS = listOf(
        // Left side
        TunerString(stringIndex = 2, stringNumber = 3, noteName = "A", octave = 1, frequencyHz = 55.00f, isLeft = true, verticalOrder = 0),
        TunerString(stringIndex = 3, stringNumber = 4, noteName = "E", octave = 1, frequencyHz = 41.20f, isLeft = true, verticalOrder = 1),
        // Right side
        TunerString(stringIndex = 1, stringNumber = 2, noteName = "D", octave = 2, frequencyHz = 73.42f, isLeft = false, verticalOrder = 0),
        TunerString(stringIndex = 0, stringNumber = 1, noteName = "G", octave = 2, frequencyHz = 98.00f, isLeft = false, verticalOrder = 1)
    )

    val UKULELE_STRINGS = listOf(
        // Left side
        TunerString(stringIndex = 2, stringNumber = 3, noteName = "C", octave = 4, frequencyHz = 261.63f, isLeft = true, verticalOrder = 0),
        TunerString(stringIndex = 3, stringNumber = 4, noteName = "G", octave = 4, frequencyHz = 392.00f, isLeft = true, verticalOrder = 1),
        // Right side
        TunerString(stringIndex = 1, stringNumber = 2, noteName = "E", octave = 4, frequencyHz = 329.63f, isLeft = false, verticalOrder = 0),
        TunerString(stringIndex = 0, stringNumber = 1, noteName = "A", octave = 4, frequencyHz = 440.00f, isLeft = false, verticalOrder = 1)
    )

    fun getStringsFor(type: TunerInstrumentType): List<TunerString> {
        return when (type) {
            TunerInstrumentType.GUITAR_6 -> GUITAR_STRINGS
            TunerInstrumentType.BASS_4 -> BASS_STRINGS
            TunerInstrumentType.UKULELE -> UKULELE_STRINGS
        }
    }
}

data class PitchReading(
    val frequencyHz: Float = 0f,
    val noteName: String = "",
    val octave: Int = 0,
    val centsDeviation: Float = 0f,
    val isInTune: Boolean = false,
    val isSilent: Boolean = true,
    val matchedString: TunerString? = null
)
