# Implementation Plan: Night Mode, Procedural Clouds & Day/Night/Combined Sky

The goal is to introduce a **Time Mode** selector within the existing Sunny tab that lets the user choose between **Día** (current sunny behaviour, unchanged), **Noche** (night sky with moon, stars, and procedural polygon clouds) and **Combinado** (a continuous animated gradient that cycles between full daylight and full night, blending sun and moon visibility). The current Storm mode is not touched.

Procedural clouds — built entirely from signed-distance-field (SDF) geometry in the fragment shader — replace texture-based clouds in the new Night and Combined modes. They are never used in Storm mode.

---

## User Review Required

> [!IMPORTANT]
> **Procedural Clouds**: Clouds are not texture sprites. Each cloud is a quad that the fragment shader fills using five smooth-unioned ellipses (SDF metaball technique). Depth (Z) modulates opacity for parallax. Cloud colour adjusts automatically to the ambient sky colour, so they look light and wispy in day mode and dark-blue/grey in night mode.
>
> **Moon Phase Rendering**: The moon is a single shader quad. The shadow is a displaced sphere subtracted from the lit circle via SDF boolean difference. The displacement is derived from `uPhase` (0–7). Phase 0 = New Moon (invisible / silhouette glow only). Phase 4 = Full Moon. The shader renders both the lit surface and a soft limb-glow halo.
>
> **Star Field**: Stars are rendered in a full-screen pass using a procedural cell hash. Every cell has a 5 % probability of containing one star. Stars twinkle via a per-cell sine wave driven by `uTime`. The colour is a uniform tint, selectable by the user.
>
> **Combined Mode Cycle**: A single float `uCycleProgress` (0.0 → 1.0, wrapping) drives everything: sky gradient interpolation, star/moon fade-in and fade-out, and sun fade-in and fade-out. The user controls the cycle speed (how many seconds a full day/night loop takes).

---

## Proposed Changes

### 1. Configuration & SharedPreferences

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigProvider.kt)

Add new interface methods:

```kotlin
fun getTimeMode(): Int                  // 0 = Día, 1 = Noche, 2 = Combinado
fun getMoonPhase(): Int                 // 0–7 (eight lunar phases)
fun getMoonPathDirection(): Int         // 0 = IzqaDer, 1 = DeraIzq, 2 = Estático, 3 = Aleatorio
fun getMoonMoveSpeed(): Int             // 0–100
fun getMoonStationaryPosition(): Int    // 0–5 (mirrors sun stationary positions)
fun getStarColorIndex(): Int            // 0–5
fun getStarDensity(): Int               // 0–100
fun getNightCloudDensity(): Int         // 0–100
fun getGradientCycleSpeed(): Int        // 0–100 (only for Combinado mode)
```

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigManager.kt)

Add new constants, defaults and getter/setter pairs:

| Key constant | String value | Default |
|---|---|---|
| `KEY_TIME_MODE` | `"time_mode"` | `0` |
| `KEY_MOON_PHASE` | `"moon_phase"` | `4` (Full Moon) |
| `KEY_MOON_PATH_DIRECTION` | `"moon_path_direction"` | `0` |
| `KEY_MOON_MOVE_SPEED` | `"moon_move_speed"` | `30` |
| `KEY_MOON_STATIONARY_POSITION` | `"moon_stationary_position"` | `2` |
| `KEY_STAR_COLOR_INDEX` | `"star_color_index"` | `0` (White) |
| `KEY_STAR_DENSITY` | `"star_density"` | `60` |
| `KEY_NIGHT_CLOUD_DENSITY` | `"night_cloud_density"` | `40` |
| `KEY_GRADIENT_CYCLE_SPEED` | `"gradient_cycle_speed"` | `30` |

- Add `setTimeMode(mode: Int)` with `.coerceIn(0, 2)`.
- Add `setMoonPhase(phase: Int)` with `.coerceIn(0, 7)`.
- Add `setMoonPathDirection(dir: Int)` with `.coerceIn(0, 3)`.
- Add remaining setters with appropriate ranges.

