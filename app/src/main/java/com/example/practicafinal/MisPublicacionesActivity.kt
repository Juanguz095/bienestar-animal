package com.example.practicafinal

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adapters.AdaptadorPublicacionesPropias
import com.example.practicafinal.controller.ControladorPublicaciones
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class MisPublicacionesActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rv: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_publicaciones)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rv = findViewById(R.id.rv_publicaciones)
        tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)

        cargarPublicaciones()
    }

    private fun cargarPublicaciones() {
        val usuarioId = SesionManager.obtenerUsuarioId(this) ?: return
        executor.execute {
            val todas = ControladorPublicaciones.obtenerPublicaciones(this)
            val propias = todas.filter { it.usuarioId == usuarioId }
            runOnUiThread {
                tvVacio.visibility = if (propias.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = AdaptadorPublicacionesPropias(propias) { publicacion ->
                    resolverPublicacion(publicacion)
                }
            }
        }
    }

    private fun resolverPublicacion(publicacion: com.example.practicafinal.model.Publicacion) {
        executor.execute {
            ControladorPublicaciones.resolver(this, publicacion.id)
            runOnUiThread {
                Toast.makeText(this, "Publicación resuelta", Toast.LENGTH_SHORT).show()
                cargarPublicaciones()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
