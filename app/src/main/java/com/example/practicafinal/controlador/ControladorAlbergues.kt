package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Albergue

/** Lógica de negocio para albergues y organizaciones. */
object ControladorAlbergues {

    /** Obtiene todos los albergues registrados. */
    fun obtenerAlbergues(context: Context): List<Albergue> {
        return DatabaseHelper(context).obtenerAlbergues()
    }

    /** Registra un albergue y devuelve su id. */
    fun insertarAlbergue(
        context: Context, nombre: String, descripcion: String, direccion: String,
        telefono: String, foto: String?, latitud: Double, longitud: Double
    ): Long {
        return DatabaseHelper(context).insertarAlbergue(
            nombre, descripcion, direccion, telefono, foto, latitud, longitud
        )
    }
}
