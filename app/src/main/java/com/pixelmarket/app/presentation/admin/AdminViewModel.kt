package com.pixelmarket.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.pixelmarket.app.domain.model.*
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) : ViewModel() {

    private val _adminStats = MutableStateFlow(AdminStats())
    val adminStats: StateFlow<AdminStats> = _adminStats

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
    val assets: StateFlow<List<Asset>> = _assets

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings

    private val _uiState = MutableStateFlow<Resource<String>?>(null)
    val uiState: StateFlow<Resource<String>?> = _uiState

    // ── Developer Applications ────────────────────────────────────────────
    private val _developerApplications = MutableStateFlow<List<DeveloperApplication>>(emptyList())
    val developerApplications: StateFlow<List<DeveloperApplication>> = _developerApplications

    init {
        observeAdminStats()
        observeUsers()
        observeAssets()
        observeThemeSettings()
        observeAppSettings()
        observeDeveloperApplications()
    }

    private fun observeAdminStats() {
        // Decouple all listeners so one failure doesn't block the rest
        
        // 1. Revenue Calculation (Orders + Developer Fees)
        firestore.collection("orders").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                val revenue = snapshot?.documents?.sumOf { it.getDouble("totalAmount") ?: 0.0 } ?: 0.0
                val sales = snapshot?.size() ?: 0
                _adminStats.value = _adminStats.value.copy(totalRevenue = revenue, totalSales = sales)
                calculateTotalRevenue()
            }
        }

        firestore.collection("transactions")
            .whereEqualTo("status", "completed")
            .addSnapshotListener { snapshot, _ ->
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val feeRevenue = snapshot?.documents?.filter { 
                        it.getString("type") == "developer_fee_received" || it.getString("type") == "developer_registration"
                    }?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                    
                    _adminStats.value = _adminStats.value.copy(totalDeveloperFees = feeRevenue)
                    calculateTotalRevenue()
                }
            }

        // 2. Users count
        firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                _adminStats.value = _adminStats.value.copy(totalUsers = snapshot?.size() ?: 0)
            }
        }

        // 3. Assets count / Pending approval
        firestore.collection("assets").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                val total = snapshot?.size() ?: 0
                val pending = snapshot?.documents?.count { it.getBoolean("approved") == false } ?: 0
                _adminStats.value = _adminStats.value.copy(totalAssets = total, pendingApprovals = pending)
            }
        }

        // 4. Downloads count
        firestore.collection("downloads").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                _adminStats.value = _adminStats.value.copy(totalDownloads = snapshot?.size() ?: 0)
            }
        }

        // 5. Developer Applications pending count
        firestore.collection("developer_applications")
            .whereEqualTo("status", "pending_review")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("AdminViewModel", "PENDING APPS ERROR: ${error.message}")
                    return@addSnapshotListener
                }
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val count = snapshot?.size() ?: 0
                    android.util.Log.d("AdminViewModel", "Found $count pending developer applications")
                    _adminStats.value = _adminStats.value.copy(pendingDeveloperApplications = count)
                }
            }
    }

    private fun observeUsers() {
        firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                val usersList = snapshot?.documents?.mapNotNull { doc ->
                    try { 
                        doc.toObject(User::class.java)?.copy(uid = doc.id) 
                    } catch (e: Exception) { 
                        android.util.Log.e("AdminViewModel", "FAILED to map User ${doc.id}: ${e.message}")
                        null 
                    }
                } ?: emptyList()
                _users.value = usersList
            }
        }
    }

    private fun observeAssets() {
        firestore.collection("assets").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                val assetsList = snapshot?.documents?.mapNotNull { doc ->
                    try { 
                        doc.toObject(Asset::class.java)?.copy(id = doc.id) 
                    } catch (e: Exception) { 
                        android.util.Log.e("AdminViewModel", "FAILED to map Asset ${doc.id}: ${e.message}")
                        null 
                    }
                } ?: emptyList()
                _assets.value = assetsList
            }
        }
    }

    private fun observeThemeSettings() {
        firestore.collection("settings").document("theme").addSnapshotListener { snapshot, _ ->
            _themeSettings.value = snapshot?.toObject(ThemeSettings::class.java) ?: ThemeSettings()
        }
    }

    private fun observeAppSettings() {
        firestore.collection("settings").document("app").addSnapshotListener { snapshot, _ ->
            _appSettings.value = snapshot?.toObject(AppSettings::class.java) ?: AppSettings()
        }
    }

    private fun observeDeveloperApplications() {
        _uiState.value = Resource.Loading()
        firestore.collection("developer_applications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("AdminViewModel", "APPS LIST ERROR: ${error.message}")
                    _uiState.value = Resource.Error("Error loading applications: ${error.message}")
                    return@addSnapshotListener
                }
                
                val docCount = snapshot?.size() ?: 0
                android.util.Log.d("AdminViewModel", "Received snapshot with $docCount developer applications")
                
                // Move heavy mapping to background thread
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val apps = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val app = doc.toObject(DeveloperApplication::class.java)
                            if (app == null) {
                                android.util.Log.e("AdminViewModel", "toObject returned NULL for app ${doc.id}")
                            }
                            app?.copy(applicationId = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("AdminViewModel", "FAILED to map DeveloperApplication ${doc.id}: ${e.message}")
                            e.printStackTrace()
                            null 
                        }
                    } ?: emptyList()
                    
                    android.util.Log.d("AdminViewModel", "Successfully mapped ${apps.size} / $docCount applications")
                    _developerApplications.value = apps
                    
                    // Clear loading state after data arrives
                    if (_uiState.value is Resource.Loading) {
                        _uiState.value = null
                    }
                }
            }
    }

    // ── Approve Developer Application ────────────────────────────────────
    fun approveDeveloperApplication(app: DeveloperApplication) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                val adminId = auth.currentUser?.uid ?: ""

                // 1. Update application status
                firestore.collection("developer_applications")
                    .document(app.applicationId)
                    .update(
                        mapOf(
                            "status" to "approved",
                            "reviewedAt" to Timestamp.now(),
                            "reviewedBy" to adminId
                        )
                    ).await()

                // 2. Promote user to developer
                firestore.collection("users")
                    .document(app.userId)
                    .update(
                        mapOf(
                            "isDeveloper" to true,
                            "role" to "seller",
                            "developerDisplayName" to app.developerDisplayName,
                            "developerBio" to app.bio,
                            "developerApplicationStatus" to "approved",
                            "developerSince" to Timestamp.now()
                        )
                    ).await()

                // 3. Create developer profile document
                firestore.collection("developers")
                    .document(app.userId)
                    .set(
                        mapOf(
                            "userId" to app.userId,
                            "displayName" to app.developerDisplayName,
                            "bio" to app.bio,
                            "portfolioUrl" to app.portfolioUrl,
                            "totalEarnings" to 0.0,
                            "availableBalance" to 0.0,
                            "totalAssets" to 0,
                            "totalSales" to 0,
                            "approvedAt" to Timestamp.now()
                        )
                    ).await()

                // 4. Record fee received transaction for admin/platform
                val txId = firestore.collection("transactions").document().id
                firestore.collection("transactions").document(txId).set(
                    mapOf(
                        "transactionId" to txId,
                        "userId" to "platform",
                        "type" to "developer_fee_received",
                        "amount" to app.fee,
                        "description" to "Developer Registration Fee from ${app.userName}",
                        "referenceId" to app.applicationId,
                        "razorpayPaymentId" to app.razorpayPaymentId,
                        "developerId" to app.userId,
                        "status" to "completed",
                        "createdAt" to Timestamp.now()
                    )
                ).await()

                // 5. Instant Realtime Notification
                database.getReference("instant_alerts")
                    .child(app.userId)
                    .setValue(mapOf(
                        "title" to "🎉 You're a Developer!",
                        "message" to "Congratulations! Your developer application has been approved. You can now upload assets.",
                        "type" to "success",
                        "timestamp" to System.currentTimeMillis()
                    ))

                _uiState.value = Resource.Success("✅ ${app.userName} is now a Developer!")

            } catch (e: Exception) {
                _uiState.value = Resource.Error("Approval failed: ${e.message}")
            }
        }
    }

    // ── Reject Developer Application ─────────────────────────────────────
    fun rejectDeveloperApplication(app: DeveloperApplication, adminNote: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                val adminId = auth.currentUser?.uid ?: ""

                // 1. Update application
                firestore.collection("developer_applications")
                    .document(app.applicationId)
                    .update(
                        mapOf(
                            "status" to "rejected",
                            "adminNote" to adminNote,
                            "reviewedAt" to Timestamp.now(),
                            "reviewedBy" to adminId
                        )
                    ).await()

                // 2. Update user document
                firestore.collection("users")
                    .document(app.userId)
                    .update("developerApplicationStatus", "rejected")
                    .await()

                // 3. Note: refund is handled manually via Razorpay dashboard
                // Record a note transaction
                val txId = firestore.collection("transactions").document().id
                firestore.collection("transactions").document(txId).set(
                    mapOf(
                        "transactionId" to txId,
                        "userId" to app.userId,
                        "type" to "developer_fee_refund_note",
                        "amount" to app.fee,
                        "description" to "Developer application rejected – manual refund required",
                        "referenceId" to app.applicationId,
                        "razorpayPaymentId" to app.razorpayPaymentId,
                        "status" to "pending_refund",
                        "adminNote" to adminNote,
                        "createdAt" to Timestamp.now()
                    )
                ).await()

                // 4. Instant Realtime Notification
                database.getReference("instant_alerts")
                    .child(app.userId)
                    .setValue(mapOf(
                        "title" to "Update on Developer App",
                        "message" to "Your developer application was reviewed. Note: $adminNote",
                        "type" to "error",
                        "timestamp" to System.currentTimeMillis()
                    ))

                _uiState.value = Resource.Success("Application rejected. Initiate refund via Razorpay dashboard.")

            } catch (e: Exception) {
                _uiState.value = Resource.Error("Rejection failed: ${e.message}")
            }
        }
    }

    fun updateThemeSettings(themeSettings: ThemeSettings, updatedBy: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                val updatedTheme = themeSettings.copy(
                    updatedAt = Timestamp.now(),
                    updatedBy = updatedBy
                )
                firestore.collection("settings").document("theme").set(updatedTheme).await()
                _uiState.value = Resource.Success("Theme updated for all users!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed: ${e.message}")
            }
        }
    }

    fun updateUserAdminStatus(userId: String, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                firestore.collection("users").document(userId)
                    .update(mapOf(
                        "isAdmin" to isAdmin,
                        "admin" to isAdmin, // Helper for rules
                        "role" to if (isAdmin) "admin" else "buyer"
                    )).await()
                _uiState.value = Resource.Success("User updated!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed: ${e.message}")
            }
        }
    }

    fun updateUserRole(userId: String, role: String, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                firestore.collection("users").document(userId)
                    .update(mapOf(
                        "role" to role, 
                        "isAdmin" to isAdmin,
                        "admin" to isAdmin // Helper for rules
                    )).await()
                _uiState.value = Resource.Success("User role updated successfully!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Update Failed: ${e.message}")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                // delete from users collection
                firestore.collection("users").document(userId).delete().await()
                // Soft delete from developers if exists
                firestore.collection("developers").document(userId).delete()
                
                _uiState.value = Resource.Success("User deleted successfully!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Delete Failed: ${e.message}")
            }
        }
    }

    fun approveAsset(assetId: String, approved: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                
                // 1. Get asset details to find seller
                val doc = firestore.collection("assets").document(assetId).get().await()
                val sellerId = doc.getString("sellerId")
                val title = doc.getString("title") ?: "Your asset"

                // 2. Update status
                firestore.collection("assets").document(assetId).update("approved", approved).await()
                
                // 3. Instant Notification
                if (sellerId != null && approved) {
                    database.getReference("instant_alerts")
                        .child(sellerId)
                        .setValue(mapOf(
                            "title" to "Asset Approved! ✅",
                            "message" to "Congratulations! Your asset '$title' is now live.",
                            "type" to "success",
                            "timestamp" to System.currentTimeMillis()
                        ))
                }

                _uiState.value = Resource.Success("Asset ${if (approved) "approved" else "rejected"}!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed: ${e.message}")
            }
        }
    }

    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                firestore.collection("assets").document(assetId).delete().await()
                _uiState.value = Resource.Success("Asset deleted!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed: ${e.message}")
            }
        }
    }

    fun toggleAssetFeatured(assetId: String, featured: Boolean) {
        viewModelScope.launch {
            try {
                firestore.collection("assets").document(assetId).update("featured", featured).await()
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed: ${e.message}")
            }
        }
    }

    private fun calculateTotalRevenue() {
        val stats = _adminStats.value
        _adminStats.value = stats.copy(
            totalCombinedRevenue = stats.totalRevenue + stats.totalDeveloperFees
        )
    }

    fun toggleMaintenanceMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                database.getReference("settings/maintenanceMode").setValue(enabled).await()
                _uiState.value = Resource.Success("Maintenance mode ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to update status: ${e.message}")
            }
        }
    }

    fun sendBroadcast(title: String, message: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                database.getReference("broadcast").setValue(mapOf(
                    "title" to title,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                )).await()
                _uiState.value = Resource.Success("Broadcast sent to all users!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Broadcast failed: ${e.message}")
            }
        }
    }

    fun clearUiState() { _uiState.value = null }
}
