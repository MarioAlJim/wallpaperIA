package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Night-mode (moon, stars, time-of-day) preferences. */
internal class NightConfigStore(private val prefs: SharedPreferences) {
    fun getTimeMode(): Int = prefs.getInt(ConfigManager.KEY_TIME_MODE, ConfigManager.DEFAULT_TIME_MODE)
    fun setTimeMode(mode: Int) { prefs.edit().putInt(ConfigManager.KEY_TIME_MODE, mode.coerceIn(0, 2)).apply() }

    fun getMoonPhase(): Int = prefs.getInt(ConfigManager.KEY_MOON_PHASE, ConfigManager.DEFAULT_MOON_PHASE)
    fun setMoonPhase(phase: Int) { prefs.edit().putInt(ConfigManager.KEY_MOON_PHASE, phase.coerceIn(0, 7)).apply() }

    fun getMoonPathDirection(): Int = prefs.getInt(ConfigManager.KEY_MOON_PATH_DIRECTION, ConfigManager.DEFAULT_MOON_PATH_DIRECTION)
    fun setMoonPathDirection(dir: Int) { prefs.edit().putInt(ConfigManager.KEY_MOON_PATH_DIRECTION, dir.coerceIn(0, 3)).apply() }

    fun getMoonMoveSpeed(): Int = prefs.getInt(ConfigManager.KEY_MOON_MOVE_SPEED, ConfigManager.DEFAULT_MOON_MOVE_SPEED)
    fun setMoonMoveSpeed(speed: Int) { prefs.edit().putInt(ConfigManager.KEY_MOON_MOVE_SPEED, speed.coerceIn(0, 100)).apply() }

    fun getMoonStationaryPosition(): Int = prefs.getInt(ConfigManager.KEY_MOON_STATIONARY_POSITION, ConfigManager.DEFAULT_MOON_STATIONARY_POSITION)
    fun setMoonStationaryPosition(pos: Int) { prefs.edit().putInt(ConfigManager.KEY_MOON_STATIONARY_POSITION, pos.coerceIn(0, 5)).apply() }

    fun getStarColorIndex(): Int = prefs.getInt(ConfigManager.KEY_STAR_COLOR_INDEX, ConfigManager.DEFAULT_STAR_COLOR_INDEX)
    fun setStarColorIndex(index: Int) { prefs.edit().putInt(ConfigManager.KEY_STAR_COLOR_INDEX, index.coerceIn(0, 5)).apply() }

    fun getStarDensity(): Int = prefs.getInt(ConfigManager.KEY_STAR_DENSITY, ConfigManager.DEFAULT_STAR_DENSITY)
    fun setStarDensity(density: Int) { prefs.edit().putInt(ConfigManager.KEY_STAR_DENSITY, density.coerceIn(0, 100)).apply() }

    fun getStarMode(): Int = prefs.getInt(ConfigManager.KEY_STAR_MODE, ConfigManager.DEFAULT_STAR_MODE)
    fun setStarMode(mode: Int) { prefs.edit().putInt(ConfigManager.KEY_STAR_MODE, mode.coerceIn(0, 1)).apply() }

    fun getNightCloudDensity(): Int = prefs.getInt(ConfigManager.KEY_NIGHT_CLOUD_DENSITY, ConfigManager.DEFAULT_NIGHT_CLOUD_DENSITY)
    fun setNightCloudDensity(density: Int) { prefs.edit().putInt(ConfigManager.KEY_NIGHT_CLOUD_DENSITY, density.coerceIn(0, 100)).apply() }

    fun getGradientCycleSpeed(): Int = prefs.getInt(ConfigManager.KEY_GRADIENT_CYCLE_SPEED, ConfigManager.DEFAULT_GRADIENT_CYCLE_SPEED)
    fun setGradientCycleSpeed(speed: Int) { prefs.edit().putInt(ConfigManager.KEY_GRADIENT_CYCLE_SPEED, speed.coerceIn(0, 100)).apply() }

    fun isShootingStarsEnabled(): Boolean = prefs.getBoolean(ConfigManager.KEY_SHOOTING_STARS_ENABLED, ConfigManager.DEFAULT_SHOOTING_STARS_ENABLED)
    fun setShootingStarsEnabled(enabled: Boolean) { prefs.edit().putBoolean(ConfigManager.KEY_SHOOTING_STARS_ENABLED, enabled).apply() }

    fun getShootingStarsDensity(): Int = prefs.getInt(ConfigManager.KEY_SHOOTING_STARS_DENSITY, ConfigManager.DEFAULT_SHOOTING_STARS_DENSITY)
    fun setShootingStarsDensity(density: Int) { prefs.edit().putInt(ConfigManager.KEY_SHOOTING_STARS_DENSITY, density.coerceIn(0, 100)).apply() }
}
