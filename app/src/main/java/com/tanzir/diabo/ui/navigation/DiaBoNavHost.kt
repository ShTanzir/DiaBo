package com.tanzir.diabo.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.tanzir.diabo.ui.home.HomeScreen
import com.tanzir.diabo.ui.ide.CodeIdeScreen
import com.tanzir.diabo.ui.projectlist.ProjectListScreen
import com.tanzir.diabo.ui.settings.SettingsScreen

@Composable
fun DiaBoNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 6 } }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenProject = { projectId ->
                    navController.navigate(Screen.CodeIde.createRoute(projectId))
                },
                onSeeAllProjects = { navController.navigate(Screen.ProjectList.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.ProjectList.route) {
            ProjectListScreen(
                onOpenProject = { projectId ->
                    navController.navigate(Screen.CodeIde.createRoute(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.CodeIde.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) {
            CodeIdeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
