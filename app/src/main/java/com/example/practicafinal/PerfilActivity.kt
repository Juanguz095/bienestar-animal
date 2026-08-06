package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class PerfilActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        // Cargar los datos del usuario real
        val tvNombre = findViewById<TextView>(R.id.tv_nombre)
        val tvCorreo = findViewById<TextView>(R.id.tv_correo)
        val tvTipo = findViewById<TextView>(R.id.tv_tipo)

        val usuarioId = SesionManager.obtenerUsuarioId(this)
        if (usuarioId != null) {
            executor.execute {
                val usuario = DatabaseHelper(this).obtenerPorId(usuarioId)
                runOnUiThread {
                    if (usuario != null) {
                        tvNombre.text = usuario.nombre
                        tvCorreo.text = usuario.correo
                        tvTipo.text = usuario.tipo
                    }
                }
            }
        }

        // Cerrar sesión: limpia la sesión y vuelve al Login
        findViewById<View>(R.id.row_cerrar_sesion).setOnClickListener {
            SesionManager.cerrarSesion(this)
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }

        // Opciones aún sin pantalla propia
        val pendientes = listOf(
            R.id.row_publicaciones,
            R.id.row_denuncias,
            R.id.row_favoritos,
            R.id.row_configuracion
        )
        pendientes.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
