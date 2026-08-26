package com.wolf.wallpaper.storm

import android.content.Context
import com.wolf.wallpaper.core.ConfigProvider
import com.wolf.wallpaper.storm.StormCloud
import kotlin.random.Random

class SceneManager(
    private val context: Context?,
    val configProvider: ConfigProvider
) {
    companion object {
        // Variable Storm Mode cycle timings (seconds)
        private const val STORM_BURST_DURATION = 30.0f
        private const val STORM_CALM_DURATION = 60.0f
        private const val STORM_TRANSITION_DURATION = 15.0f

        // Current-value smoothing rate (lerp fraction per second)
        private const val VALUE_LERP_RATE = 5f

        // Wind angle / cloud drift
        private const val MAX_WIND_ANGLE_DEGREES = 35f
        private const val MAX_CLOUD_WIND_SPEED = 0.15f
        private const val CLOUD_WRAP_SCALE_FACTOR = 2.4f
        private const val CLOUD_DRIFT_WIND_THRESHOLD = 0.1f

        // Wind lines
        private const val WIND_LINES_MIN_INTENSITY = 50
        private const val WIND_LINES_MAX_COUNT = 8
        private const val WIND_LINES_HARD_CAP = 15

        // Variable Storm Mode target ranges (calm -> burst), mixed by transition factor
        private const val VARIABLE_CLOUD_DENSITY_CALM = 40
        private const val VARIABLE_CLOUD_DENSITY_BURST = 90
        private const val VARIABLE_RAIN_INTENSITY_CALM = 15
        private const val VARIABLE_RAIN_INTENSITY_BURST = 95
        private const val VARIABLE_LIGHTNING_FREQ_CALM = 10
        private const val VARIABLE_LIGHTNING_FREQ_BURST = 85
        private const val VARIABLE_CLOUD_FLASH_FREQ_CALM = 10
        private const val VARIABLE_CLOUD_FLASH_FREQ_BURST = 80
        private const val VARIABLE_WIND_INTENSITY_CALM = 15
        private const val VARIABLE_WIND_INTENSITY_BURST = 95
        private const val VARIABLE_RAIN_SPEED_CALM = 35
        private const val VARIABLE_RAIN_SPEED_BURST = 85

        // Cloud count mapping (density 0-100 -> cloud count, max 10)
        private const val MAX_CLOUD_COUNT = 10

        // Rain drop count mapping (intensity 0-100 -> drop count, max 1000)
        private const val MAX_RAIN_DROP_COUNT = 1000
    }

    private val clouds = mutableListOf<StormCloud>()
    private val rainDrops = mutableListOf<RainDrop>()
    private val rainProjectedXBuffer = FloatArray(MAX_RAIN_DROP_COUNT)
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
    private var aspectRatio = 1.0f
    private var viewWidth = 0
    private var viewHeight = 0
    @Volatile
    private var pendingTouch: Pair<Float, Float>? = null

    // Variable Storm Mode cycle state variables
    private var stormCycleTime = 0f
    private var isBurstActive = false
    private var currentBurstWindDirection = 2
    private var stormTransitionFactor = 0f

    init {
        updateFromConfig()
        currentWindAngle = targetWindAngle
        currentRainSpeed = targetRainSpeed
        setupNextLightningDelay()
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
        var activeCount = 0
        for (drop in rainDrops) {
            drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = true, spawnCloud = getSpawnCloudForDrop(), allowFallbackToEdge = allowFallback, existingProjectedX = rainProjectedXBuffer, activeCount = activeCount)
            if (activeCount < rainProjectedXBuffer.size) {
                rainProjectedXBuffer[activeCount++] = drop.getProjectedX()
            }
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

        if (configProvider.isStormVariableEnabled()) {
            stormCycleTime += deltaTime
            if (isBurstActive) {
                if (stormCycleTime >= STORM_BURST_DURATION) {
                    isBurstActive = false
                    stormCycleTime = 0f
                }
            } else {
                if (stormCycleTime >= STORM_CALM_DURATION) {
                    isBurstActive = true
                    stormCycleTime = 0f
                    // Choose random wind direction for the burst (0: left, 2: right)
                    currentBurstWindDirection = if (Random.nextBoolean()) 2 else 0
                }
            }

            // Smoothly interpolate transition factor
            val targetTransition = if (isBurstActive) 1.0f else 0.0f
            val transitionRate = 1.0f / STORM_TRANSITION_DURATION
            if (stormTransitionFactor < targetTransition) {
                stormTransitionFactor = (stormTransitionFactor + transitionRate * deltaTime).coerceAtMost(targetTransition)
            } else if (stormTransitionFactor > targetTransition) {
                stormTransitionFactor = (stormTransitionFactor - transitionRate * deltaTime).coerceAtLeast(targetTransition)
            }
        } else {
            stormCycleTime = 0f
            isBurstActive = false
            stormTransitionFactor = 0f
        }

        updateFromConfig()

        // Interpolate current values (lerp)
        val lerpFactor = VALUE_LERP_RATE * deltaTime
        currentWindAngle += (targetWindAngle - currentWindAngle) * lerpFactor.coerceAtMost(1f)
        currentRainSpeed += (targetRainSpeed - currentRainSpeed) * lerpFactor.coerceAtMost(1f)

        // 1. Update Clouds
        val windSpeed = when (windDirection) {
            0 -> -(windIntensity / 100f) * MAX_CLOUD_WIND_SPEED
            2 -> (windIntensity / 100f) * MAX_CLOUD_WIND_SPEED
            else -> 0f
        }
        val dynamicsSpeed = configProvider.getCloudDynamicsSpeed() / 100f
        val isCloudFlashEnabled = configProvider.isCloudFlashEnabled()
        val flashFrequency = configProvider.getCloudFlashFrequency()
        val flashColorIndex = configProvider.getCloudFlashColorIndex()
        for (cloud in clouds) {
            cloud.update(
                deltaTime,
                windSpeed,
                dynamicsSpeed,
                isCloudFlashEnabled,
                flashFrequency,
                flashColorIndex
            )
            val halfWidth = cloud.scale * CLOUD_WRAP_SCALE_FACTOR
            val maxBound = aspectRatio + halfWidth
            if (cloud.positionX > maxBound || cloud.positionX < -maxBound) {
                cloud.reset(0f, aspectRatio, textureCount = getCloudTextureCount())
                val newHalfWidth = cloud.scale * CLOUD_WRAP_SCALE_FACTOR
                val driftInfluence = (1.0f - (kotlin.math.abs(windSpeed) / CLOUD_DRIFT_WIND_THRESHOLD)).coerceIn(0f, 1f)
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
                var activeCount = 0
                for (otherDrop in rainDrops) {
                    if (otherDrop !== drop) {
                        if (activeCount < rainProjectedXBuffer.size) {
                            rainProjectedXBuffer[activeCount++] = otherDrop.getProjectedX()
                        }
                    }
                }
                drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = false, spawnCloud = getSpawnCloudForDrop(), allowFallbackToEdge = allowFallback, existingProjectedX = rainProjectedXBuffer, activeCount = activeCount)
            }
        }

        // 2b. Update Wind Lines
        val targetWindLinesCount = if (configProvider.isWindLinesEnabled() && windIntensity >= WIND_LINES_MIN_INTENSITY) {
            (configProvider.getWindLinesIntensity() / 100f * WIND_LINES_MAX_COUNT).toInt().coerceIn(1, WIND_LINES_HARD_CAP)
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
                    val colorToUse = configProvider.getLightningColorIndex()
                    val spawnMode = configProvider.getLightningSpawnMode() // 0: Bordes, 1: Nubes, 2: Ambos
                    
                    val useCloud = when (spawnMode) {
                        1 -> true
                        2 -> Random.nextBoolean()
                        else -> false
                    }
                    
                    var selectedCloud: StormCloud? = null
                    if (useCloud) {
                        var activeCount = 0
                        for (c in clouds) {
                            if (!c.isFadingOut) activeCount++
                        }
                        if (activeCount > 0) {
                            val targetIndex = Random.nextInt(activeCount)
                            var currentIndex = 0
                            for (c in clouds) {
                                if (!c.isFadingOut) {
                                    if (currentIndex == targetIndex) {
                                        selectedCloud = c
                                        break
                                    }
                                    currentIndex++
                                }
                            }
                        }
                    }
                    
                    if (selectedCloud != null) {
                        inactiveLightning.triggerFromCloud(
                            selectedCloud,
                            aspectRatio,
                            getLightningTextureCount(),
                            colorToUse,
                            configProvider.getLightningDuration(),
                            isInternalOnly = false
                        )
                    } else {
                        inactiveLightning.trigger(
                            aspectRatio,
                            getLightningTextureCount(),
                            colorToUse,
                            configProvider.getLightningDuration(),
                            isInternalOnly = false
                        )
                    }
                }
                timeSinceLastLightning = 0f
                setupNextLightningDelay()
            }
        }
    }

    fun getClouds(): List<StormCloud> = clouds
    fun getRainDrops(): List<RainDrop> = rainDrops
    // Sorts the internal wind line / cloud lists in place by depth (z)
    fun sortWindLinesByDepth() { windLines.sortBy { it.z } }
    fun sortCloudsByDepth() { clouds.sortBy { it.z } }
    fun getRainColorIndex(): Int = configProvider.getRainColorIndex()
    fun getBackgroundIndex(): Int = configProvider.getBackgroundIndex()
    fun getBgBrightness(): Float = configProvider.getBgBrightness() / 100f
    fun isLightningFlashEnabled(): Boolean = configProvider.isLightningFlashEnabled()
    fun isCloudFlashEnabled(): Boolean = configProvider.isCloudFlashEnabled()
    fun isScreenDropletsEnabled(): Boolean = configProvider.isScreenDropletsEnabled()
    fun getScreenDropletsSize(): Int = configProvider.getScreenDropletsSize()
    fun getWindLines(): List<WindLine> = windLines
    fun isWindLinesEnabled(): Boolean = configProvider.isWindLinesEnabled()
    fun getWindIntensity(): Int = windIntensity
    fun getRainIntensity(): Int = rainIntensity
    fun getCurrentWindAngle(): Float = currentWindAngle

    fun applySwipeWindImpulse(velocity: Float) {
        // A swipe to the right (positive velocity) generates wind to the left (negative angle impulse).
        // Scale velocity to degrees: e.g., velocity of 5.0 -> impulse of -45.0 degrees.
        val impulse = -velocity * 9f
        val cappedImpulse = impulse.coerceIn(-45f, 45f)
        currentWindAngle = (currentWindAngle + cappedImpulse).coerceIn(-90f, 90f)
    }

    private var cachedLightningTextureCount: Int? = null
    private var cachedCloudTextureCount: Int? = null

    fun getLightningTextureCount(): Int {
        cachedLightningTextureCount?.let { return it }
        val count = if (context == null) 1 else try {
            val files = context.assets.list("lightning") ?: emptyArray()
            files.count { it.endsWith(".png") }
        } catch (e: Exception) {
            1
        }
        cachedLightningTextureCount = count
        return count
    }

    fun getCloudTextureCount(): Int {
        cachedCloudTextureCount?.let { return it }
        val count = if (context == null) 1 else try {
            val files = context.assets.list("clouds") ?: emptyArray()
            val pngCount = files.count { it.endsWith(".png") }
            if (pngCount > 0) pngCount else 1
        } catch (e: Exception) {
            1
        }
        cachedCloudTextureCount = count
        return count
    }

    private fun mixInt(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).toInt()
    }

    private fun smoothstep(x: Float): Float {
        return x * x * (3.0f - 2.0f * x)
    }

    private fun updateFromConfig() {
        val isVariable = configProvider.isStormVariableEnabled()
        val smoothFactor = smoothstep(stormTransitionFactor)

        val targetDensity = if (isVariable) {
            mixInt(VARIABLE_CLOUD_DENSITY_CALM, VARIABLE_CLOUD_DENSITY_BURST, smoothFactor)
        } else {
            configProvider.getCloudDensity()
        }

        val targetRain = if (isVariable) {
            mixInt(VARIABLE_RAIN_INTENSITY_CALM, VARIABLE_RAIN_INTENSITY_BURST, smoothFactor)
        } else {
            configProvider.getRainIntensity()
        }

        val targetLightning = if (isVariable) {
            mixInt(VARIABLE_LIGHTNING_FREQ_CALM, VARIABLE_LIGHTNING_FREQ_BURST, smoothFactor)
        } else {
            configProvider.getLightningFrequency()
        }

        val targetCloudFlashFreq = if (isVariable) {
            mixInt(VARIABLE_CLOUD_FLASH_FREQ_CALM, VARIABLE_CLOUD_FLASH_FREQ_BURST, smoothFactor)
        } else {
            configProvider.getCloudFlashFrequency()
        }

        val targetWind = if (isVariable) {
            if (smoothFactor > 0.01f) currentBurstWindDirection else 0
        } else {
            val raw = configProvider.getWindDirection()
            if (raw == 1) 0 else raw
        }

        val targetWindIntensity = if (isVariable) {
            mixInt(VARIABLE_WIND_INTENSITY_CALM, VARIABLE_WIND_INTENSITY_BURST, smoothFactor)
        } else {
            val wind = configProvider.getWindIntensity()
            when {
                wind < 12 -> 0
                wind < 37 -> 25
                wind < 62 -> 50
                wind < 87 -> 75
                else -> 100
            }
        }

        val targetRainSpeedVal = if (isVariable) {
            // Snaps rain speed to 0, 25, or 50 based on the variable cycle progress (calm, transition, burst)
            when {
                smoothFactor < 0.33f -> 0
                smoothFactor < 0.66f -> 25
                else -> 50
            }
        } else {
            // Static configuration: coerce/snap to the nearest allowed speed
            val speed = configProvider.getRainSpeed()
            when {
                speed < 12 -> 0
                speed < 37 -> 25
                else -> 50
            }
        }

        if (targetDensity != cloudDensity) {
            adjustClouds(targetDensity)
            cloudDensity = targetDensity
        }

        // Calculate target values
        targetWindAngle = when (targetWind) {
            0 -> -(targetWindIntensity / 100f) * MAX_WIND_ANGLE_DEGREES
            2 -> (targetWindIntensity / 100f) * MAX_WIND_ANGLE_DEGREES
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
        }
    }

    private fun adjustClouds(density: Int) {
        val targetCount = if (density in 0..10) {
            density
        } else {
            // Map 0-100 density percentage to custom cloud count (max 10)
            when (density) {
                0 -> 0
                25 -> 2
                50 -> 5
                75 -> 8
                90 -> 9
                100 -> 10
                else -> (density / 100f * MAX_CLOUD_COUNT).toInt()
            }
        }.coerceIn(0, MAX_CLOUD_COUNT)
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
        }.coerceIn(0, MAX_RAIN_DROP_COUNT)
        
        val allowFallback = configProvider.getRainSpawnMode() != 1
        while (rainDrops.size < targetCount) {
            val drop = RainDrop(0f, 0f, 0f, 0f)
            var activeCount = 0
            for (otherDrop in rainDrops) {
                if (activeCount < rainProjectedXBuffer.size) {
                    rainProjectedXBuffer[activeCount++] = otherDrop.getProjectedX()
                }
            }
            drop.reset(aspectRatio, currentWindAngle, currentRainSpeed, startOnScreen = true, spawnCloud = getSpawnCloudForDrop(), allowFallbackToEdge = allowFallback, existingProjectedX = rainProjectedXBuffer, activeCount = activeCount)
            rainDrops.add(drop)
        }
        
        while (rainDrops.size > targetCount) {
            rainDrops.removeAt(rainDrops.size - 1)
        }
    }

    // Maps an event frequency (0-100) to a delay in seconds, with random variance applied:
    // 0 -> Never, 25 -> 20s, 50 -> 5s, 75 -> 2s, 90 -> 0.8s, 100 -> 0.25s
    private fun frequencyToDelay(frequency: Int): Float {
        if (frequency <= 0) return Float.MAX_VALUE

        val baseDelay = when {
            frequency <= 25 -> {
                val t = frequency / 25f
                if (t <= 0.05f) 3600f else 60f - t * 40f
            }
            frequency <= 50 -> {
                val t = (frequency - 25) / 25f
                20f - t * 15f
            }
            frequency <= 75 -> {
                val t = (frequency - 50) / 25f
                5f - t * 3f
            }
            frequency <= 90 -> {
                val t = (frequency - 75) / 15f
                2f - t * 1.2f
            }
            else -> {
                val t = (frequency - 90) / 10f
                0.8f - t * 0.55f
            }
        }

        // Add random variance (+/- 40% maximum tolerance) to make intervals feel natural and variable
        val maxVariance = 0.40f
        val variance = (Random.nextFloat() * 2f - 1f) * maxVariance * baseDelay
        return baseDelay + variance
    }

    private fun setupNextLightningDelay() {
        nextLightningDelay = frequencyToDelay(lightningFrequency)
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
            val colorToUse = configProvider.getLightningColorIndex()
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
        val mode = configProvider.getRainSpawnMode() // 0: Top Edge, 1: Clouds, 2: Everywhere
        if (mode == 0) return null
        if (mode == 2 && Random.nextBoolean()) return null
        
        var activeCount = 0
        for (c in clouds) {
            if (!c.isFadingOut) activeCount++
        }
        if (activeCount == 0) return null
        
        val targetIndex = Random.nextInt(activeCount)
        var currentIndex = 0
        for (c in clouds) {
            if (!c.isFadingOut) {
                if (currentIndex == targetIndex) {
                    return c
                }
                currentIndex++
            }
        }
        return null
    }
}
