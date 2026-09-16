package com.tokoreader.presentation

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
    
    TokoReaderTheme(themeMode = themeMode, accentColor = accentColor) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

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
                        icon = { Icon(Icons.Filled.Radar, contentDescription = "Radar") },
                        label = { Text("Radar") },
                        selected = currentRoute?.startsWith("radar") == true,
                        onClick = {
                            navController.navigate("radar") {
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
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        onNavigateToRadar = { symbol ->
                            navController.navigate("radar/$symbol")
                        }
                    )
                }
                composable("portfolio") { PortfolioScreen() }
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
