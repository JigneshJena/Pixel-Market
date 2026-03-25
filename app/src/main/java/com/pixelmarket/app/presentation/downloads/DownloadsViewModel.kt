package com.pixelmarket.app.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelmarket.app.data.service.AssetDownloadManager
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.model.Download
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.pixelmarket.app.domain.repository.CartRepository
import com.pixelmarket.app.domain.model.CartItem
import com.pixelmarket.app.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class DownloadItem(
    val downloadId: Long,
    val assetTitle: String,
    val fileName: String,
    val progress: Int = 0,
    val isComplete: Boolean = false,
    val isFailed: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: AssetDownloadManager,
    private val assetRepository: AssetRepository,
    private val cartRepository: CartRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads

    private val _purchasedAssets = MutableStateFlow<Resource<List<Asset>>>(Resource.Loading())
    val purchasedAssets: StateFlow<Resource<List<Asset>>> = _purchasedAssets

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    private val _downloadHistory = MutableStateFlow<Resource<List<Download>>>(Resource.Loading())
    val downloadHistory: StateFlow<Resource<List<Download>>> = _downloadHistory

    private val _checkoutStatus = MutableStateFlow<Resource<Unit>?>(null)
    val checkoutStatus: StateFlow<Resource<Unit>?> = _checkoutStatus

    init {
        loadPurchasedAssets()
        loadDownloadHistory()
        loadCartItems()
        startDownloadMonitoring()
    }

    fun loadCartItems() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
            ?: return
        
        viewModelScope.launch {
            cartRepository.getCartItems(userId).collect { items ->
                _cartItems.value = items
            }
        }
    }

    fun removeFromCart(assetId: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
            ?: return
        viewModelScope.launch {
            cartRepository.removeFromCart(userId, assetId)
        }
    }

    fun checkoutAll() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
            ?: return
        val items = _cartItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            _checkoutStatus.value = Resource.Loading()
            
            val totalAmount = items.sumOf { it.price }
            val balance = walletRepository.getWalletBalance(userId)

            if (balance < totalAmount) {
                _checkoutStatus.value = Resource.Error("Insufficient balance. Please top up your wallet.")
                return@launch
            }

            try {
                // Deduct total amount
                walletRepository.deductMoney(userId, totalAmount, "Purchase of ${items.size} assets")

                // Purchase each asset
                for (item in items) {
                    // Fetch full asset details first to ensure we have file URLs
                    assetRepository.getAssetDetails(item.assetId).first { resource ->
                        if (resource is Resource.Success && resource.data != null) {
                            assetRepository.purchaseAsset(userId, resource.data).collect { }
                            true
                        } else resource is Resource.Error
                    }
                }

                // Clear cart
                cartRepository.clearCart(userId)
                
                _checkoutStatus.value = Resource.Success(Unit)
                loadPurchasedAssets()
            } catch (e: Exception) {
                _checkoutStatus.value = Resource.Error(e.localizedMessage ?: "Checkout failed")
            }
        }
    }

    /**
     * Clear checkout status after it's been handled by the UI
     */
    fun resetCheckoutStatus() {
        _checkoutStatus.value = null
    }

    fun loadPurchasedAssets() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
            ?: return
        
        viewModelScope.launch {
            assetRepository.getPurchasedAssets(userId).collect { result ->
                _purchasedAssets.value = result
            }
        }
    }

    fun loadDownloadHistory() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
            ?: return
        
        viewModelScope.launch {
            assetRepository.getUserDownloads(userId).collect { result ->
                _downloadHistory.value = result
            }
        }
    }

    /**
     * Start downloading an asset
     */
    fun startDownload(fileUrl: String, fileName: String, assetTitle: String) {
        viewModelScope.launch {
            val downloadId = downloadManager.downloadAsset(fileUrl, fileName, assetTitle)
            
            val newDownload = DownloadItem(
                downloadId = downloadId,
                assetTitle = assetTitle,
                fileName = fileName
            )
            
            _downloads.value = _downloads.value + newDownload
            
            // Start monitoring download progress
            monitorDownload(downloadId)
        }
    }

    /**
     * Start continuous monitoring of all downloads
     */
    private fun startDownloadMonitoring() {
        viewModelScope.launch {
            while (true) {
                val systemDownloads = downloadManager.getAllDownloads()
                
                val updatedItems = systemDownloads.map { (id, status, progress) ->
                    val existing = _downloads.value.find { it.downloadId == id }
                    
                    if (existing != null) {
                        existing.copy(
                            progress = progress,
                            isComplete = status == android.app.DownloadManager.STATUS_SUCCESSFUL,
                            isFailed = status == android.app.DownloadManager.STATUS_FAILED
                        )
                    } else {
                        // Discover new download
                        val (_, _, title) = downloadManager.getDownloadStatus(id)
                        DownloadItem(
                            downloadId = id,
                            assetTitle = title ?: "Unknown Asset",
                            fileName = "In Progress...",
                            progress = progress,
                            isComplete = status == android.app.DownloadManager.STATUS_SUCCESSFUL,
                            isFailed = status == android.app.DownloadManager.STATUS_FAILED
                        )
                    }
                }.filter { !it.isComplete } // Only show active downloads in this list
                
                _downloads.value = updatedItems
                
                // If any downloads completed, refresh history
                if (systemDownloads.any { it.second == android.app.DownloadManager.STATUS_SUCCESSFUL }) {
                    loadDownloadHistory()
                }

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    /**
     * Monitor download progress
     */
    private fun monitorDownload(downloadId: Long) {
        // No longer needed as startDownloadMonitoring handles all downloads
    }

    /**
     * Cancel a download
     */
    fun cancelDownload(downloadId: Long) {
        downloadManager.cancelDownload(downloadId)
        _downloads.value = _downloads.value.filter { it.downloadId != downloadId }
    }

    /**
     * Retry a failed download
     */
    fun retryDownload(download: DownloadItem) {
        // Remove old download
        _downloads.value = _downloads.value.filter { it.downloadId != download.downloadId }
        
        // TODO: Get file URL from Firestore and restart download
        // startDownload(fileUrl, download.fileName, download.assetTitle)
    }
}

