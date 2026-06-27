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

### 3. Gotas de Pantalla (Refraction) - ¡Lente Original, Dirección Síncrona y Rastros!
*   **Efecto de Lente Original**: Se mantiene la lógica de lente convexa de la gota de agua para conservar la transparencia alta y la refracción nítida original.
*   **Rastros de Agua Mojada (Wet Trails)**:
    *   Se reintrodujo el cálculo de rastros en la función `GetDropletEffects`.
    *   El grosor del rastro se ha configurado para ser de **al menos el 75% del tamaño de la gota** (`float trailWidth = radius * 0.75`).
    *   Para dar paso a la renderización del rastro sin recortes, se amplió el límite vertical de descarte de fragmentos a `pos.y < -slideDist - radius`.
    *   El rastro aplica un efecto de refracción ondulante y se difumina gradualmente conforme se aleja de la gota hacia arriba (`trailFade`).
*   **Dirección Síncrona hacia Abajo**:
    *   En [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag), corregimos la orientación del eje vertical de `bgTexCoord` invirtiendo su valor: `bgTexCoord.y = 0.5 - (ndc.y / uBgScale.y) * 0.5`.
    *   Esto hace que tanto la textura de fondo como la de las nubes compartan exactamente la misma orientación espacial de texturas de arriba a abajo (donde Y=0 es la parte superior y Y=1 es la inferior).
    *   Al estar alinados, ambos shaders ejecutan el desplazamiento de gotas hacia abajo con el mismo efecto síncrono físico, **sin que haya refracciones ni deslizamientos en direcciones opuestas**.

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

### 6. Tormenta Variable (Ciclos de Ráfagas) - ¡Transición Suave Hermite!
*   **Aumento de Duración de los Ciclos**: Se ampliaron los tiempos de ciclo para evitar oscilaciones climáticas muy rápidas y artificiales:
    *   **Calma**: `60 segundos` de duración.
    *   **Ráfaga**: `30 segundos` de duración.
*   **Interpolación de Transición Hermite (Smoothstep)**:
    *   Se introdujo una variable de control `stormTransitionFactor` (de `0.0f` a `1.0f`) que cambia gradualmente a una tasa de `1.0f / 15.0f` por segundo (duración de **15 segundos**).
    *   Para eliminar cualquier aceleración o parada brusca en el viento, cantidad de lluvia o densidad nubosa, el valor de transición se suaviza mediante la curva Hermite de grado tres: `smoothFactor = x * x * (3.0 - 2.0 * x)`.
    *   Todos los parámetros climatológicos se mezclan progresivamente con `smoothFactor`, resultando en transiciones extremadamente suaves, fluidas y orgánicas.
*   **Modelo de Datos y Prefs (`ConfigProvider`, `ConfigManager`)**:
    *   Añadida la clave `"storm_variable_enabled"` para guardar la preferencia en el dispositivo.
    *   Se declararon los métodos `isStormVariableEnabled()` y `setStormVariableEnabled(Boolean)` en la API del sistema de configuraciones.
*   **Simulación por Ciclos (`SceneManager`)**:
    *   Se implementó un temporizador de ciclos en el motor de físicas de la tormenta (`stormCycleTime`).
    *   Cuando el modo está activo, se anula la lectura estática de SharedPreferences y se sobreescriben dinámicamente en tiempo real los targets.
*   **Diseño de la Interfaz (`fragment_storm_settings.xml`)**:
    *   Se agregó una tarjeta premium de configuración de color verde brillante al inicio de la pantalla con un switch (`switchStormVariable`) para encender o apagar el modo de tormenta variable.
*   **Control y Bloqueo Dinámico de la UI (`StormSettingsFragment.kt`)**:
    *   Se implementó `updateControlsLockState` que bloquea todos los controles individuales cuando el modo tormenta variable se activa.
    *   Los resúmenes de secciones en los acordeones se actualizan automáticamente para mostrar `"Ciclo Activo"`.
*   **Pruebas Unitarias (`SceneManagerTest.kt`)**:
    *   Se adaptó la prueba `testVariableStormCycle()` para validar la transición suave de 15 segundos y las nuevas duraciones del ciclo (60s/30s).

---

## Resultados de Pruebas y Verificación

1.  **Compilación de Gradle**:
    *   Comando: `.\gradlew.bat compileDebugSources`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los módulos Kotlin y shaders vinculan de forma limpia.
2.  **Pruebas Unitarias**:
    *   Comando: `.\gradlew.bat test`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los tests se ejecutaron con éxito.
