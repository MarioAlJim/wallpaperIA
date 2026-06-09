# Walkthrough – Configuración de Dinámica de Nubes (v18)

Hemos implementado un nuevo parámetro de configuración deslizante (Slider) bajo la sección "Nubes y Viento" para permitir al usuario ajustar o desactivar por completo los efectos dinámicos de las nubes (respiración de tamaño y transiciones de opacidad).

## Cambios Realizados

### 1. Modelo de Preferencias y Configuración
- **[ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigProvider.kt)**: Añadimos la función `fun getCloudDynamicsSpeed(): Int`.
- **[ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/ConfigManager.kt)**:
  - Definimos la clave de preferencia `"cloud_dynamics_speed"` con un valor por defecto del `100%`.
  - Implementamos los métodos `getCloudDynamicsSpeed(): Int` y `setCloudDynamicsSpeed(speed: Int)`.

### 2. Lógica de Simulación de Nubes
- **[Cloud.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/Cloud.kt)**:
  - Actualizamos la firma del método `update()` para aceptar el factor `dynamicsSpeed` (entre `0.0f` y `1.0f`, por defecto `1.0f`).
  - Escalamos el incremento de fase `pulseTime` y la amplitud del seno de tamaño (`scale`) usando `dynamicsSpeed`. Al estar en `0f` (0%), el tamaño permanece estático en su `baseScale`.
  - **Aumento del 50% en la amplitud de respiración**: Incrementamos la fluctuación de tamaño a ±12% (anteriormente ±8%) para una variación más perceptible:
    `scale = baseScale * (1.0f + sin(pulseTime) * 0.12f * dynamicsSpeed)`
  - Multiplicamos la velocidad base de transición de opacidad (`activeFadeSpeed`) por el factor `(0.2f + 0.8f * dynamicsSpeed)` para que la aparición/desaparición al cambiar de densidad opere a un 20% de velocidad mínima de seguridad, evitando que las nubes se queden invisibles de forma permanente si el parámetro está al 0%.
- **[SceneManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/SceneManager.kt)**:
  - Recuperamos `dynamicsSpeed` en el bucle principal de físicas de `update()` y lo pasamos al llamar a `cloud.update(deltaTime, windSpeed, dynamicsSpeed)`.

### 3. Pantalla de Ajustes y Manifiesto
- **[AndroidManifest.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/AndroidManifest.xml)**:
  - Configuramos los atributos `android:icon` y `android:roundIcon` para utilizar el recurso `@drawable/ic_storm`, estableciendo el ícono vectorizado premium como ícono oficial de la aplicación.
- **[activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)**:
  - Insertamos un nuevo grupo de control RelativeLayout + Slider + TextView para "Respiración y Opacidad" (ID `seekBarCloudDynamicsSpeed`) dentro del primer acordeón.
- **[WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)**:
  - Enlazamos y configuramos el Slider `seekBarCloudDynamicsSpeed` con el gestor de preferencias.
  - Formateamos el texto dinámico para mostrar `"Desactivado"` al estar en 0% y el valor del porcentaje (`value%`) en los demás casos.

### 4. Pruebas Unitarias
- En **[SceneManagerTest.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/test/java/com/wolf/wallpaper/SceneManagerTest.kt)**:
  - Actualizamos el `MockConfigProvider` para implementar `getCloudDynamicsSpeed()`.
  - Añadimos la prueba unitaria `testCloudDynamicsSpeedConfig()` que valida que al estar en 0% de dinámica, la escala de las nubes permanece estática en su valor base y la opacidad sigue aumentando al 20% de su velocidad de seguridad.

---

## Verificación

### Pruebas Automatizadas
Ejecutamos la suite de pruebas unitarias locales en el entorno:
```bash
./gradlew testDebugUnitTest
```
**Resultado**: `BUILD SUCCESSFUL` - Las 19 pruebas de físicas de la simulación del wallpaper pasaron con éxito.
