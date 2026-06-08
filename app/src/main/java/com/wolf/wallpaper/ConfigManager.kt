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
        
        const val DEFAULT_CLOUD_DENSITY = 50
        const val DEFAULT_RAIN_INTENSITY = 50
        const val DEFAULT_LIGHTNING_FREQUENCY = 50
        const val DEFAULT_WIND_DIRECTION = 0
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
}
