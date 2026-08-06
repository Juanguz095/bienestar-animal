package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adapters.PerdidasAdapter
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.model.Publicacion
import java.util.concurrent.Executors

class MascotasPerdidasActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rvPerdidas: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mascotas_perdidas)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvPerdidas = findViewById(R.id.rv_perdidas)
        tvVacio = findViewById(R.id.tv_vacio)
        rvPerdidas.layoutManager = LinearLayoutManager(this)

        // Publicar desde esta pantalla (tipo Perdida preseleccionado)
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
            val lista = DatabaseHelper(this).obtenerPerdidas()
            runOnUiThread {
                tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                rvPerdidas.adapter = PerdidasAdapter(lista) { publicacion ->
                    reportarAvistamiento(publicacion)
                }
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

    override fun onResume() {
        super.onResume()
        cargarPerdidas()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
