# Plan de Implementación – Dinámica Atmosférica y Deriva Bidireccional (v16)

Este plan describe el diseño e implementación de la característica de "Dinámica Atmosférica y Deriva Bidireccional" para el movimiento y comportamiento visual de las nubes en el fondo de pantalla animado.

## User Review Required

> [!IMPORTANT]
> - **Deriva Bidireccional**: Cada nube tendrá una velocidad de deriva propia `driftSpeed` entre `-0.03f` y `+0.03f` (asignada de forma aleatoria en `reset()`), lo que permitirá que se crucen en direcciones opuestas incluso cuando no hay viento horizontal (`windSpeed == 0f`).
> - **Variación Dinámica de Tamaño**: Se implementará una oscilación del tamaño (`scale`) basada en una función seno que fluctúa sutilmente (±8%) del tamaño base (`baseScale`).
> - **Escalado Proporcional al Viento**: La velocidad del cambio de opacidad (`fadeSpeed`) y la frecuencia de la oscilación de tamaño (`pulseTime`) se multiplicarán por un factor que escala proporcionalmente con la velocidad del viento horizontal:
>   $$\text{windFactor} = 1.0f + |windSpeed| \times 10.0f$$
>   Esto acelera los cambios sutiles bajo vientos fuertes de manera continua y sin saltos bruscos.
> - **Límites de Tamaño Modificados**: Se incrementará el tamaño máximo de las nubes en un 25% (de `1.0f` a `1.25f` antes de profundidad `z`) y el tamaño mínimo en un 15% (de `0.3f` a `0.345f` antes de `z`).

## Proposed Changes

---

### Versión de la Característica
- Se creará el directorio `implementation/v16` y se guardarán copias de este plan y de la bitácora (`walkthrough.md`) una vez completados.

---

### Simulación de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- Definir las variables internas:
  - `var driftSpeed: Float = 0f`
  - `var baseScale: Float = scale`
  - `var pulseTime: Float = 0f`
- En `reset()`, inicializar:
  - `driftSpeed = Random.nextFloat() * 0.06f - 0.03f` (Rango `[-0.03f, 0.03f]`)
  - Límites de escala incrementados: `minScale = 0.345f` (0.3 * 1.15) y `maxScale = 1.25f` (1.0 * 1.25).
  - `baseScale = (Random.nextFloat() * (maxScale - minScale) + minScale) * z`
  - `scale = baseScale`
  - `pulseTime = Random.nextFloat() * 10f`
- En `update(deltaTime, windSpeed)`:
  - Calcular el factor proporcional al viento: `val windFactor = 1.0f + kotlin.math.abs(windSpeed) * 10f`.
  - Incrementar `pulseTime` usando `deltaTime * windFactor`.
  - Actualizar `scale` usando la fórmula de latido:
    $$\text{scale} = \text{baseScale} \times (1.0f + \sin(\text{pulseTime}) \times 0.08f)$$
  - Modificar la ecuación de `positionX` para sumar la deriva:
    $$\text{positionX} += (\text{windSpeed} + \text{driftSpeed}) \times \text{speedFactor} \times \text{speedZFactor} \times \text{deltaTime}$$
  - Multiplicar `fadeSpeed` por `windFactor` para que el tiempo de transición de opacidad sea proporcional a la velocidad del viento:
    `val activeFadeSpeed = 1.5f * windFactor`

---

### Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- En `testCloudDepthAndParallax()`, actualizar los límites esperados de la escala de la nube:
  - `minExpectedScale = 0.345f * cloud.z - 0.001f`
  - `maxExpectedScale = 1.25f * cloud.z + 0.001f`
- En `testCloudDriftAndOpacityOscillation()`, renombrarlo a `testCloudDriftAndScaleOscillation()` para validar:
  - Que `driftSpeed` esté en el rango `[-0.03f, 0.03f]`.
  - Que la nube se desplace con `windSpeed = 0f`.
  - Que la escala oscile sutilmente en base a `pulseTime`.
  - Que el incremento de `pulseTime` y el cambio de opacidad sea mayor cuando `windSpeed != 0f`.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales para confirmar que todo compile y pase:
```bash
./gradlew testDebugUnitTest
```

### Manual Verification
- Comprobar visualmente en el emulador que las nubes se desplacen en ambas direcciones (bidireccional) cuando el viento sea neutro (0) o vertical.
- Comprobar que el tamaño de las nubes "respire" de forma natural y que esta oscilación acelere sutilmente al aumentar la velocidad del viento.
