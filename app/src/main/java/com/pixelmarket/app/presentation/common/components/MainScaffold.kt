package com.pixelmarket.app.presentation.common.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pixelmarket.app.navigation.Screen

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun MainScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        BottomNavItem(Screen.Home.route, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Marketplace.route, Icons.Default.Store, "Market"),
        BottomNavItem(Screen.Upload.route, Icons.Default.Add, "Upload"),
        BottomNavItem(Screen.Downloads.route, Icons.Default.Download, "Downloads"),
        BottomNavItem(Screen.Profile.route, Icons.Default.Person, "Profile")
    )

    val showBottomBar = currentRoute in navItems.map { it.route }

    Scaffold(
        bottomBar = {
            // Floating navigation bar with more elevation
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.9f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                        targetScale = 0.9f,
                        animationSpec = tween(200, easing = LinearOutSlowInEasing)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp) // More bottom elevation
                ) {
                    ModernBottomBar(
                        items = navItems,
                        currentRoute = currentRoute,
                        onItemClick = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        content(padding)
    }
}

@Composable
fun ModernBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    // Get screen configuration for responsiveness
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Adaptive sizing based on screen width
    val bottomBarHeight = when {
        screenWidth < 360.dp -> 64.dp // Small phones
        screenWidth < 600.dp -> 68.dp // Normal phones  
        screenWidth < 840.dp -> 72.dp // Large phones/small tablets
        else -> 76.dp // Tablets and larger
    }
    
    val horizontalPadding = when {
        screenWidth < 360.dp -> 8.dp
        screenWidth < 600.dp -> 12.dp
        else -> 16.dp
    }
    
    // Floating navigation bar with glassmorphism
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .shadow(
                elevation = 12.dp,
                shape = MaterialTheme.shapes.extraLarge,
                clip = false
            )
            .clip(MaterialTheme.shapes.extraLarge), // Rounded corners
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // Slight transparency
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                BottomNavButton(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item.route) },
                    screenWidth = screenWidth
                )
            }
        }
    }
}

@Composable
fun RowScope.BottomNavButton(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    screenWidth: androidx.compose.ui.unit.Dp
) {
    // Adaptive sizing
    val iconSize = when {
        screenWidth < 360.dp -> 22.dp
        screenWidth < 600.dp -> 24.dp
        else -> 26.dp
    }
    
    val buttonSize = when {
        screenWidth < 360.dp -> 44.dp
        screenWidth < 600.dp -> 48.dp
        else -> 52.dp
    }
    
    val textSize = when {
        screenWidth < 360.dp -> 10.sp
        screenWidth < 600.dp -> 11.sp
        else -> 12.sp
    }
    
    // Simple scale animation - subtle and clean
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.95f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // Simple color transition
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "iconTint"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(buttonSize)
                .scale(scale) // Simple scale effect
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }

        // Simple fade for label
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Text(
                text = item.label,
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
