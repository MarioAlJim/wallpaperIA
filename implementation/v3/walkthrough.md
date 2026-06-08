# Historial de Cambios - Versión 3.0 (Corrección de Inicialización OpenGL)

En este incremento se corrigió un problema de ciclo de vida que causaba una pantalla negra al establecer el wallpaper animado en la pantalla de inicio del dispositivo.

## Cambios Realizados

### Hilo de Renderizado OpenGL

#### [GLRenderThread.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/GLRenderThread.kt)
- Se modificó la inicialización en el método `run()` para eliminar el condicional de visibilidad al crear la superficie EGL inicial. Ahora la superficie EGL se crea siempre que la superficie física del sistema (`surfaceHolder.surface`) sea válida, sin importar si el wallpaper arranca visible o invisible.
- Esto asegura que `renderer.onSurfaceCreated()` se invoque siempre en presencia de un contexto OpenGL activo y actual, logrando compilar shaders y cargar texturas correctamente.
- Si el motor arranca invisible, el ciclo del hilo de renderizado entra inmediatamente en suspensión de forma segura tras liberar la superficie, manteniendo el contexto OpenGL intacto.
