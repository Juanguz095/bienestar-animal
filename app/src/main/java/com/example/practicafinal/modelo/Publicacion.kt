package com.example.practicafinal.modelo

data class Publicacion(
    val id: Long,
    val usuarioId: Long?,
    val tipo: String,
    val nombre: String,
    val descripcion: String,
    val foto: String?,
    val ultimoLugar: String?,
    val especie: String?,
    val latitud: Double,
    val longitud: Double,
    val fechaCreacion: Long,
    val estado: String
)
