# Especificación SDD – Wallpaper Animado de Tormenta para Android

## 1. Objetivo

Desarrollar un Live Wallpaper para Android que simule una tormenta mediante la representación gráfica de nubes, lluvia y rayos utilizando OpenGL ES 3.0.

El usuario podrá personalizar la experiencia mediante parámetros configurables que modificarán la cantidad y comportamiento de los elementos visuales.

El sistema deberá ofrecer una animación fluida, eficiente en el consumo de recursos y compatible con dispositivos Android 8.0 o superiores.

---

# 2. Alcance

El proyecto contempla:

* Renderizado de nubes animadas.
* Renderizado de lluvia dinámica.
* Generación aleatoria de rayos.
* Configuración de parámetros por parte del usuario.
* Persistencia de configuración.
* Adaptación automática a distintas resoluciones y orientaciones.

Quedan fuera del alcance inicial:

* Efectos meteorológicos adicionales.
* Interacción táctil.
* Sincronización con servicios climáticos reales.
* Personalización avanzada de texturas.
* Efectos de sonido.

---

# 3. Requisitos Funcionales

## RF-001 – Inicialización del Wallpaper

El sistema deberá iniciar automáticamente cuando el usuario seleccione el wallpaper desde la configuración de Android.

### Criterio de aceptación

* El wallpaper debe mostrarse en menos de 2 segundos después de ser seleccionado.

---

## RF-002 – Renderizado de Nubes

El sistema deberá renderizar nubes utilizando texturas transparentes.

### Comportamiento

* Las nubes se desplazarán horizontalmente.
* La velocidad será configurable internamente.
* Las nubes podrán tener tamaños variables.
* Las nubes deberán aparecer en posiciones aleatorias dentro de la escena.

### Parámetro

CloudDensity (0–100)

| Valor | Cantidad de Nubes |
| ----- | ----------------- |
| 0     | 0                 |
| 25    | 5                 |
| 50    | 10                |
| 75    | 15                |
| 100   | 20                |

### Criterio de aceptación

* El cambio de densidad debe reflejarse en menos de 1 segundo.

---

## RF-003 – Renderizado de Lluvia

El sistema deberá representar gotas de lluvia mediante partículas.

### Comportamiento

* Movimiento vertical descendente.
* Reinicio automático al salir de la pantalla.
* Aplicación de efecto visual tipo motion blur mediante shader.
* Velocidad uniforme para todas las partículas.

### Parámetro

RainIntensity (0–100)

| Valor | Partículas |
| ----- | ---------- |
| 0     | 0          |
| 25    | 250        |
| 50    | 500        |
| 75    | 750        |
| 100   | 1000       |

### Criterio de aceptación

* La intensidad deberá modificarse dinámicamente sin reiniciar el wallpaper.

---

## RF-004 – Generación de Rayos

El sistema deberá generar rayos aleatorios.

### Comportamiento

* Aparición pseudoaleatoria.
* Destello breve en pantalla.
* Posición aleatoria.
* Duración configurable internamente.
* Desaparición automática al finalizar la animación.

### Parámetro

LightningFrequency (0–100)

| Valor | Frecuencia       |
| ----- | ---------------- |
| 0     | Nunca            |
| 25    | Cada 60 segundos |
| 50    | Cada 30 segundos |
| 75    | Cada 15 segundos |
| 100   | Cada 5 segundos  |

### Criterio de aceptación

* Los rayos deberán aparecer dentro de la frecuencia configurada con una tolerancia máxima del 10%.

---

## RF-005 – Configuración del Usuario

El sistema deberá proporcionar una pantalla de configuración accesible desde el selector de wallpapers.

### Opciones

* Densidad de nubes.
* Intensidad de lluvia.
* Frecuencia de rayos.

### Persistencia

Las configuraciones deberán almacenarse mediante SharedPreferences.

### Criterio de aceptación

* Los valores configurados deberán mantenerse después de reiniciar el dispositivo.

---

## RF-006 – Adaptación de Pantalla

El wallpaper deberá ajustarse automáticamente a:

* Resoluciones distintas.
* Orientación vertical.
* Orientación horizontal.

### Criterio de aceptación

* No deberán existir deformaciones visuales visibles.
* Los elementos deberán mantener sus proporciones independientemente de la resolución.

---

## RF-007 – Gestión de Ciclo de Vida

Cuando el wallpaper deje de estar visible:

* Se detendrá el renderizado.
* Se pausarán animaciones.
* Se liberarán recursos temporales.

Cuando vuelva a ser visible:

* Se restaurará el estado previo.

### Criterio de aceptación

* No deberá existir pérdida de configuración ni errores visuales al reanudar.

---

# 4. Requisitos No Funcionales

## RNF-001 – Rendimiento

El sistema deberá mantener:

* Promedio mínimo de 55 FPS.
* Objetivo ideal de 60 FPS.

