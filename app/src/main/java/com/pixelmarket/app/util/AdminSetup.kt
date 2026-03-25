package com.pixelmarket.app.util

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.model.User
import kotlinx.coroutines.tasks.await

/**
 * One-time setup utility to create the default admin account
 * 
 * ⚠️ IMPORTANT: This should only be used for initial setup!
 * Remove or disable this in production builds.
 * 
 * Default Admin Credentials:
 * Email: pixelMadmin@gmail.com
 * Password: admin1234
 */
object AdminSetup {
    
    private const val ADMIN_EMAIL = "pixelMadmin@gmail.com"
    private const val ADMIN_PASSWORD = "admin1234"
    private const val TAG = "AdminSetup"
    
    /**
     * Create the default admin account
     * This is safe to call multiple times - it will skip if account already exists
     */
    suspend fun createDefaultAdminAccount(): Result<String> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            
            // Check if admin already exists
            val existingUsers = firestore.collection("users")
                .whereEqualTo("email", ADMIN_EMAIL)
                .get()
                .await()
            
            if (!existingUsers.isEmpty) {
                Log.d(TAG, "Admin account already exists")
                return Result.success("Admin account already exists")
            }
            
            // Create authentication account
            val authResult = try {
                auth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
            } catch (e: Exception) {
                if (e.message?.contains("already in use") == true) {
                    // Account exists in Auth but not in Firestore, get the user
                    auth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
                } else {
                    throw e
                }
            }
            
            val userId = authResult.user?.uid 
                ?: return Result.failure(Exception("Failed to get user ID"))
            
            // Create user document with admin privileges
            val adminUser = User(
                uid = userId,
                email = ADMIN_EMAIL,
                username = "Admin",
                profileImageUrl = null,
                bio = "System Administrator - PixelMarket",
                role = "admin",
                isAdmin = true,
                createdAt = Timestamp.now(),
                lastLoginAt = Timestamp.now(),
                totalPurchases = 0,
                totalSales = 0,
                rating = 5.0
            )
            
            // Save to Firestore
            firestore.collection("users")
                .document(userId)
                .set(adminUser)
                .await()
            
            // Sign out after creating
            auth.signOut()
            
            Log.d(TAG, "✅ Admin account created successfully!")
            Log.d(TAG, "Email: $ADMIN_EMAIL")
            Log.d(TAG, "Password: $ADMIN_PASSWORD")
            Log.d(TAG, "UID: $userId")
            
            Result.success("Admin account created successfully!\nEmail: $ADMIN_EMAIL\nPassword: $ADMIN_PASSWORD")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create admin account", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify if the default admin account exists
     */
    suspend fun verifyAdminAccount(): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", ADMIN_EMAIL)
                .whereEqualTo("isAdmin", true)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify admin account", e)
            false
        }
    }
    
    /**
     * Get admin account info (for debugging)
     */
    suspend fun getAdminAccountInfo(): String {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", ADMIN_EMAIL)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                "❌ Admin account not found"
            } else {
                val admin = snapshot.documents[0].toObject(User::class.java)
                """
                ✅ Admin Account Found
                UID: ${admin?.uid}
                Email: ${admin?.email}
                Username: ${admin?.username}
                Is Admin: ${admin?.isAdmin}
                Role: ${admin?.role}
                Created: ${admin?.createdAt}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "❌ Error: ${e.message}"
        }
    }
}
