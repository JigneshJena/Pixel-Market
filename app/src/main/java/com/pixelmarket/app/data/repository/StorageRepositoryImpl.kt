package com.pixelmarket.app.data.repository

import android.net.Uri
import com.pixelmarket.app.data.remote.CloudinaryManager
import com.pixelmarket.app.domain.repository.StorageRepository
import com.pixelmarket.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val cloudinaryManager: CloudinaryManager
) : StorageRepository {

    override suspend fun uploadThumbnail(fileUri: Uri): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val url = cloudinaryManager.uploadThumbnail(fileUri)
            emit(Resource.Success(url))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to upload thumbnail"))
        }
    }

    override suspend fun uploadAssetFile(fileUri: Uri): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val url = cloudinaryManager.uploadAssetFile(fileUri)
            emit(Resource.Success(url))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to upload asset file"))
        }
    }

    override suspend fun uploadPreviewVideo(fileUri: Uri): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val url = cloudinaryManager.uploadPreviewVideo(fileUri)
            emit(Resource.Success(url))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to upload preview video"))
        }
    }

    override suspend fun deleteFile(fileUrl: String): Flow<Resource<Unit>> = flow {
        // Cloudinary file deletion (optional - files can stay in cloud)
        // You can implement deletion via Cloudinary API if needed
        emit(Resource.Success(Unit))
    }
}