---

### 2. Scene Object

#### [NEW] [Moon.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/Moon.kt)

A `data class`-style state object, analogous to `Cloud.kt`, that holds:

```
positionX: Float        // current OpenGL world X
positionY: Float        // current OpenGL world Y
phase: Int              // 0–7
pathDirection: Int      // 0 IzqaDer / 1 DeraIzq / 2 Static / 3 Random
moveSpeed: Float        // normalised speed factor

// Random path state (only used when pathDirection == 3)
randomStartX / randomStartY: Float
randomEndX   / randomEndY  : Float
randomProgress: Float
isRandomPathInitialized: Boolean
```

Methods:
- `reset(aspectRatio: Float)` — places the moon off-screen at the start edge according to `pathDirection`.
- `update(deltaTime: Float, aspectRatio: Float)` — advances the moon along its trajectory (parabolic arc for L2R/R2L, linear interpolation for Random, static for Stationary).
- `generateRandomPath(aspectRatio: Float)` — picks two distinct off-screen edges (Left / Right / Top / Bottom) and sets start/end coordinates, same algorithm as `SunnyRenderer.generateRandomPath()`.

The parabolic arc formula (L2R and R2L) mirrors the sun's elevated trajectory:
```
moonY = 0.9f - 1.2f * (moonX * moonX)
```
(peaks slightly lower than the sun so both can coexist in Combined mode)

---

### 3. OpenGL Shaders

All new shader files go under:
`wallpaper-sunny/src/main/assets/shaders/`

---

#### [NEW] `cloud_proc.vert`

Standard MVP-transformed quad vertex shader. Passes:
- `vLocalUV: vec2` — fragment position in cloud-local space (–1 to +1 on each axis).
- `vDepth: float` — cloud Z value forwarded from a vertex attribute.

#### [NEW] `cloud_proc.frag`

**SDF metaball cloud** in the fragment shader.

Uniforms:
```glsl
uniform float uTime;
uniform float uDepth;       // 0–1, same as cloud.z
uniform vec3  uCloudColor;  // ambient-adapted base color
uniform float uOpacity;
```

Algorithm:
1. Receive `vLocalUV` (centred, aspect-adjusted).
2. Compute `cloudField(uv)` — a smooth union (`smin`) of five axis-aligned ellipses with the following centres and radii (in local space units):

   | Ellipse | Centre (x, y) | Radius (x, y) |
   |---|---|---|
   | Main body | (0, 0) | (1.0, 0.55) |
   | Right lobe | (0.40, 0.06) | (0.80, 0.55) |
   | Left lobe | (−0.40, 0.06) | (0.80, 0.55) |
   | Centre-right bump | (0.20, 0.26) | (0.55, 0.48) |
   | Centre-left bump | (−0.20, 0.26) | (0.55, 0.48) |

   Smooth blend factor `k = 0.18`.

3. Alpha = `smoothstep(0.06, -0.10, sdfValue) * uOpacity * depthFade`
   where `depthFade = 0.4 + 0.6 * uDepth`.
4. Colour = `uCloudColor` with a slight top-brightness boost derived from the ellipse's local Y.

---

#### [NEW] `moon.vert`

Same MVP-transformed quad as `cloud_proc.vert`. Passes `vLocalUV: vec2`.

#### [NEW] `moon.frag`

**Moon with phase shadow** rendered via SDF boolean difference.

Uniforms:
```glsl
uniform float uPhase;       // 0.0–7.99 (the integer phase index as float)
uniform vec3  uMoonColor;   // warm white: (1.0, 0.97, 0.88)
uniform float uIntensity;   // 0–1, for fade-in/out in Combined mode
```

