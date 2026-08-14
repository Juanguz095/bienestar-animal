package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.session.FavoritosManager
import com.example.practicafinal.util.decodificarImagen
import java.util.concurrent.Executors

class DetalleAdopcionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PUBLICACION_ID = "detalle_publicacion_id"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var publicacionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_adopcion)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        publicacionId = intent.getLongExtra(EXTRA_PUBLICACION_ID, -1L)
        if (publicacionId == -1L) {
            finish(); return
        }

        cargarPublicacion()
    }

    private fun cargarPublicacion() {
        executor.execute {
            val publicacion = ControladorPublicaciones.obtenerPorId(this, publicacionId)
            runOnUiThread {
                if (publicacion == null) {
                    finish(); return@runOnUiThread
                }
                mostrar(publicacion)
            }
        }
    }

    private fun mostrar(p: Publicacion) {
        
        val imgFoto = findViewById<android.widget.ImageView>(R.id.img_foto)
        val tvPlaceholder = findViewById<TextView>(R.id.tv_placeholder)
        val foto = p.foto?.let { decodificarImagen(this, it, 2) }
        if (foto != null) {
            imgFoto.setImageBitmap(foto)
            tvPlaceholder.visibility = View.GONE
        } else {
            tvPlaceholder.visibility = View.VISIBLE
        }

        
        val tvEspecie = findViewById<TextView>(R.id.tv_especie_detalle)
        if (!p.especie.isNullOrEmpty()) {
            tvEspecie.text = p.especie
            tvEspecie.visibility = View.VISIBLE
        } else {
            tvEspecie.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tv_nombre).text = p.nombre
        findViewById<TextView>(R.id.tv_descripcion).text = p.descripcion

        
        findViewById<TextView>(R.id.tv_ubicacion).apply {
            text = "📍 Ver ubicación en el mapa"
            setOnClickListener {
                val intent = Intent(this@DetalleAdopcionActivity, MainActivity::class.java).apply {
                    putExtra("centrar_lat", p.latitud)
                    putExtra("centrar_lng", p.longitud)
                    putExtra("mostrar_alerta", publicacionId)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }

        
        val btnFav = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_favorito)
        actualizarBotonFavorito(btnFav)
        btnFav.setOnClickListener {
            FavoritosManager.toggle(this, publicacionId)
            actualizarBotonFavorito(btnFav)
        }

        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_whatsapp)
            .setOnClickListener { contactarWhatsApp(p) }
    }

    private fun actualizarBotonFavorito(btn: com.google.android.material.button.MaterialButton) {
        val esFav = FavoritosManager.esFavorito(this, publicacionId)
        btn.text = if (esFav) "❤  Quitar de favoritos" else "🤍  Agregar a favoritos"
    }

    private fun contactarWhatsApp(p: Publicacion) {
        val texto =
            "${p.nombre} · En adopción\n${p.descripcion}\n📍 https://maps.google.com/?q=${p.latitud},${p.longitud}"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(texto)}")))
        } catch (_: Exception) {
            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
