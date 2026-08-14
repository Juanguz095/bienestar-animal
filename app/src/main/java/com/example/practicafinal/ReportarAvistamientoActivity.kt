package com.example.practicafinal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.controller.ControladorPublicaciones
import com.example.practicafinal.session.SesionManager
import com.example.practicafinal.util.decodificarImagen
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.concurrent.Executors

class ReportarAvistamientoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PUBLICACION_ID = "extra_publicacion_id"
        const val EXTRA_NOMBRE = "extra_nombre"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var map: MapView
    private lateinit var pinSeleccion: Marker
    private var puntoSeleccionado: GeoPoint? = null
    private lateinit var etDescripcion: EditText
    private var fotoAvistUri: String? = null
    private lateinit var imgFotoAvist: ImageView
    private lateinit var tvFotoHintAvist: TextView

    private val pickerGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fotoAvistUri = uri.toString()
            imgFotoAvist.setImageBitmap(decodificarImagen(this, uri.toString(), 4))
            tvFotoHintAvist.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = "MapaAlertas/1.0 (juanguz619@gmail.com)"

        setContentView(R.layout.activity_reportar_avistamiento)

        setContentView(R.layout.activity_reportar_avistamiento)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: "la mascota"
        findViewById<TextView>(R.id.tv_mascota).text = "👀 Avistamiento de $nombre"

        etDescripcion = findViewById(R.id.et_descripcion)
        imgFotoAvist = findViewById(R.id.img_foto_avist)
        tvFotoHintAvist = findViewById(R.id.tv_foto_hint_avist)

        findViewById<View>(R.id.contenedor_foto_avist).setOnClickListener {
            pickerGaleria.launch("image/*")
        }

        configurarMapa()

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_reportar)
            .setOnClickListener { reportar() }
    }

    private fun configurarMapa() {
        map = findViewById(R.id.mapa_avistamiento)
        map.setUseDataConnection(true)
        map.setTileSource(
            XYTileSource(
                "OpenStreetMap", 0, 19, 256, ".png",
                arrayOf(
                    "https://tile.openstreetmap.org/",
                    "https://a.tile.openstreetmap.org/",
                    "https://b.tile.openstreetmap.org/",
                    "https://c.tile.openstreetmap.org/"
                )
            )
        )
        map.setBuiltInZoomControls(false)
        map.controller.setZoom(15.0)

        val centro = obtenerUbicacionActual() ?: GeoPoint(-12.0464, -77.0428)
        map.controller.setCenter(centro)

        puntoSeleccionado = centro
        pinSeleccion = Marker(map).apply {
            position = centro
            icon = ContextCompat.getDrawable(this@ReportarAvistamientoActivity, R.drawable.ic_pin_amarillo)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(pinSeleccion)

        map.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    puntoSeleccionado = p
                    pinSeleccion.position = p
                    map.invalidate()
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            })
        )
    }

    private fun obtenerUbicacionActual(): GeoPoint? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            loc?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun reportar() {
        val publicacionId = intent.getLongExtra(EXTRA_PUBLICACION_ID, -1L)
        val punto = puntoSeleccionado
        if (publicacionId == -1L || punto == null) {
            Toast.makeText(this, "No se pudo reportar", Toast.LENGTH_SHORT).show()
            return
        }
        val descripcion = etDescripcion.text.toString().trim()

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_reportar).isEnabled = false
        executor.execute {
            val usuarioId = SesionManager.obtenerUsuarioId(this)
            ControladorPublicaciones.reportarAvistamiento(
                this, publicacionId, usuarioId, punto.latitude, punto.longitude, descripcion, fotoAvistUri
            )
            runOnUiThread {
                Toast.makeText(this, "¡Avistamiento reportado! Gracias 🙌", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
