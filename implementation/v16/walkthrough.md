# Walkthrough – Dinámica Atmosférica y Deriva Bidireccional en Nubes (v16)

Hemos implementado la característica de **Dinámica Atmosférica y Deriva Bidireccional** para el movimiento horizontal y tamaño de las nubes en el fondo de pantalla animado.

## Cambios Realizados

### 1. Dinámica de Movimiento y Deriva Bidireccional (`driftSpeed`)
- En [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt):
  - Añadimos la variable `driftSpeed` inicializada aleatoriamente en `reset()` a un valor entre `-0.03f` y `0.03f` (velocidad horizontal propia).
  - En la física de `update()`, sumamos la deriva al viento:
    $$\text{positionX} += (\text{windSpeed} + \text{driftSpeed}) \times \text{speedFactor} \times \text{speedZFactor} \times \text{deltaTime}$$
  - Esto garantiza que incluso con viento inactivo u horizontal neutro (`windSpeed == 0f`), las nubes continúen moviéndose a velocidades propias, cruzándose en ambas direcciones (bidireccionalidad).

### 2. Variación Dinámica de Tamaño (Oscilación y "Latido")
- En [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt):
  - Añadimos `baseScale` (almacena el tamaño inicial calculado en `reset()`) y `pulseTime` (un acumulador de tiempo con desfase aleatorio).
  - En cada ciclo `update()`, recalculamos `scale` aplicando una fluctuación sutil del ±8% basada en seno:
    $$\text{scale} = \text{baseScale} \times (1.0f + \sin(\text{pulseTime}) \times 0.08f)$$
  - Esto da un efecto vaporoso y natural a las nubes, simulando que "respiran" y cambian de forma lentamente.

### 3. Escalamiento Proporcional al Viento (`windFactor`)
- Para sincronizar el dinamismo atmosférico general, la velocidad del desvanecimiento (`fadeSpeed`) y la velocidad de la oscilación de tamaño (`pulseTime`) se multiplican por un factor que depende de la velocidad del viento:
  $$\text{windFactor} = 1.0f + |windSpeed| \times 10.0f$$
  - `activeFadeSpeed = 1.5f * windFactor`
  - `pulseTime += deltaTime * windFactor`
- Al aumentar el viento horizontal, las nubes cambian su opacidad y tamaño de manera más ágil, sin producir transiciones bruscas.

### 4. Nuevos Límites de Tamaño (+15% mínimo, +25% máximo)
- En `reset()`, se aumentaron los límites del tamaño inicial calculado antes de la escala por profundidad `z`:
  - Tamaño mínimo: `0.345f` (antes `0.3f`, aumento del 15%).
  - Tamaño máximo: `1.25f` (antes `1.0f`, aumento del 25%).

### 5. Pruebas Unitarias
- En [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt):
  - Actualizamos `testCloudDepthAndParallax` para validar los nuevos límites de escala.
  - Implementamos la prueba `testCloudDriftAndScaleOscillation()` para verificar la inicialización bidireccional, movimiento con viento neutro, límites de oscilación y el escalamiento proporcional de tiempo bajo viento.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas unitarias locales en el entorno:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Todas las pruebas compilaron y pasaron con éxito.
