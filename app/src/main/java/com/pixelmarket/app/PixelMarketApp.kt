package com.pixelmarket.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

import com.pixelmarket.app.util.ThemeManager

@HiltAndroidApp
class PixelMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Force light mode — dark mode disabled for now
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize and apply theme at the application level
        ThemeManager(this).applyTheme()
    }
}
