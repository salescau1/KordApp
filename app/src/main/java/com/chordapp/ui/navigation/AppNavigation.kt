package com.chordapp.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chordapp.viewmodels.ExploreViewModel
import com.chordapp.ui.screens.about.AboutScreen
import com.chordapp.ui.screens.artist.ArtistDirectoryScreen
import com.chordapp.ui.screens.explore.ExploreScreen
import com.chordapp.ui.screens.favorites.FavoritesScreen
import com.chordapp.ui.screens.home.HomeScreen
import com.chordapp.ui.screens.search.SearchScreen
import com.chordapp.ui.screens.setlists.SetlistsScreen
import com.chordapp.ui.screens.settings.SettingsScreen
import com.chordapp.ui.screens.splash.SplashScreen
import com.chordapp.ui.theme.AccentGold
import com.chordapp.ui.theme.Background
import com.chordapp.ui.theme.Border
import com.chordapp.ui.theme.CardSurface
import com.chordapp.ui.theme.MutedText
import com.chordapp.viewmodels.ViewModelFactory

internal val ExpandedNavigationMinWidth = 840.dp

internal fun usesNavigationRail(width: Dp): Boolean = width >= ExpandedNavigationMinWidth

internal fun showsPrimaryNavigation(route: String?, isEditMode: Boolean = false): Boolean = route != null &&
    !(route == Screen.Explore.route && isEditMode) &&
    !route.startsWith("artist_directory") &&
    !route.startsWith("song_versions") &&
    !route.startsWith("player") &&
    !route.startsWith("about") &&
    !route.startsWith("tracklist/") &&
    !route.startsWith("splash") &&
    !route.startsWith("activation") &&
    !route.startsWith("band_standby")

