package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion

/** Lógica de negocio para publicaciones y avistamientos. */
object ControladorPublicaciones {

    /**
     * Valida los campos de una publicación.
     * @return mensaje de error o null si todo está bien.
     */
    fun validarPublicacion(nombre: String, descripcion: String): String? {
        if (nombre.isBlank() || descripcion.isBlank())
            return "Completa el nombre y la descripción"
        return null
    }

    /** Crea una publicación y devuelve su id. */
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

    /** Marca una publicación como resuelta. */
    fun resolver(context: Context, id: Long) {
        DatabaseHelper(context).marcarResuelta(id)
    }

    /** Inserta un reporte de avistamiento. */
    fun reportarAvistamiento(
        context: Context, publicacionId: Long, usuarioId: Long?,
        latitud: Double, longitud: Double, descripcion: String, foto: String?
    ): Long {
        return DatabaseHelper(context).insertarAvistamiento(
            publicacionId, usuarioId, latitud, longitud, descripcion, foto
        )
    }

    /** Obtiene todas las publicaciones. */
    fun obtenerPublicaciones(context: Context): List<Publicacion> {
        return DatabaseHelper(context).obtenerPublicaciones()
    }

    /** Obtiene una publicación por su id. */
    fun obtenerPorId(context: Context, id: Long): Publicacion? {
        return DatabaseHelper(context).obtenerPorIdPublicacion(id)
    }

    /** Obtiene solo las publicaciones de mascotas perdidas. */
    fun obtenerPerdidas(context: Context): List<Publicacion> {
        return DatabaseHelper(context).obtenerPerdidas()
    }

    /** Obtiene solo las publicaciones de adopción. */
    fun obtenerAdopciones(context: Context): List<Publicacion> {
        return DatabaseHelper(context).obtenerPublicaciones().filter { it.tipo == "Adopcion" }
    }

    /** Obtiene todos los avistamientos reportados. */
    fun obtenerAvistamientos(context: Context): List<Avistamiento> {
        return DatabaseHelper(context).obtenerAvistamientos()
    }

    /** Actualiza la ubicación de un avistamiento ya registrado. */
    fun actualizarAvistamiento(context: Context, id: Long, latitud: Double, longitud: Double) {
        DatabaseHelper(context).actualizarAvistamiento(id, latitud, longitud)
    }
}
