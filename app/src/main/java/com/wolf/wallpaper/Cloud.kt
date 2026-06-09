package com.wolf.wallpaper

import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.abs

class Cloud(
    val id: Int,
    var positionX: Float,
    var positionY: Float,
    var speedFactor: Float,
    var scale: Float,
    var opacity: Float,
    val textureIndex: Int
) : StormObject {

    var z: Float = 1.0f
    var targetOpacity: Float = opacity
    var isFadingOut: Boolean = false

    var driftSpeed: Float = 0f
    var baseScale: Float = scale
    var pulseTime: Float = 0f

    val speedZFactor: Float
        get() = 0.225f + ((z - 0.3f) / 0.7f) * 1.025f

    override fun update(deltaTime: Float) {
        // Required by StormObject, we use update(deltaTime, windSpeed) in SceneManager
    }

    fun update(deltaTime: Float, windSpeed: Float) {
        val windFactorOpacity = 1.0f + abs(windSpeed) * 2.5f
        val windFactorBreathing = 1.0f + abs(windSpeed) * 1.25f
        pulseTime += deltaTime * windFactorBreathing
        scale = baseScale * (1.0f + sin(pulseTime) * 0.08f)

        positionX += (windSpeed + driftSpeed) * speedFactor * speedZFactor * deltaTime
        
        // Smoothly transition opacity
        val activeFadeSpeed = 1.5f * windFactorOpacity
        if (isFadingOut) {
            opacity = (opacity - activeFadeSpeed * deltaTime).coerceAtLeast(0f)
        } else {
            if (opacity < targetOpacity) {
                opacity = (opacity + activeFadeSpeed * deltaTime).coerceAtMost(targetOpacity)
            } else if (opacity > targetOpacity) {
                opacity = (opacity - activeFadeSpeed * deltaTime).coerceAtLeast(targetOpacity)
            }
        }
    }

    override fun render() {
        // Rendering is coordinated by StormRenderer using the Cloud attributes
    }

    fun reset(startX: Float, aspectRatio: Float) {
        z = Random.nextFloat() * 0.7f + 0.3f
        positionX = startX
        speedFactor = Random.nextFloat() * 0.4f + 0.8f // Random speed factor between 0.8 and 1.2
        
        val minScale = 0.345f
        val maxScale = 1.25f
        baseScale = (Random.nextFloat() * (maxScale - minScale) + minScale) * z
        scale = baseScale
        
        // Calculate Y range to keep the entire cloud body in the upper half (Y >= 0.0f and Y <= 1.0f)
        val minY = scale * 0.5f
        val maxY = 1.0f - scale * 0.5f
        positionY = if (minY < maxY) Random.nextFloat() * (maxY - minY) + minY else minY
        
        targetOpacity = (Random.nextFloat() * 0.4f + 0.4f) * z
        opacity = targetOpacity
        isFadingOut = false

        driftSpeed = Random.nextFloat() * 0.06f - 0.03f
        pulseTime = Random.nextFloat() * 10f
    }
}
