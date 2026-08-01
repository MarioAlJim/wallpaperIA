# Plan para Corregir Pantalla Negra en la Pantalla de Inicio (v3)

Este plan describe la solución para corregir el error por el cual el fondo de pantalla animado se muestra correctamente en la previsualización pero se ve negro al establecerse definitivamente en la pantalla de inicio.

## Análisis del Problema

El ciclo de vida del `WallpaperService` de Android difiere al previsualizar y al aplicar el fondo de pantalla:
1. En la previsualización, la ventana del wallpaper se inicia directamente en estado visible (`visible = true`). El hilo de renderizado inicializa EGL, crea la superficie, la activa y luego llama a `renderer.onSurfaceCreated()` para compilar shaders y cargar texturas dentro de un contexto OpenGL válido.
2. Al aplicar el wallpaper a la pantalla de inicio, Android crea un nuevo motor de fondo que inicialmente no es visible (`visible = false`). El hilo de renderizado inicia EGL pero **no crea la superficie de ventana** debido a la condición `if (visible ...)`.
3. Sin embargo, el hilo prosigue de forma inmediata a llamar a `renderer.onSurfaceCreated()`. Dado que no hay ninguna superficie activa asociada ni un contexto de renderizado configurado como actual en el hilo (`eglMakeCurrent` nunca se ejecutó), todas las llamadas de OpenGL ES (compilación de shaders, carga de texturas, configuración de estados) **fallan silenciosamente**.
4. Al salir al launcher, la visibilidad cambia a `true`, el hilo crea la superficie y activa el contexto, pero los shaders y texturas necesarios para dibujar nunca fueron cargados en ese contexto. El resultado es un fondo negro.

## Solución Propuesta

Garantizar que al arrancar el hilo de renderizado se cree la superficie EGL y se active el contexto actual si la superficie del sistema (`SurfaceHolder.surface`) es válida, independientemente del estado inicial de visibilidad. De este modo, `renderer.onSurfaceCreated()` se ejecutará siempre con un contexto OpenGL activo y válido. Si la visibilidad inicial era falsa, el hilo destruirá inmediatamente la superficie y entrará en suspensión conservando los shaders cargados en el contexto EGL.

## User Review Required

> [!IMPORTANT]
> - Este cambio modifica únicamente la secuencia de inicialización del hilo de renderizado (`GLRenderThread`). No afecta a la lógica física de las partículas ni a la interfaz de configuración.

## Proposed Changes

---

### Motor de Renderizado

#### [MODIFY] [GLRenderThread.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/GLRenderThread.kt)
- Modificar la verificación de inicialización en `run()` para crear la superficie EGL inicial omitiendo la condición `visible`, basándose únicamente en la validez de `surfaceHolder.surface`.

## Verification Plan

### Manual Verification
- Compilar e instalar la aplicación.
- Abrir la aplicación y pulsar "Establecer como Fondo de Pantalla".
- Verificar que en el previsualizador nativo la lluvia cae correctamente.
- Establecer el fondo de pantalla en la pantalla de inicio (Home Screen).
- Volver a la pantalla de inicio del dispositivo y comprobar que la animación de lluvia diagonal continúa renderizándose de forma fluida y sin pantallas negras.
