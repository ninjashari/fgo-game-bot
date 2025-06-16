/**
 * FGO Bot - Navigation System
 * 
 * This file defines the navigation structure for the FGO Bot application.
 * It provides a centralized navigation system using Jetpack Compose Navigation
 * with support for bottom navigation and deep linking.
 * 
 * Features:
 * - Bottom navigation with main screens
 * - Navigation state management
 * - Deep linking support
 * - Screen transitions and animations
 */

package com.fgobot.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fgobot.R
import com.fgobot.presentation.screens.*
import com.fgobot.presentation.viewmodel.AutomationViewModel

/**
 * Navigation destinations
 */
sealed class Screen(
    val route: String,
    val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Automation : Screen("automation", R.string.battle_automation, Icons.Default.PlayArrow)
    object Teams : Screen("teams", R.string.nav_teams, Icons.Default.Group)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object BattleLogs : Screen("battle_logs", R.string.nav_battle, Icons.Default.History)
}

/**
 * Main navigation composable
 * 
 * @param automationViewModel Shared automation ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FGOBotNavigation(
    automationViewModel: AutomationViewModel
) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            FGOBotBottomNavigation(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAutomation = {
                        navController.navigate(Screen.Automation.route)
                    },
                    onNavigateToTeams = {
                        navController.navigate(Screen.Teams.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            
            composable(Screen.Automation.route) {
                AutomationScreen(
                    viewModel = automationViewModel,
                    onNavigateToTeams = {
                        navController.navigate(Screen.Teams.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            
            composable(Screen.Teams.route) {
                TeamsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable(Screen.BattleLogs.route) {
                BattleLogsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

/**
 * Bottom navigation bar
 */
@Composable
private fun FGOBotBottomNavigation(
    navController: NavHostController
) {
    val screens = listOf(
        Screen.Home,
        Screen.Automation,
        Screen.Teams,
        Screen.BattleLogs,
        Screen.Settings
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.titleRes)
                    )
                },
                label = {
                    Text(stringResource(screen.titleRes))
                },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
} 