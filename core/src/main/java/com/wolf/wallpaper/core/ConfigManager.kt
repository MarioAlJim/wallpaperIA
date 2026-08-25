package com.wolf.wallpaper.core

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) : ConfigProvider {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val storm = StormConfigStore(prefs)
    private val sunny = SunnyConfigStore(prefs)
    private val night = NightConfigStore(prefs)
    private val combined = CombinedConfigStore(prefs)

    // Each store memoizes its reads (see CachedPrefStore) so the render thread doesn't
    // hit SharedPreferences on every frame. Settings can be changed from a different
    // ConfigManager instance than the one the wallpaper engine is polling (the settings
    // screen and the wallpaper service each hold their own), so we listen for changes on
    // the underlying prefs object (shared per-process by Android) and drop the stale
    // cached value wherever it lives. A strong reference to the listener is kept here
    // because SharedPreferences only holds a weak reference to registered listeners.
    private val prefsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null) {
            storm.invalidate(key)
            sunny.invalidate(key)
            night.invalidate(key)
            combined.invalidate(key)
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
        const val KEY_MOON_CUSTOM_X = "moon_custom_x"
        const val KEY_MOON_CUSTOM_Y = "moon_custom_y"
        const val KEY_SUNNY_CUSTOM_SKY_TOP_COLOR = "sunny_custom_sky_top_color"
        const val KEY_SUNNY_CUSTOM_SKY_BOTTOM_COLOR = "sunny_custom_sky_bottom_color"
        const val KEY_SUNNY_GOD_RAYS_ENABLED = "sunny_god_rays_enabled"
        const val KEY_SUNNY_GOD_RAYS_INTENSITY = "sunny_god_rays_intensity"
        const val KEY_SUNNY_LENS_FLARE_ENABLED = "sunny_lens_flare_enabled"
        const val KEY_SUNNY_LENS_FLARE_INTENSITY = "sunny_lens_flare_intensity"
        const val KEY_SUNNY_LENS_FLARE_CHROMATIC = "sunny_lens_flare_chromatic"
        const val KEY_SUNNY_GYRO_ENABLED = "sunny_gyro_enabled"
        const val KEY_SUNNY_TOUCH_BURST_ENABLED = "sunny_touch_burst_enabled"
        const val KEY_SCREEN_DROPLETS_ENABLED = "screen_droplets_enabled"
        const val KEY_SCREEN_DROPLETS_SIZE = "screen_droplets_size"
        const val KEY_RAIN_SPAWN_MODE = "rain_spawn_mode"
        const val KEY_WIND_LINES_ENABLED = "wind_lines_enabled"
        const val KEY_WIND_LINES_INTENSITY = "wind_lines_intensity"
        const val KEY_TIME_MODE = "time_mode"
        const val KEY_MOON_PHASE = "moon_phase"
        const val KEY_MOON_PATH_DIRECTION = "moon_path_direction"
        const val KEY_MOON_MOVE_SPEED = "moon_move_speed"
        const val KEY_MOON_STATIONARY_POSITION = "moon_stationary_position"
        const val KEY_STAR_COLOR_INDEX = "star_color_index"
        const val KEY_STAR_DENSITY = "star_density"
        const val KEY_STAR_MODE = "star_mode"
        const val KEY_NIGHT_CLOUD_DENSITY = "night_cloud_density"
        const val KEY_GRADIENT_CYCLE_SPEED = "gradient_cycle_speed"
        const val KEY_NEBULA_INTENSITY = "nebula_intensity"

        // New keys for Combined Mode
        const val KEY_COMBINED_SUN_SIZE = "combined_sun_size"
        const val KEY_COMBINED_MOON_SIZE = "combined_moon_size"
        const val KEY_COMBINED_MOON_PHASE = "combined_moon_phase"
        const val KEY_COMBINED_STAR_DENSITY = "combined_star_density"
        const val KEY_COMBINED_STAR_COLOR_INDEX = "combined_star_color_index"
        const val KEY_COMBINED_PATH_DIRECTION = "combined_path_direction"
        const val KEY_COMBINED_MOON_PHASE_AUTO_ADVANCE = "combined_moon_phase_auto_advance"

        const val DEFAULT_TIME_MODE = 0
        const val DEFAULT_MOON_PHASE = 4
        const val DEFAULT_MOON_PATH_DIRECTION = 0
        const val DEFAULT_MOON_MOVE_SPEED = 30
        const val DEFAULT_MOON_STATIONARY_POSITION = 2
        const val DEFAULT_STAR_COLOR_INDEX = 0
        const val DEFAULT_STAR_DENSITY = 60
        const val DEFAULT_STAR_MODE = 0
        const val DEFAULT_NIGHT_CLOUD_DENSITY = 40
        const val DEFAULT_GRADIENT_CYCLE_SPEED = 30
        const val DEFAULT_NEBULA_INTENSITY = 50

        const val DEFAULT_COMBINED_SUN_SIZE = 50
        const val DEFAULT_COMBINED_MOON_SIZE = 50
        const val DEFAULT_COMBINED_MOON_PHASE = 4
        const val DEFAULT_COMBINED_STAR_DENSITY = 60
        const val DEFAULT_COMBINED_STAR_COLOR_INDEX = 0
        const val DEFAULT_COMBINED_PATH_DIRECTION = 0
        const val DEFAULT_COMBINED_MOON_PHASE_AUTO_ADVANCE = false

        const val DEFAULT_CLOUD_DENSITY = 50
        const val DEFAULT_RAIN_INTENSITY = 50
        const val DEFAULT_LIGHTNING_FREQUENCY = 50
        const val DEFAULT_WIND_DIRECTION = 0
        const val DEFAULT_RAIN_COLOR_INDEX = 0
        const val DEFAULT_WIND_INTENSITY = 50
        const val KEY_RAINBOW_ALWAYS = "rainbow_always"
        const val DEFAULT_RAINBOW_ALWAYS = false
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
        const val DEFAULT_BG_BRIGHTNESS = 100

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
        const val DEFAULT_MOON_CUSTOM_X = 50
        const val DEFAULT_MOON_CUSTOM_Y = 74
        const val DEFAULT_SUNNY_CUSTOM_SKY_TOP_COLOR = 0xFF1A0D40.toInt()
        const val DEFAULT_SUNNY_CUSTOM_SKY_BOTTOM_COLOR = 0xFFF2731A.toInt()
        const val DEFAULT_SUNNY_GOD_RAYS_ENABLED = true
        const val DEFAULT_SUNNY_GOD_RAYS_INTENSITY = 50
        const val DEFAULT_SUNNY_LENS_FLARE_ENABLED = true
        const val DEFAULT_SUNNY_LENS_FLARE_INTENSITY = 50
        const val DEFAULT_SUNNY_LENS_FLARE_CHROMATIC = true
        const val DEFAULT_SUNNY_GYRO_ENABLED = true
        const val DEFAULT_SUNNY_TOUCH_BURST_ENABLED = true
        const val DEFAULT_SCREEN_DROPLETS_ENABLED = true
        const val DEFAULT_SCREEN_DROPLETS_SIZE = 100
        const val DEFAULT_RAIN_SPAWN_MODE = 0
        const val DEFAULT_WIND_LINES_ENABLED = true
        const val DEFAULT_WIND_LINES_INTENSITY = 50

        // Shooting Stars
        const val KEY_SHOOTING_STARS_ENABLED = "shooting_stars_enabled"
        const val KEY_SHOOTING_STARS_DENSITY = "shooting_stars_density"
        const val DEFAULT_SHOOTING_STARS_ENABLED = true
        const val DEFAULT_SHOOTING_STARS_DENSITY = 50

        // Fireflies
        const val KEY_FIREFLIES_ENABLED = "fireflies_enabled"
        const val DEFAULT_FIREFLIES_ENABLED = true

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

    // Sunny (day) mode
    override fun getActiveEffect(): Int = sunny.getActiveEffect()
    fun setActiveEffect(effect: Int) = sunny.setActiveEffect(effect)

    override fun getSunSize(): Int = sunny.getSunSize()
    fun setSunSize(size: Int) = sunny.setSunSize(size)

    override fun getSunSpeed(): Int = sunny.getSunSpeed()
    fun setSunSpeed(speed: Int) = sunny.setSunSpeed(speed)

    override fun getSunnyTheme(): Int = sunny.getSunnyTheme()
    fun setSunnyTheme(theme: Int) = sunny.setSunnyTheme(theme)

    override fun getSunPathDirection(): Int = sunny.getSunPathDirection()
    fun setSunPathDirection(direction: Int) = sunny.setSunPathDirection(direction)

    override fun getSunMoveSpeed(): Int = sunny.getSunMoveSpeed()
    fun setSunMoveSpeed(speed: Int) = sunny.setSunMoveSpeed(speed)

    override fun getSunnyBackgroundIndex(): Int = sunny.getSunnyBackgroundIndex()
    fun setSunnyBackgroundIndex(index: Int) = sunny.setSunnyBackgroundIndex(index)

    override fun getSunStationaryPosition(): Int = sunny.getSunStationaryPosition()
    fun setSunStationaryPosition(position: Int) = sunny.setSunStationaryPosition(position)

    override fun getSunCustomX(): Int = sunny.getSunCustomX()
    fun setSunCustomX(x: Int) = sunny.setSunCustomX(x)

    override fun getSunCustomY(): Int = sunny.getSunCustomY()
    fun setSunCustomY(y: Int) = sunny.setSunCustomY(y)

    override fun getMoonCustomX(): Int = sunny.getMoonCustomX()
    fun setMoonCustomX(x: Int) = sunny.setMoonCustomX(x)

    override fun getMoonCustomY(): Int = sunny.getMoonCustomY()
    fun setMoonCustomY(y: Int) = sunny.setMoonCustomY(y)

    override fun getSunnyCustomSkyTopColor(): Int = sunny.getSunnyCustomSkyTopColor()
    fun setSunnyCustomSkyTopColor(color: Int) = sunny.setSunnyCustomSkyTopColor(color)

    override fun getSunnyCustomSkyBottomColor(): Int = sunny.getSunnyCustomSkyBottomColor()
    fun setSunnyCustomSkyBottomColor(color: Int) = sunny.setSunnyCustomSkyBottomColor(color)

    override fun isSunnyGodRaysEnabled(): Boolean = sunny.isSunnyGodRaysEnabled()
    fun setSunnyGodRaysEnabled(enabled: Boolean) = sunny.setSunnyGodRaysEnabled(enabled)

    override fun getSunnyGodRaysIntensity(): Int = sunny.getSunnyGodRaysIntensity()
    fun setSunnyGodRaysIntensity(intensity: Int) = sunny.setSunnyGodRaysIntensity(intensity)

    override fun isSunnyLensFlareEnabled(): Boolean = sunny.isSunnyLensFlareEnabled()
    fun setSunnyLensFlareEnabled(enabled: Boolean) = sunny.setSunnyLensFlareEnabled(enabled)

    override fun isSunnyLensFlareChromaticEnabled(): Boolean = sunny.isSunnyLensFlareChromaticEnabled()
    fun setSunnyLensFlareChromaticEnabled(enabled: Boolean) = sunny.setSunnyLensFlareChromaticEnabled(enabled)

    override fun getSunnyLensFlareIntensity(): Int = sunny.getSunnyLensFlareIntensity()
    fun setSunnyLensFlareIntensity(intensity: Int) = sunny.setSunnyLensFlareIntensity(intensity)

    override fun isSunnyGyroEnabled(): Boolean = sunny.isSunnyGyroEnabled()
    fun setSunnyGyroEnabled(enabled: Boolean) = sunny.setSunnyGyroEnabled(enabled)

    override fun isSunnyTouchBurstEnabled(): Boolean = sunny.isSunnyTouchBurstEnabled()
    fun setSunnyTouchBurstEnabled(enabled: Boolean) = sunny.setSunnyTouchBurstEnabled(enabled)

    override fun isFirefliesEnabled(): Boolean = sunny.isFirefliesEnabled()
    fun setFirefliesEnabled(enabled: Boolean) = sunny.setFirefliesEnabled(enabled)

    override fun isRainbowAlwaysEnabled(): Boolean = sunny.isRainbowAlwaysEnabled()
    fun setRainbowAlwaysEnabled(enabled: Boolean) = sunny.setRainbowAlwaysEnabled(enabled)

    // Night mode
    override fun getTimeMode(): Int = night.getTimeMode()
    fun setTimeMode(mode: Int) = night.setTimeMode(mode)

    override fun getMoonPhase(): Int = night.getMoonPhase()
    fun setMoonPhase(phase: Int) = night.setMoonPhase(phase)

    override fun getMoonPathDirection(): Int = night.getMoonPathDirection()
    fun setMoonPathDirection(dir: Int) = night.setMoonPathDirection(dir)

    override fun getMoonMoveSpeed(): Int = night.getMoonMoveSpeed()
    fun setMoonMoveSpeed(speed: Int) = night.setMoonMoveSpeed(speed)

    override fun getMoonStationaryPosition(): Int = night.getMoonStationaryPosition()
    fun setMoonStationaryPosition(pos: Int) = night.setMoonStationaryPosition(pos)

    override fun getStarColorIndex(): Int = night.getStarColorIndex()
    fun setStarColorIndex(index: Int) = night.setStarColorIndex(index)

    override fun getStarDensity(): Int = night.getStarDensity()
    fun setStarDensity(density: Int) = night.setStarDensity(density)

    override fun getStarMode(): Int = night.getStarMode()
    fun setStarMode(mode: Int) = night.setStarMode(mode)

    override fun getNightCloudDensity(): Int = night.getNightCloudDensity()
    fun setNightCloudDensity(density: Int) = night.setNightCloudDensity(density)

    override fun getGradientCycleSpeed(): Int = night.getGradientCycleSpeed()
    fun setGradientCycleSpeed(speed: Int) = night.setGradientCycleSpeed(speed)

    override fun getNebulaIntensity(): Int = night.getNebulaIntensity()
    fun setNebulaIntensity(intensity: Int) = night.setNebulaIntensity(intensity)

    override fun isShootingStarsEnabled(): Boolean = night.isShootingStarsEnabled()
    fun setShootingStarsEnabled(enabled: Boolean) = night.setShootingStarsEnabled(enabled)

    override fun getShootingStarsDensity(): Int = night.getShootingStarsDensity()
    fun setShootingStarsDensity(density: Int) = night.setShootingStarsDensity(density)

    // Combined day+night mode
    override fun getCombinedSunSize(): Int = combined.getCombinedSunSize()
    fun setCombinedSunSize(size: Int) = combined.setCombinedSunSize(size)

    override fun getCombinedMoonSize(): Int = combined.getCombinedMoonSize()
    fun setCombinedMoonSize(size: Int) = combined.setCombinedMoonSize(size)

    override fun getCombinedMoonPhase(): Int = combined.getCombinedMoonPhase()
    fun setCombinedMoonPhase(phase: Int) = combined.setCombinedMoonPhase(phase)

    override fun getCombinedStarDensity(): Int = combined.getCombinedStarDensity()
    fun setCombinedStarDensity(density: Int) = combined.setCombinedStarDensity(density)

    override fun getCombinedStarColorIndex(): Int = combined.getCombinedStarColorIndex()
    fun setCombinedStarColorIndex(index: Int) = combined.setCombinedStarColorIndex(index)

    override fun getCombinedPathDirection(): Int = combined.getCombinedPathDirection()
    fun setCombinedPathDirection(dir: Int) = combined.setCombinedPathDirection(dir)

    override fun isCombinedMoonPhaseAutoAdvanceEnabled(): Boolean = combined.isCombinedMoonPhaseAutoAdvanceEnabled()
    fun setCombinedMoonPhaseAutoAdvanceEnabled(enabled: Boolean) = combined.setCombinedMoonPhaseAutoAdvanceEnabled(enabled)
}
