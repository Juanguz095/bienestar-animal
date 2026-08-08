package com.example.practicafinal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.R
import com.example.practicafinal.model.Publicacion
import com.example.practicafinal.util.decodificarImagen
import com.example.practicafinal.util.fechaRelativa

class PublicacionesPropiasAdapter(
    private val items: List<Publicacion>,
    private val onResolver: (Publicacion) -> Unit
) : RecyclerView.Adapter<PublicacionesPropiasAdapter.PubViewHolder>() {

    class PubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvTipo: TextView = view.findViewById(R.id.tv_tipo)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado)
        val btnResolver: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btn_resolver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PubViewHolder {
        return PubViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mi_publicacion, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PubViewHolder, position: Int) {
        val p = items[position]

        val foto = p.foto?.let { decodificarImagen(holder.itemView.context, it, 8) }
        if (foto != null) {
            holder.imgFoto.setImageBitmap(foto)
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            holder.tvPlaceholder.visibility = View.VISIBLE
            holder.tvPlaceholder.text = when {
                p.especie.equals("Perro", ignoreCase = true) -> "🐶"
                p.especie.equals("Gato", ignoreCase = true) -> "🐱"
                else -> "🐾"
            }
        }

        holder.tvNombre.text = p.nombre
        holder.tvTipo.text = when (p.tipo) {
            "Perdida" -> "Mascota perdida"
            "Encontrada" -> "Mascota encontrada"
            else -> "En adopción"
        }
        holder.tvFecha.text = fechaRelativa(p.fechaCreacion)

        val resuelta = p.estado == "Resuelta"
        holder.tvEstado.text = if (resuelta) "Resuelta" else "Activa"
        holder.tvEstado.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (resuelta) android.R.color.darker_gray else R.color.verde_estado
            )
        )
        holder.btnResolver.visibility = if (resuelta) View.GONE else View.VISIBLE
        holder.btnResolver.setOnClickListener { onResolver(p) }
    }

    override fun getItemCount(): Int = items.size
}
