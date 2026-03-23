package com.example.straightup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val intervalMinutes = PreferenceHelper.getInterval(context)
        if (intervalMinutes <= 0) return

        AlarmReceiver.scheduleNext(context, intervalMinutes)
    }
}
