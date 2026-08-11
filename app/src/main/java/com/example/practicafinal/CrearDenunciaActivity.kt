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
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.util.decodificarImagen
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.concurrent.Executors

class CrearDenunciaActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var map: MapView
    private lateinit var pin: Marker
    private var punto: GeoPoint? = null
    private var motivo = "Maltrato"
    private var fotoUri: String? = null

    private lateinit var etDesc: EditText;
    private lateinit var tvError: TextView
    private lateinit var imgFoto: ImageView;
    private lateinit var tvHint: TextView

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoUri = uri.toString(); imgFoto.setImageBitmap(
                decodificarImagen(
                    this,
                    uri.toString(),
                    4
                )
            ); tvHint.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "MapaAlertas/1.0"
        setContentView(R.layout.activity_crear_denuncia)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        etDesc = findViewById(R.id.et_descripcion); tvError = findViewById(R.id.tv_error)
        imgFoto = findViewById(R.id.img_foto); tvHint = findViewById(R.id.tv_foto_hint)
        findViewById<View>(R.id.contenedor_foto).setOnClickListener { picker.launch("image/*") }

        val chips = listOf(
            R.id.chip_maltrato to "Maltrato",
            R.id.chip_abandono to "Abandono",
            R.id.chip_venta to "Venta ilegal"
        )
        chips.forEach { (id, m) -> findViewById<TextView>(id).setOnClickListener { seleccionarMotivo(m, chips) } }

        configurarMapa()
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_enviar).setOnClickListener { enviar() }
    }

    private fun seleccionarMotivo(m: String, chips: List<Pair<Int, String>>) {
        motivo = m
        chips.forEach { (id, cm) ->
            val tv = findViewById<TextView>(id)
            val activo = cm == m
            tv.background = if (activo) ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            else ContextCompat.getDrawable(this, R.drawable.bg_chip)
            tv.setTextColor(
                if (activo) ContextCompat.getColor(this, android.R.color.black)
                else ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
    }

    private fun configurarMapa() {
        map = findViewById(R.id.mapa_denuncia); map.setUseDataConnection(true)
        map.setTileSource(XYTileSource("OpenStreetMap", 0, 19, 256, ".png", arrayOf("https://tile.openstreetmap.org/")))
        map.setMultiTouchControls(true); map.setBuiltInZoomControls(false); map.controller.setZoom(15.0)
        val centro = obtenerUbicacion() ?: GeoPoint(-12.0464, -77.0428)
        map.controller.setCenter(centro); punto = centro
        pin = Marker(map).apply {
            position = centro; icon = ContextCompat.getDrawable(
            this@CrearDenunciaActivity,
            R.drawable.ic_pin_rojo
        ); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(pin)
        map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint) =
                true.apply { punto = p; pin.position = p; map.invalidate() }

            override fun longPressHelper(p: GeoPoint) = false
        }))
    }

    private fun obtenerUbicacion(): GeoPoint? {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null
        return try {
            (getSystemService(Context.LOCATION_SERVICE) as LocationManager).let { lm ->
                lm.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                ) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun enviar() {
        val desc = etDesc.text.toString().trim()
        val pt = punto
        if (desc.isEmpty()) {
            tvError.text = "Escribe una descripción"; tvError.visibility = View.VISIBLE; return
        }
        if (pt == null) {
            tvError.text = "Elige la ubicación"; tvError.visibility = View.VISIBLE; return
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_enviar).isEnabled = false
        exec.execute {
            DatabaseHelper(this).insertarDenuncia(motivo, desc, fotoUri, pt.latitude, pt.longitude)
            runOnUiThread { Toast.makeText(this, "Denuncia enviada", Toast.LENGTH_SHORT).show(); finish() }
        }
    }

    override fun onResume() {
        super.onResume(); map.onResume()
    }

    override fun onPause() {
        super.onPause(); map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
