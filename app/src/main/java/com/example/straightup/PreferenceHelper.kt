package com.example.straightup

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object PreferenceHelper {

    private const val PREF_NAME = "UserPrefs"

    // Keys
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_INTERVAL = "interval_minutes"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_HIGHEST_STREAK = "highest_streak"
    private const val KEY_LAST_LOGIN = "last_login"

    // Nickname
    fun saveNick(context: Context, nick: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NICKNAME, nick).apply()
    }

    fun getNick(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NICKNAME, "user") ?: "user"
    }

    // Interval (in minutes)
    fun saveInterval(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_INTERVAL, minutes).apply()
    }

    fun getInterval(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERVAL, 45) // default 45 minutes
    }

    // Streaks
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveLoginDate(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val lastLoginString = prefs.getString(KEY_LAST_LOGIN, null)

        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val highestStreak = prefs.getInt(KEY_HIGHEST_STREAK, 0)

        if (lastLoginString != null) {
            val lastLogin = LocalDate.parse(lastLoginString)
            val daysBetween = ChronoUnit.DAYS.between(lastLogin, today)

            when (daysBetween) {
                0L -> return // Already logged in today
                1L -> currentStreak += 1
                else -> currentStreak = 1 // Missed a day
            }
        } else {
            currentStreak = 1 // First login
        }

        val newHighest = maxOf(currentStreak, highestStreak)

        prefs.edit()
            .putString(KEY_LAST_LOGIN, today.toString())
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_HIGHEST_STREAK, newHighest)
            .apply()
    }

    fun getCurrentStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    fun getHighestStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HIGHEST_STREAK, 0)
    }
}
