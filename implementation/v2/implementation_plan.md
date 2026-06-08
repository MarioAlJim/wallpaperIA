# Plan para Mudar la Configuración a una Activity de Lanzamiento e Integrar Botón de Aplicar Wallpaper

Este plan detalla las modificaciones para hacer que la actividad de configuración (`WallpaperSettingsActivity`) sea accesible desde el lanzador de aplicaciones de Android (App Drawer) y añadir un botón premium dentro de ella que permita al usuario establecer directamente el Wallpaper en su dispositivo a través del selector del sistema.

## User Review Required

> [!IMPORTANT]
> - Se añadirá la categoría `<category android:name="android.intent.category.LAUNCHER" />` a la actividad en `AndroidManifest.xml` para que aparezca como una aplicación instalada en el cajón de aplicaciones del dispositivo.
> - Se creará un botón con diseño premium ("Establecer como Fondo de Pantalla") al final del layout de configuración que abrirá el intent oficial de previsualización y aplicación de Live Wallpapers de Android.

## Proposed Changes

---

### Configuración del Sistema y Manifiesto

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/AndroidManifest.xml)
- Modificar el `<intent-filter>` de `WallpaperSettingsActivity` para incluir la categoría `<category android:name="android.intent.category.LAUNCHER" />` para que aparezca en el menú del dispositivo.

---

### Diseño de la Interfaz

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Envolver el contenido en un `ScrollView` para evitar problemas de desbordamiento en pantallas pequeñas.
- Añadir un botón premium (`Button` o `MaterialButton` estilizado) con bordes redondeados (`12dp`), relleno vertical generoso y un degradado o color sólido atractivo (ej. `#1E90FF`) que diga "Establecer como fondo de pantalla".

---

### Lógica de la Actividad

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Agregar el manejador de clics para el botón de aplicar wallpaper.
- Implementar el lanzamiento del intent de Android `WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER` con el componente de servicio `StormWallpaperService` para abrir la previsualización nativa.

## Verification Plan

### Manual Verification
- Compilar e instalar la aplicación.
- Comprobar que aparece un nuevo icono de la aplicación en el cajón de aplicaciones del dispositivo (Launcher).
- Abrir la aplicación desde el launcher y verificar que muestra la pantalla de configuración de Densidad de Lluvia.
- Presionar el botón "Establecer como Fondo de Pantalla" y confirmar que se abre el previsualizador nativo de Android con nuestro wallpaper animado "Tormenta OpenGL".
- Aplicar el wallpaper y verificar que se establece correctamente en el dispositivo.
