# Walkthrough – Configuración de Dirección, Fuerza de Viento, Ajuste de Gotas y Color de la Lluvia

Hemos implementado la posibilidad de cambiar la dirección del viento (lluvia) en tres opciones (Izquierda, Vertical, Derecha), regular de forma variable la fuerza del viento (0% a 100%), configurar la escala de gotas de lluvia a una escala no lineal con opción de "Nada" (0, 10, 25, 50 y 100 partículas), y agregado una nueva configuración para cambiar el color de las gotas de lluvia entre 6 variantes.

## Cambios Realizados

### 1. Dirección del Viento y Cantidad de Gotas

* **Configuración**:
  - Se agregó `getWindDirection(): Int` a [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt).
  - Se implementaron métodos en [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt) para guardar y leer la dirección en SharedPreferences (`KEY_WIND_DIRECTION`, `setWindDirection()`).

* **Físicas de Partículas**:
  - En [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt), la función `reset()` calcula ahora la velocidad y ángulo en X e Y de acuerdo a la dirección (0 = Izquierda, 1 = Vertical, 2 = Derecha). Se generalizaron los rangos de spawn horizontal para que las gotas cubran la pantalla de forma simétrica independientemente de la dirección de la velocidad.

* **Control y Límites**:
  - En [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt), se añadió el seguimiento del estado de dirección del viento. Al detectar un cambio de viento en SharedPreferences, se re-inicializan las gotas activas.
  - Se modificaron los límites de reinicio en X para detectar salidas del viewport por el lado derecho o izquierdo según el signo de `velocityX`.
  - Se implementó un mapeo no lineal en `adjustRain()` para cambiar el recuento de gotas de lluvia a **0, 10, 25, 50 y 100** gotas según la intensidad configurada.

* **Interfaz de Usuario**:
  - Se añadió un nuevo `CardView` con una barra deslizante (SeekBar) de 3 estados a [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml).
  - Se agregaron recursos de cadenas al archivo [strings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/values/strings.xml).
  - Se enlazaron los controles Seekbar en [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt) y se actualizó `updateRainTextView` para mostrar el recuento correcto de partículas no lineales (0, 10, 25, 50 y 100 gotas) incluyendo la nueva opción **Nada**.

* **Pruebas**:
  - Se mockeó la nueva propiedad de dirección de viento y se actualizaron las aserciones de recuento de partículas (0, 10, 25, 50 y 100) en [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt).

### 2. Color de las Gotas de Lluvia

* **Capa de Shaders**:
  - Se modificó [rain.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/assets/shaders/rain.frag) declarando `uniform vec4 uRainColor;` y asignándola a `fragColor` en el fragment shader.

* **Configuración**:
  - Se agregó `getRainColorIndex(): Int` a [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt).
  - Se implementaron métodos en [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt) para guardar y leer la dirección en SharedPreferences (`KEY_RAIN_COLOR_INDEX`, `setRainColorIndex()`).

* **Renderizado**:
  - Se expuso la propiedad de color mediante `getRainColorIndex(): Int` en [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt).
  - En [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt), se implementó `getRainColor(index: Int): FloatArray` mapeando los índices a colores RGBA (Azul, Blanco, Rojo, Verde, Amarillo, Morado). Se carga este valor en el uniform `uRainColor` antes de dibujar el sistema de partículas de lluvia.

* **Interfaz de Usuario**:
  - Se añadió un nuevo `CardView` con una barra deslizante (SeekBar) de 6 estados a [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml).
  - Se enlazaron los controles Seekbar en [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt) y se añadió la función `updateColorTextView` para mostrar el nombre del color seleccionado en tiempo real.

* **Pruebas**:
  - Se mockeó la propiedad `getRainColorIndex(): Int` en `MockConfigProvider` dentro de [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt).

### 3. Intensidad de Viento Variable

* **Configuración**:
  - Se agregó `getWindIntensity(): Int` a [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt).
  - Se implementaron métodos en [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt) para leer y escribir SharedPreferences (`KEY_WIND_INTENSITY`, `setWindIntensity()`).

* **Físicas de Partículas**:
  - En [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt), la función `reset()` utiliza el valor `windIntensity` para escalar el ángulo de caída en un rango progresivo de hasta 35 grados (con 5 grados de variación aleatoria por gota), resultando en una caída recta a 0% de intensidad y caída muy diagonal a 100%.

* **Control y Límites**:
  - En [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt), se añadió `windIntensity` al seguimiento de variables. Un cambio en la fuerza del viento provocará el reinicio de las trayectorias de las gotas en la animación.

* **Interfaz de Usuario**:
  - Se añadió un nuevo `CardView` con una barra deslizante (SeekBar) de 0% a 100% a [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml).
  - Se enlazaron los controles Seekbar en [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt) y se agregó el caso `seekBarWindIntensity` en `updateTextView()` para dar formato al porcentaje en pantalla.

* **Pruebas**:
  - Se mockeó `getWindIntensity(): Int` en `MockConfigProvider` dentro de [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt).

## Commits Realizados (Conventional Commits)

Realizamos commits incrementales para cada fase lógica:
- `feat(config): add wind direction configuration keys and interface`
- `feat(physics): support rain direction kinematics in RainDrop`
- `feat(scene): integrate windDirection configuration in SceneManager`
- `feat(ui): add wind direction seekbar and resource labels to settings layout`
- `feat(ui): connect wind direction settings UI and adjust rain drop text levels`
- `test(scene): update test configurations and rain intensity mapping assertions`
- `feat(shader): parameterize rain drop color as uniform in rain.frag`
- `feat(config): add rain color index preference interface and keys`
- `feat(scene): expose rain color index in SceneManager`
- `feat(renderer): bind uRainColor uniform during drawRain`
- `feat(ui): add rain color seekbar layout and string label`
- `feat(ui): hook up seekBarRainColor settings listener and label`
- `test(scene): mock rain color index in test suite MockConfigProvider`
- `feat(physics): change rain intensity mapping to 50, 150, 300, and 500 drops`
- `feat(ui): add Nada option with 0 drops to rain intensity settings`
- `feat(physics): update rain intensity mapping to 0, 10, 25, 50, and 100 drops`
- `feat(config): add wind intensity preference interface and keys`
- `feat(physics): support variable windIntensity angle calculation in RainDrop`
- `feat(scene): integrate windIntensity variable in SceneManager reset calls`
- `feat(ui): add wind intensity seekbar and string label`
- `feat(ui): hook up seekBarWindIntensity settings listener and label`
- `test(scene): mock wind intensity in test suite MockConfigProvider`

## Verificación

### Pruebas Automatizadas
Se ejecutó la suite de pruebas unitarias con el siguiente comando exitoso:
```bash
.\gradlew.bat testDebugUnitTest
```
**Resultado**:
`BUILD SUCCESSFUL` con todas las aserciones de mapeo de lluvia y frecuencias validadas.
