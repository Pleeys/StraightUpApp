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
        // Pobierz ustawione czasy przerwy nocnej z preferencji
        val nightStart = PreferenceHelper.getNightBreakStart(context)  // np. "22:00"
        val nightEnd = PreferenceHelper.getNightBreakEnd(context)      // np. "09:00"

        // Zamiana ustawionych czasów na minuty od północy
        val startParts = nightStart.split(":").map { it.toInt() }
        val endParts = nightEnd.split(":").map { it.toInt() }
        val startMinutes = startParts[0] * 60 + startParts[1]
        val endMinutes = endParts[0] * 60 + endParts[1]

        // Pobierz aktualny czas
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // Sprawdź, czy jesteśmy w przerwie nocnej
        val inNightBreak = if (startMinutes < endMinutes) {
            // Przypadek, gdy przerwa nocna nie przechodzi przez północ (np. 23:00 - 07:00)
            currentMinutes in startMinutes until endMinutes
        } else {
            // Przypadek, gdy przerwa nocna przechodzi przez północ (np. 22:00 - 09:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }

        if (inNightBreak) {
            // W przerwie nocnej nie wysyłamy powiadomień
            return
        }

        // Jeżeli nie jesteśmy w przerwie nocnej – wyświetl powiadomienie
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


