# Walkthrough – Ajuste de Parámetros y Movimiento Y de Nubes (v19)

Hemos completado la implementación de los ajustes en la simulación de nubes, aumentando su opacidad de desvanecimiento, su amplitud de respiración, su tamaño máximo e incorporando un movimiento sutil en el eje Y.

## Cambios Realizados

### 1. Ajustes Físicos en el Modelo de Nubes
- **[Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)**:
  - **Opacidad (+50%)**: Se incrementó la velocidad base de transición de opacidad (`activeFadeSpeed`) de `0.225f` a `0.3375f`.
  - **Tamaño de Respiración (+25%)**: Se aumentó la amplitud del seno de variación de tamaño de ±15% a **±18.75%** (`0.1875f`).
  - **Tamaño Máximo (+25%)**: Se incrementó `maxScale` en el método `reset()` de `1.25f` a **`1.5625f`**.
  - **Movimiento en Eje Y**: Se introdujo el campo `basePositionY` y la fórmula para desplazar verticalmente la nube:
    `positionY = basePositionY + sin(pulseTime * 0.5f) * 0.03f * baseScale * dynamicsSpeed`
    Este movimiento oscilatorio se detiene por completo si `dynamicsSpeed` está en 0%.

### 2. Pruebas Unitarias Robustas
- **[SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)**:
  - **Límites de Escala**: Se ajustó `testCloudDepthAndParallax()` para verificar la escala máxima de las nubes con el nuevo factor `1.5625f`.
  - **Amplitud y Movimiento Y**: Se actualizó `testCloudDriftAndScaleOscillation()` para validar que el tamaño oscile en el rango de ±18.75% y que la posición Y cambie en el tiempo bajo dinámicas activas.
  - **Velocidad Mínima y Estática**: Se actualizó `testCloudDynamicsSpeedConfig()` verificando la velocidad de desvanecimiento de seguridad al 20% (`0.0675f`) y comprobando que el eje Y permanece estático al desactivar las dinámicas.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas unitarias:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Las 20 pruebas unitarias de físicas pasaron correctamente y con éxito.
