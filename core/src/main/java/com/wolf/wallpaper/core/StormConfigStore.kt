package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Storm-mode (rain, clouds, lightning, wind) preferences. */
internal class StormConfigStore(prefs: SharedPreferences) : CachedPrefStore(prefs) {
    fun getCloudDensity(): Int = cachedInt(ConfigManager.KEY_CLOUD_DENSITY, ConfigManager.DEFAULT_CLOUD_DENSITY)
    fun setCloudDensity(density: Int) { putInt(ConfigManager.KEY_CLOUD_DENSITY, density.coerceIn(0, 100)) }

    fun getRainIntensity(): Int = cachedInt(ConfigManager.KEY_RAIN_INTENSITY, ConfigManager.DEFAULT_RAIN_INTENSITY)
    fun setRainIntensity(intensity: Int) { putInt(ConfigManager.KEY_RAIN_INTENSITY, intensity.coerceIn(0, 100)) }

    fun getLightningFrequency(): Int = cachedInt(ConfigManager.KEY_LIGHTNING_FREQUENCY, ConfigManager.DEFAULT_LIGHTNING_FREQUENCY)
    fun setLightningFrequency(frequency: Int) { putInt(ConfigManager.KEY_LIGHTNING_FREQUENCY, frequency.coerceIn(0, 100)) }

    fun getWindDirection(): Int = cachedInt(ConfigManager.KEY_WIND_DIRECTION, ConfigManager.DEFAULT_WIND_DIRECTION)
    fun setWindDirection(direction: Int) { putInt(ConfigManager.KEY_WIND_DIRECTION, direction.coerceIn(0, 2)) }

    fun getRainColorIndex(): Int = cachedInt(ConfigManager.KEY_RAIN_COLOR_INDEX, ConfigManager.DEFAULT_RAIN_COLOR_INDEX)
    fun setRainColorIndex(index: Int) { putInt(ConfigManager.KEY_RAIN_COLOR_INDEX, index.coerceIn(0, 5)) }

    fun getWindIntensity(): Int = cachedInt(ConfigManager.KEY_WIND_INTENSITY, ConfigManager.DEFAULT_WIND_INTENSITY)
    fun setWindIntensity(intensity: Int) { putInt(ConfigManager.KEY_WIND_INTENSITY, intensity.coerceIn(0, 100)) }

    fun getRainSpeed(): Int = cachedInt(ConfigManager.KEY_RAIN_SPEED, ConfigManager.DEFAULT_RAIN_SPEED)
    fun setRainSpeed(speed: Int) { putInt(ConfigManager.KEY_RAIN_SPEED, speed.coerceIn(0, 100)) }

    fun getLightningColorIndex(): Int = cachedInt(ConfigManager.KEY_LIGHTNING_COLOR_INDEX, ConfigManager.DEFAULT_LIGHTNING_COLOR_INDEX)
    fun setLightningColorIndex(index: Int) { putInt(ConfigManager.KEY_LIGHTNING_COLOR_INDEX, index.coerceIn(0, 6)) }

    fun getLightningDuration(): Int = cachedInt(ConfigManager.KEY_LIGHTNING_DURATION, ConfigManager.DEFAULT_LIGHTNING_DURATION)
    fun setLightningDuration(duration: Int) { putInt(ConfigManager.KEY_LIGHTNING_DURATION, duration.coerceIn(0, 100)) }

    fun getBackgroundIndex(): Int = cachedInt(ConfigManager.KEY_BACKGROUND_INDEX, ConfigManager.DEFAULT_BACKGROUND_INDEX)
    fun setBackgroundIndex(index: Int) { putInt(ConfigManager.KEY_BACKGROUND_INDEX, index.coerceIn(0, 8)) }

    fun getCloudFlashFrequency(): Int = cachedInt(ConfigManager.KEY_CLOUD_FLASH_FREQUENCY, ConfigManager.DEFAULT_CLOUD_FLASH_FREQUENCY)
    fun setCloudFlashFrequency(frequency: Int) { putInt(ConfigManager.KEY_CLOUD_FLASH_FREQUENCY, frequency.coerceIn(0, 100)) }

