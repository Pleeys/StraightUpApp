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
        return prefs.getInt(KEY_INTERVAL, 0)
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

    private const val KEY_LAST_ALARM_TIME = "last_alarm_time"

    fun saveLastAlarmTime(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_ALARM_TIME, timestamp).apply()
    }

    fun getLastAlarmTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_ALARM_TIME, -1L)
    }

    private const val KEY_NIGHT_BREAK_START = "night_break_start"
    private const val KEY_NIGHT_BREAK_END = "night_break_end"

    fun saveNightBreakStart(context: Context, time: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NIGHT_BREAK_START, time).apply()
    }

    fun saveNightBreakEnd(context: Context, time: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NIGHT_BREAK_END, time).apply()
    }

    fun getNightBreakStart(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NIGHT_BREAK_START, "22:00") ?: "22:00"

    fun getNightBreakEnd(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NIGHT_BREAK_END, "09:00") ?: "09:00"

    // --- Statystyki potwierdzeń ---
    private const val KEY_CONFIRM_COUNT = "confirm_count"
    private const val KEY_TOTAL_NOTIFICATIONS = "total_notifications"
    private const val KEY_CONFIRM_PREFIX = "confirm_day_"

    fun incrementTotalNotifications(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_TOTAL_NOTIFICATIONS, 0)
        prefs.edit().putInt(KEY_TOTAL_NOTIFICATIONS, current + 1).apply()
    }

    fun saveConfirmation(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Całkowita liczba potwierdzeń
        val current = prefs.getInt(KEY_CONFIRM_COUNT, 0)
        prefs.edit().putInt(KEY_CONFIRM_COUNT, current + 1).apply()
        // Potwierdzenie dla dzisiejszego dnia (format: confirm_day_2026-03-23)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val dayKey = KEY_CONFIRM_PREFIX + today
        val todayCount = prefs.getInt(dayKey, 0)
        prefs.edit().putInt(dayKey, todayCount + 1).apply()
    }

    fun getConfirmationCount(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_CONFIRM_COUNT, 0)
    }

    fun getTotalNotificationsCount(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TOTAL_NOTIFICATIONS, 0)
    }

    fun getResponseRate(context: Context): Float {
        val total = getTotalNotificationsCount(context)
        if (total == 0) return 0f
        return getConfirmationCount(context).toFloat() / total * 100f
    }

    /** Zwraca liczbę potwierdzeń dla każdego z ostatnich 7 dni (od najstarszego do dziś) */
    fun getWeeklyData(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        return (6 downTo 0).map { daysAgo ->
            val cal = calendar.clone() as java.util.Calendar
            cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            val dateStr = sdf.format(cal.time)
            prefs.getInt(KEY_CONFIRM_PREFIX + dateStr, 0)
        }
    }

}
