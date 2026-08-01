# Plan: Custom / Free Sun Position Adjustment

The goal is to allow the user to freely configure the sun's location when in **Estático** (Stationary) mode. We will add a 6th card option called **Personalizado** (Custom) to the sun position selector. Selecting this card will dynamically display two sliders (one for the X coordinate and one for the Y coordinate), letting the user adjust the sun's position anywhere in the sky.

## User Review Required

> [!NOTE]
> **Dynamic Layout Visibility**: The custom X and Y sliders will only be visible when the "Personalizado" card is active, preventing UI clutter when using the preset locations.

## Proposed Changes

### Interfaces and SharedPreferences

#### [MODIFY] [ConfigProvider.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigProvider.kt)
- Add `getSunCustomX(): Int` and `getSunCustomY(): Int` to retrieve the slider values (0-100).

#### [MODIFY] [ConfigManager.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/core/src/main/java/com/wolf/wallpaper/core/ConfigManager.kt)
- Add constants `KEY_SUN_CUSTOM_X`, `KEY_SUN_CUSTOM_Y` (defaults: 50 for X [center], 74 for Y [upper sky]).
- Implement getters and setters for the custom X and Y values.

### Rendering

#### [MODIFY] [SunnyRenderer.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/wallpaper-sunny/src/main/java/com/wolf/wallpaper/sunny/SunnyRenderer.kt)
- Update the stationary sun positioning inside `onUpdate` to map index 5 (Custom) to custom coordinates:
  - $sunX = -1.0f + \frac{\text{customX}}{100} \times 2.0f$
  - $sunY = -0.8f + \frac{\text{customY}}{100} \times \left(\frac{1.0}{\text{aspectRatio}} + 0.8f\right)$ (mapped to screen corners to keep at least 1/4 of the sun visible)

### User Interface

#### [MODIFY] [activity_settings.xml](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/res/layout/activity_settings.xml)
- Add a 6th MaterialCardView card for the **Personalizado** option. This card will display a mini screen container showing both sliders/sliders icon or an adjustable layout indicator.
- Add a layout block `layoutSunCustomXY` containing the X and Y Sliders and their text value indicators.

#### [MODIFY] [WallpaperSettingsActivity.kt](file:///C:/Users/Wildwolf/AndroidStudioProjects/wallpaper/app/src/main/java/com/wolf/wallpaper/WallpaperSettingsActivity.kt)
- Bind the 6th card and the custom X/Y sliders.
- Configure click listeners for the sliders to write to SharedPreferences immediately.
- Update visibility triggers: show `layoutSunCustomXY` only when the "Personalizado" card is selected.
- Update summary text to show `Sol: Fijo (Personalizado: X, Y)`.

## Verification Plan

### Manual Verification
- Compile and run the project.
- Go to Sunny Mode -> select "Estático" -> select "Personalizado".
- Adjust the X and Y sliders and confirm the sun updates its position in real time on the wallpaper.