Algorithm:
1. `moonR = 0.44`, `p = vLocalUV`.
2. `moonSDF = length(p) - moonR`.
3. Compute phase-driven shadow:
   - `t = uPhase / 8.0` maps to 0..1 over a full cycle.
   - Shadow circle centre: `shadowX = cos(t * π) * moonR * 1.05`  
     (at phase 0 → centre+right = new moon; at phase 4 → centre−left = full moon; returns symmetrically).
   - `shadowSDF = length(p - vec2(shadowX, 0.0)) - moonR`.
   - Lit region SDF: `litSDF = max(moonSDF, -shadowSDF)`.  
     (Inside moon AND outside shadow.)
   - For New Moon (phase 0): render only a faint halo glow regardless.
4. Outer glow halo: `halo = smoothstep(moonR + 0.18, moonR + 0.02, length(p)) * 0.35`.
5. Surface colour with subtle limb darkening: `mix(uMoonColor * 0.55, uMoonColor, limb)`.
6. Final alpha = `(litAlpha + halo) * uIntensity`.

---

#### [NEW] `stars.vert`

Full-screen quad (identity MVP). Passes `vUV: vec2` (0..1 screen UVs).

#### [NEW] `stars.frag`

**Procedural twinkling star field**.

Uniforms:
```glsl
uniform float uTime;
uniform float uDensity;     // 0.0–1.0 (mapped from 0–100 setting)
uniform vec3  uStarColor;   // from star color index
uniform float uIntensity;   // for Combined mode fade
uniform float uAspect;      // screen aspect ratio
```

Algorithm:
1. Scale UVs by `gridScale = mix(18.0, 55.0, uDensity)` and correct for aspect ratio.
2. `cell = floor(scaledUV)`, `cellUV = fract(scaledUV)`.
3. `r = hash22(cell).x` — two-component hash for reproducibility.
4. Gate: only cells with `r < 0.05` contain a star (5 % fill probability).
5. Star position within cell: `starPos = hash22(cell + 7.31)`.
6. `dist = length(cellUV - starPos)`.
7. Brightness twinkle: `twinkle = 0.7 + 0.3 * sin(uTime * (2.0 + hash22(cell).y * 3.0))`.
8. `alpha = smoothstep(0.10, 0.01, dist) * twinkle * uIntensity`.
9. Output: `vec4(uStarColor, alpha)`.

Hash function (GLSL ES 3.0 compatible):
```glsl
vec2 hash22(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p.yx + 19.19);
    return fract((p.xx + p.yx) * p.xy);
}
```

---

### 4. Rendering Pipeline

#### [MODIFY] [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)

**New state fields:**
```kotlin
private var cloudProcProgram = 0
private var moonProgram = 0
private var starsProgram = 0

private val moon = Moon()
private val nightClouds = mutableListOf<Cloud>()  // reuse Cloud data class for position/scale/z
private var cycleProgress = 0f                     // 0..1, only for Combined mode
private lateinit var fullscreenStarBuffer: FloatBuffer
```

**`onSurfaceCreated()` additions:**
- Compile and link `cloudProcProgram`, `moonProgram`, `starsProgram` from new shader assets.
- Build `fullscreenStarBuffer` (same coords as existing `fullscreenQuadBuffer`).

**`onUpdate(deltaTime)` additions:**
- Read `timeMode = configProvider.getTimeMode()`.
- If `timeMode == 1` or `timeMode == 2`: update `moon.update(deltaTime, aspectRatio)`.
- If `timeMode == 2`: advance `cycleProgress`:
  ```kotlin
  val cycleDuration = 30f + (1f - configProvider.getGradientCycleSpeed() / 100f) * 270f
  cycleProgress = (cycleProgress + deltaTime / cycleDuration) % 1f
  ```
- Sync `nightClouds` list size to `configProvider.getNightCloudDensity()` (0 density → 0 clouds, 100 → 15 clouds, same mapping as Storm clouds).

**`onDrawFrame()` modifications:**

Replace the current single-path draw with a `timeMode` branch:

```
when (timeMode) {
    0 -> drawDayScene()        // existing sunny drawing, no changes
    1 -> drawNightScene(1f)    // night at full intensity
    2 -> drawCombinedScene()   // blend driven by cycleProgress
}
```

**New private methods:**

