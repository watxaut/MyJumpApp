package com.watxaut.myjumpapp.presentation.navigation

sealed class NavigationDestinations(val route: String) {
    object Home : NavigationDestinations("home")
    object UserSelection : NavigationDestinations("user_selection")
    object Camera : NavigationDestinations("camera/{userId}") {
        fun createRoute(userId: String) = "camera/$userId"
    }
    object Statistics : NavigationDestinations("statistics/{userId}") {
        fun createRoute(userId: String) = "statistics/$userId"
    }
    object Settings : NavigationDestinations("settings")
    object UserProfile : NavigationDestinations("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
}