# 🐾 Bienestar Animal — Documentación técnica

## 📱 ¿Qué es?

Aplicación Android para conectar a la comunidad con mascotas perdidas, encontradas y en adopción.  
Permite publicar alertas con ubicación en el mapa, reportar avistamientos y gestionar adopciones.

---

## 🧰 Stack tecnológico

| Componente | Herramienta | ¿Por qué? |
|---|---|---|
| **Lenguaje** | Kotlin | Nativo de Android, moderno y conciso |
| **Mapas** | osmdroid (OpenStreetMap) | Código abierto, sin API keys, funciona sin cuenta de Google |
| **Base de datos** | SQLite + SQLiteOpenHelper | Viene incluido en Android, cero dependencias |
| **Encriptación** | SHA-256 + sal aleatoria | Contraseñas seguras sin guardar texto plano |
| **Sesión** | SharedPreferences | Ligero, para guardar si el usuario ya inició sesión |
| **Favoritos** | SharedPreferences | Rápido, no necesita tabla extra |

> ⚠️ **No se usa Room** (la librería de Google). Se usa `SQLiteOpenHelper` directamente, que es más simple y no requiere procesadores de anotaciones (KSP/KAPT), evitando problemas de versiones.

---

## 🏗️ Arquitectura de la app

```
┌─────────────────────────────────────────┐
│  ACTIVITIES  (pantallas)                │
│  MainActivity · Login · Adopciones ...  │
└──────────────┬──────────────────────────┘
               │  usan
┌──────────────▼──────────────────────────┐
│  DatabaseHelper  (capa de datos)        │
│  SQLiteOpenHelper                       │
│  - usuarios                             │
│  - publicaciones                        │
│  - avistamientos                        │
└──────────────┬──────────────────────────┘
               │  devuelve
┌──────────────▼──────────────────────────┐
│  MODELOS  (data classes)                │
│  Usuario · Publicacion · Avistamiento   │
└─────────────────────────────────────────┘
```

---

## 🗄️ Base de datos (SQLite)

### ¿Cómo funciona?

`SQLiteOpenHelper` es una clase de Android que **crea y actualiza** la base de datos automáticamente.  
Cuando la app se instala por primera vez, llama a `onCreate()` que ejecuta los `CREATE TABLE`.

```kotlin
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "bienestar_animal.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, ...)")
        db.execSQL("CREATE TABLE publicaciones (...)")
        db.execSQL("CREATE TABLE avistamientos (...)")
    }
}
```

### Tablas

| Tabla | ¿Qué guarda? |
|---|---|
| `usuarios` | Nombre, correo, contraseña (encriptada), tipo |
| `publicaciones` | Tipo (perdida/encontrada/adopción), nombre, descripción, **foto**, último lugar visto, especie, coordenadas, fecha, estado |
| `avistamientos` | Publicación relacionada, coordenadas, descripción, **foto**, fecha |

### ¿Cómo se consulta?

Cada operación usa `readableDatabase` (lectura) o `writableDatabase` (escritura):

```kotlin
fun validarLogin(correo: String, contrasena: String): Usuario? {
    val cursor = readableDatabase.query(
        "usuarios", null, "correo = ?", arrayOf(correo), null, null, null
    )
    // ... recorrer el cursor y construir objeto Usuario ...
    cursor.close()
    return usuario
}
```

### ⚡ Operaciones en segundo plano

Todas las consultas a la base de datos se ejecutan en **hilos separados** con `Executors.newSingleThreadExecutor()`.  
Esto evita que la interfaz se congele mientras se lee o escribe en la BD.

```kotlin
executor.execute {
    val resultado = DatabaseHelper(context).obtenerPublicaciones()
    runOnUiThread {
        // Actualizar la interfaz con el resultado
    }
}
```

---

## 🔐 Seguridad: contraseñas

Las contraseñas **nunca se guardan en texto plano**. El proceso es:

1. Se genera una **sal aleatoria** de 16 bytes
2. Se concatena: `sal + contraseña`
3. Se aplica **SHA-256**
4. Se guardan el hash y la sal en la base de datos

```kotlin
private fun hashContrasena(contrasena: String, sal: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest("$sal$contrasena".toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { "%02x".format(it) }
}
```

