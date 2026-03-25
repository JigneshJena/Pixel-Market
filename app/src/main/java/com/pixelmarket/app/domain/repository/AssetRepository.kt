package com.pixelmarket.app.domain.repository

import com.pixelmarket.app.domain.model.Asset
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
}
