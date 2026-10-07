package com.chordapp.ui.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationTest {

    @Test
    fun `navigation rail starts at expanded width breakpoint`() {
        assertFalse(usesNavigationRail(839.dp))
        assertTrue(usesNavigationRail(840.dp))
        assertTrue(usesNavigationRail(1_200.dp))
    }

    @Test
    fun `primary navigation is hidden on detail routes`() {
        assertFalse(showsPrimaryNavigation(null))
        assertTrue(showsPrimaryNavigation(Screen.Home.route))
        assertTrue(showsPrimaryNavigation(Screen.Search.route))

        assertFalse(showsPrimaryNavigation("artist_directory/ed-sheeran"))
        assertFalse(showsPrimaryNavigation("song_versions/ed-sheeran/perfect"))
        assertFalse(showsPrimaryNavigation("player/source?transpose=0&capo=0"))
        assertFalse(showsPrimaryNavigation(Screen.About.route))
        assertFalse(showsPrimaryNavigation("tracklist/42"))
    }
}
