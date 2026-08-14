package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.controlador.ControladorAlbergues
import com.example.practicafinal.util.decodificarImagen
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.concurrent.Executors

class DetalleAlbergueActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "MapaAlertas/1.0"
        setContentView(R.layout.activity_detalle_albergue)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val id = intent.getLongExtra("albergue_id", -1L)
        if (id == -1L) {
            finish(); return
        }

        exec.execute {
            val a = ControladorAlbergues.obtenerAlbergues(this).find { it.id == id }
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

                
                val map = findViewById<MapView>(R.id.mapa_albergue)
                map.setUseDataConnection(true)
                map.setTileSource(
                    XYTileSource(
                        "OpenStreetMap",
                        0,
                        19,
                        256,
                        ".png",
                        arrayOf("https://tile.openstreetmap.org/")
                    )
                )
                map.setMultiTouchControls(false); map.setBuiltInZoomControls(false)
                val centro = GeoPoint(a.latitud, a.longitud)
                map.controller.setZoom(15.0); map.controller.setCenter(centro)
                map.overlays.add(Marker(map).apply {
                    position = centro; icon =
                    ContextCompat.getDrawable(this@DetalleAlbergueActivity, R.drawable.ic_pin_albergue); setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
                )
                })
                map.invalidate(); map.onResume()

                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_whatsapp).setOnClickListener {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/51${a.telefono}?text=Hola ${a.nombre}")
                            )
                        )
                    } catch (_: Exception) {
                        Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
                    }
                }

                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_llamar).setOnClickListener {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${a.telefono}")))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
