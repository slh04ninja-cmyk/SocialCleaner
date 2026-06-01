package com.socialcleaner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class CleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.notif_cleanup_started), Toast.LENGTH_SHORT).show()
        // Launch main activity to perform cleanup
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("auto_cleanup", true)
        }
        context.startActivity(launchIntent)
    }
}
