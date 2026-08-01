# Walkthrough – Duplicación de Ancho de Rayos y Movimiento Vertical de Nubes (v21)

Hemos duplicado (incrementado en un 100%) el ancho de los rayos en la pantalla y la amplitud del desplazamiento vertical en el eje Y de las nubes.

## Cambios Realizados

### 1. Ancho de Rayos Duplicado
- **[Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)**:
  - Se incrementó el rango de la propiedad `scaleX` (escala horizontal del rayo) en un 100%, pasando de `0.225f..0.45f` a **`0.45f..0.9f`**:
    `scaleX = kotlin.random.Random.nextFloat() * 0.45f + 0.45f`

### 2. Oscilación en Y de las Nubes Duplicada
- **[Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)**:
  - Se duplicó la amplitud máxima de la oscilación vertical en el eje Y, cambiando el factor de `0.03f` a **`0.06f`**:
    `positionY = basePositionY + sin(pulseTime * 0.5f) * 0.06f * baseScale * dynamicsSpeed`

### 3. Pruebas Unitarias Adaptadas
- **[SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)**:
  - **Rayos**: Ajustamos la aserción en `testDiagonalLightningTrigger()` para validar que `scaleX` se genere correctamente en el nuevo rango duplicado (`0.45f..0.91f`).
  - **Nubes**: Actualizamos la tolerancia de oscilación vertical en `testCloudDriftAndScaleOscillation()` para validar que el desplazamiento en Y alcance hasta `0.061f * cloud.baseScale`.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Las 21 pruebas unitarias del proyecto pasaron exitosamente.
