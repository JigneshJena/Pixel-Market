package com.pixelmarket.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PixelMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Cloudinary will be initialized when first used
    }
}
