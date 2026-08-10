package com.wolf.wallpaper.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigStoresTest {

    private val prefs = FakeSharedPreferences()

    // --- StormConfigStore ---

    @Test
    fun storm_defaults_matchConfigManagerDefaults() {
        val store = StormConfigStore(prefs)
        assertEquals(ConfigManager.DEFAULT_CLOUD_DENSITY, store.getCloudDensity())
        assertEquals(ConfigManager.DEFAULT_RAIN_SPEED, store.getRainSpeed())
        assertTrue(store.isLightningFlashEnabled())
    }

    @Test
    fun storm_setCloudDensity_roundTrips() {
        val store = StormConfigStore(prefs)
        store.setCloudDensity(75)
        assertEquals(75, store.getCloudDensity())
    }

    @Test
    fun storm_setCloudDensity_clampsAboveRange() {
        val store = StormConfigStore(prefs)
        store.setCloudDensity(500)
        assertEquals(100, store.getCloudDensity())
    }

    @Test
    fun storm_setCloudDensity_clampsBelowRange() {
        val store = StormConfigStore(prefs)
        store.setCloudDensity(-20)
        assertEquals(0, store.getCloudDensity())
    }

    @Test
    fun storm_setRainColorIndex_clampsToValidPalette() {
        val store = StormConfigStore(prefs)
        store.setRainColorIndex(99)
        assertEquals(5, store.getRainColorIndex())
    }

    @Test
    fun storm_setScreenDropletsSize_clampsToDocumentedRange() {
        val store = StormConfigStore(prefs)
        store.setScreenDropletsSize(1) // below the documented 10-200 range
        assertEquals(10, store.getScreenDropletsSize())
        store.setScreenDropletsSize(1000)
        assertEquals(200, store.getScreenDropletsSize())
    }

    @Test
    fun storm_setInteractiveLightningEnabled_roundTrips() {
        val store = StormConfigStore(prefs)
        store.setInteractiveLightningEnabled(false)
        assertFalse(store.isInteractiveLightningEnabled())
    }

    // --- SunnyConfigStore ---

    @Test
    fun sunny_defaults_matchConfigManagerDefaults() {
        val store = SunnyConfigStore(prefs)
        assertEquals(ConfigManager.DEFAULT_SUN_SIZE, store.getSunSize())
        assertEquals(ConfigManager.DEFAULT_SUNNY_THEME, store.getSunnyTheme())
    }

    @Test
    fun sunny_setSunnyTheme_clampsAboveRange() {
        val store = SunnyConfigStore(prefs)
        store.setSunnyTheme(42)
        assertEquals(3, store.getSunnyTheme())
    }

    @Test
    fun sunny_setSunnyBackgroundIndex_allowsCustomGalleryImageIndex() {
        val store = SunnyConfigStore(prefs)
        store.setSunnyBackgroundIndex(8)
        assertEquals(8, store.getSunnyBackgroundIndex())
    }

    @Test
    fun sunny_setSunnyBackgroundIndex_clampsOutOfRangeValues() {
        val store = SunnyConfigStore(prefs)
        store.setSunnyBackgroundIndex(-1)
        assertEquals(0, store.getSunnyBackgroundIndex())
        store.setSunnyBackgroundIndex(9)
        assertEquals(8, store.getSunnyBackgroundIndex())
    }

    @Test
    fun sunny_setSunCustomX_roundTrips() {
        val store = SunnyConfigStore(prefs)
        store.setSunCustomX(30)
        assertEquals(30, store.getSunCustomX())
    }

    // --- NightConfigStore ---

    @Test
    fun night_defaults_matchConfigManagerDefaults() {
        val store = NightConfigStore(prefs)
        assertEquals(ConfigManager.DEFAULT_MOON_PHASE, store.getMoonPhase())
        assertEquals(ConfigManager.DEFAULT_STAR_DENSITY, store.getStarDensity())
    }

    @Test
    fun night_setMoonPhase_clampsAboveRange() {
        val store = NightConfigStore(prefs)
        store.setMoonPhase(20)
        assertEquals(7, store.getMoonPhase())
    }

    // --- CombinedConfigStore ---

    @Test
    fun combined_defaults_matchConfigManagerDefaults() {
        val store = CombinedConfigStore(prefs)
        assertEquals(ConfigManager.DEFAULT_COMBINED_SUN_SIZE, store.getCombinedSunSize())
    }

    @Test
    fun combined_setCombinedPathDirection_clampsAboveRange() {
        val store = CombinedConfigStore(prefs)
        store.setCombinedPathDirection(9)
        assertEquals(3, store.getCombinedPathDirection())
    }
}
