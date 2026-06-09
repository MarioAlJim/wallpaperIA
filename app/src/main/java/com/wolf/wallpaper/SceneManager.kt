package com.wolf.wallpaper

import android.content.Context
import kotlin.random.Random

class SceneManager(
    private val context: Context?,
    private val configProvider: ConfigProvider
) {
    private val clouds = mutableListOf<Cloud>()
    private val rainDrops = mutableListOf<RainDrop>()
    val lightnings = List(3) { Lightning() }
    val lightning: Lightning get() = lightnings[0]
    
    private var cloudDensity = -1
    private var rainIntensity = -1
    private var lightningFrequency = -1
    private var windDirection = -1
    private var windIntensity = -1
    private var rainSpeed = -1

    private var targetWindAngle = 0f
    private var targetRainSpeed = 50f
    private var currentWindAngle = 0f
    private var currentRainSpeed = 50f
    
    private var timeSinceLastLightning = 0f
    private var nextLightningDelay = 0f
    private var aspectRatio = 1.0f

    init {
        updateFromConfig()
        currentWindAngle = targetWindAngle
        currentRainSpeed = targetRainSpeed
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
            drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = true)
        }
    }

    fun update(deltaTime: Float) {
        updateFromConfig()

        // Interpolate current values (lerp)
        val lerpFactor = 5f * deltaTime
        currentWindAngle += (targetWindAngle - currentWindAngle) * lerpFactor.coerceAtMost(1f)
        currentRainSpeed += (targetRainSpeed - currentRainSpeed) * lerpFactor.coerceAtMost(1f)

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
            drop.updateVelocity(currentWindAngle, currentRainSpeed)
            drop.update(deltaTime)
            // Check boundaries based on direction of velocity
            if (drop.positionY < -1.05f || 
                (drop.velocityX < 0f && drop.positionX < -aspectRatio - 0.1f) ||
                (drop.velocityX > 0f && drop.positionX > aspectRatio + 0.1f)) {
                drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = false)
            }
        }

        // 3. Update Lightning
        for (l in lightnings) {
            if (l.isActive) {
                l.update(deltaTime)
            }
        }

        if (lightningFrequency > 0) {
            timeSinceLastLightning += deltaTime
            if (timeSinceLastLightning >= nextLightningDelay) {
                val inactiveLightning = lightnings.firstOrNull { !it.isActive }
                if (inactiveLightning != null) {
                    val configColorIndex = configProvider.getLightningColorIndex()
                    val colorToUse = if (configColorIndex == 6) {
                        Random.nextInt(6)
                    } else {
                        configColorIndex
                    }
                    inactiveLightning.trigger(aspectRatio, getLightningTextureCount(), colorToUse, configProvider.getLightningDuration())
                }
                timeSinceLastLightning = 0f
                setupNextLightningDelay()
            }
        }
    }

    fun getClouds(): List<Cloud> = clouds
    fun getRainDrops(): List<RainDrop> = rainDrops
    fun getRainColorIndex(): Int = configProvider.getRainColorIndex()

    fun getLightningTextureCount(): Int {
        if (context == null) return 1
        return try {
            val files = context.assets.list("lightning") ?: emptyArray()
            files.filter { it.endsWith(".png") }.size
        } catch (e: Exception) {
            1
        }
    }

    private fun updateFromConfig() {
        val targetDensity = configProvider.getCloudDensity()
        val targetRain = configProvider.getRainIntensity()
        val targetLightning = configProvider.getLightningFrequency()
        val targetWind = configProvider.getWindDirection()
        val targetWindIntensity = configProvider.getWindIntensity()
        val targetRainSpeedVal = configProvider.getRainSpeed()

        if (targetDensity != cloudDensity) {
            adjustClouds(targetDensity)
            cloudDensity = targetDensity
        }

        // Calculate target values
        targetWindAngle = when (targetWind) {
            0 -> -(targetWindIntensity / 100f) * 35f
            2 -> (targetWindIntensity / 100f) * 35f
            else -> 0f
        }
        targetRainSpeed = targetRainSpeedVal.toFloat()

        windDirection = targetWind
        windIntensity = targetWindIntensity
        rainSpeed = targetRainSpeedVal

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
        // Map 0-100 intensity to non-linear drops count: 0, 10, 25, 50, 100
        val targetCount = when (intensity) {
            0 -> 0
            25 -> 10
            50 -> 25
            75 -> 50
            100 -> 100
            else -> (intensity / 100f * 100).toInt()
        }.coerceIn(0, 1000)
        
        while (rainDrops.size < targetCount) {
            val drop = RainDrop(0f, 0f, 0f, 0f)
            drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = true)
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
        // 25 -> 20s
        // 50 -> 5s
        // 75 -> 2s
        // 90 -> 0.8s
        // 100 -> 0.25s
        val baseDelay = when {
            lightningFrequency <= 25 -> {
                val t = lightningFrequency / 25f
                if (t <= 0.05f) 3600f else 60f - t * 40f
            }
            lightningFrequency <= 50 -> {
                val t = (lightningFrequency - 25) / 25f
                20f - t * 15f
            }
            lightningFrequency <= 75 -> {
                val t = (lightningFrequency - 50) / 25f
                5f - t * 3f
            }
            lightningFrequency <= 90 -> {
                val t = (lightningFrequency - 75) / 15f
                2f - t * 1.2f
            }
            else -> {
                val t = (lightningFrequency - 90) / 10f
                0.8f - t * 0.55f
            }
        }
        
        // Add random variance (+/- 40% maximum tolerance) to make intervals feel natural and variable
        val maxVariance = 0.40f
        val variance = (Random.nextFloat() * 2f - 1f) * maxVariance * baseDelay
        nextLightningDelay = baseDelay + variance
    }
}
