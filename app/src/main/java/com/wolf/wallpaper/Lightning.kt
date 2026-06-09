package com.wolf.wallpaper

import kotlin.random.Random

class Lightning(
    var positionX: Float = 0f,
    var positionY: Float = 0f,
    var scaleX: Float = 1.0f,
    var scaleY: Float = 1.0f,
    var duration: Float = 0.25f,
    var intensity: Float = 1.0f,
    var selectedTextureIndex: Int = 0,
    var selectedColorIndex: Int = 0,
    var rotationAngle: Float = 0f,
    var growthProgress: Float = 1.0f,
    var isInternalOnly: Boolean = false
) : StormObject {

    private var age = 0f
    var isActive = false
        private set

    override fun update(deltaTime: Float) {
        if (!isActive) return
        age += deltaTime
        if (age >= duration) {
            isActive = false
        } else {
            // Calculate growth progress (takes first 20% of duration)
            val growthFraction = 0.20f
            growthProgress = if (duration > 0f) {
                (age / (duration * growthFraction)).coerceIn(0f, 1f)
            } else {
                1f
            }

            // Realistic double flash effect
            val progress = age / duration
            intensity = if (progress < 0.15f) {
                progress / 0.15f // First quick ramp-up
            } else if (progress < 0.35f) {
                1.0f - (progress - 0.15f) / 0.20f // First decay
            } else if (progress < 0.55f) {
                (progress - 0.35f) / 0.20f * 0.8f // Second smaller peak
            } else {
                (1.0f - (progress - 0.55f) / 0.45f) * 0.8f // Final slow decay
            }
        }
    }

    override fun render() {
        // Coordinated by StormRenderer
    }

    fun trigger(aspectRatio: Float, textureCount: Int, colorIndex: Int, durationPercentage: Int = 30, isInternalOnly: Boolean = false) {
        this.isInternalOnly = isInternalOnly
        isActive = true
        age = 0f
        intensity = 1.0f
        growthProgress = 0.0f
        
        val baseMin = if (durationPercentage <= 50) {
            0.15f + (durationPercentage / 50f) * 0.15f
        } else {
            0.30f + ((durationPercentage - 50) / 50f) * 0.70f
        }
        val baseMax = baseMin + 0.15f
        duration = kotlin.random.Random.nextFloat() * (baseMax - baseMin) + baseMin
        
        // Select random texture index
        selectedTextureIndex = if (textureCount > 0) kotlin.random.Random.nextInt(textureCount) else 0
        selectedColorIndex = colorIndex
        
        val startX: Float
        val startY: Float
        
        val borderType = kotlin.random.Random.nextInt(3)
        when (borderType) {
            0 -> { // Top border
                startX = (kotlin.random.Random.nextFloat() * 2.0f - 1.0f) * aspectRatio
                startY = 1.0f
                rotationAngle = kotlin.random.Random.nextFloat() * 30f - 15f // -15 to +15 degrees
            }
            1 -> { // Left border
                startX = -aspectRatio
                startY = kotlin.random.Random.nextFloat() * 0.6f + 0.3f // upper part: 0.3 to 0.9
                rotationAngle = kotlin.random.Random.nextFloat() * 25f + 20f // +20 to +45 degrees (shoots down-right)
            }
            else -> { // Right border
                startX = aspectRatio
                startY = kotlin.random.Random.nextFloat() * 0.6f + 0.3f // upper part: 0.3 to 0.9
                rotationAngle = -(kotlin.random.Random.nextFloat() * 25f + 20f) // -20 to -45 degrees (shoots down-left)
            }
        }

        val minHeight = 2.0f * 0.15f // 15% of screen height (0.3f)
        val maxHeight = 2.0f * 0.75f // 75% of screen height (1.5f)
        scaleY = kotlin.random.Random.nextFloat() * (maxHeight - minHeight) + minHeight
        scaleX = kotlin.random.Random.nextFloat() * 0.225f + 0.225f // width of the bolt: 0.225 to 0.45

        positionX = startX
        positionY = startY - scaleY * 0.5f
    }
}
