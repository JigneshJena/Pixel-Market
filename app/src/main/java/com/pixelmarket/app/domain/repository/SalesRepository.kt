package com.pixelmarket.app.domain.repository

interface SalesRepository {
    suspend fun recordSale(saleData: Map<String, Any>): Result<String>
    suspend fun getUserSales(userId: String): Result<List<Map<String, Any>>>
    suspend fun getDeveloperSales(developerId: String): Result<List<Map<String, Any>>>
}
