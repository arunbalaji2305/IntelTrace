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
import com.example.inteltrace_v3.presentation.connections.ConnectionsScreen
import com.example.inteltrace_v3.presentation.dashboard.DashboardScreen
import com.example.inteltrace_v3.presentation.reports.ReportsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Reports : Screen("reports", "Reports", Icons.Default.DateRange)
    object About : Screen("about", "About", Icons.Default.Info)
    object Connections : Screen("connections", "Connections", Icons.AutoMirrored.Filled.List)
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
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
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
                            }
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
                        navController.navigate(Screen.Connections.route)
                    },
                    onNavigateToThreats = {
                        navController.navigate(Screen.Connections.route)
                    },
                    onNavigateToAlerts = {
                        navController.navigate(Screen.Connections.route)
                    },
                    onNavigateToSettings = {
                        // Could add settings screen later
                    }
                )
            }
            
            composable(Screen.Reports.route) {
                ReportsScreen()
            }
            
            composable(Screen.About.route) {
                AboutScreen()
            }
            
            composable(Screen.Connections.route) {
                ConnectionsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
