package com.example.practicafinal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.controlador.ControladorAlbergues
import com.example.practicafinal.modelo.Albergue
import com.example.practicafinal.util.decodificarImagen
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.Executors

class AlberguesActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: RecyclerView;
    private lateinit var tvVacio: TextView;
    private lateinit var etBuscar: EditText
    private var albergues: List<Albergue> = emptyList();
    private var userLoc: GeoPoint? = null
    private var ordenPorCercanos = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_albergues)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_albergues); tvVacio = findViewById(R.id.tv_vacio); etBuscar =
            findViewById(R.id.et_buscar)
        rv.layoutManager = LinearLayoutManager(this)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = mostrarLista()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val chipCercanos = findViewById<TextView>(R.id.chip_cercanos)
        val chipAz = findViewById<TextView>(R.id.chip_az)
        chipCercanos.setOnClickListener {
            ordenPorCercanos = true
            chipCercanos.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            chipCercanos.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            chipAz.background = ContextCompat.getDrawable(this, R.drawable.bg_chip); chipAz.setTextColor(
            ContextCompat.getColor(
                this,
                android.R.color.darker_gray
            )
        ); mostrarLista()
        }
        chipAz.setOnClickListener {
            ordenPorCercanos = false
            chipAz.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            chipAz.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            chipCercanos.background = ContextCompat.getDrawable(this, R.drawable.bg_chip); chipCercanos.setTextColor(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        ); mostrarLista()
        }

        cargar()
    }

    private fun obtenerUbicacion(): GeoPoint? {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null
        return try {
            (getSystemService(Context.LOCATION_SERVICE) as LocationManager).let {
                it.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                ) ?: it.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun cargar() {
        exec.execute {
            if (ControladorAlbergues.obtenerAlbergues(this).isEmpty()) {
                ControladorAlbergues.insertarAlbergue(
                    this,
                    "Albergue Patitas",
                    "Refugio de mascotas",
                    "Av. Universitaria 123",
                    "999888777",
                    null,
                    -12.0850,
                    -77.0050
                )
                ControladorAlbergues.insertarAlbergue(
                    this,
                    "Refugio Huellitas",
                    "Hogar temporal",
                    "Jr. Las Flores 456",
                    "987654321",
                    null,
                    -12.0200,
                    -77.0800
                )
                ControladorAlbergues.insertarAlbergue(
                    this,
                    "Hogar Peludo",
                    "Adopción responsable",
                    "Calle Los Olivos 789",
                    "912345678",
                    null,
                    -12.0760,
                    -77.0620
                )
            }
            albergues = ControladorAlbergues.obtenerAlbergues(this); userLoc = obtenerUbicacion()
            runOnUiThread { mostrarLista() }
        }
    }

    private fun mostrarLista() {
        val texto = etBuscar.text.toString().trim().lowercase()
        val filtrados =
            albergues.filter { it.nombre.lowercase().contains(texto) || it.direccion.lowercase().contains(texto) }
                .sortedWith(if (ordenPorCercanos && userLoc != null) compareBy {
                    GeoPoint(
                        it.latitud,
                        it.longitud
                    ).distanceToAsDouble(userLoc!!)
                } else compareBy { it.nombre.lowercase() })

        tvVacio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                LayoutInflater.from(p.context).inflate(R.layout.item_albergue_card, p, false)
            ) {}

            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val a = filtrados[pos]
                val foto = a.foto?.let { decodificarImagen(h.itemView.context, it, 4) }
                val img = h.itemView.findViewById<android.widget.ImageView>(R.id.img_foto)
                val pl = h.itemView.findViewById<TextView>(R.id.tv_placeholder)
                if (foto != null) {
                    img.setImageBitmap(foto); pl.visibility = View.GONE
                } else pl.visibility = View.VISIBLE
                (h.itemView.findViewById<TextView>(R.id.tv_nombre)).text = a.nombre
                (h.itemView.findViewById<TextView>(R.id.tv_direccion)).text = a.direccion
                (h.itemView.findViewById<TextView>(R.id.tv_telefono)).text = "📞 ${a.telefono}"
                if (userLoc != null) (h.itemView.findViewById<TextView>(R.id.tv_distancia)).text = String.format(
                    Locale("es"),
                    "A %.1f km",
                    GeoPoint(a.latitud, a.longitud).distanceToAsDouble(userLoc!!) / 1000.0
                )
                else h.itemView.findViewById<TextView>(R.id.tv_distancia).text = ""
                h.itemView.setOnClickListener {
                    startActivity(
                        Intent(
                            this@AlberguesActivity,
                            DetalleAlbergueActivity::class.java
                        ).apply { putExtra("albergue_id", a.id) })
                }
                h.itemView.findViewById<android.widget.ImageView>(R.id.btn_llamar)
                    .setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${a.telefono}"))) }
            }

            override fun getItemCount() = filtrados.size
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
