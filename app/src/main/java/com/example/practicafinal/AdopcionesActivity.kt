package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adaptadores.AdaptadorAdopciones
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.session.FavoritosManager
import java.util.concurrent.Executors

class AdopcionesActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etBuscar: EditText
    private lateinit var rvAnimales: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvFavCount: TextView
    private lateinit var tvOpciones: TextView

    private var publicaciones: List<Publicacion> = emptyList()
    private var filtroEspecie: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adopciones)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        etBuscar = findViewById(R.id.et_buscar)
        rvAnimales = findViewById(R.id.rv_animales)
        tvFavCount = findViewById(R.id.tv_fav_count)
        tvOpciones = findViewById(R.id.tv_opciones)
        rvAnimales.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Corazón de favoritos: abre la ventana de animales favoritos
        findViewById<View>(R.id.btn_favoritos).setOnClickListener {
            startActivity(Intent(this, MisFavoritosActivity::class.java))
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = aplicarFiltros()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Chips de filtro de especie
        val chipTodos = findViewById<TextView>(R.id.chip_todos)
        val chipPerros = findViewById<TextView>(R.id.chip_perros)
        val chipGatos = findViewById<TextView>(R.id.chip_gatos)

        fun seleccionarChip(chip: TextView, tipo: String?) {
            listOf(chipTodos to null, chipPerros to "Perro", chipGatos to "Gato").forEach { (c, t) ->
                val activo = t == tipo
                c.background = if (activo) ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
                else ContextCompat.getDrawable(this, R.drawable.bg_chip)
                c.setTextColor(
                    if (activo) ContextCompat.getColor(this, android.R.color.black)
                    else ContextCompat.getColor(this, android.R.color.darker_gray)
                )
            }
            filtroEspecie = tipo
            aplicarFiltros()
        }

        chipTodos.setOnClickListener { seleccionarChip(chipTodos, null) }
        chipPerros.setOnClickListener { seleccionarChip(chipPerros, "Perro") }
        chipGatos.setOnClickListener { seleccionarChip(chipGatos, "Gato") }

        cargarAdopciones()
    }

    private fun cargarAdopciones() {
        executor.execute {
            publicaciones = ControladorPublicaciones.obtenerAdopciones(this)
            runOnUiThread {
                aplicarFiltros()
                actualizarContadorFav()
                val n = publicaciones.size
                tvOpciones.text = if (n == 1) "1 opción para adoptar" else "$n opciones para adoptar"
            }
        }
    }

    private fun aplicarFiltros() {
        val texto = etBuscar.text.toString().trim().lowercase()
        val filtradas = publicaciones.filter { p ->
            val coincideNombre = p.nombre.lowercase().contains(texto) ||
                    p.descripcion.lowercase().contains(texto)
            val coincideEspecie = filtroEspecie == null ||
                    p.especie?.equals(filtroEspecie, ignoreCase = true) == true
            coincideNombre && coincideEspecie
        }

        rvAnimales.adapter = AdaptadorAdopciones(filtradas) { publicacion ->
            startActivity(
                Intent(this, DetalleAdopcionActivity::class.java).apply {
                    putExtra(DetalleAdopcionActivity.EXTRA_PUBLICACION_ID, publicacion.id)
                }
            )
        }
    }

    private fun actualizarContadorFav() {
        val idsFav = FavoritosManager.obtenerIds(this)
        val count = publicaciones.count { idsFav.contains(it.id.toString()) }
        tvFavCount.text = count.toString()
    }

    override fun onResume() {
        super.onResume()
        cargarAdopciones()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
