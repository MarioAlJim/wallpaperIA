package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Storm-mode (rain, clouds, lightning, wind) preferences. */
internal class StormConfigStore(private val prefs: SharedPreferences) {
    fun getCloudDensity(): Int = prefs.getInt(ConfigManager.KEY_CLOUD_DENSITY, ConfigManager.DEFAULT_CLOUD_DENSITY)
    fun setCloudDensity(density: Int) { prefs.edit().putInt(ConfigManager.KEY_CLOUD_DENSITY, density.coerceIn(0, 100)).apply() }

    fun getRainIntensity(): Int = prefs.getInt(ConfigManager.KEY_RAIN_INTENSITY, ConfigManager.DEFAULT_RAIN_INTENSITY)
    fun setRainIntensity(intensity: Int) { prefs.edit().putInt(ConfigManager.KEY_RAIN_INTENSITY, intensity.coerceIn(0, 100)).apply() }

    fun getLightningFrequency(): Int = prefs.getInt(ConfigManager.KEY_LIGHTNING_FREQUENCY, ConfigManager.DEFAULT_LIGHTNING_FREQUENCY)
    fun setLightningFrequency(frequency: Int) { prefs.edit().putInt(ConfigManager.KEY_LIGHTNING_FREQUENCY, frequency.coerceIn(0, 100)).apply() }

    fun getWindDirection(): Int = prefs.getInt(ConfigManager.KEY_WIND_DIRECTION, ConfigManager.DEFAULT_WIND_DIRECTION)
    fun setWindDirection(direction: Int) { prefs.edit().putInt(ConfigManager.KEY_WIND_DIRECTION, direction.coerceIn(0, 2)).apply() }

    fun getRainColorIndex(): Int = prefs.getInt(ConfigManager.KEY_RAIN_COLOR_INDEX, ConfigManager.DEFAULT_RAIN_COLOR_INDEX)
    fun setRainColorIndex(index: Int) { prefs.edit().putInt(ConfigManager.KEY_RAIN_COLOR_INDEX, index.coerceIn(0, 5)).apply() }

    fun getWindIntensity(): Int = prefs.getInt(ConfigManager.KEY_WIND_INTENSITY, ConfigManager.DEFAULT_WIND_INTENSITY)
    fun setWindIntensity(intensity: Int) { prefs.edit().putInt(ConfigManager.KEY_WIND_INTENSITY, intensity.coerceIn(0, 100)).apply() }

    fun getRainSpeed(): Int = prefs.getInt(ConfigManager.KEY_RAIN_SPEED, ConfigManager.DEFAULT_RAIN_SPEED)
    fun setRainSpeed(speed: Int) { prefs.edit().putInt(ConfigManager.KEY_RAIN_SPEED, speed.coerceIn(0, 100)).apply() }

    fun getLightningColorIndex(): Int = prefs.getInt(ConfigManager.KEY_LIGHTNING_COLOR_INDEX, ConfigManager.DEFAULT_LIGHTNING_COLOR_INDEX)
    fun setLightningColorIndex(index: Int) { prefs.edit().putInt(ConfigManager.KEY_LIGHTNING_COLOR_INDEX, index.coerceIn(0, 6)).apply() }

    fun getLightningDuration(): Int = prefs.getInt(ConfigManager.KEY_LIGHTNING_DURATION, ConfigManager.DEFAULT_LIGHTNING_DURATION)
    fun setLightningDuration(duration: Int) { prefs.edit().putInt(ConfigManager.KEY_LIGHTNING_DURATION, duration.coerceIn(0, 100)).apply() }

    fun getBackgroundIndex(): Int = prefs.getInt(ConfigManager.KEY_BACKGROUND_INDEX, ConfigManager.DEFAULT_BACKGROUND_INDEX)
    fun setBackgroundIndex(index: Int) { prefs.edit().putInt(ConfigManager.KEY_BACKGROUND_INDEX, index.coerceIn(0, 8)).apply() }

