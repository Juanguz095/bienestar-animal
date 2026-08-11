# Guion técnico con código explicado

Presentación de "Bienestar Animal" — cada sección incluye qué decir y el código real bien explicado.

---

## 1. Arquitectura de la app

### Qué decir
> *"La app usa una arquitectura simple de capas: cada pantalla es una Activity que se comunica con una capa de datos central (DatabaseHelper) y modelos de datos. No usé MVVM porque para un proyecto de este tamaño añade complejidad innecesaria, pero la separación de responsabilidades está clara."*

### El código

**Modelo de datos** — una clase que representa una fila de la base:

```kotlin
// model/Publicacion.kt
data class Publicacion(
    val id: Long,               // identificador único
    val usuarioId: Long?,       // quién la publicó (null = sistema)
    val tipo: String,           // "Perdida", "Encontrada", "Adopcion"
    val nombre: String,         // nombre de la mascota
    val descripcion: String,    // descripción libre
    val foto: String?,          // URI de la foto (opcional)
    val ultimoLugar: String?,   // "última vez visto" en texto
    val especie: String?,       // "Perro" o "Gato"
    val latitud: Double,        // coordenadas del pin
    val longitud: Double,
    val fechaCreacion: Long,    // en milisegundos
    val estado: String          // "Activa" o "Resuelta"
)
```

**Punto clave**: la app es un triángulo — *Activity (pantalla) → DatabaseHelper (datos) → Modelo (objeto)*. Cada pantalla pide datos al DatabaseHelper y recibe modelos listos para mostrar.

---

## 2. Base de datos

### Qué decir
> *"La persistencia está hecha con SQLite a través de SQLiteOpenHelper, la API nativa de Android — sin librerías externas. Tiene tres tablas: usuarios, publicaciones y avistamientos. Las publicaciones se relacionan con avistamientos uno a muchos. Las consultas se ejecutan en hilos secundarios para no bloquear la interfaz."*

### El código

**Creación de las tablas** — se ejecuta UNA vez al instalar:

```kotlin
// db/DatabaseHelper.kt
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "bienestar_animal.db", null, 4) {
    // "bienestar_animal.db" = nombre del archivo
    // 4 = versión de la base (si cambia la estructura, se recrea)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE publicaciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,   -- id auto generado
                usuario_id INTEGER,                      -- FK → usuarios
                tipo TEXT NOT NULL,
                nombre TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                foto TEXT,                               -- nullable = opcional
                ultimo_lugar TEXT,
                especie TEXT,
                latitud REAL NOT NULL,
                longitud REAL NOT NULL,
                fecha_creacion INTEGER NOT NULL,
                estado TEXT NOT NULL DEFAULT 'Activa'
            )
        """)
        db.execSQL("""
            CREATE TABLE avistamientos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                publicacion_id INTEGER NOT NULL,  -- FK → publicaciones
                latitud REAL NOT NULL,
                longitud REAL NOT NULL,
                descripcion TEXT NOT NULL DEFAULT '',
                foto TEXT,
                fecha INTEGER NOT NULL
            )
        """)
    }
}
```

**Relación uno a muchos** — un avistamiento apunta a su publicación:

```kotlin
fun insertarAvistamiento(publicacionId: Long, latitud: Double, longitud: Double): Long {
    val values = ContentValues().apply {
        put("publicacion_id", publicacionId)   // ← la relación
        put("latitud", latitud)
        put("longitud", longitud)
        put("fecha", System.currentTimeMillis())
    }
    return writableDatabase.insert("avistamientos", null, values)
}
```

**Consultas en hilos** — nunca tocar la BD en el hilo principal:

```kotlin
// Un executor = un hilo de trabajo por pantalla
private val executor = Executors.newSingleThreadExecutor()

executor.execute {                    // ← hilo secundario: consulta BD
    val db = DatabaseHelper(this)
    val lista = db.obtenerPublicaciones()

    runOnUiThread {                   // ← hilo principal: actualizar UI
        renderAlertas(lista)
    }
}
```

---

## 3. Seguridad

### Qué decir
> *"Las contraseñas no se guardan en texto plano. Cada registro genera una sal aleatoria de 16 bytes con SecureRandom, se aplica SHA-256 a sal+contraseña, y solo se almacena el hash. Al iniciar sesión se recalcula con la sal guardada y se comparan."*

### El código

