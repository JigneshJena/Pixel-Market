package com.pixelmarket.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pixelmarket.app.R
import com.pixelmarket.app.presentation.ui.MainActivity

class PixelMarketMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "pixelmarket_notifications"
        private const val CHANNEL_NAME = "PixelMarket Notifications"
        private const val NOTIFICATION_ID = 1
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to your server to enable push notifications
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Handle notification payload
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "PixelMarket",
                body = notification.body ?: ""
            )
        }

        // Handle data payload
        message.data.isNotEmpty().let {
            handleDataPayload(message.data)
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new assets, downloads, and updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun handleDataPayload(data: Map<String, String>) {
        // Handle custom data payload
        val type = data["type"]
        when (type) {
            "new_asset" -> {
                // Show notification about new asset
            }
            "download_complete" -> {
                // Show download complete notification
            }
            "purchase_confirmed" -> {
                // Show purchase confirmation
            }
        }
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Send FCM token to your backend server
        // This allows you to send targeted push notifications to this device
        android.util.Log.d("FCM Token", "New token: $token")
    }
}
