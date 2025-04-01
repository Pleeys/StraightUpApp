package com.example.straightup

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nightStart = PreferenceHelper.getNightBreakStart(context)
        val nightEnd = PreferenceHelper.getNightBreakEnd(context)

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        fun parseHourMinute(time: String): Pair<Int, Int> {
            val parts = time.split(":")
            return Pair(parts[0].toInt(), parts[1].toInt())
        }

        fun isInNightBreak(hour: Int, minute: Int): Boolean {
            val (startH, startM) = parseHourMinute(nightStart)
            val (endH, endM) = parseHourMinute(nightEnd)
            val current = hour * 60 + minute
            val start = startH * 60 + startM
            val end = endH * 60 + endM
            return if (start < end) {
                current in start until end
            } else {
                current >= start || current < end
            }
        }

        if (isInNightBreak(currentHour, currentMinute)) {
            return // 🔕 NIE pokazujemy powiadomienia
        }

        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setContentTitle("Time to take a break")
            .setContentText("Stay on track with your goals.")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        manager.notify(1001, notification)
    }
}

