# Plan de Implementación v30 - Ajustes de Rayos en Tormenta y Refinamiento del Modo Nocturno

Este plan abarca dos correcciones críticas implementadas en el proyecto:
1. **Aspecto y Visibilidad de Rayos en Modo Tormenta**: Mantener una relación de aspecto de 3:4 para los rayos a fin de evitar que se distorsionen y asegurar que no queden completamente fuera de la pantalla cuando se generen en los bordes.
2. **Refinamiento del Modo Nocturno y Combinado**: Corregir el bug que bloqueaba las nubes de noche en el borde izquierdo cuando la velocidad neta era negativa, y vincular su movimiento y comportamiento a la configuración de viento y velocidad dinámica del usuario.

## Proposed Changes

### Componente de Tormenta (`wallpaper-storm`)

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/main/java/com/wolf/wallpaper/storm/Lightning.kt)
- Calcular `scaleX` como `scaleY * 0.75f` en `trigger()` y `triggerAt()` para mantener la relación de aspecto 3:4.
- Desplazar la coordenada `positionX` hacia el interior en un factor de `scaleX * 0.25f` cuando los rayos se disparen en los bordes para mantener la parte central visible y cortada de forma natural.

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-storm/src/test/java/com/wolf/wallpaper/storm/SceneManagerTest.kt)
- Ajustar la aserción de `scaleX` en `testDiagonalLightningTrigger()` a la nueva escala esperada (`0.33f..1.69f`) y añadir la validación de la relación de aspecto 3:4 (`scaleX / scaleY = 0.75`).

---

### Componente Core (`core`)

#### [MODIFY] [Moon.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/Moon.kt)
- Modificar la fórmula del eje Y para la trayectoria parabólica L2R y R2L de la luna a `positionY = 1.0f - 1.3f * (positionX * positionX)` para que alcance la cima de la pantalla.

---

### Componente Soleado (`wallpaper-sunny`)

#### [MODIFY] [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)
- Condicionar la densidad de nubes de día y de noche según el `timeMode` activo en `onUpdate()`. Si el modo no está activo, su densidad se reduce a 0 para que se desvanezcan suavemente y se limpien de la lista.
- Sincronizar las nubes de noche (`nightClouds`) con las variables del viento y velocidad dinámica en lugar de usar valores fijos.
- Resolver el bug de reinicio infinito de posición (`positionX`) de las nubes de noche con velocidad negativa, reubicándolas al borde derecho (`aspectRatio + newHalfW`) al salir por la izquierda.
- Modificar la fórmula parabólica del sol (en modo día y modo combinado) y la luna (en modo combinado) a `y = 1.0f - 1.3f * (x * x)` para alcanzar el punto más alto del visor (`Y = 1.0f`) en su cenit.

## Verification Plan

### Automated Tests
- Validar la suite completa de pruebas unitarias del proyecto:
  `.\gradlew.bat test`
