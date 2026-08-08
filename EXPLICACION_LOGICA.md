# Cómo programé la lógica de la app

Este documento explica **cómo funciona la app por dentro**, con el código real de cada parte.

---

## 1. Arranque de la app

Cuando se abre la app, lo primero es **preguntarse: ¿hay alguien con sesión iniciada?**

```kotlin
// SesionManager.kt — la sesión es un id guardado en preferencias
fun tieneSesion(context: Context): Boolean = obtenerUsuarioId(context) != null

fun obtenerUsuarioId(context: Context): Long? {
    val id = prefs(context).getLong(KEY_ID, -1L)
    return if (id == -1L) null else id
}
```

```kotlin
// MainActivity.kt — si no hay sesión, redirige al Login
if (!SesionManager.tieneSesion(this)) {
    startActivity(Intent(this, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    })
    finish()
    return
}
```

---

## 2. Registro y Login

**Registro** — valida campos y guarda con contraseña encriptada:

```kotlin
// RegistroActivity.kt — validaciones
when {
    nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || confirmar.isEmpty() ->
        mostrarError("Completa todos los campos")
    contrasena.length < 6 ->
        mostrarError("La contraseña debe tener al menos 6 caracteres")
    contrasena != confirmar ->
        mostrarError("Las contraseñas no coinciden")
    else -> {
        executor.execute {
            val id = DatabaseHelper(this).registrar(nombre, correo, contrasena)
            // si id != null → sesión guardada y entrar; si no → "correo ya registrado"
        }
    }
}
```

**Encriptación** — la contraseña se revuelve con una sal y se convierte en hash:

```kotlin
// DatabaseHelper.kt
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
```

**Login** — busca por correo y compara el hash:

```kotlin
fun validarLogin(correo: String, contrasena: String): Usuario? {
    val cursor = readableDatabase.query(
        TABLA_USUARIOS, null, "correo = ?",
        arrayOf(correo.lowercase()), null, null, null
    )
    if (cursor.moveToFirst()) {
        val hash = cursor.getString(cursor.getColumnIndexOrThrow("contrasena_hash"))
        val sal = cursor.getString(cursor.getColumnIndexOrThrow("sal"))
        if (hashContrasena(contrasena, sal) == hash) {
            // construir Usuario con sus datos...
        }
    }
    cursor.close()
    return usuario
}
```

---

## 3. El mapa principal

**Centrar en el usuario** — usa la última ubicación o le pide una al GPS:

```kotlin
private fun centrarEnUsuario() {
    val ubicacion = obtenerUltimaUbicacion()
    if (ubicacion != null) {
        centrarEnPunto(ubicacion)
    } else {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.requestSingleUpdate(LocationManager.GPS_PROVIDER,
            { loc -> centrarEnPunto(loc) }, mainLooper)
    }
}

private fun centrarEnPunto(ubicacion: Location) {
    val punto = GeoPoint(ubicacion.latitude, ubicacion.longitude)
    userLocation = punto
    agregarMarcadorUsuario(punto)      // punto azul
    agregarCirculoCerca(punto)         // círculo de 2 km
    map.controller.setZoom(16.0)
    map.controller.setCenter(punto)
}
```

**Cargar publicaciones y pintar pines**:

```kotlin
private fun cargarAlertasDesdeBD() {
    executor.execute {
        val db = DatabaseHelper(this)
        val lista = db.obtenerPublicaciones()
        val avistamientos = db.obtenerAvistamientos()
        runOnUiThread { renderAlertas(lista, avistamientos, nombres) }
    }
}
```

