package com.example.practicafinal.model

data class Publicacion(
    val id: Long,
    val usuarioId: Long?,
    val tipo: String,           // "Perdida", "Encontrada", "Adopcion"
    val nombre: String,
    val descripcion: String,
    val foto: String?,          // URI de la foto (opcional)
    val ultimoLugar: String?,   // "Última vez visto" en texto (opcional)
    val latitud: Double,
    val longitud: Double,
    val fechaCreacion: Long,
    val estado: String          // "Activa", "Resuelta"
)
