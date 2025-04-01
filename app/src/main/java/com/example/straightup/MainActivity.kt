package com.example.straightup

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Save today's login date and update streak
        PreferenceHelper.saveLoginDate(this)
        findViewById<TextView>(R.id.streakText).text = "${PreferenceHelper.getCurrentStreak(this)} days"

        // Load and display saved nickname
        findViewById<TextView>(R.id.usernameText).text = PreferenceHelper.getNick(this)

        // Navigate to profile screen when avatar is clicked
        findViewById<ImageView>(R.id.avatarImage).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Show dialog to add a new challenge
        findViewById<Button>(R.id.addChallengeButton).setOnClickListener {
            showAddChallengeDialog()
        }

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        // Create notification channel for reminders (Android 8+)
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

    // Load and display saved interval
        val interval = PreferenceHelper.getInterval(this)
        findViewById<TextView>(R.id.alarmInterval).text = "$interval minutes"

    // Jeśli interwał nieustawiony – pokaż placeholdery
        if (interval == 0) {
            findViewById<TextView>(R.id.alarmTime).text = "--:--"
            findViewById<TextView>(R.id.alarmCountdown).text = "No alarm set"
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Jeśli ustawiony, pokaż normalnie
            updateNextAlarmTime(interval)
        }


    // Jeśli użytkownik wcześniej ustawił interwał > 0, ustaw alarm i pokaż czas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && interval > 0) {
            updateNextAlarmTime(interval)
        }

    // Show interval picker dialog on edit button click
        findViewById<ImageView>(R.id.editInterval).setOnClickListener {
            showIntervalPickerDialog()
        }

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
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        // Jeśli pendingIntent już istnieje, nie ustawiamy alarmu ponownie
        if (pendingIntent != null) {
            return
        }

        val actualPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val intervalMillis = intervalMinutes * 60 * 1000
        val triggerAt = System.currentTimeMillis() + intervalMillis

        PreferenceHelper.saveLastAlarmTime(this, triggerAt)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intervalMillis.toLong(),
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

        saveButton.setOnClickListener {
            val interval = picker.value
            PreferenceHelper.saveInterval(this, interval)
            findViewById<TextView>(R.id.alarmInterval).text = "$interval minutes"
            updateNextAlarmTime(interval)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scheduleRepeatingNotification(interval)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    // Update displayed next alarm time
    private fun updateNextAlarmTime(interval: Int) {
        val savedTriggerTime = PreferenceHelper.getLastAlarmTime(this)

        val nextAlarm = if (savedTriggerTime > 0) {
            val now = System.currentTimeMillis()
            val elapsed = now - savedTriggerTime
            val intervalsPassed = (elapsed / (interval * 60 * 1000)).toInt()
            val next = savedTriggerTime + ((intervalsPassed + 1) * interval * 60 * 1000)
            Calendar.getInstance().apply { timeInMillis = next }
        } else {
            Calendar.getInstance().apply { add(Calendar.MINUTE, interval) }
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(nextAlarm.time)

        findViewById<TextView>(R.id.alarmTime).text = formattedTime
        findViewById<TextView>(R.id.alarmPeriod).visibility = View.GONE
    }

    // Handle permission result callback
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
    }
}
