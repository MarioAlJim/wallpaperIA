# Plan de Implementación – Configuración de Color de las Gotas de Lluvia

Implementaremos una nueva configuración en SharedPreferences y la interfaz de usuario de configuración del fondo de pantalla animado para permitir al usuario cambiar el color de las gotas de lluvia. Ofreceremos 6 opciones de color: Azul Claro (predeterminado), Blanco Crystalline, Rojo Fuego, Verde Neón, Amarillo Eléctrico y Morado Eléctrico.

## User Review Required

- Se agregará una opción nueva en la UI de configuración ("Color de la Lluvia") como un deslizador (SeekBar) con 6 niveles correspondientes a los distintos colores.
- El color de las gotas de lluvia pasará de estar hardcodeado en el fragment shader a ser un uniform configurado dinámicamente desde el Renderer OpenGL.

## Proposed Changes

### Capa de Shaders

#### [MODIFY] [rain.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/assets/shaders/rain.frag)
- Declarar `uniform vec4 uRainColor;`.
- Usar `fragColor = uRainColor;` en `main()`.

### Capa de Configuración

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Agregar el método `fun getRainColorIndex(): Int` a la interfaz `ConfigProvider`.

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Definir la clave `KEY_RAIN_COLOR_INDEX = "rain_color_index"` y el valor predeterminado `DEFAULT_RAIN_COLOR_INDEX = 0` (Azul).
- Implementar `getRainColorIndex(): Int` y agregar `setRainColorIndex(index: Int)` para guardar la configuración.

### Capa de Renderizado y Física

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Exponer el color actual mediante el método `fun getRainColorIndex(): Int`.

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Agregar el método `getRainColor(index: Int): FloatArray` que asocie los índices a vectores RGBA de coma flotante.
- En `drawRain()`, obtener el identificador del uniform `uRainColor` y asignar el vector correspondiente antes de la llamada de dibujado.
- En `drawFrame()`, pasar el índice de color obtenido de `sceneManager.getRainColorIndex()` al método `drawRain()`.

### Capa de Interfaz de Usuario

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Agregar un nuevo `CardView` debajo del selector de dirección de lluvia para la configuración "Color de la Lluvia" con un `SeekBar` (`max="5"`).

#### [MODIFY] [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml)
- Añadir la cadena de texto `@string/rain_color_label`.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Enlazar el control `seekBarRainColor`, cargar su estado guardado y persistir las modificaciones realizadas por el usuario.

### Capa de Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- Implementar `getRainColorIndex()` en `MockConfigProvider`.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar pruebas unitarias de `SceneManagerTest` con Gradle:
  `./gradlew testDebugUnitTest`

### Verificación Manual
- Compilar e instalar en un emulador o dispositivo.
- Abrir Configuración y validar que el SeekBar del color de lluvia cambie entre los 6 colores descritos.
- Seleccionar un color (ej. Verde Neón), establecer el fondo y verificar que las gotas caigan del color seleccionado.
