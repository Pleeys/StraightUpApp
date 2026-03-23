package com.example.straightup

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

        /**
         * Planuje następny alarm. Używa setExactAndAllowWhileIdle — działa w Doze mode
         * bez konieczności uprawnienia SCHEDULE_EXACT_ALARM.
         */
        fun scheduleNext(context: Context, intervalMinutes: Int) {
            if (intervalMinutes <= 0) return
            val intervalMillis = intervalMinutes * 60 * 1000L
            val triggerTime = System.currentTimeMillis() + intervalMillis
            PreferenceHelper.saveLastAlarmTime(context, triggerTime)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pending = buildPendingIntent(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pending
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pending)
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildPendingIntent(context))
            PreferenceHelper.saveLastAlarmTime(context, -1L)
        }

        private fun buildPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java)
            return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Sprawdź ciszę nocną z obsługą błędnych danych
        if (isInNightBreak(context)) return

        // Zaplanuj kolejny alarm (setExactAndAllowWhileIdle nie powtarza się automatycznie)
        val interval = PreferenceHelper.getInterval(context)
        scheduleNext(context, interval)

        PreferenceHelper.incrementTotalNotifications(context)

        sendNotification(context)
    }

    private fun isInNightBreak(context: Context): Boolean {
        return try {
            val nightStart = PreferenceHelper.getNightBreakStart(context)
            val nightEnd = PreferenceHelper.getNightBreakEnd(context)

            val startParts = nightStart.split(":").map { it.toInt() }
            val endParts = nightEnd.split(":").map { it.toInt() }
            val startMinutes = startParts[0] * 60 + startParts[1]
            val endMinutes = endParts[0] * 60 + endParts[1]

            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            if (startMinutes > endMinutes) {
                currentMinutes >= startMinutes || currentMinutes < endMinutes
            } else {
                currentMinutes in startMinutes until endMinutes
            }
        } catch (e: Exception) {
            false // przy błędnych danych nie blokuj powiadomienia
        }
    }

    private fun sendNotification(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val confirmPending = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, PostureConfirmReceiver::class.java).apply {
                action = ACTION_CONFIRM
                putExtra("notification_id", NOTIFICATION_ID)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setContentTitle("StraightUp — czas na postawę!")
            .setContentText(POSTURE_MESSAGES.random())
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_alarm, "Wyprostowałem! ✓", confirmPending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
