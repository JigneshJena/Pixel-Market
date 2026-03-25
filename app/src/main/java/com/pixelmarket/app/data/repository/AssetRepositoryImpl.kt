package com.pixelmarket.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AssetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
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
            var firestoreQuery = firestore.collection("assets")
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + "\uf8ff")
            
            if (category != null && category != "All") {
                firestoreQuery = firestoreQuery.whereEqualTo("category", category)
            }
            
            val snapshot = firestoreQuery.get().await()
            val assets = snapshot.toObjects(Asset::class.java)
            emit(Resource.Success(assets))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error searching assets"))
        }
    }

    override fun getAssetDetails(assetId: String): Flow<Resource<Asset>> = flow {
        emit(Resource.Loading())
        try {
            val doc = firestore.collection("assets").document(assetId).get().await()
            val asset = doc.toObject(Asset::class.java) ?: throw Exception("Asset not found")
            emit(Resource.Success(asset))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error fetching asset details"))
        }
    }

    override fun getAllAssets(): Flow<Resource<List<Asset>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = firestore.collection("assets")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
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
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val assets = snapshot.toObjects(Asset::class.java)
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
}