    fun getCloudFlashFrequency(): Int = prefs.getInt(ConfigManager.KEY_CLOUD_FLASH_FREQUENCY, ConfigManager.DEFAULT_CLOUD_FLASH_FREQUENCY)
    fun setCloudFlashFrequency(frequency: Int) { prefs.edit().putInt(ConfigManager.KEY_CLOUD_FLASH_FREQUENCY, frequency.coerceIn(0, 100)).apply() }

    fun getCloudFlashColorIndex(): Int = prefs.getInt(ConfigManager.KEY_CLOUD_FLASH_COLOR_INDEX, ConfigManager.DEFAULT_CLOUD_FLASH_COLOR_INDEX)
    fun setCloudFlashColorIndex(index: Int) { prefs.edit().putInt(ConfigManager.KEY_CLOUD_FLASH_COLOR_INDEX, index.coerceIn(0, 6)).apply() }

    fun getCloudDynamicsSpeed(): Int = prefs.getInt(ConfigManager.KEY_CLOUD_DYNAMICS_SPEED, ConfigManager.DEFAULT_CLOUD_DYNAMICS_SPEED)
    fun setCloudDynamicsSpeed(speed: Int) { prefs.edit().putInt(ConfigManager.KEY_CLOUD_DYNAMICS_SPEED, speed.coerceIn(0, 100)).apply() }

    fun isLightningFlashEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_LIGHTNING_FLASH_ENABLED, ConfigManager.DEFAULT_LIGHTNING_FLASH_ENABLED)
    fun setLightningFlashEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_LIGHTNING_FLASH_ENABLED, enabled).apply() }

    fun isCloudFlashEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_CLOUD_FLASH_ENABLED, ConfigManager.DEFAULT_CLOUD_FLASH_ENABLED)
    fun setCloudFlashEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_CLOUD_FLASH_ENABLED, enabled).apply() }

    fun isInteractiveLightningEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_INTERACTIVE_LIGHTNING_ENABLED, ConfigManager.DEFAULT_INTERACTIVE_LIGHTNING_ENABLED)
    fun setInteractiveLightningEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_INTERACTIVE_LIGHTNING_ENABLED, enabled).apply() }

    fun isScreenDropletsEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_SCREEN_DROPLETS_ENABLED, ConfigManager.DEFAULT_SCREEN_DROPLETS_ENABLED)
    fun setScreenDropletsEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_SCREEN_DROPLETS_ENABLED, enabled).apply() }

    fun getScreenDropletsSize(): Int = prefs.getInt(ConfigManager.KEY_SCREEN_DROPLETS_SIZE, ConfigManager.DEFAULT_SCREEN_DROPLETS_SIZE)
    fun setScreenDropletsSize(size: Int) { prefs.edit().putInt(ConfigManager.KEY_SCREEN_DROPLETS_SIZE, size.coerceIn(10, 200)).apply() }

    fun getRainSpawnMode(): Int = prefs.getInt(ConfigManager.KEY_RAIN_SPAWN_MODE, ConfigManager.DEFAULT_RAIN_SPAWN_MODE)
    fun setRainSpawnMode(mode: Int) { prefs.edit().putInt(ConfigManager.KEY_RAIN_SPAWN_MODE, mode.coerceIn(0, 2)).apply() }

    fun isWindLinesEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_WIND_LINES_ENABLED, ConfigManager.DEFAULT_WIND_LINES_ENABLED)
    fun setWindLinesEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_WIND_LINES_ENABLED, enabled).apply() }

    fun getWindLinesIntensity(): Int = prefs.getInt(ConfigManager.KEY_WIND_LINES_INTENSITY, ConfigManager.DEFAULT_WIND_LINES_INTENSITY)
    fun setWindLinesIntensity(intensity: Int) { prefs.edit().putInt(ConfigManager.KEY_WIND_LINES_INTENSITY, intensity.coerceIn(0, 100)).apply() }

    fun isStormVariableEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_STORM_VARIABLE_ENABLED, ConfigManager.DEFAULT_STORM_VARIABLE_ENABLED)
    fun setStormVariableEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_STORM_VARIABLE_ENABLED, enabled).apply() }
}
