package com.chordapp.localization

interface AppStrings {
    // Navigation & Destinations
    val navHome: String
    val navExplore: String
    val navSetlists: String
    val navFavorites: String
    val navSettings: String
    val navSearch: String
    val navAbout: String
    val navActivation: String
    val navVersions: String
    val navArtist: String
    val navPlayer: String

    // Common Actions & Words
    val back: String
    val cancel: String
    val save: String
    val create: String
    val delete: String
    val retry: String
    val clearSearch: String
    val calculating: String
    val unavailable: String
    val all: String
    val undo: String
    val play: String
    val options: String
    val close: String

    // Home Screen
    val searchPlaceholderHome: String
    val searchButton: String
    val recentlyPlayedHeader: String
    val recentlyHeader1: String
    val recentlyHeader2: String
    val nothingPlayedYet: String
    val songsPlayedAppearHere: String
    val offlineDatabase: String
    val artists: String
    val songs: String
    val size: String
    val top50: String
    val chartsNeedInternetTitle: String
    val chartsNeedInternetDesc: String
    val removeFromFavorites: String
    val addToFavorites: String
    val notAvailable: String

    // Explore Screen
    val exploreTitle: String
    val exploreSubtitle: String
    fun artistsCount(count: Int): String
    val searchArtistPlaceholder: String
    val genresAndCategoriesSection: String
    val alphabeticalCatalogSection: String
    val selectCharacter: String
    val backToExplore: String
    val backToFranchises: String
    fun artistsFound(count: Int): String
    val searchOstPlaceholder: String
    val otherSoundtracksSection: String
    fun songsCount(count: Int): String
    fun chordVersionsCount(count: Int): String
    val allBack: String
    val searchResults: String
    fun noArtistsFound(query: String): String
    val tryDifferentArtistKeyword: String

    // Search Screen
    val searchTitle: String
    val searchPlaceholder: String
    val artistsHeader: String
    val songsHeader: String
    fun artistSongsBadge(count: Int): String
    fun noResultsFor(query: String): String

    // Favorites Screen
    val favoritesTitle: String
    fun savedSongsCount(count: Int): String
    val searchFavoritesPlaceholder: String
    val sortLatest: String
    val sortTitle: String
    val sortArtist: String
    val noFavoritesYet: String
    val noMatchingFavorites: String
    val alternateVersion: String
    val removedFromFavorites: String

    // Setlists Screen & Tracklist Screen
    val setlistsTitle: String
    val setlistsSubtitle: String
    val sortSetlists: String
    val sortAZ: String
    val sortPopular: String
    val searchSetlists: String
    val noSetlistsYet: String
    val noSetlistsFound: String
    val newSetlist: String
    val setlistName: String
    val createSetlist: String
    val songRemoved: String
    val renameSetlist: String
    val deleteSetlistConfirmTitle: String
    val deleteSetlistConfirmMessage: String
    val setlistOptions: String
    val noSongsInSetlist: String
    val dragToReorder: String
    val deleteSong: String

    // Player Screen & Chord Dialog
    fun addedToSetlist(name: String): String
    fun alreadyInSetlist(name: String): String
    val failedToAddToSetlist: String
    val addedToFavorites: String
    val failedToUpdateFavorites: String
    val addSongToSetlist: String
    val noSheetContent: String
    val addToSetlistDialogTitle: String
    val noSetlistsPrompt: String
    val createAndAdd: String
    fun diagramNotAvailable(symbol: String): String
    val previousShape: String
    val nextShape: String
    val addAnotherVersion: String

    // Song Versions Screen & Artist Directory
    val noVersionsFound: String
    fun versionsCount(count: Int): String
    fun keyLabel(key: String): String
    fun chordsCount(count: Int): String
    fun linesCount(count: Int): String
    val allSources: String
    val noSongsForArtist: String
    val filterSongs: String
    fun noSongsMatch(filter: String): String

    // Settings Screen
    val settingsTitle: String
    val settingsSubtitle: String
    val themeSection: String
    val appThemeLabel: String
    val selectTheme: String
    val themeDark: String
    val themeLight: String
    val themePaper: String
    val themeBlackboard: String
    val themeGreenboard: String
    val themeSystem: String
    val languageSection: String
    val appLanguageLabel: String
    val selectLanguage: String
    val offlineCatalogSection: String
    val catalogSize: String
    val licenseAndDeviceSection: String
    val licenseStatus: String
    val licenseActivePro: String
    val licenseInactive: String
    val licenseKey: String
    val transferLicenseToOtherPhone: String
    val transferLicenseTitle: String
    fun transferLicenseMessage(key: String): String
    val releaseLicenseAction: String
    val releasingLicense: String
    val deviceId: String
    val applicationSection: String
    val version: String
    val apkSize: String
    val aboutApp: String
    val offlineDisclaimer: String

    // About Screen
    fun versionWithCode(versionName: String, versionCode: Long): String

    // Activation Screen
    val activationHeader: String
    val enterLicenseKey: String
    val activationDesc: String
    val licenseKeyPlaceholder: String
    val deviceIdCopied: String
    val pleaseEnterLicenseKey: String
    val noLicenseKeyYet: String
    val contactAdmin: String
    val activateNow: String
    val verifying: String
    val unableToOpenBrowserOrWa: String
    fun waOrderMessage(deviceId: String): String
}
