# Plan de Implementación – Incremento de Respiración de Nubes y Control de Destellos de Rayos (v20)

Este plan describe los cambios para aumentar la respiración de las nubes en un 50% (amplitud de ±28.125%) e implementar un interruptor en la pantalla de ajustes para activar/desactivar los destellos e iluminación completa de pantalla producidos por los rayos.

## User Review Required

> [!IMPORTANT]
> - **Incremento de Respiración**: En `Cloud.kt`, la amplitud de la oscilación del tamaño de las nubes se incrementará de `0.1875f` a `0.28125f` (un 50% adicional).
> - **Configuración de Destellos**: Se introduce una nueva preferencia booleana `lightning_flash_enabled` (por defecto `true`).
> - **Interfaz de Usuario**: Se añadirá un Switch de Material Design (`SwitchMaterial`) bajo la sección de "Rayos y Tormenta" en la pantalla de ajustes.
> - **Control de Renderizado (Destellos)**: Si el interruptor está desactivado, el renderizador (`StormRenderer`) dibujará los rayos (las ramificaciones texturizadas), pero omitirá los destellos de luz (el fogonazo de pantalla completa, la iluminación de fondo y la iluminación de nubes).

---

## Proposed Changes

### 1. Proveedor de Configuración

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Declarar: `fun isLightningFlashEnabled(): Boolean`

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Definir constantes:
  - `const val KEY_LIGHTNING_FLASH_ENABLED = "lightning_flash_enabled"`
  - `const val DEFAULT_LIGHTNING_FLASH_ENABLED = true`
- Implementar métodos:
  - `override fun isLightningFlashEnabled(): Boolean`
  - `fun setLightningFlashEnabled(enabled: Boolean)`

---

### 2. Modelo y Físicas de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- Actualizar la fórmula de escala con el nuevo valor de respiración aumentado en un 50% (`0.1875 * 1.5 = 0.28125f`):
  `scale = baseScale * (1.0f + sin(pulseTime) * 0.28125f * dynamicsSpeed)`

---

### 3. SceneManager

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Exponer el estado de los destellos:
  `fun isLightningFlashEnabled(): Boolean = configProvider.isLightningFlashEnabled()`

---

### 4. Renderizado OpenGL

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Modificar las firmas de dibujo para recibir `flashEnabled`:
  - `private fun drawBackground(backgroundIndex: Int, lightnings: List<Lightning>, flashEnabled: Boolean)`
  - `private fun drawClouds(clouds: List<Cloud>, lightnings: List<Lightning>, flashEnabled: Boolean)`
  - `private fun drawLightning(lightnings: List<Lightning>, flashEnabled: Boolean)`
- En `drawFrame()`, leer `sceneManager.isLightningFlashEnabled()` y pasarlo a las funciones de dibujo:
  - Si `flashEnabled` es `false`, omitir la coloración adaptativa del fondo claro (`clearR, clearG, clearB`).
- En `drawLightning()`, omitir la renderización del quad de destello de pantalla completa si `flashEnabled` es `false`.
- En `drawBackground()` and `drawClouds()`, filtrar o forzar `maxIntensity = 0f` para las luces flash si `flashEnabled` es `false`.

---

### 5. Pantalla de Ajustes

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Insertar un control `RelativeLayout` con `SwitchMaterial` (`switchLightningFlash`) y un `TextView` explicativo en el acordeón "Rayos y Tormenta" (dentro de `contentLightning`).

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Enlazar y configurar el switch `switchLightningFlash`:
  ```kotlin
  val switchLightningFlash = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchLightningFlash)
  if (switchLightningFlash != null) {
      switchLightningFlash.isChecked = configManager.isLightningFlashEnabled()
      switchLightningFlash.setOnCheckedChangeListener { _, isChecked ->
          configManager.setLightningFlashEnabled(isChecked)
          updateSummaries()
      }
  }
  ```

---

### 6. Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- En `MockConfigProvider`, mockear `isLightningFlashEnabled()` (por defecto `true`).
- Actualizar `testCloudDriftAndScaleOscillation()` para verificar la escala bajo el nuevo rango de ±28.125% (`0.28125f`).
- Añadir la prueba unitaria `testLightningFlashToggleConfig()` para verificar que `isLightningFlashEnabled()` del SceneManager cambia según la configuración.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
