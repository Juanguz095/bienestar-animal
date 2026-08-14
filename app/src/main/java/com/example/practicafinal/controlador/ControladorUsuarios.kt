package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Usuario

/** Lógica de negocio para registro e inicio de sesión. */
object ControladorUsuarios {

    /**
     * Valida los campos de registro.
     * @return mensaje de error o null si todo está bien.
     */
    fun validarRegistro(
        nombre: String, correo: String,
        contrasena: String, confirmar: String
    ): String? {
        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank() || confirmar.isBlank())
            return "Completa todos los campos"
        if (contrasena.length < 6)
            return "La contraseña debe tener al menos 6 caracteres"
        if (contrasena != confirmar)
            return "Las contraseñas no coinciden"
        return null
    }

    /** Inserta un usuario en la base de datos. Devuelve su id o null si el correo ya existe. */
    fun registrar(context: Context, nombre: String, correo: String, contrasena: String): Long? {
        return DatabaseHelper(context).registrar(nombre, correo, contrasena)
    }

    /** Verifica las credenciales. Devuelve el usuario si son correctas, null si no. */
    fun iniciarSesion(context: Context, correo: String, contrasena: String): Usuario? {
        return DatabaseHelper(context).validarLogin(correo, contrasena)
    }

    /** Obtiene un usuario por su id. */
    fun obtenerPorId(context: Context, id: Long): Usuario? {
        return DatabaseHelper(context).obtenerPorId(id)
    }
}
