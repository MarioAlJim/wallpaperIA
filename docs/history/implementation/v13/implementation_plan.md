# Plan de Implementación – Animación de Dibujado Progresivo de Rayos

Implementaremos un efecto de revelado dinámico de arriba hacia abajo para cada rayo de tormenta. En lugar de que el rayo aparezca completo de forma instantánea en pantalla, se dibujará progresivamente desde su punto de inicio en el borde superior o lateral, recorriendo la pantalla hacia abajo durante la fase inicial de su ciclo de vida.

## User Review Required

- Este cambio es netamente estético y de renderizado; no requiere configuraciones adicionales en la pantalla de ajustes.
- La animación de dibujado se adaptará de forma proporcional a la duración del rayo: tomará el primer 20% del ciclo de vida total de cada descarga eléctrica.

---

## Proposed Changes

### Capa de Shaders

#### [MODIFY] [lightning.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/assets/shaders/lightning.frag)
- Declarar el nuevo uniform `uniform float uGrowthProgress;`.
- En la lógica de textura (`uIsTextured == 1`), descartar los fragmentos cuya coordenada vertical de textura sea mayor que `uGrowthProgress` (`vTexCoord.y > uGrowthProgress`). Esto enmascara y oculta la parte inferior del rayo revelándolo progresivamente.

### Capa de Físicas y Entidades

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Agregar una propiedad `var growthProgress: Float = 1.0f` a la clase `Lightning`.
- Al inicio del trigger, definir `growthProgress = 0.0f`.
- En `update(deltaTime)`, calcular la fracción de crecimiento. Si el tiempo actual `age` es menor al 20% de la duración total del rayo (`duration * 0.20f`), interpolar `growthProgress` linealmente de 0.0f a 1.0f. A partir de esa marca temporal, fijar `growthProgress = 1.0f`.

### Capa de Renderizado OpenGL

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- Obtener la ubicación de uniform para `uGrowthProgress` en `drawLightning()`.
- Para el destello de fondo (pantalla completa, `uIsTextured == 0`), asignar el valor fijo de `1.0f` (completamente visible).
- Para el quad texturizado de cada rayo (`uIsTextured == 1`), asignar el valor dinámico `lightning.growthProgress`.

---

## Plan de Verificación

### Pruebas Automatizadas
- Agregar una prueba unitaria `testLightningGrowthProgress` en [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt) para verificar que el progreso de crecimiento es exactamente `0.0f` al inicio, avanza de manera lineal en la primera fase del ciclo, y se estabiliza en `1.0f` para la fase final del destello.
- Ejecutar `./gradlew testDebugUnitTest`.

### Verificación Manual
- Ejecutar el simulador y aumentar la duración de los rayos en ajustes al máximo para observar en detalle cómo las descargas eléctricas se dibujan de arriba hacia abajo antes de desvanecerse.
