# Walkthrough – Deriva Atmosférica, Oscilación de Opacidad y Transición Suave en Nubes

Hemos completado el desarrollo para introducir la "Deriva Atmosférica" y la "Oscilación de Opacidad por Seno" en las nubes, además de solucionar problemas de pop visuales y transiciones de creación y remoción, en la rama `feat-cloud-drift`.

## Cambios Realizados

### 1. Deriva Atmosférica (Drift Speed)
* En [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt):
  * Definimos el atributo `driftSpeed` inicializado aleatoriamente en `reset()` a `+0.02f` o `-0.02f` para que cada nube tenga un rumbo propio e independiente.
  * Modificamos la ecuación de actualización en `update()` para sumar la deriva al viento:
    $$\text{positionX} += (\text{windSpeed} + \text{driftSpeed}) \times \text{speedFactor} \times \text{speedZFactor} \times \text{deltaTime}$$
  * Esto garantiza que incluso si el viento está inactivo o sopla en dirección vertical (`windSpeed == 0f`), las nubes mantengan un leve movimiento horizontal que evidencia el efecto de paralaje 3D.

### 2. Oscilación de Opacidad por Seno
* En [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt):
  * Se añadieron los campos `timeOffset` (número aleatorio entre `0` y `100` en `reset()`) y `timeAccumulator` (que se incrementa en `update()`).
  * En cada ciclo de físicas de `update()`, se aplica una oscilación basada en la función seno (`sin`) al valor de la opacidad:
    $$\text{oscillation} = \sin(\text{timeAccumulator} + \text{timeOffset}) \times 0.05f \times \text{progress}$$
  * **Atenuación Progresiva**: Para evitar que las nubes recién creadas inicien su ciclo con un destello/pop opaco, multiplicamos la oscilación por un factor `progress = baseOpacity / targetOpacity` (de manera que si está totalmente transparente al crearse, la oscilación vale exactamente `0`).

### 3. Transición Suave en Nubes (Fade In / Fade Out)
* **Aparición (Fade In)**: Las nubes nuevas añadidas al aumentar la densidad se crean con `opacity = 0f` para desvanecerse suavemente.
* **Desaparición (Fade Out)**: En [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt), las nubes excedentes se marcan con `isFadingOut = true` y disminuyen su opacidad a velocidad constante antes de ser removidas físicamente.

### 4. Corrección de Envoltura (Wrapping Pop)
* Reposicionamos las nubes que cruzan los límites de la pantalla calculando sus coordenadas de reingreso usando la **nueva escala** generada en `reset()`, evitando saltos visuales en los bordes.

### 5. Pruebas Unitarias de Regresión
* En [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt):
  * **`testCloudDriftAndOpacityOscillation`**: Verifica la correcta inicialización de `driftSpeed` y que las nubes se desplacen y oscilen en opacidad con viento inactivo.
  * **`testCloudFadeInAndFadeOut`**: Valida el comportamiento asíncrono de desvanecimiento con desvanecimientos progresivos y borrado.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` con todas las pruebas validadas correctamente.
