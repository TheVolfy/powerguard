package com.powerguard.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.powerguard.app.ui.screen.dashboard.DashboardScreen
import com.powerguard.app.ui.screen.features.FeatureDetailScreen
import com.powerguard.app.ui.screen.features.FeaturesScreen
import com.powerguard.app.ui.screen.log.LogScreen
import com.powerguard.app.ui.screen.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Dashboard     : Screen("dashboard")
    data object Features      : Screen("features")
    data object Log           : Screen("log")
    data object Settings      : Screen("settings")
    data object FeatureDetail : Screen("feature_detail/{featureKey}") {
        fun createRoute(featureKey: String) = "feature_detail/$featureKey"
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(onNavigateToFeatures = { navController.navigate(Screen.Features.route) })
        }
        composable(Screen.Features.route) {
            FeaturesScreen(
                onFeatureClick = { featureKey ->
                    navController.navigate(Screen.FeatureDetail.createRoute(featureKey))
                }
            )
        }
        composable(
            route = Screen.FeatureDetail.route,
            arguments = listOf(navArgument("featureKey") { type = NavType.StringType }),
        ) { backStackEntry ->
            val featureKey = backStackEntry.arguments?.getString("featureKey") ?: return@composable
            FeatureDetailScreen(
                featureKey = featureKey,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Log.route) {
            LogScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
