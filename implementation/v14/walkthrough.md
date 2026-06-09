# Walkthrough – Nubes Dinámicas e Integración del Viento

Hemos completado la implementación del sistema de nubes dinámicas en el fondo de pantalla animado. A partir de ahora, las nubes se basan en los 14 archivos PNG existentes en assets, varían de tamaño de forma aleatoria, responden a la densidad de nubes configurada y se desplazan continuamente de un lado a otro de la pantalla de acuerdo con la dirección y la intensidad del viento seleccionadas.

## Cambios Realizados

### 1. Entidad Nube (`Cloud.kt`)
* Se reemplazó la velocidad estática `speed: Float` por un factor individual `speedFactor: Float` (entre `0.8` y `1.2`). Esto agrega sensación de profundidad (paralaje) cuando las nubes se mueven a distintas velocidades individuales basándose en una velocidad global.
* Se agregó la función `update(deltaTime: Float, windSpeed: Float)` que desplaza la nube horizontalmente en función de la velocidad global del viento.
* Se modificó `reset()` para establecer un tamaño (scale) aleatorio de la nube entre `0.3` y `0.9` y aleatorizar su factor de velocidad.

### 2. Gestión de la Escena (`SceneManager.kt`)
* Se añadió `getCloudTextureCount()` para contar dinámicamente cuántas texturas de nubes hay en la carpeta assets.
* En `adjustClouds()`, se inicializan las nubes cargando texturas aleatorias en el rango completo detectado dinámicamente.
* En `onSurfaceChanged()`, se re-habilitó el re-posicionamiento de las nubes al rotar la pantalla.
* En `update()`, se habilitó la actualización física de las nubes. Se calcula la velocidad del viento global para las nubes basándose en la intensidad y dirección del viento de la configuración, y se implementó un sistema de envolvente de pantalla (screen wrapping) bilateral: si la nube viaja hacia la derecha y sale de la pantalla, reaparece en el extremo izquierdo; si viaja hacia la izquierda y sale, reaparece en el extremo derecho.

### 3. Renderizado OpenGL (`StormRenderer.kt`)
* Se modificó el arreglo de texturas `cloudTextures` para que sea una lista dinámica mutable.
* En `onSurfaceCreated()`, se escanea el directorio `assets/clouds` para cargar todos los archivos PNG dinámicamente y ordenarlos alfabéticamente para mantener consistencia.
* En `drawFrame()`, se habilitó la llamada a `drawClouds(sceneManager.getClouds())`.
* Se actualizó `drawClouds()` para usar la lista dinámica de texturas de forma segura aplicando el módulo del tamaño de la lista: `cloudTextures[cloud.textureIndex % cloudTextures.size]`.

### 4. Pruebas Unitarias (`SceneManagerTest.kt`)
* Se añadió el caso de prueba `testCloudWindAndWrapping()` que simula:
  1. Movimiento correcto de las nubes hacia la derecha bajo viento en dirección derecha.
  2. Movimiento correcto de las nubes hacia la izquierda bajo viento en dirección izquierda.
  3. Envolvente (wrapping) correcto de pantalla en ambos casos cuando la nube supera los límites laterales (utilizando la fórmula de la mitad del ancho del objeto para asegurar suavidad al salir de pantalla).

## Commits Sugeridos

- `feat(cloud): refactor Cloud properties to use speedFactor and dynamic scaling 0.3..0.9`
- `feat(scene): enable cloud updates with wind-driven horizontal speed and screen wrapping`
- `feat(renderer): load all cloud textures dynamically and enable cloud rendering`
- `test(scene): add test cases for cloud movement, wind direction, and screen wrapping`

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite completa de unit tests exitosamente:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` con todos los tests de nubes, lluvia, y rayos validados correctamente.
