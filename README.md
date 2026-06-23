# Wallpaper Animado de Clima (Tormenta y Sol) para Android

Este es un Live Wallpaper para Android desarrollado en Kotlin y C++ (OpenGL ES 3.0) que simula dos modos climáticos principales: **Tormenta** (con nubes animadas, lluvia de partículas realistas, destellos y rayos) y **Sol/Soleado** (con movimiento del sol, degradados de cielo dinámicos y siluetas de fondo).

El proyecto está diseñado de forma modular, permitiendo un alto rendimiento (objetivo de 60 FPS) y un consumo energético controlado.

---

## Estructura del Proyecto

El proyecto está organizado en los siguientes módulos gradle:

* **`:app`**: Módulo principal de la aplicación. Contiene el servicio Android del Wallpaper, los perfiles de optimización y la actividad de configuración (`WallpaperSettingsActivity`) con una interfaz premium que utiliza selectores de tarjetas dinámicas con vistas de previsualización en tiempo real.
* **`:core`**: Contiene la lógica de persistencia de configuración (`ConfigManager` y `ConfigProvider`) mediante `SharedPreferences`, modelos base y las interfaces compartidas de la escena.
* **`:wallpaper-storm`**: Módulo dedicado al modo **Tormenta**. Incluye shaders OpenGL ES (vértice y fragmento), clases de renderizado de nubes animadas, lluvia de partículas en forma de gotas procedurales en degradado, rayos aleatorios/simultáneos y la simulación visual del viento mediante líneas cinéticas (`WindLine`).
* **`:wallpaper-sunny`**: Módulo dedicado al modo **Soleado**. Incluye shaders y renderizadores para simular el sol, sus trayectorias arqueadas (de izquierda a derecha o aleatorias), degradados editables en el cielo y siluetas vectorizadas en parallax (colinas, haze, etc.).

---

## Comandos del Proyecto (Gradle Wrapper)

Dado que es un proyecto de Android estándar, puedes gestionar todas las etapas de desarrollo utilizando el Gradle Wrapper (`gradlew` en Linux/macOS o `gradlew.bat` en Windows) desde el directorio raíz.

### 1. Compilación y Construcción

Para realizar tareas relacionadas con compilar el código y generar binarios:

* **Limpiar el proyecto** (elimina directorios de compilación anteriores):
  ```bash
  # Windows
  gradlew.bat clean
  # Linux/macOS
  ./gradlew clean
  ```

* **Compilar código fuente debug** (verifica que no haya errores de compilación):
  ```bash
  # Windows
  gradlew.bat compileDebugSources
  # Linux/macOS
  ./gradlew compileDebugSources
  ```

* **Construir el APK de desarrollo (Debug)**:
  Genera el APK en `app/build/outputs/apk/debug/app-debug.apk`.
  ```bash
  # Windows
  gradlew.bat assembleDebug
  # Linux/macOS
  ./gradlew assembleDebug
  ```

* **Construir el APK de producción (Release)**:
  Genera el APK optimizado y listo para firmar en `app/build/outputs/apk/release/app-release-unsigned.apk`.
  ```bash
  # Windows
  gradlew.bat assembleRelease
  # Linux/macOS
  ./gradlew assembleRelease
  ```

### 2. Ejecución y Despliegue en Dispositivo/Emulador

Asegúrate de tener un emulador abierto o un dispositivo físico conectado mediante ADB:

* **Instalar el APK de depuración**:
  Compila e instala el wallpaper en el dispositivo conectado de forma automática.
  ```bash
  # Windows
  gradlew.bat installDebug
  # Linux/macOS
  ./gradlew installDebug
  ```

* **Desinstalar la aplicación**:
  ```bash
  # Windows
  gradlew.bat uninstallAll
  # Linux/macOS
  ./gradlew uninstallAll
  ```

### 3. Pruebas Unitarias e Integración

Para ejecutar las pruebas del proyecto y garantizar la estabilidad:

* **Ejecutar todas las pruebas unitarias de todos los módulos**:
  ```bash
  # Windows
  gradlew.bat test
  # Linux/macOS
  ./gradlew test
  ```

* **Ejecutar pruebas de un módulo específico**:
  Si solo estás modificando un módulo (por ejemplo, `wallpaper-storm`), puedes ejecutar solo sus pruebas para ahorrar tiempo:
  ```bash
  # Módulo app
  ./gradlew :app:testDebugUnitTest
  
  # Módulo core
  ./gradlew :core:testDebugUnitTest
  
  # Módulo storm
  ./gradlew :wallpaper-storm:testDebugUnitTest
  
  # Módulo sunny
  ./gradlew :wallpaper-sunny:testDebugUnitTest
  ```

* **Ejecutar pruebas instrumentadas (AndroidTest)**:
  Ejecuta las pruebas de integración en un emulador o dispositivo conectado (si están configuradas):
  ```bash
  # Windows
  gradlew.bat connectedAndroidTest
  # Linux/macOS
  ./gradlew connectedAndroidTest
  ```

### 4. Calidad de Código y Análisis Estático

* **Ejecutar el Linter de Android (detecta problemas potenciales y bugs)**:
  ```bash
  # Windows
  gradlew.bat lint
  # Linux/macOS
  ./gradlew lint
  ```

---

## Notas sobre Shaders y Recursos Gráficos

* **Shaders (`.vert` / `.frag`)**: Los archivos de sombreadores se encuentran bajo la carpeta `assets/shaders` de cada módulo de renderizado. En Android OpenGL ES, **no se precompilan**. Se empaquetan como archivos de texto plano dentro del APK y es el propio dispositivo Android el que los lee y compila en tiempo de ejecución (runtime) a través de la API de OpenGL ES 3.0 (aprovechando el driver de la GPU del dispositivo).
* **Recursos Gráficos (Imágenes y XMLs)**: Las texturas (`.png`) y los vectores de la interfaz (`.xml`) son procesados y optimizados automáticamente por la herramienta **AAPT2** (Android Asset Packaging Tool) integrada dentro del flujo normal de construcción de Gradle. Al ejecutar `gradlew assembleDebug`, todo el empaquetado gráfico se realiza sin necesidad de comandos externos.

