package com.pixelmarket.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.model.Download
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase
) : AssetRepository {

    override fun getFeaturedAssets(): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("assets")
                .whereEqualTo("featured", true)
                .limit(5)
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error fetching featured assets"))
        }
    }

    override fun getTrendingAssets(): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("assets")
                .orderBy("downloadCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error fetching trending assets"))
        }
    }

    override fun getNewReleases(): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("assets")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error fetching new releases"))
        }
    }

    override fun searchAssets(query: String, category: String?): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            var firestoreQuery: com.google.firebase.firestore.Query = firestore.collection("assets")
            
            // 1. Apply category filter if needed (simple equality doesn't need index)
            if (category != null && category != "All") {
                firestoreQuery = firestoreQuery.whereEqualTo("category", category)
            }

            // 2. Apply title range search ONLY if query is not empty
            // Combining this with whereEqualTo above often requires an index, 
            // so we handle it more gracefully.
            if (query.isNotEmpty()) {
                firestoreQuery = firestoreQuery
                    .whereGreaterThanOrEqualTo("title", query)
                    .whereLessThanOrEqualTo("title", query + "\uf8ff")
            }
            
            val snapshot = firestoreQuery.get().await()
            val assets = snapshot.toObjects(Asset::class.java)
                .sortedByDescending { it.createdAt } // Client-side sort to avoid complex indexes
                
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error searching assets"))
        }
    }

    override fun getAssetDetails(assetId: String): Flow<Resource<Asset>> = kotlinx.coroutines.flow.callbackFlow {
        trySend(Resource.Loading())
        val listener = firestore.collection("assets").document(assetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching asset details"))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val asset = snapshot.toObject(Asset::class.java)
                    if (asset != null) {
                        trySend(Resource.Success(asset))
                    } else {
                        trySend(Resource.Error("Asset not found"))
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getAllAssets(): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("assets")
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
                .sortedByDescending { it.createdAt }
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error fetching assets"))
        }
    }

    override fun getUserAssets(userId: String): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("assets")
                .whereEqualTo("sellerId", userId)
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
                .sortedByDescending { it.createdAt } // Client-side sort to avoid index
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error fetching user assets"))
        }
    }

    override suspend fun createAsset(asset: Asset): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("assets")
                .document(asset.id)
                .set(asset)
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error creating asset"))
        }
    }

    override suspend fun deleteAsset(assetId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("assets")
                .document(assetId)
                .delete()
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error deleting asset"))
        }
    }

    override suspend fun purchaseAsset(userId: String, asset: Asset): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Save to user's purchases
            firestore.collection("users")
                .document(userId)
                .collection("purchases")
                .document(asset.id)
                .set(asset)
                .await()
            
            // Increment download/sale count on the asset itself
            try {
                firestore.collection("assets")
                    .document(asset.id)
                    .update("downloadCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()
            } catch (e: Exception) {
                // Ignore if we don't have permission to update global counts
                android.util.Log.e("AssetRepository", "Failed to increment download count", e)
            }

            // 3. Notify Seller instantly (RTDB)
            try {
                database.getReference("instant_alerts")
                    .child(asset.sellerId)
                    .setValue(mapOf(
                        "title" to "Asset Sold! 💰",
                        "message" to "You sold '${asset.title}' for ₹${asset.price}!",
                        "type" to "success",
                        "timestamp" to System.currentTimeMillis()
                    ))
            } catch (e: Exception) { /* soft fail */ }

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error purchasing asset"))
        }
    }

    override fun getPurchasedAssets(userId: String): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("purchases")
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            // Silently emit empty list — do not expose permission or network errors to the UI
            android.util.Log.w("AssetRepository", "getPurchasedAssets failed (silent): ${e.message}")
            emit(Resource.Success(emptyList()))
        }
    }

    override suspend fun isAssetPurchased(userId: String, assetId: String): Boolean {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("purchases")
                .document(assetId)
                .get()
                .await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun recordDownload(userId: String, asset: Asset): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val downloadId = firestore.collection("downloads").document().id
            
            // Create download record
            val download = Download(
                id = downloadId,
                userId = userId,
                assetId = asset.id,
                assetTitle = asset.title,
                thumbnailUrl = asset.thumbnailUrl,
                fileType = asset.fileType,
                price = asset.price,
                sellerId = asset.sellerId,
                sellerName = asset.sellerName,
                downloadDate = Timestamp.now(),
                fileSize = asset.fileSize,
                fileUrls = asset.fileUrls,
                downloadStatus = "completed"
            )
            
            // Save to global downloads collection
            firestore.collection("downloads")
                .document(downloadId)
                .set(download)
                .await()
            
            // Save to user's downloads subcollection for easy querying
            firestore.collection("users")
                .document(userId)
                .collection("downloads")
                .document(downloadId)
                .set(download)
                .await()
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error recording download"))
        }
    }

    override fun getUserDownloads(userId: String): Flow<Resource<List<Download>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("downloads")
                .orderBy("downloadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val downloads = snapshot.toObjects(Download::class.java)
            emit(Resource.Success(downloads))
        } catch (e: Exception) {
            // Silently emit empty list — do not expose permission or network errors to the UI
            android.util.Log.w("AssetRepository", "getUserDownloads failed (silent): ${e.message}")
            emit(Resource.Success(emptyList()))
        }
    }

    override suspend fun likeAsset(userId: String, assetId: String): Result<Unit> {
        return try {
            // Add like document to user's likes subcollection
            firestore.collection("users")
                .document(userId)
                .collection("likes")
                .document(assetId)
                .set(mapOf("assetId" to assetId, "likedAt" to com.google.firebase.Timestamp.now()))
                .await()
            // Atomically increment likeCount on asset
            firestore.collection("assets")
                .document(assetId)
                .update("likeCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikeAsset(userId: String, assetId: String): Result<Unit> {
        return try {
            // Remove like document
            firestore.collection("users")
                .document(userId)
                .collection("likes")
                .document(assetId)
                .delete()
                .await()
            // Atomically decrement likeCount on asset
            firestore.collection("assets")
                .document(assetId)
                .update("likeCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAssetLiked(userId: String, assetId: String): Boolean {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("likes")
                .document(assetId)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}

