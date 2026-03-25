package com.pixelmarket.app.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val phoneNumber: String? = null,

    // Role & Permissions
    val role: String = "buyer",                // "buyer", "seller", "both", "admin"
    
    @get:PropertyName("isDeveloper")
    @set:PropertyName("isDeveloper")
    var isDeveloper: Boolean = false,
    
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,

    // Helper fields to capture different naming conventions in Firestore
    @get:PropertyName("developer")
    @set:PropertyName("developer")
    var legacyDeveloper: Boolean = false,

    @get:PropertyName("admin")
    @set:PropertyName("admin")
    var legacyAdmin: Boolean = false,

    // Developer Fields (to avoid Firestore warnings)
    val developerDisplayName: String? = null,
    val developerBio: String? = null,
    val developerApplicationStatus: String? = null,    // "pending", "approved", "rejected"
    val developerApplicationId: String? = null,
    val registeredAsDevAt: Timestamp? = null,

    // Wallet & Financial (Regular User)
    val walletBalance: Double = 0.0,
    val totalSpending: Double = 0.0,
    val totalTopups: Double = 0.0,

    // Developer Financial (only if isDeveloper = true)
    val developerProfileId: String? = null,
    val totalEarnings: Double = 0.0,
    val availableBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val totalWithdrawn: Double = 0.0,

    // Statistics
    val totalPurchases: Int = 0,
    val totalSales: Int = 0,
    val totalDownloads: Int = 0,
    val assetsUploaded: Int = 0,
    val rating: Double = 0.0,

    // Timestamps
    val createdAt: Timestamp = Timestamp.now(),
    val lastLoginAt: Timestamp = Timestamp.now(),
    val developerSince: Timestamp? = null
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
    val sellerAvatarUrl: String? = null,     // creator profile picture
    val sellerRating: Double = 0.0,          // creator overall rating
    val sellerAssetCount: Int = 0,           // total assets by creator
    val thumbnailUrl: String = "",
    val thumbnailType: String = "image",
    val previewVideoUrl: String? = null,     // optional 5–10s preview clip
    val fileUrls: List<String> = emptyList(),
    val fileSize: String = "",
    val fileSizeBytes: Long = 0L,
    val tags: List<String> = emptyList(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val totalRatingSum: Double = 0.0,
    val downloadCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val featured: Boolean = false,
    val approved: Boolean = true
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
    val price: Double = 0.0,
    val sellerId: String = "",
    val sellerName: String = "",
    val downloadDate: Timestamp = Timestamp.now(),
    val localPath: String? = null,
    val fileSize: String = "",
    val fileUrls: List<String> = emptyList(),
    val downloadStatus: String = "completed"
)

data class ThemeSettings(
    val id: String = "app_theme",
    val primaryColor: String = "#6366F1",
    val secondaryColor: String = "#0EA5E9",
    val accentColor: String = "#F43F5E",
    val backgroundColor: String = "#F8FAFC",
    val surfaceColor: String = "#FFFFFF",
    val errorColor: String = "#EF4444",
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
    val developerRegistrationFee: Double = 499.0,   // Fee in INR to become a developer
    val updatedAt: Timestamp = Timestamp.now()
)

data class AdminStats(
    val totalUsers: Int = 0,
    val totalAssets: Int = 0,
    val totalDownloads: Int = 0,
    val totalSales: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalDeveloperFees: Double = 0.0,
    val totalCombinedRevenue: Double = 0.0,
    val activeUsers: Int = 0,
    val pendingApprovals: Int = 0,
    val pendingDeveloperApplications: Int = 0
)

// ── Developer Application ──────────────────────────────────────────────────
// status: "pending_payment" | "pending_review" | "approved" | "rejected"
data class DeveloperApplication(
    val applicationId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val developerDisplayName: String = "",
    val bio: String = "",
    val portfolioUrl: String = "",
    val reason: String = "",
    val fee: Double = 499.0,
    val razorpayPaymentId: String = "",
    val status: String = "pending_payment",       // pending_payment | pending_review | approved | rejected
    val adminNote: String = "",
    val appliedAt: Timestamp = Timestamp.now(),
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String = ""
)

// ── Cart Item ──────────────────────────────────────────────────────────────
data class CartItem(
    val cartItemId: String = "",   // Firestore doc ID (== assetId for uniqueness)
    val assetId: String = "",
    val title: String = "",
    val thumbnailUrl: String = "",
    val price: Double = 0.0,
    val sellerId: String = "",
    val sellerName: String = "",
    val category: String = "",
    val addedAt: Timestamp = Timestamp.now()
)

// ── Transaction ────────────────────────────────────────────────────────────
// type: "topup" | "purchase" | "earning" | "withdrawal" | "developer_fee" | "developer_fee_received"
data class Transaction(
    val transactionId: String = "",
    val id: String = "",                        // Alias for transactionId to match data model
    val userId: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val referenceId: String = "",               // orderId / applicationId / paymentId
    val razorpayPaymentId: String = "",
    val status: String = "completed",           // "completed" | "pending" | "failed"
    val createdAt: Timestamp = Timestamp.now(),
    val timestamp: Timestamp = Timestamp.now()   // Alias for createdAt to match data model
)
