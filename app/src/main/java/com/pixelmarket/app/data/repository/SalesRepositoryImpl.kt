package com.pixelmarket.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.repository.SalesRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SalesRepository {

    override suspend fun recordSale(saleData: Map<String, Any>): Result<String> {
        return try {
            val docRef = firestore.collection("sales")
                .add(saleData)
                .await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserSales(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection("sales")
                .whereEqualTo("buyerId", userId)
                .get()
                .await()
            
            val sales = snapshot.documents.mapNotNull { it.data }
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDeveloperSales(developerId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection("sales")
                .whereEqualTo("developerId", developerId)
                .get()
                .await()
            
            val sales = snapshot.documents.mapNotNull { it.data }
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
