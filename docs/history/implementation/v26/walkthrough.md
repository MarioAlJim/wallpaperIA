# Walkthrough: Custom Sun Position with Increased Y Range

I have successfully added a configuration setting to customize the location of the sun in **Sunny Mode** when the trajectory is set to **Estático** (Stationary). The user can choose between 5 pre-established positions, or adjust the sun's position freely using X and Y sliders.

Additionally, I increased the predefined top corner coordinates so that the sun sits high enough in the actual corners of the screen.

## Changes Made

### 1. Extended Settings Configuration
- **Interface modified**: [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigProvider.kt)
  - Added `getSunCustomX(): Int` and `getSunCustomY(): Int` to retrieve custom positions (0-100).
- **Class modified**: [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigManager.kt)
  - Added SharedPreferences keys and defaults (`KEY_SUN_CUSTOM_X` / `KEY_SUN_CUSTOM_Y`).
  - Implemented getters and setters. Allowed stationary position selection index up to `5`.

### 2. Rendered Stationary Position presets and Custom position mapping
- **File modified**: [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)
  - Adjusted the coordinate values for the predefined top-left and top-right stationary positions inside `onUpdate()` to place the center of the sun exactly at the corners of the viewport (which causes exactly a quarter of the sun disk to be visible inside the screen):
    - **Top Left**: $X = -1.0, Y = \frac{1.0}{\text{aspectRatio}}$
    - **Top Right**: $X = 1.0, Y = \frac{1.0}{\text{aspectRatio}}$
  - Added mapping for option `5` (Custom/Personalizado) inside `onUpdate()` to restrict coordinates to viewport boundaries so the sun never disappears completely:
    - $sunX = -1.0f + \frac{\text{customX}}{100} \times 2.0f$ (mapping to range $[-1.0, 1.0]$)
    - $sunY = -0.8f + \frac{\text{customY}}{100} \times \left(\frac{1.0}{\text{aspectRatio}} + 0.8f\right)$ (mapping to range $[-0.8, \frac{1.0}{\text{aspectRatio}}]$).

### 3. Developed visual cards and sliders in Settings Layout
- **File modified**: [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
  - Appended a 6th `MaterialCardView` card with ID `@+id/cardSunPosCustom` labeled **Personalizado** with a custom "X, Y" text representation.
  - Added a container `@+id/layoutSunCustomXY` containing two sliders (Horizontal position X and Vertical position Y).
- **File modified**: [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
  - Bound the 6th card and the custom X/Y sliders.
  - Implemented logic to hide/show `layoutSunCustomXY` container dynamically depending on whether the "Personalizado" card is active.
  - Configured ChangeListeners on the X and Y sliders to write to SharedPreferences immediately, giving real-time feedback.
  - Updated settings summary to show the current X/Y coordinates when custom mode is selected: e.g. `Sol: Fijo (Libre (50, 74))`.
