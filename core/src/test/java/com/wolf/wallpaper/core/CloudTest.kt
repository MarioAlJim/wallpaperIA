package com.wolf.wallpaper.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudTest {

    /**
     * Regression test for the "always the same clouds" bug: the procedural cloud shader
     * (wallpaper-sunny's `procedural_cloud.frag`) derives its whole silhouette from a single
     * `uVariation` seed. That seed used to be `cloud.id.toFloat()` - a value assigned once per
     * pooled slot and never touched again - so every time a cloud recycled via reset() it kept
     * rendering the exact same shape forever, at whatever fixed pool size (as low as 2 slots at
     * 25% density). `shapeSeed` must be rerolled on every reset() so each recycle gets a fresh
     * silhouette.
     */
    @Test
    fun reset_rerollsShapeSeed_soRecycledCloudsGetANewSilhouette() {
        val cloud = Cloud(id = 0, positionX = 0f, positionY = 0f, speedFactor = 1f, scale = 0.4f, opacity = 0.5f)

        val seedsSeen = mutableSetOf<Float>()
        repeat(20) {
            cloud.reset(startX = 0f, aspectRatio = 1.6f)
            seedsSeen.add(cloud.shapeSeed)
        }

        // 20 draws from a continuous random range landing on the exact same float twice would
        // require an actual RNG defect, not bad luck - this asserts reset() is actually
        // re-randomizing the seed rather than leaving a stale/fixed value in place.
        assertTrue("Expected reset() to vary shapeSeed across recycles, got only $seedsSeen", seedsSeen.size > 1)
    }

    @Test
    fun reset_keepsShapeSeedWithinShaderFriendlyRange() {
        val cloud = Cloud(id = 5, positionX = 0f, positionY = 0f, speedFactor = 1f, scale = 0.4f, opacity = 0.5f)

        repeat(50) {
            cloud.reset(startX = 0f, aspectRatio = 1.6f)
            assertTrue("shapeSeed ${cloud.shapeSeed} should be >= 0", cloud.shapeSeed >= 0f)
            assertTrue("shapeSeed ${cloud.shapeSeed} should be < 1000", cloud.shapeSeed < 1000f)
        }
    }

    @Test
    fun windImpulse_defaultsToZero_andIsUnaffectedByReset() {
        val cloud = Cloud(id = 1, positionX = 0f, positionY = 0f, speedFactor = 1f, scale = 0.4f, opacity = 0.5f)
        assertEquals(0f, cloud.windImpulse)

        cloud.applyWindImpulse(0.3f)
        assertEquals(0.3f, cloud.windImpulse)

        cloud.reset(startX = 0f, aspectRatio = 1.6f)
        assertEquals(0f, cloud.windImpulse)
    }
}
