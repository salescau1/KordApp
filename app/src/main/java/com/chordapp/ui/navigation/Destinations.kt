package com.chordapp.ui.navigation

import com.chordapp.localization.AppStrings

sealed class Screen(val route: String) {
    open fun title(strings: AppStrings): String = "KordApp"

    object Splash : Screen("splash") {
        override fun title(strings: AppStrings) = "KordApp"
    }
    object Home : Screen("home") {
        override fun title(strings: AppStrings) = strings.navHome
    }
    object Explore : Screen("explore") {
        override fun title(strings: AppStrings) = strings.navExplore
    }
    object Setlists : Screen("setlists") {
        override fun title(strings: AppStrings) = strings.navSetlists
    }
    object Favorites : Screen("favorites") {
        override fun title(strings: AppStrings) = strings.navFavorites
    }
    object Settings : Screen("settings") {
        override fun title(strings: AppStrings) = strings.navSettings
    }
    object About : Screen("about") {
        override fun title(strings: AppStrings) = strings.navAbout
    }
    
    // Dedicated Search
    object Search : Screen("search?query={query}&source={source}") {
        override fun title(strings: AppStrings) = strings.navSearch
        fun createRoute(query: String? = null, source: String? = null): String {
            val q = if (!query.isNullOrBlank()) "query=${java.net.URLEncoder.encode(query, "UTF-8")}" else null
            val s = if (!source.isNullOrBlank()) "source=${java.net.URLEncoder.encode(source, "UTF-8")}" else null
            val params = listOfNotNull(q, s).joinToString("&")
            return if (params.isNotEmpty()) "search?$params" else "search"
        }
    }
    
    // Activation screen
    object Activation : Screen("activation") {
        override fun title(strings: AppStrings) = strings.navActivation
    }
    
    // Detail screens (hides bottom nav)
    object ArtistDirectory : Screen("artist_directory/{artistSlug}") {
        override fun title(strings: AppStrings) = strings.navArtist
        fun createRoute(artistSlug: String) = "artist_directory/$artistSlug"
    }
    object SongVersions : Screen("song_versions/{artistSlug}/{titleSlug}") {
        override fun title(strings: AppStrings) = strings.navVersions
        fun createRoute(artistSlug: String, titleSlug: String) = "song_versions/$artistSlug/$titleSlug"
    }
    object Player : Screen("player/{sourceUrlHash}?transpose={transpose}&capo={capo}") {
        override fun title(strings: AppStrings) = strings.navPlayer
        fun createRoute(sourceUrlHash: String, transpose: Int = 0, capo: Int = 0) =
            "player/$sourceUrlHash?transpose=$transpose&capo=$capo"
    }
}
