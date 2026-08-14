package com.example.practicafinal

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adaptadores.AdaptadorPublicacionesPropias
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.session.FavoritosManager
import java.util.concurrent.Executors

class MisFavoritosActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_favoritos)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_favoritos); tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)
        cargar()
    }

    private fun cargar() {
        exec.execute {
            val idsFav = FavoritosManager.obtenerIds(this)
            val pubs = ControladorPublicaciones.obtenerPublicaciones(this).filter { idsFav.contains(it.id.toString()) }
            runOnUiThread {
                tvVacio.visibility = if (pubs.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = AdaptadorPublicacionesPropias(
                    pubs, onResolver = { publicacion ->
                        FavoritosManager.toggle(this, publicacion.id)
                        Toast.makeText(this, "Quitado de favoritos", Toast.LENGTH_SHORT).show()
                        cargar()
                    },
                    mostrarBotonResolver = false
                )
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
