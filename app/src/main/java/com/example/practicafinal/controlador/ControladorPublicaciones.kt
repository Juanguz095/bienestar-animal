package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion


object ControladorPublicaciones {

    
    fun validarPublicacion(nombre: String, descripcion: String): String? {
        if (nombre.isBlank() || descripcion.isBlank())
            return "Completa el nombre y la descripción"
        return null
    }

    
    fun publicar(
        context: Context,
        usuarioId: Long?, tipo: String, nombre: String, descripcion: String,
        foto: String?, ultimoLugar: String?, especie: String?,
        latitud: Double, longitud: Double
    ): Long {
        return DatabaseHelper(context).insertarPublicacion(
            usuarioId, tipo, nombre, descripcion, foto, ultimoLugar, especie, latitud, longitud
        )
    }

    
    fun resolver(context: Context, id: Long) {
        DatabaseHelper(context).marcarResuelta(id)
    }

    
    fun reportarAvistamiento(
        context: Context, publicacionId: Long, usuarioId: Long?,
        latitud: Double, longitud: Double, descripcion: String, foto: String?
    ): Long {
        return DatabaseHelper(context).insertarAvistamiento(
            publicacionId, usuarioId, latitud, longitud, descripcion, foto
        )
    }

    
    fun obtenerPublicaciones(context: Context): List<Publicacion> {
        return DatabaseHelper(context).obtenerPublicaciones()
    }

    
    fun obtenerPorId(context: Context, id: Long): Publicacion? {
        return DatabaseHelper(context).obtenerPorIdPublicacion(id)
    }

    
    fun obtenerPerdidas(context: Context): List<Publicacion> {
        return DatabaseHelper(context).obtenerPerdidas()
    }

    
    fun obtenerAdopciones(context: Context): List<Publicacion> {
        return DatabaseHelper(context).obtenerPublicaciones().filter { it.tipo == "Adopcion" }
    }

    
    fun obtenerAvistamientos(context: Context): List<Avistamiento> {
        return DatabaseHelper(context).obtenerAvistamientos()
    }

    
    fun actualizarAvistamiento(context: Context, id: Long, latitud: Double, longitud: Double) {
        DatabaseHelper(context).actualizarAvistamiento(id, latitud, longitud)
    }
}
