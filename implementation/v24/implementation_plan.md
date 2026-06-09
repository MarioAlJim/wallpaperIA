# Plan de Implementación – Ajuste de Dimensiones de Rayos y Nubes (v24)

Este plan describe los cambios para aumentar las dimensiones de los rayos y de las nubes de acuerdo con los nuevos requerimientos.

## User Review Required

> [!IMPORTANT]
> - **Rayos**:
>   - Altura máxima en Y (`maxHeight`): aumenta en un 50%, pasando de `1.5f` a **`2.25f`** (el rango Y ahora es `0.45f..2.25f`).
>   - Ancho máximo en X: aumenta en un 75%, pasando de `0.9f` a **`1.575f`** (el rango X ahora es `0.45f..1.575f`).
> - **Nubes**:
>   - Desplazamiento vertical máximo en Y: aumenta en un 100%, de `0.06f` a **`0.12f`**.
>   - Efecto de latido (breathing amplitude): aumenta en un 50%, de `0.5625f` a **`0.84375f`**.
>   - Tamaño máximo (`maxScale`): aumenta en un 50%, de `1.5625f` a **`2.34375f`**.
>   - Tamaño mínimo (`minScale`): aumenta en un 25%, de `0.345f` a **`0.43125f`**.

---

## Proposed Changes

### 1. Modelo de Rayos

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Modificar `maxHeight` en la función `trigger()` para establecerlo en `2.0f * 1.125f` (`2.25f`).
- Modificar `scaleX` en `trigger()` para que se genere en el rango `[0.45f, 1.575f]`:
  `scaleX = kotlin.random.Random.nextFloat() * 1.125f + 0.45f` (donde `1.125 = 1.575 - 0.45`).

---

### 2. Modelo de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- En `update()`, cambiar el factor de oscilación vertical Y del factor `0.06f` a **`0.12f`**:
  `positionY = basePositionY + sin(pulseTime * 0.5f) * 0.12f * baseScale * dynamicsSpeed`
- En `update()`, cambiar la amplitud de respiración `amplitude = 0.5625f * dynamicsSpeed` a **`0.84375f * dynamicsSpeed`**.
- En `reset()`, actualizar `minScale` a `0.43125f` y `maxScale` a `2.34375f`.

---

### 3. Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- En `testDiagonalLightningTrigger()`, cambiar la aserción de `scaleY` a `in 0.45f..2.26f` y de `scaleX` a `in 0.45f..1.576f`.
- En `testCloudDriftAndScaleOscillation()`:
  - Cambiar los rangos de prueba para grow-only a `[baseScale, baseScale * 1.84376f]` y shrink-only a `[baseScale * 0.15624f, baseScale]`.
  - Cambiar la aserción de oscilación Y a `0.121f`.
- En `testCloudDensityMapping()`, cambiar los límites de aserción de escala en base a `minScale = 0.43125f` y `maxScale = 2.34375f`.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
