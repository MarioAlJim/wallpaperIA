package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Sunny-mode (sun, sky, god rays, lens flare) preferences. */
internal class SunnyConfigStore(private val prefs: SharedPreferences) {
    fun getActiveEffect(): Int = prefs.getInt(ConfigManager.KEY_ACTIVE_EFFECT, ConfigManager.DEFAULT_ACTIVE_EFFECT)
    fun setActiveEffect(effect: Int) { prefs.edit().putInt(ConfigManager.KEY_ACTIVE_EFFECT, effect.coerceIn(0, 1)).apply() }

    fun getSunSize(): Int = prefs.getInt(ConfigManager.KEY_SUN_SIZE, ConfigManager.DEFAULT_SUN_SIZE)
    fun setSunSize(size: Int) { prefs.edit().putInt(ConfigManager.KEY_SUN_SIZE, size.coerceIn(0, 100)).apply() }

    fun getSunSpeed(): Int = prefs.getInt(ConfigManager.KEY_SUN_SPEED, ConfigManager.DEFAULT_SUN_SPEED)
    fun setSunSpeed(speed: Int) { prefs.edit().putInt(ConfigManager.KEY_SUN_SPEED, speed.coerceIn(0, 100)).apply() }

    fun getSunnyTheme(): Int = prefs.getInt(ConfigManager.KEY_SUNNY_THEME, ConfigManager.DEFAULT_SUNNY_THEME)
    fun setSunnyTheme(theme: Int) { prefs.edit().putInt(ConfigManager.KEY_SUNNY_THEME, theme.coerceIn(0, 3)).apply() }

    fun getSunPathDirection(): Int = prefs.getInt(ConfigManager.KEY_SUN_PATH_DIRECTION, ConfigManager.DEFAULT_SUN_PATH_DIRECTION)
    fun setSunPathDirection(direction: Int) { prefs.edit().putInt(ConfigManager.KEY_SUN_PATH_DIRECTION, direction.coerceIn(0, 3)).apply() }

    fun getSunMoveSpeed(): Int = prefs.getInt(ConfigManager.KEY_SUN_MOVE_SPEED, ConfigManager.DEFAULT_SUN_MOVE_SPEED)
    fun setSunMoveSpeed(speed: Int) { prefs.edit().putInt(ConfigManager.KEY_SUN_MOVE_SPEED, speed.coerceIn(0, 100)).apply() }

    fun getSunnyBackgroundIndex(): Int = prefs.getInt(ConfigManager.KEY_SUNNY_BACKGROUND_INDEX, ConfigManager.DEFAULT_SUNNY_BACKGROUND_INDEX)
    fun setSunnyBackgroundIndex(index: Int) { prefs.edit().putInt(ConfigManager.KEY_SUNNY_BACKGROUND_INDEX, index.coerceIn(0, 7)).apply() }

    fun getSunStationaryPosition(): Int = prefs.getInt(ConfigManager.KEY_SUN_STATIONARY_POSITION, ConfigManager.DEFAULT_SUN_STATIONARY_POSITION)
    fun setSunStationaryPosition(position: Int) { prefs.edit().putInt(ConfigManager.KEY_SUN_STATIONARY_POSITION, position.coerceIn(0, 5)).apply() }

    fun getSunCustomX(): Int = prefs.getInt(ConfigManager.KEY_SUN_CUSTOM_X, ConfigManager.DEFAULT_SUN_CUSTOM_X)
    fun setSunCustomX(x: Int) { prefs.edit().putInt(ConfigManager.KEY_SUN_CUSTOM_X, x.coerceIn(0, 100)).apply() }

    fun getSunCustomY(): Int = prefs.getInt(ConfigManager.KEY_SUN_CUSTOM_Y, ConfigManager.DEFAULT_SUN_CUSTOM_Y)
    fun setSunCustomY(y: Int) { prefs.edit().putInt(ConfigManager.KEY_SUN_CUSTOM_Y, y.coerceIn(0, 100)).apply() }

