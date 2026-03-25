package com.pixelmarket.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.repository.DeveloperRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DeveloperRepository {

    override suspend fun getDeveloperProfile(userId: String): Result<Map<String, Any>?> {
        return try {
            val doc = firestore.collection("developers")
                .document(userId)
                .get()
                .await()
            
            Result.success(doc.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDeveloperProfile(userId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("developers")
                .document(userId)
                .update(data)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTotalEarnings(userId: String): Double {
        return try {
            val doc = firestore.collection("developers")
                .document(userId)
                .get()
                .await()
            
            doc.getDouble("totalEarnings") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    override suspend fun getAvailableBalance(userId: String): Double {
        return try {
            val doc = firestore.collection("developers")
                .document(userId)
                .get()
                .await()
            
            doc.getDouble("availableBalance") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
