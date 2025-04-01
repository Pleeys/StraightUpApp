package com.example.straightup

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private val refreshHandler = android.os.Handler()
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val interval = PreferenceHelper.getInterval(this@MainActivity)
            if (interval > 0) {
                updateNextAlarmTime(interval)
            }
            refreshHandler.postDelayed(this, 60_000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Save the current login date and update the user's streak.
        PreferenceHelper.saveLoginDate(this)
        findViewById<TextView>(R.id.streakText).text = "${PreferenceHelper.getCurrentStreak(this)} days"
        findViewById<TextView>(R.id.usernameText).text = PreferenceHelper.getNick(this)

        val nightBreakStart = PreferenceHelper.getNightBreakStart(this)
        val nightBreakEnd = PreferenceHelper.getNightBreakEnd(this)
        findViewById<TextView>(R.id.nightBreak).text = "$nightBreakStart - $nightBreakEnd"


        // Navigate to the ProfileActivity when the avatar is clicked.
        findViewById<ImageView>(R.id.avatarImage).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Show the dialog to add a new challenge.
        findViewById<Button>(R.id.addChallengeButton).setOnClickListener {
            showAddChallengeDialog()
        }

        // Request POST_NOTIFICATIONS permission on devices running Android Tiramisu (API 33) or higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        // Create a notification channel for reminder notifications (required on Android Oreo/API 26 and above).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Reminder Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for interval reminders"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Retrieve the saved alarm interval from preferences and update the UI.
        val interval = PreferenceHelper.getInterval(this)
        findViewById<TextView>(R.id.alarmInterval).text = "$interval minutes"

        // Update the alarm time and countdown display based on the interval.
        if (interval == 0) {
            findViewById<TextView>(R.id.alarmTime).text = "--:--"
            findViewById<TextView>(R.id.alarmCountdown).text = "No alarm set"
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            updateNextAlarmTime(interval)
        }

        // Refresh the alarm time display if an interval is set.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && interval > 0) {
            updateNextAlarmTime(interval)
        }

        // Set up click listeners for editing the alarm interval and the night break settings.
        findViewById<ImageView>(R.id.editInterval).setOnClickListener {
            showIntervalPickerDialog()
        }
        findViewById<ImageView>(R.id.editNightBreak).setOnClickListener {
            showNightBreakPickerDialog()
        }

        // Begin periodic UI updates using the refresh handler.
        refreshHandler.post(refreshRunnable)
    }


    private fun showAddChallengeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Challenge")

        val input = EditText(this).apply {
            hint = "Enter challenge name"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val challengeName = input.text.toString().trim()
            Toast.makeText(this, "Added: $challengeName", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleRepeatingNotification(intervalMinutes: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)

        // Anulowanie poprzedniego alarmu, jeśli istnieje
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }

        val actualPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val intervalMillis = intervalMinutes * 60 * 1000L
        val now = System.currentTimeMillis()
        var triggerAt = now + intervalMillis

        // Obsługa przerwy nocnej (night break)
        val nightStart = PreferenceHelper.getNightBreakStart(this)
        val nightEnd = PreferenceHelper.getNightBreakEnd(this)

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
            // Obsługa sytuacji, gdy przedział nocny przechodzi przez północ
            return if (start < end) {
                current in start until end
            } else {
                current >= start || current < end
            }
        }

        val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val triggerHour = cal.get(Calendar.HOUR_OF_DAY)
        val triggerMinute = cal.get(Calendar.MINUTE)

        if (isInNightBreak(triggerHour, triggerMinute)) {
            // Jeśli alarm przypada w czasie przerwy nocnej, ustaw go na koniec przerwy
            val (endH, endM) = parseHourMinute(nightEnd)
            cal.set(Calendar.HOUR_OF_DAY, endH)
            cal.set(Calendar.MINUTE, endM)
            cal.set(Calendar.SECOND, 0)
            // Jeśli wyliczony czas już minął, dodajemy jeden dzień
            if (cal.timeInMillis < now) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            triggerAt = cal.timeInMillis
        }

        // Zapisujemy nowy czas alarmu – można też usunąć tę linię, jeśli nie chcesz korzystać z poprzedniego zapisu
        PreferenceHelper.saveLastAlarmTime(this, triggerAt)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intervalMillis,
            actualPendingIntent
        )
    }


    private fun showIntervalPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.interval_picker_dialog, null)
        val picker = dialogView.findViewById<NumberPicker>(R.id.intervalPicker)
        val saveButton = dialogView.findViewById<Button>(R.id.saveIntervalButton)

        picker.minValue = 5
        picker.maxValue = 120
        picker.value = PreferenceHelper.getInterval(this)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        saveButton.setOnClickListener {
            val interval = picker.value
            PreferenceHelper.saveInterval(this, interval)
            findViewById<TextView>(R.id.alarmInterval).text = "$interval minutes"

            cancelExistingAlarm()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scheduleRepeatingNotification(interval)
            }

            updateNextAlarmTime(interval)

            refreshHandler.removeCallbacks(refreshRunnable)
            refreshHandler.post(refreshRunnable)

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showNightBreakPickerDialog() {
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
            findViewById<TextView>(R.id.nightBreak).text = "$start - $end"

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateNextAlarmTime(interval: Int) {
        val savedTriggerTime = PreferenceHelper.getLastAlarmTime(this)
        val nextAlarm = Calendar.getInstance().apply { timeInMillis = savedTriggerTime }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getDefault()
        val formattedTime = timeFormat.format(nextAlarm.time)

        findViewById<TextView>(R.id.alarmTime).text = formattedTime
        findViewById<TextView>(R.id.alarmPeriod).visibility = View.GONE

        val now = Calendar.getInstance().timeInMillis
        val diffMillis = nextAlarm.timeInMillis - now
        val minutesUntilAlarm = (diffMillis / 60000).toInt()

        val countdownText = when {
            minutesUntilAlarm < 1 -> "less than a minute"
            minutesUntilAlarm == 1 -> "in 1 minute"
            else -> "in $minutesUntilAlarm minutes"
        }

        findViewById<TextView>(R.id.alarmCountdown).text = countdownText
    }

    private fun cancelExistingAlarm() {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshHandler.removeCallbacks(refreshRunnable)
    }
}
