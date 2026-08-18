package com.wolf.wallpaper.storm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RainDropTest {

    @Test
    fun update_whileSpawnDelayPending_keepsDropOffscreenAndCountsDown() {
        val drop = RainDrop(0f, 0f, 0f, 0f, spawnDelay = 1.0f)
        drop.update(0.4f)
        assertEquals(0.6f, drop.spawnDelay, 0.0001f)
        assertEquals(999f, drop.positionY, 0.0001f)
        assertFalse(drop.isActive)
    }

    @Test
    fun update_whenSpawnDelayElapses_activatesAtSpawnPosition() {
        val drop = RainDrop(0f, 0f, 1f, -2f, spawnDelay = 0f)
        drop.spawnX = 0.5f
        drop.spawnY = 1.05f
        drop.update(0.1f)
        assertTrue(drop.isActive)
        // First active frame snaps to spawn position, then integrates velocity for this frame
        assertEquals(0.5f + 1f * 0.1f, drop.positionX, 0.0001f)
        assertEquals(1.05f - 2f * 0.1f, drop.positionY, 0.0001f)
    }

    @Test
    fun update_whenAlreadyActive_integratesPositionByVelocity() {
        val drop = RainDrop(0f, 0f, 2f, -3f, spawnDelay = 0f)
        drop.isActive = true
        drop.positionX = 1f
        drop.positionY = 1f
        drop.update(0.5f)
        assertEquals(1f + 2f * 0.5f, drop.positionX, 0.0001f)
        assertEquals(1f - 3f * 0.5f, drop.positionY, 0.0001f)
    }

    @Test
    fun updateVelocity_atRainSpeed50_scalesBaseSpeedByPointSix() {
        // Reset first to populate baseSpeed (range [3.0, 5.0]) and a small +/-4.5 degree angle offset
        val drop = RainDrop(0f, 0f, 0f, 0f)
        drop.reset(aspectRatio = 1f, windAngle = 0f, rainSpeed = 50f, startOnScreen = true)

        val speed = kotlin.math.sqrt(drop.velocityX * drop.velocityX + drop.velocityY * drop.velocityY)
        // speedFactor = 0.3 + (50/100)*0.6 = 0.6; baseSpeed in [3.0, 5.0] -> speed in [1.8, 3.0]
        assertTrue("speed ($speed) should be in [1.8, 3.0]", speed in 1.8f..3.01f)
    }

    @Test
    fun updateVelocity_fallsMostlyDownward_whenWindAngleIsZero() {
        val drop = RainDrop(0f, 0f, 0f, 0f)
        drop.reset(aspectRatio = 1f, windAngle = 0f, rainSpeed = 50f, startOnScreen = true)
        // With windAngle 0 and only a small +/-2.5 degree deviation, the drop should fall mostly downward
        assertTrue("velocityY should be negative (falling)", drop.velocityY < 0f)
        assertTrue("dirY should be close to -1 for near-vertical fall", drop.dirY < -0.99f)
    }

    @Test
    fun reset_withCloudSpawn_marksSpawnFromCloudAndTracksParent() {
        val cloud = StormCloud(id = 0, positionX = 0f, positionY = 0.5f, speedFactor = 1f, scale = 0.5f, opacity = 1f, textureIndex = 0)
        cloud.z = 0.8f
        val drop = RainDrop(0f, 0f, 0f, 0f)
        drop.reset(aspectRatio = 1f, windAngle = 0f, rainSpeed = 50f, startOnScreen = true, spawnCloud = cloud)

        assertTrue(drop.spawnFromCloud)
        assertEquals(cloud, drop.parentCloud)
        assertEquals(cloud.z, drop.z, 0.021f)
    }

    @Test
    fun reset_withCloudSpawn_capsDropLengthForCloseUpClouds() {
        // A large, close-up cloud (high baseScale and z close to 1) would otherwise
        // produce an oversized drop: baseScale * 0.2016 * z with no ceiling.
        val cloud = StormCloud(id = 0, positionX = 0f, positionY = 0.5f, speedFactor = 1f, scale = 1.40625f, opacity = 1f, textureIndex = 0)
        cloud.z = 1.0f
        val drop = RainDrop(0f, 0f, 0f, 0f)
        drop.reset(aspectRatio = 1f, windAngle = 0f, rainSpeed = 50f, startOnScreen = true, spawnCloud = cloud)

        val uncappedLength = cloud.baseScale * 0.2016f * cloud.z
        assertTrue("uncapped length ($uncappedLength) should exceed the cap for this scenario", uncappedLength > 0.1386f)
        // The cap matches the maximum edge-rain drop can reach: (0.09492 + 0.04368) * zMax(1.0).
        assertEquals(0.1386f, drop.length, 0.0001f)
    }

    @Test
    fun update_withSubFrameSpawning_interpolatesPosition() {
        val drop = RainDrop(0f, 0f, 10f, -20f, spawnDelay = 0.05f)
        drop.spawnX = 100f
        drop.spawnY = 200f
        // deltaTime 0.1f means 0.05f delay is elapsed with 0.05f overflow
        drop.update(0.1f)
        assertTrue(drop.isActive)
        assertEquals(100f + 10f * 0.05f, drop.positionX, 0.0001f)
        assertEquals(200f - 20f * 0.05f, drop.positionY, 0.0001f)
    }

    @Test
    fun reset_edgeRainWithFallbackDisallowed_disablesDropUntilNextCheck() {
        val drop = RainDrop(0f, 0f, 0f, 0f)
        drop.reset(aspectRatio = 1f, windAngle = 0f, rainSpeed = 50f, startOnScreen = true, spawnCloud = null, allowFallbackToEdge = false)

        assertFalse(drop.isActive)
        assertEquals(0f, drop.length, 0.0001f)
        assertEquals(999f, drop.positionY, 0.0001f)
        assertTrue("spawnDelay should be scheduled for a quick re-check", drop.spawnDelay in 0.1f..0.6f)
    }

    @Test
    fun reset_edgeRainWithFallbackAllowed_marksNotFromCloud() {
        val drop = RainDrop(0f, 0f, 0f, 0f)
        drop.reset(aspectRatio = 1f, windAngle = 0f, rainSpeed = 50f, startOnScreen = true, spawnCloud = null, allowFallbackToEdge = true)

        assertFalse(drop.spawnFromCloud)
        assertEquals(null, drop.parentCloud)
        assertTrue(drop.isActive)
    }
}
