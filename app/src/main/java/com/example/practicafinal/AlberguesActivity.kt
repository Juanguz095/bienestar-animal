package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.model.Albergue
import com.example.practicafinal.util.decodificarImagen
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.Executors

class AlberguesActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_albergues)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_albergues); tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)
        cargar()
    }

    private fun cargar() {
        exec.execute {
            val lista = DatabaseHelper(this).obtenerAlbergues()
            runOnUiThread {
                tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                        LayoutInflater.from(p.context).inflate(R.layout.item_albergue_card, p, false)
                    ) {}

                    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                        val a = lista[pos]
                        val foto = a.foto?.let { decodificarImagen(h.itemView.context, it, 4) }
                        val img = h.itemView.findViewById<android.widget.ImageView>(R.id.img_foto)
                        val pl = h.itemView.findViewById<TextView>(R.id.tv_placeholder)
                        if (foto != null) {
                            img.setImageBitmap(foto); pl.visibility = View.GONE
                        } else {
                            pl.visibility = View.VISIBLE
                        }
                        (h.itemView.findViewById<TextView>(R.id.tv_nombre)).text = a.nombre
                        (h.itemView.findViewById<TextView>(R.id.tv_direccion)).text = a.direccion
                        // TODO: calcular distancia real
                        (h.itemView.findViewById<TextView>(R.id.tv_distancia)).text = ""
                        h.itemView.setOnClickListener {
                            startActivity(Intent(this@AlberguesActivity, DetalleAlbergueActivity::class.java).apply {
                                putExtra("albergue_id", a.id)
                            })
                        }
                    }

                    override fun getItemCount() = lista.size
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
