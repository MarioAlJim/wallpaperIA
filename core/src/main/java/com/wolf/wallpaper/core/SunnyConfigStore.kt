package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Sunny-mode (sun, sky, god rays, lens flare) preferences. */
internal class SunnyConfigStore(prefs: SharedPreferences) : CachedPrefStore(prefs) {
    fun getActiveEffect(): Int = cachedInt(ConfigManager.KEY_ACTIVE_EFFECT, ConfigManager.DEFAULT_ACTIVE_EFFECT)
    fun setActiveEffect(effect: Int) { putInt(ConfigManager.KEY_ACTIVE_EFFECT, effect.coerceIn(0, 1)) }

    fun getSunSize(): Int = cachedInt(ConfigManager.KEY_SUN_SIZE, ConfigManager.DEFAULT_SUN_SIZE)
    fun setSunSize(size: Int) { putInt(ConfigManager.KEY_SUN_SIZE, size.coerceIn(0, 100)) }

    fun getSunSpeed(): Int = cachedInt(ConfigManager.KEY_SUN_SPEED, ConfigManager.DEFAULT_SUN_SPEED)
    fun setSunSpeed(speed: Int) { putInt(ConfigManager.KEY_SUN_SPEED, speed.coerceIn(0, 100)) }

    fun getSunnyTheme(): Int = cachedInt(ConfigManager.KEY_SUNNY_THEME, ConfigManager.DEFAULT_SUNNY_THEME)
    fun setSunnyTheme(theme: Int) { putInt(ConfigManager.KEY_SUNNY_THEME, theme.coerceIn(0, 3)) }

    fun getSunPathDirection(): Int = cachedInt(ConfigManager.KEY_SUN_PATH_DIRECTION, ConfigManager.DEFAULT_SUN_PATH_DIRECTION)
    fun setSunPathDirection(direction: Int) { putInt(ConfigManager.KEY_SUN_PATH_DIRECTION, direction.coerceIn(0, 4)) }

    fun getSunMoveSpeed(): Int = cachedInt(ConfigManager.KEY_SUN_MOVE_SPEED, ConfigManager.DEFAULT_SUN_MOVE_SPEED)
    fun setSunMoveSpeed(speed: Int) { putInt(ConfigManager.KEY_SUN_MOVE_SPEED, speed.coerceIn(0, 100)) }

    fun getSunnyBackgroundIndex(): Int = cachedInt(ConfigManager.KEY_SUNNY_BACKGROUND_INDEX, ConfigManager.DEFAULT_SUNNY_BACKGROUND_INDEX)
    fun setSunnyBackgroundIndex(index: Int) { putInt(ConfigManager.KEY_SUNNY_BACKGROUND_INDEX, index.coerceIn(0, 8)) }

    fun getSunStationaryPosition(): Int = cachedInt(ConfigManager.KEY_SUN_STATIONARY_POSITION, ConfigManager.DEFAULT_SUN_STATIONARY_POSITION)
    fun setSunStationaryPosition(position: Int) { putInt(ConfigManager.KEY_SUN_STATIONARY_POSITION, position.coerceIn(0, 5)) }

    fun getSunCustomX(): Int = cachedInt(ConfigManager.KEY_SUN_CUSTOM_X, ConfigManager.DEFAULT_SUN_CUSTOM_X)
    fun setSunCustomX(x: Int) { putInt(ConfigManager.KEY_SUN_CUSTOM_X, x.coerceIn(0, 100)) }

    fun getSunCustomY(): Int = cachedInt(ConfigManager.KEY_SUN_CUSTOM_Y, ConfigManager.DEFAULT_SUN_CUSTOM_Y)
    fun setSunCustomY(y: Int) { putInt(ConfigManager.KEY_SUN_CUSTOM_Y, y.coerceIn(0, 100)) }

    fun getMoonCustomX(): Int = cachedInt(ConfigManager.KEY_MOON_CUSTOM_X, ConfigManager.DEFAULT_MOON_CUSTOM_X)
    fun setMoonCustomX(x: Int) { putInt(ConfigManager.KEY_MOON_CUSTOM_X, x.coerceIn(0, 100)) }