Al iniciar sesión, se repite el proceso con la sal guardada y se compara el resultado.

---

## 🗺️ Mapas con osmdroid

### ¿Cómo funciona?

`osmdroid` es una librería que muestra mapas de **OpenStreetMap** (gratis, sin API keys).

```kotlin
// Configurar antes de inflar el layout
Configuration.getInstance().load(context, preferences)
Configuration.getInstance().userAgentValue = "MiApp/1.0 (email@ejemplo.com)"

// Fuente de azulejos (tiles)
mapa.setTileSource(XYTileSource("OpenStreetMap", 0, 19, 256, ".png",
    arrayOf("https://tile.openstreetmap.org/")
))

// Marcador
val marcador = Marker(mapa).apply {
    position = GeoPoint(-12.0464, -77.0428)
    icon = getDrawable(R.drawable.ic_pin_rojo)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
}
mapa.overlays.add(marcador)
```

### Gesto "mantener presionado"

Se usa `MapEventsOverlay` con `longPressHelper`:

```kotlin
mapa.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
    override fun longPressHelper(punto: GeoPoint): Boolean {
        // Abrir formulario con las coordenadas del punto presionado
        return true
    }
}))
```

### Tipos de pines

| Color | Significado |
|---|---|
| 🔴 Rojo | Mascota perdida |
| 🟢 Verde | Mascota encontrada |
| 🟠 Naranja | En adopción |
| 🟡 Amarillo | Avistamiento reportado |
| ⚪ Gris | Caso resuelto |

---

## 🧩 Flujo principal (ejemplo)

### Publicar una mascota perdida

```
1. Usuario mantiene presionado el mapa
2. Se abre CrearPublicacionActivity con las coordenadas
3. Completa: nombre + foto + último lugar visto + especie
4. Toca "Publicar"
5. Se guarda en la BD → aparece pin rojo en el mapa
```

### Avistamiento comunitario

```
1. Otro usuario ve el pin rojo en el mapa
2. Va a "Mascotas perdidas" → toca "👀 Vi esta mascota"
3. Abre mapa → toca donde la vio → puede agregar foto
4. Toca "Reportar avistamiento"
5. Se guarda → aparece pin amarillo en el mapa
6. El dueño ve "👀 X avistamientos" en su lista
```

---

## 📂 Estructura del proyecto

```
app/src/main/java/com/example/practicafinal/
├── MainActivity.kt          ← Mapa principal
├── LoginActivity.kt         ← Inicio de sesión
├── RegistroActivity.kt      ← Crear cuenta
├── AdopcionesActivity.kt    ← Lista de adopciones
├── DetalleAdopcionActivity.kt ← Detalle de una adopción
├── CrearPublicacionActivity.kt ← Formulario de publicación
├── MascotasPerdidasActivity.kt ← Lista de mascotas perdidas
├── ReportarAvistamientoActivity.kt ← Reportar donde se vio
├── MenuOpcionesActivity.kt  ← Menú flotante
├── PerfilActivity.kt        ← Perfil del usuario
├── model/
│   ├── Usuario.kt
│   ├── Publicacion.kt
│   └── Avistamiento.kt
├── db/
│   └── DatabaseHelper.kt    ← Toda la lógica de BD
├── session/
│   ├── SesionManager.kt     ← Persistencia de sesión
│   └── FavoritosManager.kt  ← Persistencia de favoritos
├── adapters/                ← Adaptadores para RecyclerView
└── util/                    ← Funciones de ayuda (fechas, imágenes)
```

---

## ✨ Lo que aprendí en este proyecto

- **SQLite sin Room**: manejar la base de datos directamente con `SQLiteOpenHelper`, cursores y consultas SQL
- **Mapas sin Google**: usar OpenStreetMap con `osmdroid`, gestionar marcadores, polígonos y eventos táctiles
- **Encriptación real**: SHA-256 con sal para contraseñas
- **Hilos con ExecutorService**: consultas a BD en segundo plano para no bloquear la UI
- **SharedPreferences**: para sesiones y datos simples (favoritos) sin necesidad de tablas
- **Diseño de UX completo**: desde cero, con Material Design, menús flotantes, paneles deslizantes y barras de herramientas
