package com.wolf.wallpaper

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class SceneManagerTest {

    private val mockConfig = MockConfigProvider()
    private val mockContext: Context? = null

    class MockConfigProvider : ConfigProvider {
        var mockCloudDensity = 50
        var mockRainIntensity = 50
        var mockLightningFrequency = 50
        var mockWindDirection = 0
        var mockRainColorIndex = 0

        override fun getCloudDensity(): Int = mockCloudDensity
        override fun getRainIntensity(): Int = mockRainIntensity
        override fun getLightningFrequency(): Int = mockLightningFrequency
        override fun getWindDirection(): Int = mockWindDirection
        override fun getRainColorIndex(): Int = mockRainColorIndex
    }

    @Test
    fun testCloudDensityMapping() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.onSurfaceChanged(1080, 1920) // set aspect ratio

        // Case 1: Density 0 -> 0 clouds
        mockConfig.mockCloudDensity = 0
        sceneManager.update(0.016f)
        assertEquals(0, sceneManager.getClouds().size)

        // Case 2: Density 25 -> 5 clouds
        mockConfig.mockCloudDensity = 25
        sceneManager.update(0.016f)
        assertEquals(5, sceneManager.getClouds().size)

        // Case 3: Density 50 -> 10 clouds
        mockConfig.mockCloudDensity = 50
        sceneManager.update(0.016f)
        assertEquals(10, sceneManager.getClouds().size)

        // Case 4: Density 100 -> 20 clouds
        mockConfig.mockCloudDensity = 100
        sceneManager.update(0.016f)
        assertEquals(20, sceneManager.getClouds().size)
    }

    @Test
    fun testRainIntensityMapping() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.onSurfaceChanged(1080, 1920)

        // Case 1: Intensity 0 -> 0 rain drops
        mockConfig.mockRainIntensity = 0
        sceneManager.update(0.016f)
        assertEquals(0, sceneManager.getRainDrops().size)

        // Case 2: Intensity 50 -> 150 rain drops
        mockConfig.mockRainIntensity = 50
        sceneManager.update(0.016f)
        assertEquals(150, sceneManager.getRainDrops().size)

        // Case 3: Intensity 100 -> 500 rain drops
        mockConfig.mockRainIntensity = 100
        sceneManager.update(0.016f)
        assertEquals(500, sceneManager.getRainDrops().size)
    }

    @Test
    fun testLightningFrequencyDelayCalculation() {
        val sceneManager = SceneManager(mockContext, mockConfig)

        // Test frequency 25 -> should yield base delay of 60s (+/- 10% tolerance)
        // Expected range: [54.0s, 66.0s]
        mockConfig.mockLightningFrequency = 25
        for (i in 0 until 50) { // run multiple iterations to verify random variance is within bounds
            sceneManager.update(0.016f) // forces updating next delay
            // We can inspect nextLightningDelay using reflection or checking trigger timings.
            // Let's use reflection to read private nextLightningDelay field.
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 25", delay >= 54f && delay <= 66f)
        }

        // Test frequency 50 -> base delay 30s (+/- 10% tolerance)
        // Expected range: [27.0s, 33.0s]
        mockConfig.mockLightningFrequency = 50
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 50", delay >= 27f && delay <= 33f)
        }

        // Test frequency 75 -> base delay 15s (+/- 10% tolerance)
        // Expected range: [13.5s, 16.5s]
        mockConfig.mockLightningFrequency = 75
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 75", delay >= 13.5f && delay <= 16.5f)
        }

        // Test frequency 100 -> base delay 5s (+/- 10% tolerance)
        // Expected range: [4.5s, 5.5s]
        mockConfig.mockLightningFrequency = 100
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 100", delay >= 4.5f && delay <= 5.5f)
        }
    }
}
