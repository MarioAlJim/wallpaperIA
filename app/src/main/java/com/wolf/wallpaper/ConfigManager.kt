package com.wolf.wallpaper

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
        const val KEY_SHOW_BACKGROUND = "show_background"
        
        const val DEFAULT_CLOUD_DENSITY = 50
        const val DEFAULT_RAIN_INTENSITY = 50
        const val DEFAULT_LIGHTNING_FREQUENCY = 50
        const val DEFAULT_WIND_DIRECTION = 0
        const val DEFAULT_RAIN_COLOR_INDEX = 0
        const val DEFAULT_WIND_INTENSITY = 50
        const val DEFAULT_RAIN_SPEED = 50
        const val DEFAULT_LIGHTNING_COLOR_INDEX = 0
        const val DEFAULT_LIGHTNING_DURATION = 30
        const val DEFAULT_SHOW_BACKGROUND = true
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

    override fun getShowBackground(): Boolean {
        return prefs.getBoolean(KEY_SHOW_BACKGROUND, DEFAULT_SHOW_BACKGROUND)
    }

    fun setShowBackground(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BACKGROUND, show).apply()
    }
}
