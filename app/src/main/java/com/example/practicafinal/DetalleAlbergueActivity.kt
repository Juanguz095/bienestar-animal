package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.util.decodificarImagen
import java.util.concurrent.Executors

class DetalleAlbergueActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_albergue)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val id = intent.getLongExtra("albergue_id", -1L)
        if (id == -1L) {
            finish(); return
        }

        exec.execute {
            val a = DatabaseHelper(this).obtenerAlbergues().find { it.id == id }
            runOnUiThread {
                if (a == null) {
                    finish(); return@runOnUiThread
                }
                val foto = a.foto?.let { decodificarImagen(this, it, 2) }
                val img = findViewById<android.widget.ImageView>(R.id.img_foto)
                val pl = findViewById<TextView>(R.id.tv_placeholder)
                if (foto != null) {
                    img.setImageBitmap(foto); pl.visibility = View.GONE
                } else pl.visibility = View.VISIBLE
                findViewById<TextView>(R.id.tv_nombre).text = a.nombre
                findViewById<TextView>(R.id.tv_descripcion).text = a.descripcion
                findViewById<TextView>(R.id.tv_direccion).text = a.direccion

                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_whatsapp)
                    .setOnClickListener {
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/51${a.telefono}?text=Hola ${a.nombre}, quisiera información")
                                )
                            )
                        } catch (_: Exception) {
                            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
