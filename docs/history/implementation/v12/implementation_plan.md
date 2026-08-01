# Plan de Implementación – Configuración de Duración de los Rayos

Implementaremos la capacidad de personalizar la duración de la animación de los rayos desde la pantalla de configuración del fondo de pantalla. El usuario podrá regular la duración (entre 150 ms y más de 1.1 segundos) mediante un control deslizante (SeekBar) que persistirá este valor en SharedPreferences.

## User Review Required

- Se agregará una nueva tarjeta en la pantalla de Ajustes ("Duración de los Rayos") con un control SeekBar de 0% a 100%.
- La duración estándar actual (~250-350ms) se ubicará alrededor del 30% en el control deslizante, permitiendo acortarla (hasta 150ms en 0%) o alargarla progresivamente hasta superar 1 segundo (en 100%).

---

## Proposed Changes

### Capa de Configuración y SharedPreferences

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Agregar la función `fun getLightningDuration(): Int` a la interfaz.

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Agregar la clave `KEY_LIGHTNING_DURATION = "lightning_duration"` y el valor predeterminado `DEFAULT_LIGHTNING_DURATION = 30` (aproximadamente 300 ms).
- Implementar `getLightningDuration()` y `setLightningDuration(duration: Int)`.

### Capa de Físicas y Lógica de Escena

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Modificar el método `trigger(aspectRatio, textureCount, colorIndex, durationPercentage)` para recibir el porcentaje de duración.
- Implementar un mapeo de la duración basado en el porcentaje:
  - De 0% a 50%: Rango de base entre `0.15s` y `0.30s`.
  - De 50% a 100%: Rango de base entre `0.30s` y `1.00s`.
  - Añadir una variación aleatoria de `0.15s` (`baseMax = baseMin + 0.15f`).
  - Calcular la duración como `duration = Random.nextFloat() * (baseMax - baseMin) + baseMin`.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Al disparar un rayo inactivo en `update()`, leer `configProvider.getLightningDuration()` y pasarlo al método `trigger()`.

### Capa de Interfaz de Usuario (UI)

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Agregar un nuevo `CardView` debajo del selector "Color de los Rayos" para regular la "Duración de los Rayos" con un `SeekBar` (`max="100"`).

#### [MODIFY] [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml)
- Agregar el recurso `@string/lightning_duration_label` ("Duración de los Rayos").

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Enlazar y configurar `seekBarLightningDuration`.
- Implementar la actualización dinámica de texto en `updateTextView()` indicando descriptores textuales amigables:
  - 0% a 20%: "Corto" (ej. "15% (Corto)")
  - 21% to 60%: "Normal"
  - 61% to 85%: "Largo"
  - 86% to 100%: "Extremo"

---

## Plan de Verificación

### Pruebas Automatizadas
- Actualizar `MockConfigProvider` en [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt) para implementar `getLightningDuration()`.
- Agregar un test unitario `testLightningDurationMapping` que verifique que el porcentaje se traduce correctamente a la duración esperada de la animación (ej. verificar que al 0% está en $[0.15s, 0.30s]$, al 50% en $[0.30s, 0.45s]$ y al 100% en $[1.00s, 1.15s]$).
- Correr `./gradlew testDebugUnitTest`.

### Verificación Manual
- Abrir la pantalla de ajustes, seleccionar la duración al 0% y verificar que los rayos son destellos muy rápidos.
- Seleccionar al 100% y verificar que las animaciones de los rayos persisten de forma prolongada (aproximadamente un segundo de duración).
