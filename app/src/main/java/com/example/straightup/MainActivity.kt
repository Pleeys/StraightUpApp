package com.example.straightup

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    enum class Priority { LOW, MEDIUM, HIGH }

    data class Challenge(
        val title: String,
        val current: Int = 0,
        val total: Int = 3,
        val priority: Priority,
        val isUserCreated: Boolean = false,
        val isCompleted: Boolean = false
    )

    private val refreshHandler = android.os.Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val interval = PreferenceHelper.getInterval(this@MainActivity)
            if (interval > 0) updateNextAlarmDisplay()
            refreshHandler.postDelayed(this, 60_000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PreferenceHelper.saveLoginDate(this)

        setupHeader()
        setupNotificationChannel()
        setupAlarmSection()
        setupConfirmButton()
        setupBottomNav()
        requestNotificationPermission()
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.usernameText).text = PreferenceHelper.getNick(this)
        findViewById<TextView>(R.id.streakText).text = "${PreferenceHelper.getCurrentStreak(this)} dni"
        findViewById<ImageView>(R.id.avatarImage).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<ImageView>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Przypomnienia o postawie",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kanał przypomnień o prostowaniu pleców"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupAlarmSection() {
        val nightBreakStart = PreferenceHelper.getNightBreakStart(this)
        val nightBreakEnd = PreferenceHelper.getNightBreakEnd(this)
        findViewById<TextView>(R.id.nightBreak).text = "$nightBreakStart - $nightBreakEnd"

        val interval = PreferenceHelper.getInterval(this)
        findViewById<TextView>(R.id.alarmInterval).text =
            if (interval == 0) "Ustaw!" else "$interval minut"

        if (interval == 0) {
            findViewById<TextView>(R.id.alarmTime).text = "--:--"
            findViewById<TextView>(R.id.alarmCountdown).text = "Brak alarmu"
        } else {
            // Jeśli czas alarmu już minął (lub nigdy nie był ustawiony), zaplanuj od nowa
            val lastAlarmTime = PreferenceHelper.getLastAlarmTime(this)
            if (lastAlarmTime == -1L || lastAlarmTime < System.currentTimeMillis()) {
                scheduleAlarm(interval)
            }
            updateNextAlarmDisplay()
        }

        findViewById<ImageView>(R.id.editInterval).setOnClickListener {
            showIntervalPickerDialog()
        }
        findViewById<ImageView>(R.id.editNightBreak).setOnClickListener {
            showNightBreakPickerDialog()
        }
    }

    private fun setupConfirmButton() {
        updateConfirmCount()
        findViewById<Button>(R.id.confirmPostureButton).setOnClickListener {
            PreferenceHelper.saveConfirmation(this)
            updateConfirmCount()
            Toast.makeText(this, "Brawo! Zadbałeś o plecy 💪", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateConfirmCount() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val todayCount = prefs.getInt("confirm_day_$today", 0)
        findViewById<TextView>(R.id.confirmCountText).text = "Dziś: $todayCount potwierdzeń"
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_exercises -> {
                    startActivity(Intent(this, ExercisesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    // --- Planowanie alarmu ---

    private fun scheduleAlarm(intervalMinutes: Int) {
        AlarmReceiver.scheduleNext(this, intervalMinutes)
    }

    private fun cancelAlarm() {
        AlarmReceiver.cancel(this)
    }

    // --- Dialogi ---

    private fun showIntervalPickerDialog() {
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
            findViewById<TextView>(R.id.alarmInterval).text = "$interval minut"
            scheduleAlarm(interval)          // ← planuje alarm w systemie
            updateNextAlarmDisplay()
            dialog.dismiss()
        }
        cancelButton.setOnClickListener { dialog.dismiss() }
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
        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // --- Wyświetlanie czasu ---

    private fun updateNextAlarmDisplay() {
        val savedTriggerTime = PreferenceHelper.getLastAlarmTime(this)
        if (savedTriggerTime == -1L) {
            findViewById<TextView>(R.id.alarmTime).text = "--:--"
            findViewById<TextView>(R.id.alarmCountdown).text = "Brak alarmu"
            return
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getDefault()
        findViewById<TextView>(R.id.alarmTime).text =
            timeFormat.format(Date(savedTriggerTime))

        val diffMillis = savedTriggerTime - System.currentTimeMillis()
        val minutesUntil = (diffMillis / 60000).toInt()

        findViewById<TextView>(R.id.alarmCountdown).text = when {
            minutesUntil < 1 -> "za chwilę"
            minutesUntil == 1 -> "za 1 minutę"
            else -> "za $minutesUntil minut"
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
        setupHeader()
        setupAlarmSection()
        updateConfirmCount()
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshHandler.removeCallbacks(refreshRunnable)
    }
}
