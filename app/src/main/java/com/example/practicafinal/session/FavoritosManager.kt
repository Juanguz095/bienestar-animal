package com.example.practicafinal.session

import android.content.Context
import android.content.SharedPreferences

/** Almacena y consulta los IDs de publicaciones favoritas de un usuario. */
object FavoritosManager {

    private const val PREFS = "favoritos"
    private const val KEY_FAVS = "ids_favoritos"

    fun esFavorito(context: Context, publicacionId: Long): Boolean =
        prefs(context).getStringSet(KEY_FAVS, emptySet())!!.contains(publicacionId.toString())

    fun toggle(context: Context, publicacionId: Long): Boolean {
        val set = prefs(context).getStringSet(KEY_FAVS, emptySet())!!.toMutableSet()
        val key = publicacionId.toString()
        val agregado = if (set.contains(key)) {
            set.remove(key)
            false
        } else {
            set.add(key)
            true
        }
        prefs(context).edit().putStringSet(KEY_FAVS, set).apply()
        return agregado
    }

    fun contar(context: Context): Int =
        prefs(context).getStringSet(KEY_FAVS, emptySet())!!.size

    fun obtenerIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FAVS, emptySet())!!

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
