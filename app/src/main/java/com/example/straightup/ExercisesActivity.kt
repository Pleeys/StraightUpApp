package com.example.straightup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExercisesActivity : AppCompatActivity() {

    private val exercises = mutableListOf(
        Exercise(0, "Rozciąganie szyi", "5 minut"),
        Exercise(1, "Mostek (bridge)", "3 serie × 10 powtórzeń"),
        Exercise(2, "Deska (plank)", "3 × 30 sekund"),
        Exercise(3, "Spacer", "10 minut"),
        Exercise(4, "Rozciąganie klatki piersiowej", "3 minuty"),
        Exercise(5, "Rotacje tułowia", "2 × 10 w każdą stronę"),
        Exercise(6, "Cat-Cow (joga)", "10 powtórzeń")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercises)

        loadDoneState()

        val recyclerView = findViewById<RecyclerView>(R.id.exercisesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ExerciseAdapter(exercises) { exercise, isDone ->
            saveDoneState(exercise.id, isDone)
        }

        setupBottomNav()
    }

    private fun todayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun loadDoneState() {
        val prefs = getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)
        val today = todayKey()
        exercises.forEach { exercise ->
            exercise.isDone = prefs.getBoolean("done_${exercise.id}_$today", false)
        }
    }

    private fun saveDoneState(id: Int, isDone: Boolean) {
        val prefs = getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)
        val today = todayKey()
        prefs.edit().putBoolean("done_${id}_$today", isDone).apply()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_exercises
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_exercises -> true
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
