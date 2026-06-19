package com.wolf.wallpaper.core

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) : ConfigProvider {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        
        // New keys
        const val KEY_ACTIVE_EFFECT = "active_effect"
        const val KEY_SUN_SIZE = "sun_size"
        const val KEY_SUN_SPEED = "sun_speed"
        const val KEY_SUNNY_THEME = "sunny_theme"
        const val KEY_SUN_PATH_DIRECTION = "sun_path_direction"
        const val KEY_SUN_MOVE_SPEED = "sun_move_speed"
        const val KEY_SUNNY_BACKGROUND_INDEX = "sunny_background_index"
        const val KEY_SUN_STATIONARY_POSITION = "sun_stationary_position"
        const val KEY_SUN_CUSTOM_X = "sun_custom_x"
        const val KEY_SUN_CUSTOM_Y = "sun_custom_y"
        
        const val DEFAULT_CLOUD_DENSITY = 50
        const val DEFAULT_RAIN_INTENSITY = 50
        const val DEFAULT_LIGHTNING_FREQUENCY = 50
        const val DEFAULT_WIND_DIRECTION = 0
        const val DEFAULT_RAIN_COLOR_INDEX = 0
        const val DEFAULT_WIND_INTENSITY = 50
        const val DEFAULT_RAIN_SPEED = 50
        const val DEFAULT_LIGHTNING_COLOR_INDEX = 0
        const val DEFAULT_LIGHTNING_DURATION = 30
        const val DEFAULT_BACKGROUND_INDEX = 1
        const val DEFAULT_CLOUD_FLASH_FREQUENCY = 50
        const val DEFAULT_CLOUD_FLASH_COLOR_INDEX = 0
        const val DEFAULT_CLOUD_DYNAMICS_SPEED = 100
        const val DEFAULT_LIGHTNING_FLASH_ENABLED = true
        const val DEFAULT_CLOUD_FLASH_ENABLED = true
        const val DEFAULT_INTERACTIVE_LIGHTNING_ENABLED = true
        
        // New defaults
        const val DEFAULT_ACTIVE_EFFECT = 0
        const val DEFAULT_SUN_SIZE = 50
        const val DEFAULT_SUN_SPEED = 50
        const val DEFAULT_SUNNY_THEME = 0
        const val DEFAULT_SUN_PATH_DIRECTION = 0
        const val DEFAULT_SUN_MOVE_SPEED = 50
        const val DEFAULT_SUNNY_BACKGROUND_INDEX = 0
        const val DEFAULT_SUN_STATIONARY_POSITION = 2
        const val DEFAULT_SUN_CUSTOM_X = 50
        const val DEFAULT_SUN_CUSTOM_Y = 74
    }

    override fun getCloudDensity(): Int {
        return prefs.getInt(KEY_CLOUD_DENSITY, DEFAULT_CLOUD_DENSITY)
    }

    override fun getRainIntensity(): Int {
        return prefs.getInt(KEY_RAIN_INTENSITY, DEFAULT_RAIN_INTENSITY)
    }

    override fun getLightningFrequency(): Int {
        return prefs.getInt(KEY_LIGHTNING_FREQUENCY, DEFAULT_LIGHTNING_FREQUENCY)
    }

    override fun getWindDirection(): Int {
        return prefs.getInt(KEY_WIND_DIRECTION, DEFAULT_WIND_DIRECTION)
    }

    override fun getRainColorIndex(): Int {
        return prefs.getInt(KEY_RAIN_COLOR_INDEX, DEFAULT_RAIN_COLOR_INDEX)
    }

    override fun getWindIntensity(): Int {
        return prefs.getInt(KEY_WIND_INTENSITY, DEFAULT_WIND_INTENSITY)
    }

    override fun getRainSpeed(): Int {
        return prefs.getInt(KEY_RAIN_SPEED, DEFAULT_RAIN_SPEED)
    }

    fun setCloudDensity(density: Int) {
        prefs.edit().putInt(KEY_CLOUD_DENSITY, density.coerceIn(0, 100)).apply()
    }

    fun setRainIntensity(intensity: Int) {
        prefs.edit().putInt(KEY_RAIN_INTENSITY, intensity.coerceIn(0, 100)).apply()
    }

    fun setLightningFrequency(frequency: Int) {
        prefs.edit().putInt(KEY_LIGHTNING_FREQUENCY, frequency.coerceIn(0, 100)).apply()
    }

    fun setWindDirection(direction: Int) {
        prefs.edit().putInt(KEY_WIND_DIRECTION, direction.coerceIn(0, 2)).apply()
    }

    fun setRainColorIndex(index: Int) {
        prefs.edit().putInt(KEY_RAIN_COLOR_INDEX, index.coerceIn(0, 5)).apply()
    }

    fun setWindIntensity(intensity: Int) {
        prefs.edit().putInt(KEY_WIND_INTENSITY, intensity.coerceIn(0, 100)).apply()
    }

    fun setRainSpeed(speed: Int) {
        prefs.edit().putInt(KEY_RAIN_SPEED, speed.coerceIn(0, 100)).apply()
    }

    override fun getLightningColorIndex(): Int {
        return prefs.getInt(KEY_LIGHTNING_COLOR_INDEX, DEFAULT_LIGHTNING_COLOR_INDEX)
    }

    fun setLightningColorIndex(index: Int) {
        prefs.edit().putInt(KEY_LIGHTNING_COLOR_INDEX, index.coerceIn(0, 6)).apply()
    }

    override fun getLightningDuration(): Int {
        return prefs.getInt(KEY_LIGHTNING_DURATION, DEFAULT_LIGHTNING_DURATION)
    }

    fun setLightningDuration(duration: Int) {
        prefs.edit().putInt(KEY_LIGHTNING_DURATION, duration.coerceIn(0, 100)).apply()
    }

    override fun getBackgroundIndex(): Int {
        return prefs.getInt(KEY_BACKGROUND_INDEX, DEFAULT_BACKGROUND_INDEX)
    }

    fun setBackgroundIndex(index: Int) {
        prefs.edit().putInt(KEY_BACKGROUND_INDEX, index.coerceIn(0, 8)).apply()
    }

    override fun getCloudFlashFrequency(): Int {
        return prefs.getInt(KEY_CLOUD_FLASH_FREQUENCY, DEFAULT_CLOUD_FLASH_FREQUENCY)
    }

    fun setCloudFlashFrequency(frequency: Int) {
        prefs.edit().putInt(KEY_CLOUD_FLASH_FREQUENCY, frequency.coerceIn(0, 100)).apply()
    }

    override fun getCloudFlashColorIndex(): Int {
        return prefs.getInt(KEY_CLOUD_FLASH_COLOR_INDEX, DEFAULT_CLOUD_FLASH_COLOR_INDEX)
    }

    fun setCloudFlashColorIndex(index: Int) {
        prefs.edit().putInt(KEY_CLOUD_FLASH_COLOR_INDEX, index.coerceIn(0, 6)).apply()
    }

    override fun getCloudDynamicsSpeed(): Int {
        return prefs.getInt(KEY_CLOUD_DYNAMICS_SPEED, DEFAULT_CLOUD_DYNAMICS_SPEED)
    }

    fun setCloudDynamicsSpeed(speed: Int) {
        prefs.edit().putInt(KEY_CLOUD_DYNAMICS_SPEED, speed.coerceIn(0, 100)).apply()
    }

    override fun isLightningFlashEnabled(): Boolean {
        return prefs.getBoolean(KEY_LIGHTNING_FLASH_ENABLED, DEFAULT_LIGHTNING_FLASH_ENABLED)
    }

    fun setLightningFlashEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LIGHTNING_FLASH_ENABLED, enabled).apply()
    }

    override fun isCloudFlashEnabled(): Boolean {
        return prefs.getBoolean(KEY_CLOUD_FLASH_ENABLED, DEFAULT_CLOUD_FLASH_ENABLED)
    }

    fun setCloudFlashEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_FLASH_ENABLED, enabled).apply()
    }

    override fun isInteractiveLightningEnabled(): Boolean {
        return prefs.getBoolean(KEY_INTERACTIVE_LIGHTNING_ENABLED, DEFAULT_INTERACTIVE_LIGHTNING_ENABLED)
    }

    fun setInteractiveLightningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTERACTIVE_LIGHTNING_ENABLED, enabled).apply()
    }

    // New configuration methods
    override fun getActiveEffect(): Int {
        return prefs.getInt(KEY_ACTIVE_EFFECT, DEFAULT_ACTIVE_EFFECT)
    }

    fun setActiveEffect(effect: Int) {
        prefs.edit().putInt(KEY_ACTIVE_EFFECT, effect.coerceIn(0, 1)).apply()
    }

    override fun getSunSize(): Int {
        return prefs.getInt(KEY_SUN_SIZE, DEFAULT_SUN_SIZE)
    }

    fun setSunSize(size: Int) {
        prefs.edit().putInt(KEY_SUN_SIZE, size.coerceIn(0, 100)).apply()
    }

    override fun getSunSpeed(): Int {
        return prefs.getInt(KEY_SUN_SPEED, DEFAULT_SUN_SPEED)
    }

    fun setSunSpeed(speed: Int) {
        prefs.edit().putInt(KEY_SUN_SPEED, speed.coerceIn(0, 100)).apply()
    }

    override fun getSunnyTheme(): Int {
        return prefs.getInt(KEY_SUNNY_THEME, DEFAULT_SUNNY_THEME)
    }

    fun setSunnyTheme(theme: Int) {
        prefs.edit().putInt(KEY_SUNNY_THEME, theme.coerceIn(0, 2)).apply()
    }

    override fun getSunPathDirection(): Int {
        return prefs.getInt(KEY_SUN_PATH_DIRECTION, DEFAULT_SUN_PATH_DIRECTION)
    }

    fun setSunPathDirection(direction: Int) {
        prefs.edit().putInt(KEY_SUN_PATH_DIRECTION, direction.coerceIn(0, 2)).apply()
    }

    override fun getSunMoveSpeed(): Int {
        return prefs.getInt(KEY_SUN_MOVE_SPEED, DEFAULT_SUN_MOVE_SPEED)
    }

    fun setSunMoveSpeed(speed: Int) {
        prefs.edit().putInt(KEY_SUN_MOVE_SPEED, speed.coerceIn(0, 100)).apply()
    }

    override fun getSunnyBackgroundIndex(): Int {
        return prefs.getInt(KEY_SUNNY_BACKGROUND_INDEX, DEFAULT_SUNNY_BACKGROUND_INDEX)
    }

    fun setSunnyBackgroundIndex(index: Int) {
        prefs.edit().putInt(KEY_SUNNY_BACKGROUND_INDEX, index.coerceIn(0, 7)).apply()
    }

    override fun getSunStationaryPosition(): Int {
        return prefs.getInt(KEY_SUN_STATIONARY_POSITION, DEFAULT_SUN_STATIONARY_POSITION)
    }

    fun setSunStationaryPosition(position: Int) {
        prefs.edit().putInt(KEY_SUN_STATIONARY_POSITION, position.coerceIn(0, 5)).apply()
    }

    override fun getSunCustomX(): Int {
        return prefs.getInt(KEY_SUN_CUSTOM_X, DEFAULT_SUN_CUSTOM_X)
    }

    fun setSunCustomX(x: Int) {
        prefs.edit().putInt(KEY_SUN_CUSTOM_X, x.coerceIn(0, 100)).apply()
    }

    override fun getSunCustomY(): Int {
        return prefs.getInt(KEY_SUN_CUSTOM_Y, DEFAULT_SUN_CUSTOM_Y)
    }

    fun setSunCustomY(y: Int) {
        prefs.edit().putInt(KEY_SUN_CUSTOM_Y, y.coerceIn(0, 100)).apply()
    }
}
