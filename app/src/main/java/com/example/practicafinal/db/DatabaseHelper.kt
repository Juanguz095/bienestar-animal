package com.example.practicafinal.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.practicafinal.modelo.Albergue
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Denuncia
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.modelo.Usuario
import java.security.MessageDigest
import java.security.SecureRandom

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "bienestar_animal.db", null, 6) {

    companion object {
        private const val TABLA_USUARIOS = "usuarios"
        private const val TABLA_PUBLICACIONES = "publicaciones"
        private const val TABLA_AVISTAMIENTOS = "avistamientos"
        private const val TABLA_ALBERGUES = "albergues"
        private const val TABLA_DENUNCIAS = "denuncias"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLA_USUARIOS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT NOT NULL, " +
                    "correo TEXT NOT NULL UNIQUE, " +
                    "contrasena_hash TEXT NOT NULL, " +
                    "sal TEXT NOT NULL, " +
                    "tipo TEXT NOT NULL DEFAULT 'Ciudadano')"
        )
        db.execSQL(
            "CREATE TABLE $TABLA_PUBLICACIONES (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "usuario_id INTEGER, " +
                    "tipo TEXT NOT NULL, " +
                    "nombre TEXT NOT NULL, " +
                    "descripcion TEXT NOT NULL, " +
                    "foto TEXT, " +
                    "ultimo_lugar TEXT, " +
                    "especie TEXT, " +
                    "latitud REAL NOT NULL, " +
                    "longitud REAL NOT NULL, " +
                    "fecha_creacion INTEGER NOT NULL, " +
                    "estado TEXT NOT NULL DEFAULT 'Activa')"
        )
        db.execSQL(
            "CREATE TABLE $TABLA_AVISTAMIENTOS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "publicacion_id INTEGER NOT NULL, " +
                    "usuario_id INTEGER, " +
                    "latitud REAL NOT NULL, " +
                    "longitud REAL NOT NULL, " +
                    "descripcion TEXT NOT NULL DEFAULT '', " +
                    "foto TEXT, " +
                    "fecha INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE $TABLA_ALBERGUES (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT NOT NULL, " +
                    "descripcion TEXT NOT NULL DEFAULT '', " +
                    "direccion TEXT NOT NULL DEFAULT '', " +
                    "telefono TEXT NOT NULL DEFAULT '', " +
                    "foto TEXT, " +
                    "latitud REAL NOT NULL, " +
                    "longitud REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE $TABLA_DENUNCIAS (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "motivo TEXT NOT NULL, " +
                    "descripcion TEXT NOT NULL, " +
                    "foto TEXT, " +
                    "latitud REAL NOT NULL, " +
                    "longitud REAL NOT NULL, " +
                    "fecha INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLA_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_PUBLICACIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_AVISTAMIENTOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_ALBERGUES")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_DENUNCIAS")
        onCreate(db)
    }

    /**
     * Registra un usuario con la contraseña encriptada (hash + sal).
     * Devuelve su id, o null si el correo ya está registrado.
     */
    fun registrar(nombre: String, correo: String, contrasena: String, tipo: String = "Ciudadano"): Long? {
        val sal = generarSal()
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("correo", correo.lowercase())
            put("contrasena_hash", hashContrasena(contrasena, sal))
            put("sal", sal)
            put("tipo", tipo)
        }
        return try {
            writableDatabase.insertOrThrow(TABLA_USUARIOS, null, values)
        } catch (_: Exception) {
            null // correo duplicado
        }
    }

    /** Verifica correo + contraseña. Devuelve el usuario si son correctos. */
    fun validarLogin(correo: String, contrasena: String): Usuario? {
        val cursor = readableDatabase.query(
            TABLA_USUARIOS, null, "correo = ?",
            arrayOf(correo.lowercase()), null, null, null
        )
        var usuario: Usuario? = null
        if (cursor.moveToFirst()) {
            val hash = cursor.getString(cursor.getColumnIndexOrThrow("contrasena_hash"))
            val sal = cursor.getString(cursor.getColumnIndexOrThrow("sal"))
            if (hashContrasena(contrasena, sal) == hash) {
                usuario = Usuario(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"))
                )
            }
        }
        cursor.close()
        return usuario
    }

    fun obtenerPorId(id: Long): Usuario? {
        val cursor = readableDatabase.query(
            TABLA_USUARIOS, null, "id = ?",
            arrayOf(id.toString()), null, null, null
        )
        var usuario: Usuario? = null
        if (cursor.moveToFirst()) {
            usuario = Usuario(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"))
            )
        }
        cursor.close()
        return usuario
    }

    // ─── Publicaciones ─────────────────────────────────────────────

    fun insertarPublicacion(
        usuarioId: Long?,
        tipo: String,
        nombre: String,
        descripcion: String,
        foto: String?,
        ultimoLugar: String?,
        especie: String?,
        latitud: Double,
        longitud: Double
    ): Long {
        val values = ContentValues().apply {
            put("usuario_id", usuarioId)
            put("tipo", tipo)
            put("nombre", nombre)
            put("descripcion", descripcion)
            put("foto", foto)
            put("ultimo_lugar", ultimoLugar)
            put("especie", especie)
            put("latitud", latitud)
            put("longitud", longitud)
            put("fecha_creacion", System.currentTimeMillis())
            put("estado", "Activa")
        }
        return writableDatabase.insert(TABLA_PUBLICACIONES, null, values)
    }

    fun obtenerPublicaciones(): List<Publicacion> {
        val lista = mutableListOf<Publicacion>()
        val cursor = readableDatabase.query(
            TABLA_PUBLICACIONES, null, null, null, null, null,
            "fecha_creacion DESC"
        )
        while (cursor.moveToNext()) {
            val idx = cursor.getColumnIndexOrThrow("usuario_id")
            val idxFoto = cursor.getColumnIndexOrThrow("foto")
            val idxLugar = cursor.getColumnIndexOrThrow("ultimo_lugar")
            val idxEspecie = cursor.getColumnIndexOrThrow("especie")
            lista.add(
                Publicacion(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    usuarioId = if (cursor.isNull(idx)) null else cursor.getLong(idx),
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    foto = if (cursor.isNull(idxFoto)) null else cursor.getString(idxFoto),
                    ultimoLugar = if (cursor.isNull(idxLugar)) null else cursor.getString(idxLugar),
                    especie = if (cursor.isNull(idxEspecie)) null else cursor.getString(idxEspecie),
                    latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                    longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                    fechaCreacion = cursor.getLong(cursor.getColumnIndexOrThrow("fecha_creacion")),
                    estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                )
            )
        }
        cursor.close()
        return lista
    }

    fun obtenerPerdidas(): List<Publicacion> =
        obtenerPublicaciones().filter { it.tipo == "Perdida" }

    fun obtenerPorIdPublicacion(id: Long): Publicacion? {
        val cursor = readableDatabase.query(
            TABLA_PUBLICACIONES, null, "id = ?", arrayOf(id.toString()),
            null, null, null
        )
        var p: Publicacion? = null
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndexOrThrow("usuario_id")
            val idxFoto = cursor.getColumnIndexOrThrow("foto")
            val idxLugar = cursor.getColumnIndexOrThrow("ultimo_lugar")
            val idxEspecie = cursor.getColumnIndexOrThrow("especie")
            p = Publicacion(
                id = id,
                usuarioId = if (cursor.isNull(idx)) null else cursor.getLong(idx),
                tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                foto = if (cursor.isNull(idxFoto)) null else cursor.getString(idxFoto),
                ultimoLugar = if (cursor.isNull(idxLugar)) null else cursor.getString(idxLugar),
                especie = if (cursor.isNull(idxEspecie)) null else cursor.getString(idxEspecie),
                latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                fechaCreacion = cursor.getLong(cursor.getColumnIndexOrThrow("fecha_creacion")),
                estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
            )
        }
        cursor.close()
        return p
    }

    // ─── Avistamientos ─────────────────────────────────────────────

    fun insertarAvistamiento(
        publicacionId: Long,
        usuarioId: Long?,
        latitud: Double,
        longitud: Double,
        descripcion: String,
        foto: String?
    ): Long {
        val values = ContentValues().apply {
            put("publicacion_id", publicacionId)
            put("usuario_id", usuarioId)
            put("latitud", latitud)
            put("longitud", longitud)
            put("descripcion", descripcion)
            put("foto", foto)
            put("fecha", System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLA_AVISTAMIENTOS, null, values)
    }

    fun obtenerAvistamientos(): List<Avistamiento> {
        val lista = mutableListOf<Avistamiento>()
        val cursor = readableDatabase.query(
            TABLA_AVISTAMIENTOS, null, null, null, null, null,
            "fecha DESC"
        )
        while (cursor.moveToNext()) {
            val idx = cursor.getColumnIndexOrThrow("usuario_id")
            val idxFoto = cursor.getColumnIndexOrThrow("foto")
            lista.add(
                Avistamiento(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    publicacionId = cursor.getLong(cursor.getColumnIndexOrThrow("publicacion_id")),
                    usuarioId = if (cursor.isNull(idx)) null else cursor.getLong(idx),
                    latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                    longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    foto = if (cursor.isNull(idxFoto)) null else cursor.getString(idxFoto),
                    fecha = cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))
                )
            )
        }
        cursor.close()
        return lista
    }

    fun marcarResuelta(id: Long) {
        val values = ContentValues().apply { put("estado", "Resuelta") }
        writableDatabase.update(TABLA_PUBLICACIONES, values, "id = ?", arrayOf(id.toString()))
    }

    fun obtenerAvistamientosPorPublicacion(publicacionId: Long): List<Avistamiento> {
        val lista = mutableListOf<Avistamiento>()
        val cursor = readableDatabase.query(
            TABLA_AVISTAMIENTOS, null,
            "publicacion_id = ?", arrayOf(publicacionId.toString()),
            null, null, "fecha DESC"
        )
        while (cursor.moveToNext()) {
            val idx = cursor.getColumnIndexOrThrow("usuario_id")
            val idxFoto = cursor.getColumnIndexOrThrow("foto")
            lista.add(
                Avistamiento(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    publicacionId = publicacionId,
                    usuarioId = if (cursor.isNull(idx)) null else cursor.getLong(idx),
                    latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                    longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    foto = if (cursor.isNull(idxFoto)) null else cursor.getString(idxFoto),
                    fecha = cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))
                )
            )
        }
        cursor.close()
        return lista
    }

    /** Actualiza la ubicación al reportar un avistamiento ("Sé dónde está"). */
    fun actualizarAvistamiento(id: Long, latitud: Double, longitud: Double) {
        val values = ContentValues().apply {
            put("latitud", latitud)
            put("longitud", longitud)
            put("fecha_creacion", System.currentTimeMillis())
        }
        writableDatabase.update(TABLA_PUBLICACIONES, values, "id = ?", arrayOf(id.toString()))
    }

    fun insertarAlbergue(
        nombre: String, descripcion: String, direccion: String,
        telefono: String, foto: String?, latitud: Double, longitud: Double
    ): Long {
        val values = ContentValues().apply {
            put("nombre", nombre); put("descripcion", descripcion)
            put("direccion", direccion); put("telefono", telefono)
            put("foto", foto); put("latitud", latitud); put("longitud", longitud)
        }
        return writableDatabase.insert(TABLA_ALBERGUES, null, values)
    }

    fun obtenerAlbergues(): List<Albergue> {
        val lista = mutableListOf<Albergue>()
        val cursor = readableDatabase.query(TABLA_ALBERGUES, null, null, null, null, null, null)
        while (cursor.moveToNext()) {
            val idxFoto = cursor.getColumnIndexOrThrow("foto")
            lista.add(
                Albergue(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    direccion = cursor.getString(cursor.getColumnIndexOrThrow("direccion")),
                    telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                    foto = if (cursor.isNull(idxFoto)) null else cursor.getString(idxFoto),
                    latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                    longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"))
                )
            )
        }
        cursor.close()
        return lista
    }

    // ─── Denuncias ─────────────────────────────────────────────────

    fun insertarDenuncia(
        motivo: String, descripcion: String, foto: String?,
        latitud: Double, longitud: Double
    ): Long {
        val values = ContentValues().apply {
            put("motivo", motivo); put("descripcion", descripcion)
            put("foto", foto); put("latitud", latitud); put("longitud", longitud)
            put("fecha", System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLA_DENUNCIAS, null, values)
    }

    fun obtenerDenuncias(): List<Denuncia> {
        val lista = mutableListOf<Denuncia>()
        val cursor = readableDatabase.query(TABLA_DENUNCIAS, null, null, null, null, null, "fecha DESC")
        while (cursor.moveToNext()) {
            val idxFoto = cursor.getColumnIndexOrThrow("foto")
            lista.add(
                Denuncia(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    motivo = cursor.getString(cursor.getColumnIndexOrThrow("motivo")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    foto = if (cursor.isNull(idxFoto)) null else cursor.getString(idxFoto),
                    latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                    longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                    fecha = cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))
                )
            )
        }
        cursor.close()
        return lista
    }

    private fun generarSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashContrasena(contrasena: String, sal: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("$sal$contrasena".toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
