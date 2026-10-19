package app.ytsleep.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.ytsleep.YtSleepApp
import kotlinx.coroutines.flow.combine
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.ytsleep.ui.feed.FeedScreen
import app.ytsleep.ui.player.PlayerSheetHost
import app.ytsleep.ui.search.SearchScreen
import app.ytsleep.ui.settings.SettingsScreen
import app.ytsleep.ui.start.StartScreen
import app.ytsleep.ui.account.ConnectAccountScreen
import app.ytsleep.ui.account.PasteCookiesScreen
import app.ytsleep.ui.playlist.PlaylistScreen

private val noOpUriHandler = object : UriHandler {
    override fun openUri(uri: String) { /* never open external apps */ }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    AutoInstallUpdates()

    androidx.compose.runtime.CompositionLocalProvider(LocalUriHandler provides noOpUriHandler) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == "start" } == true,
                        onClick = { navController.navigate("start") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }},
                        icon = { Icon(Icons.Default.Home, contentDescription = "Start") },
                        label = { Text("Start") }
                    )
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == "feed" } == true,
                        onClick = { navController.navigate("feed") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }},
                        icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = "Feed") },
                        label = { Text("Feed") }
                    )
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == "search" } == true,
                        onClick = { navController.navigate("search") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }},
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == "settings" } == true,
                        onClick = { navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }},
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    modifier = Modifier.weight(1f),
                    navController = navController,
                    startDestination = "start",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                ) {
                    composable("start") { StartScreen(navController) }
                    composable("feed") { FeedScreen(navController) }
                    composable("search") { SearchScreen(navController) }
                    composable(
                        "playlist/{playlistId}?title={title}&count={count}",
                        arguments = listOf(
                            navArgument("title") { type = NavType.StringType; defaultValue = "" },
                            navArgument("count") { type = NavType.IntType; defaultValue = -1 },
                        ),
                    ) { backStackEntry ->
                        val args = backStackEntry.arguments
                        PlaylistScreen(
                            playlistId = args?.getString("playlistId").orEmpty(),
                            title = args?.getString("title").orEmpty().ifEmpty { "Playlist" },
                            itemCount = args?.getInt("count")?.takeIf { it >= 0 },
                            navController = navController,
                        )
                    }
                    composable("settings") { SettingsScreen(navController) }
                    composable("account/connect") { ConnectAccountScreen(navController) }
                    composable("account/paste") { PasteCookiesScreen(navController) }
                }
                // Mini-player sits between the current screen and the bottom bar.
                PlayerSheetHost(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * While the app is in the foreground, opens the system install prompt for a downloaded update
 * as soon as nothing is playing (FR-021, contracts/update-manifest.md).
 */
@Composable
private fun AutoInstallUpdates() {
    val container = (LocalContext.current.applicationContext as YtSleepApp).container
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            combine(container.updateChecker.state, container.playerConnection.state) { _, player -> player }
                .collect { player ->
                    // Wait for the controller: before it connects, isPlaying is unknown.
                    if (player.connected) container.updateChecker.maybeAutoInstall(player.isPlaying)
                }
        }
    }
}

fun playlistRoute(playlist: app.ytsleep.youtube.PlaylistSummary): String =
    "playlist/${android.net.Uri.encode(playlist.playlistId)}" +
        "?title=${android.net.Uri.encode(playlist.title)}&count=${playlist.itemCount ?: -1}"
