package com.astralimit.dogfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ActivityHistoryAdapter(private var data: List<DogActivityData>) :
    RecyclerView.Adapter<ActivityHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.timeText)
        val activityText: TextView = view.findViewById(R.id.activityText)
        val stepsText: TextView = view.findViewById(R.id.stepsText)
        val caloriesText: TextView = view.findViewById(R.id.caloriesText)
        val activityIcon: ImageView = view.findViewById(R.id.activityIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val context = holder.itemView.context

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.timeText.text = timeFormat.format(Date(item.timestamp))

        val activityType = when (item.activityType) {
            0 -> {
                // Icono para reposo
                try {
                    holder.activityIcon.setImageResource(R.drawable.ic_rest)
                } catch (e: Exception) {
                    // Si no existe el icono, usa uno del sistema
                    holder.activityIcon.setImageResource(android.R.drawable.ic_lock_idle_lock)
                }
                context.getString(R.string.activity_rest)
            }
            1 -> {
                // Icono para caminata
                try {
                    holder.activityIcon.setImageResource(R.drawable.ic_walk)
                } catch (e: Exception) {
                    holder.activityIcon.setImageResource(android.R.drawable.ic_dialog_map)
                }
                context.getString(R.string.activity_walk)
            }
            2 -> {
                // Icono para carrera
                try {
                    holder.activityIcon.setImageResource(R.drawable.ic_run)
                } catch (e: Exception) {
                    holder.activityIcon.setImageResource(android.R.drawable.star_big_on)
                }
                context.getString(R.string.activity_run)
            }
            3 -> {
                // Icono para juego
                try {
                    holder.activityIcon.setImageResource(R.drawable.ic_play)
                } catch (e: Exception) {
                    holder.activityIcon.setImageResource(android.R.drawable.ic_media_play)
                }
                context.getString(R.string.activity_play)
            }
            else -> {
                // Icono desconocido
                try {
                    holder.activityIcon.setImageResource(R.drawable.ic_unknown)
                } catch (e: Exception) {
                    holder.activityIcon.setImageResource(android.R.drawable.ic_menu_help)
                }
                context.getString(R.string.activity_unknown)
            }
        }

        // Establecer color según actividad
        val colorRes = when (item.activityType) {
            0 -> R.color.activity_rest
            1 -> R.color.activity_walk
            2 -> R.color.activity_run
            3 -> R.color.activity_play
            else -> R.color.activity_unknown
        }

        try {
            val color = ContextCompat.getColor(context, colorRes)
            holder.activityIcon.setColorFilter(color)
        } catch (e: Exception) {
            // Si falla el color, usa el default
        }

        holder.activityText.text = activityType
        holder.stepsText.text = "${item.durationMinutes} min"
        holder.caloriesText.text = "${String.format("%.1f", item.calories)} cal"
    }

    override fun getItemCount() = data.size

    fun updateData(newData: List<DogActivityData>) {
        data = newData
        notifyDataSetChanged()
    }
}