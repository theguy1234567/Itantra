package com.itantara.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Connection : Screen("connection")
    data object Communication : Screen("communication")
}
