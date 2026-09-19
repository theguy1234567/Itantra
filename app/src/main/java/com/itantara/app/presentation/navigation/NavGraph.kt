package com.itantara.app.presentation.navigation

import androidx.compose.runtime.Composable
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
    NavHost(
        navController = navController,
        startDestination = Screen.Setup.route
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onNavigateToConnection = {
                    navController.navigate(Screen.Connection.route)
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
