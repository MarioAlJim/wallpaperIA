# Walkthrough – Separación de Ajustes de Destellos de Rayos y Destellos en Nubes (v23)

Hemos independizado las configuraciones de destellos en pantalla completa producidos por los rayos externos y de destellos internos e iluminación dentro de las nubes, añadiendo un interruptor para activar/desactivar estos últimos de forma independiente.

## Cambios Realizados

### 1. Independización de Configuraciones
- **[ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)**:
  - Declaramos el método `isCloudFlashEnabled(): Boolean`.
- **[ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)**:
  - Añadimos la clave de preferencia `"cloud_flash_enabled"` y su valor por defecto (`true`).
  - Implementamos los métodos `isCloudFlashEnabled()` y `setCloudFlashEnabled()`.
- **[SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)**:
  - Expusimos `isCloudFlashEnabled()` en el mánager.
  - Modificamos el loop de actualización en `update()` para evitar el disparo de destellos internos (`isInternalOnly = true`) si la configuración de destellos de nubes está desactivada.

### 2. Modificaciones en el Renderizador (StormRenderer)
- **[StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)**:
  - Obtuvimos por separado `lightningFlashEnabled` y `cloudFlashEnabled`.
  - Actualizamos `drawClouds()` para recibir ambos parámetros y filtrar las luces de tal forma que los rayos comunes iluminan si `lightningFlashEnabled` es `true`, y los rayos de nubes iluminan si `cloudFlashEnabled` es `true`.

### 3. Interfaz de Ajustes y Vinculación
- **[activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)**:
  - Agregamos un switch Material Design con ID `switchCloudFlash` y texto `"Destellos en las Nubes"`.
- **[WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)**:
  - Vinculamos el switch `switchCloudFlash` con las preferencias del `ConfigManager`.

### 4. Pruebas Unitarias
- **[SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)**:
  - Añadimos mock para la nueva propiedad en `MockConfigProvider`.
  - Implementamos la prueba `testCloudFlashToggleConfig()` para verificar la propagación del nuevo interruptor.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Las 25 pruebas unitarias pasaron exitosamente.