- `drawNightScene(intensity: Float)`:
  1. `drawNightSky(intensity)` — renders the background gradient (`vec3(0.01, 0.01, 0.06)` top to `vec3(0.04, 0.02, 0.12)` bottom). Reuses `backgroundProgram` or applies a flat `glClearColor`.
  2. `drawStars(intensity)` — full-screen pass with `starsProgram`.
  3. `drawMoon(intensity)` — renders moon quad via `moonProgram`.
  4. `drawNightClouds(intensity)` — iterates `nightClouds`, renders each via `cloudProcProgram`.

- `drawCombinedScene()`:
  - `sunIntensity = smootherstep(0f, 1f, computeSunFade(cycleProgress))`
  - `nightIntensity = smootherstep(0f, 1f, computeNightFade(cycleProgress))`
  - Calls existing `drawDaySky(sunIntensity)` and `drawSun(sunIntensity)` with an intensity multiplier (needs a new `uIntensity` uniform added to the sun shader).
  - Calls `drawNightScene(nightIntensity)`.
  - Sky background colour is interpolated between day and night colours using `cycleProgress`.
  - `computeSunFade`: peaks at `cycleProgress ≈ 0.0 / 1.0`, fades between 0.25 and 0.75.
  - `computeNightFade`: peaks at `cycleProgress ≈ 0.5`, fades between 0.2 and 0.8.

**Night cloud colour adaptation:**
```kotlin
val cloudColor = if (timeMode == 1)
    floatArrayOf(0.55f, 0.62f, 0.78f)   // blue-grey night clouds
else
    floatArrayOf(0.70f, 0.74f, 0.88f)   // lighter dusk/dawn clouds
```

---

### 5. User Interface (Settings)

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)

Inside `contentSunny` (the existing Sunny accordion content), **at the very top**, add a Time Mode card row before all existing controls:

```xml
<!-- Time Mode selector -->
<HorizontalScrollView android:id="@+id/scrollTimeModeCards" ...>
  <LinearLayout android:orientation="horizontal" ...>
    <com.google.android.material.card.MaterialCardView android:id="@+id/cardTimeModeDay"   ...>
      <!-- Icon: ☀️  Label: "Día"       -->
    </com.google.android.material.card.MaterialCardView>
    <com.google.android.material.card.MaterialCardView android:id="@+id/cardTimeModeNight" ...>
      <!-- Icon: 🌙  Label: "Noche"     -->
    </com.google.android.material.card.MaterialCardView>
    <com.google.android.material.card.MaterialCardView android:id="@+id/cardTimeModeCombined" ...>
      <!-- Icon: 🌅  Label: "Combinado" -->
    </com.google.android.material.card.MaterialCardView>
  </LinearLayout>
</HorizontalScrollView>
```

Wrap all existing Sunny controls in `<LinearLayout android:id="@+id/layoutDayControls">` for easy show/hide.

Add new block `<LinearLayout android:id="@+id/layoutNightControls" android:visibility="gone">` with:

1. **Moon phase selector** — `HorizontalScrollView` with 8 `MaterialCardView` cards, each showing a simple phase symbol drawn by a small canvas or a Unicode glyph:
   - 🌑 Luna Nueva · 🌒 Creciente · 🌓 Cuarto Creciente · 🌔 Gibosa Creciente  
   - 🌕 Llena · 🌖 Gibosa Menguante · 🌗 Cuarto Menguante · 🌘 Menguante

2. **Moon path direction** — same 4-card `HorizontalScrollView` as the sun (Izq→Der, Der→Izq, Estático, Aleatorio) with IDs `cardMoonDirL2R`, `cardMoonDirR2L`, `cardMoonDirStationary`, `cardMoonDirRandom`.

3. **Moon speed slider** (`seekBarMoonMoveSpeed` / `textViewMoonMoveSpeedValue`).

4. **Moon stationary position** — same 6-card row as the sun's, with IDs prefixed `cardMoonPos…`.  Visible only when moon path = Estático.

