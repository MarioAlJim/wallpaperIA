package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Night-mode (moon, stars, time-of-day) preferences. */
internal class NightConfigStore(prefs: SharedPreferences) : CachedPrefStore(prefs) {
    fun getTimeMode(): Int = cachedInt(ConfigManager.KEY_TIME_MODE, ConfigManager.DEFAULT_TIME_MODE)
    fun setTimeMode(mode: Int) { putInt(ConfigManager.KEY_TIME_MODE, mode.coerceIn(0, 2)) }

    fun getMoonPhase(): Int = cachedInt(ConfigManager.KEY_MOON_PHASE, ConfigManager.DEFAULT_MOON_PHASE)
    fun setMoonPhase(phase: Int) { putInt(ConfigManager.KEY_MOON_PHASE, phase.coerceIn(0, 7)) }

    fun getMoonPathDirection(): Int = cachedInt(ConfigManager.KEY_MOON_PATH_DIRECTION, ConfigManager.DEFAULT_MOON_PATH_DIRECTION)
    fun setMoonPathDirection(dir: Int) { putInt(ConfigManager.KEY_MOON_PATH_DIRECTION, dir.coerceIn(0, 4)) }

    fun getMoonMoveSpeed(): Int = cachedInt(ConfigManager.KEY_MOON_MOVE_SPEED, ConfigManager.DEFAULT_MOON_MOVE_SPEED)
    fun setMoonMoveSpeed(speed: Int) { putInt(ConfigManager.KEY_MOON_MOVE_SPEED, speed.coerceIn(0, 100)) }

    fun getMoonStationaryPosition(): Int = cachedInt(ConfigManager.KEY_MOON_STATIONARY_POSITION, ConfigManager.DEFAULT_MOON_STATIONARY_POSITION)
    fun setMoonStationaryPosition(pos: Int) { putInt(ConfigManager.KEY_MOON_STATIONARY_POSITION, pos.coerceIn(0, 5)) }

    fun getStarColorIndex(): Int = cachedInt(ConfigManager.KEY_STAR_COLOR_INDEX, ConfigManager.DEFAULT_STAR_COLOR_INDEX)
    fun setStarColorIndex(index: Int) { putInt(ConfigManager.KEY_STAR_COLOR_INDEX, index.coerceIn(0, 5)) }

    fun getStarDensity(): Int = cachedInt(ConfigManager.KEY_STAR_DENSITY, ConfigManager.DEFAULT_STAR_DENSITY)
    fun setStarDensity(density: Int) { putInt(ConfigManager.KEY_STAR_DENSITY, density.coerceIn(0, 100)) }

    fun getStarMode(): Int = cachedInt(ConfigManager.KEY_STAR_MODE, ConfigManager.DEFAULT_STAR_MODE)
    fun setStarMode(mode: Int) { putInt(ConfigManager.KEY_STAR_MODE, mode.coerceIn(0, 1)) }

    fun getNightCloudDensity(): Int = cachedInt(ConfigManager.KEY_NIGHT_CLOUD_DENSITY, ConfigManager.DEFAULT_NIGHT_CLOUD_DENSITY)
    fun setNightCloudDensity(density: Int) { putInt(ConfigManager.KEY_NIGHT_CLOUD_DENSITY, density.coerceIn(0, 100)) }

    fun getGradientCycleSpeed(): Int = cachedInt(ConfigManager.KEY_GRADIENT_CYCLE_SPEED, ConfigManager.DEFAULT_GRADIENT_CYCLE_SPEED)
    fun setGradientCycleSpeed(speed: Int) { putInt(ConfigManager.KEY_GRADIENT_CYCLE_SPEED, speed.coerceIn(0, 100)) }

    fun isShootingStarsEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_SHOOTING_STARS_ENABLED, ConfigManager.DEFAULT_SHOOTING_STARS_ENABLED)
    fun setShootingStarsEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_SHOOTING_STARS_ENABLED, enabled) }

    fun getShootingStarsDensity(): Int = cachedInt(ConfigManager.KEY_SHOOTING_STARS_DENSITY, ConfigManager.DEFAULT_SHOOTING_STARS_DENSITY)
    fun setShootingStarsDensity(density: Int) { putInt(ConfigManager.KEY_SHOOTING_STARS_DENSITY, density.coerceIn(0, 100)) }
}
