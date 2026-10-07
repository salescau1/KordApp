package com.chordapp.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerPreferencesTest {
    private lateinit var context: Context
    private lateinit var preferences: PlayerPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("player_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        preferences = PlayerPreferences(context)
    }

    @After
    fun teardown() {
        context.getSharedPreferences("player_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultsAreFunctionalPlayerValues() {
        assertTrue(preferences.keepScreenAwake)
        assertEquals(1f, preferences.defaultScrollSpeed)
    }

    @Test
    fun valuesPersistAcrossInstances() {
        preferences.keepScreenAwake = false
        preferences.defaultScrollSpeed = 2.5f

        val restored = PlayerPreferences(context)
        assertFalse(restored.keepScreenAwake)
        assertEquals(2.5f, restored.defaultScrollSpeed)
    }

    @Test
    fun scrollSpeedIsClampedToPlayerBounds() {
        preferences.defaultScrollSpeed = 9f
        assertEquals(4f, PlayerPreferences(context).defaultScrollSpeed)

        preferences.defaultScrollSpeed = 0.1f
        assertEquals(0.5f, PlayerPreferences(context).defaultScrollSpeed)
    }
}
