package com.pixelmarket.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val firestore: FirebaseFirestore
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

    init {
        loadAdminStats()
        loadUsers()
        loadAssets()
        loadThemeSettings()
        loadAppSettings()
    }

    /**
     * Load admin statistics
     */
    private fun loadAdminStats() {
        viewModelScope.launch {
            try {
                val usersSnapshot = firestore.collection("users").get().await()
                val assetsSnapshot = firestore.collection("assets").get().await()
                val downloadsSnapshot = firestore.collection("downloads").get().await()
                val ordersSnapshot = firestore.collection("orders").get().await()
                
                val totalRevenue = ordersSnapshot.documents.sumOf { 
                    it.getDouble("totalAmount") ?: 0.0 
                }
                
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
                val activeUsers = usersSnapshot.documents.count { doc ->
                    val lastLogin = doc.getTimestamp("lastLoginAt")?.toDate()?.time ?: 0
                    lastLogin > thirtyDaysAgo
                }
                
                val pendingApprovals = assetsSnapshot.documents.count { doc ->
                    doc.getBoolean("approved") == false
                }
                
                _adminStats.value = AdminStats(
                    totalUsers = usersSnapshot.size(),
                    totalAssets = assetsSnapshot.size(),
                    totalDownloads = downloadsSnapshot.size(),
                    totalSales = ordersSnapshot.size(),
                    totalRevenue = totalRevenue,
                    activeUsers = activeUsers,
                    pendingApprovals = pendingApprovals
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Load all users
     */
    fun loadUsers() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").get().await()
                _users.value = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to load users: ${e.message}")
            }
        }
    }

    /**
     * Load all assets
     */
    fun loadAssets() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("assets").get().await()
                _assets.value = snapshot.documents.mapNotNull { it.toObject(Asset::class.java) }
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to load assets: ${e.message}")
            }
        }
    }

    /**
     * Load theme settings
     */
    private fun loadThemeSettings() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("settings").document("theme").get().await()
                _themeSettings.value = doc.toObject(ThemeSettings::class.java) ?: ThemeSettings()
            } catch (e: Exception) {
                _themeSettings.value = ThemeSettings()
            }
        }
    }

    /**
     * Load app settings
     */
    private fun loadAppSettings() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("settings").document("app").get().await()
                _appSettings.value = doc.toObject(AppSettings::class.java) ?: AppSettings()
            } catch (e: Exception) {
                _appSettings.value = AppSettings()
            }
        }
    }

    /**
     * Update theme settings
     */
    fun updateThemeSettings(themeSettings: ThemeSettings, updatedBy: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                val updatedTheme = themeSettings.copy(
                    updatedAt = com.google.firebase.Timestamp.now(),
                    updatedBy = updatedBy
                )
                firestore.collection("settings")
                    .document("theme")
                    .set(updatedTheme)
                    .await()
                _themeSettings.value = updatedTheme
                _uiState.value = Resource.Success("Theme updated successfully!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to update theme: ${e.message}")
            }
        }
    }

    /**
     * Update app settings
     */
    fun updateAppSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                val updatedSettings = appSettings.copy(
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                firestore.collection("settings")
                    .document("app")
                    .set(updatedSettings)
                    .await()
                _appSettings.value = updatedSettings
                _uiState.value = Resource.Success("Settings updated successfully!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to update settings: ${e.message}")
            }
        }
    }

    /**
     * Update user admin status
     */
    fun updateUserAdminStatus(userId: String, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                firestore.collection("users")
                    .document(userId)
                    .update("isAdmin", isAdmin, "role", if (isAdmin) "admin" else "buyer")
                    .await()
                loadUsers() // Reload users
                _uiState.value = Resource.Success("User updated successfully!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to update user: ${e.message}")
            }
        }
    }

    /**
     * Delete user
     */
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                firestore.collection("users")
                    .document(userId)
                    .delete()
                    .await()
                loadUsers() // Reload users
                loadAdminStats() // Reload stats
                _uiState.value = Resource.Success("User deleted successfully!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to delete user: ${e.message}")
            }
        }
    }

    /**
     * Approve asset
     */
    fun approveAsset(assetId: String, approved: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                firestore.collection("assets")
                    .document(assetId)
                    .update("approved", approved)
                    .await()
                loadAssets() // Reload assets
                loadAdminStats() // Reload stats
                _uiState.value = Resource.Success("Asset ${if (approved) "approved" else "rejected"}!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to update asset: ${e.message}")
            }
        }
    }

    /**
     * Delete asset
     */
    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = Resource.Loading()
                firestore.collection("assets")
                    .document(assetId)
                    .delete()
                    .await()
                loadAssets() // Reload assets
                loadAdminStats() // Reload stats
                _uiState.value = Resource.Success("Asset deleted successfully!")
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to delete asset: ${e.message}")
            }
        }
    }

    /**
     * Toggle asset featured status
     */
    fun toggleAssetFeatured(assetId: String, featured: Boolean) {
        viewModelScope.launch {
            try {
                firestore.collection("assets")
                    .document(assetId)
                    .update("featured", featured)
                    .await()
                loadAssets()
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to update asset: ${e.message}")
            }
        }
    }

    fun clearUiState() {
        _uiState.value = null
    }
}
