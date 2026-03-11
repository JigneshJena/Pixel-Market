package com.pixelmarket.app.domain.repository

import android.net.Uri
import com.pixelmarket.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    suspend fun uploadThumbnail(fileUri: Uri): Flow<Resource<String>>
    suspend fun uploadAssetFile(fileUri: Uri): Flow<Resource<String>>
    suspend fun uploadPreviewVideo(fileUri: Uri): Flow<Resource<String>>
    suspend fun deleteFile(fileUrl: String): Flow<Resource<Unit>>
}
