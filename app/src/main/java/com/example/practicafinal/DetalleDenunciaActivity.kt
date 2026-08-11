package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.util.decodificarImagen
import com.example.practicafinal.util.fechaRelativa
import java.util.concurrent.Executors

class DetalleDenunciaActivity : AppCompatActivity() {
    private val exec = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_denuncia)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        val id = intent.getLongExtra("denuncia_id", -1L)
        if (id == -1L) {
            finish(); return
        }
        exec.execute {
            val d = DatabaseHelper(this).obtenerDenuncias().find { it.id == id }
            runOnUiThread {
                if (d == null) {
                    finish(); return@runOnUiThread
                }
                val icono = when (d.motivo) {
                    "Maltrato" -> "🚨"; "Abandono" -> "🏚️"; else -> "💰"
                }
                val foto = d.foto?.let { decodificarImagen(this, it, 2) }
                val img = findViewById<android.widget.ImageView>(R.id.img_foto)
                val pl = findViewById<TextView>(R.id.tv_placeholder)
                if (foto != null) {
                    img.setImageBitmap(foto); pl.visibility = View.GONE
                } else {
                    pl.visibility = View.VISIBLE; pl.text = icono
                }
                findViewById<TextView>(R.id.tv_motivo).text = "$icono ${d.motivo}"
                findViewById<TextView>(R.id.tv_descripcion).text = d.descripcion
                findViewById<TextView>(R.id.tv_fecha).text = "Reportado ${fechaRelativa(d.fecha)}"
                findViewById<TextView>(R.id.tv_ubicacion).setOnClickListener {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:${d.latitud},${d.longitud}?q=${d.latitud},${d.longitud}")
                        )
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
