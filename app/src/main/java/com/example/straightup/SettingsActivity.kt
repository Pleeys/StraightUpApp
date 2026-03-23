package com.example.straightup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<android.view.View>(R.id.backButton).setOnClickListener { finish() }

        refreshValues()

        findViewById<android.view.View>(R.id.rowInterval).setOnClickListener {
            showIntervalDialog()
        }
        findViewById<android.view.View>(R.id.rowNightBreak).setOnClickListener {
            showNightBreakDialog()
        }
        findViewById<android.view.View>(R.id.rowProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshValues()
    }

    private fun refreshValues() {
        val interval = PreferenceHelper.getInterval(this)
        findViewById<TextView>(R.id.intervalValue).text =
            if (interval == 0) "Nie ustawiono" else "Co $interval minut"

        // Przełóż alarm jeśli czas minął
        if (interval > 0) {
            val lastAlarmTime = PreferenceHelper.getLastAlarmTime(this)
            if (lastAlarmTime == -1L || lastAlarmTime < System.currentTimeMillis()) {
                scheduleAlarm(interval)
            }
        }

        val start = PreferenceHelper.getNightBreakStart(this)
        val end = PreferenceHelper.getNightBreakEnd(this)
        findViewById<TextView>(R.id.nightBreakValue).text = "$start – $end"

        findViewById<TextView>(R.id.profileNameValue).text = PreferenceHelper.getNick(this)
    }

    private fun showIntervalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.interval_picker_dialog, null)
        val picker = dialogView.findViewById<NumberPicker>(R.id.intervalPicker)
        val saveButton = dialogView.findViewById<Button>(R.id.saveIntervalButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelIntervalButton)

        picker.minValue = 5
        picker.maxValue = 120
        picker.value = PreferenceHelper.getInterval(this).takeIf { it >= 5 } ?: 30

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        saveButton.setOnClickListener {
            val interval = picker.value
            PreferenceHelper.saveInterval(this, interval)
            scheduleAlarm(interval)
            refreshValues()
            dialog.dismiss()
        }
        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showNightBreakDialog() {
        val dialogView = layoutInflater.inflate(R.layout.night_break_picker_dialog, null)
        val startPicker = dialogView.findViewById<TimePicker>(R.id.startTimePicker)
        val endPicker = dialogView.findViewById<TimePicker>(R.id.endTimePicker)
        val saveButton = dialogView.findViewById<Button>(R.id.saveNightBreakButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelNightBreakButton)

        startPicker.setIs24HourView(true)
        endPicker.setIs24HourView(true)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        saveButton.setOnClickListener {
            val start = String.format("%02d:%02d", startPicker.hour, startPicker.minute)
            val end = String.format("%02d:%02d", endPicker.hour, endPicker.minute)
            PreferenceHelper.saveNightBreakStart(this, start)
            PreferenceHelper.saveNightBreakEnd(this, end)
            refreshValues()
            dialog.dismiss()
        }
        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun scheduleAlarm(intervalMinutes: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMillis = intervalMinutes * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + intervalMillis
        PreferenceHelper.saveLastAlarmTime(this, triggerTime)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, triggerTime, intervalMillis, pendingIntent
        )
    }
}
