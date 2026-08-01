# Plan de Implementación – Nubes Dinámicas e Integración del Viento

Implementaremos y habilitaremos el sistema de nubes animadas en el fondo de pantalla. Las nubes se cargarán dinámicamente desde todos los archivos PNG de la carpeta `assets/clouds` (14 imágenes en total), variarán de tamaño de forma aleatoria, responderán a la configuración de densidad (cantidad) y se desplazarán de forma continua respondiendo a la dirección e intensidad del viento en la configuración.

## Proposed Changes

### Capa de Físicas y Entidades

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- Cambiar la propiedad `speed: Float` por `speedFactor: Float` (multiplicador aleatorio individual entre `0.8` y `1.2` para dar sensación de profundidad y paralaje).
- Implementar la función `update(deltaTime: Float, windSpeed: Float)` para desplazar la nube horizontalmente en función de la velocidad global del viento y su factor propio.
- En `reset()`, ampliar el rango de la escala (`scale`) de `0.3f` a `0.9f` para mayor variedad visual de tamaños, y definir `speedFactor` de forma aleatoria.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Agregar el método `getCloudTextureCount(): Int` para contar dinámicamente cuántos assets de nubes PNG existen.
- En `adjustClouds()`, usar el conteo dinámico de texturas para asignar un `textureIndex` aleatorio al instanciar nuevas nubes.
- Habilitar y actualizar el bucle de nubes en `update(deltaTime)`. Calcular la velocidad del viento para las nubes basándose en `windDirection` y `windIntensity`.
- Implementar el envolvente de pantalla (wrapping) para que las nubes que salgan por un lateral reaparezcan en el extremo opuesto, soportando tanto la dirección hacia la izquierda como hacia la derecha.

### Capa de Renderizado OpenGL

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Cambiar el arreglo estático `cloudTextures = IntArray(3)` por una lista mutable dinámica: `cloudTextures = mutableListOf<Int>()`.
- Cargar dinámicamente en `onSurfaceCreated()` todas las imágenes `.png` del directorio `assets/clouds`.
- Descomentar el renderizado de nubes en `drawFrame()`: `drawClouds(sceneManager.getClouds())`.
- Actualizar `drawClouds()` para que cargue y enlace de forma segura la textura usando `cloudTextures[cloud.textureIndex % cloudTextures.size]`.

---

## Plan de Verificación

### Pruebas Automatizadas
- Actualizar o añadir tests en [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt) para verificar que la cantidad de nubes corresponde a la densidad mapeada en SharedPreferences y que las nubes se desplazan de acuerdo a la dirección del viento.
- Ejecutar `./gradlew testDebugUnitTest`.

### Verificación Manual
- Abrir la aplicación y verificar que se visualizan las nubes flotando en el fondo.
- Mover el control de viento a la derecha y verificar que las nubes flotan hacia la derecha.
- Mover el control de viento a la izquierda y verificar que flotan hacia la izquierda.
- Aumentar la densidad de nubes y verificar que se agregan múltiples nubes de diferentes formas y tamaños en pantalla.
