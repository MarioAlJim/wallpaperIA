# Plan de Implementación – Ajuste de Opacidad, Tamaño y Oscilación en Y de Nubes (v19)

Este plan describe los cambios para aumentar el factor de opacidad de las nubes en un 50%, el tamaño (amplitud de respiración) en un 25%, el tamaño máximo en un 25%, e incorporar un movimiento vertical sutil (oscilación) en el eje Y.

## User Review Required

> [!IMPORTANT]
> - **Ajuste de Opacidad**: El factor base de velocidad de desvanecimiento (`activeFadeSpeed`) en `Cloud.kt` se incrementará en un 50%, pasando de `0.225f` a `0.3375f`.
> - **Aumento de Tamaño (Oscilación/Respiración)**: La amplitud de la oscilación de tamaño de las nubes aumentará en un 25%, pasando de `0.15f` (±15%) a `0.1875f` (±18.75%).
> - **Aumento de Tamaño Máximo**: En la función `reset()`, el límite `maxScale` de las nubes pasará de `1.25f` a `1.5625f` (un incremento del 25%).
> - **Movimiento en el Eje Y**: Se añadirá `basePositionY` en `Cloud.kt` para almacenar la posición Y inicial, y se aplicará una oscilación sutil basada en una función seno:
>   `positionY = basePositionY + sin(pulseTime * 0.5f) * 0.03f * baseScale * dynamicsSpeed`
>   Esto hace que el movimiento sea dinámico y responda al control de velocidad de dinámicas de nubes.

---

## Proposed Changes

### 1. Modelo de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
* Definir variable de estado:
  `var basePositionY: Float = positionY`
* En `update()`, modificar la fórmula de escala:
  `scale = baseScale * (1.0f + sin(pulseTime) * 0.1875f * dynamicsSpeed)`
* En `update()`, aplicar el desplazamiento vertical en el eje Y:
  `positionY = basePositionY + sin(pulseTime * 0.5f) * 0.03f * baseScale * dynamicsSpeed`
* En `update()`, actualizar el factor base de `activeFadeSpeed` a `0.3375f`:
  `val activeFadeSpeed = 0.3375f * windFactorOpacity * (0.2f + 0.8f * dynamicsSpeed)`
* En `reset()`, actualizar `maxScale` a `1.5625f`:
  `val maxScale = 1.5625f`
* En `reset()`, guardar la posición base de Y:
  `basePositionY = positionY`

---

### 2. Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
* Actualizar/añadir aserciones en `testCloudDriftAndScaleOscillation()` para verificar:
  * Que la escala de la nube oscila dentro del rango de ±18.75% de `baseScale`.
  * Que la posición Y varía sutilmente a lo largo del tiempo bajo dinámicas activas y permanece estática bajo `dynamicsSpeed = 0f`.
* Actualizar `testCloudDynamicsSpeedConfig()` para verificar que la velocidad de desvanecimiento de seguridad al 20% con el nuevo factor base `0.3375f` es `0.3375f * 0.2f = 0.0675f`.

---

## Verification Plan

### Automated Tests
* Ejecución de pruebas unitarias:
  ```bash
  ./gradlew testDebugUnitTest
  ```
