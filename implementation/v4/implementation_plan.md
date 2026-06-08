# Plan de Implementación – Configuración de Intensidad de Viento Variable

Implementaremos una nueva configuración en SharedPreferences y la interfaz de usuario de configuración para controlar de manera variable la intensidad (fuerza) del viento. Esto afectará el ángulo de caída de la lluvia de forma progresiva.

## User Review Required

- Se agregará una opción nueva en la UI de configuración ("Fuerza del Viento") como un deslizador (SeekBar) con valores de 0% a 100%.
- A mayor fuerza de viento, mayor será el ángulo de inclinación lateral de la lluvia en la dirección configurada. Si la dirección de la lluvia es "Vertical", la fuerza del viento no modificará la trayectoria (caída recta).

## Proposed Changes

### Capa de Configuración

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Agregar el método `fun getWindIntensity(): Int` a la interfaz `ConfigProvider`.

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Definir la clave `KEY_WIND_INTENSITY = "wind_intensity"` y el valor predeterminado `DEFAULT_WIND_INTENSITY = 50`.
- Implementar `getWindIntensity(): Int` y `setWindIntensity(intensity: Int)` para guardar la configuración.

### Capa de Físicas y Entidades

#### [MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt)
- Cambiar la firma de la función `reset` a: `fun reset(aspectRatio: Float, windDirection: Int, windIntensity: Int, startOnScreen: Boolean = false)`.
- Calcular el ángulo de caída `angleDeg` proporcional al valor de `windIntensity` (ej: ángulo máximo de 35 grados con un rango de dispersión aleatoria de 5 grados).

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Agregar la propiedad `private var windIntensity = -1` para rastrear los cambios de fuerza del viento.
- En `updateFromConfig()`, leer `getWindIntensity()`, compararlo con el valor actual y, si cambió, resetear todas las gotas de lluvia activas con el nuevo parámetro de viento.
- Pasar `windIntensity` a todas las llamadas a `reset()` de las gotas de lluvia.

### Capa de Interfaz de Usuario

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Agregar un nuevo `CardView` debajo del selector de dirección de la lluvia para la configuración "Fuerza del Viento" con un `SeekBar` (`max="100"`).

#### [MODIFY] [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml)
- Añadir la cadena de texto `@string/wind_intensity_label`.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Enlazar el nuevo control de fuerza de viento (`seekBarWindIntensity`), cargar su estado guardado y persistir las modificaciones.

### Capa de Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- Implementar `getWindIntensity()` en `MockConfigProvider`.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar pruebas unitarias de `SceneManagerTest` con Gradle:
  `./gradlew testDebugUnitTest`

### Verificación Manual
- Compilar e instalar la app.
- Abrir la pantalla de Configuración y validar que al deslizar el control "Fuerza del Viento", el ángulo de inclinación de la lluvia cambie progresivamente en tiempo real en la vista previa del fondo.
