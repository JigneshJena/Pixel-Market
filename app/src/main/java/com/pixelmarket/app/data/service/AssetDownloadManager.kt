package com.pixelmarket.app.data.service

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * Download an asset file from URL
     * @param fileUrl URL of the file to download
     * @param fileName Name to save the file as
     * @param assetTitle Title of the asset (for notification)
     * @return Download ID for tracking
     */
    fun downloadAsset(
        fileUrl: String,
        fileName: String,
        assetTitle: String
    ): Long {
        if (fileUrl.isBlank()) return -1L
        
        return try {
            val request = DownloadManager.Request(Uri.parse(fileUrl))
                .setTitle("Downloading: $assetTitle")
                .setDescription("PixelMarket Asset Download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)
        } catch (e: Exception) {
            android.util.Log.e("AssetDownloadManager", "Failed to enqueue download", e)
            -1L
        }
    }

    /**
     * Get download status
     * @return Pair of status and progress percentage
     */
    fun getDownloadStatus(downloadId: Long): Triple<Int, Int, String?> {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)

            val status = cursor.getInt(statusIndex)
            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
            val bytesTotal = cursor.getLong(bytesTotalIndex)
            val title = cursor.getString(titleIndex)

            val progress = if (bytesTotal > 0) {
                ((bytesDownloaded * 100L) / bytesTotal).toInt()
            } else {
                0
            }

            cursor.close()
            return Triple(status, progress, title)
        }

        cursor?.close()
        return Triple(DownloadManager.STATUS_FAILED, 0, null)
    }

    /**
     * Get all active and recent downloads
     */
    fun getAllDownloads(): List<Triple<Long, Int, Int>> {
        val query = DownloadManager.Query()
        val cursor = downloadManager.query(query)
        val list = mutableListOf<Triple<Long, Int, Int>>()
        
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            
            do {
                val id = cursor.getLong(idIndex)
                val status = cursor.getInt(statusIndex)
                val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                val bytesTotal = cursor.getLong(bytesTotalIndex)
                
                val progress = if (bytesTotal > 0) {
                    ((bytesDownloaded * 100L) / bytesTotal).toInt()
                } else {
                    0
                }
                
                list.add(Triple(id, status, progress))
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return list
    }

    /**
     * Check if download is complete
     */
    fun isDownloadComplete(downloadId: Long): Boolean {
        val (status, _, _) = getDownloadStatus(downloadId)
        return status == DownloadManager.STATUS_SUCCESSFUL
    }

    /**
     * Cancel a download
     */
    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }
}
