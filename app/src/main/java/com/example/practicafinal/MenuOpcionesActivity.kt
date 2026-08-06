package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MenuOpcionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_opciones)

        // Cerrar el menú
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cerrar)
            .setOnClickListener { finish() }

        // Navegación a pantallas existentes (el menú queda en la pila para poder volver)
        findViewById<View>(R.id.item_adopciones).setOnClickListener {
            startActivity(Intent(this, AdopcionesActivity::class.java))
        }

        findViewById<View>(R.id.item_perfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        findViewById<View>(R.id.item_perdidas).setOnClickListener {
            startActivity(Intent(this, MascotasPerdidasActivity::class.java))
        }

        // Las demás opciones aún no tienen pantalla
        val pendientes = listOf(
            R.id.item_albergues,
            R.id.item_denuncias
        )
        pendientes.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