    fun getMoonCustomY(): Int = cachedInt(ConfigManager.KEY_MOON_CUSTOM_Y, ConfigManager.DEFAULT_MOON_CUSTOM_Y)
    fun setMoonCustomY(y: Int) { putInt(ConfigManager.KEY_MOON_CUSTOM_Y, y.coerceIn(0, 100)) }

    fun getSunnyCustomSkyTopColor(): Int = cachedInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_TOP_COLOR, ConfigManager.DEFAULT_SUNNY_CUSTOM_SKY_TOP_COLOR)
    fun setSunnyCustomSkyTopColor(color: Int) { putInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_TOP_COLOR, color) }

    fun getSunnyCustomSkyBottomColor(): Int = cachedInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_BOTTOM_COLOR, ConfigManager.DEFAULT_SUNNY_CUSTOM_SKY_BOTTOM_COLOR)
    fun setSunnyCustomSkyBottomColor(color: Int) { putInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_BOTTOM_COLOR, color) }

    fun isSunnyGodRaysEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_SUNNY_GOD_RAYS_ENABLED, ConfigManager.DEFAULT_SUNNY_GOD_RAYS_ENABLED)
    fun setSunnyGodRaysEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_SUNNY_GOD_RAYS_ENABLED, enabled) }

    fun getSunnyGodRaysIntensity(): Int = cachedInt(ConfigManager.KEY_SUNNY_GOD_RAYS_INTENSITY, ConfigManager.DEFAULT_SUNNY_GOD_RAYS_INTENSITY)
    fun setSunnyGodRaysIntensity(intensity: Int) { putInt(ConfigManager.KEY_SUNNY_GOD_RAYS_INTENSITY, intensity.coerceIn(0, 100)) }

    fun isSunnyLensFlareEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_SUNNY_LENS_FLARE_ENABLED, ConfigManager.DEFAULT_SUNNY_LENS_FLARE_ENABLED)
    fun setSunnyLensFlareEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_SUNNY_LENS_FLARE_ENABLED, enabled) }

    fun isSunnyLensFlareChromaticEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_SUNNY_LENS_FLARE_CHROMATIC, ConfigManager.DEFAULT_SUNNY_LENS_FLARE_CHROMATIC)
    fun setSunnyLensFlareChromaticEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_SUNNY_LENS_FLARE_CHROMATIC, enabled) }

    fun getSunnyLensFlareIntensity(): Int = cachedInt(ConfigManager.KEY_SUNNY_LENS_FLARE_INTENSITY, ConfigManager.DEFAULT_SUNNY_LENS_FLARE_INTENSITY)
    fun setSunnyLensFlareIntensity(intensity: Int) { putInt(ConfigManager.KEY_SUNNY_LENS_FLARE_INTENSITY, intensity.coerceIn(0, 100)) }

    fun isSunnyGyroEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_SUNNY_GYRO_ENABLED, ConfigManager.DEFAULT_SUNNY_GYRO_ENABLED)
    fun setSunnyGyroEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_SUNNY_GYRO_ENABLED, enabled) }

    fun isSunnyTouchBurstEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_SUNNY_TOUCH_BURST_ENABLED, ConfigManager.DEFAULT_SUNNY_TOUCH_BURST_ENABLED)
    fun setSunnyTouchBurstEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_SUNNY_TOUCH_BURST_ENABLED, enabled) }

    fun isFirefliesEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_FIREFLIES_ENABLED, ConfigManager.DEFAULT_FIREFLIES_ENABLED)
    fun setFirefliesEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_FIREFLIES_ENABLED, enabled) }

    fun isRainbowAlwaysEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_RAINBOW_ALWAYS, ConfigManager.DEFAULT_RAINBOW_ALWAYS)
    fun setRainbowAlwaysEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_RAINBOW_ALWAYS, enabled) }
}
