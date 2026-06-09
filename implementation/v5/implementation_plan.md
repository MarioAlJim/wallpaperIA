# Plan de Implementación – Configuración de Velocidad de la Lluvia

Implementaremos una nueva configuración en SharedPreferences y la interfaz de usuario de configuración para controlar de manera variable la velocidad a la que caen las gotas de lluvia. Esto preservará la variación aleatoria individual de cada gota pero escalará la velocidad global.

## User Review Required

- Se agregará una opción nueva en la UI de configuración ("Velocidad de la Lluvia") como un deslizador (SeekBar) con valores de 0% a 100% (predeterminado en 50%).
- La velocidad base de cada gota (entre 3.0 y 4.5) se multiplicará por un factor de escala calculado de forma progresiva según la velocidad elegida por el usuario:
  `speedFactor = 0.2f + (rainSpeed / 100f) * 1.6f`

## Proposed Changes

### Capa de Configuración

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Agregar el método `fun getRainSpeed(): Int` a la interfaz `ConfigProvider`.

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Definir la clave `KEY_RAIN_SPEED = "rain_speed"` y el valor predeterminado `DEFAULT_RAIN_SPEED = 50`.
- Implementar `getRainSpeed(): Int` y `setRainSpeed(speed: Int)` para guardar la configuración.

### Capa de Físicas y Entidades

#### [MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt)
- Cambiar la firma de la función `reset` a: `fun reset(aspectRatio: Float, windDirection: Int, windIntensity: Int, rainSpeed: Int, startOnScreen: Boolean = false)`.
- Calcular el factor de velocidad `speedFactor` (rango de factor de 0.2 a 1.8) y multiplicar la velocidad base aleatoria de la gota por dicho factor.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Agregar la propiedad `private var rainSpeed = -1` para rastrear los cambios de velocidad de la lluvia.
- En `updateFromConfig()`, leer `getRainSpeed()`, compararlo con el valor actual y, si cambió, resetear todas las gotas de lluvia activas.
- Pasar `rainSpeed` a todas las llamadas a `reset()` de las gotas de lluvia.

### Capa de Interfaz de Usuario

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Agregar un nuevo `CardView` debajo del selector de fuerza del viento para la configuración "Velocidad de la Lluvia" con un `SeekBar` (`max="100"`).

#### [MODIFY] [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml)
- Añadir la cadena de texto `@string/rain_speed_label`.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Enlazar el nuevo control de velocidad de lluvia (`seekBarRainSpeed`), cargar su estado guardado y persistir las modificaciones.

### Capa de Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- Implementar `getRainSpeed()` en `MockConfigProvider`.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar pruebas unitarias de `SceneManagerTest` con Gradle:
  `./gradlew testDebugUnitTest`

### Verificación Manual
- Compilar e instalar la app.
- Abrir la pantalla de Configuración y validar que al deslizar el control "Velocidad de la Lluvia", la velocidad de caída de las gotas varíe progresivamente en tiempo real en la vista previa del fondo.
