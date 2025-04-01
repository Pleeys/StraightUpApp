package com.example.straightup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ChallengeAdapter(private val challenges: MutableList<MainActivity.Challenge>) :
    RecyclerView.Adapter<ChallengeAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.challengeTitle)
        val progressText: TextView? = itemView.findViewById(R.id.progressText)
        val progressBar: ProgressBar? = itemView.findViewById(R.id.challengeProgressBar)
        val doneCircle: ImageView? = itemView.findViewById(R.id.doneCircle)
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

        if (holder.doneCircle != null) {
            holder.progressText?.visibility = View.GONE
            holder.progressBar?.visibility = View.GONE
            holder.doneCircle.visibility = View.VISIBLE

            holder.doneCircle.setOnClickListener {
                holder.doneCircle.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .withEndAction {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            challenges.removeAt(pos)
                            notifyItemRemoved(pos)
                            Toast.makeText(
                                holder.itemView.context,
                                "Congratulations!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }.start()
            }
        } else {
            holder.progressText?.text = "${challenge.current} / ${challenge.total}"
            holder.progressBar?.max = challenge.total
            holder.progressBar?.progress = challenge.current
        }

        val bgRes = when (challenge.priority) {
            MainActivity.Priority.LOW -> R.drawable.challenge_item_low
            MainActivity.Priority.MEDIUM -> R.drawable.challenge_item_medium
            MainActivity.Priority.HIGH -> R.drawable.challenge_item_high
        }
        holder.itemView.setBackgroundResource(bgRes)
    }
}
