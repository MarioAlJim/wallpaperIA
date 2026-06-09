# Plan de Implementación – Configuración de Color de Rayos y Modo Aleatorio

Implementaremos la posibilidad de personalizar el color de los rayos (y sus correspondientes destellos de fondo) desde la pantalla de configuración. El usuario podrá elegir un color específico (Blanco, Azul, Amarillo, Rojo, Verde, Morado) o activar el modo "Aleatorio", el cual seleccionará un color distinto al azar para cada rayo que caiga.

## User Review Required

- Se añadirá una nueva opción en la pantalla de Ajustes ("Color de los Rayos") mediante un control deslizante (SeekBar) con 7 opciones (Blanco, Azul, Amarillo, Rojo, Verde, Morado y Aleatorio).
- Al elegir un color, tanto la textura del rayo como el destello de la pantalla completa se teñirán con dicho color. En el modo "Aleatorio", cada rayo tendrá su propio color resolved.

## Proposed Changes

### Capa de Configuración

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Agregar el método `fun getLightningColorIndex(): Int` a la interfaz `ConfigProvider`.

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Definir la clave `KEY_LIGHTNING_COLOR_INDEX = "lightning_color_index"` y el valor predeterminado `DEFAULT_LIGHTNING_COLOR_INDEX = 0` (Blanco).
- Implementar `getLightningColorIndex(): Int` and `setLightningColorIndex(index: Int)` para persistir el valor en SharedPreferences.

### Capa de Shaders

#### [MODIFY] [lightning.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/assets/shaders/lightning.frag)
- Declarar el uniform `uniform vec4 uLightningColor;`.
- Multiplicar el color del fragmento (tanto de la textura como del color sólido) por `uLightningColor` para teñir el rayo y el flash con el color correspondiente.

### Capa de Interfaz de Usuario (UI)

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Agregar un nuevo `CardView` para "Color de los Rayos" debajo de la tarjeta "Frecuencia de Rayos" con un `SeekBar` (`max="6"`).

#### [MODIFY] [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml)
- Añadir la cadena de texto `@string/lightning_color_label`.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Enlazar el nuevo SeekBar de color de rayos, inicializarlo, agregar su listener para guardar cambios y actualizar un método `updateLightningColorTextView` que indique en texto el color seleccionado (Blanco, Azul, Amarillo, Rojo, Verde, Morado, Aleatorio).

### Capa de Físicas y Entidades

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Agregar la propiedad `var selectedColorIndex: Int = 0`.
- Modificar `trigger(aspectRatio: Float, textureCount: Int, colorIndex: Int)` para almacenar el índice de color resuelto en `selectedColorIndex`.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- En `update(deltaTime)`, si se dispara un rayo, leer el color de la configuración. Si es modo "Aleatorio" (6), seleccionar un índice aleatorio entre 0 y 5; de lo contrario, pasar el índice seleccionado al disparador del rayo.

### Capa de Renderizado OpenGL

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Implementar un mapeo `getLightningColor(index: Int): FloatArray` que devuelva los valores RGBA del color (Blanco, Azul, Amarillo, Rojo, Verde, Morado).
- Cargar este color en el uniform `uLightningColor` antes de renderizar tanto el destello como el quad de rayo.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar `./gradlew testDebugUnitTest` para verificar que la compilación y lógica de escena se mantengan correctas.

### Verificación Manual
- Abrir la aplicación y verificar que la tarjeta de "Color de los Rayos" aparece y es controlable.
- Seleccionar varios colores específicos y comprobar que tanto el rayo como el destello se tiñen del color correspondiente.
- Seleccionar "Aleatorio" y verificar que cada destello sucesivo tiene un color aleatorio diferente.
