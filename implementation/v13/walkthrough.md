# Walkthrough – Animación de Dibujado Progresivo de Rayos

Hemos implementado un efecto visual realista en el dibujado de los rayos de tormenta, logrando que no se desplieguen de golpe, sino que se dibujen progresivamente de arriba hacia abajo (descenso del líder escalonado). Esto se consiguió mediante máscaras a nivel de fragment shader (descarte de píxeles) calculadas de forma dinámica y lineal.

## Cambios Realizados

### 1. Capa de Shaders
* **Shader Fragment (`lightning.frag`)**:
  - Se declaró el uniform `uniform float uGrowthProgress;`.
  - En la rama de renderizado texturizado (`uIsTextured == 1`), se evalúa la coordenada vertical de la textura: si `vTexCoord.y > uGrowthProgress`, el píxel es descartado (`discard`), ocultando la mitad inferior y simulando un corte a esa altura.

### 2. Capa de Físicas y Animación
* **Entidad Rayo (`Lightning.kt`)**:
  - Añadida la propiedad pública `growthProgress: Float` al constructor.
  - Modificado `trigger()` para inicializar `growthProgress = 0.0f`.
  - Modificado `update(deltaTime)` para que durante el primer 20% de la duración total del rayo (`duration * 0.20f`), se incremente linealmente `growthProgress` de 0.0 a 1.0. Pasada esa fase, se mantiene estático en 1.0f durante la fase de desvanecimiento y parpadeo.

### 3. Capa de Renderizado
* **OpenGL (`StormRenderer.kt`)**:
  - Se localiza el uniform `uGrowthProgress` en `drawLightning()`.
  - Se envía un valor estático de `1.0f` al dibujar el destello de fondo (ya que la luz ambiental llena toda la pantalla simultáneamente).
  - Se vincula el valor dinámico `lightning.growthProgress` antes de dibujar el quad texturizado de cada rayo.

### 4. Pruebas Unitarias (`SceneManagerTest.kt`)
- Se implementó el test unitario `testLightningGrowthProgress` que simula el ciclo de vida de un rayo, validando que:
  1. Al inicio (`age = 0f`), el progreso es exactamente `0.0f`.
  2. A mitad de la fase de crecimiento (10% de duración), el progreso se encuentra alrededor de `0.5f` (+/- 5% de tolerancia).
  3. Tras superar la fase de crecimiento (30% de duración), el progreso es de manera inamovible `1.0f`.

## Commits Realizados

- `feat(shader): support progressive drawing animation in lightning fragment shader`
- `feat(physics): compute growthProgress over the first 20 percent of lightning duration`
- `feat(renderer): upload uGrowthProgress uniform to lightning shader program`
- `test(physics): add test case for lightning growthProgress calculations`

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite completa de unit tests exitosamente:
```bash
.\gradlew.bat testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` en 4s.
