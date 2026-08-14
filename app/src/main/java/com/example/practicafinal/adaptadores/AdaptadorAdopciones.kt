package com.example.practicafinal.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.R
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.util.decodificarImagen

class AdaptadorAdopciones(
    private val items: List<Publicacion>,
    private val onCardClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<AdaptadorAdopciones.AdopcionViewHolder>() {

    class AdopcionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvEspecieChip: TextView = view.findViewById(R.id.tv_especie_chip)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvDesc: TextView = view.findViewById(R.id.tv_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdopcionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_adopcion_card, parent, false)
        return AdopcionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdopcionViewHolder, position: Int) {
        val p = items[position]

        val foto = p.foto?.let { decodificarImagen(holder.itemView.context, it, 4) }
        if (foto != null) {
            holder.imgFoto.setImageBitmap(foto)
        } else {
            val res =
                if (p.especie.equals("Gato", ignoreCase = true)) R.drawable.gato_adopcion else R.drawable.perro_adopcion
            holder.imgFoto.setImageResource(res)
        }
        holder.tvPlaceholder.visibility = View.GONE

        val especie = p.especie?.take(20) ?: ""
        if (especie.isNotEmpty()) {
            holder.tvEspecieChip.text = especie
            holder.tvEspecieChip.visibility = View.VISIBLE
        } else {
            holder.tvEspecieChip.visibility = View.GONE
        }

        holder.tvNombre.text = p.nombre
        holder.tvDesc.text = p.descripcion
        holder.itemView.setOnClickListener { onCardClick(p) }
    }

    override fun getItemCount(): Int = items.size
}
