# Walkthrough – Incremento de Respiración de Nubes y Control de Destellos (v20)

Hemos implementado un incremento del 50% en el efecto de respiración de tamaño de las nubes e incorporado una nueva opción de configuración en la pantalla de ajustes para activar/desactivar los destellos y relámpagos de fondo generados por los rayos.

## Cambios Realizados

### 1. Respiración de Nubes Aumentada
- **[Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)**:
  - Se aumentó la amplitud de la respiración senoidal en un 50% adicional sobre el valor anterior, quedando en ±28.125% (`0.28125f`):
    `scale = baseScale * (1.0f + sin(pulseTime) * 0.28125f * dynamicsSpeed)`

### 2. Configuración de Destellos de Rayos
- **[ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)**:
  - Declaramos: `fun isLightningFlashEnabled(): Boolean`.
- **[ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)**:
  - Añadimos la clave de preferencia `"lightning_flash_enabled"` (por defecto `true`).
  - Implementamos los métodos `isLightningFlashEnabled()` y `setLightningFlashEnabled()`.
- **[SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)**:
  - Expusimos `isLightningFlashEnabled()` para facilitar la consulta del renderizador.

### 3. Modificaciones en el Renderizado (StormRenderer)
- **[StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)**:
  - Se modificaron los métodos `drawBackground()`, `drawClouds()`, y `drawLightning()` para aceptar el parámetro `flashEnabled`.
  - Si `flashEnabled` es `false`, se omiten las iluminaciones adaptativas de color de fondo (`clearR, clearG, clearB`), los destellos internos en nubes y fondo (`maxIntensity = 0f`), y el quad de color de pantalla completa en `drawLightning()`. Las ramificaciones texturizadas de los rayos (el rayo en sí) se siguen dibujando normalmente.

### 4. Interfaz de Usuario y Ajustes
- **[activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)**:
  - Añadimos un interruptor `SwitchMaterial` con ID `switchLightningFlash` y texto `"Destellos de los Rayos"`.
- **[WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)**:
  - Vinculamos el interruptor con las preferencias de configuración y llamamos a `updateSummaries()` para mantener sincronizada la interfaz.

### 5. Pruebas Unitarias
- **[SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)**:
  - Implementamos `isLightningFlashEnabled()` en `MockConfigProvider`.
  - Actualizamos `testCloudDriftAndScaleOscillation()` para verificar el nuevo rango de oscilación del ±28.125% (`0.28126f`).
  - Añadimos la prueba `testLightningFlashToggleConfig()` para validar el interruptor de los destellos.
  - Mitigamos la inestabilidad en `testCloudNeutralWindWrapping()` incrementando los límites forzados a `±2.0f` para compensar el nuevo tamaño de respiración de las nubes.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas locales:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Las 21 pruebas unitarias pasaron satisfactoriamente.
