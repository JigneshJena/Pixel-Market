package com.pixelmarket.app.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.appcompat.app.AppCompatDelegate

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        applyTheme()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun saveRemoteColors(primary: String, secondary: String) {
        prefs.edit().putString("primary_color", primary).putString("secondary_color", secondary).apply()
    }

    fun getPrimaryColor(): Int = AndroidColor.parseColor(prefs.getString("primary_color", "#6366F1"))
    fun getSecondaryColor(): Int = AndroidColor.parseColor(prefs.getString("secondary_color", "#0EA5E9"))

    fun applyTheme() {
        val mode = if (isDarkMode()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    /**
     * Call this in Activity.onCreate() before super.onCreate()
     */
    fun applyThemeToActivity(activity: Activity) {
        applyTheme()
    }
}
