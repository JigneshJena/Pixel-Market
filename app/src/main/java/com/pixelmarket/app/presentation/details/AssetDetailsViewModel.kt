package com.pixelmarket.app.presentation.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pixelmarket.app.data.service.AssetDownloadManager
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.domain.repository.CartRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
    private val downloadManager: AssetDownloadManager,
    private val cartRepository: CartRepository
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

    private val _hasUserRated = MutableStateFlow(false)
    val hasUserRated: StateFlow<Boolean> = _hasUserRated

    // One-shot error events for like/rating failures
    private val _errorEvent = Channel<String>(Channel.BUFFERED)
    val errorEvent: Flow<String> = _errorEvent.receiveAsFlow()

    // One-shot success events for rating
    private val _ratingSuccessEvent = Channel<Unit>(Channel.BUFFERED)
    val ratingSuccessEvent: Flow<Unit> = _ratingSuccessEvent.receiveAsFlow()

    private val _deleteEvent = Channel<String>(Channel.BUFFERED)
    val deleteEvent: Flow<String> = _deleteEvent.receiveAsFlow()

    // Cart state
    private val _isInCart = MutableStateFlow(false)
    val isInCart: StateFlow<Boolean> = _isInCart

    // Cart events: "added" | "removed" | "already_in_cart" | "error" | "login_required"
    private val _cartEvent = Channel<String>(Channel.BUFFERED)
    val cartEvent: Flow<String> = _cartEvent.receiveAsFlow()

    // True when the logged-in user is the uploader/owner of this asset
    private val _isOwnAsset = MutableStateFlow(false)
    val isOwnAsset: StateFlow<Boolean> = _isOwnAsset

    // Real-time seller profile
    private val _sellerProfile = MutableStateFlow<com.pixelmarket.app.domain.model.User?>(null)
    val sellerProfile: StateFlow<com.pixelmarket.app.domain.model.User?> = _sellerProfile

    private var currentAssetId: String = ""
    // Guard flags so we don't re-launch listeners on every Firestore update
    private var likeCountInitialized = false
    private var purchaseAndRatingChecked = false

    fun loadAssetDetails(assetId: String) {
        currentAssetId = assetId
        likeCountInitialized = false
        purchaseAndRatingChecked = false

        // ── 1. Firestore asset stream ──────────────────────────────────────────
        viewModelScope.launch {
            assetRepository.getAssetDetails(assetId).collect { result ->
                _asset.value = result

                if (result is Resource.Success) {
                    result.data?.let { asset ->

                        // Check if current user is the owner/uploader
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        _isOwnAsset.value = userId != null && userId == asset.sellerId

                        // Set likeCount ONCE from Firestore on first load.
                        // After that, RTDB listener (below) owns the count.
                        if (!likeCountInitialized) {
                            _likeCount.value = asset.likeCount
                            likeCountInitialized = true
                        }

                        // Check purchase & rating status ONCE — not on every Firestore ping
                        if (!purchaseAndRatingChecked) {
                            purchaseAndRatingChecked = true
                            if (userId != null) {
                                _isPurchased.value = assetRepository.isAssetPurchased(userId, assetId)
                                checkUserRated(userId, assetId)
                            }
                        }

                        // Real-time fetch seller profile if missing or always to keep it fresh
                        fetchSellerProfile(asset.sellerId)
                    }
                }
            }
        }

        // ── 2. RTDB real-time like count ───────────────────────────────────────
        // Only overrides Firestore value once RTDB has data (first like ever sets it)
        viewModelScope.launch {
            assetRepository.getAssetLikeCount(assetId).collect { rtdbCount ->
                _likeCount.value = rtdbCount
                likeCountInitialized = true
            }
        }

        // ── 3. RTDB real-time like status ──────────────────────────────────────
        checkLikeStatus(assetId)

        // ── 4. Cart status ─────────────────────────────────────────────────────
        checkCartStatus(assetId)
    }

    private fun fetchSellerProfile(sellerId: String) {
        if (sellerId.isBlank()) return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(sellerId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val user = snapshot.toObject(com.pixelmarket.app.domain.model.User::class.java)
                _sellerProfile.value = user
            }
    }

    private fun checkCartStatus(assetId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _isInCart.value = cartRepository.isInCart(userId, assetId)
        }
    }

    fun toggleCart() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val authUid = currentUser?.uid
        
        if (authUid == null) {
            _cartEvent.trySend("login_required")
            return
        }
        
        val asset = _asset.value.data ?: return
        
        if (_isPurchased.value) {
            _cartEvent.trySend("already_purchased")
            return
        }

        viewModelScope.launch {
            if (_isInCart.value) {
                // Remove from cart
                val result = cartRepository.removeFromCart(authUid, asset.id)
                if (result.isSuccess) {
                    _isInCart.value = false
                    _cartEvent.trySend("removed")
                } else {
                    _cartEvent.trySend("error:${result.exceptionOrNull()?.message}")
                }
            } else {
                // Add to cart
                val result = cartRepository.addToCart(authUid, asset)
                if (result.isSuccess) {
                    _isInCart.value = true
                    _cartEvent.trySend("added")
                } else {
                    _cartEvent.trySend("error:${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    private fun checkUserRated(userId: String, assetId: String) {
        viewModelScope.launch {
            assetRepository.hasUserRated(userId, assetId).collect { rated ->
                _hasUserRated.value = rated
            }
        }
    }

    fun submitRating(rating: Float) {
        val assetId = currentAssetId.ifEmpty { return }
        viewModelScope.launch {
            val result = assetRepository.rateAsset(assetId, rating)
            if (result.isSuccess) {
                _hasUserRated.value = true
                _ratingSuccessEvent.trySend(Unit)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "Rating failed"
                Log.e("AssetDetailsVM", "submitRating failed: $errMsg")
                _errorEvent.trySend("Could not submit rating. Please try again.")
            }
        }
    }

    private fun checkLikeStatus(assetId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            assetRepository.isAssetLiked(userId, assetId).collect { liked ->
                _isLiked.value = liked
            }
        }
    }

    fun toggleLike() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val assetId = currentAssetId.ifEmpty { return }

        // Optimistic update — flip immediately for instant feedback
        val wasLiked = _isLiked.value
        _isLiked.value = !wasLiked
        _likeCount.value = if (wasLiked) maxOf(0, _likeCount.value - 1) else _likeCount.value + 1

        viewModelScope.launch {
            val result = if (wasLiked) {
                assetRepository.unlikeAsset(userId, assetId)
            } else {
                assetRepository.likeAsset(userId, assetId)
            }
            if (result.isFailure) {
                // Rollback the optimistic update
                _isLiked.value = wasLiked
                _likeCount.value = if (wasLiked) _likeCount.value + 1 else maxOf(0, _likeCount.value - 1)
                val errMsg = result.exceptionOrNull()?.message ?: "Action failed"
                Log.e("AssetDetailsVM", "toggleLike failed: $errMsg")
                _errorEvent.trySend("Could not update like. Please try again.")
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

    fun deleteAsset() {
        val assetId = currentAssetId.ifEmpty { return }
        viewModelScope.launch {
            assetRepository.deleteAsset(assetId).collect { result ->
                if (result is Resource.Success) {
                    _deleteEvent.trySend("success")
                } else if (result is Resource.Error) {
                    _deleteEvent.trySend("error:${result.message}")
                }
            }
        }
    }
}
