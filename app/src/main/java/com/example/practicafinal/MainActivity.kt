package com.example.practicafinal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import com.example.practicafinal.model.Alerta
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val puntosAlertas = mutableListOf<GeoPoint>()
    private var userLocation: GeoPoint? = null
    private var circleOverlay: Polygon? = null

    // Panel inferior
    private lateinit var panel: View
    private lateinit var panelEmoji: TextView
    private lateinit var panelTitulo: TextView
    private lateinit var panelDesc: TextView
    private lateinit var panelUbicacion: TextView

    // Filtros del mapa
    private val filtrosActivos = mutableSetOf("Perdida", "Encontrada", "Albergue")
    private val marcadoresPorTipo = mutableMapOf<String, MutableList<Marker>>()

    // Chips de filtro con sus vistas
    private lateinit var chipPerdidas: View
    private lateinit var chipEncontradas: View
    private lateinit var chipAlbergues: View
    private lateinit var dotPerdidas: View
    private lateinit var dotEncontradas: View
    private lateinit var dotAlbergues: View
    private lateinit var bgChipOff: Drawable
    private lateinit var bgDotGris: Drawable

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
                arrayOf("https://tile.openstreetmap.org/")
            )
        )
        map.setMultiTouchControls(true)

        // Panel inferior
        panel = findViewById(R.id.panel_alerta)
        panelEmoji = findViewById(R.id.panel_emoji)
        panelTitulo = findViewById(R.id.panel_titulo)
        panelDesc = findViewById(R.id.panel_desc)
        panelUbicacion = findViewById(R.id.panel_ubicacion)
        panel.findViewById<TextView>(R.id.panel_cerrar).setOnClickListener { ocultarPanel() }
        panel.findViewById<MaterialButton>(R.id.panel_whatsapp).setOnClickListener { enviarWhatsApp() }
        panel.findViewById<MaterialButton>(R.id.panel_compartir).setOnClickListener { compartirAlerta() }

        // Chips de filtro
        chipPerdidas = findViewById(R.id.chip_mapa_perdidas)
        chipEncontradas = findViewById(R.id.chip_mapa_encontradas)
        chipAlbergues = findViewById(R.id.chip_mapa_albergues)
        dotPerdidas = findViewById(R.id.dot_mapa_perdidas)
        dotEncontradas = findViewById(R.id.dot_mapa_encontradas)
        dotAlbergues = findViewById(R.id.dot_mapa_albergues)
        bgChipOff = ContextCompat.getDrawable(this, R.drawable.bg_chip)!!
        bgDotGris = ContextCompat.getDrawable(this, R.drawable.bg_dot_gris)!!

        chipPerdidas.setOnClickListener { alternarFiltro("Perdida") }
        chipEncontradas.setOnClickListener { alternarFiltro("Encontrada") }
        chipAlbergues.setOnClickListener { alternarFiltro("Albergue") }

        // Botones flotantes
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_menu
        ).setOnClickListener { startActivity(Intent(this, MenuOpcionesActivity::class.java)) }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_perfil
        ).setOnClickListener { startActivity(Intent(this, PerfilActivity::class.java)) }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fab_recenter
        ).setOnClickListener { centrarEnUsuario() }

        agregarAlertas()

        if (tienePermisoUbicacion()) centrarEnUsuario()
        else requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ─── Permisos y ubicación ──────────────────────────────────────

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
            val punto = GeoPoint(ubicacion.latitude, ubicacion.longitude)
            userLocation = punto
            agregarMarcadorUsuario(punto)
            agregarCirculoCerca(punto)
            map.controller.setZoom(16.0)
            map.controller.setCenter(punto)
        } else {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            centrarEnLima()
        }
    }

    private fun centrarEnLima() {
        map.controller.setCenter(GeoPoint(-12.0464, -77.0428))
        map.controller.setZoom(12.0)
        map.invalidate()
    }

    // ─── Marcadores ─────────────────────────────────────────────────

    private fun agregarMarcadorUsuario(punto: GeoPoint) {
        val m = Marker(map)
        m.position = punto
        m.title = "Mi ubicación"
        m.icon = ContextCompat.getDrawable(this, R.drawable.ic_punto_azul)
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(m)
    }

    private fun agregarAlertas() {
        val alertas = listOf(
            Alerta("Max · Perro perdido", "Perdida", "🐶 Se perdió cerca del Centro de Lima", -12.0464, -77.0428),
            Alerta("Michi · Gato perdido", "Perdida", "🐱 Se perdió en La Victoria", -12.0670, -77.0337),
            Alerta("Luna · Perro encontrado", "Encontrada", "🐶 Encontrado en Lince, busca dueño", -12.0911, -77.0359),
            Alerta("Albergue Patitas", "Albergue", "🏠 Refugio de mascotas · Abierto hoy", -12.0850, -77.0050),
            Alerta("Refugio Huellitas", "Albergue", "🏠 Refugio de mascotas · Abierto hoy", -12.0200, -77.0800)
        )

        alertas.forEach { alerta ->
            val punto = GeoPoint(alerta.latitud, alerta.longitud)
            puntosAlertas.add(punto)

            val m = Marker(map)
            m.position = punto
            m.icon = ContextCompat.getDrawable(this, iconoPara(alerta.tipo))
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            m.relatedObject = alerta
            m.setOnMarkerClickListener { marker, _ ->
                val a = marker.relatedObject as? Alerta
                if (a != null) {
                    // Zoom animado hacia el pin
                    map.controller.setZoom(15.0)
                    map.controller.animateTo(marker.position)
                    mostrarPanel(a)
                }
                true
            }

            map.overlays.add(m)
            marcadoresPorTipo.getOrPut(alerta.tipo) { mutableListOf() }.add(m)
        }
        map.invalidate()
    }

    private fun iconoPara(tipo: String): Int = when (tipo) {
        "Perdida" -> R.drawable.ic_pin_rojo
        "Encontrada" -> R.drawable.ic_pin_verde
        else -> R.drawable.ic_pin_morado
    }

    // ─── Filtros del mapa ───────────────────────────────────────────

    private fun alternarFiltro(tipo: String) {
        if (tipo in filtrosActivos) filtrosActivos.remove(tipo)
        else filtrosActivos.add(tipo)

        actualizarChip(tipo, tipo in filtrosActivos)
        aplicarFiltros()
    }

    private fun actualizarChip(tipo: String, activo: Boolean) {
        val (chip, dot, bgOn) = when (tipo) {
            "Perdida" -> Triple(
                chipPerdidas, dotPerdidas,
                ContextCompat.getDrawable(this, R.drawable.bg_chip_filtro_on_perdida)
            )

            "Encontrada" -> Triple(
                chipEncontradas, dotEncontradas,
                ContextCompat.getDrawable(this, R.drawable.bg_chip_filtro_on_encontrada)
            )

            else -> Triple(
                chipAlbergues, dotAlbergues,
                ContextCompat.getDrawable(this, R.drawable.bg_chip_filtro_on_albergue)
            )
        }
        chip.background = if (activo) bgOn else bgChipOff
        dot.background = if (activo) ContextCompat.getDrawable(
            this,
            when (tipo) {
                "Perdida" -> R.drawable.bg_dot_rojo
                "Encontrada" -> R.drawable.bg_dot_verde
                else -> R.drawable.bg_dot_morado
            }
        ) else bgDotGris
    }

    private fun aplicarFiltros() {
        marcadoresPorTipo.forEach { (tipo, marcadores) ->
            marcadores.forEach { it.isEnabled = tipo in filtrosActivos }
        }
        map.invalidate()
    }

    // ─── Círculo de radio ──────────────────────────────────────────

    private fun agregarCirculoCerca(centro: GeoPoint) {
        circleOverlay?.let { map.overlays.remove(it) }
        val puntos = (0..48).map { i ->
            centro.destinationPoint(2000.0, i * 360.0 / 48)
        }
        circleOverlay = Polygon().apply {
            points = puntos
            fillColor = 0x141E88E5
            strokeColor = 0x401E88E5
            strokeWidth = 2f
        }
        map.overlays.add(0, circleOverlay)
        map.invalidate()
    }

    // ─── Panel inferior ─────────────────────────────────────────────

    private var alertaActual: Alerta? = null

    private fun mostrarPanel(alerta: Alerta) {
        alertaActual = alerta
        panelEmoji.text = when (alerta.tipo) {
            "Perdida" -> "🐾"
            "Encontrada" -> "🐶"
            else -> "🏠"
        }
        panelTitulo.text = alerta.titulo
        panelDesc.text = alerta.descripcion

        val punto = GeoPoint(alerta.latitud, alerta.longitud)
        val distKm = userLocation?.distanceToAsDouble(punto)?.div(1000.0)
        panelUbicacion.text = if (distKm != null) "📍 A %.1f km de ti".format(distKm)
        else "📍 Lima, Perú"

        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.post {
            panel.translationY = panel.height.toFloat()
            panel.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun ocultarPanel() {
        panel.animate().translationY(panel.height.toFloat()).alpha(0f)
            .setDuration(200).withEndAction { panel.visibility = View.GONE }.start()
        alertaActual = null
    }

    private fun enviarWhatsApp() {
        val a = alertaActual ?: return
        val t = "${a.titulo}\n${a.descripcion}\n📍 https://maps.google.com/?q=${a.latitud},${a.longitud}"
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/?text=${Uri.encode(t)}")
                )
            )
        } catch (_: Exception) {
            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirAlerta() {
        val a = alertaActual ?: return
        val t = "${a.titulo}\n${a.descripcion}\n📍 https://maps.google.com/?q=${a.latitud},${a.longitud}"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, t) },
                "Compartir alerta"
            )
        )
    }

    // ─── Ciclo de vida ──────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