5. **Star color selector** — 6-card `HorizontalScrollView`:
   - Blanco · Azul Frío · Amarillo Cálido · Rosa · Verde · Mixto  
   Cards show a filled circle in the corresponding colour. IDs: `cardStarColor0`…`cardStarColor5`.

6. **Star density slider** (`seekBarStarDensity` / `textViewStarDensityValue`).

7. **Night cloud density slider** (`seekBarNightCloudDensity` / `textViewNightCloudDensityValue`).

Add new block `<LinearLayout android:id="@+id/layoutCombinedControls" android:visibility="gone">` with:
- **Gradient cycle speed slider** (`seekBarGradientCycleSpeed` / `textViewGradientCycleSpeedValue`).  
  Label: "Duración del ciclo" with hint text showing the equivalent duration in seconds.

---

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)

**`setupTimeModeCards()`** — new private method called from `onCreate()`:
- Binds `cardTimeModeDay`, `cardTimeModeNight`, `cardTimeModeCombined`.
- On click: saves `configManager.setTimeMode(index)`, updates card border strokes (selected = accent stroke, unselected = transparent), calls `updateTimeModeVisibility(index)`.

**`updateTimeModeVisibility(mode: Int)`** — shows/hides the three layout groups:
```kotlin
layoutDayControls.visibility      = if (mode == 0 || mode == 2) VISIBLE else GONE
layoutNightControls.visibility     = if (mode == 1 || mode == 2) VISIBLE else GONE
layoutCombinedControls.visibility  = if (mode == 2) VISIBLE else GONE
```

**`setupMoonPhaseCards()`** — binds 8 phase cards:
- On click: saves `configManager.setMoonPhase(index)`, highlights selected card.

**`setupMoonDirectionCards()`** — identical logic to `setupSunDirectionCards()` but reads/writes `moon_path_direction`. Also toggles visibility of moon speed slider vs moon stationary position row.

**`setupMoonStationaryCards()`** — mirrors `setupSunStationaryCards()`.

**`setupStarColorCards()`** — binds 6 star color cards:
- On click: saves `configManager.setStarColorIndex(index)`.
- Each card background is a solid circle in the respective color:

  | Index | Color name | RGB |
  |---|---|---|
  | 0 | Blanco | (1.00, 1.00, 1.00) |
  | 1 | Azul Frío | (0.60, 0.80, 1.00) |
  | 2 | Amarillo Cálido | (1.00, 0.95, 0.70) |
  | 3 | Rosa | (1.00, 0.70, 0.85) |
  | 4 | Verde | (0.50, 1.00, 0.70) |
  | 5 | Mixto | gradient drawn on card |

**`setupNightSliders()`** — wires `seekBarStarDensity`, `seekBarNightCloudDensity`, `seekBarGradientCycleSpeed` via the existing `setupSlider()` helper.

**`updateSummaries()` additions:**
- Time mode label: `"Día"`, `"Noche"`, `"Combinado"`.
- Moon phase label: `"Luna Nueva"`, `"Creciente"`, …, `"Menguante"`.
- Moon direction label: same mapping as sun direction.
- Star colour label: `"Blanco"`, `"Azul Frío"`, …, `"Mixto"`.

---

## Verification Plan

### Automated Tests
```
.\gradlew.bat compileDebugSources
```

### Manual Verification

1. Open Settings → Sunny tab.
2. Select **Noche** time mode card — verify `layoutNightControls` appears and day controls are hidden.
3. Tap each moon phase card — confirm the selected phase is highlighted and saved.
4. Tap **Aleatorio** moon direction — confirm the speed slider is visible and stationary cards are hidden.
5. Change star density and color — verify settings persist across re-opens.
6. Select **Combinado** — verify both day and night control groups are visible plus the cycle speed slider.
7. Select **Día** — verify only existing sunny controls are visible (no regressions).
8. On the live wallpaper in Night mode: stars should twinkle, moon should move, procedural clouds should be visible as soft polygon shapes.
9. In Combined mode: the sky gradient should cycle smoothly from warm orange/blue (day) to deep blue/black (night) and back.
