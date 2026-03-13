package com.pixelmarket.app.domain.repository

interface DeveloperRepository {
    suspend fun getDeveloperProfile(userId: String): Result<Map<String, Any>?>
    suspend fun updateDeveloperProfile(userId: String, data: Map<String, Any>): Result<Unit>
    suspend fun getTotalEarnings(userId: String): Double
    suspend fun getAvailableBalance(userId: String): Double
}
