# Walkthrough – Ajuste de Dimensiones de Rayos y Nubes (v24)

Hemos completado las modificaciones de la versión 24, incrementando el tamaño máximo de los rayos en X e Y, y ajustando la oscilación Y, la amplitud de respiración (latido) y las dimensiones mínimas y máximas de las nubes.

## Cambios Realizados

### 1. Dimensiones Máximas de Rayos Incrementadas
- **[Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)**:
  - Incrementamos `maxHeight` de `1.5f` a `2.25f` (aumento del 50%).
  - Modificamos el ancho máximo en X de `0.9f` a `1.575f` (aumento del 75%), con un rango final en X de `[0.45f, 1.575f]`.

### 2. Dimensiones y Movimiento de Nubes Ajustados
- **[Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)**:
  - **Oscilación Y**: Aumentamos en un 100% la amplitud del desplazamiento vertical en Y, pasando de `0.06f` a `0.12f`.
  - **Latido (Respiración)**: Aumentamos en un 50% la amplitud del latido, pasando de `0.5625f` a `0.84375f`.
  - **Escala de Nubes**: Aumentamos el tamaño máximo (`maxScale`) en un 50% (de `1.5625f` a `2.34375f`) y el tamaño mínimo (`minScale`) en un 25% (de `0.345f` a `0.43125f`).

### 3. Pruebas Unitarias Adaptadas
- **[SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)**:
  - **Rayos**: Actualizamos las aserciones en `testDiagonalLightningTrigger()` para validar que `scaleY` se encuentre en `0.45f..2.26f` y `scaleX` en `0.45f..1.576f`.
  - **Nubes**:
    - Ajustamos la validación del rango de z-scale parallax (`testCloudDepthAndParallax()`) al nuevo rango de escala `0.43125f..2.34375f`.
    - Actualizamos la validación de latidos (`testCloudDriftAndScaleOscillation()`) para grow-only (`baseScale..baseScale * 1.84376f`) y shrink-only (`baseScale * 0.15624f..baseScale`).
    - Modificamos la tolerancia del desplazamiento en Y a `0.121f`.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Las 24 pruebas unitarias pasaron exitosamente.
