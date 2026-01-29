package com.pixelmarket.app.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive dimensions for all screen sizes
 */
object Dimens {
    // Screen breakpoints
    const val COMPACT_WIDTH = 600
    const val MEDIUM_WIDTH = 840
    
    @Composable
    fun getScreenWidth(): Int {
        return LocalConfiguration.current.screenWidthDp
    }
    
    @Composable
    fun isCompactScreen(): Boolean = getScreenWidth() < COMPACT_WIDTH
    
    @Composable
    fun isMediumScreen(): Boolean = getScreenWidth() in COMPACT_WIDTH until MEDIUM_WIDTH
    
    @Composable
    fun isExpandedScreen(): Boolean = getScreenWidth() >= MEDIUM_WIDTH
    
    // Spacing
    val Space4: Dp @Composable get() = 4.dp
    val Space8: Dp @Composable get() = 8.dp
    val Space12: Dp @Composable get() = 12.dp
    val Space16: Dp @Composable get() = 16.dp
    val Space24: Dp @Composable get() = 24.dp
    val Space32: Dp @Composable get() = 32.dp
    val Space48: Dp @Composable get() = 48.dp
    
    // Component sizes
    val BottomNavHeight: Dp @Composable get() = if (isCompactScreen()) 64.dp else 72.dp
    val TopBarHeight: Dp @Composable get() = if (isCompactScreen()) 56.dp else 64.dp
    val CardElevation: Dp @Composable get() = 4.dp
    val CardCornerRadius: Dp @Composable get() = 16.dp
    
    // Content
    val ContentPadding: Dp @Composable get() = if (isCompactScreen()) 16.dp else 24.dp
    val ScreenPadding: Dp @Composable get() = if (isCompactScreen()) 16.dp else 32.dp
    
    // Grid
    val GridColumns: Int @Composable get() = when {
        isCompactScreen() -> 2
        isMediumScreen() -> 3
        else -> 4
    }
}
