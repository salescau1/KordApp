package com.chordapp.localization

object EnglishStrings : AppStrings {
    // Navigation & Destinations
    override val navHome = "Home"
    override val navExplore = "Explore"
    override val navSetlists = "Setlists"
    override val navFavorites = "Favorites"
    override val navSettings = "Settings"
    override val navSearch = "Search"
    override val navAbout = "About Application"
    override val navActivation = "License Activation"
    override val navVersions = "Versions"
    override val navArtist = "Artist"
    override val navPlayer = "Player"

    // Common Actions & Words
    override val back = "Back"
    override val cancel = "Cancel"
    override val save = "Save"
    override val create = "Create"
    override val delete = "Delete"
    override val retry = "Retry"
    override val clearSearch = "Clear search"
    override val calculating = "Calculating…"
    override val unavailable = "Unavailable"
    override val all = "All"
    override val undo = "Undo"
    override val play = "Play"
    override val options = "Options"
    override val close = "Close"

    // Home Screen
    override val searchPlaceholderHome = "Search songs, artists, or lyrics..."
    override val searchButton = "Search"
    override val recentlyPlayedHeader = "RECENTLY PLAYED"
    override val recentlyHeader1 = "RECENTLY"
    override val recentlyHeader2 = "PLAYED"
    override val nothingPlayedYet = "Nothing played yet"
    override val songsPlayedAppearHere = "Songs you play will appear here"
    override val offlineDatabase = "Offline Database"
    override val artists = "Artists"
    override val songs = "Songs"
    override val size = "Size"
    override val top50 = "TOP 50"
    override val chartsNeedInternetTitle = "Charts need an internet connection"
    override val chartsNeedInternetDesc = "The Top 50 appears once you're online. Until then, use Search to find songs and artists in the offline catalog."
    override val removeFromFavorites = "Remove from favorites"
    override val addToFavorites = "Add to favorites"
    override val notAvailable = "Not available"

    // Explore Screen
    override val exploreTitle = "EXPLORE"
    override val exploreSubtitle = "Artists & Music Genres"
    override fun artistsCount(count: Int) = "$count Artists"
    override val searchArtistPlaceholder = "Search artist or band name..."
    override val genresAndCategoriesSection = "EXPLORE GENRES & CATEGORIES"
    override val alphabeticalCatalogSection = "ALPHABETICAL CATALOG (# — Z)"
    override val selectCharacter = "Select character"
    override val backToExplore = "Back to Explore"
    override val backToFranchises = "Back to Franchises"
    override fun artistsFound(count: Int) = "$count artists found"
    override val searchOstPlaceholder = "Search soundtrack franchise or song..."
    override val otherSoundtracksSection = "OTHER SOUNDTRACK SONGS"
    override fun songsCount(count: Int) = "$count Songs"
    override fun chordVersionsCount(count: Int) = "$count chord versions"
    override val allBack = "‹ All"
    override val searchResults = "SEARCH RESULTS"
    override fun noArtistsFound(query: String) = "No artists found for \"$query\""
    override val tryDifferentArtistKeyword = "Try searching with a different artist name."

    // Search Screen
    override val searchTitle = "SEARCH"
    override val searchPlaceholder = "Search songs or artists..."
    override val artistsHeader = "ARTISTS"
    override val songsHeader = "SONGS"
    override fun artistSongsBadge(count: Int) = "$count songs • Artist"
    override fun noResultsFor(query: String) = "No results found for \"$query\""

    // Favorites Screen
    override val favoritesTitle = "FAVORITES"
    override fun savedSongsCount(count: Int) = "$count saved songs"
    override val searchFavoritesPlaceholder = "Search title or artist"
    override val sortLatest = "Recent"
    override val sortTitle = "Title"
    override val sortArtist = "Artist"
    override val noFavoritesYet = "No favorite songs yet. Add songs from the Player using the heart icon."
    override val noMatchingFavorites = "No matching favorites found."
    override val alternateVersion = "• Alternate version"
    override val removedFromFavorites = "Removed from favorites"

