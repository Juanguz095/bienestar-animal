# Bienestar Animal

Aplicación Android para conectar a la comunidad con mascotas perdidas, encontradas y en adopción. Permite publicar alertas con ubicación en el mapa, reportar avistamientos de mascotas perdidas y gestionar adopciones.

## Funcionalidades

### Mapa de alertas
- Mapa basado en OpenStreetMap (sin API keys ni servicios de pago)
- Pines de colores según el tipo de alerta:
  - Rojo: mascota perdida
  - Verde: mascota encontrada
  - Naranja: mascota en adopción
  - Amarillo: avistamiento reportado
  - Gris: caso resuelto
- Círculo de búsqueda de 1.5 km alrededor de cada mascota perdida
- Botones de recentrar ubicación y zoom propio

### Publicaciones
- Mantener presionado el mapa para publicar en el punto exacto
- Foto desde la galería, nombre, descripción, especie y último lugar visto
- Estados: activa / resuelta

### Avistamientos comunitarios
- Cualquier usuario puede reportar dónde vio una mascota perdida
- Foto y descripción opcionales
- El dueño visualiza el contador de avistamientos en la lista y el detalle de cada uno
- Los avistamientos aparecen como pines amarillos en el mapa

### Adopciones
- Lista de animales en adopción con foto, especie y descripción
- Buscador y filtros por especie (perros / gatos / todos)
- Pantalla de detalle con favoritos y contacto por WhatsApp
- Enlace para ver la ubicación en el mapa de la aplicación

### Usuarios
- Registro e inicio de sesión con contraseñas encriptadas (SHA-256 con sal)
- Sesión persistente en el dispositivo
- Perfil de usuario y cierre de sesión

### Contacto y compartición
- Compartir alertas por WhatsApp con texto y enlace a la ubicación
- Compartir por cualquier otra aplicación instalada

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin (Android nativo) |
| Mapas | osmdroid (OpenStreetMap) |
| Base de datos | SQLite (SQLiteOpenHelper) |
| Encriptación | SHA-256 con sal aleatoria |
| Sesión y favoritos | SharedPreferences |

## Requisitos

- Android 8.0 (API 26) o superior
- Conexión a internet para cargar los mapas
- Android Studio (Arctic Fox o superior)

## Instalación

1. Abrir el proyecto en Android Studio (File → Open → seleccionar la carpeta del proyecto)
2. Esperar a que Gradle sincronice las dependencias
3. Conectar un dispositivo físico o iniciar un emulador
4. Ejecutar la aplicación (Run)

## Flujo de demostración

1. Registrar una cuenta con correo y contraseña
2. Mantener presionado un punto del mapa y publicar una mascota perdida con foto
3. Verificar que el pin rojo aparece en el mapa y que el panel de detalle muestra el círculo de búsqueda
4. Acceder a "Mascotas perdidas" y reportar un avistamiento sobre la publicación
5. Verificar el contador de avistamientos en la lista y el detalle de cada reporte
6. En "Adopciones", abrir el detalle de una mascota, agregarla a favoritos y compartirla por WhatsApp

## Estructura del proyecto

```
app/src/main/java/com/example/practicafinal/
├── MainActivity.kt                 Mapa principal con alertas
├── LoginActivity.kt                Inicio de sesión
├── RegistroActivity.kt             Creación de cuenta
├── AdopcionesActivity.kt           Lista de adopciones
├── DetalleAdopcionActivity.kt      Detalle con favoritos y WhatsApp
├── CrearPublicacionActivity.kt     Formulario de publicación
├── MascotasPerdidasActivity.kt     Lista de perdidas y avistamientos
├── ReportarAvistamientoActivity.kt Reporte de avistamiento
├── MenuOpcionesActivity.kt         Menú de navegación
├── PerfilActivity.kt               Perfil del usuario
├── model/                          Modelos de datos
├── db/                             DatabaseHelper (lógica SQLite)
├── session/                        Gestión de sesión y favoritos
├── adapters/                       Adaptadores RecyclerView
└── util/                           Utilidades
```

## Limitaciones conocidas

- Los datos se almacenan localmente en el dispositivo. Para compartir información entre usuarios se requiere un backend (por ejemplo, Firebase).
- La actualización de la versión de la base de datos reinicia los datos de ejemplo.
- Proyecto académico: práctica final.

## Documentación

- [EXPLICACION_LOGICA.md](EXPLICACION_LOGICA.md): explicación de la lógica con el código real
- [EXPLICACION_SIMPLE.md](EXPLICACION_SIMPLE.md): explicación en lenguaje claro
- [EXPLICACION_TECNICA.md](EXPLICACION_TECNICA.md): documentación técnica detallada
