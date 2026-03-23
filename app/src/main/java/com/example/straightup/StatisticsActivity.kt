package com.example.straightup

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        loadStats()
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        val total = PreferenceHelper.getTotalNotificationsCount(this)
        val confirmations = PreferenceHelper.getConfirmationCount(this)
        val responseRate = PreferenceHelper.getResponseRate(this)
        val currentStreak = PreferenceHelper.getCurrentStreak(this)
        val highestStreak = PreferenceHelper.getHighestStreak(this)
        val weeklyData = PreferenceHelper.getWeeklyData(this)

        findViewById<TextView>(R.id.statTotalNotifications).text = total.toString()
        findViewById<TextView>(R.id.statConfirmations).text = confirmations.toString()
        findViewById<TextView>(R.id.statResponseRate).text = "%.0f%%".format(responseRate)
        findViewById<TextView>(R.id.statCurrentStreak).text = currentStreak.toString()
        findViewById<TextView>(R.id.statHighestStreak).text = highestStreak.toString()

        buildWeeklyChart(weeklyData)
    }

    private fun buildWeeklyChart(data: List<Int>) {
        val chartContainer = findViewById<LinearLayout>(R.id.weeklyChart)
        val labelsContainer = findViewById<LinearLayout>(R.id.weeklyLabels)
        chartContainer.removeAllViews()
        labelsContainer.removeAllViews()

        val maxValue = data.maxOrNull()?.takeIf { it > 0 } ?: 1
        val density = resources.displayMetrics.density
        val chartHeightPx = (density * 96).toInt()
        val barWidthPx = (density * 20).toInt()

        val dayLabels = buildDayLabels()

        data.forEachIndexed { index, value ->
            val colLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                )
            }

            // Słupek
            val barHeightRatio = value.toFloat() / maxValue
            val barHeightPx = (chartHeightPx * barHeightRatio).toInt().coerceAtLeast(4)
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    barWidthPx,
                    barHeightPx
                ).apply {
                    gravity = Gravity.BOTTOM
                }
                setBackgroundColor(
                    if (value > 0) Color.parseColor("#1565C0") else Color.parseColor("#DDDDDD")
                )
                background.apply {
                    // zaokrąglone rogi słupka (prosto przez shape)
                }
            }

            // Liczba nad słupkiem
            val countText = TextView(this).apply {
                text = if (value > 0) value.toString() else ""
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#666666"))
            }

            colLayout.addView(countText)
            colLayout.addView(bar)
            chartContainer.addView(colLayout)

            // Etykieta dnia
            val label = TextView(this).apply {
                text = dayLabels[index]
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#999999"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            labelsContainer.addView(label)
        }
    }

    private fun buildDayLabels(): List<String> {
        val sdf = SimpleDateFormat("EEE", Locale("pl"))
        val cal = Calendar.getInstance()
        return (6 downTo 0).map { daysAgo ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            sdf.format(c.time).replaceFirstChar { it.uppercase() }
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_stats
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_exercises -> {
                    startActivity(Intent(this, ExercisesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_stats -> true
                else -> false
            }
        }
    }
}