    // Setlists Screen & Tracklist Screen
    override val setlistsTitle = "SETLISTS"
    override val setlistsSubtitle = "Manage and organize your song setlists"
    override val sortSetlists = "Sort setlists"
    override val sortAZ = "A–Z"
    override val sortPopular = "Popular"
    override val searchSetlists = "Search setlists"
    override val noSetlistsYet = "No setlists yet."
    override val noSetlistsFound = "No setlists found."
    override val newSetlist = "New setlist"
    override val setlistName = "Setlist name"
    override val createSetlist = "Create setlist"
    override val songRemoved = "Song removed"
    override val renameSetlist = "Rename setlist"
    override val deleteSetlistConfirmTitle = "Delete setlist?"
    override val deleteSetlistConfirmMessage = "All songs in this setlist will be removed. This action cannot be undone."
    override val setlistOptions = "Setlist options"
    override val noSongsInSetlist = "No songs in this setlist yet."
    override val dragToReorder = "Hold and drag to reorder"
    override val deleteSong = "Remove song"

    // Player Screen & Chord Dialog
    override fun addedToSetlist(name: String) = "Added to $name"
    override fun alreadyInSetlist(name: String) = "Already in $name"
    override val failedToAddToSetlist = "Failed to add to setlist"
    override val addedToFavorites = "Added to favorites"
    override val failedToUpdateFavorites = "Failed to update favorites"
    override val addSongToSetlist = "Add song to setlist"
    override val noSheetContent = "No sheet content available for this version."
    override val addToSetlistDialogTitle = "Add to setlist"
    override val noSetlistsPrompt = "No setlists yet. Create a new setlist to save this song."
    override val createAndAdd = "Create & add"
    override fun diagramNotAvailable(symbol: String) = "Chord diagram not yet available for $symbol."
    override val previousShape = "Previous shape"
    override val nextShape = "Next shape"
    override val addAnotherVersion = "Add another version"

    // Song Versions Screen & Artist Directory
    override val noVersionsFound = "No versions found"
    override fun versionsCount(count: Int) = "$count Versions"
    override fun keyLabel(key: String) = "Key: $key"
    override fun chordsCount(count: Int) = "$count Chords"
    override fun linesCount(count: Int) = "$count Lines"
    override val allSources = "All Sources"
    override val noSongsForArtist = "No songs for this artist."
    override val filterSongs = "Filter songs"
    override fun noSongsMatch(filter: String) = "No songs match \"$filter\"."

    // Settings Screen
    override val settingsTitle = "SETTINGS"
    override val settingsSubtitle = "Application system and display preferences"
    override val themeSection = "APPEARANCE"
    override val appThemeLabel = "Theme"
    override val selectTheme = "Select Theme"
    override val themeDark = "Dark Mode"
    override val themeLight = "Light Mode"
    override val themePaper = "Paper (Sepia)"
    override val themeBlackboard = "Blackboard"
    override val themeGreenboard = "Greenboard"
    override val themeSystem = "System Default"
    override val languageSection = "LANGUAGE"
    override val appLanguageLabel = "App Language"
    override val selectLanguage = "Select Language"
    override val offlineCatalogSection = "OFFLINE CATALOG"
    override val catalogSize = "Catalog size"
    override val licenseAndDeviceSection = "LICENSE & DEVICE"
    override val licenseStatus = "License Status"
    override val licenseActivePro = "PRO ACTIVE"
    override val licenseInactive = "INACTIVE"
    override val licenseKey = "License Key"
    override val transferLicenseToOtherPhone = "Transfer License to Another Device"
    override val transferLicenseTitle = "Transfer License?"
    override fun transferLicenseMessage(key: String) =
        "Your license key: $key\n\nPlease ensure you have copied the code above. Once released, the app on this device will be locked again and the key can be used immediately on your new device."
    override val releaseLicenseAction = "Yes, Release License"
    override val releasingLicense = "Releasing…"
    override val deviceId = "Device ID"
    override val applicationSection = "APPLICATION"
    override val version = "Version"
    override val apkSize = "APK Size"
    override val aboutApp = "About Application"
    override val offlineDisclaimer = "Catalog, favorites, setlists, and Player settings are available offline."

    // About Screen
    override fun versionWithCode(versionName: String, versionCode: Long) = "Version $versionName ($versionCode)"

