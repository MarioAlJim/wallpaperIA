# Plan de Implementación – Duplicación de Ancho de Rayos y Movimiento Vertical de Nubes (v21)

Este plan describe los cambios para duplicar (incrementar en un 100%) el ancho de los rayos en la pantalla, y duplicar la amplitud máxima del desplazamiento vertical oscilatorio (en el eje Y) de las nubes.

## User Review Required

> [!IMPORTANT]
> - **Duplicación de Ancho de Rayos**: En `Lightning.kt`, la escala horizontal del rayo (`scaleX`) se incrementará en un 100%, pasando del rango `0.225f..0.45f` a **`0.45f..0.9f`**.
> - **Duplicación de Desplazamiento Y de Nubes**: En `Cloud.kt`, la amplitud de la oscilación vertical sobre el eje Y se duplicará, pasando del factor `0.03f` al factor **`0.06f`**.
>   La nueva fórmula de posición Y será:
>   `positionY = basePositionY + sin(pulseTime * 0.5f) * 0.06f * baseScale * dynamicsSpeed`

---

## Proposed Changes

### 1. Modelo de Rayos

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Modificar el rango de `scaleX` en la función `trigger()`:
  `scaleX = kotlin.random.Random.nextFloat() * 0.45f + 0.45f`

---

### 2. Modelo de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- Modificar la amplitud de oscilación vertical en `update()` del factor `0.03f` a `0.06f`:
  `positionY = basePositionY + sin(pulseTime * 0.5f) * 0.06f * baseScale * dynamicsSpeed`

---

### 3. Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- **Ancho de Rayos**: En `testDiagonalLightningTrigger()`, actualizar el rango de la aserción de `scaleX` a `0.45f..0.91f`.
- **Desplazamiento Y de Nubes**: En `testCloudDriftAndScaleOscillation()`, actualizar la aserción del límite del desplazamiento vertical en Y para admitir hasta `0.061f * cloud.baseScale`.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
