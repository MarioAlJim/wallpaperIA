package com.wolf.wallpaper.core

import android.content.SharedPreferences

/** Combined day+night mode preferences. */
internal class CombinedConfigStore(prefs: SharedPreferences) : CachedPrefStore(prefs) {
    fun getCombinedSunSize(): Int = cachedInt(ConfigManager.KEY_COMBINED_SUN_SIZE, ConfigManager.DEFAULT_COMBINED_SUN_SIZE)
    fun setCombinedSunSize(size: Int) { putInt(ConfigManager.KEY_COMBINED_SUN_SIZE, size.coerceIn(0, 100)) }

    fun getCombinedMoonSize(): Int = cachedInt(ConfigManager.KEY_COMBINED_MOON_SIZE, ConfigManager.DEFAULT_COMBINED_MOON_SIZE)
    fun setCombinedMoonSize(size: Int) { putInt(ConfigManager.KEY_COMBINED_MOON_SIZE, size.coerceIn(0, 100)) }

    fun getCombinedMoonPhase(): Int = cachedInt(ConfigManager.KEY_COMBINED_MOON_PHASE, ConfigManager.DEFAULT_COMBINED_MOON_PHASE)
    fun setCombinedMoonPhase(phase: Int) { putInt(ConfigManager.KEY_COMBINED_MOON_PHASE, phase.coerceIn(0, 7)) }

    fun getCombinedStarDensity(): Int = cachedInt(ConfigManager.KEY_COMBINED_STAR_DENSITY, ConfigManager.DEFAULT_COMBINED_STAR_DENSITY)
    fun setCombinedStarDensity(density: Int) { putInt(ConfigManager.KEY_COMBINED_STAR_DENSITY, density.coerceIn(0, 100)) }

    fun getCombinedStarColorIndex(): Int = cachedInt(ConfigManager.KEY_COMBINED_STAR_COLOR_INDEX, ConfigManager.DEFAULT_COMBINED_STAR_COLOR_INDEX)
    fun setCombinedStarColorIndex(index: Int) { putInt(ConfigManager.KEY_COMBINED_STAR_COLOR_INDEX, index.coerceIn(0, 5)) }

    fun getCombinedPathDirection(): Int = cachedInt(ConfigManager.KEY_COMBINED_PATH_DIRECTION, ConfigManager.DEFAULT_COMBINED_PATH_DIRECTION)
    fun setCombinedPathDirection(dir: Int) { putInt(ConfigManager.KEY_COMBINED_PATH_DIRECTION, dir.coerceIn(0, 3)) }

    fun isCombinedMoonPhaseAutoAdvanceEnabled(): Boolean = cachedBoolean(ConfigManager.KEY_COMBINED_MOON_PHASE_AUTO_ADVANCE, ConfigManager.DEFAULT_COMBINED_MOON_PHASE_AUTO_ADVANCE)
    fun setCombinedMoonPhaseAutoAdvanceEnabled(enabled: Boolean) { putBoolean(ConfigManager.KEY_COMBINED_MOON_PHASE_AUTO_ADVANCE, enabled) }
}
