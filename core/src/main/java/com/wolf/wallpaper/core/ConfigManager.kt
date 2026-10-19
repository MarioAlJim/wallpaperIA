package com.wolf.wallpaper.core

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) : ConfigProvider {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val storm = StormConfigStore(prefs)

    private val prefsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null) {
            storm.invalidate(key)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)
    }

    companion object {
        const val PREFS_NAME = "storm_wallpaper_prefs"
        const val KEY_CLOUD_DENSITY = "cloud_density"
        const val KEY_RAIN_INTENSITY = "rain_intensity"
        const val KEY_LIGHTNING_FREQUENCY = "lightning_frequency"
        const val KEY_WIND_DIRECTION = "wind_direction"
        const val KEY_RAIN_COLOR_INDEX = "rain_color_index"
        const val KEY_WIND_INTENSITY = "wind_intensity"
        const val KEY_RAIN_SPEED = "rain_speed"
        const val KEY_LIGHTNING_COLOR_INDEX = "lightning_color_index"
        const val KEY_LIGHTNING_DURATION = "lightning_duration"
        const val KEY_BACKGROUND_INDEX = "background_index"
        const val KEY_CLOUD_FLASH_FREQUENCY = "cloud_flash_frequency"
        const val KEY_CLOUD_FLASH_COLOR_INDEX = "cloud_flash_color_index"
        const val KEY_CLOUD_DYNAMICS_SPEED = "cloud_dynamics_speed"
        const val KEY_LIGHTNING_FLASH_ENABLED = "lightning_flash_enabled"
        const val KEY_CLOUD_FLASH_ENABLED = "cloud_flash_enabled"
        const val KEY_INTERACTIVE_LIGHTNING_ENABLED = "interactive_lightning_enabled"
        const val KEY_BG_BRIGHTNESS = "bg_brightness"

        const val KEY_SCREEN_DROPLETS_ENABLED = "screen_droplets_enabled"
        const val KEY_SCREEN_DROPLETS_SIZE = "screen_droplets_size"
        const val KEY_RAIN_SPAWN_MODE = "rain_spawn_mode"
        const val KEY_WIND_LINES_ENABLED = "wind_lines_enabled"
        const val KEY_WIND_LINES_INTENSITY = "wind_lines_intensity"

        const val KEY_SUNNY_GYRO_ENABLED = "sunny_gyro_enabled"
        const val DEFAULT_SUNNY_GYRO_ENABLED = true

        const val DEFAULT_CLOUD_DENSITY = 50
        const val DEFAULT_RAIN_INTENSITY = 50
        const val DEFAULT_LIGHTNING_FREQUENCY = 30
        const val DEFAULT_WIND_DIRECTION = 0
        const val DEFAULT_RAIN_COLOR_INDEX = 0
        const val DEFAULT_WIND_INTENSITY = 30
        const val DEFAULT_RAIN_SPEED = 50
        const val DEFAULT_LIGHTNING_COLOR_INDEX = 0
        const val DEFAULT_LIGHTNING_DURATION = 50
        const val DEFAULT_BACKGROUND_INDEX = 0
        const val DEFAULT_CLOUD_FLASH_FREQUENCY = 50
        const val DEFAULT_CLOUD_FLASH_COLOR_INDEX = 0
        const val DEFAULT_CLOUD_DYNAMICS_SPEED = 100
        const val DEFAULT_LIGHTNING_FLASH_ENABLED = true
        const val DEFAULT_CLOUD_FLASH_ENABLED = true
        const val DEFAULT_INTERACTIVE_LIGHTNING_ENABLED = true
        const val DEFAULT_BG_BRIGHTNESS = 20

        const val DEFAULT_SCREEN_DROPLETS_ENABLED = true
        const val DEFAULT_SCREEN_DROPLETS_SIZE = 50
        const val DEFAULT_RAIN_SPAWN_MODE = 2
        const val DEFAULT_WIND_LINES_ENABLED = true
        const val DEFAULT_WIND_LINES_INTENSITY = 50

        // Storm Variable
        const val KEY_STORM_VARIABLE_ENABLED = "storm_variable_enabled"
        const val DEFAULT_STORM_VARIABLE_ENABLED = false

        // Lightning Spawn Mode
        const val KEY_LIGHTNING_SPAWN_MODE = "lightning_spawn_mode"
        const val DEFAULT_LIGHTNING_SPAWN_MODE = 0
    }

    // Storm mode
    override fun getCloudDensity(): Int = storm.getCloudDensity()
    fun setCloudDensity(density: Int) = storm.setCloudDensity(density)

    override fun getRainIntensity(): Int = storm.getRainIntensity()
    fun setRainIntensity(intensity: Int) = storm.setRainIntensity(intensity)

    override fun getLightningFrequency(): Int = storm.getLightningFrequency()
    fun setLightningFrequency(frequency: Int) = storm.setLightningFrequency(frequency)

    override fun getWindDirection(): Int = storm.getWindDirection()
    fun setWindDirection(direction: Int) = storm.setWindDirection(direction)

    override fun getRainColorIndex(): Int = storm.getRainColorIndex()
    fun setRainColorIndex(index: Int) = storm.setRainColorIndex(index)

    override fun getWindIntensity(): Int = storm.getWindIntensity()
    fun setWindIntensity(intensity: Int) = storm.setWindIntensity(intensity)

    override fun getRainSpeed(): Int = storm.getRainSpeed()
    fun setRainSpeed(speed: Int) = storm.setRainSpeed(speed)

    override fun getLightningColorIndex(): Int = storm.getLightningColorIndex()
    fun setLightningColorIndex(index: Int) = storm.setLightningColorIndex(index)

    override fun getLightningDuration(): Int = storm.getLightningDuration()
    fun setLightningDuration(duration: Int) = storm.setLightningDuration(duration)

    override fun getBackgroundIndex(): Int = storm.getBackgroundIndex()
    fun setBackgroundIndex(index: Int) = storm.setBackgroundIndex(index)

    override fun getCloudFlashFrequency(): Int = storm.getCloudFlashFrequency()
    fun setCloudFlashFrequency(frequency: Int) = storm.setCloudFlashFrequency(frequency)

    override fun getCloudFlashColorIndex(): Int = storm.getCloudFlashColorIndex()
    fun setCloudFlashColorIndex(index: Int) = storm.setCloudFlashColorIndex(index)

    override fun getCloudDynamicsSpeed(): Int = storm.getCloudDynamicsSpeed()
    fun setCloudDynamicsSpeed(speed: Int) = storm.setCloudDynamicsSpeed(speed)

    override fun isLightningFlashEnabled(): Boolean = storm.isLightningFlashEnabled()
    fun setLightningFlashEnabled(enabled: Boolean) = storm.setLightningFlashEnabled(enabled)

    override fun isCloudFlashEnabled(): Boolean = storm.isCloudFlashEnabled()
    fun setCloudFlashEnabled(enabled: Boolean) = storm.setCloudFlashEnabled(enabled)

    override fun isInteractiveLightningEnabled(): Boolean = storm.isInteractiveLightningEnabled()
    fun setInteractiveLightningEnabled(enabled: Boolean) = storm.setInteractiveLightningEnabled(enabled)

    override fun isScreenDropletsEnabled(): Boolean = storm.isScreenDropletsEnabled()
    fun setScreenDropletsEnabled(enabled: Boolean) = storm.setScreenDropletsEnabled(enabled)

    override fun getScreenDropletsSize(): Int = storm.getScreenDropletsSize()
    fun setScreenDropletsSize(size: Int) = storm.setScreenDropletsSize(size)

    override fun getRainSpawnMode(): Int = storm.getRainSpawnMode()
    fun setRainSpawnMode(mode: Int) = storm.setRainSpawnMode(mode)

    override fun isWindLinesEnabled(): Boolean = storm.isWindLinesEnabled()
    fun setWindLinesEnabled(enabled: Boolean) = storm.setWindLinesEnabled(enabled)

    override fun getWindLinesIntensity(): Int = storm.getWindLinesIntensity()
    fun setWindLinesIntensity(intensity: Int) = storm.setWindLinesIntensity(intensity)

    override fun isStormVariableEnabled(): Boolean = storm.isStormVariableEnabled()
    fun setStormVariableEnabled(enabled: Boolean) = storm.setStormVariableEnabled(enabled)

    override fun getLightningSpawnMode(): Int = storm.getLightningSpawnMode()
    fun setLightningSpawnMode(mode: Int) = storm.setLightningSpawnMode(mode)

    override fun getBgBrightness(): Int = storm.getBgBrightness()
    fun setBgBrightness(brightness: Int) = storm.setBgBrightness(brightness)

    override fun isSunnyGyroEnabled(): Boolean = prefs.getBoolean(KEY_SUNNY_GYRO_ENABLED, DEFAULT_SUNNY_GYRO_ENABLED)
    fun setSunnyGyroEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_SUNNY_GYRO_ENABLED, enabled).apply() }

    override fun getActiveEffect(): Int = 0
}
