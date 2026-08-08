# Explicación de Bienestar Animal

## ¿Con qué está hecha?

**Kotlin** para Android.  
Los mapas usan **OpenStreetMap**, que es gratis como Google Maps pero sin pagar.  
Los datos se guardan en **SQLite**, que es una base de datos que ya viene en todos los celulares Android.

---

## Lo más importante de entender

### 1. La base de datos

Imagina que la app tiene 3 **hojas de Excel** adentro del celular:

| Hoja | Guarda |
|---|---|
| **usuarios** | Quién se registró (nombre, correo, contraseña) |
| **publicaciones** | Cada animal perdido/encontrado/adopción (con foto, ubicación, estado) |
| **avistamientos** | Cada vez que alguien dice "yo lo vi aquí" (con foto, lugar) |

Cuando abres la app, lee esas hojas. Cuando publicas algo, escribe una fila nueva.

### 2. El mapa

Cada publicación es un **pin de color** en el mapa:
- 🔴 Perro perdido
- 🟢 Perro encontrado  
- 🟠 Animal en adopción
- 🟡 Alguien reportó haberlo visto

Tocas un pin → sale una tarjeta con la información y botones.

### 3. "Mantener presionado" para publicar

Si mantienes presionado el mapa, la app agarra **las coordenadas exactas de ese punto** y abre un formulario para que publiques un animal.

### 4. Avistamientos

Si ves un animal perdido, entras a la lista, tocas "Vi esta mascota", marcas en un mapa dónde fue, y eso se guarda como un **pin amarillo** nuevo. El dueño puede ver cuánta gente reportó haber visto a su mascota.

---

## ¿Cómo se conectan las piezas?

```
Pantalla → Consulta a la BD → Muestra datos

Ejemplo:
Adopciones → pedir publicaciones tipo "Adopcion" → BD devuelve lista → se muestran tarjetas
```

Las consultas a la base de datos se hacen **en segundo plano** (no congelan la pantalla).

---

## ¿Por qué no Firebase?

Porque necesita internet, cuenta de Google y configuración extra.  
Con SQLite la app funciona **sin internet**, los datos están en el teléfono y no hay que pagar nada.

---

## Preguntas típicas y respuestas cortas

**¿Por qué no usaste Room?**  
Room necesita complementos extra. SQLiteOpenHelper hace lo mismo y es más simple.

**¿Cómo proteges las contraseñas?**  
Se revuelven con un código aleatorio (sal) y se pasan por una función que las convierte en un texto sin sentido (SHA-256). No se guarda la contraseña real.

**¿Los datos se comparten entre usuarios?**  
En esta versión no — cada teléfono tiene su propia base. Para compartir entre usuarios se necesitaría Firebase o un servidor.

**¿Qué fue lo más difícil?**  
Los mapas. Coordinar que el long-press abra el formulario en el punto exacto, que los pines se actualicen al crear o resolver una alerta, y que los avistamientos aparezcan como pines amarillos separados.
