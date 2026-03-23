package com.example.straightup

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_CONFIRM = "com.example.straightup.ACTION_POSTURE_CONFIRM"
        private const val TAG = "AlarmReceiver"

        private val POSTURE_MESSAGES = listOf(
            "Wyprostuj plecy!",
            "Sprawdź swoją postawę!",
            "Czas wstać na chwilę!",
            "Twój kręgosłup ci dziękuje — wyprostuj się!",
            "Przerwa od siedzenia — już czas!",
            "Barki do tyłu, głowa prosto!",
            "Pamiętaj o postawie — Twój kręgosłup to doceni!"
        )

        fun scheduleNext(context: Context, intervalMinutes: Int) {
            if (intervalMinutes <= 0) return
            val triggerTime = System.currentTimeMillis() + intervalMinutes.toLong() * 60_000L
            scheduleAt(context, triggerTime)
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildPendingIntent(context))
            PreferenceHelper.saveLastAlarmTime(context, -1L)
        }

        private fun scheduleAt(context: Context, triggerTime: Long) {
            PreferenceHelper.saveLastAlarmTime(context, triggerTime)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pending = buildPendingIntent(context)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pending
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pending)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Exact alarm requires SCHEDULE_EXACT_ALARM permission, using inexact fallback", e)
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pending)
            } catch (e: Exception) {
                Log.e(TAG, "Exact alarm scheduling failed unexpectedly, using inexact fallback", e)
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pending)
            }
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
        val interval = PreferenceHelper.getInterval(context)

        // Jeśli trwa cisza nocna — zaplanuj jeden alarm na jej koniec (bez wakeupów co N minut)
        val nightBreakEnd = nightBreakEndTime(context)
        if (nightBreakEnd != null) {
            scheduleAt(context, nightBreakEnd)
            return
        }

        scheduleNext(context, interval)
        PreferenceHelper.incrementTotalNotifications(context)
        sendNotification(context)
    }

    /**
     * Jeśli teraz jest cisza nocna, zwraca timestamp końca tej przerwy.
     * W przeciwnym razie zwraca null.
     */
    private fun nightBreakEndTime(context: Context): Long? {
        return try {
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

            if (!inNightBreak) return null

            // Oblicz timestamp końca przerwy
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, endParts[0])
            cal.set(Calendar.MINUTE, endParts[1])
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            // Jeśli koniec przerwy jest wcześniej niż teraz (przekroczono północ) → jutro
            if (cal.timeInMillis < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.timeInMillis
        } catch (e: Exception) {
            null // przy błędnych danych nie blokuj
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
