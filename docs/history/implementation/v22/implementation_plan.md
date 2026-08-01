# Plan de Implementación – Aumento de Tamaño Mínimo de Rayos y Respiración Exclusiva de Nubes (v22)

Este plan describe los cambios para incrementar en un 50% el tamaño mínimo de los rayos y modificar la oscilación de respiración de las nubes de forma que cada instancia de nube pueda únicamente crecer o únicamente encogerse (pero no ambas), duplicando la amplitud máxima de crecimiento y de encogimiento.

## User Review Required

> [!IMPORTANT]
> - **Tamaño Mínimo de Rayos**: En `Lightning.kt`, la altura mínima (`minHeight`) de los rayos se incrementará en un 50%, pasando de `0.3f` (15% del alto de pantalla) a **`0.45f`** (22.5% del alto de pantalla).
> - **Respiración Exclusiva de Nubes**: En `Cloud.kt`, añadimos una propiedad booleana `onlyGrows` que determina si una nube solo crece o solo se encoge.
>   - La oscilación usará un factor de onda senoidal desplazada `(sin(pulseTime) + 1.0f) * 0.5f` para variar suavemente entre `0` y `1`.
>   - Si `onlyGrows` es `true`, el tamaño varía entre `baseScale` y `baseScale * 1.5625` (crecimiento máximo aumentado en 100%, de `0.28125` a `0.5625`).
>   - Si `onlyGrows` es `false`, el tamaño varía entre `baseScale * 0.4375` y `baseScale` (encogimiento máximo aumentado en 100%, de `0.28125` a `0.5625`).

---

## Proposed Changes

### 1. Modelo de Rayos

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Incrementar la altura mínima (`minHeight`) del rayo de `2.0f * 0.15f` a `2.0f * 0.225f` (aumento del 50%, a `0.45f`).

---

### 2. Modelo de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- Añadir la propiedad `var onlyGrows: Boolean = true` a la clase.
- En la función `reset()`, inicializar `onlyGrows = Random.nextBoolean()` de manera aleatoria.
- En `update()`, modificar la lógica de escala:
  - Calcular `breathingFactor = (sin(pulseTime) + 1.0f) * 0.5f`.
  - Calcular la amplitud `0.5625f * dynamicsSpeed` (duplicada de `0.28125f`).
  - Aplicar crecimiento si `onlyGrows` es `true`, o reducción si es `false`.

---

### 3. Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- En `testDiagonalLightningTrigger()`, cambiar la aserción de `scaleY` para validar que esté en el rango `0.45f..1.5f` (en lugar de `0.3f..1.5f`).
- En `testCloudDriftAndScaleOscillation()`, actualizar las aserciones de oscilación de escala de la nube para validar desviaciones máximas de `baseSc * 0.5626f` y agregar una aserción específica para comprobar el comportamiento exclusivo (solo crece o solo se encoge).

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```
