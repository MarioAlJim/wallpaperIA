# Plan de Implementación – Configuración de Dinámica de Nubes (v18)

Este plan describe el diseño e implementación de un nuevo parámetro de configuración para controlar la velocidad y amplitud del efecto de respiración (crecimiento) y la transición de opacidad de las nubes en los ajustes del fondo de pantalla.

## User Review Required

> [!IMPORTANT]
> - **Nuevo Parámetro**: Se introduce `cloudDynamicsSpeed` (0% a 100%) en las preferencias.
>   - A 0%, las nubes permanecen estáticas en tamaño y no respiran (calma minimalista total). Las transiciones de desvanecimiento (fade in/out) se realizan a una velocidad básica mínima para que los cambios de densidad sigan funcionando.
>   - A 100%, los efectos operan a su velocidad completa.
> - **Control en UI**: Se añadirá un nuevo control deslizante (Slider de Material 3) bajo la sección "Nubes y Viento" en la pantalla de ajustes de la aplicación.
> - **Modificación en Cloud.kt**:
>   - La firma de `update` se actualizará con un parámetro por defecto: `fun update(deltaTime: Float, windSpeed: Float, dynamicsSpeed: Float = 1.0f)`.
>   - Se escalará la acumulación de la fase `pulseTime` y la amplitud del seno de `scale` por `dynamicsSpeed`.
>   - Se escalará `activeFadeSpeed` por `(0.2f + 0.8f * dynamicsSpeed)` para mantener una velocidad de transición básica cuando el efecto de dinámica esté al 0%.

## Proposed Changes

---

### Versión de la Característica
- Se creará el directorio `implementation/v18` y se guardarán copias de este plan y de la bitácora (`walkthrough.md`) una vez completados.

---

### Proveedor de Configuración

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)
- Declarar: `fun getCloudDynamicsSpeed(): Int`

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)
- Definir constantes:
  - `const val KEY_CLOUD_DYNAMICS_SPEED = "cloud_dynamics_speed"`
  - `const val DEFAULT_CLOUD_DYNAMICS_SPEED = 100`
- Implementar métodos getter y setter:
  - `override fun getCloudDynamicsSpeed(): Int`
  - `fun setCloudDynamicsSpeed(speed: Int)`

---

### Simulación de Nubes

#### [MODIFY] [Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)
- Actualizar `update` a:
  ```kotlin
  fun update(deltaTime: Float, windSpeed: Float, dynamicsSpeed: Float = 1.0f) {
      val windFactorOpacity = 1.0f + abs(windSpeed) * 2.5f
      val windFactorBreathing = 1.0f + abs(windSpeed) * 1.25f
      
      // Aplicar dynamicsSpeed a la velocidad de respiración
      pulseTime += deltaTime * windFactorBreathing * 0.1f * dynamicsSpeed
      // Aplicar dynamicsSpeed a la amplitud de la respiración (0% = sin cambio de tamaño)
      scale = baseScale * (1.0f + sin(pulseTime) * 0.12f * dynamicsSpeed)

      val windThreshold = 0.1f
      val driftInfluence = (1.0f - (abs(windSpeed) / windThreshold)).coerceIn(0f, 1f)
      positionX += (windSpeed + (driftSpeed * driftInfluence)) * speedFactor * speedZFactor * deltaTime
      
      // Escalar la velocidad de desvanecimiento manteniendo una velocidad mínima de seguridad del 20%
      val activeFadeSpeed = 0.15f * windFactorOpacity * (0.2f + 0.8f * dynamicsSpeed)
      if (isFadingOut) {
          opacity = (opacity - activeFadeSpeed * deltaTime).coerceAtLeast(0f)
      } else {
          if (opacity < targetOpacity) {
              opacity = (opacity + activeFadeSpeed * deltaTime).coerceAtMost(targetOpacity)
          } else if (opacity > targetOpacity) {
              opacity = (opacity - activeFadeSpeed * deltaTime).coerceAtLeast(targetOpacity)
          }
      }
  }
  ```

#### [MODIFY] [SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)
- En `update(deltaTime)`, obtener el factor de velocidad:
  `val dynamicsSpeed = configProvider.getCloudDynamicsSpeed() / 100f`
- Pasar `dynamicsSpeed` en el bucle de actualización de nubes:
  `cloud.update(deltaTime, windSpeed, dynamicsSpeed)`

---

### Pantalla de Ajustes

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Insertar un nuevo grupo de control RelativeLayout + Slider + TextView para "Respiración y Opacidad" (ID `seekBarCloudDynamicsSpeed`) debajo del bloque de intensidad del viento dentro de la sección "Nubes y Viento".

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- En `onCreate()`, enlazar el nuevo control deslizante `seekBarCloudDynamicsSpeed`:
  ```kotlin
  setupSlider(
      R.id.seekBarCloudDynamicsSpeed,
      R.id.textViewCloudDynamicsSpeedValue,
      configManager.getCloudDynamicsSpeed()
  ) { value ->
      configManager.setCloudDynamicsSpeed(value)
  }
  ```
- En `updateTextView()`, manejar el formateo del valor:
  ```kotlin
  R.id.seekBarCloudDynamicsSpeed -> {
      textView.text = if (value == 0) "Desactivado" else "$value%"
  }
  ```

---

### Pruebas Unitarias

#### [MODIFY] [SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)
- Registrar `mockCloudDynamicsSpeed = 100` y `getCloudDynamicsSpeed()` en `MockConfigProvider`.
- Añadir una prueba unitaria `testCloudDynamicsSpeedConfig()` para verificar que cuando `dynamicsSpeed` se establece a 0, la escala no fluctúa y que la velocidad de desvanecimiento opera a su ritmo básico del 20%.

---

## Verification Plan

### Automated Tests
- Ejecutar la suite de pruebas unitarias locales para confirmar que todo compile y pase:
```bash
./gradlew testDebugUnitTest
```

### Manual Verification
- Abrir la actividad de ajustes.
- Ajustar el slider "Respiración y Opacidad" a 0% y verificar que las nubes dejen de respirar/oscilar en tamaño por completo.
- Ajustar el slider a 100% y verificar que la animación opere normalmente.
