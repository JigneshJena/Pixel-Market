package com.pixelmarket.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Marketplace : Screen("marketplace")
    object Upload : Screen("upload")
    object Downloads : Screen("downloads")
    object Profile : Screen("profile")
    object AssetDetails : Screen("asset_details/{assetId}") {
        fun createRoute(assetId: String) = "asset_details/$assetId"
    }
    object Cart : Screen("cart")
    object Admin : Screen("admin")
    object AdminUsers : Screen("admin/users")
    object AdminTheme : Screen("admin/theme")
    object AdminSettings : Screen("admin/settings")
    object AdminAssets : Screen("admin/assets")
}
