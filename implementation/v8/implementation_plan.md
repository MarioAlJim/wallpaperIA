# Plan de Implementación – Renderizado de Rayos con Assets e Integración de Frecuencia

Implementaremos el renderizado de rayos realistas utilizando texturas (imágenes assets) cargadas dinámicamente desde la carpeta `assets/lightning/`. El sistema elegirá un asset al azar en cada destello, permitiendo agregar más imágenes en el futuro sin modificar código. Adicionalmente, habilitaremos la visualización y control de la frecuencia de rayos en la interfaz de configuración.

## User Review Required

- Se habilitará la visibilidad del CardView de "Frecuencia de Rayos" en la pantalla de Ajustes para permitir al usuario cambiar la frecuencia de la animación.
- Los rayos ya no se dibujarán mediante líneas simples generadas procedimentalmente, sino a través de un quad texturizado con la imagen seleccionada aleatoriamente de la carpeta `assets/lightning/`.

## Proposed Changes

### Capa de Shaders

#### [MODIFY] [lightning.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/assets/shaders/lightning.vert)
- Modificar el vertex shader para recibir coordenadas de textura (`aTexCoord`) y transmitirlas como output (`vTexCoord`).

#### [MODIFY] [lightning.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/assets/shaders/lightning.frag)
- Añadir el sampler de textura `uTexture` y un uniform de control `uIsTextured`.
- Si `uIsTextured == 1`, renderizar el fragmento usando el color de la textura escalado por la intensidad del brillo. Si es `0`, conservar el renderizado de color sólido blanco (usado para el destello a pantalla completa).

### Capa de Interfaz de Usuario (UI)

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Cambiar la propiedad `android:visibility="gone"` a `android:visibility="visible"` (o eliminarla) en el CardView de "Frecuencia de Rayos" (`seekBarLightningFrequency`).

### Capa de Físicas y Entidades

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Reemplazar la lógica de generación procedimental de ramas (`Branch` y `generateLightningPath`) por campos necesarios para el quad texturizado:
  - `var selectedTextureIndex: Int`
  - `var positionX: Float`
  - `var scaleX: Float`
  - `var scaleY: Float`
- Actualizar `trigger(aspectRatio: Float, textureCount: Int)` para seleccionar un índice de textura al azar y definir posición y dimensiones aleatorias del rayo en la parte superior de la pantalla.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Descomentar el bloque de actualización de rayos en `update(deltaTime)`.
- Cuando el tiempo transcurrido supere el retardo calculado, disparar el rayo pasando el número total de texturas disponibles.

### Capa de Renderizado OpenGL

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Escanear dinámicamente la carpeta `assets/lightning/` usando `AssetManager` y cargar todos los archivos PNG como texturas OpenGL, almacenando sus IDs en una lista (`lightningTextures`).
- Definir un búfer de vértices con coordenadas UV para dibujar quads texturizados.
- Descomentar y reescribir `drawLightning(lightning)` para enlazar la textura correspondiente y dibujar el rayo usando el shader modificado cuando `lightning.isActive` es verdadero.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar `./gradlew testDebugUnitTest` para validar que las aserciones de cálculo de frecuencias y demoras en `SceneManagerTest` continúan pasando sin fallos.

### Verificación Manual
- Abrir la aplicación y verificar que la configuración de frecuencia de rayos es visible y interactiva.
- Modificar la frecuencia y observar que los rayos aparecen dinámicamente usando el asset `lightning.png`.
