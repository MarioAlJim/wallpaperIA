# Walkthrough – Configuración de Color de Rayos y Modo Aleatorio

Hemos implementado la opción de personalizar el color de los rayos y sus destellos ambientales asociados desde la pantalla de configuración del fondo de pantalla animado. El usuario puede ahora fijar un color específico (Blanco, Azul, Amarillo, Rojo, Verde o Morado) o seleccionar la opción "Aleatorio" para que cada rayo sucesivo caiga en un color al azar.

## Cambios Realizados

### 1. Interfaz de Usuario (UI)
* **Nueva tarjeta de configuración**:
  - Agregamos un control deslizante `SeekBar` (`seekBarLightningColor` con valor máximo de 6) y un visor de estado de texto `textViewLightningColorValue` en [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml) para permitir al usuario seleccionar el color del rayo.
  - Definimos la cadena de texto `@string/lightning_color_label` en [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml).
* **Controlador de pantalla**:
  - Enlazamos y configuramos el listener del SeekBar en [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt), traduciendo el índice del SeekBar a etiquetas amigables ("Blanco", "Azul", "Amarillo", "Rojo", "Verde", "Morado", "Aleatorio").

### 2. Capa de Preferencias y Datos
* **Claves de Configuración**:
  - Definimos `KEY_LIGHTNING_COLOR_INDEX = "lightning_color_index"` con valor predeterminado 0 (Blanco) en [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt).
  - Agregamos `getLightningColorIndex()` a la interfaz [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt).
  - Implementamos los accesores get/set de color en `ConfigManager.kt`.

### 3. Físicas de Tormenta
* **Entidad Rayo**:
  - Añadimos la propiedad `selectedColorIndex` a la clase [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt).
  - Actualizamos `trigger()` para recibir y almacenar el índice del color del rayo actual.
* **Orquestador de Escena**:
  - Modificamos el ciclo de actualización en [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt). Al disparar un nuevo rayo, si la preferencia indica "Aleatorio" (6), el SceneManager selecciona dinámicamente un número aleatorio entre 0 y 5. Si es un color estático, utiliza el índice configurado directamente.

### 4. Shaders y Renderizado OpenGL
* **Cargar uniform de color**:
  - Modificamos `drawLightning()` en [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt) para mapear el índice del color a un vector RGBA mediante `getLightningColor()`.
  - Vinculamos este vector al nuevo uniform `uLightningColor`.
* **Shader Fragment**:
  - Actualizamos [lightning.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/assets/shaders/lightning.frag) para declarar `uniform vec4 uLightningColor`.
  - Multiplicamos la salida de fragmento por `uLightningColor` para que tanto el destello ambiental en pantalla completa (`uIsTextured == 0`) como el quad texturizado del rayo físico (`uIsTextured == 1`) asimilen la tonalidad seleccionada.

## Commits Realizados

- `feat(ui): add lightning color seekbar and strings`
- `feat(config): add lightning color index configuration to preferences`
- `feat(shader): add uLightningColor uniform and bind listener in settings`
- `feat(physics): update Lightning and SceneManager to support lightning color selection`
- `feat(renderer): map lightning color index and upload uniform to shaders`
- `test(physics): add tests for custom and random lightning colors`

## Verificación

### Pruebas Automatizadas
Creamos una nueva prueba unitaria `testLightningColorTrigger` en [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt) para verificar la correcta resolución del color al disparar un rayo, incluyendo la aleatorización correcta.

Ejecutamos la suite de pruebas mediante Gradle de manera exitosa:
```bash
.\gradlew.bat testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` en 1s.
