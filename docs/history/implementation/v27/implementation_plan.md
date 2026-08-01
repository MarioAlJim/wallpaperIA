# Implementation Plan: Custom Sky Gradient Editor in Sunny Mode

We want to allow the user to define their own custom sky gradient colors (Top and Bottom colors) instead of relying solely on the 3 predefined themes. 

We will replace the current spinner dropdown for the sky themes with a card-based visual selector (Noon Blue, Sunset Orange, Purple Dusk, and a 4th "Personalizado" card). When the Custom card is selected, an inline color selection section will be shown, allowing the user to select custom top and bottom colors using a rich, custom-built Color Picker Dialog (which includes presets, RGB sliders, and Hex text input). The custom colors will be passed to the OpenGL shaders to dynamically render the sky gradient and adapt the silhouette colors accordingly.

---

## User Review Required

> [!IMPORTANT]
> **Dynamic Atmospheric Tinting**: To prevent silhouettes from looking out-of-place with custom sky colors, the shader will dynamically compute matching silhouette tints (foreground shadow, background fog, and sun glare highlight) by interpolating the user's custom sky bottom color.
>
> **Interactive Preview**: The "Personalizado" theme card will show a live gradient preview of the selected custom colors in the settings panel itself, updating instantly when the colors are changed.

---

## Proposed Changes

### 1. Configuration & SharedPreferences

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigProvider.kt)
- Add getters for custom sky colors:
  - `fun getSunnyCustomSkyTopColor(): Int`
  - `fun getSunnyCustomSkyBottomColor(): Int`

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigManager.kt)
- Define keys:
  - `KEY_SUNNY_CUSTOM_SKY_TOP_COLOR` = `"sunny_custom_sky_top_color"`
  - `KEY_SUNNY_CUSTOM_SKY_BOTTOM_COLOR` = `"sunny_custom_sky_bottom_color"`
- Define defaults:
  - `DEFAULT_SUNNY_CUSTOM_SKY_TOP_COLOR` = `0xFF1A0D40` (Dark Indigo / Blue-Purple)
  - `DEFAULT_SUNNY_CUSTOM_SKY_BOTTOM_COLOR` = `0xFFF2731A` (Sunset Orange)
- Implement corresponding getters and setters.
- Relax theme limit in `setSunnyTheme(theme)` from `.coerceIn(0, 2)` to `.coerceIn(0, 3)`.

---

### 2. OpenGL Shaders

#### [MODIFY] [sunny.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/assets/shaders/sunny.frag)
- Declare new uniforms:
  - `uniform vec3 uSkyTop;`
  - `uniform vec3 uSkyBottom;`
- In `main()`, if `uTheme == 3`, map `skyTop = uSkyTop` and `skyBottom = uSkyBottom`. Use a warm yellow `sunColor` and a mixed golden-orange `sunGlowColor`.

#### [MODIFY] [sunny_background.frag](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/assets/shaders/sunny_background.frag)
- Declare new uniforms:
  - `uniform vec3 uSkyTop;`
  - `uniform vec3 uSkyBottom;`
- In `main()`, if `uTheme == 3`, compute custom silhouette colors based on `uSkyBottom`:
  - `fgColor = mix(uSkyBottom * 0.12, vec3(0.04, 0.04, 0.06), 0.4)`
  - `bgColor = mix(uSkyBottom, vec3(0.25, 0.25, 0.3), 0.35) * 0.65`
  - `highlightColor = mix(vec3(1.0, 0.95, 0.82), uSkyBottom, 0.4)`

---

### 3. Rendering Pipeline

#### [MODIFY] [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)
- Query and store uniform locations for `uSkyTop` and `uSkyBottom` in both main and background programs.
- In `onDrawFrame()`, query custom colors from `configProvider` if `uTheme == 3`, convert them to normalized float arrays, and pass them as `glUniform3f` to the shaders.
- Adapt the cloud tinting color for the custom theme in `onUpdate()` based on the custom sky bottom color.

---

### 4. User Interface (Settings)

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Replace `spinnerSunnyTheme` and its outer `TextInputLayout` with a `HorizontalScrollView` containing a `LinearLayout` with 4 theme cards:
  - **Celeste** (Noon Blue)
  - **Atardecer** (Sunset Orange)
  - **Anochecer** (Purple Dusk)
  - **Personalizado** (Custom Gradient)
- Add a new collapsible layout `layoutSunnyCustomGradient` below the theme cards.
- Inside `layoutSunnyCustomGradient`, create two side-by-side clickable preview cards:
  - **Color Superior**: Displays a circular preview of the custom top color and its hex code.
  - **Color Inferior**: Displays a circular preview of the custom bottom color and its hex code.

#### [NEW] [dialog_color_picker.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/dialog_color_picker.xml)
- Design a premium, custom dialog layout containing:
  - A color preview card at the top (combining a preview circle and hex input).
  - A grid of preset color circle buttons (curated, modern colors).
  - RGB Sliders (Red, Green, Blue) with value labels (0-255).
  - Hex input EditText.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Bind the new theme cards and layout containers.
- Implement theme card selection logic, updating the cards' border strokes/backgrounds and toggling the visibility of `layoutSunnyCustomGradient`.
- Dynamically build and set `GradientDrawable` backgrounds on the card previews (especially the live gradient preview on the "Personalizado" card).
- Set up click listeners for the custom color picker cards.
- Implement the custom Color Picker Dialog logic (syncing the RGB sliders, Hex EditText, preset grid, and updating the preference value immediately upon approval).

---

## Verification Plan

### Automated Tests
- Run `.\gradlew.bat compileDebugSources` to ensure there are no compilation or layout errors.

### Manual Verification
1. Open settings -> Sunny Mode.
2. Select the "Personalizado" theme card. Verify the custom gradient layout container appears.
3. Click "Color Superior" or "Color Inferior" to open the Color Picker.
4. Drag RGB sliders, input hex strings, or click presets, and verify the preview updates in real-time.
5. Click Accept and verify the live gradient preview inside the "Personalizado" card updates instantly.
6. Verify on the wallpaper preview that the sky gradient matches the chosen colors and the silhouettes adapt their tones correctly.
