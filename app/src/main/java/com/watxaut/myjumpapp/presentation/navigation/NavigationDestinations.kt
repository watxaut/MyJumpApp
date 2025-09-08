package com.watxaut.myjumpapp.presentation.navigation

sealed class NavigationDestinations(val route: String) {
    object Home : NavigationDestinations("home")
    object UserSelection : NavigationDestinations("user_selection")
    object SurfaceSelection : NavigationDestinations("surface_selection/{userId}") {
        fun createRoute(userId: String) = "surface_selection/$userId"
    }
    object JumpTypeSelection : NavigationDestinations("jump_type_selection/{userId}/{surfaceType}") {
        fun createRoute(userId: String, surfaceType: String) = "jump_type_selection/$userId/$surfaceType"
    }
    object Camera : NavigationDestinations("camera/{userId}/{surfaceType}/{jumpType}") {
        fun createRoute(userId: String, surfaceType: String, jumpType: String) = "camera/$userId/$surfaceType/$jumpType"
    }
    object Statistics : NavigationDestinations("statistics/{userId}") {
        fun createRoute(userId: String) = "statistics/$userId"
    }
    object Settings : NavigationDestinations("settings")
    object UserProfile : NavigationDestinations("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
}