    // Activation Screen
    override val activationHeader = "User License Activation"
    override val enterLicenseKey = "Enter License Key"
    override val activationDesc = "Enter the code you received after purchase to unlock 69,000+ offline chords."
    override val licenseKeyPlaceholder = "Example: KORD-TEST-1234"
    override val deviceIdCopied = "Device ID copied!"
    override val pleaseEnterLicenseKey = "Please enter your license key."
    override val noLicenseKeyYet = "Don't have a license key yet?"
    override val contactAdmin = "Buy License / Contact Admin"
    override val activateNow = "Activate Now"
    override val verifying = "Verifying..."
    override val unableToOpenBrowserOrWa = "Unable to open browser or WhatsApp"
    override fun waOrderMessage(deviceId: String) =
        "Hello KordApp Admin, I would like to purchase an official KordApp license."
    override val buyLicenseLabel = "Buy License"
    override val lostAccessLabel = "Lost Access?"
    override val resetKeyAction = "Reset License"
    override val resetKeyDialogTitle = "Reset Device & Recover License"
    override val resetKeyDialogDesc = "If your old phone was lost, broken, or replaced, enter your registered recovery email to unbind the license and activate it on this device."
    override val sendResetVerificationBtn = "Send Reset Verification to Email"
    override val enterResetOtpTitle = "Enter 6-Digit Code from Email"
    override val confirmResetBtn = "Confirm & Release Old Device"
    override val recoveryEmailPlaceholder = "name@email.com"
    override val resetSuccessMsg = "License successfully released from old device! Ready to activate."
    override val bindEmailEduTitle = "Secure Your License!"
    override val bindEmailEduDesc = "Link your email now. If you ever lose your phone or switch devices, you can easily recover and transfer your license yourself."
    override val bindEmailNowBtn = "Link Email Now"
    override val setupLaterBtn = "Setup Later"
    override val bindEmailSuccess = "Recovery email successfully verified and linked!"
    override val recoveryEmailTitle = "Recovery Email"
    override val notLinkedStatus = "Not linked"
    override val linkAction = "Link"
    override val verifiedStatus = "Verified"
    override val qrisDialogTitle = "Official QRIS Payment"
    override val qrisDialogDesc = "Scan official KordApp QRIS code using any Indonesian e-wallet or mobile banking app. License will be activated instantly upon payment confirmation."

    // Band Live Sync (P2P Mode)
    override val bandSection = "BAND MODE (LIVE SYNC)"
    override val bandModeServer = "Server"
    override val bandModeNormal = "Normal"
    override val bandModeClient = "Client"
    override val bandServerName = "Device / Leader Name"
    override val bandPinProtection = "PIN Protection (4 Digits)"
    override val bandPinPlaceholder = "Example: 1234"
    override val bandServerActive = "Server Active (Broadcasting)"
    override fun bandConnectedCount(count: Int) = "$count Clients Connected"
    override val bandSearchingServers = "Searching for servers on Wi-Fi / Hotspot..."
    override val bandNoServersFound = "No servers found yet."
    override val bandConnect = "Connect"
    override val bandDisconnect = "Disconnect"
    override fun bandConnectedTo(name: String) = "Connected to $name"
    override val bandStandbyTitle = "STAGE STANDBY"
    override val bandWaitingForLeader = "Waiting for songs from Leader..."
    override val bandEnterPinTitle = "Enter Session PIN"
    override val bandEnterPinPrompt = "This session is protected by a security PIN:"
    override val bandInvalidPin = "Invalid PIN entered."
    override val bandOpenStandby = "Open Standby Screen"
    override val bandLiveBadge = "LIVE"
    override val bandSyncBadge = "SYNC"

