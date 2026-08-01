# Historial de Cambios - Versión 1.0 (Refinamiento de Lluvia y Ocultación)

En este incremento se ha rediseñado el comportamiento de las partículas de lluvia para que caigan en diagonal y a frecuencia aleatoria, y se han desactivado/ocultado temporalmente las funciones de nubes y rayos.

## Cambios Realizados

### Interfaz de Usuario y Configuración

#### [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Se añadieron propiedades `android:visibility="gone"` en las tarjetas de configuración de nubes y rayos para ocultarlas completamente del usuario.
- Se cambió el atributo `android:max` de la SeekBar de lluvia de `100` a `3` para admitir 4 niveles discretos de densidad de lluvia (Pocas, Media, Alta, Muy alta).

#### [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml)
- Se renombró la cadena `rain_intensity_label` a "Densidad de Lluvia".

#### [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Se implementó un listener personalizado para `seekBarRainIntensity` que intercepta los progresos del 0 al 3 y los guarda en SharedPreferences mapeados al 25%, 50%, 75% y 100%.
- Se creó el método auxiliar `updateRainTextView` para mostrar de manera descriptiva el nivel actual, el porcentaje y el número aproximado de gotas (Pocas, Media, Alta, Muy alta).

### Física y Lógica de Partículas

#### [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt)
- Se rediseñó la estructura de la clase y el método `reset()` para calcular velocidades diagonales `velocityX` y `velocityY` utilizando ángulos de 20° a 30° respecto a la vertical y velocidades aleatorias entre 3.0 y 4.5.
- Se incorporaron las variables de dirección normalizada `dirX` y `dirY` para simplificar y optimizar el cálculo del segmento de línea en el renderizador.
- Se implementó un retardo aleatorio `spawnDelay` (de hasta 2.0 segundos) en el reset de cada partícula para desincronizar sus caídas y generar un flujo de lluvia de frecuencia aleatoria continuo.
- Se añadió el flag `startOnScreen` en `reset()` para inicializar las partículas distribuidas a lo largo del eje Y en el arranque de la aplicación, evitando que la pantalla comience vacía.
- Se expandió el área de generación horizontal en `positionX` para cubrir la pantalla completa teniendo en cuenta el desplazamiento diagonal de las partículas al caer.

#### [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Se comentaron las llamadas de actualización y recolocación de nubes y rayos dentro de los métodos `update()` y `onSurfaceChanged()`.
- Se actualizó el método `adjustRain()` y la comprobación de salida de pantalla para reposicionar las partículas usando el nuevo flujo diagonal y reseteándolas con `startOnScreen = false`.

### Renderizado OpenGL

#### [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Se comentaron las llamadas de dibujo `drawClouds()` y `drawLightning()` dentro de `drawFrame()`.
- Se comentó la lógica de destello del color de fondo relacionado con los rayos en `drawFrame()`.
- Se reescribió el cálculo de las posiciones de vértices de la lluvia en `drawRain()` para dibujar líneas alineadas con el vector de dirección normalizado de la partícula: de `(positionX, positionY)` a `(positionX + dirX * length, positionY + dirY * length)`.
