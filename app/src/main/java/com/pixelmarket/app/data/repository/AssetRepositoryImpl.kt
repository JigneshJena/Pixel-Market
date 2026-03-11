package com.pixelmarket.app.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
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

    override fun getFeaturedAssets(): Flow<Resource<List<Asset>>> = callbackFlow {
        trySend(Resource.Loading())
        val registration = firestore.collection("assets")
            .whereEqualTo("featured", true)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching featured assets"))
                    return@addSnapshotListener
                }
                val assets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
                trySend(Resource.Success(assets.sortedByDescending { it.createdAt }))
            }
        awaitClose { registration.remove() }
    }

    override fun getTrendingAssets(): Flow<Resource<List<Asset>>> = callbackFlow {
        trySend(Resource.Loading())
        val registration = firestore.collection("assets")
            .orderBy("downloadCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(15)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching trending assets"))
                    return@addSnapshotListener
                }
                val assets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
                trySend(Resource.Success(assets))
            }
        awaitClose { registration.remove() }
    }

    override fun getNewReleases(): Flow<Resource<List<Asset>>> = callbackFlow {
        trySend(Resource.Loading())
        val registration = firestore.collection("assets")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching new releases"))
                    return@addSnapshotListener
                }
                val assets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
                trySend(Resource.Success(assets))
            }
        awaitClose { registration.remove() }
    }

    override fun searchAssets(query: String, category: String?): Flow<Resource<List<Asset>>> = callbackFlow {
        trySend(Resource.Loading())
        var firestoreQuery: com.google.firebase.firestore.Query = firestore.collection("assets")
        
        if (category != null && category != "All") {
            firestoreQuery = firestoreQuery.whereEqualTo("category", category)
        }

        if (query.isNotEmpty()) {
            firestoreQuery = firestoreQuery
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + "\uf8ff")
        }
        
        val registration = firestoreQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.localizedMessage ?: "Error searching assets"))
                return@addSnapshotListener
            }
            val assets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
            trySend(Resource.Success(assets.sortedByDescending { it.createdAt }))
        }
        awaitClose { registration.remove() }
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

    override fun getAllAssets(): Flow<Resource<List<Asset>>> = callbackFlow {
        trySend(Resource.Loading())
        val registration = firestore.collection("assets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching assets"))
                    return@addSnapshotListener
                }
                val assets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
                trySend(Resource.Success(assets.sortedByDescending { it.createdAt }))
            }
        awaitClose { registration.remove() }
    }

    override fun getUserAssets(userId: String): Flow<Resource<List<Asset>>> = callbackFlow {
        trySend(Resource.Loading())
        val registration = firestore.collection("assets")
            .whereEqualTo("sellerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error fetching user assets"))
                    return@addSnapshotListener
                }
                val assets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
                trySend(Resource.Success(assets.sortedByDescending { it.createdAt }))
            }
        awaitClose { registration.remove() }
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
            // 1. Save to buyer's purchases subcollection
            firestore.collection("users")
                .document(userId)
                .collection("purchases")
                .document(asset.id)
                .set(asset)
                .await()

            // 2. Credit the SELLER's earnings (atomic increment — soft-fail)
            if (asset.sellerId.isNotEmpty() && asset.price > 0) {
                try {
                    firestore.collection("users")
                        .document(asset.sellerId)
                        .update(
                            mapOf(
                                "totalEarnings"    to com.google.firebase.firestore.FieldValue.increment(asset.price),
                                "availableBalance" to com.google.firebase.firestore.FieldValue.increment(asset.price),
                                "totalSales"       to com.google.firebase.firestore.FieldValue.increment(1)
                            )
                        )
                        .await()
                    Log.i("AssetRepository", "Seller ${asset.sellerId} credited ₹${asset.price} for '${asset.title}'")
                } catch (e: Exception) {
                    Log.e("AssetRepository", "❌ Seller credit FAILED for ${asset.sellerId}: ${e.message}")
                }
            }

            // 3. Write a sale record to global `sales` collection (admin reporting)
            try {
                val saleId = firestore.collection("sales").document().id
                firestore.collection("sales").document(saleId).set(
                    mapOf(
                        "saleId"       to saleId,
                        "assetId"      to asset.id,
                        "assetTitle"   to asset.title,
                        "buyerId"      to userId,
                        "developerId"  to asset.sellerId,
                        "sellerName"   to asset.sellerName,
                        "amount"       to asset.price,
                        "soldAt"       to com.google.firebase.Timestamp.now()
                    )
                ).await()
            } catch (e: Exception) {
                Log.w("AssetRepository", "Sale record skipped (soft-fail): ${e.message}")
            }

            // 4. Soft-fail: increment downloadCount on the asset document
            try {
                firestore.collection("assets")
                    .document(asset.id)
                    .update("downloadCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()
            } catch (e: Exception) {
                Log.w("AssetRepository", "downloadCount increment skipped (no write perm, ok): ${e.message}")
            }

            // 5. Notify Seller instantly via RTDB
            try {
                database.getReference("instant_alerts")
                    .child(asset.sellerId)
                    .setValue(mapOf(
                        "title"     to "Asset Sold! 💰",
                        "message"   to "You sold '${asset.title}' for ₹${asset.price}!",
                        "type"      to "success",
                        "timestamp" to System.currentTimeMillis()
                    ))
            } catch (e: Exception) { /* soft fail */ }

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error purchasing asset"))
        }
    }

    override fun getPurchasedAssets(userId: String): Flow<Resource<List<Asset>>> = callbackFlow {
        trySend(Resource.Loading())
        val registration = firestore.collection("users")
            .document(userId)
            .collection("purchases")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Success(emptyList()))
                    return@addSnapshotListener
                }
                val assets = snapshot?.toObjects(Asset::class.java) ?: emptyList()
                trySend(Resource.Success(assets.sortedByDescending { it.updatedAt }))
            }
        awaitClose { registration.remove() }
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
            // ── Guard: skip if user already has a download record for this asset ──
            // Prevents duplicate entries in Downloads Library for the same account
            val existingCheck = firestore.collection("users")
                .document(userId)
                .collection("downloads")
                .whereEqualTo("assetId", asset.id)
                .limit(1)
                .get()
                .await()

            if (!existingCheck.isEmpty) {
                // Already downloaded — skip silently and return success
                Log.i("AssetRepository", "recordDownload: already recorded for ${asset.id}, skipping")
                emit(Resource.Success(Unit))
                return@flow
            }

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

    override fun getUserDownloads(userId: String): Flow<Resource<List<Download>>> = callbackFlow {
        trySend(Resource.Loading())
        val registration = firestore.collection("users")
            .document(userId)
            .collection("downloads")
            // NOTE: No orderBy here — avoids requiring a composite Firestore index.
            // We sort client-side below.
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AssetRepository", "getUserDownloads error: ${error.message}")
                    trySend(Resource.Success(emptyList()))
                    return@addSnapshotListener
                }
                val downloads = snapshot?.toObjects(Download::class.java) ?: emptyList()
                // Sort newest-first on the client
                trySend(Resource.Success(downloads.sortedByDescending { it.downloadDate }))
            }
        awaitClose { registration.remove() }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ✅ LIKES — Realtime Database (instant, no permission issues)
    // Path: user_likes/{userId}/{assetId} = true
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun likeAsset(userId: String, assetId: String): Result<Unit> {
        return try {
            // 1. Write to RTDB — authenticated users always have write access to their own node
            database.getReference("user_likes/$userId/$assetId")
                .setValue(true)
                .await()

            // 2. Increment like count in RTDB (atomic transaction)
            database.getReference("asset_like_counts/$assetId")
                .runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(data: MutableData): com.google.firebase.database.Transaction.Result {
                        val current = data.getValue(Long::class.java) ?: 0L
                        data.value = current + 1
                        return com.google.firebase.database.Transaction.success(data)
                    }
                    override fun onComplete(e: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                })

            // 3. Soft-fail: sync likeCount to Firestore for analytics
            try {
                firestore.collection("assets").document(assetId)
                    .update("likeCount", FieldValue.increment(1)).await()
            } catch (e: Exception) {
                Log.w("AssetRepository", "Firestore likeCount sync skipped: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikeAsset(userId: String, assetId: String): Result<Unit> {
        return try {
            // 1. Remove from RTDB
            database.getReference("user_likes/$userId/$assetId")
                .removeValue()
                .await()

            // 2. Decrement like count in RTDB (atomic transaction, floor at 0)
            database.getReference("asset_like_counts/$assetId")
                .runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(data: MutableData): com.google.firebase.database.Transaction.Result {
                        val current = data.getValue(Long::class.java) ?: 0L
                        data.value = maxOf(0L, current - 1)
                        return com.google.firebase.database.Transaction.success(data)
                    }
                    override fun onComplete(e: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                })

            // 3. Soft-fail: sync likeCount to Firestore
            try {
                firestore.collection("assets").document(assetId)
                    .update("likeCount", FieldValue.increment(-1)).await()
            } catch (e: Exception) {
                Log.w("AssetRepository", "Firestore likeCount sync skipped: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isAssetLiked(userId: String, assetId: String): Flow<Boolean> = callbackFlow {
        // Real-time listener on RTDB — updates instantly when like is added/removed
        val ref = database.getReference("user_likes/$userId/$assetId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists() && snapshot.getValue(Boolean::class.java) == true)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AssetRepository", "isAssetLiked cancelled: ${error.message}")
                trySend(false)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun getAssetLikeCount(assetId: String): Flow<Int> = callbackFlow {
        // Real-time RTDB listener for like count — only emits when RTDB has data
        val ref = database.getReference("asset_like_counts/$assetId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    trySend(snapshot.getValue(Long::class.java)?.toInt() ?: 0)
                }
                // If no RTDB data yet, don't emit — ViewModel keeps Firestore value
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AssetRepository", "getAssetLikeCount cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ✅ RATINGS — Realtime Database (instant, no permission issues)
    // Path: user_ratings/{userId}/{assetId} = { rating: float, timestamp: long }
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun rateAsset(assetId: String, rating: Float): Result<Unit> {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(Exception("User not logged in"))

        return try {
            // 1. Write rating to RTDB — always succeeds for authenticated users
            database.getReference("user_ratings/$userId/$assetId")
                .setValue(mapOf(
                    "rating" to rating,
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()

            // 2. Soft-fail: update rolling average in Firestore for display
            try {
                firestore.runTransaction { transaction ->
                    val assetRef = firestore.collection("assets").document(assetId)
                    val assetDoc = transaction.get(assetRef)
                    val currentCount = assetDoc.getLong("reviewCount") ?: 0L
                    val currentRating = assetDoc.getDouble("rating") ?: 0.0
                    val currentSum = assetDoc.getDouble("totalRatingSum")
                        ?: (currentRating * currentCount)
                    val newSum = currentSum + rating
                    val newCount = currentCount + 1
                    transaction.update(assetRef, mapOf(
                        "totalRatingSum" to newSum,
                        "reviewCount"    to newCount,
                        "rating"         to newSum / newCount
                    ))
                }.await()
            } catch (e: Exception) {
                Log.w("AssetRepository", "Firestore rating sync skipped: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun hasUserRated(userId: String, assetId: String): Flow<Boolean> = callbackFlow {
        // Real-time listener on RTDB — updates instantly after rating is submitted
        val ref = database.getReference("user_ratings/$userId/$assetId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AssetRepository", "hasUserRated cancelled: ${error.message}")
                trySend(false)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}

