package com.pixelmarket.app.presentation.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pixelmarket.app.domain.model.Asset
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.domain.repository.StorageRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

sealed class UploadUiState {
    object Idle : UploadUiState()
    object UploadingFiles : UploadUiState()
    object SavingAsset : UploadUiState()
    data class Success(val message: String) : UploadUiState()
    data class Error(val message: String) : UploadUiState()
}

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageRepository: StorageRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {

    private val _uploadState = MutableStateFlow<Resource<String>?>(null)
    val uploadState: StateFlow<Resource<String>?> = _uploadState

    private val _thumbnailUrl = MutableStateFlow<String?>(null)
    val thumbnailUrl: StateFlow<String?> = _thumbnailUrl

    private val _assetFileUrl = MutableStateFlow<String?>(null)
    val assetFileUrl: StateFlow<String?> = _assetFileUrl

    private val _assetFileType = MutableStateFlow<String>("")
    val assetFileType: StateFlow<String> = _assetFileType

    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uiState: StateFlow<UploadUiState> = _uiState

    private val _assetFileSize = MutableStateFlow<Long>(0L)
    val assetFileSize: StateFlow<Long> = _assetFileSize

    private val _assetFileSizeFormatted = MutableStateFlow<String>("")
    val assetFileSizeFormatted: StateFlow<String> = _assetFileSizeFormatted

    private val _thumbnailType = MutableStateFlow<String>("image")
    val thumbnailType: StateFlow<String> = _thumbnailType

    // Preview video (optional, 5-10s clip)
    private val _previewVideoUrl = MutableStateFlow<String?>(null)
    val previewVideoUrl: StateFlow<String?> = _previewVideoUrl

    /**
     * Upload thumbnail image or GIF
     */
    fun uploadThumbnail(fileUri: Uri) {
        viewModelScope.launch {
            // Detect if it's a GIF
            val mimeType = context.contentResolver.getType(fileUri)
            _thumbnailType.value = if (mimeType == "image/gif") "gif" else "image"
            
            _uiState.value = UploadUiState.UploadingFiles
            storageRepository.uploadThumbnail(fileUri).collect { result ->
                _uploadState.value = result
                when (result) {
                    is Resource.Success -> {
                        _thumbnailUrl.value = result.data
                        val type = if (_thumbnailType.value == "gif") "GIF" else "Image"
                        _uiState.value = UploadUiState.Success("Thumbnail $type uploaded successfully! ✅")
                    }
                    is Resource.Error -> {
                        _uiState.value = UploadUiState.Error("Failed to upload thumbnail: ${result.message}")
                    }
                    is Resource.Loading -> {
                        _uiState.value = UploadUiState.UploadingFiles
                    }
                }
            }
        }
    }

    /**
     * Upload asset file (ZIP, Blender, etc.)
     */
    fun uploadAssetFile(fileUri: Uri) {
        viewModelScope.launch {
            // Calculate file size
            val fileSize = getFileSize(fileUri)
            _assetFileSize.value = fileSize
            _assetFileSizeFormatted.value = formatFileSize(fileSize)
            
            // Detect file extension
            val mimeType = context.contentResolver.getType(fileUri)
            val extension = getExtensionFromUri(fileUri, mimeType)
            _assetFileType.value = extension
            
            _uiState.value = UploadUiState.UploadingFiles
            storageRepository.uploadAssetFile(fileUri).collect { result ->
                _uploadState.value = result
                when (result) {
                    is Resource.Success -> {
                        _assetFileUrl.value = result.data
                        _uiState.value = UploadUiState.Success("Asset file (${_assetFileSizeFormatted.value}) uploaded successfully! ✅")
                    }
                    is Resource.Error -> {
                        _uiState.value = UploadUiState.Error("Failed to upload asset: ${result.message}")
                    }
                    is Resource.Loading -> {
                        _uiState.value = UploadUiState.UploadingFiles
                    }
                }
            }
        }
    }

    /**
     * Upload a short preview video (5-10 seconds) for the asset
     */
    fun uploadPreviewVideo(fileUri: Uri) {
        viewModelScope.launch {
            _uiState.value = UploadUiState.UploadingFiles
            storageRepository.uploadPreviewVideo(fileUri).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _previewVideoUrl.value = result.data
                        _uiState.value = UploadUiState.Success("Preview video uploaded! ✅")
                    }
                    is Resource.Error -> {
                        _uiState.value = UploadUiState.Error("Video upload failed: ${result.message}")
                    }
                    is Resource.Loading -> {
                        _uiState.value = UploadUiState.UploadingFiles
                    }
                }
            }
        }
    }

    /**
     * Publish asset to marketplace
     */
    fun publishAsset(
        title: String,
        description: String,
        category: String,
        price: Double
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _uiState.value = UploadUiState.Error("You must be logged in to publish assets")
            return
        }

        viewModelScope.launch {
            _uiState.value = UploadUiState.SavingAsset

            try {
                // 1. Verify developer permission (Robust check)
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(currentUser.uid).get().await()
                
                if (!snapshot.exists()) {
                    _uiState.value = UploadUiState.Error("User record not found.")
                    return@launch
                }

                val role = snapshot.getString("role")?.lowercase() ?: "buyer"
                fun isEnabled(f: String) = try {
                    val v = snapshot.get(f)
                    v is Boolean && v || v?.toString()?.lowercase() == "true" || v?.toString() == "1"
                } catch (ex: Exception) { false }

                val hasDevAccess = isEnabled("isDeveloper") || isEnabled("isAdmin") || 
                                   isEnabled("developer") || isEnabled("admin") ||
                                   role == "seller" || role == "admin" || role == "developer"

                if (!hasDevAccess) {
                    _uiState.value = UploadUiState.Error("Your account does not have developer permissions. Please apply first.")
                    return@launch
                }

                // 2. Prepare Asset object
                val asset = Asset(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    category = category,
                    fileType = _assetFileType.value,
                    price = price,
                    thumbnailUrl = thumbnailUrl.value ?: "",
                    thumbnailType = _thumbnailType.value,
                    previewVideoUrl = _previewVideoUrl.value,
                    fileUrls = listOfNotNull(assetFileUrl.value),
                    fileSize = _assetFileSizeFormatted.value,
                    fileSizeBytes = _assetFileSize.value,
                    sellerId = currentUser.uid,
                    sellerName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Unknown",
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now(),
                    downloadCount = 0,
                    rating = 0.0,
                    featured = false,
                    approved = true
                )

                // 3. Save to repository
                assetRepository.createAsset(asset).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.value = UploadUiState.Success("🎉 Asset published successfully!")
                            resetForm()
                        }
                        is Resource.Error -> {
                            _uiState.value = UploadUiState.Error("Failed to publish: ${result.message}")
                        }
                        is Resource.Loading -> {
                            _uiState.value = UploadUiState.SavingAsset
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UploadUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = null
        _uiState.value = UploadUiState.Idle
    }

    private fun resetForm() {
        _thumbnailUrl.value = null
        _assetFileUrl.value = null
        _previewVideoUrl.value = null
        _uploadState.value = null
        _assetFileSize.value = 0L
        _assetFileSizeFormatted.value = ""
        _thumbnailType.value = "image"
    }

    /**
     * Get file size from URI
     */
    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            fileSize = cursor.getLong(sizeIndex)
        }
        return fileSize
    }

    /**
     * Format file size to human readable format
     */
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format(
            "%.2f %s",
            bytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }
    /**
     * Get file extension from URI
     */
    private fun getExtensionFromUri(uri: Uri, mimeType: String?): String {
        // Try to get extension from filename first
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        
        val extension = fileName?.substringAfterLast('.', "") ?: ""
        if (extension.isNotEmpty()) return extension
        
        // Fallback to MIME type
        return when (mimeType) {
            "application/zip" -> "zip"
            "application/x-zip-compressed" -> "zip"
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "audio/mpeg" -> "mp3"
            "audio/wav" -> "wav"
            else -> ""
        }
    }
}
