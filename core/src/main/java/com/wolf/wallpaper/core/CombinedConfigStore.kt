package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Combined day+night mode preferences. */
internal class CombinedConfigStore(private val prefs: SharedPreferences) {
    fun getCombinedSunSize(): Int = prefs.getInt(ConfigManager.KEY_COMBINED_SUN_SIZE, ConfigManager.DEFAULT_COMBINED_SUN_SIZE)
    fun setCombinedSunSize(size: Int) { prefs.edit().putInt(ConfigManager.KEY_COMBINED_SUN_SIZE, size.coerceIn(0, 100)).apply() }

    fun getCombinedMoonSize(): Int = prefs.getInt(ConfigManager.KEY_COMBINED_MOON_SIZE, ConfigManager.DEFAULT_COMBINED_MOON_SIZE)
    fun setCombinedMoonSize(size: Int) { prefs.edit().putInt(ConfigManager.KEY_COMBINED_MOON_SIZE, size.coerceIn(0, 100)).apply() }

    fun getCombinedMoonPhase(): Int = prefs.getInt(ConfigManager.KEY_COMBINED_MOON_PHASE, ConfigManager.DEFAULT_COMBINED_MOON_PHASE)
    fun setCombinedMoonPhase(phase: Int) { prefs.edit().putInt(ConfigManager.KEY_COMBINED_MOON_PHASE, phase.coerceIn(0, 7)).apply() }

    fun getCombinedStarDensity(): Int = prefs.getInt(ConfigManager.KEY_COMBINED_STAR_DENSITY, ConfigManager.DEFAULT_COMBINED_STAR_DENSITY)
    fun setCombinedStarDensity(density: Int) { prefs.edit().putInt(ConfigManager.KEY_COMBINED_STAR_DENSITY, density.coerceIn(0, 100)).apply() }

    fun getCombinedStarColorIndex(): Int = prefs.getInt(ConfigManager.KEY_COMBINED_STAR_COLOR_INDEX, ConfigManager.DEFAULT_COMBINED_STAR_COLOR_INDEX)
    fun setCombinedStarColorIndex(index: Int) { prefs.edit().putInt(ConfigManager.KEY_COMBINED_STAR_COLOR_INDEX, index.coerceIn(0, 5)).apply() }

    fun getCombinedPathDirection(): Int = prefs.getInt(ConfigManager.KEY_COMBINED_PATH_DIRECTION, ConfigManager.DEFAULT_COMBINED_PATH_DIRECTION)
    fun setCombinedPathDirection(dir: Int) { prefs.edit().putInt(ConfigManager.KEY_COMBINED_PATH_DIRECTION, dir.coerceIn(0, 3)).apply() }
}
