package com.socialcleaner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.DecimalFormat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "social_cleaner_channel"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_name)
            val descriptionText = context.getString(R.string.notif_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCleanupNotification(freedSize: Long) {
        val prefs = context.getSharedPreferences("social_cleaner", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", true)) return

        val sizeStr = formatSize(freedSize)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanIntent = Intent(context, CleanupReceiver::class.java)
        val cleanPendingIntent = PendingIntent.getBroadcast(
            context, 1, cleanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_small)
            .setContentTitle(context.getString(R.string.notif_title, sizeStr))
            .setContentText(context.getString(R.string.notif_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notif_small, context.getString(R.string.notif_action), cleanPendingIntent)
            .setColor(0xFF4ECDC4.toInt())

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    private fun formatSize(bytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytes < 1024 -> "$bytes o"
            bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} Ko"
            bytes < 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024))} Mo"
            else -> "${df.format(bytes / (1024.0 * 1024 * 1024))} Go"
        }
    }
}
