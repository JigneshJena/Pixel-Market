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
        val request = DownloadManager.Request(Uri.parse(fileUrl))
            .setTitle("Downloading: $assetTitle")
            .setDescription("PixelMarket Asset Download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "PixelMarket/$fileName"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return downloadManager.enqueue(request)
    }

    /**
     * Get download status
     * @return Pair of status and progress percentage
     */
    fun getDownloadStatus(downloadId: Long): Pair<Int, Int> {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

            val status = cursor.getInt(statusIndex)
            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
            val bytesTotal = cursor.getLong(bytesTotalIndex)

            val progress = if (bytesTotal > 0) {
                ((bytesDownloaded * 100L) / bytesTotal).toInt()
            } else {
                0
            }

            cursor.close()
            return Pair(status, progress)
        }

        cursor.close()
        return Pair(DownloadManager.STATUS_FAILED, 0)
    }

    /**
     * Check if download is complete
     */
    fun isDownloadComplete(downloadId: Long): Boolean {
        val (status, _) = getDownloadStatus(downloadId)
        return status == DownloadManager.STATUS_SUCCESSFUL
    }

    /**
     * Cancel a download
     */
    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }
}
