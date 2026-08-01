# Plan de Implementación – Rayos Interactivos Táctiles (v25)

Este plan describe los cambios necesarios para permitir al usuario invocar rayos al tocar la pantalla del dispositivo. El rayo tendrá el mismo comportamiento que los rayos aleatorios, pero caerá cerca de donde se dio el toque. Adicionalmente, esta opción podrá desactivarse desde la pantalla de configuración.

## User Review Required

> [!IMPORTANT]
> - **Cálculo Geométrico del Rayo**:
>   - Para que el rayo "caiga cerca" del toque en la pantalla de forma natural, determinaremos el borde de origen (superior, izquierdo o derecho) basándonos en la coordenada X e Y del toque.
>   - Calcularemos el ángulo de rotación $\theta$ de manera aleatoria, y luego derivaremos la altura (`scaleY`) y el punto de partida (`startX` o `startY`) de tal manera que el extremo inferior del rayo coincida con las coordenadas del toque.
> - **Control de Hilos**:
>   - Dado que los eventos de toque se reciben en el hilo de la interfaz de usuario (UI Thread) y la simulación/renderizado ocurre en el hilo de OpenGL (`GLRenderThread`), utilizaremos una referencia `pendingTouch` de tipo `@Volatile` en `SceneManager` para transferir el evento de forma segura y libre de condiciones de carrera.

---

## Open Questions

> [!NOTE]
> No hay preguntas abiertas críticas en este momento. Se asume que el comportamiento por defecto de la opción táctil estará activado.

---

## Proposed Changes

### 1. Configuración de la Aplicación

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Agregar el método `fun isInteractiveLightningEnabled(): Boolean` a la interfaz.

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Agregar la clave de preferencia `KEY_INTERACTIVE_LIGHTNING_ENABLED = "interactive_lightning_enabled"`.
- Definir el valor predeterminado `DEFAULT_INTERACTIVE_LIGHTNING_ENABLED = true`.
- Implementar `isInteractiveLightningEnabled(): Boolean` y `setInteractiveLightningEnabled(enabled: Boolean)`.

---

### 2. Interfaz de Usuario (Configuración)

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Agregar un nuevo interruptor (`SwitchMaterial` con id `switchInteractiveLightning`) dentro del acordeón de configuración de Rayos (Accordion 3).
- Incluir un texto descriptivo indicando que permite invocar rayos al tocar la pantalla.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Inicializar y enlazar el interruptor `switchInteractiveLightning` con el valor de `configManager.isInteractiveLightningEnabled()`.
- Agregar un listener para persistir el cambio cuando el usuario interactúe con el switch.

---

### 3. Servicio de Wallpaper e Interacción Táctil

#### [MODIFY] [StormWallpaperService.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/StormWallpaperService.kt)
- En `StormEngine.onCreate()`, llamar a `setTouchEventsEnabled(true)` para habilitar la detección de gestos táctiles.
- Sobrescribir el método `onTouchEvent(event: MotionEvent?)`.
- En caso de evento `ACTION_DOWN` y si la opción interactiva está habilitada, enviar las coordenadas en píxeles `(event.x, event.y)` al `sceneManager` usando un nuevo método de encolado.

---

### 4. Simulación y Lógica de Escena

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- Almacenar los valores de `viewWidth` y `viewHeight` en la función `onSurfaceChanged` para realizar la conversión de coordenadas de píxeles de pantalla a coordenadas normalizadas de OpenGL.
- Agregar una variable thread-safe `@Volatile private var pendingTouch: Pair<Float, Float>? = null`.
- Implementar la función `queueTouch(x: Float, y: Float)` para almacenar temporalmente el toque desde el hilo de la UI.
- En la función `update(deltaTime: Float)`, procesar el toque pendiente:
  - Convertir `x` e `y` a coordenadas OpenGL:
    - $openglX = (x / viewWidth \times 2.0 - 1.0) \times aspectRatio$
    - $openglY = 1.0 - (y / viewHeight \times 2.0)$
  - Buscar un rayo inactivo en el pool y dispararlo en esas coordenadas utilizando la nueva función `triggerAt` de `Lightning`.

#### [MODIFY] [Lightning.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Lightning.kt)
- Agregar la función `triggerAt(touchX: Float, touchY: Float, aspectRatio: Float, textureCount: Int, colorIndex: Int, durationPercentage: Int = 30, isInternalOnly: Boolean = false)`.
- La lógica interna de `triggerAt` realizará lo siguiente:
  - Seleccionar la textura, color, duración y escala en X de la misma forma pseudoaleatoria que en `trigger()`.
  - Determinar el tipo de borde inicial (`borderType`):
    - Si el toque es alto (`touchY > 0.4f`), usar siempre el borde superior (`0`).
    - Si es bajo, y está a la izquierda (`touchX < -aspectRatio / 3`), elegir aleatoriamente entre borde superior (`0`) y borde izquierdo (`1`).
    - Si es bajo, y está a la derecha (`touchX > aspectRatio / 3`), elegir aleatoriamente entre borde superior (`0`) y borde derecho (`2`).
    - De lo contrario, usar borde superior (`0`).
  - Calcular la posición inicial `startX`, `startY`, el ángulo de rotación `rotationAngle` y la altura `scaleY`:
    - **Borde Superior (`0`)**:
      - `startY = 1.0f`
      - `rotationAngle` aleatorio en el rango `[-15f, 15f]`.
      - `scaleY = ((1.0f - touchY) / cos(angleRad)).coerceIn(minHeight, maxHeight)`
      - `startX = touchX - scaleY * sin(angleRad)`
    - **Borde Izquierdo (`1`)**:
      - `startX = -aspectRatio`
      - `rotationAngle` aleatorio en el rango `[20f, 45f]`.
      - `scaleY = ((touchX + aspectRatio) / sin(angleRad)).coerceIn(minHeight, maxHeight)`
      - `startY = (touchY + scaleY * cos(angleRad)).coerceIn(0.3f, 1.0f)`
    - **Borde Derecho (`2`)**:
      - `startX = aspectRatio`
      - `rotationAngle` aleatorio en el rango `[-45f, -20f]`.
      - `scaleY = ((touchX - aspectRatio) / sin(angleRad)).coerceIn(minHeight, maxHeight)`
      - `startY = (touchY + scaleY * cos(angleRad)).coerceIn(0.3f, 1.0f)`
  - Establecer `positionX = startX` y `positionY = startY - scaleY * 0.5f`.
  - Marcar el rayo como activo.

---

### 5. Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- Agregar casos de prueba unitaria para validar:
  - La correcta conversión de coordenadas de píxeles a OpenGL.
  - Que la función `triggerAt` asigne posiciones y dimensiones correctas que ubiquen el extremo del rayo cerca del punto de toque.
  - Que al desactivar la opción en configuración, los toques en pantalla no generen rayos.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales:
```bash
./gradlew testDebugUnitTest
```

### Manual Verification
- Compilar la aplicación y desplegar el Live Wallpaper en un emulador o dispositivo Android físico.
- Abrir la pantalla de configuración y habilitar "Rayos Interactivos".
- Aplicar el fondo de pantalla, tocar la pantalla en distintas posiciones (centro, parte baja, laterales) y confirmar que los rayos caen cerca del toque.
- Regresar a configuración, deshabilitar la opción, y comprobar que tocar la pantalla ya no genera rayos.