    fun getCloudFlashColorIndex(): Int = cachedInt(ConfigManager.KEY_CLOUD_FLASH_COLOR_INDEX, ConfigManager.DEFAULT_CLOUD_FLASH_COLOR_INDEX)
    fun setCloudFlashColorIndex(index: Int) { putInt(ConfigManager.KEY_CLOUD_FLASH_COLOR_INDEX, index.coerceIn(0, 6)) }

    fun getCloudDynamicsSpeed(): Int = cachedInt(ConfigManager.KEY_CLOUD_DYNAMICS_SPEED, ConfigManager.DEFAULT_CLOUD_DYNAMICS_SPEED)
    fun setCloudDynamicsSpeed(speed: Int) { putInt(ConfigManager.KEY_CLOUD_DYNAMICS_SPEED, speed.coerceIn(0, 100)) }

    fun isLightningFlashEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_LIGHTNING_FLASH_ENABLED, ConfigManager.DEFAULT_LIGHTNING_FLASH_ENABLED)
    fun setLightningFlashEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_LIGHTNING_FLASH_ENABLED, enabled) }

    fun isCloudFlashEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_CLOUD_FLASH_ENABLED, ConfigManager.DEFAULT_CLOUD_FLASH_ENABLED)
    fun setCloudFlashEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_CLOUD_FLASH_ENABLED, enabled) }

    fun isInteractiveLightningEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_INTERACTIVE_LIGHTNING_ENABLED, ConfigManager.DEFAULT_INTERACTIVE_LIGHTNING_ENABLED)
    fun setInteractiveLightningEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_INTERACTIVE_LIGHTNING_ENABLED, enabled) }

    fun isScreenDropletsEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_SCREEN_DROPLETS_ENABLED, ConfigManager.DEFAULT_SCREEN_DROPLETS_ENABLED)
    fun setScreenDropletsEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_SCREEN_DROPLETS_ENABLED, enabled) }

    fun getScreenDropletsSize(): Int = cachedInt(ConfigManager.KEY_SCREEN_DROPLETS_SIZE, ConfigManager.DEFAULT_SCREEN_DROPLETS_SIZE)
    fun setScreenDropletsSize(size: Int) { putInt(ConfigManager.KEY_SCREEN_DROPLETS_SIZE, size.coerceIn(10, 200)) }

    fun getRainSpawnMode(): Int = cachedInt(ConfigManager.KEY_RAIN_SPAWN_MODE, ConfigManager.DEFAULT_RAIN_SPAWN_MODE)
    fun setRainSpawnMode(mode: Int) { putInt(ConfigManager.KEY_RAIN_SPAWN_MODE, mode.coerceIn(0, 2)) }

    fun isWindLinesEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_WIND_LINES_ENABLED, ConfigManager.DEFAULT_WIND_LINES_ENABLED)
    fun setWindLinesEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_WIND_LINES_ENABLED, enabled) }

    fun getWindLinesIntensity(): Int = cachedInt(ConfigManager.KEY_WIND_LINES_INTENSITY, ConfigManager.DEFAULT_WIND_LINES_INTENSITY)
    fun setWindLinesIntensity(intensity: Int) { putInt(ConfigManager.KEY_WIND_LINES_INTENSITY, intensity.coerceIn(0, 100)) }

    fun isStormVariableEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_STORM_VARIABLE_ENABLED, ConfigManager.DEFAULT_STORM_VARIABLE_ENABLED)
    fun setStormVariableEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_STORM_VARIABLE_ENABLED, enabled) }

    fun getLightningSpawnMode(): Int = cachedInt(ConfigManager.KEY_LIGHTNING_SPAWN_MODE, ConfigManager.DEFAULT_LIGHTNING_SPAWN_MODE)
    fun setLightningSpawnMode(mode: Int) { putInt(ConfigManager.KEY_LIGHTNING_SPAWN_MODE, mode.coerceIn(0, 2)) }
}
