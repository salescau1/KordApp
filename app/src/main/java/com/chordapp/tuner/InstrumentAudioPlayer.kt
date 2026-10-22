package com.chordapp.tuner

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.chordapp.R

class InstrumentAudioPlayer(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val soundMap = mutableMapOf<Int, Int>()

    init {
        // Pre-load all studio-recorded acoustic instrument samples
        loadSound(R.raw.guitar_e2)
        loadSound(R.raw.guitar_a2)
        loadSound(R.raw.guitar_d3)
        loadSound(R.raw.guitar_g3)
        loadSound(R.raw.guitar_b3)
        loadSound(R.raw.guitar_e4)

        loadSound(R.raw.uke_g4)
        loadSound(R.raw.uke_c4)
        loadSound(R.raw.uke_e4)
        loadSound(R.raw.uke_a4)

        loadSound(R.raw.bass_e1)
        loadSound(R.raw.bass_a1)
        loadSound(R.raw.bass_d2)
        loadSound(R.raw.bass_g2)

        loadSound(R.raw.in_tune_chime)
    }

    private fun loadSound(resId: Int) {
        try {
            val soundId = soundPool.load(context, resId, 1)
            soundMap[resId] = soundId
        } catch (e: Exception) {
            // Ignore load failure
        }
    }

    fun playStringNote(instrumentType: TunerInstrumentType, stringNumber: Int) {
        val (resId, rate) = when (instrumentType) {
            TunerInstrumentType.GUITAR_6 -> when (stringNumber) {
                6 -> Pair(R.raw.guitar_e2, 1.0f)
                5 -> Pair(R.raw.guitar_a2, 1.0f)
                4 -> Pair(R.raw.guitar_d3, 1.0f)
                3 -> Pair(R.raw.guitar_g3, 1.0f)
                2 -> Pair(R.raw.guitar_b3, 1.0f)
                1 -> Pair(R.raw.guitar_e4, 1.0f)
                else -> Pair(R.raw.guitar_e2, 1.0f)
            }
            TunerInstrumentType.BASS_4 -> when (stringNumber) {
                4 -> Pair(R.raw.bass_e1, 1.0f)
                3 -> Pair(R.raw.bass_a1, 1.0f)
                2 -> Pair(R.raw.bass_d2, 1.0f)
                1 -> Pair(R.raw.bass_g2, 1.0f)
                else -> Pair(R.raw.bass_e1, 1.0f)
            }
            TunerInstrumentType.UKULELE -> when (stringNumber) {
                4 -> Pair(R.raw.uke_g4, 1.0f)
                3 -> Pair(R.raw.uke_c4, 1.0f)
                2 -> Pair(R.raw.uke_e4, 1.0f)
                1 -> Pair(R.raw.uke_a4, 1.0f)
                else -> Pair(R.raw.uke_c4, 1.0f)
            }
        }

        val soundId = soundMap[resId] ?: return
        try {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, rate)
        } catch (e: Exception) {
            // Ignore playback failure
        }
    }

    fun playInTuneChime() {
        val soundId = soundMap[R.raw.in_tune_chime] ?: return
        try {
            soundPool.play(soundId, 1.0f, 1.0f, 2, 0, 1.0f)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