    // Settings & Customization Strings
    override val musicAndCatalogSection = "MUSIC & CATALOG"
    override val downloadCatalogLabel = "Download Catalog"
    override val themeLabel = "Theme"
    override val languageLabel = "Language"
    override val revealKey = "Reveal"
    override val hideKey = "Hide"
    override val transferLicense = "Transfer License"
    override val transferLicenseDesc = "Screen lock / biometric protection"
    override val copyKeyAction = "Copy Key"
    override val checkUpdate = "Check Update"
    override val checkingUpdate = "Checking..."
    override val checkVersion = "Check Version"
    override fun appUpToDate(version: String) = "App is already up to date (v$version)"
    override val deleteAction = "Delete"
    override val downloadAction = "Download"
    override val packageA = "Package A"
    override val packageB = "Package B"
    override val packageC = "Package C"
    override fun packageSongsCount(count: String) = "$count Songs"
    override val tierCoreBadge = "⭐ Core"
    override val tierProBadge = "🎸 Popular"
    override val tierArchiveBadge = "📚 Archive"
    override val tierInstalledBadge = "✓ Installed"
    override fun tierDownloadingBadge(percent: Int) = "$percent%"
    override val tierBackgroundNote = "Download continues in the background even if you leave this page."
    override val tierSectionHint = "Choose packages based on your device storage needs"
    override fun downloadingTierTitle(tier: String) = "Downloading Package $tier"

    // Feedback & Suggestions
    override val feedbackAndSuggestions = "Feedback & Suggestions"
    override val sendEmailAction = "Send Email"
    override val feedbackDialogTitle = "Select Feedback Category"
    override val feedbackFeatureCategory = "💡 Feature Request"
    override val feedbackBugCategory = "🐛 Bug Report"
    override val feedbackGeneralCategory = "💬 Feedback & Suggestions"
    override fun feedbackFeatureBody(version: String, device: String) = """
[KordApp-Feature]
------------------------------
Hello, KordApp Team

[Your Message]

Device Info : 
- Device : $device
- KordApp Version : v$version

Thank you.
""".trimIndent()

    override fun feedbackBugBody(version: String, device: String) = """
[KordApp-Bug]
------------------------------
Hello, KordApp Team

[Your Message]

Device Info : 
- Device : $device
- KordApp Version : v$version

Thank you.
""".trimIndent()

    override fun feedbackGeneralBody(version: String, device: String) = """
[KordApp-Feedback]
------------------------------
Hello, KordApp Team

[Your Message]

Device Info : 
- Device : $device
- KordApp Version : v$version

Thank you.
""".trimIndent()

    // Cooldown Note
    override val resetCooldownNote = "ℹ️ Note: License reset is limited to once per month (30 days)."

    // Additional Dialogs & Prompts
    override val bandHostDetected = "DISCOVERED HOSTS:"
    override val copyAction = "Copy"
    override fun linkCopiedToast(url: String) = "Link copied: $url"
    override val keyCopiedToast = "License key copied successfully!"
    override val bindEmailDialogTitle = "Link Recovery Email"
    override val bindEmailDialogDesc = "Link your email so you can recover and transfer your license yourself if this device is ever lost or broken."
    override val enter6DigitOtpPlaceholder = "Enter 6-Digit OTP"
    override val sendOtpBtn = "Send OTP"
    override val verifyBtn = "Verify"
    override fun deletePackageDialogTitle(target: String) = "Delete Package $target Database?"
    override fun deletePackageDialogDesc(target: String) = "Are you sure you want to remove Package $target songs catalog from your device storage?"
    override val stageMirrorInstruction = "Safari iPhone, another Android, or laptop on Wi-Fi / Hotspot:"
    override val stageMirrorTitle = "Web Browser Access (Stage Mirror)"
    override val stageMirrorOptionName = "Host Name (Recommended)"
    override val stageMirrorOptionIp = "IP Address"
    override val noEmailAppFound = "No email app found."
    override val transferDialogWarning = "Warning: Transferring the license will copy your license key and release access rights from this device so it can be used on another device."
    override val copyingAction = "Copying..."
    override val invalidEmailError = "Please enter a valid email address."
    override fun otpSentToast(email: String) = "OTP code has been sent to $email. Please check your inbox or spam folder."
    override val otpLengthError = "OTP code must be 6 digits."
    override fun packageDeletedToast(target: String) = "Package $target successfully deleted"
    override val updateAvailableTitle = "Update Available!"
    override fun newVersionAvailable(version: String) = "A new version v$version has been released."
    override val updateAction = "Update"
    override val laterAction = "Later"
    override val downloadStartedToast = "Download started. Check notifications."
    override val verifyAndContinueBtn = "Verify & Continue"
    override val confirmToWhatsApp = "Confirm via WhatsApp"
}