---

## RNF-002 – Uso de Memoria

El consumo máximo de memoria no deberá superar:

100 MB

---

## RNF-003 – Tiempo de Carga

La inicialización completa no deberá superar:

2 segundos

---

## RNF-004 – Consumo Energético

El wallpaper no deberá incrementar significativamente el consumo energético del dispositivo durante el uso normal.

Objetivo:

* Mantener un impacto inferior al 5% por hora en pruebas controladas.

---

## RNF-005 – Compatibilidad

Versiones soportadas:

* Android 8.0 (API 26)
* Android 9
* Android 10
* Android 11
* Android 12
* Android 13
* Android 14
* Android 15

---

# 5. Modelo de Dominio

## Cloud

Representa una nube renderizada.

### Atributos

* id
* positionX
* positionY
* speed
* scale
* opacity

---

## RainDrop

Representa una partícula de lluvia.

### Atributos

* positionX
* positionY
* velocity
* length

---

## Lightning

Representa un rayo.

### Atributos

* startPoint
* endPoint
* duration
* intensity
* branches

---

# 6. Arquitectura

## Componentes

### WallpaperService

Responsabilidades:

* Integración con Android.
* Gestión del ciclo de vida.
* Creación del motor OpenGL.

---

### Renderer

Responsabilidades:

* Inicialización de OpenGL ES 3.0.
* Gestión de shaders.
* Actualización de buffers.
* Dibujado de la escena.

---

### SceneManager

Responsabilidades:

* Crear entidades visuales.
* Actualizar estados.
* Gestionar destrucción y reciclado de objetos.

---

### ConfigManager

Responsabilidades:

* Leer configuraciones.
* Persistir configuraciones.
* Notificar cambios a la escena.

---

### Antigravity Engine

Responsabilidades:

* Actualización temporal.
* Interpolaciones.
* Gestión de animaciones.
* Coordinación de efectos visuales.

---

# 7. Flujo de Renderizado

WallpaperService

↓

OpenGL Engine

↓

Renderer

↓

SceneManager

↓

CloudLayer

RainLayer

LightningLayer

---

# 8. Contratos e Interfaces

## StormObject

```kotlin
interface StormObject {
    fun update(deltaTime: Float)
    fun render()
}
```

## ConfigProvider

```kotlin
interface ConfigProvider {
    fun getCloudDensity(): Int
    fun getRainIntensity(): Int
    fun getLightningFrequency(): Int
}
```

---

# 9. Recursos

## Texturas

cloud_01.png

cloud_02.png

cloud_03.png

rain_particle.png

lightning.png

---

## Shaders

cloud.vert

cloud.frag

rain.vert

rain.frag

lightning.vert

lightning.frag

---

# 10. Casos Límite

## CL-001

CloudDensity = 0

Resultado esperado:

No se renderizan nubes.

---

## CL-002

RainIntensity = 0

Resultado esperado:

No se renderiza lluvia.

---

## CL-003

LightningFrequency = 0

Resultado esperado:

No aparecen rayos.

---

## CL-004

Rotación de pantalla.

Resultado esperado:

Reescalado automático sin reiniciar el wallpaper.

---

## CL-005

Modo ahorro de energía activado.

Resultado esperado:

Reducción automática de partículas para preservar rendimiento.

---

## CL-006

Aplicación de configuración cerrada inesperadamente.

Resultado esperado:

La última configuración válida permanece almacenada.

---

# 11. Casos de Prueba

## TC-001

Configurar CloudDensity = 100.

Resultado esperado:

20 nubes visibles simultáneamente.

---

## TC-002

Configurar RainIntensity = 100.

Resultado esperado:

1000 partículas activas.

---

## TC-003

Configurar LightningFrequency = 100.

Resultado esperado:

Aparición aproximada de un rayo cada 5 segundos.

---

## TC-004

Reiniciar dispositivo.

Resultado esperado:

Configuraciones restauradas correctamente.

---

## TC-005

Rotar dispositivo durante la animación.

Resultado esperado:

Escena reescalada sin artefactos visuales.

---

# 12. Escenarios de Uso

## Escenario 1

El usuario instala el wallpaper.

Resultado:

El wallpaper aparece disponible en el selector de fondos.

---

## Escenario 2

El usuario configura los parámetros visuales.

Resultado:

Los cambios se reflejan en tiempo real.

---

## Escenario 3

El usuario bloquea y desbloquea el dispositivo.

Resultado:

La animación continúa funcionando correctamente respetando el ciclo de vida del sistema.

---

# 13. Extensiones Futuras

## EF-001

Sistema de viento dinámico.

## EF-002

Colores adaptativos según la hora del día.

## EF-003

Interacción táctil para generar rayos.

## EF-004

Integración con información meteorológica real.

## EF-005

Múltiples temas de tormenta.

## EF-006

Capas adicionales de nubes con efecto de profundidad.
