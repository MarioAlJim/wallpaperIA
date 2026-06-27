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

### 3. Gotas de Pantalla (Refraction) - ¡Fusión Orgánica Metaball y Rastros Potenciados!
*   **Fusión de Gotas Metaball (Smooth Blending)**:
    *   Reemplazamos el snap-merge con un modelo de mezcla metaball por campo de distancias. Las propiedades de refracción se mezclan ponderadamente según su altura local (`h_height`). Las gotas se fusionan de forma 100% fluida y continua sin glitches.
*   **Efecto de Rastro Potenciado**:
    *   Se incrementó la amplitud del rastro de refracción ondulante a `0.015` en horizontal y `0.004` en vertical (un aumento del doble frente a la versión previa) para hacer el rastro mojado mucho más perceptible.
    *   El grosor mínimo del rastro se mantiene al 75% del tamaño de la gota (`radius * 0.75`).
*   **Dirección Síncrona hacia Abajo**:
    *   Ambos shaders alinean el eje Y de texturas de arriba a abajo, evitando refracciones en sentidos opuestos.

### 4. Apilamiento de Capas de Nubes (Cloud Slicing 3D) - ¡NUEVA FUNCIÓN!
*   **Técnica de Slicing en Renderizado (`StormRenderer.kt`)**:
    *   Para dar un efecto volumétrico denso y realista de nubes 3D sin el costo de Raymarching, modificamos `drawClouds` para dibujar **4 capas de renderizado** por cada nube individual (de atrás hacia adelante).
    *   **Auto-Sombreado**: Las capas posteriores son progresivamente más oscuras (`shadeFactor = 1.0 - i * 0.12`), simulando la oclusión y profundidad de la luz dentro del volumen.
    *   **Espesor Estático**: Se aplica un desplazamiento estático mínimo por capa (`staticShiftX`, `staticShiftY`) proporcional al tamaño de la nube, dándole espesor visible incluso en estado quieto.
    *   **Paralaje por Giroscopio**: Las capas se desplazan dinámicamente según la inclinación del giroscopio (`sensorTiltX`, `sensorTiltY`) con factores proporcionales. Al mover el teléfono, las capas se deslizan una sobre otra, generando una sensación tridimensional premium.
*   **Ajuste del Límite de Nubes en Configuraciones**:
    *   Para optimizar el rendimiento y compensar el renderizado multicapa de las nubes (4x llamadas por nube), redujimos y ajustamos el número máximo de nubes configurables en pantalla a **10 nubes**.
    *   **[MODIFY] [StormSettingsFragment.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormSettingsFragment.kt)**: Se actualizaron los arreglos `cloudOptions` (de 0 a 10 nubes) e intArray `cloudCounts` (0, 1, 2, 4, 6, 8, 10). Se adaptó el mapeo en `getClosestCloudOptionIndex` para reflejar un máximo de 10.
    *   **[MODIFY] [fragment_storm_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/fragment_storm_settings.xml)**: Se actualizó el texto informativo de ayuda a "(0 a 10 nubes)".
    *   **[MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/SceneManager.kt)**: Se expuso `configProvider` de forma pública y se modificó `adjustClouds` para limitar el recuento máximo a 10 nubes y mapear de manera proporcional el porcentaje de densidad.
*   **Pruebas Unitarias (`SceneManagerTest.kt`)**:
    *   Se actualizó la prueba de densidad `testCloudDensityMapping()` para validar que al 100% de densidad se instancien exactamente 10 nubes en la tormenta.

### 5. Rayos de Sombra (Dark God Rays)
*   **[MODIFY] [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag)** & **[NEW] [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag)**:
    *   Se implementó `GetDarkGodRays` que realiza un ray-marcher isotrópico de 10 muestras en coordenadas NDC hacia la posición del relámpago más brillante. Mide la densidad del volumen nuboso usando ruido FBM y proyecta sombras radiales oscuras.

### 6. Iluminación Ambiental Selectiva (Rim Lighting)
*   **[MODIFY] [background.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.vert)** & [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag):
    *   Aplica un golpe de luz de borde (Rim Light) brillante del color del relámpago en el borde superior de la silueta montañosa durante los milisegundos del flash.

### 7. Tormenta Variable (Ciclos de Ráfagas) - ¡Transición Suave Hermite!
*   **Ciclos**: Calma (60s) / Ráfaga (30s).
*   **Interpolación Hermite (Smoothstep)**: Transición suavizada de 15 segundos mediante la curva `smoothFactor = x * x * (3.0 - 2.0 * x)`.

---

## Resultados de Pruebas y Verificación

1.  **Compilación de Gradle**:
    *   Comando: `.\gradlew.bat compileDebugSources`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los módulos vinculan limpiamente.
2.  **Pruebas Unitarias**:
    *   Comando: `.\gradlew.bat test`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los tests (incluidos los mapeos y ciclos actualizados) pasaron de forma limpia.
