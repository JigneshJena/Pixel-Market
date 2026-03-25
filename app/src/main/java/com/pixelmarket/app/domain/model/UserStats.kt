package com.pixelmarket.app.domain.model

data class UserStats(
    val totalSales: Int = 0,
    val totalPurchases: Int = 0,
    val totalDownloads: Int = 0,
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val assetsUploaded: Int = 0,
    val favoriteCount: Int = 0,
    val accountBalance: Double = 0.0
)
