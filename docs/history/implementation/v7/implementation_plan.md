# Plan de Implementación – Suavizado de la Transición de Viento y Velocidad

Para evitar que las gotas de lluvia parpadeen o salten de posición bruscamente al mover los controles de configuración (SeekBar), implementaremos una interpolación lineal suave (lerp) del ángulo del viento y la velocidad de la lluvia en el hilo de renderizado. En lugar de reubicar y reiniciar las gotas inmediatamente ante cambios de configuración, estas adaptarán su velocidad y ángulo en pleno vuelo.

## User Review Required

> [!NOTE]
> Este cambio elimina la reubicación abrupta de las gotas de lluvia (`drop.reset`) ante modificaciones de viento en SharedPreferences. En su lugar, el ángulo y velocidad se interpolan de manera constante cada fotograma, modificando la velocidad de las gotas en pleno vuelo.

## Proposed Changes

### Capa de Físicas y Entidades

#### [MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt)
- Agregar propiedades privadas `baseSpeed` y `angleOffset` para almacenar la velocidad inicial individual y la desviación aleatoria de ángulo de cada gota de lluvia.
- Cambiar la firma de `reset` para aceptar `windAngle: Float` y `rainSpeed: Float`.
- Implementar una función `updateVelocity(windAngle: Float, rainSpeed: Float)` que recalcule `velocityX`, `velocityY`, `dirX` y `dirY` basándose en los valores flotantes interpolados, sin alterar la posición actual de la gota de lluvia.

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Definir propiedades para almacenar los valores objetivo (`targetWindAngle`, `targetRainSpeed`) y los valores actuales interpolados (`currentWindAngle`, `currentRainSpeed`).
- En `updateFromConfig()`, calcular el ángulo objetivo del viento en grados (ej. `-35°` para izquierda a máxima intensidad, `0°` para vertical, `+35°` para derecha) y actualizar los objetivos sin forzar un reset de las gotas.
- En `update(deltaTime)`, interpolar `currentWindAngle` y `currentRainSpeed` hacia sus objetivos de forma progresiva.
- Actualizar el bucle de actualización de lluvia para llamar a `drop.updateVelocity(currentWindAngle, currentRainSpeed)` en cada fotograma antes de mover las gotas.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar la suite de pruebas con Gradle para comprobar que no hay errores de compilación ni de lógica básica:
  `./gradlew testDebugUnitTest`

### Verificación Manual
- Compilar la aplicación y abrir la pantalla de Ajustes.
- Mover los deslizadores de "Fuerza del Viento", "Dirección de la Lluvia" y "Velocidad de la Lluvia". Comprobar que la lluvia se inclina y acelera suavemente en tiempo real en la previsualización sin parpadeos ni reinicios bruscos.