```kotlin
private fun renderAlertas(lista, avistamientos, nombres) {
    // borrar pines viejos
    marcadoresAlertas.forEach { map.overlays.remove(it) }

    lista.forEach { publicacion ->
        val m = Marker(map)
        m.position = GeoPoint(publicacion.latitud, publicacion.longitud)
        m.icon = ContextCompat.getDrawable(this, iconoPara(publicacion))
        m.relatedObject = publicacion
        m.setOnMarkerClickListener { marker, _ ->
            map.controller.animateTo(marker.position)   // zoom al pin
            mostrarCirculoBusqueda(p)                    // círculo 1.5 km si es perdida
            mostrarPanel(p)                              // panel inferior
        }
        map.overlays.add(m)
    }
}
```

**Color del pin según tipo y estado**:

```kotlin
private fun iconoPara(publicacion: Publicacion): Int = when {
    publicacion.estado == "Resuelta" -> R.drawable.ic_pin_gris
    publicacion.tipo == "Perdida" -> R.drawable.ic_pin_rojo
    publicacion.tipo == "Encontrada" -> R.drawable.ic_pin_verde
    else -> R.drawable.ic_pin_naranja
}
```

---

## 4. Publicar una mascota

**Long-press captura las coordenadas exactas y abre el formulario**:

```kotlin
// MainActivity.kt
map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
    override fun longPressHelper(p: GeoPoint): Boolean {
        startActivity(Intent(this@MainActivity, CrearPublicacionActivity::class.java).apply {
            putExtra(CrearPublicacionActivity.EXTRA_LAT, p.latitude)
            putExtra(CrearPublicacionActivity.EXTRA_LNG, p.longitude)
        })
        return true
    }
}))
```

```kotlin
// CrearPublicacionActivity.kt — recibe el punto y coloca el pin
val lat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
val lng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)
val puntoInicial = if (!lat.isNaN() && !lng.isNaN()) GeoPoint(lat, lng) else null
```

**Guardar en la BD**:

```kotlin
fun insertarPublicacion(
    usuarioId: Long?, tipo: String, nombre: String, descripcion: String,
    foto: String?, ultimoLugar: String?, especie: String?,
    latitud: Double, longitud: Double
): Long {
    val values = ContentValues().apply {
        put("usuario_id", usuarioId); put("tipo", tipo)
        put("nombre", nombre); put("descripcion", descripcion)
        put("foto", foto); put("ultimo_lugar", ultimoLugar); put("especie", especie)
        put("latitud", latitud); put("longitud", longitud)
        put("fecha_creacion", System.currentTimeMillis())
        put("estado", "Activa")
    }
    return writableDatabase.insert(TABLA_PUBLICACIONES, null, values)
}
```

---

## 5. Avistamientos comunitarios

**Guardar un avistamiento** (se vincula con la publicación):

```kotlin
fun insertarAvistamiento(
    publicacionId: Long, usuarioId: Long?,
    latitud: Double, longitud: Double,
    descripcion: String, foto: String?
): Long {
    val values = ContentValues().apply {
        put("publicacion_id", publicacionId); put("usuario_id", usuarioId)
        put("latitud", latitud); put("longitud", longitud)
        put("descripcion", descripcion); put("foto", foto)
        put("fecha", System.currentTimeMillis())
    }
    return writableDatabase.insert(TABLA_AVISTAMIENTOS, null, values)
}
```

**El dueño ve el contador y la lista**:

```kotlin
// MascotasPerdidasActivity.kt
val todosAvist = db.obtenerAvistamientos()
val avistPorId = todosAvist.groupBy { it.publicacionId }   // agrupa por mascota

// En la tarjeta se muestra:
val avistCount = avistPorPublicacion[p.id]?.size ?: 0
holder.tvAvist.text = "👀 $avistCount avistamiento${if (avistCount > 1) "s" else ""}"
```

**Pines amarillos en el mapa**:

```kotlin
// MainActivity.kt
m.icon = ContextCompat.getDrawable(this, R.drawable.ic_pin_amarillo)
m.relatedObject = avistamiento
m.setOnMarkerClickListener { marker, _ ->
    mostrarPanelAvistamiento(a, nombres[a.publicacionId] ?: "la mascota")
}
```

---

## 6. Adopciones

