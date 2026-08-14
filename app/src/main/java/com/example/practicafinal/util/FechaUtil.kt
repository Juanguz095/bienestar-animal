package com.example.practicafinal.util


fun fechaRelativa(fechaMs: Long): String {
    val minutos = (System.currentTimeMillis() - fechaMs) / 60000
    return when {
        minutos < 1 -> "ahora mismo"
        minutos < 60 -> "hace $minutos min"
        minutos < 1440 -> "hace ${minutos / 60} h"
        else -> "hace ${minutos / 1440} d"
    }
}
