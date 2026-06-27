# Walkthrough: Mejoras Visuales y Modo Tormenta Variable (v32)

Este documento detalla el trabajo realizado para implementar las mejoras visuales y la nueva funcionalidad de **Modo Tormenta Variable (Ciclos de Ráfagas)** en la rama `storm-effects-enhancement`.

---

## Cambios Realizados

### 1. Creación de la Rama Git
*   Se creó y cambió exitosamente a la rama de desarrollo `storm-effects-enhancement`.

### 2. Desacoplamiento de Shaders de Nubes
*   **[NEW] [storm_cloud.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.vert)**: Pasa las posiciones en pantalla `vScreenPos` al fragment shader.
*   **[NEW] [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag)**: Fragment shader local que mapea coordenadas NDC, calcula distorsión refractiva síncrona con rastros en pantalla y aplica el ensombrecimiento volumétrico de los Dark God Rays.
*   **[MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/StormRenderer.kt)**: Se modificó para cargar `storm_cloud.vert` y `storm_cloud.frag` en lugar de los shaders compartidos de `:core`, protegiendo el modo Soleado de regresiones.

### 3. Gotas de Pantalla con Rastro (Trail Refraction)
*   **Restauración del Efecto de Lente**: Se revirtió el uso de `screenUV` como coordenada de renderizado de las gotas en [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag) al uso original de `vTexCoord`. Esto mantiene intacta la proporción geométrica, tamaño de cuadrícula y aspecto visual original de las lentes de refracción.
*   **Sincronización Avanzada en Nubes**:
    *   Se agregaron los uniformes `uBgScale` (escala del fondo) y `uCloudScale` (escala de la nube individual) en [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag).
    *   El fragment shader de nubes convierte las coordenadas NDC de pantalla a las coordenadas de textura del fondo: `bgTexCoord = vec2(ndc.x * uAspectRatio / uBgScale.x, ndc.y / uBgScale.y) * 0.5 + 0.5`.
    *   Calcula el efecto de refracción en ese espacio común y luego escala la distorsión al espacio local de textura de la nube: `droplet.xy * (uBgScale * 2.0 / uCloudScale)`.
    *   Esto garantiza un acoplamiento perfecto de distorsión visual al pasar nubes detrás de las gotas, sin alterar la escala nativa de las lentes.
*   **Dirección de Desplazamiento**: Se corrigió el error de signo en `pos.y` (cambiando `pos.y -= ...` por `pos.y += ...`) de modo que la inversión del sistema de coordenadas UV de OpenGL no haga que algunas gotas se desplacen hacia arriba. Ahora **todas las gotas se deslizan hacia abajo**.
*   **Bucle de Rango**: Se cambió el rango de iteración del bucle Y en la búsqueda de vecinos de `for (int y = -9; y <= 1; y++)` a `for (int y = -1; y <= 9; y++)` para que se verifiquen correctamente las celdas superiores desde las cuales las gotas caen sobre la celda actual.

### 4. Rayos de Sombra (Dark God Rays)
*   **[MODIFY] [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag)** & **[NEW] [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag)**:
    *   Se implementó `GetDarkGodRays` que realiza un ray-marcher isotrópico rápido de 10 muestras en coordenadas NDC hacia la posición del relámpago más brillante (`uLightningPos`).
    *   Mide la densidad del volumen nuboso usando ruido FBM y proyecta sombras radiales oscuras que atenúan la iluminación ambiental y el cielo durante los relámpagos.

### 5. Iluminación Ambiental Selectiva (Rim Lighting)
*   **[MODIFY] [background.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.vert)**: Añadido `out vec2 vScreenPos` para corregir la discrepancia de interfaz del shader y pasar coordenadas válidas.
*   **[MODIFY] [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag)**:
    *   Calcula un offset de muestreo en dirección de proyección del rayo hacia la silueta: `rimOffset = normalize(vec2(vScreenPos.x - uLightningPos.x, 1.2)) * 0.008`.
    *   Detecta si el pixel actual está en la silueta y el pixel desplazado está en el cielo para identificar los bordes superiores expuestos a la luz del flash.
    *   Aplica un golpe de luz de borde (Rim Light) brillante del color del relámpago que dura únicamente los milisegundos del flash.

### 6. Tormenta Variable (Ciclos de Ráfagas) - ¡NUEVA FUNCIÓN!
*   **Modelo de Datos y Prefs (`ConfigProvider`, `ConfigManager`)**:
    *   Añadida la clave `"storm_variable_enabled"` para guardar la preferencia en el dispositivo.
    *   Se declararon los métodos `isStormVariableEnabled()` y `setStormVariableEnabled(Boolean)` en la API del sistema de configuraciones.
*   **Simulación por Ciclos (`SceneManager`)**:
    *   Se implementó un temporizador de ciclos en el motor de físicas de la tormenta (`stormCycleTime`).
    *   El ciclo transiciona automáticamente entre **Periodo de Calma** (20 segundos con lluvia fina, poco viento y pocos rayos) y **Periodo de Ráfaga** (10 segundos con lluvia torrencial, viento huracanado de dirección aleatoria y relámpagos constantes).
    *   Cuando el modo está activo, se anula la lectura estática de SharedPreferences y se sobreescriben dinámicamente en tiempo real los targets de:
        *   Densidad de nubes
        *   Intensidad de lluvia
        *   Velocidad de lluvia
        *   Frecuencia de rayos
        *   Frecuencia de destellos en nubes
        *   Dirección e intensidad del viento
*   **Diseño de la Interfaz (`fragment_storm_settings.xml`)**:
    *   Se agregó una tarjeta premium de configuración de color verde brillante al inicio de la pantalla con un switch (`switchStormVariable`) para encender o apagar el modo de tormenta variable.
*   **Control y Bloqueo Dinámico de la UI (`StormSettingsFragment.kt`)**:
    *   Se implementó `updateControlsLockState` que bloquea (con `isEnabled = false` y opacidad reducida `0.4f`) todos los controles individuales que son definidos por el ciclo (deslizadores de viento, velocidad y frecuencia, y selectores de nubes y lluvia) cuando el modo tormenta variable se activa.
    *   Los resúmenes de secciones en los acordeones se actualizan automáticamente para mostrar `"Ciclo Activo"` para todos los parámetros bloqueados.
*   **Pruebas Unitarias (`SceneManagerTest.kt`)**:
    *   Se añadió la prueba `testVariableStormCycle()` que valida con precisión la simulación temporal y el correcto override de las variables físicas durante la calma y la ráfaga.

---

## Resultados de Pruebas y Verificación

1.  **Compilación de Gradle**:
    *   Comando: `.\gradlew.bat compileDebugSources`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los módulos Kotlin y shaders vinculan de forma limpia.
2.  **Pruebas Unitarias**:
    *   Comando: `.\gradlew.bat test`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los tests (incluido el nuevo de ciclo de tormenta variable) se ejecutaron con éxito.
