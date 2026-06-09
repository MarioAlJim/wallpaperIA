# Plan de Implementación – Deriva Atmosférica y Oscilación de Opacidad en Nubes

Este plan describe el diseño e implementación de la característica de "Deriva Atmosférica" en las nubes. Esto asegura que, incluso si no hay viento horizontal (viento neutro o vertical), las nubes mantengan un movimiento autónomo mínimo y una leve oscilación en su opacidad, haciendo que la escena se sientan viva y tridimensional en todo momento.

## User Review Required

> [!IMPORTANT]
> - Se creará una nueva rama de Git llamada `feat-cloud-drift` para contener el desarrollo de esta versión.
> - Cada nube tendrá un factor de deriva individual `driftSpeed` de `+0.02f` o `-0.02f` asignado aleatoriamente al reiniciarse.
> - La fórmula de actualización de posición horizontal sumará la deriva al viento:
>   $$\text{positionX} += (\text{windSpeed} + \text{driftSpeed}) \times \text{speedFactor} \times \text{speedZFactor} \times \text{deltaTime}$$
> - Se implementará una oscilación de opacidad basada en una función seno con desfasajes temporales individuales (`timeOffset`) para que las nubes pulsen su luminosidad de forma independiente:
>   $$\text{opacity} = \text{baseOpacity} + \sin(\text{time} + \text{offset}) \times 0.05f$$

## Proposed Changes

---

### Versión de la Característica
- Se creará el directorio `implementation/v15` y se guardarán copias de este plan y de la bitácora (`walkthrough.md`) una vez completados.

---

### Simulación de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- Importar `kotlin.math.sin`.
- Agregar los atributos mutables:
  - `var driftSpeed: Float = 0f`
  - `var timeOffset: Float = 0f`
  - `private var timeAccumulator: Float = 0f`
- En `reset()`, inicializar de forma aleatoria:
  - `driftSpeed = if (Random.nextBoolean()) 0.02f else -0.02f`
  - `timeOffset = Random.nextFloat() * 100f`
  - `timeAccumulator = 0f`
- En `update(deltaTime, windSpeed)`:
  - Acumular el tiempo: `timeAccumulator += deltaTime`.
  - Actualizar `positionX` sumando `driftSpeed` a `windSpeed`.
  - Aplicar el desvanecimiento normal (`fadeSpeed`) para calcular la `baseOpacity`.
  - Si no está desvaneciéndose (`!isFadingOut`), sumarle a la opacidad el resultado de la función seno:
    ```kotlin
    val oscillation = sin(timeAccumulator + timeOffset) * 0.05f
    opacity = (baseOpacity + oscillation).coerceIn(0.02f, 1.0f)
    ```

---

### Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- Añadir la prueba unitaria `testCloudDriftAndOpacityOscillation()` para verificar:
  - Que la deriva `driftSpeed` se inicialice a `+0.02f` o `-0.02f`.
  - Que las nubes se muevan incluso cuando `windSpeed = 0f`.
  - Que la opacidad de la nube oscile con el tiempo cuando no está desvaneciéndose.

---

## Verification Plan

### Automated Tests
- Ejecuatar la suite de pruebas unitarias locales para confirmar que todo compile y pase:
```bash
./gradlew testDebugUnitTest
```

### Manual Verification
- Comprobar visualmente que con viento neutro (0%) o vertical:
  - Las nubes continúen moviéndose de manera lenta pero persistente e independiente (unas a la izquierda y otras a la derecha).
  - La opacidad de cada nube varíe levemente dando sensación de volumen y vaporosidad cambiante.
