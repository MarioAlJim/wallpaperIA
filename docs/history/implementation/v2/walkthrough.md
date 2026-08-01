# Historial de Cambios - Versión 2.0 (Actividad de Lanzamiento y Botón Aplicar)

En este incremento se ha modificado la aplicación para que la actividad de configuración (`WallpaperSettingsActivity`) actúe como la actividad principal (Launcher) de la app, permitiendo al usuario abrirla desde el cajón de aplicaciones del dispositivo. Adicionalmente, se ha integrado un botón para establecer/aplicar el fondo de pantalla animado de forma directa e interactiva.

## Cambios Realizados

### Configuración del Sistema y Manifiesto

#### [AndroidManifest.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/AndroidManifest.xml)
- Se añadió la categoría `<category android:name="android.intent.category.LAUNCHER" />` al `<intent-filter>` de `WallpaperSettingsActivity`. Esto registra la actividad como accesible desde el menú de aplicaciones del dispositivo Android.

### Diseño y Layout de la Interfaz

#### [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Se envolvió el diseño completo en un `ScrollView` con `fillViewport="true"`, previniendo errores de desbordamiento en pantallas pequeñas al desplazarse verticalmente.
- Se añadió un botón premium (`Button` inflado como `MaterialButton`) identificado con `buttonApplyWallpaper`, que posee bordes redondeados (`12dp`), relleno vertical generoso (`14dp`), texto en minúsculas mixtas (`textAllCaps="false"`) y fondo azul estético (`#1E90FF`).

### Lógica de Control e Interacción

#### [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Se agregaron las importaciones necesarias para interactuar con el administrador de fondos de pantalla de Android: `android.app.WallpaperManager`, `android.content.ComponentName`, `android.content.Intent` y `android.widget.Button`.
- Se enlazó el manejador de clics del botón `buttonApplyWallpaper` dentro del método `onCreate()`. Al ser pulsado, construye un Intent con la acción `WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER` e indica que el servicio objetivo es `StormWallpaperService::class.java` para abrir directamente la previsualización y botón de aplicar oficial del sistema.
