# Walkthrough – Nubes Dinámicas e Integración del Viento

Hemos completado la implementación del sistema de nubes dinámicas en el fondo de pantalla animado. Las nubes se basan en los 14 archivos PNG de assets, varían de tamaño de forma aleatoria, responden a la densidad configurada y se desplazan continuamente según la dirección e intensidad del viento.

Adicionalmente, se aplicaron las siguientes correcciones de diseño solicitadas:
1. **Dispersión vertical (Eje Y)**: Las nubes ahora se distribuyen a lo largo de toda la pantalla (eje Y de `-0.9` a `0.9`), en lugar de agruparse únicamente en la mitad superior.
2. **Posición delantera (Capas)**: Se cambió el orden de renderizado para dibujar las nubes al final del ciclo de renderizado, colocándolas delante de la lluvia y de los rayos.
3. **Incremento de tamaño**: Se aumentó el tamaño general de las nubes y sus variaciones en `0.2` (escalas entre `0.5` y `1.3`).

## Cambios Realizados

### 1. Entidad Nube (`Cloud.kt`)
* Se reemplazó la velocidad estática `speed: Float` por un factor individual `speedFactor: Float` (entre `0.8` y `1.2`). Esto agrega sensación de profundidad (paralaje).
* Se agregó la función `update(deltaTime: Float, windSpeed: Float)` que desplaza la nube horizontalmente en función de la velocidad global del viento.
* Se modificó `reset()` para establecer un tamaño (scale) aleatorio de la nube entre `0.5` y `1.3` (incremento general de 0.2) y dispersar su posición Y entre `-0.9` y `0.9`.

### 2. Gestión de la Escena (`SceneManager.kt`)
* Se añadió `getCloudTextureCount()` para contar dinámicamente cuántas texturas de nubes hay en la carpeta assets.
* En `adjustClouds()`, se inicializan las nubes cargando texturas aleatorias en el rango completo detectado dinámicamente.
* En `onSurfaceChanged()`, se re-habilitó el re-posicionamiento de las nubes al rotar la pantalla.
* En `update()`, se habilitó la actualización física de las nubes. Se calcula la velocidad del viento global para las nubes basándose en la intensidad y dirección del viento de la configuración, y se implementó un sistema de envolvente de pantalla (screen wrapping) bilateral.

### 3. Renderizado OpenGL (`StormRenderer.kt`)
* Se modificó el arreglo de texturas `cloudTextures` para que sea una lista dinámica mutable.
* En `onSurfaceCreated()`, se cargan dinámicamente y se ordenan alfabéticamente todos los PNG de `assets/clouds`.
* En `drawFrame()`, se cambió el orden de renderizado para llamar a `drawClouds` después de dibujar la lluvia y los rayos (dibujándolas al final para colocarlas al frente).
* Se actualizó `drawClouds()` para usar la lista dinámica de texturas de forma segura aplicando el módulo del tamaño de la lista: `cloudTextures[cloud.textureIndex % cloudTextures.size]`.

### 4. Pruebas Unitarias (`SceneManagerTest.kt`)
* Se añadió el caso de prueba `testCloudWindAndWrapping()` que valida el movimiento horizontal en ambas direcciones del viento y el correcto screen wrapping bilateral.

## Commits Realizados

- `feat(cloud): refactor Cloud properties to use speedFactor and dynamic scaling 0.3..0.9`
- `feat(scene): enable cloud updates with wind-driven horizontal speed and screen wrapping`
- `feat(renderer): load all cloud textures dynamically and enable cloud rendering`
- `test(scene): add test cases for cloud movement, wind direction, and screen wrapping`
- `fix(clouds): disperse clouds along Y axis, render them in front of rain/lightning, and increase scale by 0.2`

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite completa de unit tests exitosamente:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` con todos los tests de nubes, lluvia, y rayos validados correctamente.