    fun getMoonCustomX(): Int = prefs.getInt(ConfigManager.KEY_MOON_CUSTOM_X, ConfigManager.DEFAULT_MOON_CUSTOM_X)
    fun setMoonCustomX(x: Int) { prefs.edit().putInt(ConfigManager.KEY_MOON_CUSTOM_X, x.coerceIn(0, 100)).apply() }

    fun getMoonCustomY(): Int = prefs.getInt(ConfigManager.KEY_MOON_CUSTOM_Y, ConfigManager.DEFAULT_MOON_CUSTOM_Y)
    fun setMoonCustomY(y: Int) { prefs.edit().putInt(ConfigManager.KEY_MOON_CUSTOM_Y, y.coerceIn(0, 100)).apply() }

    fun getSunnyCustomSkyTopColor(): Int = prefs.getInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_TOP_COLOR, ConfigManager.DEFAULT_SUNNY_CUSTOM_SKY_TOP_COLOR)
    fun setSunnyCustomSkyTopColor(color: Int) { prefs.edit().putInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_TOP_COLOR, color).apply() }

    fun getSunnyCustomSkyBottomColor(): Int = prefs.getInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_BOTTOM_COLOR, ConfigManager.DEFAULT_SUNNY_CUSTOM_SKY_BOTTOM_COLOR)
    fun setSunnyCustomSkyBottomColor(color: Int) { prefs.edit().putInt(ConfigManager.KEY_SUNNY_CUSTOM_SKY_BOTTOM_COLOR, color).apply() }

    fun isSunnyGodRaysEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_SUNNY_GOD_RAYS_ENABLED, ConfigManager.DEFAULT_SUNNY_GOD_RAYS_ENABLED)
    fun setSunnyGodRaysEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_SUNNY_GOD_RAYS_ENABLED, enabled).apply() }

    fun getSunnyGodRaysIntensity(): Int = prefs.getInt(ConfigManager.KEY_SUNNY_GOD_RAYS_INTENSITY, ConfigManager.DEFAULT_SUNNY_GOD_RAYS_INTENSITY)
    fun setSunnyGodRaysIntensity(intensity: Int) { prefs.edit().putInt(ConfigManager.KEY_SUNNY_GOD_RAYS_INTENSITY, intensity.coerceIn(0, 100)).apply() }

    fun isSunnyLensFlareEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_SUNNY_LENS_FLARE_ENABLED, ConfigManager.DEFAULT_SUNNY_LENS_FLARE_ENABLED)
    fun setSunnyLensFlareEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_SUNNY_LENS_FLARE_ENABLED, enabled).apply() }

    fun getSunnyLensFlareIntensity(): Int = prefs.getInt(ConfigManager.KEY_SUNNY_LENS_FLARE_INTENSITY, ConfigManager.DEFAULT_SUNNY_LENS_FLARE_INTENSITY)
    fun setSunnyLensFlareIntensity(intensity: Int) { prefs.edit().putInt(ConfigManager.KEY_SUNNY_LENS_FLARE_INTENSITY, intensity.coerceIn(0, 100)).apply() }

    fun isSunnyGyroEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_SUNNY_GYRO_ENABLED, ConfigManager.DEFAULT_SUNNY_GYRO_ENABLED)
    fun setSunnyGyroEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_SUNNY_GYRO_ENABLED, enabled).apply() }

    fun isSunnyTouchBurstEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_SUNNY_TOUCH_BURST_ENABLED, ConfigManager.DEFAULT_SUNNY_TOUCH_BURST_ENABLED)
    fun setSunnyTouchBurstEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_SUNNY_TOUCH_BURST_ENABLED, enabled).apply() }

    fun isFirefliesEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_FIREFLIES_ENABLED, ConfigManager.DEFAULT_FIREFLIES_ENABLED)
    fun setFirefliesEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_FIREFLIES_ENABLED, enabled).apply() }
}
