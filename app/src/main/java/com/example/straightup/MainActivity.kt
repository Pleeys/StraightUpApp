package com.example.straightup

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var challengeAdapter: ChallengeAdapter
    private val challenges = mutableListOf<Challenge>()

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

    enum class Priority {
        LOW, MEDIUM, HIGH
    }

    data class Challenge(val title: String, val current: Int = 0, val total: Int = 3, val priority: Priority)

    class ChallengeAdapter(private val challenges: List<Challenge>) :
        RecyclerView.Adapter<ChallengeAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.challengeTitle)
            val progressText: TextView = itemView.findViewById(R.id.progressText)
            val progressBar: ProgressBar = itemView.findViewById(R.id.challengeProgressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.challenge_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = challenges.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val challenge = challenges[position]
            holder.title.text = challenge.title
            holder.progressText.text = "${challenge.current} / ${challenge.total}"
            holder.progressBar.max = challenge.total
            holder.progressBar.progress = challenge.current

            val bg = when (challenge.priority) {
                Priority.LOW -> R.drawable.challenge_item_low
                Priority.MEDIUM -> R.drawable.challenge_item_medium
                Priority.HIGH -> R.drawable.challenge_item_high
            }
            holder.itemView.setBackgroundResource(bg)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        challenges.addAll(ChallengeStorage.loadChallenges(this))

        PreferenceHelper.saveLoginDate(this)
        findViewById<TextView>(R.id.streakText).text = "${PreferenceHelper.getCurrentStreak(this)} days"
        findViewById<TextView>(R.id.usernameText).text = PreferenceHelper.getNick(this)

        val nightBreakStart = PreferenceHelper.getNightBreakStart(this)
        val nightBreakEnd = PreferenceHelper.getNightBreakEnd(this)
        findViewById<TextView>(R.id.nightBreak).text = "$nightBreakStart - $nightBreakEnd"

        findViewById<ImageView>(R.id.avatarImage).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.addChallengeButton).setOnClickListener {
            showAddChallengeDialog { newChallenge ->
                challenges.add(newChallenge)
                challengeAdapter.notifyItemInserted(challenges.size - 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

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

        val interval = PreferenceHelper.getInterval(this)
        findViewById<TextView>(R.id.alarmInterval).text = "$interval minutes"

        if (interval == 0) {
            findViewById<TextView>(R.id.alarmTime).text = "--:--"
            findViewById<TextView>(R.id.alarmCountdown).text = "No alarm set"
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            updateNextAlarmTime(interval)
        }

        findViewById<ImageView>(R.id.editInterval).setOnClickListener {
            showIntervalPickerDialog()
        }
        findViewById<ImageView>(R.id.editNightBreak).setOnClickListener {
            showNightBreakPickerDialog()
        }

        refreshHandler.post(refreshRunnable)

        setupChallengeList()

        updateChallengeCount()

    }

    private fun showIntervalPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.interval_picker_dialog, null)
        val picker = dialogView.findViewById<NumberPicker>(R.id.intervalPicker)
        val saveButton = dialogView.findViewById<Button>(R.id.saveIntervalButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelIntervalButton)

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
            updateNextAlarmTime(interval)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddChallengeDialog(onChallengeAdded: (Challenge) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.add_challenge_dialog, null)

        val input = dialogView.findViewById<EditText>(R.id.inputChallenge)
        val counter = dialogView.findViewById<TextView>(R.id.charCounter)
        val saveButton = dialogView.findViewById<Button>(R.id.saveChallengeButton)

        val priorityLow = dialogView.findViewById<View>(R.id.priorityLow)
        val priorityMedium = dialogView.findViewById<View>(R.id.priorityMedium)
        val priorityHigh = dialogView.findViewById<View>(R.id.priorityHigh)

        var selectedPriority: Priority? = null

        fun updateSelection(selected: View, priority: Priority) {
            dialogView.findViewById<ImageView>(R.id.priorityLowSelected).visibility = View.GONE
            dialogView.findViewById<ImageView>(R.id.priorityMediumSelected).visibility = View.GONE
            dialogView.findViewById<ImageView>(R.id.priorityHighSelected).visibility = View.GONE

            val dotId = when (priority) {
                Priority.LOW -> R.id.priorityLowSelected
                Priority.MEDIUM -> R.id.priorityMediumSelected
                Priority.HIGH -> R.id.priorityHighSelected
            }
            dialogView.findViewById<ImageView>(dotId).visibility = View.VISIBLE

            selectedPriority = priority
        }

        priorityLow.setOnClickListener { updateSelection(priorityLow, Priority.LOW) }
        priorityMedium.setOnClickListener { updateSelection(priorityMedium, Priority.MEDIUM) }
        priorityHigh.setOnClickListener { updateSelection(priorityHigh, Priority.HIGH) }

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                counter.text = "${s?.length ?: 0}/50"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        saveButton.setOnClickListener {
            val name = input.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a challenge name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedPriority == null) {
                Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val challenge = Challenge(title = name, priority = selectedPriority!!)
            onChallengeAdded(challenge)
            ChallengeStorage.saveChallenges(this, challenges)
            updateChallengeCount()
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

    private fun setupChallengeList() {
        val recyclerView = findViewById<RecyclerView>(R.id.challengesRecyclerView)

        challenges.clear()
        challenges.addAll(ChallengeStorage.loadChallenges(this))

        challengeAdapter = ChallengeAdapter(challenges)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = challengeAdapter
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

    override fun onDestroy() {
        super.onDestroy()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun updateChallengeCount() {
        val count = challenges.size
        if(count == 0){
            findViewById<TextView>(R.id.challengeCount).text = "Add new challenges!"
        }
        else {
            findViewById<TextView>(R.id.challengeCount).text = "Challenges ($count)"
        }
    }


}