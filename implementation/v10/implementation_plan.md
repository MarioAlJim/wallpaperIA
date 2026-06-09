# Plan de Implementación – Rayos Simultáneos y Spawns Laterales

Implementaremos una nueva lógica para los rayos de tormenta, incrementando la frecuencia máxima y permitiendo renderizar múltiples rayos al mismo tiempo. Además, los rayos podrán generarse desde las esquinas y laterales superiores de la pantalla de forma diagonal, logrando una tormenta mucho más dinámica y realista.

## User Review Required

- **Frecuencia Excesiva en Rango Alto**: En la frecuencia del 90% al 100%, el retardo de generación se reduce drásticamente (hasta 80 ms), permitiendo que múltiples rayos estén en pantalla al mismo tiempo. El texto descriptivo en ajustes reflejará este cambio ("Maximo caos" y "Tempestad extrema").
- **Spawns Diagonales Laterales**: Los rayos podrán nacer en los costados laterales superiores (izquierdo o derecho) y proyectarse hacia adentro de forma diagonal con ángulos de hasta 40 grados.

---

## Proposed Changes

### Capa de Físicas y Datos

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Añadir la propiedad `var rotationAngle: Float = 0f`.
- Modificar `trigger(aspectRatio, textureCount, colorIndex)` para soportar tres modos de generación aleatoria:
  1. **Top-Center (Estándar)**: Ángulo de inclinación leve ($-10^{\circ}$ a $+10^{\circ}$), naciendo en la parte superior central.
  2. **Lateral Izquierdo**: Nace en el borde superior-izquierdo o lateral-izquierdo superior y se proyecta en ángulo positivo ($+15^{\circ}$ a $+40^{\circ}$) hacia la derecha.
  3. **Lateral Derecho**: Nace en el borde superior-derecho o lateral-derecho superior y se proyecta en ángulo negativo ($-15^{\circ}$ a $-40^{\circ}$) hacia la izquierda.
- Ajustar `scaleY` de forma trigonométrica basándose en el coseno del ángulo de inclinación (`scaleY = 2f / cos(rad)`) para asegurar que el rayo alcance y sobrepase el límite inferior de la pantalla.
- Calcular la posición física del centro de la textura (`positionY = startY - scaleY * 0.5f`) de modo que el pivote de rotación quede alineado al punto de inicio.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Reemplazar la propiedad singular `val lightning = Lightning()` por un grupo de rayos `val lightnings = List(8) { Lightning() }`.
- Para mantener compatibilidad con cualquier otra llamada o tests existentes, declarar un getter delegado: `val lightning: Lightning get() = lightnings[0]`.
- Modificar la función `update(deltaTime)` para actualizar el ciclo de vida de todos los rayos activos en el pool.
- En la lógica de trigger, buscar el primer rayo inactivo (`!it.isActive`) y dispararlo.
- Rediseñar `setupNextLightningDelay()` para mapear las frecuencias altas a retrasos extremadamente cortos:
  - Freq 90..100% -> retrasos de 0.4s a 0.08s.

### Capa de Renderizado OpenGL

#### [MODIFY] [StormRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormRenderer.kt)
- En `drawFrame()`, calcular la acumulación de la intensidad de los destellos y promediar los colores de todos los rayos activos para teñir el color de fondo físico de la pantalla.
- Modificar la firma de `drawLightning` para recibir la colección `lightnings: List<Lightning>`.
- Orquestar un único renderizado del flash de pantalla completa combinando el brillo máximo y el color promedio de los rayos activos.
- Recorrer y renderizar las texturas de cada rayo activo, aplicando traslación al punto de nacimiento, rotación por `rotationAngle` en el pivote del extremo superior de la textura, y escala.

### Interfaz de Usuario y Ajustes

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Modificar `updateTextView` para la clave de frecuencia de rayos para describir con mayor fidelidad los nuevos modos rápidos:
  - De 90% a 99%: "Tempestad extrema (0.08–0.4s)"
  - 100%: "Máximo caos (múltiples rayos)"

---

## Plan de Verificación

### Pruebas Automatizadas
- Actualizar `testLightningFrequencyDelayCalculation` en [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt) para verificar los nuevos límites de demora a frecuencia máxima (0.08s).
- Correr `./gradlew testDebugUnitTest`.

### Verificación Manual
- Abrir la pantalla de ajustes, seleccionar la frecuencia al 100% (Máximo caos) y validar que se visualizan múltiples rayos de diferentes colores (si está en Aleatorio) cayendo simultáneamente desde los extremos superiores y laterales de la pantalla de forma diagonal.
