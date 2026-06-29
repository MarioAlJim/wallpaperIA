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
    *   Se incrementó la amplitud del rastro de refracción ondulante a `0.015` en horizontal y `0.004` en vertical (un aumento del doble) para hacer el rastro mojado mucho más perceptible.
    *   El grosor mínimo del rastro se mantiene al 75% del tamaño de la gota (`radius * 0.75`).
*   **Dirección Síncrona hacia Abajo**:
    *   Ambos shaders alinean el eje Y de texturas de arriba a abajo, evitando refracciones en sentidos opuestos.

### 4. Apilamiento y Paralaje de Nubes (Distinct Cloud Depth Parallax)
*   **Técnica de Paralaje Tridimensional por Giroscopio**:
    *   Conforme a la aclaración sobre el apilamiento de nubes, en lugar de renderizar la misma nube múltiples veces en capas, renderizamos cada instancia de nube individual una sola vez en su posición natural con un desplazamiento de paralaje dinámico dependiente de su profundidad Z.
    *   **Cálculo de Desplazamiento**: Calculamos un factor de profundidad `depthFactor = 0.2f + 0.8f * ((cloud.z - 0.3f) / 0.7f)` basado en la coordenada `z` (0.3 a 1.0) de cada nube.
*   **Técnica de Paralaje Tridimensional y Ordenamiento por Capas**:
    *   Conforme a la aclaración sobre el apilamiento de nubes, en lugar de renderizar la misma nube múltiples veces en capas, renderizamos cada instancia de nube individual una sola vez en su posición natural con un desplazamiento de paralaje dinámico dependiente de su profundidad Z.
    *   **Cálculo de Desplazamiento**: Calculamos un factor de profundidad `depthFactor = 0.2f + 0.8f * ((cloud.z - 0.3f) / 0.7f)` basado en la coordenada `z` (0.3 a 1.0) de cada nube.
    *   Las nubes del primer plano (`z` cercano a 1.0) responden más rápido y con mayor rango al giroscopio, mientras que las nubes lejanas se mueven más lentamente, logrando el efecto tridimensional.
    *   **Ordenamiento por Capas Verticales (50% a 90% Y)**: En [StormCloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/StormCloud.kt), los Y-centers de las nubes se distribuyen y ordenan en capas desde el 50% de la pantalla (`Y = 0.0f` en NDC, para las nubes de hasta atrás) hasta el 90% de la pantalla (`Y = 0.8f` en NDC, para las nubes más altas y cercanas) en base a su valor `z`.
    *   **Variabilidad de Tamaños**: El tamaño de la nube (`baseScale`) se calcula de forma independiente de `z`, brindando una mayor variabilidad visual en cualquier plano de profundidad.
    *   **Gradiente Vertical Dinámico**: En [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag), se introdujo la variable `uHeightFactor` que representa la posición vertical de la nube en la pantalla. Multiplicamos el color de la nube por un gradiente de abajo (oscuro) a arriba (claro) que responde a su altura: las nubes localizadas en la parte inferior (hasta atrás, `uHeightFactor = 0.0`) tienen un gradiente predominantemente blanco y claro (de 0.85 a 1.25), y a medida que las capas son más altas en el eje Y y cercanas al frente (`uHeightFactor = 1.0`), su gradiente se vuelve significativamente más oscuro (de 0.15 a 0.5).
*   **Ajuste del Límite de Nubes en Configuraciones**:
    *   Para mantener un rendimiento óptimo de renderizado, limitamos el número máximo de nubes configurables en pantalla a **10 nubes**.
    *   **[MODIFY] [StormSettingsFragment.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormSettingsFragment.kt)**: Se actualizaron los arreglos `cloudOptions` (de 0 a 10 nubes) e intArray `cloudCounts` (0, 1, 2, 4, 6, 8, 10). Se adaptó el mapeo en `getClosestCloudOptionIndex` para reflejar un máximo de 10.
    *   **[MODIFY] [fragment_storm_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/fragment_storm_settings.xml)**: Se actualizó el texto informativo de ayuda a "(0 a 10 nubes)".
    *   **[MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/SceneManager.kt)**: Se expuso `configProvider` de forma pública y se modificó `adjustClouds` para limitar el recuento máximo a 10 nubes y mapear de manera proporcional el porcentaje de densidad.
*   **Pruebas Unitarias (`SceneManagerTest.kt`)**:
    *   Se actualizó la prueba de densidad `testCloudDensityMapping()` para validar que al 100% de densidad se instancien exactamente 10 nubes.
    *   Se actualizó `testCloudDepthAndParallax()` para verificar la validez del rango de tamaño en la escala de las nubes ahora de manera independiente de `z`.

### 5. Rayos de Sombra (Dark God Rays)
*   **[MODIFY] [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag)** & **[NEW] [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag)**:
    *   Se implementó `GetDarkGodRays` que realiza un ray-marcher isotrópico de 10 muestras en coordenadas NDC hacia la posición del relámpago más brillante. Mide la densidad del volumen nuboso usando ruido FBM y proyecta sombras radiales oscuras.

