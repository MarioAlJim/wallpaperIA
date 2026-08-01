# Plan de Refinamiento de Gotas de Lluvia y Ocultación de Nubes/Rayos

Este plan describe las modificaciones necesarias para refinar el comportamiento de la lluvia en el wallpaper animado (haciendo que caiga en diagonal y a intervalos/frecuencia aleatoria con 4 niveles de densidad configurables) y ocultar temporalmente los efectos y opciones de configuración relacionados con las nubes y los rayos.

## User Review Required

> [!IMPORTANT]
> - Las funciones de nubes y rayos serán comentadas en el código del motor de física y de renderizado, y sus tarjetas de configuración correspondientes se ocultarán en la interfaz de usuario (`android:visibility="gone"`).
> - Se modificará la SeekBar de intensidad de lluvia para que solo permita seleccionar 4 niveles discretos: **Pocas (25%)**, **Media (50%)**, **Alta (75%)** y **Muy alta (100%)**.

## Proposed Changes

---

### Interfaz de Usuario y Configuración

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Ocultar las tarjetas de configuración de nubes (`seekBarCloudDensity`) y rayos (`seekBarLightningFrequency`) estableciendo su visibilidad a `gone`.
- Ajustar la SeekBar de intensidad de lluvia (`seekBarRainIntensity`) para tener `android:max="3"` y representar los 4 niveles discretos de densidad.

#### [MODIFY] [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml)
- Cambiar el texto de la etiqueta `rain_intensity_label` a "Densidad de Lluvia".

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Ajustar el setup del slider de lluvia para mapear los 4 niveles (0-3) a los valores correspondientes (25%, 50%, 75%, 100%) al guardar en SharedPreferences.
- Actualizar la interfaz de usuario para mostrar las descripciones correspondientes a cada nivel: "Pocas (25% - 250 gotas)", "Media (50% - 500 gotas)", "Alta (75% - 750 gotas)" y "Muy alta (100% - 1000 gotas)".

---

### Lógica de Simulación de Lluvia

#### [MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt)
- Modificar el constructor para soportar velocidad diagonal (`velocityX` y `velocityY`), la dirección normalizada (`dirX` y `dirY`) y un retardo de aparición (`spawnDelay`) para la frecuencia aleatoria.
- Implementar la actualización del retardo `spawnDelay`. Si es mayor que cero, la partícula se mantiene fuera de pantalla (ej. `positionY = 999f`) hasta que expire.
- Modificar la función `reset` para calcular un ángulo diagonal aleatorio (entre 20° y 30° respecto a la vertical), velocidad aleatoria (entre 3.0 y 4.5) y un `spawnDelay` aleatorio (entre 0 y 2.0 segundos).
- Ajustar el cálculo de la posición de generación `positionX` para cubrir la pantalla completa teniendo en cuenta la trayectoria diagonal (evitando esquinas vacías).
- Soportar el parámetro `startOnScreen` en `reset` para que las gotas iniciales se distribuyan de forma continua en pantalla y no haya una pantalla inicial vacía.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Modificar `adjustRain` y `onSurfaceChanged` para hacer uso del nuevo método `reset(aspectRatio, startOnScreen = true)` en la inicialización.
- Comentar las secciones que actualizan y reposicionan nubes y rayos dentro de `update` y `onSurfaceChanged`.

---

### Renderizado de la Escena

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Modificar `drawRain` para calcular las posiciones finales del segmento de línea usando la dirección diagonal normalizada (`dirX`, `dirY`) multiplicada por la longitud de la gota (`length`).
- Comentar las llamadas de dibujo `drawClouds` y `drawLightning` en `drawFrame`.
- Comentar el destello de fondo de los rayos (modificación del color de fondo en `drawFrame` when lightning is active).

## Verification Plan

### Manual Verification
- Compilar y ejecutar la aplicación en el dispositivo o emulador Android.
- Abrir la pantalla de configuración y comprobar que las opciones de Nubes y Rayos están ocultas.
- Verificar que el control de lluvia ahora tiene 4 niveles bien diferenciados con etiquetas claras de 25%, 50%, 75% y 100%.
- Abrir la previsualización del Wallpaper y verificar que:
  - Las gotas de lluvia caen notablemente en diagonal.
  - Las gotas caen de forma continua e irregular (frecuencia aleatoria) y no en bloques uniformes.
  - La densidad se corresponde visiblemente con el nivel seleccionado en la configuración.
  - Las nubes y los destellos/rayos no se visualizan.
