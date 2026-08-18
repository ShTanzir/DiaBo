package com.tanzir.diabo.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ProjectList : Screen("project_list")
    data object CodeIde : Screen("code_ide/{projectId}") {
        fun createRoute(projectId: String) = "code_ide/$projectId"
    }
    data object Settings : Screen("settings")
}
