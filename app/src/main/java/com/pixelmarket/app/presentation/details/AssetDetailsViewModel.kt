package com.pixelmarket.app.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pixelmarket.app.data.service.AssetDownloadManager
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.pixelmarket.app.domain.repository.WalletRepository

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Processing : PurchaseState()
    data class Success(val downloadUrl: String) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

@HiltViewModel
class AssetDetailsViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val walletRepository: WalletRepository,
    private val downloadManager: AssetDownloadManager
) : ViewModel() {

    private val _asset = MutableStateFlow<Resource<Asset>>(Resource.Loading())
    val asset: StateFlow<Resource<Asset>> = _asset

    private val _isPurchased = MutableStateFlow(false)
    val isPurchased: StateFlow<Boolean> = _isPurchased

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    // Like state
    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount

    private var currentAssetId: String = ""

    fun loadAssetDetails(assetId: String) {
        currentAssetId = assetId
        viewModelScope.launch {
            assetRepository.getAssetDetails(assetId).collect { result ->
                _asset.value = result

                // Check if already purchased if we have user and asset
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null && result is Resource.Success) {
                    _isPurchased.value = assetRepository.isAssetPurchased(userId, assetId)

                    // Update like count from real-time asset data
                    _likeCount.value = result.data?.likeCount ?: 0
                }
            }
        }

        // Check if user has liked this asset
        checkLikeStatus(assetId)
    }

    private fun checkLikeStatus(assetId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _isLiked.value = assetRepository.isAssetLiked(userId, assetId)
        }
    }

    fun toggleLike() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val assetId = currentAssetId.ifEmpty { return }

        viewModelScope.launch {
            if (_isLiked.value) {
                // Unlike
                val result = assetRepository.unlikeAsset(userId, assetId)
                if (result.isSuccess) {
                    _isLiked.value = false
                    _likeCount.value = (_likeCount.value - 1).coerceAtLeast(0)
                }
            } else {
                // Like
                val result = assetRepository.likeAsset(userId, assetId)
                if (result.isSuccess) {
                    _isLiked.value = true
                    _likeCount.value = _likeCount.value + 1
                }
            }
        }
    }

    suspend fun getWalletBalance(): Double {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return 0.0
        return walletRepository.getWalletBalance(uid)
    }

    fun buyWithWallet(asset: Asset) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _purchaseState.value = PurchaseState.Error("Please login to purchase")
            return
        }

        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Processing
            try {
                // 1. Deduct from wallet
                val result = walletRepository.deductMoney(
                    currentUser.uid,
                    asset.price,
                    "Bought asset: ${asset.title}"
                )

                if (result.isFailure) {
                    throw Exception(result.exceptionOrNull()?.message ?: "Insufficient wallet balance")
                }

                // 2. Complete purchase logic
                completePurchase(currentUser.uid, asset)

            } catch (e: Exception) {
                _purchaseState.value = PurchaseState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    private suspend fun completePurchase(userId: String, asset: Asset) {
        // Record the purchase in Firestore
        var purchaseSuccess = false
        assetRepository.purchaseAsset(userId, asset).collect { result ->
            if (result is Resource.Success) purchaseSuccess = true
            if (result is Resource.Error) throw Exception(result.message)
        }

        if (!purchaseSuccess) throw Exception("Purchase recording failed")

        // Record the download
        var downloadRecorded = false
        assetRepository.recordDownload(userId, asset).collect { result ->
            if (result is Resource.Success) downloadRecorded = true
            if (result is Resource.Error) throw Exception(result.message)
        }

        if (!downloadRecorded) throw Exception("Download recording failed")

        // Update purchase state locally
        _isPurchased.value = true

        // Start downloading
        startAssetsDownload(asset)

        val downloadUrl = asset.fileUrls.firstOrNull() ?: throw Exception("No download link available")
        _purchaseState.value = PurchaseState.Success(downloadUrl)
    }

    private fun startAssetsDownload(asset: Asset) {
        asset.fileUrls.forEachIndexed { index, fileUrl ->
            val extension = if (asset.fileType.isNotEmpty()) {
                if (asset.fileType.startsWith(".")) asset.fileType.lowercase()
                else ".${asset.fileType.lowercase()}"
            } else ""

            val fileName = "${asset.title.replace(" ", "_")}_file_${index + 1}$extension"
            downloadManager.downloadAsset(
                fileUrl = fileUrl,
                fileName = fileName,
                assetTitle = asset.title
            )
        }
    }

    fun buyNow(asset: Asset) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _purchaseState.value = PurchaseState.Error("Please login to purchase")
            return
        }

        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Processing
            try {
                // For external payments (Razorpay), we skip wallet deduction since it's already paid externally
                completePurchase(currentUser.uid, asset)
            } catch (e: Exception) {
                _purchaseState.value = PurchaseState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun downloadAssetDirectly(asset: Asset) {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Processing
            try {
                // Just start the download since it's already purchased
                startAssetsDownload(asset)

                // Record the download event anyway
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                assetRepository.recordDownload(userId, asset).collect {}

                val downloadUrl = asset.fileUrls.firstOrNull() ?: throw Exception("No download link available")
                _purchaseState.value = PurchaseState.Success(downloadUrl)
            } catch (e: Exception) {
                _purchaseState.value = PurchaseState.Error(e.message ?: "Download failed")
            }
        }
    }
}
