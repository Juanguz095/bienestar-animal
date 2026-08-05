package com.example.practicafinal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adapters.AnimalAdapter
import com.example.practicafinal.model.Animal

class AdopcionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adopciones)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rvAnimales =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_animales)
        rvAnimales.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAnimales.adapter = AnimalAdapter(
            listOf(
                Animal("Luna", "Perro", 2, "Golden Retriever"),
                Animal("Firulais", "Perro", 3, "Labrador"),
                Animal("Mishi", "Gato", 1, "Siames")
            )
        )
    }
}
