package com.tokoreader.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tokoreader.TokoReaderApp
import com.tokoreader.presentation.dashboard.DashboardScreen
import com.tokoreader.presentation.portfolio.PortfolioScreen
import com.tokoreader.presentation.radar.RadarTradeScreen
import com.tokoreader.presentation.settings.SettingsScreen
import com.tokoreader.ui.theme.TokoReaderTheme

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsRepository = remember(context) { (context.applicationContext as TokoReaderApp).container.settingsRepository }
    val themeMode by settingsRepository.getThemeMode().collectAsStateWithLifecycle(initialValue = "Dark Navy")
    val accentColor by settingsRepository.getAccentColor().collectAsStateWithLifecycle(initialValue = "Electric Blue")

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDetailScreen = currentRoute?.startsWith("detail/") == true || currentRoute?.startsWith("radar/") == true || currentRoute == "radar"

    // Handle back button smoothly to prevent accidental app exits from other main tabs
    if (currentRoute != null && currentRoute != "dashboard") {
        BackHandler {
            if (!navController.popBackStack()) {
                navController.navigate("dashboard") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    
    TokoReaderTheme(themeMode = themeMode, accentColor = accentColor) {
        Scaffold(
            bottomBar = {
                if (!isDetailScreen) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            selected = currentRoute == "dashboard",
                            onClick = {
                                navController.navigate("dashboard") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.PieChart, contentDescription = "Portfolio") },
                            label = { Text("Portfolio") },
                            selected = currentRoute == "portfolio",
                            onClick = {
                                navController.navigate("portfolio") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            selected = currentRoute == "settings",
                            onClick = {
                                navController.navigate("settings") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(260)
                    ) + fadeIn(animationSpec = tween(260))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(260)
                    ) + fadeOut(animationSpec = tween(260))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(260)
                    ) + fadeIn(animationSpec = tween(260))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(260)
                    ) + fadeOut(animationSpec = tween(260))
                }
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        onNavigateToDetail = { symbol ->
                            navController.navigate("detail/$symbol")
                        }
                    )
                }
                composable("portfolio") { PortfolioScreen() }
                composable(
                    route = "detail/{symbol}",
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol") ?: "BTCIDR"
                    RadarTradeScreen(
                        initialSymbol = symbol,
                        onBackClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate("dashboard")
                            }
                        }
                    )
                }
                // Backwards compatibility alias for radar
                composable(
                    route = "radar/{symbol}",
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol") ?: "BTCIDR"
                    RadarTradeScreen(
                        initialSymbol = symbol,
                        onBackClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate("dashboard")
                            }
                        }
                    )
                }
                composable("radar") {
                    RadarTradeScreen(
                        initialSymbol = "BTCIDR",
                        onBackClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate("dashboard")
                            }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onBackClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate("dashboard")
                            }
                        }
                    )
                }
            }
        }
    }
}
