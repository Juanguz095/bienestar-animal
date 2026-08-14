package com.example.practicafinal.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.R
import com.example.practicafinal.modelo.Animal

class AdaptadorAnimales(private val items: List<Animal>) :
    RecyclerView.Adapter<AdaptadorAnimales.AnimalViewHolder>() {

    class AnimalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tv_emoji)
        val tvEspecie: TextView = view.findViewById(R.id.tv_especie)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvSubtitulo: TextView = view.findViewById(R.id.tv_subtitulo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_animal_card, parent, false)
        return AnimalViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        val animal = items[position]
        holder.tvEmoji.text = if (animal.especie.equals("Perro", ignoreCase = true)) "🐶" else "🐱"
        holder.tvEspecie.text = animal.especie
        holder.tvNombre.text = animal.nombre
        holder.tvSubtitulo.text = "Edad: ${animal.edad} años · ${animal.raza}"
    }

    override fun getItemCount(): Int = items.size
}
