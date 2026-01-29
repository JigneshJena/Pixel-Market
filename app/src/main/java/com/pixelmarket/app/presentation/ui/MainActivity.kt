package com.pixelmarket.app.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixelmarket.app.navigation.Screen
import com.pixelmarket.app.presentation.auth.LoginScreen
import com.pixelmarket.app.presentation.auth.RegisterScreen
import com.pixelmarket.app.presentation.auth.SplashScreen
import com.pixelmarket.app.presentation.home.HomeScreen
import com.pixelmarket.app.presentation.marketplace.MarketplaceScreen
import com.pixelmarket.app.presentation.upload.UploadScreen
import com.pixelmarket.app.presentation.downloads.DownloadsScreen
import com.pixelmarket.app.presentation.profile.ProfileScreen
import com.pixelmarket.app.presentation.details.AssetDetailsScreen
import com.pixelmarket.app.presentation.common.components.MainScaffold
import com.pixelmarket.app.presentation.ui.theme.PixelMarketTheme
import com.pixelmarket.app.presentation.admin.AdminDashboardScreen
import com.pixelmarket.app.presentation.admin.AdminUsersScreen
import com.pixelmarket.app.presentation.admin.AdminThemeScreen
import com.pixelmarket.app.presentation.admin.AdminAssetsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for modern Android 15+ feel
        enableEdgeToEdge()
        
        setContent {
            // Dynamic theme with real-time updates from Firebase
            PixelMarketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    MainScaffold(navController = navController) { padding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash.route,
                            modifier = Modifier.padding(padding),
                            // Add smooth page transitions
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { 1000 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) + fadeIn(animationSpec = tween(300))
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -1000 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) + fadeOut(animationSpec = tween(300))
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -1000 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) + fadeIn(animationSpec = tween(300))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { 1000 },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) + fadeOut(animationSpec = tween(300))
                            }
                        ) {
                            composable(Screen.Splash.route) {
                                SplashScreen(
                                    onNavigateToLogin = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    },
                                    onNavigateToHome = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Login.route) {
                                LoginScreen(
                                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                                    onNavigateToHome = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Register.route) {
                                RegisterScreen(
                                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                                    onNavigateToHome = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Register.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Home.route) {
                                HomeScreen(onNavigateToDetails = { id ->
                                    navController.navigate(Screen.AssetDetails.createRoute(id))
                                })
                            }
                            composable(Screen.Marketplace.route) {
                                MarketplaceScreen(onNavigateToDetails = { id ->
                                    navController.navigate(Screen.AssetDetails.createRoute(id))
                                })
                            }
                            composable(Screen.AssetDetails.route) { backStackEntry ->
                                val assetId = backStackEntry.arguments?.getString("assetId") ?: ""
                                AssetDetailsScreen(assetId = assetId, onBack = { navController.popBackStack() })
                            }
                            composable(Screen.Upload.route) {
                                UploadScreen()
                            }
                            composable(Screen.Downloads.route) {
                                DownloadsScreen()
                            }
                            composable(Screen.Profile.route) {
                                ProfileScreen(
                                    onLogout = {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onNavigateToAdmin = {
                                        navController.navigate(Screen.Admin.route)
                                    }
                                )
                            }
                            
                            // Admin Panel Routes
                            composable(Screen.Admin.route) {
                                AdminDashboardScreen(
                                    onNavigateToUsers = { navController.navigate(Screen.AdminUsers.route) },
                                    onNavigateToTheme = { navController.navigate(Screen.AdminTheme.route) },
                                    onNavigateToAssets = { navController.navigate(Screen.AdminAssets.route) },
                                    onNavigateToSettings = { /* TODO */ }
                                )
                            }
                            
                            composable(Screen.AdminUsers.route) {
                                AdminUsersScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            
                            composable(Screen.AdminTheme.route) {
                                AdminThemeScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            
                            composable(Screen.AdminAssets.route) {
                                AdminAssetsScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
