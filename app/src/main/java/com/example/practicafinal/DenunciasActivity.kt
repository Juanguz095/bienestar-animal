package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.model.Denuncia
import com.example.practicafinal.util.fechaRelativa
import java.util.concurrent.Executors

class DenunciasActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: RecyclerView;
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_denuncias)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_denuncias); tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_denunciar).setOnClickListener {
            startActivity(Intent(this, CrearDenunciaActivity::class.java))
        }
        cargar()
    }

    private fun cargar() {
        exec.execute {
            val lista = DatabaseHelper(this).obtenerDenuncias()
            runOnUiThread {
                tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                        LayoutInflater.from(p.context).inflate(R.layout.item_denuncia_card, p, false)
                    ) {}

                    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                        val d = lista[pos]
                        (h.itemView.findViewById<TextView>(R.id.tv_motivo)).text = d.motivo
                        (h.itemView.findViewById<TextView>(R.id.tv_desc)).text = d.descripcion.take(80)
                        (h.itemView.findViewById<TextView>(R.id.tv_fecha)).text = fechaRelativa(d.fecha)
                        h.itemView.setOnClickListener {
                            startActivity(
                                Intent(
                                    this@DenunciasActivity,
                                    DetalleDenunciaActivity::class.java
                                ).apply { putExtra("denuncia_id", d.id) })
                        }
                    }

                    override fun getItemCount() = lista.size
                }
            }
        }
    }

    override fun onResume() {
        super.onResume(); cargar()
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
