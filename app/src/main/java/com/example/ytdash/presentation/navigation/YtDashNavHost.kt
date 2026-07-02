package com.example.ytdash.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ytdash.core.link.ExternalLinkViewModel
import com.example.ytdash.presentation.home.HomeScreen
import com.example.ytdash.presentation.login.LoginScreen
import com.example.ytdash.presentation.map.MapScreen

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val MAP = "map"
}

@Composable
fun YtDashNavHost(
    externalLinkViewModel: ExternalLinkViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.LOGIN, modifier = modifier) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                externalLinkViewModel = externalLinkViewModel,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                },
                onNavigateToMap = { navController.navigate(Routes.MAP) },
            )
        }
        composable(Routes.MAP) {
            MapScreen(externalLinkViewModel = externalLinkViewModel)
        }
    }
}
