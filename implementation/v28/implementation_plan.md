# Implementation Plan: Sun Movement Heights and Random Paths

The goal is to update the maximum height of the sun's trajectory in the movement modes (Left-to-Right / Right-to-Left) to allow the sun to travel higher in the sky. Additionally, we will replace the dropdown spinner for the sun path direction with a visual card selector in the settings screen, and implement a new **Random Movement** mode where the sun travels between random off-screen start and end coordinates.

---

## User Review Required

> [!IMPORTANT]
> **Random Movement Route Generation**: In the new "Aleatorio" mode, when the sun reaches its destination (completely off-screen), a new random route is generated immediately. The start and end positions will always be completely off-screen (e.g. left, right, top, or bottom edges) so that the sun naturally rises and sets from different directions across the sky without suddenly appearing/disappearing.
>
> **Elevated Sun Path**: In Left-to-Right and Right-to-Left modes, the maximum height of the sun's trajectory will be increased to peak at `Y = 1.2f` (previously `0.5f`), traversing a taller, more natural arc over portrait displays.

---

## Proposed Changes

### 1. Configuration & SharedPreferences

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigManager.kt)
- Relax the bounds in `setSunPathDirection(direction)` from `.coerceIn(0, 2)` to `.coerceIn(0, 3)` to support option `3` (Random).

---

### 2. Rendering & Movement Logic

#### [MODIFY] [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)
- **Taller Trajectories**: In `onUpdate()`, change the Left-to-Right and Right-to-Left movement parabolas to use a higher peak of `1.2f` and a sharper fall-off of `1.5f` so they set cleanly at the screen edges:
  - `sunY = 1.2f - 1.5f * (sunX * sunX)` (previously `0.5f - 0.7f * (sunX * sunX)`)
- **Random Path Variables**: Declare state variables in `SunnyRenderer`:
  - `private var randomStartX = 0f`
  - `private var randomStartY = 0f`
  - `private var randomEndX = 0f`
  - `private var randomEndY = 0f`
  - `private var randomProgress = 0f`
  - `private var isRandomPathInitialized = false`
- **Random Path Logic**:
  - Implement a helper method `generateRandomPath()`:
    - Selects a random start edge (0: Left, 1: Right, 2: Top, 3: Bottom) and a random end edge (different from start).
    - Left edge: `X = -1.5f`, `Y` random between `-0.8f` and `1.2f / aspectRatio`.
    - Right edge: `X = 1.5f`, `Y` random between `-0.8f` and `1.2f / aspectRatio`.
    - Top edge: `Y = 1.2f / aspectRatio`, `X` random between `-1.2f` and `1.2f`.
    - Bottom edge: `Y = -1.2f`, `X` random between `-1.2f` and `1.2f`.
  - In `onUpdate()`, if `direction == 3` (Random):
    - If not initialized, call `generateRandomPath()`.
    - Advance `randomProgress` using the speed setting:
      `val speedFactor = 0.05f + (configProvider.getSunMoveSpeed() / 100f) * 0.45f`
      `randomProgress += deltaTime * speedFactor`
    - Linearly interpolate `sunX` and `sunY` between start and end.
    - If `randomProgress >= 1.0f`, reset progress to `0.0f` and trigger `generateRandomPath()`.

---

### 3. User Interface (Settings)

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Replace `spinnerSunPathDirection` and its outer `TextInputLayout` with a `HorizontalScrollView` containing 4 selection cards:
  - **Izquierda a Derecha** (`cardSunDirL2R`): Displays `→` in the mini screen.
  - **Derecha a Izquierda** (`cardSunDirR2L`): Displays `←` in the mini screen.
  - **Estático** (`cardSunDirStationary`): Displays `●` in the mini screen.
  - **Aleatorio** (`cardSunDirRandom`): Displays `🎲` in the mini screen.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Bind the 4 new sun direction cards.
- Implement click listeners on the cards to save the selection index (0-3), trigger border highlights, and toggle layout visibilities (speed slider vs. stationary positions selector).
- Update `updateSummaries()` summary mappings for the new index:
  - `3` -> `"Aleatorio"`
  - Also update L2R / R2L labels.

---

## Verification Plan

### Automated Tests
- Run `.\gradlew.bat compileDebugSources` to ensure there are no compilation or layout errors.

### Manual Verification
1. Open settings -> Sunny Mode.
2. Select the "Aleatorio" movement card. Verify the speed slider is visible, and the stationary options are hidden.
3. Observe the sun on the wallpaper preview: it should traverse the screen from a random edge to another, disappear, and start a new path immediately.
4. Select "Izquierda a Derecha" or "Derecha a Izquierda" and verify the sun traverses a significantly taller path peaking high in the sky.
