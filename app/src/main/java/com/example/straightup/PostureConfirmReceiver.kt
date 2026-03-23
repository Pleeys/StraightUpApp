package com.example.straightup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class PostureConfirmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PreferenceHelper.saveConfirmation(context)

        val notificationId = intent.getIntExtra("notification_id", AlarmReceiver.NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(notificationId)

        Toast.makeText(context, "Brawo! Zadbałeś o plecy 💪", Toast.LENGTH_SHORT).show()
    }
}
