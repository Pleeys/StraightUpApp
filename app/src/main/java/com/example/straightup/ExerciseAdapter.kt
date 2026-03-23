package com.example.straightup

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Exercise(
    val id: Int,
    val title: String,
    val duration: String,
    var isDone: Boolean = false
)

class ExerciseAdapter(
    private val exercises: MutableList<Exercise>,
    private val onCheckedChange: (Exercise, Boolean) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.exerciseCheckbox)
        val title: TextView = itemView.findViewById(R.id.exerciseTitle)
        val duration: TextView = itemView.findViewById(R.id.exerciseDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.exercise_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = exercises.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = exercises[position]

        // Blokujemy listener przed ustawieniem wartości żeby nie triggerować callback
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = exercise.isDone

        holder.title.text = exercise.title
        holder.duration.text = exercise.duration

        applyStrikethrough(holder, exercise.isDone)

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            exercise.isDone = isChecked
            applyStrikethrough(holder, isChecked)
            onCheckedChange(exercise, isChecked)
        }
    }

    private fun applyStrikethrough(holder: ViewHolder, isDone: Boolean) {
        if (isDone) {
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.title.alpha = 0.5f
        } else {
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.title.alpha = 1f
        }
    }
}
