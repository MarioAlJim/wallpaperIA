package com.wolf.wallpaper.storm

import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.abs
import com.wolf.wallpaper.core.StormObject

class StormCloud(
    val id: Int,
    var positionX: Float,
    var positionY: Float,
    var speedFactor: Float,
    var scale: Float,
    var opacity: Float,
    var textureIndex: Int
) : StormObject {

    var z: Float = 1.0f
    var targetOpacity: Float = opacity
    var isFadingOut: Boolean = false

    var driftSpeed: Float = 0f
    var baseScale: Float = scale
    var basePositionY: Float = positionY
    var pulseTime: Float = 0f
    var onlyGrows: Boolean = true

    // Exaggerated parallax: background clouds drift far slower and foreground clouds far faster,
    // since relative speed difference is the strongest depth cue for the human eye.
    val speedZFactor: Float
        get() = 0.15f + ((z - 0.3f) / 0.7f) * 1.30f

    var flashIntensity: Float = 0f
        private set
    val flashColor: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f)
    
    private var flashTimer = 0f
    private var flashDuration = 0f
    private var timeSinceLastFlash = 0f
    private var nextFlashDelay = 0f
    private var isFlashActive = false

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
            else -> {
                val t = (frequency - 75) / 25f
                2f - t * 1.75f
            }
        }
        val variance = 0.7f + Random.nextFloat() * 0.6f
        return baseDelay * variance
    }

    override fun update(deltaTime: Float) {
        // Default call using 0f windSpeed and 1f dynamicsSpeed to satisfy StormObject contract
        update(deltaTime, 0f, 1.0f, false, 0, 0)
    }

    fun update(
        deltaTime: Float,
        windSpeed: Float,
        dynamicsSpeed: Float = 1.0f,
        isCloudFlashEnabled: Boolean = false,
        flashFrequency: Int = 0,
        flashColorIndex: Int = 0
    ) {
        val windFactorOpacity = 1.0f + abs(windSpeed) * 2.5f
        val windFactorBreathing = 1.0f + abs(windSpeed) * 1.25f
        pulseTime += deltaTime * windFactorBreathing * 0.1f * dynamicsSpeed
        
        val breathingFactor = (sin(pulseTime) + 1.0f) * 0.5f
        val amplitude = 0.84375f * dynamicsSpeed
        scale = if (onlyGrows) {
            baseScale * (1.0f + breathingFactor * amplitude)
        } else {
            baseScale * (1.0f - breathingFactor * amplitude)
        }

        // Subtle vertical Y axis movement based on dynamicsSpeed
        positionY = basePositionY + sin(pulseTime * 0.5f) * 0.12f * baseScale * dynamicsSpeed

        val windThreshold = 0.1f
        val driftInfluence = (1.0f - (abs(windSpeed) / windThreshold)).coerceIn(0f, 1f)
        positionX += (windSpeed + (driftSpeed * driftInfluence)) * speedFactor * speedZFactor * deltaTime
        
        // Smoothly transition opacity - decreased by 90% (base speed from 1.5f to 0.3375f) and scaled by dynamicsSpeed (min speed 20%)
        val activeFadeSpeed = 0.3375f * windFactorOpacity * (0.2f + 0.8f * dynamicsSpeed)
        if (isFadingOut) {
            opacity = (opacity - activeFadeSpeed * deltaTime).coerceAtLeast(0f)
        } else {
            if (opacity < targetOpacity) {
                opacity = (opacity + activeFadeSpeed * deltaTime).coerceAtMost(targetOpacity)
            } else if (opacity > targetOpacity) {
                opacity = (opacity - activeFadeSpeed * deltaTime).coerceAtLeast(targetOpacity)
            }
        }

        // Individual cloud flash logic
        if (isCloudFlashEnabled && flashFrequency > 0) {
            if (nextFlashDelay <= 0f) {
                nextFlashDelay = frequencyToDelay(flashFrequency)
            }
            if (isFlashActive) {
                flashTimer += deltaTime
                if (flashTimer >= flashDuration) {
                    isFlashActive = false
                    flashIntensity = 0f
                } else {
                    // Realistic double flash effect
                    val progress = flashTimer / flashDuration
                    flashIntensity = if (progress < 0.15f) {
                        progress / 0.15f
                    } else if (progress < 0.35f) {
                        1.0f - (progress - 0.15f) / 0.20f
                    } else if (progress < 0.55f) {
                        (progress - 0.35f) / 0.20f * 0.8f
                    } else {
                        (1.0f - (progress - 0.55f) / 0.45f) * 0.8f
                    }
                }
            } else {
                timeSinceLastFlash += deltaTime
                if (timeSinceLastFlash >= nextFlashDelay) {
                    isFlashActive = true
                    flashTimer = 0f
                    val baseMin = 0.24f
                    val baseMax = 0.39f
                    flashDuration = Random.nextFloat() * (baseMax - baseMin) + baseMin
                    
                    if (flashColorIndex == 6) {
                        val r = Random.nextFloat()
                        val g = Random.nextFloat()
                        val b = Random.nextFloat()
                        val max = maxOf(r, maxOf(g, b))
                        flashColor[0] = if (max > 0f) r / max else 1.0f
                        flashColor[1] = if (max > 0f) g / max else 1.0f
                        flashColor[2] = if (max > 0f) b / max else 1.0f
                    } else {
                        val colors = arrayOf(
                            floatArrayOf(1.0f, 1.0f, 1.0f),
                            floatArrayOf(0.4f, 0.6f, 1.0f),
                            floatArrayOf(1.0f, 0.9f, 0.2f),
                            floatArrayOf(1.0f, 0.2f, 0.2f),
                            floatArrayOf(0.2f, 1.0f, 0.2f),
                            floatArrayOf(0.8f, 0.3f, 1.0f)
                        )
                        val c = colors.getOrElse(flashColorIndex) { colors[0] }
                        flashColor[0] = c[0]
                        flashColor[1] = c[1]
                        flashColor[2] = c[2]
                    }
                    timeSinceLastFlash = 0f
                    nextFlashDelay = frequencyToDelay(flashFrequency)
                }
            }
        } else {
            isFlashActive = false
            flashIntensity = 0f
            nextFlashDelay = 0f
            timeSinceLastFlash = 0f
        }
    }

    override fun render() {
        // Rendering is coordinated by StormRenderer using the StormCloud attributes
    }

    fun reset(startX: Float, aspectRatio: Float, textureCount: Int = 0) {
        z = Random.nextFloat() * 0.7f + 0.3f
        positionX = startX
        speedFactor = Random.nextFloat() * 0.4f + 0.8f // Random speed factor between 0.8 and 1.2
        
        val minScale = 0.43125f
        val maxScale = 1.40625f
        val normZ = (z - 0.3f) / 0.7f // 0.0 (background/far) to 1.0 (foreground/near)

        // Scale proportional to depth (farther = smaller, closer = larger) with some random jitter
        val scaleJitter = (Random.nextFloat() * 0.2f - 0.1f)
        baseScale = (minScale + (maxScale - minScale) * normZ + scaleJitter).coerceIn(minScale, maxScale)
        scale = baseScale
        
        // Position: smaller/farther (normZ = 0) near Y = 0.2 (lower bound of top 2/5), 
        // larger/closer (normZ = 1) higher up (Y = 0.85) for enhanced 3D depth perspective.
        // Clouds must strictly be in the top 2/5 of the screen (Y >= 0.2).
        val minY = 0.2f
        val maxY = 0.85f
        val targetY = minY + (maxY - minY) * normZ
        val jitter = (Random.nextFloat() * 2f - 1f) * 0.06f
        positionY = (targetY + jitter).coerceIn(0.2f, 1.0f)
        basePositionY = positionY
        
        targetOpacity = ((Random.nextFloat() * 0.3f + 0.55f) * 2.0f * (0.6f + 0.4f * z)).coerceAtMost(1.0f)
        opacity = targetOpacity
        isFadingOut = false
  
        driftSpeed = Random.nextFloat() * 0.06f - 0.03f
        pulseTime = Random.nextFloat() * 10f
        onlyGrows = Random.nextBoolean()

        if (textureCount > 1) {
            textureIndex = Random.nextInt(textureCount)
        }
    }
}
