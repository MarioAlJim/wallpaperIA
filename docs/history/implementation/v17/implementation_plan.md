# Plan de Implementación – Atenuación de Deriva por Viento Dominante (v17)

Este plan describe el diseño e implementación de la característica de "Atenuación de Deriva por Viento Dominante" para evitar que las nubes viajen en contra o choquen con vientos horizontales establecidos.

## User Review Required

> [!IMPORTANT]
> - **Umbral de Dominancia**: Establecemos un umbral `windThreshold = 0.1f`. A partir de este valor de velocidad de viento horizontal, la deriva individual (`driftSpeed`) de la nube se anula por completo.
> - **Atenuación Gradual**: A medida que el viento horizontal aumenta, la influencia de la deriva disminuye de manera lineal:
>   $$\text{driftInfluence} = \left(1.0f - \frac{|windSpeed|}{windThreshold}\right).coerceIn(0.0f, 1.0f)$$
> - **Fórmula de Movimiento Refactorizada**:
>   $$\text{positionX} += (\text{windSpeed} + (\text{driftSpeed} \times \text{driftInfluence})) \times \text{speedFactor} \times \text{speedZFactor} \times \text{deltaTime}$$
> - **Beneficio**:
>   - Con viento neutro/vertical (`windSpeed == 0f`), la deriva es máxima (bidireccionalidad pura).
>   - Con viento moderado/fuerte ($|windSpeed| \ge 0.1f$), la deriva se desactiva y todas las nubes siguen estrictamente la dirección del viento global.

## Proposed Changes

---

### Versión de la Característica
- Se creará el directorio `implementation/v17` y se guardarán copias de este plan y de la bitácora (`walkthrough.md`) una vez completados.

---

### Simulación de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- En `update(deltaTime, windSpeed)`:
  - Definir `val windThreshold = 0.1f`.
  - Calcular la influencia de la deriva: `val driftInfluence = (1.0f - (abs(windSpeed) / windThreshold)).coerceIn(0f, 1f)`.
  - Actualizar el cálculo de `positionX` aplicando `driftInfluence` a `driftSpeed`:
    `positionX += (windSpeed + (driftSpeed * driftInfluence)) * speedFactor * speedZFactor * deltaTime`

---

### Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- En `testCloudDriftAndScaleOscillation()`:
  - Añadir una sección para verificar que cuando `windSpeed = 0.1f` (el umbral), el desplazamiento de la nube es determinado únicamente por el viento y que la deriva individual se anula por completo.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales para confirmar que todo compile y pase:
```bash
./gradlew testDebugUnitTest
```

### Manual Verification
- Comprobar visualmente en el simulador que:
  - Con viento en 0% o vertical, las nubes se desplazan en ambas direcciones (bidireccionalidad).
  - Al aumentar gradualmente la velocidad del viento horizontal, las nubes ralentizan su deriva individual en contra del viento hasta alinearse estrictamente en la dirección del viento dominante.
