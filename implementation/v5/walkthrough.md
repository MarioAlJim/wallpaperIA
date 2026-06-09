# Walkthrough – Configuración de Velocidad de la Lluvia Variable

Hemos implementado la posibilidad de configurar de manera variable la velocidad a la que caen las gotas de lluvia a través de la interfaz de usuario de configuración. Esto escala la velocidad global de la lluvia de forma lineal según la preferencia del usuario, manteniendo la variación aleatoria individual de cada gota para un comportamiento natural.

## Cambios Realizados

### 1. Capa de Configuración y SharedPreferences

* **Interfaz de Proveedor**:
  - Se agregó `fun getRainSpeed(): Int` a [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt).
* **Gestor de Configuración**:
  - Se definieron la clave `KEY_RAIN_SPEED = "rain_speed"` y el valor predeterminado `DEFAULT_RAIN_SPEED = 50` en [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt).
  - Se implementaron los métodos `getRainSpeed(): Int` y `setRainSpeed(speed: Int)` para persistir el valor en SharedPreferences.

### 2. Capa de Físicas y Animación de Partículas

* **Gotas de Lluvia**:
  - En [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt), la firma del método `reset` se modificó para aceptar `rainSpeed: Int`.
  - Se implementó un factor de velocidad dinámico: `val speedFactor = 0.2f + (rainSpeed / 100f) * 1.6f`. Con esto, a `0%` de velocidad la lluvia cae muy lentamente (0.2x), a `50%` (predeterminado) cae a velocidad normal (1.0x) y a `100%` cae muy rápido (1.8x). La velocidad base de cada gota (entre 3.0 y 4.5) se multiplica por este factor.
* **Controlador de Escena**:
  - Se añadió la propiedad `rainSpeed` en [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt) para rastrear y detectar cambios en la velocidad.
  - Al detectar un cambio de velocidad, se vuelven a inicializar (resetear) todas las partículas activas para aplicar la velocidad en tiempo real.

### 3. Capa de Interfaz de Usuario (UI)

* **Recursos de Diseño y Cadenas**:
  - Se añadió un nuevo control deslizante (SeekBar) dentro de un CardView en [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml).
  - Se agregó la cadena de texto `@string/rain_speed_label` en [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml) con el valor `"Velocidad de la Lluvia"`.
* **Actividad de Configuración**:
  - En [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt), se vinculó el control `seekBarRainSpeed` con su indicador `textViewRainSpeedValue`.
  - Se inicializó el SeekBar con el valor almacenado y se agregó el listener de cambios para actualizar SharedPreferences y formatear el porcentaje actual.

### 4. Pruebas Unitarias

* En [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt), se implementó `getRainSpeed()` en la clase mock `MockConfigProvider`.
* Se añadió el test unitario `testRainSpeedUpdates` para verificar que `SceneManager` detecta correctamente la nueva velocidad y actualiza su estado interno al ejecutarse la escena.

## Commits Realizados (Conventional Commits)

- `feat(config): add rain speed preference interface and keys`
- `feat(physics): scale RainDrop speed dynamically based on rainSpeed parameter`
- `feat(scene): integrate windIntensity variable in SceneManager reset calls` (contiene la integración de `rainSpeed` en SceneManager)
- `feat(ui): add rain speed seekbar layout and settings listener`

## Verificación

### Pruebas Automatizadas
Se ejecutaron todas las pruebas unitarias usando Gradle de forma exitosa:
```bash
.\gradlew.bat clean testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` sin fallos.
