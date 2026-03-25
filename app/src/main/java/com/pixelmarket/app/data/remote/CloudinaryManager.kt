package com.pixelmarket.app.data.remote

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryManager {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val config = HashMap<String, Any>()
            config["cloud_name"] = "dgywpx1xw"
            config["api_key"] = "346514736652462"
            config["api_secret"] = "9nUzc5etqfcGq7bPmho_6dCn-1o"
            config["secure"] = true
            
            MediaManager.init(context, config)
            isInitialized = true
        }
    }

    /**
     * Upload an image to Cloudinary
     * @param fileUri Local file URI
     * @param folder Folder name in Cloudinary (e.g., "thumbnails" or "assets")
     * @return Public URL of the uploaded file
     */
    suspend fun uploadFile(fileUri: Uri, folder: String = "assets"): String {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get()
                .upload(fileUri)
                .option("folder", folder)
                .option("resource_type", "auto") // Automatically detect file type
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Progress update (optional)
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            continuation.resume(secureUrl)
                        } else {
                            continuation.resumeWithException(
                                Exception("Upload succeeded but no URL returned")
                            )
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(
                            Exception("Upload failed: ${error.description}")
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Upload rescheduled
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                // Cancel upload if coroutine is cancelled
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }

    /**
     * Upload thumbnail image
     */
    suspend fun uploadThumbnail(fileUri: Uri): String {
        return uploadFile(fileUri, "thumbnails")
    }

    /**
     * Upload asset file (ZIP, Blender, etc.)
     */
    suspend fun uploadAssetFile(fileUri: Uri): String {
        return uploadFile(fileUri, "assets")
    }

    /**
     * Upload a short preview video clip (5-10s) — saved in a dedicated folder
     */
    suspend fun uploadPreviewVideo(fileUri: Uri): String {
        return uploadFile(fileUri, "preview_videos")
    }
}