private val primaryScreens = listOf(
    Screen.Home,
    Screen.Explore,
    Screen.Setlists,
    Screen.Favorites,
    Screen.Settings
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModelFactory = ViewModelFactory(context)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination = navBackStackEntry?.destination

    val exploreViewModel: ExploreViewModel = viewModel(factory = viewModelFactory)
    val isEditMode by exploreViewModel.isEditMode.collectAsState()

    // Global listener for Band Live Sync in Client Mode (only when connected)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.chordapp.p2p.BandSessionManager.incomingSong.collect { payload ->
            if (com.chordapp.p2p.BandSessionManager.currentMode.value == com.chordapp.p2p.BandMode.CLIENT &&
                com.chordapp.p2p.BandSessionManager.connectionState.value is com.chordapp.p2p.ClientConnectionState.Connected
            ) {
                val activeRoute = navController.currentBackStackEntry?.destination?.route
                if (activeRoute?.startsWith("player") != true) {
                    val safeHash = java.net.URLEncoder.encode(payload.sourceUrl, "UTF-8")
                    navController.navigate(
                        Screen.Player.createRoute(
                            safeHash,
                            transpose = payload.transpose,
                            capo = payload.capo
                        )
                    )
                }
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.chordapp.p2p.BandSessionManager.standbyTrigger.collect {
            if (com.chordapp.p2p.BandSessionManager.currentMode.value == com.chordapp.p2p.BandMode.CLIENT &&
                com.chordapp.p2p.BandSessionManager.connectionState.value is com.chordapp.p2p.ClientConnectionState.Connected
            ) {
                val activeRoute = navController.currentBackStackEntry?.destination?.route
                if (activeRoute?.startsWith("player") == true) {
                    navController.popBackStack()
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expanded = usesNavigationRail(maxWidth)
        val showPrimaryNavigation = showsPrimaryNavigation(currentRoute, isEditMode)

        if (expanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    viewModelFactory = viewModelFactory,
                    exploreViewModel = exploreViewModel,
                    expandedHome = true,
                    modifier = Modifier.weight(1f)
                )
                if (showPrimaryNavigation) {
                    PrimaryNavigationRail(
                        currentDestination = currentDestination,
                        onSelect = { navigateToPrimary(navController, it) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
            ) {
                AppNavHost(
                    navController = navController,
                    viewModelFactory = viewModelFactory,
                    exploreViewModel = exploreViewModel,
                    expandedHome = false,
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedVisibility(
                    visible = showPrimaryNavigation,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.30f to Background.copy(alpha = 0.65f),
                                    0.65f to Background.copy(alpha = 0.90f),
                                    1.0f to Background
                                )
                            )
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 12.dp)
                    ) {
                        PrimaryBottomBar(
                            currentDestination = currentDestination,
                            onSelect = { navigateToPrimary(navController, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryBottomBar(
    currentDestination: NavDestination?,
    onSelect: (Screen) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = CardSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.8f)),
        shadowElevation = 16.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            primaryScreens.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.primaryNavIcon(), contentDescription = screen.title(strings)) },
                    label = { Text(screen.title(strings)) },
                    selected = currentDestination.isSelected(screen),
                    onClick = { onSelect(screen) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentGold,
                        selectedTextColor = AccentGold,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MutedText,
                        unselectedTextColor = MutedText
                    )
                )
            }
        }
    }
}

@Composable
private fun PrimaryNavigationRail(
    currentDestination: NavDestination?,
    onSelect: (Screen) -> Unit
) {
    val strings = com.chordapp.localization.LocalAppStrings.current
    NavigationRail(containerColor = CardSurface) {
        primaryScreens.forEach { screen ->
            NavigationRailItem(
                selected = currentDestination.isSelected(screen),
                onClick = { onSelect(screen) },
                icon = { Icon(screen.primaryNavIcon(), contentDescription = screen.title(strings)) },
                label = { Text(screen.title(strings)) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = AccentGold,
                    selectedTextColor = AccentGold,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MutedText,
                    unselectedTextColor = MutedText
                )
            )
        }
    }
}

private fun NavDestination?.isSelected(screen: Screen): Boolean =
    this?.hierarchy?.any { it.route == screen.route } == true

private fun navigateToPrimary(navController: NavHostController, screen: Screen) {
    navController.navigate(screen.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    exploreViewModel: ExploreViewModel,
    expandedHome: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Activation.route) {
            com.chordapp.ui.screens.activation.ActivationScreen(
                onActivationSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Activation.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            val promoViewModel: com.chordapp.viewmodels.PromoViewModel =
                viewModel(factory = viewModelFactory)
            val promo by promoViewModel.promo.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            HomeScreen(
                viewModel = viewModel(factory = viewModelFactory),
                expanded = expandedHome,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onSongClick = { artistSlug, titleSlug ->
                    navController.navigate(Screen.SongVersions.createRoute(artistSlug, titleSlug))
                }
            )
            promo?.let { active ->
                com.chordapp.ui.screens.home.PromoDialog(
                    promo = active,
                    onDismiss = { promoViewModel.dismiss() },
                    onAction = { action ->
                        when (action) {
                            is com.chordapp.promo.PromoAction.External -> {
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(action.url)
                                        )
                                    )
                                }
                            }
                            is com.chordapp.promo.PromoAction.Catalog -> {
                                navController.navigate(
                                    Screen.SongVersions.createRoute(action.artistSlug, action.titleSlug)
                                )
                            }
                        }
                    }
                )
            }
        }
        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("source") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val queryArg = backStackEntry.arguments?.getString("query")
            val sourceArg = backStackEntry.arguments?.getString("source")
            val searchViewModel: com.chordapp.viewmodels.SearchViewModel = viewModel(factory = viewModelFactory)
            androidx.compose.runtime.LaunchedEffect(queryArg, sourceArg) {
                if (!sourceArg.isNullOrBlank()) {
                    runCatching {
                        val parsed = com.chordapp.viewmodels.SearchSource.valueOf(sourceArg.uppercase())
                        searchViewModel.setSource(parsed)
                    }
                } else {
                    searchViewModel.setSource(com.chordapp.viewmodels.SearchSource.LOCAL)
                }
                if (!queryArg.isNullOrBlank()) {
                    runCatching {
                        searchViewModel.onQueryChange(java.net.URLDecoder.decode(queryArg, "UTF-8"))
                    }
                }
            }
            SearchScreen(
                viewModel = searchViewModel,
                onBackClick = { navController.popBackStack() },
                onArtistClick = { artistSlug ->
                    navController.navigate(Screen.ArtistDirectory.createRoute(artistSlug))
                },
                onSongClick = { artistSlug, titleSlug ->
                    navController.navigate(Screen.SongVersions.createRoute(artistSlug, titleSlug))
                }
            )
        }
        composable(
            route = Screen.ArtistDirectory.route,
            arguments = listOf(navArgument("artistSlug") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistSlug = backStackEntry.arguments?.getString("artistSlug") ?: ""
            ArtistDirectoryScreen(
                artistSlug = artistSlug,
                viewModel = viewModel(factory = viewModelFactory),
                onBackClick = { navController.popBackStack() },
                onSongClick = { songArtistSlug, songTitleSlug ->
                    navController.navigate(
                        Screen.SongVersions.createRoute(songArtistSlug, songTitleSlug)
                    )
                }
            )
        }
        composable(Screen.Explore.route) {
            ExploreScreen(
                viewModel = exploreViewModel,
                onArtistClick = { artistSlug ->
                    navController.navigate(Screen.ArtistDirectory.createRoute(artistSlug))
                },
                onSongClick = { artistSlug, titleSlug ->
                    navController.navigate(Screen.SongVersions.createRoute(artistSlug, titleSlug))
                }
            )
        }
        composable(Screen.Setlists.route) {
            SetlistsScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onSetlistClick = { id -> navController.navigate("tracklist/$id") }
            )
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onSongClick = { sourceUrl, transpose, capo ->
                    navController.navigate(
                        Screen.Player.createRoute(
                            java.net.URLEncoder.encode(sourceUrl, "UTF-8"),
                            transpose,
                            capo
                        )
                    )
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onAboutClick = { navController.navigate(Screen.About.route) },
                onLicenseTransferred = {
                    navController.navigate(Screen.Activation.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToStandby = {
                    navController.navigate(Screen.BandStandby.route)
                }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(
                viewModel = viewModel(factory = viewModelFactory),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = "tracklist/{setlistId}",
            arguments = listOf(navArgument("setlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("setlistId") ?: -1L
            com.chordapp.ui.screens.setlists.SetlistTracklistScreen(
                setlistId = id,
                viewModel = viewModel(factory = viewModelFactory),
                onBackClick = { navController.popBackStack() },
                onSongClick = { sourceUrl, transpose, capo ->
                    navController.navigate(
                        Screen.Player.createRoute(
                            java.net.URLEncoder.encode(sourceUrl, "UTF-8"),
                            transpose,
                            capo
                        )
                    )
                }
            )
        }
        composable(
            route = Screen.SongVersions.route,
            arguments = listOf(
                navArgument("artistSlug") { type = NavType.StringType },
                navArgument("titleSlug") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val artistSlug = backStackEntry.arguments?.getString("artistSlug") ?: ""
            val titleSlug = backStackEntry.arguments?.getString("titleSlug") ?: ""
            com.chordapp.ui.screens.versions.SongVersionsScreen(
                artistSlug = artistSlug,
                titleSlug = titleSlug,
                viewModel = viewModel(factory = viewModelFactory),
                onBackClick = { navController.popBackStack() },
                onNavigateToPlayer = { sourceUrl ->
                    val safeHash = java.net.URLEncoder.encode(sourceUrl, "UTF-8")
                    navController.navigate(Screen.Player.createRoute(safeHash)) {
                        popUpTo(Screen.SongVersions.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("sourceUrlHash") { type = NavType.StringType },
                navArgument("transpose") { type = NavType.IntType; defaultValue = 0 },
                navArgument("capo") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val sourceUrlHash = backStackEntry.arguments?.getString("sourceUrlHash") ?: ""
            val initialTranspose = backStackEntry.arguments?.getInt("transpose") ?: 0
            val initialCapo = backStackEntry.arguments?.getInt("capo") ?: 0
            com.chordapp.ui.screens.player.PlayerScreen(
                sourceUrlHash = sourceUrlHash,
                initialTranspose = initialTranspose,
                initialCapo = initialCapo,
                viewModel = viewModel(factory = viewModelFactory),
                onBackClick = { navController.popBackStack() },
                onSearchClick = { query, source ->
                    navController.navigate(Screen.Search.createRoute(query, source))
                }
            )
        }
        composable(Screen.BandStandby.route) {
            com.chordapp.ui.screens.p2p.BandStandbyScreen(
                onCloseClick = {
                    navController.popBackStack()
                },
                onDisconnectClick = {
                    com.chordapp.p2p.BandSessionManager.disconnectClient()
                    navController.popBackStack()
                }
            )
        }
    }
}

private fun Screen.primaryNavIcon(): ImageVector = when (this) {
    Screen.Home -> Icons.Filled.Home
    Screen.Explore -> Icons.Filled.Explore
    Screen.Setlists -> Icons.AutoMirrored.Filled.QueueMusic
    Screen.Favorites -> Icons.Filled.Favorite
    Screen.Settings -> Icons.Filled.Settings
    else -> Icons.Filled.Home
}
