# Plan de Implementación: Mejoras Visuales y Modo Tormenta Variable (v32)

Este plan detalla el diseño técnico para implementar las mejoras del **Modo Tormenta (Storm Mode)**:
1.  **Rayos de Sombra (Dark God Rays)**: Sombras volumétricas proyectadas desde relámpagos detrás de nubes densas.
2.  **Gotas Refractivas con Rastro (Refraction & Trails)**: Distorsión real en fondo y nubes sincronizada matemáticamente con lentes originales y rastros ondulantes.
3.  **Iluminación Ambiental Selectiva (Rim Lighting)**: Destellos de luz en los bordes superiores de las montañas orientadas hacia el rayo durante el flash.
4.  **Tormenta Variable (Ciclos de Ráfagas)**: Un sistema de temporización y anulación dinámica que alterna entre periodos de calma y ráfagas huracanadas y eléctricas de forma automática.

---

## Cambios Propuestos

### 1. Control de Ramas (Git Branching)
Crearemos una nueva rama `storm-effects-enhancement` a partir de la rama actual para aislar estos desarrollos.

---

### 2. Desacoplamiento de Shaders de Nubes
Para evitar regresiones en el modo Soleado (Sunny), crearemos shaders específicos de nubes para Tormenta dentro de `:wallpaper-storm` en lugar de reutilizar los de `:core`.

#### [NEW] [storm_cloud.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.vert)
Vertex shader para las nubes de tormenta. Pasará `vScreenPos` al fragment shader para poder mapear las coordenadas en espacio de pantalla (Screen-Space UV).

#### [NEW] [storm_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/storm_cloud.frag)
Fragment shader para nubes de tormenta:
*   Convertirá las coordenadas de pantalla a espacio de textura de fondo utilizando `uBgScale`.
*   Muestreará las gotas en ese espacio común y escalará la distorsión resultante al espacio local de la nube usando la escala de la nube `uCloudScale`.
*   Aplicará atenuación por **Dark God Rays** (oscureciendo la nube y sus bordes según el rayo volumétrico de sombra proyectado desde la posición del relámpago).

---

### 3. Modificaciones a Shaders Existentes

#### [MODIFY] [background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/assets/shaders/background.frag)
*   **Gotas con Rastro (Trail Refraction)**: Actualizar la función `GetDropletEffects` para calcular la región de rastro vertical detrás de cada gota deslizante de arriba hacia abajo (Y del bucle de `-1` a `9`).
*   **Rayos de Sombra (Dark God Rays)**: Ray-marcher rápido de 10 muestras en espacio de pantalla desde las coordenadas del pixel hacia `uLightningPos`, acumulando densidad fractal de ruido (FBM).
*   **Iluminación de Bordes (Rim Lighting)**: Rim offset proporcional al flash para encender siluetas expuestas.

---

### 4. Integración de Tormenta Variable (Ciclos)

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigProvider.kt) & [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigManager.kt)
*   Definir clave de SharedPreferences `KEY_STORM_VARIABLE_ENABLED`.
*   Implementar `isStormVariableEnabled()` y `setStormVariableEnabled()`.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/SceneManager.kt)
*   Añadir temporizador `stormCycleTime` y bandera `isBurstActive`.
*   En `update(deltaTime)`, alternar entre **20s de Calma** y **10s de Ráfaga**.
*   En `updateFromConfig()`, si el modo variable está activo, sobreescribir los targets de densidad, lluvia, viento y relámpagos con constantes prefijadas (Calma: 40% densidad, 15% lluvia, 15% viento, 10% rayos; Ráfaga: 90% densidad, 95% lluvia, 95% viento, 85% rayos).

#### [MODIFY] [fragment_storm_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/fragment_storm_settings.xml) & [StormSettingsFragment.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormSettingsFragment.kt)
*   Añadir switch en la interfaz para activar la tormenta variable.
*   Implementar `updateControlsLockState()` que deshabilita los deslizadores e indica `"Ciclo Activo"` en los textos resumen cuando está activado.

---

## Plan de Verificación

### Pruebas Automatizadas
*   `.\gradlew.bat compileDebugSources` para comprobar tipos.
*   `.\gradlew.bat test` para ejecutar la suite de pruebas unitarias (incluyendo `testVariableStormCycle`).
