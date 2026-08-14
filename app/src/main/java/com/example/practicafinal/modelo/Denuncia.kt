package com.example.practicafinal.modelo

data class Denuncia(
    val id: Long,
    val motivo: String,
    val descripcion: String,
    val foto: String?,
    val latitud: Double,
    val longitud: Double,
    val fecha: Long
)
