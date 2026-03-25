package com.pixelmarket.app.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelmarket.app.data.service.AssetDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    private val downloadManager: AssetDownloadManager
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads

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
     * Monitor download progress
     */
    private fun monitorDownload(downloadId: Long) {
        viewModelScope.launch {
            while (true) {
                val (status, progress) = downloadManager.getDownloadStatus(downloadId)
                
                _downloads.value = _downloads.value.map { download ->
                    if (download.downloadId == downloadId) {
                        download.copy(
                            progress = progress,
                            isComplete = downloadManager.isDownloadComplete(downloadId),
                            isFailed = status == android.app.DownloadManager.STATUS_FAILED
                        )
                    } else {
                        download
                    }
                }
                
                // Stop monitoring if download is complete or failed
                if (downloadManager.isDownloadComplete(downloadId) || 
                    status == android.app.DownloadManager.STATUS_FAILED) {
                    break
                }
                
                kotlinx.coroutines.delay(1000) // Check every second
            }
        }
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
