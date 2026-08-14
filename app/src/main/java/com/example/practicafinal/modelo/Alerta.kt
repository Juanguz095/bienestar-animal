package com.example.practicafinal.modelo

data class Alerta(
    val titulo: String,
    val tipo: String, // "Perdida", "Encontrada", "Albergue"
    val descripcion: String,
    val latitud: Double,
    val longitud: Double
)
