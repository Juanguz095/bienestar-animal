package com.example.practicafinal.modelo

data class Albergue(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val direccion: String,
    val telefono: String,
    val foto: String?,
    val latitud: Double,
    val longitud: Double
)
