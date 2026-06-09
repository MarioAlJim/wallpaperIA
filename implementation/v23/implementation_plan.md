# Plan de Implementación – Separación de Ajustes de Destellos de Rayos y Destellos en Nubes (v23)

Este plan describe los cambios para independizar las configuraciones de destellos en pantalla completa (producidos por los rayos) de los destellos internos en las nubes. Esto permitirá activar los destellos internos en las nubes incluso si la animación de destellos en pantalla de los rayos está desactivada.

## User Review Required

> [!IMPORTANT]
> - **Independización de Destellos**:
>   - Se añade la nueva propiedad booleana `cloud_flash_enabled` (por defecto `true`).
>   - El motor de renderizado (`StormRenderer`) utilizará `lightningFlashEnabled` para los destellos y la iluminación de pantalla/fondo generados por rayos externos.
>   - Utilizará `cloudFlashEnabled` para los destellos internos y la iluminación que ocurre dentro de las nubes.
> - **Interfaz de Ajustes**:
>   - Se agrega un nuevo Switch `switchCloudFlash` bajo la sección "Color de Destellos en Nubes" para poder activar o desactivar este efecto en tiempo real.

---

## Proposed Changes

### 1. Proveedor de Configuración

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Declarar: `fun isCloudFlashEnabled(): Boolean`

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Definir constantes:
  - `const val KEY_CLOUD_FLASH_ENABLED = "cloud_flash_enabled"`
  - `const val DEFAULT_CLOUD_FLASH_ENABLED = true`
- Implementar:
  - `override fun isCloudFlashEnabled(): Boolean`
  - `fun setCloudFlashEnabled(enabled: Boolean)`

---

### 2. SceneManager y Flujo de Actualización

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Exponer el estado: `fun isCloudFlashEnabled(): Boolean = configProvider.isCloudFlashEnabled()`
- Modificar el flujo en `update()` para comprobar `configProvider.isCloudFlashEnabled()` antes de programar y disparar destellos internos de nubes (`isInternalOnly = true`).

---

### 3. Renderizado OpenGL

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- En `drawFrame()`, leer tanto `lightningFlashEnabled` como `cloudFlashEnabled`.
- Actualizar la firma de `drawClouds`:
  - `private fun drawClouds(clouds: List<Cloud>, lightnings: List<Lightning>, lightningFlashEnabled: Boolean, cloudFlashEnabled: Boolean)`
- En `drawClouds()`, filtrar las luces usando las variables correspondientes a su tipo:
  - Rayos normales (`!lightning.isInternalOnly`) aplican si `lightningFlashEnabled` es `true`.
  - Rayos internos de nubes (`lightning.isInternalOnly`) aplican si `cloudFlashEnabled` es `true`.

---

### 4. Actividades e Interfaz de Usuario

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Añadir el Switch Material Design `switchCloudFlash` y su texto descriptivo dentro del acordeón, justo antes del switch de destellos de los rayos.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Enlazar y configurar el switch `switchCloudFlash` con `configManager.isCloudFlashEnabled()` y `configManager.setCloudFlashEnabled(isChecked)`.

---

### 5. Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- Añadir mock de `isCloudFlashEnabled()` en `MockConfigProvider` (por defecto `true`).
- Crear la prueba unitaria `testCloudFlashToggleConfig()` para verificar la propagación del nuevo interruptor.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
