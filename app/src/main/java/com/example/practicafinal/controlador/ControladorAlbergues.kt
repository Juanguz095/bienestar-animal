package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Albergue


object ControladorAlbergues {

    
    fun obtenerAlbergues(context: Context): List<Albergue> {
        return DatabaseHelper(context).obtenerAlbergues()
    }

    
    fun insertarAlbergue(
        context: Context, nombre: String, descripcion: String, direccion: String,
        telefono: String, foto: String?, latitud: Double, longitud: Double
    ): Long {
        return DatabaseHelper(context).insertarAlbergue(
            nombre, descripcion, direccion, telefono, foto, latitud, longitud
        )
    }
}
