package com.pixelmarket.app.presentation.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.model.ThemeSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/**
 * Dynamic theme that loads colors from Firebase in real-time
 * When admin changes colors, ALL users see updates instantly!
 */
@Composable
fun PixelMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Listen to theme changes from Firebase in real-time
    val themeSettings by produceThemeSettings()
    
    // Convert hex strings to colors
    val primaryColor = hexToColor(themeSettings.primaryColor)
    val secondaryColor = hexToColor(themeSettings.secondaryColor)
    val accentColor = hexToColor(themeSettings.accentColor)
    val backgroundColor = hexToColor(themeSettings.backgroundColor)
    val surfaceColor = hexToColor(themeSettings.surfaceColor)
    
    // Create dynamic color schemes based on Firebase values
    val lightColorScheme = lightColorScheme(
        // Primary colors from Firebase
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = secondaryColor,
        onPrimaryContainer = accentColor,
        
        // Secondary colors
        secondary = secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = secondaryColor.copy(alpha = 0.3f),
        onSecondaryContainer = accentColor,
        
        // Tertiary/Accent
        tertiary = accentColor,
        onTertiary = Color.White,
        tertiaryContainer = accentColor.copy(alpha = 0.3f),
        onTertiaryContainer = primaryColor,
        
        // Background & Surface from Firebase
        background = backgroundColor,
        onBackground = Color(0xFF1A1A1A),
        surface = surfaceColor,
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = surfaceColor.copy(alpha = 0.9f),
        onSurfaceVariant = Color(0xFF424242),
        
        // Error colors (static)
        error = Color(0xFFBA1A1A),
        onError = Color.White
    )
    
    val darkColorScheme = darkColorScheme(
        // Primary colors from Firebase (lighter for dark mode)
        primary = secondaryColor,
        onPrimary = accentColor,
        primaryContainer = primaryColor,
        onPrimaryContainer = Color.White,
        
        // Secondary colors
        secondary = secondaryColor.copy(alpha = 0.8f),
        onSecondary = accentColor,
        secondaryContainer = secondaryColor,
        onSecondaryContainer = Color.White,
        
        // Tertiary/Accent
        tertiary = accentColor.copy(alpha = 0.9f),
        onTertiary = accentColor,
        tertiaryContainer = primaryColor,
        onTertiaryContainer = Color.White,
        
        // Background & Surface (darker versions)
        background = darkenColor(backgroundColor),
        onBackground = Color(0xFFE8F4F8),
        surface = darkenColor(surfaceColor),
        onSurface = Color(0xFFE8F4F8),
        surfaceVariant = darkenColor(surfaceColor, 0.8f),
        onSurfaceVariant = Color(0xFFB8D8D8),
        
        // Error colors
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005)
    )
    
    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Real-time listener for theme settings from Firebase
 * Returns a State that updates automatically when Firestore changes
 */
@Composable
private fun produceThemeSettings(): State<ThemeSettings> {
    return produceState(initialValue = ThemeSettings()) {
        val firestore = FirebaseFirestore.getInstance()
        
        val listener = firestore.collection("settings")
            .document("theme")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep current/default theme on error
                    return@addSnapshotListener
                }
                
                val theme = snapshot?.toObject(ThemeSettings::class.java)
                if (theme != null) {
                    value = theme
                }
            }
        
        // Cleanup listener when composable leaves composition
        awaitDispose {
            listener.remove()
        }
    }
}

/**
 * Convert hex string to Compose Color
 */
private fun hexToColor(hex: String): Color {
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (e: Exception) {
        // Fallback to default teal if parsing fails
        Color(0xFF088395)
    }
}

/**
 * Darken a color for dark mode
 */
private fun darkenColor(color: Color, factor: Float = 0.3f): Color {
    return Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = color.alpha
    )
}
