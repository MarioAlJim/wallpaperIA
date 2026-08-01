# Plan de Implementación - Motor de Nubes Procedurales Estilo Fluffy

El objetivo es actualizar el motor de nubes del modo día y noche en `wallpaper-sunny` para replicar el dibujo procedural de nubes esponjosas (con 5 elipses para el cuerpo, 5 elipses para la sombra y volumen/paralaje) en OpenGL ES 3.0, evitando el uso de texturas rasterizadas estáticas.

## User Review Required

> [!IMPORTANT]
> **Detalles clave del diseño:**
> 1. **Cálculo en Fragment Shader**: Renderizaremos las 10 elipses (5 de sombra, 5 de cuerpo) de forma procedural en un Fragment Shader personalizado. Esto garantiza bordes perfectamente suaves (anti-aliasing matemático con `smoothstep`), escalabilidad infinita sin pixelación y óptimo rendimiento.
> 2. **Evitar Regresiones en Modo Tormenta**: Colocaremos los nuevos shaders dentro del módulo `wallpaper-sunny` en lugar del módulo `core` para que no afecten a las nubes del modo de tormenta (`wallpaper-storm`).
> 3. **Independencia de Aspect Ratio**: Normalizaremos las coordenadas en el shader considerando la relación de aspecto actual de los quads (`2.4`), logrando que las nubes se dibujen sin deformaciones.

## Proposed Changes

### Módulo Soleado (`wallpaper-sunny`)

---

#### [NEW] [procedural_cloud.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/assets/shaders/procedural_cloud.vert)
- Crear el Vertex Shader correspondiente para pasar las coordenadas del quad (`aPosition`) y las coordenadas de textura (`aTexCoord` que representarán las coordenadas normalizadas del plano de dibujo de la nube).

#### [NEW] [procedural_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/assets/shaders/procedural_cloud.frag)
- Crear el Fragment Shader que dibuja las elipses:
  - Definir la función `ellipseAlpha(vec2 p, vec2 center, vec2 r)` para calcular el factor de forma y suavizado de cada elipse.
  - Implementar las 5 elipses de cuerpo y las 5 elipses de sombra con las proporciones y desplazamientos exactos trasladados del código de referencia JS.
  - Invertir la orientación del eje Y (`0.5 - vTexCoord.y`) para corregir la visualización y evitar que parezcan estar al revés debido al origen de coordenadas de textura en OpenGL.
  - Introducir variaciones dinámicas en la forma de cada nube usando un hash pseudo-aleatorio basado en la semilla `uVariation`.
  - Aplicar el color de la sombra (`vec3(0.627, 0.765, 0.902)`) atenuado/multiplicado por la iluminación ambiente (`uCloudColor`) y el color del cuerpo de la nube (`uCloudColor`).
  - Mezclar el color del cuerpo y de la sombra según sus respectivos factores de cobertura y aplicar la opacidad global (`uOpacity`).

#### [MODIFY] [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)
- Modificar la carga de los archivos de sombreadores para las nubes:
  - Cambiar `shaders/cloud.vert` a `shaders/procedural_cloud.vert`.
  - Cambiar `shaders/cloud.frag` a `shaders/procedural_cloud.frag`.
- Obtener y enlazar la variable uniforme `uVariation` pasando el `cloud.id` para que cada nube tenga una variación de forma única y estable.

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/Cloud.kt)
- Disminuir el tamaño máximo de las nubes (`maxScale`) en un 20% (de `0.6f` a `0.48f`).
- Incrementar el límite superior de altura (`maxY` en el método `reset()`) de `1.0f - scale * 0.5f` a `1.1f - scale * 0.5f` para permitir que las nubes se posicionen más alto en la pantalla.

## Verification Plan

### Automated Tests
- Ejecutar la suite completa de pruebas unitarias del proyecto para validar que no haya regresiones ni problemas de compilación:
  `.\gradlew.bat test`

### Manual Verification
- Inspeccionar visualmente que las nubes en el simulador / dispositivo se rendericen de forma esponjosa con su sombreado tridimensional correspondiente y bordes suaves vectoriales.
