package com.example.inteltrace_v3.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.inteltrace_v3.presentation.about.AboutScreen
import com.example.inteltrace_v3.presentation.alerts.AlertsScreen
import com.example.inteltrace_v3.presentation.connections.ConnectionsScreen
import com.example.inteltrace_v3.presentation.dashboard.DashboardScreen
import com.example.inteltrace_v3.presentation.reports.ReportsScreen
import com.example.inteltrace_v3.presentation.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Reports : Screen("reports", "Reports", Icons.Default.DateRange)
    object About : Screen("about", "About", Icons.Default.Info)
    object Connections : Screen("connections?filter={filter}", "Connections", Icons.AutoMirrored.Filled.List) {
        fun createRoute(filter: String = "all") = "connections?filter=$filter"
    }
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Alerts : Screen("alerts", "Alerts", Icons.Default.Notifications)
}

@Composable
fun IntelTraceNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Bottom nav items
    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Reports,
        Screen.About
    )
    
    Scaffold(
        bottomBar = {
            // Only show bottom nav on main screens
            if (currentDestination?.route in bottomNavItems.map { it.route }) {
                NavigationBar(
                    containerColor = com.example.inteltrace_v3.ui.theme.SystemBackground,
                    contentColor = com.example.inteltrace_v3.ui.theme.LabelPrimary
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelMedium) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Pop up to the start destination to avoid building up a large stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.inteltrace_v3.ui.theme.SystemBlue,
                                selectedTextColor = com.example.inteltrace_v3.ui.theme.SystemBlue,
                                unselectedIconColor = com.example.inteltrace_v3.ui.theme.SystemGray,
                                unselectedTextColor = com.example.inteltrace_v3.ui.theme.SystemGray,
                                indicatorColor = com.example.inteltrace_v3.ui.theme.SystemBlue.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToConnections = {
                        navController.navigate(Screen.Connections.createRoute("all"))
                    },
                    onNavigateToThreats = {
                        navController.navigate(Screen.Connections.createRoute("suspicious"))
                    },
                    onNavigateToAlerts = {
                        navController.navigate(Screen.Alerts.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            
            composable(Screen.Reports.route) {
                ReportsScreen()
            }
            
            composable(Screen.About.route) {
                AboutScreen()
            }
            
            composable(
                route = Screen.Connections.route,
                arguments = listOf(
                    androidx.navigation.navArgument("filter") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "all"
                    }
                )
            ) {
                ConnectionsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Alerts.route) {
                AlertsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
