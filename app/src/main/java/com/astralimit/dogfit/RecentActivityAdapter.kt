package com.astralimit.dogfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RecentActivityAdapter(private var data: List<DogActivityData>) :
    RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val activityIcon: ImageView = view.findViewById(R.id.activityIcon)
        val activityTypeText: TextView = view.findViewById(R.id.activityTypeText)
        val activityTimeText: TextView = view.findViewById(R.id.activityTimeText)
        val stepsText: TextView = view.findViewById(R.id.stepsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_compact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val context = holder.itemView.context

        // Configurar icono según tipo de actividad
        when (item.activityType) {
            0 -> { // Reposo
                holder.activityIcon.setImageResource(R.drawable.ic_rest)
                holder.activityIcon.setColorFilter(
                    context.getColor(R.color.activity_rest)
                )
                holder.activityTypeText.text = context.getString(R.string.activity_rest)
            }
            1 -> { // Caminata
                holder.activityIcon.setImageResource(R.drawable.ic_walk)
                holder.activityIcon.setColorFilter(
                    context.getColor(R.color.activity_walk)
                )
                holder.activityTypeText.text = context.getString(R.string.activity_walk)
            }
            2 -> { // Carrera
                holder.activityIcon.setImageResource(R.drawable.ic_run)
                holder.activityIcon.setColorFilter(
                    context.getColor(R.color.activity_run)
                )
                holder.activityTypeText.text = context.getString(R.string.activity_run)
            }
            3 -> { // Juego
                holder.activityIcon.setImageResource(R.drawable.ic_play)
                holder.activityIcon.setColorFilter(
                    context.getColor(R.color.activity_play)
                )
                holder.activityTypeText.text = context.getString(R.string.activity_play)
            }
            else -> {
                holder.activityIcon.setImageResource(R.drawable.ic_unknown)
                holder.activityIcon.setColorFilter(
                    context.getColor(R.color.activity_unknown)
                )
                holder.activityTypeText.text = context.getString(R.string.activity_unknown)
            }
        }

        // Configurar hora
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.activityTimeText.text = timeFormat.format(Date(item.timestamp))

        // Configurar duración
        holder.stepsText.text = "${item.durationMinutes} min"
    }

    override fun getItemCount() = data.size

    fun updateData(newData: List<DogActivityData>) {
        data = newData.takeLast(5).reversed() // Mostrar las 5 más recientes
        notifyDataSetChanged()
    }
}