```kotlin
// Generar la sal: 16 bytes aleatorios convertidos a hexadecimal
private fun generarSal(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)                       // aleatorio criptográfico
    return bytes.joinToString("") { "%02x".format(it) }
}

// Hash: mezcla la sal con la contraseña y aplica SHA-256
private fun hashContrasena(contrasena: String, sal: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest("$sal$contrasena".toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { "%02x".format(it) }   // 64 caracteres hex
}

// Al registrar: se guarda hash + sal (NUNCA la contraseña)
fun registrar(nombre: String, correo: String, contrasena: String): Long? {
    val sal = generarSal()
    val values = ContentValues().apply {
        put("contrasena_hash", hashContrasena(contrasena, sal))  // solo el hash
        put("sal", sal)                                          // la sal para poder verificar
    }
    return try { writableDatabase.insertOrThrow("usuarios", null, values) }
    catch (_: Exception) { null }   // null = correo duplicado
}

// Al iniciar sesión: recalcular y comparar
fun validarLogin(correo: String, contrasena: String): Usuario? {
    // buscar el usuario por correo...
    if (hashContrasena(contrasena, salGuardada) == hashGuardado) {
        return usuario   // coinciden → login correcto
    }
    return null          // no coinciden → credenciales inválidas
}
```

**Por qué sal + hash**: si dos usuarios usan la misma contraseña, sus hashes son diferentes porque cada uno tiene una sal distinta. Aunque alguien robe la base, no puede revertir el hash.

---

## 4. Mapas y geolocalización

### Qué decir
> *"Los mapas usan osmdroid sobre OpenStreetMap, gratuito y sin API keys. Cada publicación se convierte en un marcador vectorial según su tipo. Mantener presionado captura las coordenadas exactas y las pasa a la Activity de creación. Para que el panel no tape el pin, calculo el bounding box visible y desplazo el centro un 25%."*

### El código

**Configuración del mapa** — antes de inflar el layout:

```kotlin
// En onCreate, ANTES de setContentView
Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
Configuration.getInstance().userAgentValue = "MapaAlertas/1.0 (juanguz619@gmail.com)"
// ↑ sin user agent, OpenStreetMap bloquea las peticiones

map.setTileSource(XYTileSource("OpenStreetMap", 0, 19, 256, ".png",
    arrayOf("https://tile.openstreetmap.org/", "https://a.tile.openstreetmap.org/", ...)))
// 0 = zoom mínimo, 19 = zoom máximo, 256 = tamaño de cada azulejo
```

**Marcadores por tipo** — cada publicación se pinta según su estado:

```kotlin
private fun ico(p: Publicacion): Int = when {
    p.estado == "Resuelta"   -> R.drawable.ic_pin_gris    // caso cerrado
    p.tipo == "Perdida"      -> R.drawable.ic_pin_rojo    // lupa
    p.tipo == "Encontrada"   -> R.drawable.ic_pin_verde   // check
    else                     -> R.drawable.ic_pin_naranja // corazón
}
```

**El long-press** — el gesto más importante:

```kotlin
map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
    override fun longPressHelper(p: GeoPoint): Boolean {
        // GeoPoint trae las coordenadas EXACTAS del punto presionado
        startActivity(Intent(this@MainActivity, CrearPublicacionActivity::class.java).apply {
            putExtra(CrearPublicacionActivity.EXTRA_LAT, p.latitude)
            putExtra(CrearPublicacionActivity.EXTRA_LNG, p.longitude)
        })
        return true
    }
}))
```

**El pin no se tapa** — truco del bounding box:

```kotlin
private fun centrarArriba(p: GeoPoint) {
    val b = map.boundingBox                      // área visible actual
    // desplazo el centro un 25% de la altura visible hacia el sur
    val centro = GeoPoint(p.latitude - (b.latNorth - b.latSouth) * 0.25, p.longitude)
    map.controller.animateTo(centro)             // animación suave SIN cambiar zoom
}
```

---

## 5. El flujo de avistamientos

### Qué decir
> *"El caso de uso más complejo: un avistamiento se inserta con el id de la publicación como clave foránea. Al cargar la lista, agrupo los avistamientos por publicación con groupBy y muestro un contador. Después de cualquier operación, la app vuelve a leer la base completa y redibuja los marcadores."*

### El código

**Contador por mascota** — un groupBy sobre la lista:

