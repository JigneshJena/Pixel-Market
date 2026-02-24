package com.pixelmarket.app.domain.repository

import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.model.Download
import com.pixelmarket.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    // Read operations
    fun getFeaturedAssets(): Flow<Resource<List<Asset>>>
    fun getTrendingAssets(): Flow<Resource<List<Asset>>>
    fun getNewReleases(): Flow<Resource<List<Asset>>>
    fun getAllAssets(): Flow<Resource<List<Asset>>>
    fun searchAssets(query: String, category: String?): Flow<Resource<List<Asset>>>
    fun getAssetDetails(assetId: String): Flow<Resource<Asset>>
    fun getUserAssets(userId: String): Flow<Resource<List<Asset>>>
    
    // Write operations
    suspend fun createAsset(asset: Asset): Flow<Resource<Unit>>
    suspend fun deleteAsset(assetId: String): Flow<Resource<Unit>>
    
    // Purchase operations
    suspend fun purchaseAsset(userId: String, asset: Asset): Flow<Resource<Unit>>
    fun getPurchasedAssets(userId: String): Flow<Resource<List<Asset>>>
    suspend fun isAssetPurchased(userId: String, assetId: String): Boolean
    
    // Download operations
    suspend fun recordDownload(userId: String, asset: Asset): Flow<Resource<Unit>>
    fun getUserDownloads(userId: String): Flow<Resource<List<Download>>>

    // Like operations
    suspend fun likeAsset(userId: String, assetId: String): Result<Unit>
    suspend fun unlikeAsset(userId: String, assetId: String): Result<Unit>
    suspend fun isAssetLiked(userId: String, assetId: String): Boolean
}
