package com.pixelmarket.app.domain.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val role: String = "buyer", // "buyer", "seller", "both", "admin"
    val isAdmin: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val totalPurchases: Int = 0,
    val totalSales: Int = 0,
    val rating: Double = 0.0,
    val lastLoginAt: Timestamp = Timestamp.now()
)

data class Asset(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val fileType: String = "",
    val price: Double = 0.0,
    val discountPrice: Double? = null,
    val sellerId: String = "",
    val sellerName: String = "",
    val thumbnailUrl: String = "",
    val thumbnailType: String = "image", // "image" or "gif"
    val fileUrls: List<String> = emptyList(),
    val fileSize: String = "", // Human readable (e.g., "2.5 MB")
    val fileSizeBytes: Long = 0L, // Actual bytes
    val tags: List<String> = emptyList(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val downloadCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val featured: Boolean = false,
    val approved: Boolean = true // Admin approval
)

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val orderStatus: String = "completed",
    val orderDate: Timestamp = Timestamp.now()
)

data class OrderItem(
    val assetId: String = "",
    val assetTitle: String = "",
    val price: Double = 0.0
)

data class Download(
    val id: String = "",
    val userId: String = "",
    val assetId: String = "",
    val assetTitle: String = "",
    val thumbnailUrl: String = "",
    val fileType: String = "",
    val downloadDate: Timestamp = Timestamp.now(),
    val localPath: String? = null,
    val fileSize: String = "",
    val downloadStatus: String = "completed" // "pending", "downloading", "completed", "failed"
)

data class ThemeSettings(
    val id: String = "app_theme",
    val primaryColor: String = "#088395", // Teal
    val secondaryColor: String = "#7AB2B2", // Light Teal
    val accentColor: String = "#09637E", // Dark Teal
    val backgroundColor: String = "#EBF4F6", // Very Light Teal
    val surfaceColor: String = "#FFFFFF", // White
    val errorColor: String = "#BA1A1A", // Red
    val updatedAt: Timestamp = Timestamp.now(),
    val updatedBy: String = ""
)

data class AppSettings(
    val id: String = "app_settings",
    val appName: String = "PixelMarket",
    val maintenanceMode: Boolean = false,
    val featuredAssetLimit: Int = 10,
    val minUploadPrice: Double = 0.0,
    val maxUploadPrice: Double = 10000.0,
    val allowedFileTypes: List<String> = listOf("zip", "blend", "fbx", "obj", "png", "jpg", "mp3", "wav"),
    val maxFileSizeMB: Int = 500,
    val requireAdminApproval: Boolean = false,
    val updatedAt: Timestamp = Timestamp.now()
)

data class AdminStats(
    val totalUsers: Int = 0,
    val totalAssets: Int = 0,
    val totalDownloads: Int = 0,
    val totalSales: Int = 0,
    val totalRevenue: Double = 0.0,
    val activeUsers: Int = 0, // Users active in last 30 days
    val pendingApprovals: Int = 0
)

