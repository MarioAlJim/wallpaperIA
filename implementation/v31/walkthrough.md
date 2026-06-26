# Walkthrough v31 - Motor de Nubes Procedurales Estilo Fluffy

Hemos completado la implementación del motor de nubes procedurales para los modos día y noche del fondo de pantalla soleado (`wallpaper-sunny`).

## Cambios Realizados

### Módulo Soleado (`wallpaper-sunny`)

#### [NEW] [procedural_cloud.vert](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/assets/shaders/procedural_cloud.vert)
- Vertex shader dedicado que mapea las posiciones de los vértices y pasa las coordenadas UV normalizadas.

#### [NEW] [procedural_cloud.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/assets/shaders/procedural_cloud.frag)
- Fragment shader procedural que calcula la forma tridimensional de las nubes usando la fórmula matemática de la elipse con anti-aliasing (`smoothstep`).
- Dibuja hasta 8 elipses (3 fijas y 5 opcionales dinámicas por semilla) para el cuerpo de la nube y sus respectivas sombras desplazadas y translúcidas (color `rgb(160, 195, 230)`).
- **Corrección de Orientación**: Se invirtió el mapeo vertical del eje Y (`0.5 - vTexCoord.y`) para evitar que las nubes se muestren invertidas ("al revés").
- **Variaciones de Forma y Cantidad de Elipses**: Se introdujo generación pseudo-aleatoria (hash) basada en la semilla `uVariation` no solo para distorsionar la posición y el radio de las elipses, sino también para habilitar/deshabilitar de forma condicional 5 elipses opcionales (incluyendo 3 nuevas cúpulas: far-left, far-right, y top-center). Esto permite que las nubes varíen dinámicamente entre 3 y 8 elipses, logrando siluetas sumamente variadas (redondeadas, alargadas o de múltiples cúpulas).
- La sombra se adapta y tiñe dinámicamente con la luz ambiente (`uCloudColor`) para que los amaneceres, atardeceres y noches se muestren con colores correctos y armoniosos.
- Realiza el mezclado y la mezcla de transparencias (alfa) de forma nativa en la GPU para optimizar el rendimiento.

#### [MODIFY] [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)
- Se actualizó el motor de renderizado para cargar `shaders/procedural_cloud.vert` y `shaders/procedural_cloud.frag` en lugar de los shaders compartidos rasterizados del módulo `core`.
- Se configuró el envío del uniform `uVariation` pasando el `cloud.id` para inicializar el generador de formas aleatorias único de cada nube.
- Se ajustó la trayectoria de la luna en modo combinado a la nueva fórmula `y = 0.85f - 1.15f * (x * x)` para evitar desbordamientos.

#### [MODIFY] [moon.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/assets/shaders/moon.frag)
- **Halo Lunar en Luna Llena**: Se agregó un delgado anillo/halo de luz alrededor de la luna (`ringR = 0.62`, `ringThickness = 0.008`) con un factor de transparencia del 35%. Este anillo solo es visible cuando la luna está en fase llena (`uPhase == 4`), y se atenúa de forma natural por la intensidad ambiental (`uHaloIntensity` y `uIntensity`).

#### [MODIFY] [Moon.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/Moon.kt)
- **Ajuste de Altura Máxima**: Se modificó la ecuación parabólica a `y = 0.85f - 1.15f * (x * x)` en las trayectorias de lado a lado (L2R y R2L) para rebajar levemente el cenit de la luna (de `1.0f` a `0.85f`), evitando que se recorte en el borde superior de la pantalla debido a su radio/escala, mientras que sigue poniéndose completamente bajo el horizonte en los extremos (`Y = -1.09f`).

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/Cloud.kt)
- **Disminución del Tamaño Máximo**: Se redujo el tamaño máximo de las nubes (`maxScale`) en un 20% (de `0.6f` a `0.48f`).
- **Aumento de la Altura Máxima**: Se modificó la cota máxima del eje vertical (`maxY`) de `1.0f - scale * 0.5f` a `1.1f - scale * 0.5f` para permitir que las nubes floten más alto en el escenario.

---

## Resultados de Verificación

### Pruebas Automatizadas
Se ejecutó una limpieza y la suite completa de pruebas unitarias locales:
`.\gradlew.bat clean test`

**Resultado:**
```
BUILD SUCCESSFUL in 36s
79 actionable tasks: 79 executed
```
Todas las pruebas de compilación y lógica de negocio pasaron satisfactoriamente.
