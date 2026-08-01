# Walkthrough – Aumento de Tamaño Mínimo de Rayos y Respiración Exclusiva de Nubes (v22)

Hemos completado las modificaciones de la versión 22, que incrementan en un 50% la altura mínima de los rayos y cambian el efecto de respiración de las nubes a un comportamiento exclusivo de crecimiento o de encogimiento con amplitudes duplicadas.

## Cambios Realizados

### 1. Altura Mínima de Rayos Aumentada
- **[Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)**:
  - Incrementamos `minHeight` de `2.0f * 0.15f` (0.3f) a `2.0f * 0.225f` (0.45f), lo cual representa un aumento del 50% en el tamaño mínimo de los rayos.

### 2. Respiración Exclusiva y Amplificada de Nubes
- **[Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)**:
  - Añadimos la propiedad `onlyGrows` para determinar si la nube sólo crece o sólo se encoge en su ciclo de respiración.
  - Inicializamos `onlyGrows` de forma aleatoria en `reset()`.
  - Actualizamos la fórmula del tamaño de respiración en `update()` para usar una onda senoidal desplazada `(sin(pulseTime) + 1.0f) * 0.5f` multiplicada por la nueva amplitud de `0.5625f * dynamicsSpeed` (duplicada de `0.28125f` para representar el incremento del 100% en crecimiento y encogimiento).

### 3. Pruebas Unitarias Adaptadas
- **[SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)**:
  - **Rayos**: Modificamos el rango de verificación de `scaleY` a `0.45f..1.5f` en `testDiagonalLightningTrigger()`.
  - **Nubes**: Actualizamos la sección de oscilación de escala en `testCloudDriftAndScaleOscillation()` para configurar y validar individualmente los casos de `onlyGrows = true` (rango `baseScale..baseScale * 1.5625f`) y `onlyGrows = false` (rango `baseScale * 0.4375f..baseScale`).

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas locales:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Las 24 pruebas pasaron satisfactoriamente.
