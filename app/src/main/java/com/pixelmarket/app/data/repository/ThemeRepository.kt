package com.pixelmarket.app.data.repository

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.model.ThemeSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Listen to theme changes in real-time
     */
    fun observeThemeSettings(): Flow<ThemeSettings> = callbackFlow {
        val listener = firestore.collection("settings")
            .document("theme")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // If error or no theme found, use defaults
                    trySend(ThemeSettings())
                    return@addSnapshotListener
                }
                
                val theme = snapshot?.toObject(ThemeSettings::class.java) ?: ThemeSettings()
                trySend(theme)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Convert hex string to Compose Color
     */
    fun hexToColor(hex: String): Color {
        return try {
            Color(AndroidColor.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFF088395) // Default teal if parsing fails
        }
    }
}
