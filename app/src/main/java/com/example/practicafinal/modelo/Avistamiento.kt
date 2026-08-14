package com.example.practicafinal.modelo

data class Avistamiento(
    val id: Long,
    val publicacionId: Long,
    val usuarioId: Long?,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String,
    val foto: String?,
    val fecha: Long
)
