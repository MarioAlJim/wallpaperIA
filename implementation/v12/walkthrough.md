# Walkthrough – Configuración de Duración de los Rayos

Hemos agregado la capacidad de regular la duración de la animación de los rayos y su correspondiente destello ambiental. El usuario puede ahora ajustar este parámetro desde la interfaz de configuración a través de un control deslizante (SeekBar) que va de 0% a 100%, con descripciones dinámicas según el rango de tiempo asignado.

## Cambios Realizados

### 1. Capa de Datos y Configuración
* **Interfaz `ConfigProvider.kt`**:
  - Se declaró la función `getLightningDuration(): Int` para obtener el porcentaje de duración guardado.
* **Clase `ConfigManager.kt`**:
  - Definida la clave de preferencia `lightning_duration` con valor predeterminado `30` (~300 ms, para emular la duración original de forma transparente).
  - Implementado `getLightningDuration()` y `setLightningDuration(duration: Int)`.

### 2. Capa de Físicas y Animación (`Lightning.kt` & `SceneManager.kt`)
* **Mapeo No Lineal de la Duración (`Lightning.kt`)**:
  - Se actualizó el método `trigger` para recibir `durationPercentage: Int` (con un valor predeterminado de 30 para conservar la retrocompatibilidad).
  - Se implementó un mapeo no lineal para que las duraciones se perciban naturales y escalen de forma adecuada:
    - Rango 0% - 50%: Mapeado a una duración base de `0.15s` a `0.30s`.
    - Rango 50% - 100%: Mapeado a una duración base de `0.30s` a `1.00s`.
    - Se añade una ventana de variación aleatoria de `0.15s` sobre el mínimo base calculado.
* **Propagación desde Escena (`SceneManager.kt`)**:
  - Se modificó la llamada de trigger en `update()` para leer y pasar `configProvider.getLightningDuration()`.

### 3. Interfaz de Configuración (UI)
* **Layout (`activity_settings.xml`)**:
  - Se añadió la tarjeta "Duración de los Rayos" con el SeekBar `seekBarLightningDuration` y el visor de texto `textViewLightningDurationValue`.
* **Actividad (`WallpaperSettingsActivity.kt`)**:
  - Se inicializó y vinculó el SeekBar en `onCreate` asociando su listener de guardado.
  - Se añadió el manejador de texto en `updateTextView()` clasificando la duración según su porcentaje: "Corto" (0%-20%), "Normal" (21%-60%), "Largo" (61%-85%) y "Extremo" (86%-100%).

### 4. Pruebas Unitarias (`SceneManagerTest.kt`)
- Se actualizó `MockConfigProvider` para implementar `getLightningDuration()`.
- Se implementó la prueba unitaria `testLightningDurationMapping` que valida que al 0%, 50% y 100% de la configuración, los rayos disparados tengan su duración física dentro de los rangos calculados esperados.

## Commits Realizados

- `feat(config): add lightning duration preference configurations`
- `feat(ui): add seekbar control for lightning animation duration`
- `feat(physics): support custom lightning animation duration via trigger parameter`
- `test(physics): add test cases for lightning animation duration mapping`

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite completa de unit tests exitosamente:
```bash
.\gradlew.bat testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` en 4s.
