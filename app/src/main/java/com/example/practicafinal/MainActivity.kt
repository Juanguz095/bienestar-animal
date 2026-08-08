package com.example.practicafinal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.model.Avistamiento
import com.example.practicafinal.model.Publicacion
import com.example.practicafinal.session.SesionManager
import com.example.practicafinal.util.fechaRelativa
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val executor = Executors.newSingleThreadExecutor()
    private var userLocation: GeoPoint? = null
    private var circleOverlay: Polygon? = null
    private var circuloBusqueda: Polygon? = null

    private val marcadoresAlertas = mutableListOf<Marker>()
    private val marcadoresAvistamientos = mutableListOf<Marker>()

    private lateinit var panel: View
    private lateinit var panelEmoji: TextView
    private lateinit var panelTitulo: TextView
    private lateinit var panelTipo: TextView
    private lateinit var panelDesc: TextView
    private lateinit var panelUbicacion: TextView
    private lateinit var panelEstado: TextView

    private var alertaActual: Publicacion? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) centrarEnUsuario()
        else {
            Toast.makeText(this, "Permiso denegado, mostrando Lima", Toast.LENGTH_SHORT).show()
            centrarEnLima()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SesionManager.tieneSesion(this)) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = "MapaAlertas/1.0 (juanguz619@gmail.com)"

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
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
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)

        map.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = false
                override fun longPressHelper(p: GeoPoint): Boolean {
                    startActivity(
                        Intent(this@MainActivity, CrearPublicacionActivity::class.java).apply {
                            putExtra(CrearPublicacionActivity.EXTRA_LAT, p.latitude)
                            putExtra(CrearPublicacionActivity.EXTRA_LNG, p.longitude)
                        }
                    )
                    return true
                }
            })
        )

        panel = findViewById(R.id.panel_alerta)
        panelEmoji = findViewById(R.id.panel_emoji)
        panelTitulo = findViewById(R.id.panel_titulo)
        panelTipo = findViewById(R.id.panel_tipo)
        panelDesc = findViewById(R.id.panel_desc)
        panelUbicacion = findViewById(R.id.panel_ubicacion)
        panelEstado = findViewById(R.id.panel_estado)
        panel.findViewById<TextView>(R.id.panel_cerrar).setOnClickListener { ocultarPanel() }
        panel.findViewById<MaterialButton>(R.id.panel_whatsapp).setOnClickListener { enviarWhatsApp() }
        panel.findViewById<MaterialButton>(R.id.panel_compartir).setOnClickListener { compartirAlerta() }
        panel.findViewById<MaterialButton>(R.id.panel_ver).setOnClickListener { reportarAvistamiento() }
        panel.findViewById<MaterialButton>(R.id.panel_resolver).setOnClickListener { resolverAlerta() }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_menu
        ).setOnClickListener { startActivity(Intent(this, MenuOpcionesActivity::class.java)) }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_perfil
        ).setOnClickListener { startActivity(Intent(this, PerfilActivity::class.java)) }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_recenter
        ).setOnClickListener { centrarEnUsuario() }

        findViewById<TextView>(R.id.btn_leyenda).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📖 Leyenda del mapa")
                .setView(R.layout.dialog_leyenda)
                .setPositiveButton("Entendido", null)
                .show()
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_zoom_in
        ).setOnClickListener { map.controller.zoomIn() }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_zoom_out
        ).setOnClickListener { map.controller.zoomOut() }

        cargarAlertasDesdeBD()

        // Si vino de "Ver ubicación en el mapa", centrar en ese punto
        centrarEnPunto(intent)

        if (tienePermisoUbicacion()) centrarEnUsuario()
        else requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ─── Alertas desde la base de datos ────────────────────────────

    private fun cargarAlertasDesdeBD() {
        executor.execute {
            val db = DatabaseHelper(this)
            if (db.obtenerPublicaciones().isEmpty()) {
                db.insertarPublicacion(
                    null, "Perdida", "Max",
                    "Se perdió cerca del Centro de Lima", null, "Centro de Lima", "Perro",
                    -12.0464, -77.0428
                )
                db.insertarPublicacion(
                    null, "Perdida", "Michi",
                    "Se perdió en La Victoria", null, "La Victoria", "Gato",
                    -12.0670, -77.0337
                )
                db.insertarPublicacion(
                    null, "Encontrada", "Luna",
                    "Encontrado en Lince, busca dueño", null, "Lince", "Perro",
                    -12.0911, -77.0359
                )
                db.insertarPublicacion(
                    null, "Adopcion", "Pelusa",
                    "Gatita en busca de un hogar", null, null, "Gato",
                    -12.0850, -77.0050
                )
                db.insertarPublicacion(
                    null, "Adopcion", "Rocky",
                    "Perrito cariñoso en adopción", null, null, "Perro",
                    -12.0200, -77.0800
                )
            }
            val lista = db.obtenerPublicaciones()
            val avistamientos = db.obtenerAvistamientos()
            val nombres = lista.associate { it.id to it.nombre }
            runOnUiThread { renderAlertas(lista, avistamientos, nombres) }
        }
    }

    private fun renderAlertas(
        lista: List<Publicacion>,
        avistamientos: List<Avistamiento>,
        nombres: Map<Long, String>
    ) {
        marcadoresAlertas.forEach { map.overlays.remove(it) }
        marcadoresAlertas.clear()
        marcadoresAvistamientos.forEach { map.overlays.remove(it) }
        marcadoresAvistamientos.clear()

        lista.forEach { publicacion ->
            val punto = GeoPoint(publicacion.latitud, publicacion.longitud)
            val m = Marker(map)
            m.position = punto
            m.icon = ContextCompat.getDrawable(this, iconoPara(publicacion))
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            m.relatedObject = publicacion
            m.setOnMarkerClickListener { marker, _ ->
                val p = marker.relatedObject as? Publicacion
                if (p != null) {
                    map.controller.setZoom(17.0)
                    map.controller.animateTo(marker.position)
                    mostrarCirculoBusqueda(p)
                    mostrarPanel(p)
                }
                true
            }

            map.overlays.add(m)
            marcadoresAlertas.add(m)
        }

        // Pines amarillos: avistamientos reportados por la comunidad
        avistamientos.forEach { avistamiento ->
            val punto = GeoPoint(avistamiento.latitud, avistamiento.longitud)

            val m = Marker(map)
            m.position = punto
            m.icon = ContextCompat.getDrawable(this, R.drawable.ic_pin_amarillo)
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            m.relatedObject = avistamiento
            m.setOnMarkerClickListener { marker, _ ->
                val a = marker.relatedObject as? Avistamiento
                if (a != null) {
                    map.controller.setZoom(17.0)
                    map.controller.animateTo(marker.position)
                    mostrarPanelAvistamiento(a, nombres[a.publicacionId] ?: "la mascota")
                }
                true
            }
            map.overlays.add(m)
            marcadoresAvistamientos.add(m)
        }
        map.invalidate()
    }

    private fun iconoPara(publicacion: Publicacion): Int = when {
        publicacion.estado == "Resuelta" -> R.drawable.ic_pin_gris
        publicacion.tipo == "Perdida" -> R.drawable.ic_pin_rojo
        publicacion.tipo == "Encontrada" -> R.drawable.ic_pin_verde
        else -> R.drawable.ic_pin_naranja
    }

    private fun tipoLabel(tipo: String): String = when (tipo) {
        "Perdida" -> "Mascota perdida"
        "Encontrada" -> "Mascota encontrada"
        else -> "En adopción"
    }

    private fun tienePermisoUbicacion(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun obtenerUltimaUbicacion(): Location? {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun centrarEnUsuario() {
        val ubicacion = obtenerUltimaUbicacion()
        if (ubicacion != null) {
            centrarEnPunto(ubicacion)
        } else {
            // Pedir una ubicación fresca al GPS
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                lm.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    { loc -> centrarEnPunto(loc) },
                    mainLooper
                )
            } catch (_: SecurityException) {
                Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                centrarEnLima()
            }
        }
    }

    private fun centrarEnPunto(ubicacion: Location) {
        val punto = GeoPoint(ubicacion.latitude, ubicacion.longitude)
        userLocation = punto
        agregarMarcadorUsuario(punto)
        agregarCirculoCerca(punto)
        map.controller.setZoom(19.5)
        map.controller.setCenter(punto)
        map.invalidate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        centrarEnPunto(intent)
    }

    private fun centrarEnPunto(intent: Intent) {
        val lat = intent.getDoubleExtra("centrar_lat", Double.NaN)
        val lng = intent.getDoubleExtra("centrar_lng", Double.NaN)
        if (!lat.isNaN() && !lng.isNaN()) {
            map.controller.setZoom(19.5)
            map.controller.animateTo(GeoPoint(lat, lng))
            map.invalidate()
        }
    }

    private fun centrarEnLima() {
        map.controller.setCenter(GeoPoint(-12.0464, -77.0428))
        map.controller.setZoom(12.0)
        map.invalidate()
    }

    private fun agregarMarcadorUsuario(punto: GeoPoint) {
        val m = Marker(map)
        m.position = punto
        m.title = "Mi ubicación"
        m.icon = ContextCompat.getDrawable(this, R.drawable.ic_punto_azul)
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(m)
    }

    private fun agregarCirculoCerca(centro: GeoPoint) {
        circleOverlay?.let { map.overlays.remove(it) }
        circleOverlay = Polygon().apply {
            points = (0..48).map { centro.destinationPoint(2000.0, it * 360.0 / 48) }
            fillColor = 0x141E88E5; strokeColor = 0x401E88E5; strokeWidth = 2f
        }
        map.overlays.add(0, circleOverlay)
        map.invalidate()
    }

    private fun mostrarCirculoBusqueda(publicacion: Publicacion) {
        circuloBusqueda?.let { map.overlays.remove(it) }; circuloBusqueda = null
        if (publicacion.tipo == "Perdida" && publicacion.estado == "Activa") {
            val centro = GeoPoint(publicacion.latitud, publicacion.longitud)
            circuloBusqueda = Polygon().apply {
                points = (0..48).map { centro.destinationPoint(1500.0, it * 360.0 / 48) }
                fillColor = 0x14E53935; strokeColor = 0x40E53935; strokeWidth = 2f
            }
            map.overlays.add(circuloBusqueda)
            map.invalidate()
        }
    }

    private fun mostrarPanel(publicacion: Publicacion) {
        alertaActual = publicacion
        panelEmoji.text = when (publicacion.tipo) {
            "Perdida" -> "🐾"; "Encontrada" -> "🐶"; else -> "🐱"
        }
        panelTitulo.text = publicacion.nombre
        panelTipo.text = when {
            publicacion.tipo == "Perdida" && publicacion.ultimoLugar != null ->
                "${tipoLabel(publicacion.tipo)} · Última vez: ${publicacion.ultimoLugar}"

            else -> tipoLabel(publicacion.tipo)
        }
        panelDesc.text = publicacion.descripcion
        val punto = GeoPoint(publicacion.latitud, publicacion.longitud)
        val distKm = userLocation?.distanceToAsDouble(punto)?.div(1000.0)
        panelUbicacion.text = if (distKm != null) "📍 A %.1f km de ti".format(distKm)
        else "📍 Lima, Perú"
        val resuelta = publicacion.estado == "Resuelta"
        panelEstado.text = if (resuelta) "● Resuelta · ${fechaRelativa(publicacion.fechaCreacion)}"
        else "● Activa · ${fechaRelativa(publicacion.fechaCreacion)}"
        panelEstado.setTextColor(
            ContextCompat.getColor(this, if (resuelta) android.R.color.darker_gray else R.color.verde_estado)
        )
        panel.findViewById<MaterialButton>(R.id.panel_ver).isEnabled = !resuelta
        panel.findViewById<MaterialButton>(R.id.panel_resolver).isEnabled = !resuelta
        panel.visibility = View.VISIBLE; panel.alpha = 0f
        panel.post {
            panel.translationY = panel.height.toFloat()
            panel.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun mostrarPanelAvistamiento(avistamiento: Avistamiento, nombre: String) {
        alertaActual = null
        panelEmoji.text = "👀"; panelTitulo.text = "Avistamiento de $nombre"
        panelTipo.text = "Reportado por la comunidad"
        panelDesc.text = avistamiento.descripcion.ifEmpty { "Alguien reportó haber visto a esta mascota aquí." }
        val punto = GeoPoint(avistamiento.latitud, avistamiento.longitud)
        val distKm = userLocation?.distanceToAsDouble(punto)?.div(1000.0)
        panelUbicacion.text = if (distKm != null) "📍 A %.1f km de ti".format(distKm) else "📍 Lima, Perú"
        panelEstado.text = "● Avistamiento · ${fechaRelativa(avistamiento.fecha)}"
        panelEstado.setTextColor(ContextCompat.getColor(this, R.color.ambar_estado))
        panel.findViewById<MaterialButton>(R.id.panel_ver).isEnabled = false
        panel.findViewById<MaterialButton>(R.id.panel_resolver).isEnabled = false
        panel.visibility = View.VISIBLE; panel.alpha = 0f
        panel.post {
            panel.translationY = panel.height.toFloat()
            panel.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun ocultarPanel() {
        panel.animate().translationY(panel.height.toFloat()).alpha(0f)
            .setDuration(200).withEndAction { panel.visibility = View.GONE }.start()
        alertaActual = null
        circuloBusqueda?.let { map.overlays.remove(it) }; circuloBusqueda = null
        map.invalidate()
    }

    private fun reportarAvistamiento() {
        val publicacion = alertaActual ?: return
        val punto = userLocation ?: obtenerUltimaUbicacion()?.let { GeoPoint(it.latitude, it.longitude) }
        if (punto == null) {
            Toast.makeText(this, "No tenemos tu ubicación", Toast.LENGTH_SHORT).show(); return
        }
        executor.execute {
            DatabaseHelper(this).actualizarAvistamiento(publicacion.id, punto.latitude, punto.longitude)
            runOnUiThread {
                Toast.makeText(this, "¡Gracias! Se actualizó el último avistamiento", Toast.LENGTH_SHORT).show()
                ocultarPanel(); cargarAlertasDesdeBD()
            }
        }
    }

    private fun resolverAlerta() {
        val publicacion = alertaActual ?: return
        executor.execute {
            DatabaseHelper(this).marcarResuelta(publicacion.id)
            runOnUiThread {
                Toast.makeText(this, "¡Alerta resuelta!", Toast.LENGTH_SHORT).show()
                ocultarPanel(); cargarAlertasDesdeBD()
            }
        }
    }

    private fun textoAlerta(publicacion: Publicacion): String =
        "${publicacion.nombre} · ${tipoLabel(publicacion.tipo)}\n" +
                "${publicacion.descripcion}\n" +
                "Estado: ${publicacion.estado} · ${fechaRelativa(publicacion.fechaCreacion)}\n" +
                "📍 https://maps.google.com/?q=${publicacion.latitud},${publicacion.longitud}"

    private fun enviarWhatsApp() {
        val publicacion = alertaActual ?: return
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/?text=${Uri.encode(textoAlerta(publicacion))}")
                )
            )
        } catch (_: Exception) {
            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirAlerta() {
        val publicacion = alertaActual ?: return
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(
                    Intent.EXTRA_TEXT,
                    textoAlerta(publicacion)
                )
                },
                "Compartir alerta"
            )
        )
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        cargarAlertasDesdeBD()
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
