# Plan de Implementación – Ajuste de Rayos Múltiples y Spawns en Bordes

Ajustaremos la frecuencia y cantidad máxima de rayos concurrentes para evitar la saturación visual ("demasiados rayos") y asegurar que cada rayo se distinga con claridad. Asimismo, modificaremos las reglas de nacimiento para que el 100% de los rayos nazcan exactamente en los límites físicos (bordes) de la pantalla (borde superior, borde izquierdo y borde derecho).

## User Review Required

- **Reducción del Pool de Rayos**: Colocaremos el pool máximo de rayos activos a 3 (en lugar de 8) para optimizar la claridad visual.
- **Retardo Mapeado**: Aumentaremos el retardo mínimo en la frecuencia máxima de 0.08s a 0.25s, logrando que los rayos no se encimen de forma excesiva.
- **Nacimiento Estricto en Bordes**: Todos los rayos comenzarán exactamente en $Y=1.0$ (borde superior), $X=-aspectRatio$ (borde izquierdo) o $X=aspectRatio$ (borde derecho).

---

## Proposed Changes

### Capa de Físicas y Datos

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Modificar `trigger()` para forzar que los puntos de inicio estén estrictamente en los bordes:
  - Borde Superior: $startX \in [-aspectRatio, aspectRatio]$, $startY = 1.0f$, ángulo de inclinación $[-15^{\circ}, 15^{\circ}]$.
  - Borde Izquierdo: $startX = -aspectRatio$, $startY \in [0.3f, 0.9f]$, ángulo de inclinación $[20^{\circ}, 45^{\circ}]$.
  - Borde Derecho: $startX = aspectRatio$, $startY \in [0.3f, 0.9f]$, ángulo de inclinación $[-45^{\circ}, -20^{\circ}]$.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Reducir el tamaño del pool de rayos de 8 a 3: `val lightnings = List(3) { Lightning() }`.
- Reajustar `setupNextLightningDelay()` para que el retardo base a frecuencia máxima (100%) sea de 0.25s (250 ms) en lugar de 0.08s.

### Interfaz de Ajustes

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Ajustar la etiqueta para la frecuencia del 100% a "Máximo caos (rayos rápidos)".
- Ajustar la etiqueta del 90% al 99% a "Tempestad extrema (0.25–0.8s)".

---

## Plan de Verificación

### Pruebas Automatizadas
- Actualizar `testLightningFrequencyDelayCalculation` en [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt) para verificar los nuevos límites de demora a frecuencia máxima (0.25s).
- Correr `./gradlew testDebugUnitTest`.

### Verificación Manual
- Validar visualmente en ajustes que el texto descriptivo cambió.
- Verificar que los rayos nacen estrictamente de los bordes físicos de la pantalla y que no saturan la visualización.
