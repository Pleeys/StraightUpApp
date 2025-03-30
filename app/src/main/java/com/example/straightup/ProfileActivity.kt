package com.example.straightup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Handle back button behavior
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Configure action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        // Initialize username TextView and edit icon
        val usernameText = findViewById<TextView>(R.id.usernameText)
        val editIcon = findViewById<ImageView>(R.id.editIcon)

        // Load saved nickname from shared preferences
        val savedNickname = PreferenceHelper.getNick(this)
        usernameText.text = savedNickname

        // Show edit dialog on icon click
        editIcon.setOnClickListener {
            showEditNicknameDialog(usernameText)
        }

        // Initialize streak views
        val streakValueText = findViewById<TextView>(R.id.streakValueText)
        val highestStreakValueText = findViewById<TextView>(R.id.higheststreakValueText)

        // Load current and highest streak values
        val current = PreferenceHelper.getCurrentStreak(this)
        val highest = PreferenceHelper.getHighestStreak(this)

        // Display streak values
        streakValueText.text = "$current days"
        highestStreakValueText.text = "$highest days"
    }

    /**
     * Displays a dialog that allows the user to edit their nickname.
     */
    private fun showEditNicknameDialog(usernameText: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.edit_nickname_dialog, null)

        val input = dialogView.findViewById<EditText>(R.id.nicknameInput)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Pre-fill the input with the current nickname
        input.setText(usernameText.text.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Save new nickname when confirmed
        saveButton.setOnClickListener {
            val newNickname = input.text.toString().trim()

            if (newNickname.isNotEmpty()) {
                usernameText.text = newNickname
                PreferenceHelper.saveNick(this, newNickname)
                Toast.makeText(this, "Nickname updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        // Close dialog on cancel
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Apply transparent background for cleaner appearance
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
