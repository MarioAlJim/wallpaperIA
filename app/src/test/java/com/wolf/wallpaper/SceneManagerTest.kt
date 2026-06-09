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
        var mockLightningDuration = 30
        var mockBackgroundIndex = 1
        var mockCloudFlashFrequency = 50
        var mockCloudFlashColorIndex = 0

        override fun getCloudDensity(): Int = mockCloudDensity
        override fun getRainIntensity(): Int = mockRainIntensity
        override fun getLightningFrequency(): Int = mockLightningFrequency
        override fun getWindDirection(): Int = mockWindDirection
        override fun getRainColorIndex(): Int = mockRainColorIndex
        override fun getWindIntensity(): Int = mockWindIntensity
        override fun getRainSpeed(): Int = mockRainSpeed
        override fun getLightningColorIndex(): Int = mockLightningColorIndex
        override fun getLightningDuration(): Int = mockLightningDuration
        override fun getBackgroundIndex(): Int = mockBackgroundIndex
        override fun getCloudFlashFrequency(): Int = mockCloudFlashFrequency
        override fun getCloudFlashColorIndex(): Int = mockCloudFlashColorIndex
    }

    @Test
    fun testCloudDensityMapping() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.onSurfaceChanged(1080, 1920) // set aspect ratio

        // Case 1: Density 0 -> 0 clouds
        mockConfig.mockCloudDensity = 0
        sceneManager.update(0.016f)
        assertEquals(0, sceneManager.getClouds().filter { !it.isFadingOut }.size)

        // Case 2: Density 25 -> 2 clouds
        mockConfig.mockCloudDensity = 25
        sceneManager.update(0.016f)
        assertEquals(2, sceneManager.getClouds().filter { !it.isFadingOut }.size)

        // Case 3: Density 50 -> 5 clouds
        mockConfig.mockCloudDensity = 50
        sceneManager.update(0.016f)
        assertEquals(5, sceneManager.getClouds().filter { !it.isFadingOut }.size)

        // Case 4: Density 100 -> 15 clouds
        mockConfig.mockCloudDensity = 100
        sceneManager.update(0.016f)
        assertEquals(15, sceneManager.getClouds().filter { !it.isFadingOut }.size)
    }

    @Test
    fun testBackgroundConfigWrapper() {
        val sceneManager = SceneManager(mockContext, mockConfig)

        mockConfig.mockBackgroundIndex = 2
        sceneManager.update(0.016f)
        assertEquals(2, sceneManager.getBackgroundIndex())

        mockConfig.mockBackgroundIndex = 0
        sceneManager.update(0.016f)
        assertEquals(0, sceneManager.getBackgroundIndex())
    }

    @Test
    fun testRainIntensityMapping() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.onSurfaceChanged(1080, 1920)

        // Case 1: Intensity 0 -> 0 rain drops
        mockConfig.mockRainIntensity = 0
        sceneManager.update(0.016f)
        assertEquals(0, sceneManager.getRainDrops().size)

        // Case 2: Intensity 50 -> 31 rain drops
        mockConfig.mockRainIntensity = 50
        sceneManager.update(0.016f)
        assertEquals(31, sceneManager.getRainDrops().size)

        // Case 3: Intensity 100 -> 125 rain drops
        mockConfig.mockRainIntensity = 100
        sceneManager.update(0.016f)
        assertEquals(125, sceneManager.getRainDrops().size)
    }

    @Test
    fun testLightningFrequencyDelayCalculation() {
        val sceneManager = SceneManager(mockContext, mockConfig)

        // Test frequency 25 -> base delay of 20s (+/- 40% tolerance)
        // Expected range: [12.0s, 28.0s]
        mockConfig.mockLightningFrequency = 25
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 25", delay >= 12.0f && delay <= 28.0f)
        }

        // Test frequency 50 -> base delay 5s (+/- 40% tolerance)
        // Expected range: [3.0s, 7.0s]
        mockConfig.mockLightningFrequency = 50
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 50", delay >= 3.0f && delay <= 7.0f)
        }

        // Test frequency 75 -> base delay 2s (+/- 40% tolerance)
        // Expected range: [1.2s, 2.8s]
        mockConfig.mockLightningFrequency = 75
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 75", delay >= 1.2f && delay <= 2.8f)
        }

        // Test frequency 100 -> base delay 0.25s (+/- 40% tolerance)
        // Expected range: [0.15s, 0.35s]
        mockConfig.mockLightningFrequency = 100
        for (i in 0 until 50) {
            sceneManager.update(0.016f)
            val field = SceneManager::class.java.getDeclaredField("nextLightningDelay")
            field.isAccessible = true
            val delay = field.get(sceneManager) as Float
            assertTrue("Delay $delay out of bounds for freq 100", delay >= 0.15f && delay <= 0.35f)
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
            assertTrue("ScaleY should be in range [0.3f, 1.5f]", sceneManager.lightning.scaleY in 0.3f..1.5f)
            assertTrue("ScaleX should be set correctly", sceneManager.lightning.scaleX in 0.15f..0.35f)
            assertTrue("Rotation angle should be in bounds [-45, 45]", sceneManager.lightning.rotationAngle in -45f..45f)
        }
    }

    @Test
    fun testLightningDurationMapping() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        
        // 1. Duration at 0% should be in range [0.15s, 0.30s]
        sceneManager.lightning.trigger(1.0f, 1, 0, durationPercentage = 0)
        var duration = sceneManager.lightning.duration
        assertTrue("Duration $duration out of bounds for 0%", duration in 0.15f..0.30f)

        // 2. Duration at 50% should be in range [0.30s, 0.45s]
        sceneManager.lightning.trigger(1.0f, 1, 0, durationPercentage = 50)
        duration = sceneManager.lightning.duration
        assertTrue("Duration $duration out of bounds for 50%", duration in 0.30f..0.45f)

        // 3. Duration at 100% should be in range [1.00s, 1.15s]
        sceneManager.lightning.trigger(1.0f, 1, 0, durationPercentage = 100)
        duration = sceneManager.lightning.duration
        assertTrue("Duration $duration out of bounds for 100%", duration in 1.00f..1.15f)
    }

    @Test
    fun testLightningGrowthProgress() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.lightning.trigger(1.0f, 1, 0, durationPercentage = 50)
        
        // At start (age = 0f), growthProgress should be exactly 0.0f
        assertEquals(0.0f, sceneManager.lightning.growthProgress)
        
        // Update halfway through the 20% growth window
        val halfGrowthTime = sceneManager.lightning.duration * 0.10f
        sceneManager.lightning.update(halfGrowthTime)
        assertTrue("Growth progress should be around 0.5", sceneManager.lightning.growthProgress in 0.45f..0.55f)
        
        // Update past the 20% growth threshold
        val restTime = sceneManager.lightning.duration * 0.20f
        sceneManager.lightning.update(restTime)
        assertEquals("Growth progress should reach exactly 1.0", 1.0f, sceneManager.lightning.growthProgress)
    }

    @Test
    fun testCloudWindAndWrapping() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.onSurfaceChanged(1080, 1920) // aspectRatio is 1080 / 1920 = 0.5625

        // Map density 50 -> 5 clouds
        mockConfig.mockCloudDensity = 50
        sceneManager.update(0.016f)
        val clouds = sceneManager.getClouds()
        assertEquals(5, clouds.size)

        // Save initial positions of clouds
        val initialXPositions = clouds.map { it.positionX }

        // Case 1: Right wind (mockWindDirection = 2, mockWindIntensity = 100) -> windSpeed > 0f
        mockConfig.mockWindDirection = 2
        mockConfig.mockWindIntensity = 100
        sceneManager.update(1.0f) // Move by 1 second

        // All clouds should have moved to the right (positionX increased)
        // unless they wrapped around
        for (i in clouds.indices) {
            val cloud = clouds[i]
            val initialX = initialXPositions[i]
            if (cloud.positionX < initialX) {
                // If it wrapped, it must have exceeded maxBound and wrapped to -maxBound
                val halfWidth = cloud.scale * 1.2f
                val maxBound = 0.5625f + halfWidth
                assertTrue("Wrapped cloud should be on the left side", cloud.positionX <= -maxBound + 0.5f)
            } else {
                assertTrue("Cloud should have moved right", cloud.positionX > initialX)
            }
        }

        // Case 2: Left wind (mockWindDirection = 0, mockWindIntensity = 100) -> windSpeed < 0f
        mockConfig.mockWindDirection = 0
        mockConfig.mockWindIntensity = 100
        sceneManager.update(0.0f) // update config only
        val currentXPositions = clouds.map { it.positionX }
        sceneManager.update(1.0f) // Move left by 1 second

        for (i in clouds.indices) {
            val cloud = clouds[i]
            val preX = currentXPositions[i]
            if (cloud.positionX > preX) {
                // Wrapped, should be on the right side
                val halfWidth = cloud.scale * 1.2f
                val maxBound = 0.5625f + halfWidth
                assertTrue("Wrapped cloud should be on the right side", cloud.positionX >= maxBound - 0.5f)
            } else {
                assertTrue("Cloud should have moved left", cloud.positionX < preX)
            }
        }
    }

    @Test
    fun testRainDropDepthAndScaling() {
        val drop = RainDrop(0f, 0f, 0f, 0f)
        
        // Before reset, z defaults to 1.0f
        assertEquals(1.0f, drop.z)
        
        // Reset 100 times to check z bounds, scaling of length and velocity
        for (i in 0 until 100) {
            drop.reset(1.0f, 0f, 50f, startOnScreen = false)
            
            // 1. z should be between 0.2f and 1.0f
            assertTrue("z (${drop.z}) should be in range [0.2, 1.0]", drop.z in 0.2f..1.0f)
            
            // 2. Length should be scaled by z
            // Base length formula: (Random.nextFloat() * 0.05f + 0.03f) * z
            // Min base length = 0.03f, Max base length = 0.08f
            val minExpectedLength = 0.03f * drop.z - 0.001f
            val maxExpectedLength = 0.08f * drop.z + 0.001f
            assertTrue("Length (${drop.length}) should be scaled by z", drop.length in minExpectedLength..maxExpectedLength)
            
            // 3. Velocity should be scaled by z
            // baseSpeed = Random.nextFloat() * 1.5f + 3.0f -> range [3.0, 4.5]
            // speedFactor = (0.3f + (rainSpeed / 100f) * 1.5f) * z
            // For rainSpeed = 50f: speedFactor = (0.3f + 0.75f) * z = 1.05f * z
            // speed = baseSpeed * speedFactor
            val speed = kotlin.math.sqrt(drop.velocityX * drop.velocityX + drop.velocityY * drop.velocityY)
            val expectedMinSpeed = 3.0f * 1.05f * drop.z
            val expectedMaxSpeed = 4.5f * 1.05f * drop.z
            assertTrue("Speed ($speed) should be in range [${expectedMinSpeed}, ${expectedMaxSpeed}]", speed in (expectedMinSpeed - 0.05f)..(expectedMaxSpeed + 0.05f))
        }
    }

    @Test
    fun testInternalLightningFeature() {
        val sceneManager = SceneManager(mockContext, mockConfig)
        
        // Verify pool size is 6
        assertEquals(6, sceneManager.lightnings.size)
        
        // Trigger lightning and verify it supports isInternalOnly setting
        var hasInternal = false
        var hasNormal = false
        for (i in 0 until 100) {
            val l = Lightning()
            l.trigger(1.0f, 1, 0, durationPercentage = 30, isInternalOnly = (i % 2 == 0))
            if (l.isInternalOnly) {
                hasInternal = true
            } else {
                hasNormal = true
            }
        }
        assertTrue("Should support internal-only lightning triggers", hasInternal)
        assertTrue("Should support normal lightning triggers", hasNormal)
    }

    @Test
    fun testCloudDepthAndParallax() {
        val cloud = Cloud(id = 1, positionX = 0f, positionY = 0f, speedFactor = 1.0f, scale = 1.0f, opacity = 1.0f, textureIndex = 0)
        
        // Before reset, z defaults to 1.0f
        assertEquals(1.0f, cloud.z)
        
        // Reset 100 times to verify bounds and scaling
        for (i in 0 until 100) {
            cloud.reset(0f, 1.0f)
            
            // 1. z must be in [0.3f, 1.0f]
            assertTrue("z (${cloud.z}) should be in range [0.3, 1.0]", cloud.z in 0.3f..1.0f)
            
            // 2. scale should be (Random.nextFloat() * 0.7f + 0.3f) * z
            val minExpectedScale = 0.3f * cloud.z - 0.001f
            val maxExpectedScale = 1.0f * cloud.z + 0.001f
            assertTrue("Scale (${cloud.scale}) should be scaled by z", cloud.scale in minExpectedScale..maxExpectedScale)
            
            // 3. opacity should be (Random.nextFloat() * 0.4f + 0.4f) * z
            val minExpectedOpacity = 0.4f * cloud.z - 0.001f
            val maxExpectedOpacity = 0.8f * cloud.z + 0.001f
            assertTrue("Opacity (${cloud.opacity}) should be scaled by z", cloud.opacity in minExpectedOpacity..maxExpectedOpacity)
        }
    }

    @Test
    fun testIndependentCloudFlashes() {
        val sceneManager = SceneManager(mockContext, mockConfig)

        // 1. Set Lightning Frequency to 0 (Never) and Cloud Flash Frequency to 100 (Always/Maximum)
        mockConfig.mockLightningFrequency = 0
        mockConfig.mockCloudFlashFrequency = 100
        sceneManager.update(0.016f) // triggers updateFromConfig

        var triggeredCloudFlash = false
        // Simulate to guarantee trigger
        for (i in 0 until 500) {
            sceneManager.update(0.05f)
            val active = sceneManager.lightnings.filter { it.isActive }
            for (l in active) {
                assertTrue("Any active lightning must be internal only when lightningFreq is 0", l.isInternalOnly)
                triggeredCloudFlash = true
            }
        }
        assertTrue("Cloud flashes should have triggered independently", triggeredCloudFlash)

        // 2. Set Lightning Frequency to 100 and Cloud Flash Frequency to 0
        mockConfig.mockLightningFrequency = 100
        mockConfig.mockCloudFlashFrequency = 0
        sceneManager.update(0.016f)
        
        // Deactivate all lightnings
        for (l in sceneManager.lightnings) {
            val field = l.javaClass.getDeclaredField("isActive")
            field.isAccessible = true
            field.set(l, false)
        }

        var triggeredNormalLightning = false
        for (i in 0 until 500) {
            sceneManager.update(0.05f)
            val active = sceneManager.lightnings.filter { it.isActive }
            for (l in active) {
                assertTrue("Any active lightning must be normal (not internal) when cloudFlashFreq is 0", !l.isInternalOnly)
                triggeredNormalLightning = true
            }
        }
        assertTrue("Normal lightnings should have triggered independently", triggeredNormalLightning)
    }

    @Test
    fun testCloudSpeedBasedOnDepth() {
        val cloudNear = Cloud(id = 1, positionX = 0f, positionY = 0f, speedFactor = 1.0f, scale = 1.0f, opacity = 1.0f, textureIndex = 0)
        cloudNear.z = 1.0f
        // When z = 1.0f, speedZFactor must be exactly 1.25f (25% increase)
        assertEquals(1.25f, cloudNear.speedZFactor, 0.001f)

        val cloudFar = Cloud(id = 2, positionX = 0f, positionY = 0f, speedFactor = 1.0f, scale = 1.0f, opacity = 1.0f, textureIndex = 0)
        cloudFar.z = 0.3f
        // When z = 0.3f, speedZFactor must be exactly 0.225f (25% decrease of 0.3f)
        assertEquals(0.225f, cloudFar.speedZFactor, 0.001f)

        // Test position updates
        cloudNear.update(deltaTime = 1.0f, windSpeed = 1.0f)
        assertEquals(1.25f, cloudNear.positionX, 0.001f)

        cloudFar.update(deltaTime = 1.0f, windSpeed = 1.0f)
        assertEquals(0.225f, cloudFar.positionX, 0.001f)
    }

    @Test
    fun testCloudFadeInAndFadeOut() {
        // 1. Spawning transition (Fade In)
        // Set mockConfig to 0 so we initialize with no clouds, then trigger density change to 50
        mockConfig.mockCloudDensity = 0
        val sceneManager = SceneManager(mockContext, mockConfig)
        sceneManager.onSurfaceChanged(1080, 1920)
        
        assertEquals(0, sceneManager.getClouds().size)

        mockConfig.mockCloudDensity = 50 // should trigger 5 active clouds
        // update(0.0f) triggers updateFromConfig and adjustClouds but advances no time, leaving opacity at 0f
        sceneManager.update(0.0f)
        
        val clouds = sceneManager.getClouds()
        assertEquals(5, clouds.size)
        // Since they were just created, their opacity should start at 0f
        for (c in clouds) {
            assertEquals(0f, c.opacity, 0.001f)
            assertTrue("targetOpacity should be set to positive value", c.targetOpacity > 0f)
        }

        // Simulate 0.5 seconds of time -> opacity should increase towards targetOpacity
        sceneManager.update(0.5f)
        for (c in clouds) {
            assertTrue("Opacity should have increased from 0f", c.opacity > 0f)
        }

        // 2. Despawning transition (Fade Out)
        mockConfig.mockCloudDensity = 0 // should trigger 0 active clouds
        sceneManager.update(0.016f)

        // The clouds are NOT immediately removed from list, but marked isFadingOut
        val cloudsAfterDespawn = sceneManager.getClouds()
        assertTrue("Clouds should not be immediately removed", cloudsAfterDespawn.isNotEmpty())
        for (c in cloudsAfterDespawn) {
            assertTrue("Clouds should be marked as fading out", c.isFadingOut)
        }

        // Simulate time to finish fading out
        sceneManager.update(1.0f)
        // They should be removed from the sceneManager's list entirely
        assertTrue("Faded out clouds should be removed from list", sceneManager.getClouds().isEmpty())
    }
}
