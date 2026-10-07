package com.chordapp.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Splash : Screen("splash", "KordApp")
    object Home : Screen("home", "Home")
    object Explore : Screen("explore", "Explore")
    object Setlists : Screen("setlists", "Setlists")
    object Favorites : Screen("favorites", "Favorites")
    object Settings : Screen("settings", "Settings")
    object About : Screen("about", "About Application")
    
    // Dedicated Search
    object Search : Screen("search", "Search")
    
    // Activation screen
    object Activation : Screen("activation", "Aktivasi Lisensi")
    
    // Detail screens (hides bottom nav)
    object ArtistDirectory : Screen("artist_directory/{artistSlug}", "Artist") {
        fun createRoute(artistSlug: String) = "artist_directory/$artistSlug"
    }
    object SongVersions : Screen("song_versions/{artistSlug}/{titleSlug}", "Versions") {
        fun createRoute(artistSlug: String, titleSlug: String) = "song_versions/$artistSlug/$titleSlug"
    }
    object Player : Screen("player/{sourceUrlHash}?transpose={transpose}&capo={capo}", "Player") {
        fun createRoute(sourceUrlHash: String, transpose: Int = 0, capo: Int = 0) =
            "player/$sourceUrlHash?transpose=$transpose&capo=$capo"
    }
}
