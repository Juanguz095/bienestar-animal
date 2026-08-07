package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.adapters.PerdidasAdapter
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.model.Avistamiento
import com.example.practicafinal.model.Publicacion
import com.example.practicafinal.util.fechaRelativa
import java.util.concurrent.Executors

class MascotasPerdidasActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rvPerdidas: RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mascotas_perdidas)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvPerdidas = findViewById(R.id.rv_perdidas)
        tvVacio = findViewById(R.id.tv_vacio)
        rvPerdidas.layoutManager = LinearLayoutManager(this)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
            .setOnClickListener {
                startActivity(
                    Intent(this, CrearPublicacionActivity::class.java).apply {
                        putExtra(CrearPublicacionActivity.EXTRA_TIPO, "Perdida")
                    }
                )
            }

        cargarPerdidas()
    }

    private fun cargarPerdidas() {
        executor.execute {
            val db = DatabaseHelper(this)
            val lista = db.obtenerPerdidas()
            val todosAvist = db.obtenerAvistamientos()
            val avistPorId = todosAvist.groupBy { it.publicacionId }

            runOnUiThread {
                tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                rvPerdidas.adapter = PerdidasAdapter(
                    lista, avistPorId,
                    onSighting = { p -> reportarAvistamiento(p) },
                    onVerAvistamientos = { p -> mostrarAvistamientos(p, avistPorId) }
                )
            }
        }
    }

    private fun reportarAvistamiento(publicacion: Publicacion) {
        startActivity(
            Intent(this, ReportarAvistamientoActivity::class.java).apply {
                putExtra(ReportarAvistamientoActivity.EXTRA_PUBLICACION_ID, publicacion.id)
                putExtra(ReportarAvistamientoActivity.EXTRA_NOMBRE, publicacion.nombre)
            }
        )
    }

    private fun mostrarAvistamientos(
        publicacion: Publicacion,
        avistPorId: Map<Long, List<Avistamiento>>
    ) {
        val avistamientos = avistPorId[publicacion.id] ?: emptyList()
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_avistamientos, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_avistamientos)
        val tvVacioAvist = dialogView.findViewById<TextView>(R.id.tv_vacio_avist)

        rv.layoutManager = LinearLayoutManager(this)
        if (avistamientos.isEmpty()) {
            tvVacioAvist.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvVacioAvist.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = object : RecyclerView.Adapter<AvistViewHolder>() {
                override fun onCreateViewHolder(p: ViewGroup, viewType: Int) = AvistViewHolder(
                    LayoutInflater.from(p.context).inflate(R.layout.item_avistamiento_row, p, false)
                )

                override fun onBindViewHolder(h: AvistViewHolder, pos: Int) {
                    val a = avistamientos[pos]
                    h.tvFecha.text = "👀 ${fechaRelativa(a.fecha)}"
                    h.tvDesc.text = a.descripcion.ifEmpty { "Reportado por la comunidad" }
                }

                override fun getItemCount() = avistamientos.size
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Avistamientos de ${publicacion.nombre}")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private class AvistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvDesc: TextView = view.findViewById(R.id.tv_desc)
    }

    override fun onResume() {
        super.onResume()
        cargarPerdidas()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
