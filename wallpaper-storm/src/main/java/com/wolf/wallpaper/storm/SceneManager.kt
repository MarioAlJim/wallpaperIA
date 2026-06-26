package com.wolf.wallpaper.storm

import android.content.Context
import com.wolf.wallpaper.core.ConfigProvider
import com.wolf.wallpaper.storm.StormCloud
import kotlin.random.Random

class SceneManager(
    private val context: Context?,
    private val configProvider: ConfigProvider
) {
    private val clouds = mutableListOf<StormCloud>()
    private val rainDrops = mutableListOf<RainDrop>()
    private val windLines = mutableListOf<WindLine>()
    val lightnings = List(6) { Lightning() }
    val lightning: Lightning get() = lightnings[0]
    
    private var cloudDensity = -1
    private var rainIntensity = -1
    private var lightningFrequency = -1
    private var cloudFlashFrequency = -1
    private var windDirection = -1
    private var windIntensity = -1
    private var rainSpeed = -1

    private var targetWindAngle = 0f
    private var targetRainSpeed = 50f
    private var currentWindAngle = 0f
    private var currentRainSpeed = 50f
    
    private var timeSinceLastLightning = 0f
    private var nextLightningDelay = 0f
    private var timeSinceLastCloudFlash = 0f
    private var nextCloudFlashDelay = 0f
    private var aspectRatio = 1.0f
    private var viewWidth = 0
    private var viewHeight = 0
    @Volatile
    private var pendingTouch: Pair<Float, Float>? = null

    init {
        updateFromConfig()
        currentWindAngle = targetWindAngle
        currentRainSpeed = targetRainSpeed
        setupNextLightningDelay()
        setupNextCloudFlashDelay()
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        
        // Re-align objects to new aspect ratio coordinates
        val textureCount = getCloudTextureCount()
        for (cloud in clouds) {
            cloud.reset(Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio, textureCount = textureCount)
        }
        val allowFallback = configProvider.getRainSpawnMode() != 1
        for (drop in rainDrops) {
            drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = true, spawnCloud = getSpawnCloudForDrop(), allowFallbackToEdge = allowFallback)
        }
        windLines.clear()
    }

    fun update(deltaTime: Float) {
        val touch = pendingTouch
        if (touch != null) {
            pendingTouch = null
            if (configProvider.isInteractiveLightningEnabled()) {
                triggerInteractiveLightning(touch.first, touch.second)
            }
        }

        updateFromConfig()

        // Interpolate current values (lerp)
        val lerpFactor = 5f * deltaTime
        currentWindAngle += (targetWindAngle - currentWindAngle) * lerpFactor.coerceAtMost(1f)
        currentRainSpeed += (targetRainSpeed - currentRainSpeed) * lerpFactor.coerceAtMost(1f)

        // 1. Update Clouds
        val windSpeed = when (windDirection) {
            0 -> -(windIntensity / 100f) * 0.15f
            2 -> (windIntensity / 100f) * 0.15f
            else -> 0f
        }
        val dynamicsSpeed = configProvider.getCloudDynamicsSpeed() / 100f
        for (cloud in clouds) {
            cloud.update(deltaTime, windSpeed, dynamicsSpeed)
            val halfWidth = cloud.scale * 1.2f
            val maxBound = aspectRatio + halfWidth
            if (cloud.positionX > maxBound || cloud.positionX < -maxBound) {
                cloud.reset(0f, aspectRatio, textureCount = getCloudTextureCount())
                val newHalfWidth = cloud.scale * 1.2f
                val windThreshold = 0.1f
                val driftInfluence = (1.0f - (kotlin.math.abs(windSpeed) / windThreshold)).coerceIn(0f, 1f)
                val netSpeed = windSpeed + (cloud.driftSpeed * driftInfluence)
                if (netSpeed >= 0f) {
                    cloud.positionX = -aspectRatio - newHalfWidth
                } else {
                    cloud.positionX = aspectRatio + newHalfWidth
                }
            }
        }

        // Remove clouds that have finished fading out
        clouds.removeAll { it.isFadingOut && it.opacity <= 0f }

        // 2. Update Rain
        val allowFallback = configProvider.getRainSpawnMode() != 1
        for (drop in rainDrops) {
            drop.updateVelocity(currentWindAngle, currentRainSpeed)
            drop.update(deltaTime)
            // Check boundaries based on direction of velocity, or if the drop is an empty placeholder ready to check for clouds again
            val needsReset = drop.positionY < -1.05f || 
                (drop.velocityX < 0f && drop.positionX < -aspectRatio - 0.1f) ||
                (drop.velocityX > 0f && drop.positionX > aspectRatio + 0.1f) ||
                (drop.length == 0f && drop.spawnDelay <= 0f)
            if (needsReset) {
                drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = false, spawnCloud = getSpawnCloudForDrop(), allowFallbackToEdge = allowFallback)
            }
        }

        // 2b. Update Wind Lines
        val targetWindLinesCount = if (configProvider.isWindLinesEnabled() && windIntensity >= 50) {
            (configProvider.getWindLinesIntensity() / 100f * 8).toInt().coerceIn(1, 15)
        } else {
            0
        }

        while (windLines.size < targetWindLinesCount) {
            val line = WindLine()
            line.reset(aspectRatio, currentWindAngle, windIntensity)
            line.lifetime = Random.nextFloat() * line.maxLifetime
            windLines.add(line)
        }
        while (windLines.size > targetWindLinesCount) {
            windLines.removeAt(windLines.size - 1)
        }

        for (line in windLines) {
            line.update(deltaTime)
            if (!line.isActive) {
                line.reset(aspectRatio, currentWindAngle, windIntensity)
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
                    inactiveLightning.trigger(
                        aspectRatio,
                        getLightningTextureCount(),
                        colorToUse,
                        configProvider.getLightningDuration(),
                        isInternalOnly = false
                    )
                }
                timeSinceLastLightning = 0f
                setupNextLightningDelay()
            }
        }

        val currentCloudFlashFreq = configProvider.getCloudFlashFrequency()
        if (configProvider.isCloudFlashEnabled() && currentCloudFlashFreq > 0) {
            timeSinceLastCloudFlash += deltaTime
            if (timeSinceLastCloudFlash >= nextCloudFlashDelay) {
                val inactiveLightning = lightnings.firstOrNull { !it.isActive }
                if (inactiveLightning != null) {
                    val configColorIndex = configProvider.getCloudFlashColorIndex()
                    val colorToUse = if (configColorIndex == 6) {
                        Random.nextInt(6)
                    } else {
                        configColorIndex
                    }
                    inactiveLightning.trigger(
                        aspectRatio,
                        getLightningTextureCount(),
                        colorToUse,
                        configProvider.getLightningDuration(),
                        isInternalOnly = true
                    )
                }
                timeSinceLastCloudFlash = 0f
                setupNextCloudFlashDelay()
            }
        }
    }

    fun getClouds(): List<StormCloud> = clouds
    fun getRainDrops(): List<RainDrop> = rainDrops
    fun getRainColorIndex(): Int = configProvider.getRainColorIndex()
    fun getBackgroundIndex(): Int = configProvider.getBackgroundIndex()
    fun isLightningFlashEnabled(): Boolean = configProvider.isLightningFlashEnabled()
    fun isCloudFlashEnabled(): Boolean = configProvider.isCloudFlashEnabled()
    fun isScreenDropletsEnabled(): Boolean = configProvider.isScreenDropletsEnabled()
    fun getScreenDropletsSize(): Int = configProvider.getScreenDropletsSize()
    fun getWindLines(): List<WindLine> = windLines
    fun isWindLinesEnabled(): Boolean = configProvider.isWindLinesEnabled()
    fun getWindIntensity(): Int = windIntensity

    fun getLightningTextureCount(): Int {
        if (context == null) return 1
        return try {
            val files = context.assets.list("lightning") ?: emptyArray()
            files.filter { it.endsWith(".png") }.size
        } catch (e: Exception) {
            1
        }
    }

    fun getCloudTextureCount(): Int {
        if (context == null) return 1
        return try {
            val files = context.assets.list("clouds") ?: emptyArray()
            val count = files.filter { it.endsWith(".png") }.size
            if (count > 0) count else 1
        } catch (e: Exception) {
            1
        }
    }

    private fun updateFromConfig() {
        val targetDensity = configProvider.getCloudDensity()
        val targetRain = configProvider.getRainIntensity()
        val targetLightning = configProvider.getLightningFrequency()
        val targetCloudFlashFreq = configProvider.getCloudFlashFrequency()
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

        if (targetCloudFlashFreq != cloudFlashFrequency) {
            cloudFlashFrequency = targetCloudFlashFreq
            timeSinceLastCloudFlash = 0f
            setupNextCloudFlashDelay()
        }
    }

    private fun adjustClouds(density: Int) {
        val targetCount = if (density in 0..15) {
            density
        } else {
            // Map 0-100 density percentage to custom cloud count
            when (density) {
                0 -> 0
                25 -> 2
                50 -> 5
                75 -> 10
                90 -> 13
                100 -> 15
                else -> (density / 100f * 15).toInt()
            }
        }.coerceIn(0, 15)
        val textureCount = getCloudTextureCount()
        
        val activeClouds = clouds.filter { !it.isFadingOut }
        
        if (activeClouds.size < targetCount) {
            val fadingOutClouds = clouds.filter { it.isFadingOut }
            var needed = targetCount - activeClouds.size
            for (cloud in fadingOutClouds) {
                if (needed > 0) {
                    cloud.isFadingOut = false
                    needed--
                }
            }
            while (needed > 0) {
                val cloudId = if (clouds.isNotEmpty()) clouds.maxOf { it.id } + 1 else 0
                val textureIndex = Random.nextInt(textureCount)
                val cloud = StormCloud(cloudId, 0f, 0f, 0f, 0f, 0f, textureIndex)
                cloud.reset(Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio, textureCount = textureCount)
                cloud.opacity = 0f
                clouds.add(cloud)
                needed--
            }
        } else if (activeClouds.size > targetCount) {
            var excess = activeClouds.size - targetCount
            for (i in activeClouds.indices.reversed()) {
                if (excess > 0) {
                    activeClouds[i].isFadingOut = true
                    excess--
                }
            }
        }
    }

    private fun adjustRain(intensity: Int) {
        // Map 0-100 intensity to non-linear drops count, increased by 50% for very high (100): 0, 12, 31, 62, 187
        val targetCount = when (intensity) {
            0 -> 0
            25 -> 12
            50 -> 31
            75 -> 62
            100 -> 187
            else -> (intensity / 100f * 187).toInt()
        }.coerceIn(0, 1000)
        
        val allowFallback = configProvider.getRainSpawnMode() != 1
        while (rainDrops.size < targetCount) {
            val drop = RainDrop(0f, 0f, 0f, 0f)
            drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = true, spawnCloud = getSpawnCloudForDrop(), allowFallbackToEdge = allowFallback)
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

    private fun setupNextCloudFlashDelay() {
        val freq = cloudFlashFrequency
        if (freq <= 0) {
            nextCloudFlashDelay = Float.MAX_VALUE
            return
        }

        val baseDelay = when {
            freq <= 25 -> {
                val t = freq / 25f
                if (t <= 0.05f) 3600f else 60f - t * 40f
            }
            freq <= 50 -> {
                val t = (freq - 25) / 25f
                20f - t * 15f
            }
            freq <= 75 -> {
                val t = (freq - 50) / 25f
                5f - t * 3f
            }
            freq <= 90 -> {
                val t = (freq - 75) / 15f
                2f - t * 1.2f
            }
            else -> {
                val t = (freq - 90) / 10f
                0.8f - t * 0.55f
            }
        }
        
        val maxVariance = 0.40f
        val variance = (Random.nextFloat() * 2f - 1f) * maxVariance * baseDelay
        nextCloudFlashDelay = baseDelay + variance
    }

    fun queueTouch(x: Float, y: Float) {
        pendingTouch = Pair(x, y)
    }

    private fun triggerInteractiveLightning(x: Float, y: Float) {
        if (viewWidth <= 0 || viewHeight <= 0) return
        val openglX = (x / viewWidth * 2.0f - 1.0f) * aspectRatio
        val openglY = 1.0f - (y / viewHeight * 2.0f)
        
        val inactiveLightning = lightnings.firstOrNull { !it.isActive }
        if (inactiveLightning != null) {
            val configColorIndex = configProvider.getLightningColorIndex()
            val colorToUse = if (configColorIndex == 6) {
                Random.nextInt(6)
            } else {
                configColorIndex
            }
            inactiveLightning.triggerAt(
                openglX,
                openglY,
                aspectRatio,
                getLightningTextureCount(),
                colorToUse,
                configProvider.getLightningDuration(),
                isInternalOnly = false
            )
        }
    }

    private fun getSpawnCloudForDrop(): StormCloud? {
        val activeClouds = clouds.filter { !it.isFadingOut }
        if (activeClouds.isEmpty()) return null
        
        val mode = configProvider.getRainSpawnMode() // 0: Top Edge, 1: Clouds, 2: Everywhere
        return when (mode) {
            1 -> activeClouds.random()
            2 -> {
                if (Random.nextBoolean()) activeClouds.random() else null
            }
            else -> null
        }
    }
}
