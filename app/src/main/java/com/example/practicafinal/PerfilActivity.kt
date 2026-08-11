package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.session.FavoritosManager
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class PerfilActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val tvNombre = findViewById<TextView>(R.id.tv_nombre)
        val tvCorreo = findViewById<TextView>(R.id.tv_correo)
        val tvTipo = findViewById<TextView>(R.id.tv_tipo)
        val tvPublicaciones = findViewById<TextView>(R.id.tv_publicaciones)
        val tvDenuncias = findViewById<TextView>(R.id.tv_denuncias)
        val tvFavoritos = findViewById<TextView>(R.id.tv_favoritos)

        val usuarioId = SesionManager.obtenerUsuarioId(this)

        // Cargar datos reales
        executor.execute {
            val usuario = usuarioId?.let { DatabaseHelper(this).obtenerPorId(it) }
            val pubCount = if (usuarioId != null)
                DatabaseHelper(this).obtenerPublicaciones().count { it.usuarioId == usuarioId } else 0
            val favCount = FavoritosManager.contar(this)
            runOnUiThread {
                usuario?.let {
                    tvNombre.text = it.nombre; tvCorreo.text = it.correo; tvTipo.text = it.tipo
                }
                tvPublicaciones.text = pubCount.toString()
                tvDenuncias.text = "0"
                tvFavoritos.text = favCount.toString()
            }
        }

        // Cerrar sesión
        findViewById<View>(R.id.row_cerrar_sesion).setOnClickListener {
            SesionManager.cerrarSesion(this)
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        // Mis publicaciones
        findViewById<View>(R.id.row_publicaciones).setOnClickListener {
            startActivity(Intent(this, MisPublicacionesActivity::class.java))
        }

        // Mis favoritos
        findViewById<View>(R.id.row_favoritos).setOnClickListener {
            startActivity(Intent(this, MisFavoritosActivity::class.java))
        }

        // Pendientes
        val pendientes = listOf(R.id.row_denuncias, R.id.row_configuracion)
        pendientes.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // refrescar contadores al volver
        executor.execute {
            val usuarioId = SesionManager.obtenerUsuarioId(this)
            val pubCount = if (usuarioId != null)
                DatabaseHelper(this).obtenerPublicaciones().count { it.usuarioId == usuarioId } else 0
            val favCount = FavoritosManager.contar(this)
            runOnUiThread {
                findViewById<TextView>(R.id.tv_publicaciones).text = pubCount.toString()
                findViewById<TextView>(R.id.tv_favoritos).text = favCount.toString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); executor.shutdown()
    }
}
