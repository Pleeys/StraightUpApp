package com.example.straightup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChallengeAdapter(private val challenges: List<MainActivity.Challenge>) :
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

        val bgRes = when (challenge.priority) {
            MainActivity.Priority.LOW -> R.drawable.challenge_item_low
            MainActivity.Priority.MEDIUM -> R.drawable.challenge_item_medium
            MainActivity.Priority.HIGH -> R.drawable.challenge_item_high
        }
        holder.itemView.setBackgroundResource(bgRes)
    }
}
