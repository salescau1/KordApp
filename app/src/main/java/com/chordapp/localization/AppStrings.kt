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
    val buyLicenseLabel: String
    val lostAccessLabel: String
    val resetKeyAction: String
    val resetKeyDialogTitle: String
    val resetKeyDialogDesc: String
    val sendResetVerificationBtn: String
    val enterResetOtpTitle: String
    val confirmResetBtn: String
    val recoveryEmailPlaceholder: String
    val resetSuccessMsg: String
    val bindEmailEduTitle: String
    val bindEmailEduDesc: String
    val bindEmailNowBtn: String
    val setupLaterBtn: String
    val bindEmailSuccess: String
    val pinSetupEduTitle: String
    val pinSetupEduDesc: String
    val savePinBtn: String
    val skipPinBtn: String
    val pinSavedToast: String
    val licensePinSecurityTitle: String
    val licensePinSecurityDesc: String
    val licensePinActiveStatus: String
    val licensePinInactiveStatus: String
    val setPinAction: String
    val changePinAction: String
    val enterPinToRevealTitle: String
    val enterPinToTransferTitle: String
    val enterPinToDisableTitle: String
    val enterCurrentPinPrompt: String
    val enterNewPinPrompt: String
    val wrongPinError: String
    val pinSetupSuccessToast: String
    val pinDisabledSuccessToast: String
    val saveAndLockLicenseBtn: String
    val recoveryEmailTitle: String
    val notLinkedStatus: String
    val linkAction: String
    val verifiedStatus: String
    val qrisDialogTitle: String
    val qrisDialogDesc: String

    // Band Live Sync (P2P Mode)
    val bandSection: String
    val bandModeServer: String
    val bandModeNormal: String
    val bandModeClient: String
    val bandServerName: String
    val bandPinProtection: String
    val bandPinPlaceholder: String
    val bandServerActive: String
    fun bandConnectedCount(count: Int): String
    val bandSearchingServers: String
    val bandNoServersFound: String
    val bandConnect: String
    val bandDisconnect: String
    fun bandConnectedTo(name: String): String
    val bandStandbyTitle: String
    val bandWaitingForLeader: String
    val bandEnterPinTitle: String
    val bandEnterPinPrompt: String
    val bandInvalidPin: String
    val bandOpenStandby: String
    val bandLiveBadge: String
    val bandSyncBadge: String

    // Settings & Customization Strings
    val musicAndCatalogSection: String
    val downloadCatalogLabel: String
    val themeLabel: String
    val languageLabel: String
    val revealKey: String
    val hideKey: String
    val transferLicense: String
    val transferLicenseDesc: String
    val copyKeyAction: String
    val checkUpdate: String
    val checkingUpdate: String
    val checkVersion: String
    fun appUpToDate(version: String): String
    val deleteAction: String
    val downloadAction: String
    val packageA: String
    val packageB: String
    val packageC: String
    fun packageSongsCount(count: String): String
    val tierCoreBadge: String
    val tierProBadge: String
    val tierArchiveBadge: String
    val tierInstalledBadge: String
    fun tierDownloadingBadge(percent: Int): String
    val tierBackgroundNote: String
    val tierSectionHint: String
    fun downloadingTierTitle(tier: String): String

    // Feedback & Suggestions
    val feedbackAndSuggestions: String
    val sendEmailAction: String
    val feedbackDialogTitle: String
    val feedbackFeatureCategory: String
    val feedbackBugCategory: String
    val feedbackGeneralCategory: String
    fun feedbackFeatureBody(version: String, device: String): String
    fun feedbackBugBody(version: String, device: String): String
    fun feedbackGeneralBody(version: String, device: String): String

    // Cooldown Note
    val resetCooldownNote: String

    // Additional Dialogs & Prompts
    val bandHostDetected: String
    val copyAction: String
    fun linkCopiedToast(url: String): String
    val keyCopiedToast: String
    val bindEmailDialogTitle: String
    val bindEmailDialogDesc: String
    val enter6DigitOtpPlaceholder: String
    val sendOtpBtn: String
    val verifyBtn: String
    fun deletePackageDialogTitle(target: String): String
    fun deletePackageDialogDesc(target: String): String
    val stageMirrorInstruction: String
    val stageMirrorTitle: String
    val stageMirrorOptionName: String
    val stageMirrorOptionIp: String
    val noEmailAppFound: String
    val transferDialogWarning: String
    val copyingAction: String
    val invalidEmailError: String
    fun otpSentToast(email: String): String
    val otpLengthError: String
    fun packageDeletedToast(target: String): String
    val updateAvailableTitle: String
    fun newVersionAvailable(version: String): String
    val updateAction: String
    val laterAction: String
    val downloadStartedToast: String
    val verifyAndContinueBtn: String
    val confirmToWhatsApp: String
}
