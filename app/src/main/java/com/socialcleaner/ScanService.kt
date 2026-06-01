package com.socialcleaner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.socialcleaner.model.AppScanResult
import com.socialcleaner.scanner.AppRegistry
import com.socialcleaner.scanner.MediaScanner
import kotlinx.coroutines.*

class ScanService : Service() {

    companion object {
        const val CHANNEL_ID = "scan_service_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_CANCEL = "com.socialcleaner.ACTION_CANCEL"
    }

    private val binder = ScanBinder()
    private val scanner = MediaScanner()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    // Callbacks
    var onProgress: ((String, Int, Int) -> Unit)? = null  // (appName, current, total)
    var onResult: ((List<AppScanResult>) -> Unit)? = null
    var onCancelled: (() -> Unit)? = null

    inner class ScanBinder : Binder() {
        fun getService(): ScanService = this@ScanService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            cancelScan()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    fun startScan(selectedYear: Int?) {
        if (scanJob?.isActive == true) return

        // Start foreground with initial notification
        startForeground(NOTIFICATION_ID, buildNotification(0, 0, ""))

        scanJob = scope.launch {
            val allResults = mutableListOf<AppScanResult>()
            val apps = AppRegistry.supportedApps

            for ((index, app) in apps.withIndex()) {
                // Check if cancelled
                if (!isActive) break

                // Update notification
                updateNotification(index + 1, apps.size, app.name)

                // Notify UI
                withContext(Dispatchers.Main) {
                    onProgress?.invoke(app.name, index + 1, apps.size)
                }

                val scanResults = scanner.scanApp(app, selectedYear)
                allResults.addAll(scanResults)
            }

            if (isActive) {
                val sorted = allResults.sortedWith(
                    compareByDescending<AppScanResult> { it.year }
                        .thenByDescending { it.totalSize }
                )
                withContext(Dispatchers.Main) {
                    onResult?.invoke(sorted)
                }
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scope.launch(Dispatchers.Main) {
            onCancelled?.invoke()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun isScanning(): Boolean = scanJob?.isActive == true

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.scan_channel_name)
            val desc = getString(R.string.scan_channel_desc)
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
                description = desc
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(current: Int, total: Int, appName: String): Notification {
        // Cancel action
        val cancelIntent = Intent(this, ScanService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPending = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap to open app
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (total > 0) {
            getString(R.string.scan_notif_title, current, total)
        } else {
            getString(R.string.scanning)
        }

        val text = if (appName.isNotEmpty()) {
            getString(R.string.scan_notif_text, appName)
        } else {
            getString(R.string.scan_notif_starting)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_small)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(total, current, total == 0)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_trash, getString(R.string.cancel), cancelPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(0xFF4ECDC4.toInt())
            .build()
    }

    private fun updateNotification(current: Int, total: Int, appName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(current, total, appName))
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
