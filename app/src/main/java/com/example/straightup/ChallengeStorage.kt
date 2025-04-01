package com.example.straightup

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ChallengeStorage {

    private const val PREFS_NAME = "challenge_prefs"
    private const val KEY_CHALLENGES = "challenges"

    fun saveChallenges(context: Context, challenges: List<MainActivity.Challenge>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(challenges)
        prefs.edit().putString(KEY_CHALLENGES, json).apply()
    }

    fun loadChallenges(context: Context): MutableList<MainActivity.Challenge> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CHALLENGES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<MainActivity.Challenge>>() {}.type
        return Gson().fromJson(json, type)
    }
}
