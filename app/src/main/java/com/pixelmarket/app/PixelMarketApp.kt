package com.pixelmarket.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import com.pixelmarket.app.util.ThemeManager

@HiltAndroidApp
class PixelMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize and apply theme at the application level
        ThemeManager(this).applyTheme()
    }
}