**Cargar solo publicaciones tipo "Adopcion"**:

```kotlin
publicaciones = db.obtenerPublicaciones().filter { it.tipo == "Adopcion" }
```

**Buscador y chips filtran en memoria**:

```kotlin
val filtradas = publicaciones.filter { p ->
    val coincideNombre = p.nombre.lowercase().contains(texto) ||
            p.descripcion.lowercase().contains(texto)
    val coincideEspecie = filtroEspecie == null ||
            p.especie?.equals(filtroEspecie, ignoreCase = true) == true
    coincideNombre && coincideEspecie
}
rvAnimales.adapter = AdopcionesAdapter(filtradas) { publicacion ->
    startActivity(Intent(this, DetalleAdopcionActivity::class.java).apply {
        putExtra(DetalleAdopcionActivity.EXTRA_PUBLICACION_ID, publicacion.id)
    })
}
```

**Favoritos guardados en preferencias**:

```kotlin
// FavoritosManager.kt
fun toggle(context: Context, publicacionId: Long): Boolean {
    val set = prefs(context).getStringSet(KEY_FAVS, emptySet())!!.toMutableSet()
    val key = publicacionId.toString()
    val agregado = if (set.contains(key)) { set.remove(key); false }
    else { set.add(key); true }
    prefs(context).edit().putStringSet(KEY_FAVS, set).apply()
    return agregado
}
```

**Ver ubicación → abre el mapa principal centrado**:

```kotlin
// DetalleAdopcionActivity.kt
startActivity(Intent(this, MainActivity::class.java).apply {
    putExtra("centrar_lat", p.latitud)
    putExtra("centrar_lng", p.longitud)
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
})
```

```kotlin
// MainActivity.kt — recibe y centra
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    centrarEnPunto(intent)
}
```

---

## 7. Resolver una alerta

```kotlin
fun marcarResuelta(id: Long) {
    val values = ContentValues().apply { put("estado", "Resuelta") }
    writableDatabase.update(TABLA_PUBLICACIONES, values, "id = ?", arrayOf(id.toString()))
}
```

```kotlin
// MainActivity.kt — al tocar el botón del panel
executor.execute {
    DatabaseHelper(this).marcarResuelta(publicacion.id)
    runOnUiThread {
        Toast.makeText(this, "¡Alerta resuelta!", Toast.LENGTH_SHORT).show()
        ocultarPanel()
        cargarAlertasDesdeBD()   // recarga → el pin se vuelve gris
    }
}
```

---

## 8. Cómo evito que la app se congele

Toda consulta a la BD va en **hilo aparte** y el resultado vuelve al hilo principal:

```kotlin
private val executor = Executors.newSingleThreadExecutor()

executor.execute {              // hilo aparte → consulta BD
    val resultado = DatabaseHelper(this).obtenerPublicaciones()
    runOnUiThread {             // vuelve a la pantalla
        renderAlertas(resultado)
    }
}
```

---

## 9. El patrón de todo el programa

```
Acción del usuario → guardar en SQLite → volver a leer → redibujar pantalla
```

Se repite en: publicar, avistar, resolver, favoritos, login y registro.  
La función que recarga siempre es `cargarAlertasDesdeBD()`, que se llama después de cada cambio y también en `onResume()` (al volver a la app):

```kotlin
override fun onResume() {
    super.onResume()
    map.onResume()
    cargarAlertasDesdeBD()   // siempre al día
}
```

---

## Resumen para decir en 2 minutos

> *"La app funciona con un patrón simple: cada acción del usuario se guarda en una base de datos SQLite, y después la pantalla se recarga desde esa base. El mapa lee las publicaciones y las pinta según su tipo, el long-press captura coordenadas exactas para publicar, los avistamientos se guardan como registros separados que se cuentan y se muestran como pines amarillos, y las contraseñas se protegen con hash y sal. Todo en Kotlin, sin dependencias externas de pago."*
