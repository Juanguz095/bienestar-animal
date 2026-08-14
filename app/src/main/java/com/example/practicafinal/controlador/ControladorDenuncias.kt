package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Denuncia


object ControladorDenuncias {

    
    fun insertarDenuncia(
        context: Context, motivo: String, descripcion: String, foto: String?,
        latitud: Double, longitud: Double
    ): Long {
        return DatabaseHelper(context).insertarDenuncia(motivo, descripcion, foto, latitud, longitud)
    }

    
    fun obtenerDenuncias(context: Context): List<Denuncia> {
        return DatabaseHelper(context).obtenerDenuncias()
    }
}
