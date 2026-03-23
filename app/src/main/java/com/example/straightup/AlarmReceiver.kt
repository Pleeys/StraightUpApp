package com.example.straightup

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_CONFIRM = "com.example.straightup.ACTION_POSTURE_CONFIRM"

        private val POSTURE_MESSAGES = listOf(
            "Wyprostuj plecy!",
            "Sprawdź swoją postawę!",
            "Czas wstać na chwilę!",
            "Twój kręgosłup ci dziękuje — wyprostuj się!",
            "Przerwa od siedzenia — już czas!",
            "Barki do tyłu, głowa prosto!",
            "Pamiętaj o postawie — Twój kręgosłup to doceni!"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val nightStart = PreferenceHelper.getNightBreakStart(context)
        val nightEnd = PreferenceHelper.getNightBreakEnd(context)

        val startParts = nightStart.split(":").map { it.toInt() }
        val endParts = nightEnd.split(":").map { it.toInt() }
        val startMinutes = startParts[0] * 60 + startParts[1]
        val endMinutes = endParts[0] * 60 + endParts[1]

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val inNightBreak = if (startMinutes > endMinutes) {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes until endMinutes
        }

        if (inNightBreak) return

        // Zaktualizuj czas następnego alarmu w prefs (dla wyświetlacza w UI)
        val intervalMillis = PreferenceHelper.getInterval(context) * 60 * 1000L
        if (intervalMillis > 0) {
            PreferenceHelper.saveLastAlarmTime(context, System.currentTimeMillis() + intervalMillis)
        }

        PreferenceHelper.incrementTotalNotifications(context)

        val message = POSTURE_MESSAGES.random()

        val confirmIntent = Intent(context, PostureConfirmReceiver::class.java).apply {
            action = ACTION_CONFIRM
            putExtra("notification_id", NOTIFICATION_ID)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context,
            0,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setContentTitle("StraightUp — czas na postaw\u0119!")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_alarm,
                "Wyprostowa\u0142em! \u2713",
                confirmPending
            )
            .setAutoCancel(true)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        manager.notify(NOTIFICATION_ID, notification)
    }
}
