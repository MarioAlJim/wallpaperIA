# Walkthrough – Rayos Simultáneos y Spawns Laterales

Hemos implementado la capacidad de visualizar múltiples rayos cayendo simultáneamente al aumentar el límite superior de la frecuencia de generación (hasta un mínimo de 80ms de retraso). Asimismo, los rayos se pueden generar de forma diagonal desde los costados y esquinas superiores de la pantalla con una inclinación angular realista.

## Cambios Realizados

### 1. Físicas y Orquestación de Escena
* **Entidad Rayo (`Lightning.kt`)**:
  - Se agregó la propiedad `rotationAngle` para almacenar la rotación en grados del rayo.
  - Se modificó `trigger()` para soportar tres patrones de spawn: top-center estándar (ángulo leve de $-10^{\circ}$ a $+10^{\circ}$), spawn lateral izquierdo (ángulo positivo de $+15^{\circ}$ a $+40^{\circ}$) y spawn lateral derecho (ángulo negativo de $-15^{\circ}$ a $-40^{\circ}$).
  - Se introdujo cálculo trigonométrico (`scaleY = 2f / cos(rad)`) para asegurar que los rayos inclinados sobrepasen la base de la pantalla.
  - La coordenada `positionY` se calcula alineando el pivote de rotación al punto de nacimiento superior deseado.
* **Manejador de Escena (`SceneManager.kt`)**:
  - Se sustituyó la variable única `lightning` por `val lightnings = List(8) { Lightning() }` para alojar hasta 8 rayos en pantalla simultáneamente.
  - Se declaró `val lightning: Lightning get() = lightnings[0]` para mantener total compatibilidad con tests y código heredado.
  - Se modificó `update()` para iterar sobre el pool de rayos y disparar el primer elemento disponible (`!it.isActive`).
  - Se mapearon los retardos de frecuencia en `setupNextLightningDelay()`. La frecuencia máxima (100) ahora produce un retraso de 0.08 segundos (80 ms).

### 2. Renderizado OpenGL (`StormRenderer.kt`)
* **Mezcla del Destello de Fondo**:
  - Se modificó `drawFrame()` para calcular el color promedio y el pico de intensidad de todos los rayos que estén activos simultáneamente, evitando destellos sobresaturados o planos.
* **Dibujado de Múltiples Quads**:
  - `drawLightning()` ahora recibe una lista de rayos. Dibuja un único destello de fondo promediado y luego dibuja secuencialmente cada rayo físico activo aplicando las transformaciones de matriz (traslación a la coordenada de nacimiento, rotación alrededor de su extremo superior, y escala).

### 3. Interfaz de Ajustes (`WallpaperSettingsActivity.kt`)
- Se actualizaron las etiquetas dinámicas de frecuencia para que el usuario entienda los nuevos modos extremos: "Tempestad extrema" (90% a 99%) y "Máximo caos (múltiples rayos)" (100%).

### 4. Cobertura de Pruebas (`SceneManagerTest.kt`)
- Se actualizaron las aserciones de cálculo de retardo a los nuevos valores objetivos (20s, 5s, 1.5s, 0.08s).
- Se añadió la prueba `testDiagonalLightningTrigger` para corroborar que los límites de rotación, dimensiones trigonométricas de la escala y coordenadas de inicio cumplan con las tolerancias del simulador.

## Commits Realizados

- `feat(ui): update lightning frequency seekbar labels for new high-frequency modes`
- `feat(physics): support simultaneous lightnings, diagonal spawning, and high frequencies`
- `feat(renderer): draw multiple active lightnings with correct rotation and accumulated flash`
- `test(physics): update test delay ranges and add testDiagonalLightningTrigger`

## Verificación

### Pruebas Automatizadas
Ejecutamos con éxito la suite completa de pruebas unitarias:
```bash
.\gradlew.bat testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` en 5s.
