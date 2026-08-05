package com.example.practicafinal

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        // Opciones aún sin pantalla propia
        val pendientes = listOf(
            R.id.row_publicaciones,
            R.id.row_denuncias,
            R.id.row_favoritos,
            R.id.row_configuracion,
            R.id.row_cerrar_sesion
        )
        pendientes.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
