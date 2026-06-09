# Plan de Implementación – Ajuste de Velocidad Mínima y Eliminación de Efecto de Agitado

Ajustaremos la velocidad mínima de la lluvia para incrementarla en un 50%. Además, eliminaremos o verificaremos la total ausencia de cualquier lógica de sensor o efecto al agitar el dispositivo.

## Proposed Changes

### Capa de Físicas y Entidades

#### [MODIFY] [RainDrop.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/RainDrop.kt)
- Modificar el cálculo de `speedFactor` en `reset()` para que el valor mínimo (cuando `rainSpeed = 0`) pase de `0.2f` a `0.3f` (un incremento del 50% en la velocidad mínima). La fórmula cambia a:
  `val speedFactor = 0.3f + (rainSpeed / 100f) * 1.5f`
  Esto mantiene la velocidad máxima a 100% en `1.8f`.

### Eliminación del Efecto de Agitado
- Tras realizar un análisis exhaustivo del código, no existen implementaciones de `SensorManager`, `SensorEventListener`, ni permisos/declaraciones de sensores en el archivo [AndroidManifest.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/AndroidManifest.xml) o los servicios del Wallpaper. Se confirma la total ausencia de cualquier efecto al agitar el dispositivo, garantizando que el sistema no responde a estímulos físicos del sensor de movimiento.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar pruebas unitarias con Gradle:
  `./gradlew testDebugUnitTest`
