package com.wolf.wallpaper

import android.content.Context
import kotlin.random.Random

class SceneManager(
    private val context: Context?,
    private val configProvider: ConfigProvider
) {
    private val clouds = mutableListOf<Cloud>()
    private val rainDrops = mutableListOf<RainDrop>()
    val lightning = Lightning()
    
    private var cloudDensity = -1
    private var rainIntensity = -1
    private var lightningFrequency = -1
    private var windDirection = -1
    
    private var timeSinceLastLightning = 0f
    private var nextLightningDelay = 0f
    private var aspectRatio = 1.0f

    init {
        updateFromConfig()
        setupNextLightningDelay()
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        
        // Re-align objects to new aspect ratio coordinates
        // Commented out clouds for now
        /*
        for (cloud in clouds) {
            cloud.reset(Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio)
        }
        */
        for (drop in rainDrops) {
            drop.reset(aspectRatio, windDirection, startOnScreen = true)
        }
    }

    fun update(deltaTime: Float) {
        updateFromConfig()

        // 1. Update Clouds (Disabled for now)
        /*
        for (cloud in clouds) {
            cloud.update(deltaTime)
            // Reset if cloud is fully off-screen to the right
            val maxBound = aspectRatio + cloud.scale * 2.0f
            if (cloud.positionX > maxBound) {
                cloud.reset(-maxBound, aspectRatio)
            }
        }
        */

        // 2. Update Rain
        for (drop in rainDrops) {
            drop.update(deltaTime)
            // Check boundaries based on direction of velocity
            if (drop.positionY < -1.05f || 
                (drop.velocityX < 0f && drop.positionX < -aspectRatio - 0.1f) ||
                (drop.velocityX > 0f && drop.positionX > aspectRatio + 0.1f)) {
                drop.reset(aspectRatio, windDirection, startOnScreen = false)
            }
        }

        // 3. Update Lightning (Disabled for now)
        /*
        if (lightning.isActive) {
            lightning.update(deltaTime)
        } else {
            if (lightningFrequency > 0) {
                timeSinceLastLightning += deltaTime
                if (timeSinceLastLightning >= nextLightningDelay) {
                    lightning.trigger(aspectRatio)
                    timeSinceLastLightning = 0f
                    setupNextLightningDelay()
                }
            }
        }
        */
    }

    fun getClouds(): List<Cloud> = clouds
    fun getRainDrops(): List<RainDrop> = rainDrops
    fun getRainColorIndex(): Int = configProvider.getRainColorIndex()

    private fun updateFromConfig() {
        val targetDensity = configProvider.getCloudDensity()
        val targetRain = configProvider.getRainIntensity()
        val targetLightning = configProvider.getLightningFrequency()
        val targetWind = configProvider.getWindDirection()

        if (targetDensity != cloudDensity) {
            adjustClouds(targetDensity)
            cloudDensity = targetDensity
        }

        if (targetWind != windDirection) {
            windDirection = targetWind
            for (drop in rainDrops) {
                drop.reset(aspectRatio, windDirection, startOnScreen = true)
            }
        }

        if (targetRain != rainIntensity) {
            adjustRain(targetRain)
            rainIntensity = targetRain
        }

        if (targetLightning != lightningFrequency) {
            lightningFrequency = targetLightning
            timeSinceLastLightning = 0f
            setupNextLightningDelay()
        }
    }

    private fun adjustClouds(density: Int) {
        // Map 0-100 density to 0-20 clouds
        val targetCount = (density / 100f * 20).toInt().coerceIn(0, 20)
        
        while (clouds.size < targetCount) {
            val cloudId = clouds.size
            val textureIndex = Random.nextInt(3)
            val cloud = Cloud(cloudId, 0f, 0f, 0f, 0f, 0f, textureIndex)
            cloud.reset(Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio)
            clouds.add(cloud)
        }
        
        while (clouds.size > targetCount) {
            clouds.removeAt(clouds.size - 1)
        }
    }

    private fun adjustRain(intensity: Int) {
        // Map 0-100 intensity to 0-600 rain drops
        val targetCount = (intensity / 100f * 600).toInt().coerceIn(0, 600)
        
        while (rainDrops.size < targetCount) {
            val drop = RainDrop(0f, 0f, 0f, 0f)
            drop.reset(aspectRatio, windDirection, startOnScreen = true)
            rainDrops.add(drop)
        }
        
        while (rainDrops.size > targetCount) {
            rainDrops.removeAt(rainDrops.size - 1)
        }
    }

    private fun setupNextLightningDelay() {
        if (lightningFrequency <= 0) {
            nextLightningDelay = Float.MAX_VALUE
            return
        }

        // Map frequency to delay in seconds:
        // 0 -> Never
        // 25 -> 60s
        // 50 -> 30s
        // 75 -> 15s
        // 100 -> 5s
        val baseDelay = when {
            lightningFrequency <= 25 -> {
                val t = lightningFrequency / 25.0f
                if (t <= 0.05f) 3600f else 60f / t
            }
            lightningFrequency <= 50 -> {
                val t = (lightningFrequency - 25) / 25.0f
                60f - t * 30f
            }
            lightningFrequency <= 75 -> {
                val t = (lightningFrequency - 50) / 25.0f
                30f - t * 15f
            }
            else -> {
                val t = (lightningFrequency - 75) / 25.0f
                15f - t * 10f
            }
        }
        
        // Add random variance (+/- 10% maximum tolerance)
        val maxVariance = 0.10f
        val variance = (Random.nextFloat() * 2f - 1f) * maxVariance * baseDelay
        nextLightningDelay = baseDelay + variance
    }
}
