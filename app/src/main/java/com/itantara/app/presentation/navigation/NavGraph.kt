package com.itantara.app.presentation.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itantara.app.presentation.communication.CommunicationScreen
import com.itantara.app.presentation.communication.CommunicationViewModel
import com.itantara.app.presentation.connection.ConnectionScreen
import com.itantara.app.presentation.connection.ConnectionViewModel
import com.itantara.app.presentation.setup.SetupScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("itantra_ui_prefs", Context.MODE_PRIVATE)
    val startDestination = if (prefs.getBoolean("show_onboarding", true)) {
        Screen.Setup.route
    } else {
        Screen.Connection.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onNavigateToConnection = {
                    prefs.edit().putBoolean("show_onboarding", false).apply()
                    navController.navigate(Screen.Connection.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Connection.route) {
            val connectionViewModel: ConnectionViewModel = viewModel()
            ConnectionScreen(
                viewModel = connectionViewModel,
                onNavigateToCommunication = {
                    navController.navigate(Screen.Communication.route)
                }
            )
        }
        composable(Screen.Communication.route) {
            val communicationViewModel: CommunicationViewModel = viewModel()
            CommunicationScreen(viewModel = communicationViewModel)
        }
    }
}