### 6. Iluminación Ambiental Selectiva (Rim Lighting)
*   **[MODIFY] [background.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.vert)** & [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag):
    *   Aplica un golpe de luz de borde (Rim Light) brillante del color del relámpago en el borde superior de la silueta montañosa durante los milisegundos del flash.

### 7. Tormenta Variable (Ciclos de Ráfagas) - ¡Transición Suave Hermite!
*   **Ciclos**: Calma (60s) / Ráfaga (30s).
*   **Interpolación Hermite (Smoothstep)**: Transición suavizada de 15 segundos mediante la curva `smoothFactor = x * x * (3.0 - 2.0 * x)`.

### 8. Lluvia de Nubes Uniforme y de Mayor Tamaño
*   **[MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/RainDrop.kt)**:
    *   **Uniformidad por Nube**: Las gotas que nacen de una nube heredan exactamente la profundidad `z` de su nube progenitora (`z = spawnCloud.z`), garantizando que todas las partículas de lluvia de esa misma nube tengan el mismo tamaño (largo y ancho) y comportamiento.
    *   **Incremento del 100% de Tamaño**: Aumentamos en un 100% el tamaño mínimo y longitud de las gotas nacidas de las nubes en relación con el tamaño de la nube, duplicando el factor base de escala de `0.06f` a `0.12f`.

### 9. Aumento de la Opacidad de las Nubes en un 100%
*   **[MODIFY] [StormCloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/StormCloud.kt)**:
    *   **Incremento del 100% (Doble Opacidad)**: Modificamos el cálculo de `targetOpacity` en `reset()` para multiplicar el rango base por `2.0f` y aplicar una coerción superior: `((Random.nextFloat() * 0.3f + 0.4f) * 2.0f * (0.6f + 0.4f * z)).coerceAtMost(1.0f)`. Esto incrementa la opacidad en un 100% (duplicándola con tope de 1.0), logrando que todas las nubes se muestren mucho más definidas, robustas y sólidas en pantalla.
*   **[MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/test/java/com/wolf/wallpaper/storm/SceneManagerTest.kt)**:
    *   Se actualizaron las aserciones del test `testCloudDepthAndParallax()` para validar esta escala duplicada con tope en 1.0f.

### 10. Separación de Generación de Lluvia (Nubes vs. Borde)
*   **[MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/RainDrop.kt)**:
    *   **Separación del Flujo**: Dividimos la lógica física del método `reset()` delegando en dos métodos privados independientes (`resetCloudRain` y `resetEdgeRain`). Esto aísla por completo las gotas generadas desde las nubes (que heredan el tamaño y profundidad de su respectiva nube progenitora) de las gotas de lluvia normales nacidas del borde superior de la pantalla, posibilitando ajustar de forma totalmente individual cada tipo de lluvia.
    *   **Rango de Dispersión Acotado**: Modificamos el offset horizontal de spawn en `resetCloudRain` para que las gotas se generen estrictamente en la franja del 25% al 75% de la extensión total lateral de la nube, evitando que se generen o caigan fuera de su contorno físico.

### 11. Forma Realista de Gota, Degradado de Transparencia y Largo +40%
*   **[MODIFY] [rain.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/rain.frag)**:
    *   **Degradado de Transparencia**: El `verticalGradient` ahora usa la fórmula `0.10 + 0.90 * pow(y, 0.4)`. La cabeza redonda (y=1) mantiene 100% de opacidad/saturación, mientras la cola puntiaguda (y=0) se desvanece a 90% de transparencia (10% de opacidad), creando un efecto de gota real con la punta disolviéndose en el aire.
*   **[MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/RainDrop.kt)**:
    *   **Largo +40%**: Ambas funciones `resetCloudRain` (`0.12f → 0.168f`) y `resetEdgeRain` (multiplicador `* 1.4f`) aumentan el largo de las gotas en un 40%.
*   **[MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/test/java/com/wolf/wallpaper/storm/SceneManagerTest.kt)**:
    *   Actualización de las aserciones `testRainDropDepthAndScaling` para validar los nuevos rangos de longitud con el factor 1.4x.

### 12. Desplazamiento de Nubes por Deslizamiento (Swipe Parallax)
*   **[MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/StormRenderer.kt)**:
    *   **Detección de Deslizamiento**: Registramos el desplazamiento en `onOffsetsChanged(xOffset, yOffset)` definiendo `swipeOffset = (clampedX - 0.5f) * 2.0f`.
    *   **Movimiento de Nubes**: En el bucle de dibujo de las nubes, calculamos el desplazamiento horizontal del deslizamiento utilizando `cloudSwipeX = -swipeOffset * (aspectRatio * 0.5f) * depthFactor`. Esto desplaza las nubes hacia el lado al que el usuario desliza su dedo, adaptando la velocidad según la profundidad de la capa de la nube.

---

## Resultados de Pruebas y Verificación

1.  **Compilación de Gradle**:
    *   Comando: `.\gradlew.bat compileDebugSources`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los módulos vinculan limpiamente.
2.  **Pruebas Unitarias**:
    *   Comando: `.\gradlew.bat test`
    *   Resultado: **BUILD SUCCESSFUL**. Todos los tests (incluidos los mapeos y ciclos actualizados) pasaron de forma limpia.
