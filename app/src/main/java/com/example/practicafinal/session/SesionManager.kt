package com.example.practicafinal.session

import android.content.Context
import android.content.SharedPreferences

object SesionManager {

    private const val PREFS = "sesion"
    private const val KEY_ID = "usuario_id"

    fun guardarSesion(context: Context, id: Long) {
        prefs(context).edit().putLong(KEY_ID, id).apply()
    }

    fun obtenerUsuarioId(context: Context): Long? {
        val id = prefs(context).getLong(KEY_ID, -1L)
        return if (id == -1L) null else id
    }

    fun tieneSesion(context: Context): Boolean = obtenerUsuarioId(context) != null

    fun cerrarSesion(context: Context) {
        prefs(context).edit().remove(KEY_ID).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
