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
        var mockWindIntensity = 50
        var mockRainSpeed = 50
        var mockLightningColorIndex = 0

        override fun getCloudDensity(): Int = mockCloudDensity
        override fun getRainIntensity(): Int = mockRainIntensity
        override fun getLightningFrequency(): Int = mockLightningFrequency
        override fun getWindDirection(): Int = mockWindDirection
        override fun getRainColorIndex(): Int = mockRainColorIndex
        override fun getWindIntensity(): Int = mockWindIntensity
        override fun getRainSpeed(): Int = mockRainSpeed
        override fun getLightningColorIndex(): Int = mockLightningColorIndex
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

        // Case 2: Intensity 50 -> 25 rain drops
        mockConfig.mockRainIntensity = 50
        sceneManager.update(0.016f)
        assertEquals(25, sceneManager.getRainDrops().size)

        // Case 3: Intensity 100 -> 100 rain drops
        mockConfig.mockRainIntensity = 100
        sceneManager.update(0.016f)
        assertEquals(100, sceneManager.getRainDrops().size)
    }

    @Test
    fun testLightningFrequencyDelayCalculation() {
        val sceneManager = SceneManager(mockContext, mockConfig)

        // Test frequency 25 -> base delay of 20s (+/- 10% tolerance)
        // Expected range: [18.0s, 22.0s]
        mockConfig.mockLightningFrequency = 25
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 25", delay >= 18.0f && delay <= 22.0f)
        }

        // Test frequency 50 -> base delay 5s (+/- 10% tolerance)
        // Expected range: [4.5s, 5.5s]
        mockConfig.mockLightningFrequency = 50
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 50", delay >= 4.5f && delay <= 5.5f)
        }

        // Test frequency 75 -> base delay 2s (+/- 10% tolerance)
        // Expected range: [1.8s, 2.2s]
        mockConfig.mockLightningFrequency = 75
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 75", delay >= 1.8f && delay <= 2.2f)
        }

        // Test frequency 100 -> base delay 0.25s (+/- 10% tolerance)
        // Expected range: [0.225s, 0.275s]
        mockConfig.mockLightningFrequency = 100
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 100", delay >= 0.225f && delay <= 0.275f)
        }
    }

    @Test
    fun testRainSpeedUpdates() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.onSurfaceChanged(1080, 1920)

        // Set initial rainSpeed
        mockConfig.mockRainSpeed = 50
        sceneManager.update(0.016f)

        // Verify private rainSpeed field
        val field = SceneManager::class.java.getDeclaredField("rainSpeed")
        field.isAccessible = true
        assertEquals(50, field.get(sceneManager) as Int)

        // Change rainSpeed
        mockConfig.mockRainSpeed = 80
        sceneManager.update(0.016f)
        assertEquals(80, field.get(sceneManager) as Int)
    }

    @Test
    fun testLightningColorTrigger() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        
        // 1. Specific Color (e.g. Rojo = 3)
        mockConfig.mockLightningColorIndex = 3
        sceneManager.lightning.trigger(1.0f, 1, mockConfig.getLightningColorIndex())
        assertEquals(3, sceneManager.lightning.selectedColorIndex)

        // 2. Random Mode (index 6) -> should choose a valid color index (0 to 5)
        mockConfig.mockLightningColorIndex = 6
        val resolvedColor = if (mockConfig.getLightningColorIndex() == 6) {
            kotlin.random.Random.nextInt(6)
        } else {
            mockConfig.getLightningColorIndex()
        }
        sceneManager.lightning.trigger(1.0f, 1, resolvedColor)
        assertTrue(sceneManager.lightning.selectedColorIndex in 0..5)
    }

    @Test
    fun testDiagonalLightningTrigger() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        for (i in 0 until 100) {
            sceneManager.lightning.trigger(1.0f, 1, 0)
            assertTrue("ScaleY should be adjusted to be >= 2.0f", sceneManager.lightning.scaleY >= 2.0f)
            assertTrue("ScaleX should be set correctly", sceneManager.lightning.scaleX in 0.5f..0.9f)
            assertTrue("Rotation angle should be in bounds [-45, 45]", sceneManager.lightning.rotationAngle in -45f..45f)
        }
    }
}
