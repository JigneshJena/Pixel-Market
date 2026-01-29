package com.pixelmarket.app.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Processing : PurchaseState()
    data class Success(val downloadUrl: String) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

@HiltViewModel
class AssetDetailsViewModel @Inject constructor(
    private val assetRepository: AssetRepository
) : ViewModel() {

    private val _asset = MutableStateFlow<Resource<Asset>>(Resource.Loading())
    val asset: StateFlow<Resource<Asset>> = _asset

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    fun loadAssetDetails(assetId: String) {
        viewModelScope.launch {
            assetRepository.getAssetDetails(assetId).collect { result ->
                _asset.value = result
            }
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
                // Simulate payment processing
                kotlinx.coroutines.delay(2000)
                
                // In real app: Process payment, create order, etc.
                // For now, just provide download link
                val downloadUrl = asset.fileUrls.firstOrNull() 
                    ?: throw Exception("No download link available")
                
                _purchaseState.value = PurchaseState.Success(downloadUrl)
            } catch (e: Exception) {
                _purchaseState.value = PurchaseState.Error(
                    e.message ?: "Purchase failed"
                )
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}