```kotlin
// MascotasPerdidasActivity.kt
val todosAvist = db.obtenerAvistamientos()
val avistPorId = todosAvist.groupBy { it.publicacionId }
// → Map<Long, List<Avistamiento>> : id de publicación → sus avistamientos

// En la tarjeta:
val avistCount = avistPorId[p.id]?.size ?: 0
holder.tvAvist.text = "👀 $avistCount avistamiento${if (avistCount > 1) "s" else ""}"
```

**Pines amarillos en el mapa** — cada avistamiento es un marcador independiente:

```kotlin
// MainActivity.kt
val m = Marker(map)
m.position = GeoPoint(av.latitud, av.longitud)
m.icon = ContextCompat.getDrawable(this, R.drawable.ic_pin_amarillo)
m.relatedObject = av   // ← el modelo "viaja" dentro del marcador

m.setOnMarkerClickListener { mk, _ ->
    val a = mk.relatedObject as? Avistamiento   // se recupera con cast
    centrarArriba(mk.position)
    showPanelAvist(a, nombres[a.publicacionId] ?: "la mascota")
    true
}
```

**El patrón de refresco** — la clave de toda la app:

```kotlin
// Después de CUALQUIER operación (publicar, avistar, resolver):
cargarBD()

private fun cargarBD() {
    exec.execute {                          // 1. leer la base completa
        val lista = db.obtenerPublicaciones()
        val avistamientos = db.obtenerAvistamientos()
        runOnUiThread { render(lista, avistamientos, nombres) }  // 2. redibujar todo
    }
}
```

**Por qué funciona**: render() borra todos los marcadores viejos y los crea de nuevo desde los datos frescos. Es simple, consistente y no necesita estado.

---

## 6. Sesión y favoritos

### Qué decir
> *"La sesión se guarda en SharedPreferences como el id del usuario. Los favoritos también, como un conjunto de ids. Para datos simples no tiene sentido crear tablas."*

### El código

```kotlin
// session/SesionManager.kt
object SesionManager {
    fun guardarSesion(context: Context, id: Long) {
        prefs(context).edit().putLong("usuario_id", id).apply()  // escribir
    }
    fun obtenerUsuarioId(context: Context): Long? {
        val id = prefs(context).getLong("usuario_id", -1L)
        return if (id == -1L) null else id                       // -1 = no hay sesión
    }
}

// session/FavoritosManager.kt
fun toggle(context: Context, id: Long): Boolean {
    val set = prefs(context).getStringSet("ids_favoritos", emptySet())!!.toMutableSet()
    return if (set.contains(id.toString())) { set.remove(id.toString()); false }
    else { set.add(id.toString()); true }    // agrega o quita el id
    prefs(context).edit().putStringSet("ids_favoritos", set).apply()
}
```

**Protección de la Activity principal**:

```kotlin
// MainActivity.kt — al arrancar
if (!SesionManager.tieneSesion(this)) {
    startActivity(Intent(this, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // CLEAR_TASK: destruye el historial para que el botón atrás no vuelva
    })
    finish()
}
```

---

## 7. Limitaciones honestas

### Qué decir
> *"Los datos son locales por dispositivo. Para una versión multiusuario real se necesitaría migrar a Firebase o un backend, que era el siguiente paso natural."*

### Explicación técnica

- **SQLite** = cada teléfono tiene su propia copia de datos
- **Firebase** = una base en la nube compartida; al publicar, TODOS la ven
- La lógica de la app **no cambia**: solo se reemplaza `DatabaseHelper` por el SDK de Firestore (mismos métodos, otra implementación)

---

## Preguntas rápidas

| Pregunta | Respuesta |
|---|---|
| ¿Por qué SQLiteOpenHelper y no Room? | Room requiere KSP/KAPT (procesadores de anotaciones) y más dependencias; para 3 tablas es sobredimensionado |
| ¿Cómo manejas la concurrencia? | Un executor de un solo hilo por Activity + `runOnUiThread` para volver a la UI |
| ¿Cómo calculas distancias? | `GeoPoint.distanceToAsDouble()` → metros → se divide entre 1000 para km |
| ¿Cómo refrescas el mapa? | `render()` borra marcadores viejos y los recrea desde la BD (patrón pull) |
| ¿Qué es `relatedObject`? | El marcador guarda el modelo; el listener lo recupera con `as?` (cast seguro) |
| ¿Cómo pasas coordenadas entre pantallas? | Extras del Intent (`putExtra`/`getDoubleExtra`) |
