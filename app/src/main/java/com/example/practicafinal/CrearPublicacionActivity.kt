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
import com.example.practicafinal.controlador.ControladorPublicaciones
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

class CrearPublicacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_TIPO = "extra_tipo"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var map: MapView
    private lateinit var pinSeleccion: Marker
    private var puntoSeleccionado: GeoPoint? = null
    private var tipoSeleccionado = "Perdida"
    private var especieSeleccionada = "Perro"
    private var fotoUri: String? = null

    private lateinit var etNombre: EditText
    private lateinit var etUltimoLugar: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var tvError: TextView
    private lateinit var chipPerdida: View
    private lateinit var chipEncontrada: View
    private lateinit var chipAdopcion: View
    private lateinit var chipPerro: View
    private lateinit var chipGato: View
    private lateinit var imgFoto: ImageView
    private lateinit var tvFotoHint: TextView

    private val pickerGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fotoUri = uri.toString()
            imgFoto.setImageBitmap(decodificarImagen(this, uri.toString(), 4))
            tvFotoHint.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = "MapaAlertas/1.0 (juanguz619@gmail.com)"

        setContentView(R.layout.activity_crear_publicacion)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        etNombre = findViewById(R.id.et_nombre)
        etUltimoLugar = findViewById(R.id.et_ultimo_lugar)
        etDescripcion = findViewById(R.id.et_descripcion)
        tvError = findViewById(R.id.tv_error)
        chipPerdida = findViewById(R.id.chip_perdida)
        chipEncontrada = findViewById(R.id.chip_encontrada)
        chipAdopcion = findViewById(R.id.chip_adopcion)
        chipPerro = findViewById(R.id.chip_especie_perro)
        chipGato = findViewById(R.id.chip_especie_gato)
        imgFoto = findViewById(R.id.img_foto)
        tvFotoHint = findViewById(R.id.tv_foto_hint)

        // Foto desde la galería
        findViewById<View>(R.id.contenedor_foto).setOnClickListener {
            pickerGaleria.launch("image/*")
        }

        chipPerdida.setOnClickListener { seleccionarTipo("Perdida") }
        chipEncontrada.setOnClickListener { seleccionarTipo("Encontrada") }
        chipAdopcion.setOnClickListener { seleccionarTipo("Adopcion") }
        chipPerro.setOnClickListener { seleccionarEspecie("Perro") }
        chipGato.setOnClickListener { seleccionarEspecie("Gato") }

        configurarMapa()

        // Tipo preseleccionado (ej. desde "Mascotas perdidas")
        val tipoInicial = intent.getStringExtra(EXTRA_TIPO)
        if (tipoInicial != null) seleccionarTipo(tipoInicial)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
            .setOnClickListener { publicar() }
    }

    private fun configurarMapa() {
        map = findViewById(R.id.mapa_crear)
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
        map.setMultiTouchControls(false)
        map.controller.setZoom(18.0)

        // Si vino de mantener presionado el mapa, usar ese punto
        val lat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)
        val puntoInicial = if (!lat.isNaN() && !lng.isNaN()) GeoPoint(lat, lng) else null

        // Centrar en la ubicación del usuario si es posible
        val centro = puntoInicial ?: obtenerUbicacionActual() ?: GeoPoint(-12.0464, -77.0428)
        map.controller.setCenter(centro)

        puntoSeleccionado = centro
        pinSeleccion = Marker(map).apply {
            position = centro
            icon = ContextCompat.getDrawable(this@CrearPublicacionActivity, R.drawable.ic_pin_rojo)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(pinSeleccion)

        // Tocar el mapa para elegir la ubicación
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

    private fun seleccionarTipo(tipo: String) {
        tipoSeleccionado = tipo
        listOf(
            chipPerdida to "Perdida",
            chipEncontrada to "Encontrada",
            chipAdopcion to "Adopcion"
        ).forEach { (chip, t) ->
            val activo = t == tipo
            chip.background = if (activo) {
                ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_chip)
            }
            (chip as TextView).setTextColor(
                if (activo) ContextCompat.getColor(this, android.R.color.black)
                else ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
    }

    private fun seleccionarEspecie(especie: String) {
        especieSeleccionada = especie
        listOf(chipPerro to "Perro", chipGato to "Gato").forEach { (chip, e) ->
            val activo = e == especie
            chip.background = if (activo) {
                ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_chip)
            }
            (chip as TextView).setTextColor(
                if (activo) ContextCompat.getColor(this, android.R.color.black)
                else ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
    }

    private fun publicar() {
        val nombre = etNombre.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val especie = especieSeleccionada
        val ultimoLugar = etUltimoLugar.text.toString().trim().ifEmpty { null }
        val punto = puntoSeleccionado

        val error = ControladorPublicaciones.validarPublicacion(nombre, descripcion)
        if (error != null) {
            mostrarError(error); return
        }
        if (punto == null) {
            mostrarError("Elige una ubicación en el mapa"); return
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar).isEnabled = false
        executor.execute {
            val usuarioId = SesionManager.obtenerUsuarioId(this)
            ControladorPublicaciones.publicar(
                this, usuarioId, tipoSeleccionado, nombre, descripcion,
                fotoUri, ultimoLugar, especie,
                punto.latitude, punto.longitude
            )
            runOnUiThread {
                Toast.makeText(this, "¡Publicación creada!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
