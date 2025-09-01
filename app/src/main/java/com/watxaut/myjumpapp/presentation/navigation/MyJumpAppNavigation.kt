package com.watxaut.myjumpapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.watxaut.myjumpapp.presentation.ui.screens.CameraScreen
import com.watxaut.myjumpapp.presentation.ui.screens.HomeScreen
import com.watxaut.myjumpapp.presentation.ui.screens.SettingsScreen
import com.watxaut.myjumpapp.presentation.ui.screens.StatisticsScreen
import com.watxaut.myjumpapp.presentation.ui.screens.UserSelectionScreen
import com.watxaut.myjumpapp.presentation.ui.screens.UserProfileScreen

@Composable
fun MyJumpAppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestinations.Home.route
    ) {
        composable(NavigationDestinations.Home.route) {
            HomeScreen(
                onNavigateToUserSelection = {
                    navController.navigate(NavigationDestinations.UserSelection.route)
                },
                onNavigateToCamera = { userId ->
                    navController.navigate(NavigationDestinations.Camera.createRoute(userId))
                },
                onNavigateToStatistics = { userId ->
                    navController.navigate(NavigationDestinations.Statistics.createRoute(userId))
                }
            )
        }
        
        composable(NavigationDestinations.UserSelection.route) {
            UserSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUserSelected = { userId ->
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = NavigationDestinations.Camera.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            CameraScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = NavigationDestinations.Statistics.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            StatisticsScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationDestinations.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = NavigationDestinations.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserProfileScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}