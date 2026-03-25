package com.pixelmarket.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors - Teal/Blue Palette (from uploaded image)
val DarkTeal = Color(0xFF09637E)        // Primary - Deep ocean
val MediumTeal = Color(0xFF088395)      // Primary variant
val LightTeal = Color(0xFF7AB2B2)       // Secondary - Aqua
val VeryLightTeal = Color(0xFFEBF4F6)   // Background light

// Light Theme Colors
val PrimaryLight = MediumTeal
val OnPrimaryLight = Color.White
val PrimaryContainerLight = LightTeal
val OnPrimaryContainerLight = DarkTeal

val SecondaryLight = LightTeal
val OnSecondaryLight = Color.White
val SecondaryContainerLight = Color(0xFFB8D8D8)
val OnSecondaryContainerLight = DarkTeal

val TertiaryLight = DarkTeal
val OnTertiaryLight = Color.White
val TertiaryContainerLight = Color(0xFF4A9FB5)
val OnTertiaryContainerLight = Color.White

val BackgroundLight = VeryLightTeal
val OnBackgroundLight = Color(0xFF1A1A1A)
val SurfaceLight = Color.White
val OnSurfaceLight = Color(0xFF1A1A1A)
val SurfaceVariantLight = Color(0xFFD4E7EA)
val OnSurfaceVariantLight = Color(0xFF424242)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color.White

// Dark Theme Colors
val PrimaryDark = LightTeal
val OnPrimaryDark = DarkTeal
val PrimaryContainerDark = MediumTeal
val OnPrimaryContainerDark = Color.White

val SecondaryDark = Color(0xFF9AC5C5)
val OnSecondaryDark = DarkTeal
val SecondaryContainerDark = LightTeal
val OnSecondaryContainerDark = Color.White

val TertiaryDark = Color(0xFF6ABDD4)
val OnTertiaryDark = DarkTeal
val TertiaryContainerDark = MediumTeal
val OnTertiaryContainerDark = Color.White

val BackgroundDark = Color(0xFF0A1E23)   // Very dark teal
val OnBackgroundDark = Color(0xFFE8F4F8)
val SurfaceDark = Color(0xFF143842)       // Dark surface
val OnSurfaceDark = Color(0xFFE8F4F8)
val SurfaceVariantDark = Color(0xFF1F4A54)
val OnSurfaceVariantDark = Color(0xFFB8D8D8)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)

// Glassmorphism constants
val GlassBackgroundLight = VeryLightTeal.copy(alpha = 0.6f)
val GlassBackgroundDark = DarkTeal.copy(alpha = 0.5f)
val GlassBorderLight = MediumTeal.copy(alpha = 0.3f)
val GlassBorderDark = LightTeal.copy(alpha = 0.3f)
