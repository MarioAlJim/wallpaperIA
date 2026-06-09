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
    var rotationAngle: Float = 0f
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

    fun trigger(aspectRatio: Float, textureCount: Int, colorIndex: Int) {
        isActive = true
        age = 0f
        intensity = 1.0f
        duration = kotlin.random.Random.nextFloat() * 0.15f + 0.2f // between 200ms and 350ms
        
        // Select random texture index
        selectedTextureIndex = if (textureCount > 0) kotlin.random.Random.nextInt(textureCount) else 0
        selectedColorIndex = colorIndex
        
        val startX: Float
        val startY: Float
        
        val spawnType = kotlin.random.Random.nextInt(3)
        when (spawnType) {
            0 -> { // Standard top-center
                startX = (kotlin.random.Random.nextFloat() * 1.0f - 0.5f) * aspectRatio
                startY = 1.0f
                rotationAngle = kotlin.random.Random.nextFloat() * 20f - 10f
            }
            1 -> { // Left lateral
                val startsOnSide = kotlin.random.Random.nextBoolean()
                if (startsOnSide) {
                    startX = -aspectRatio
                    startY = kotlin.random.Random.nextFloat() * 0.5f + 0.5f // upper half
                } else {
                    startX = -aspectRatio * (kotlin.random.Random.nextFloat() * 0.4f + 0.6f)
                    startY = 1.0f
                }
                rotationAngle = kotlin.random.Random.nextFloat() * 25f + 15f // shoots down-right
            }
            else -> { // Right lateral
                val startsOnSide = kotlin.random.Random.nextBoolean()
                if (startsOnSide) {
                    startX = aspectRatio
                    startY = kotlin.random.Random.nextFloat() * 0.5f + 0.5f // upper half
                } else {
                    startX = aspectRatio * (kotlin.random.Random.nextFloat() * 0.4f + 0.6f)
                    startY = 1.0f
                }
                rotationAngle = -(kotlin.random.Random.nextFloat() * 25f + 15f) // shoots down-left
            }
        }

        val rad = Math.toRadians(rotationAngle.toDouble())
        scaleY = (2.0 / Math.cos(rad)).toFloat().coerceIn(2.0f, 2.8f)
        scaleX = kotlin.random.Random.nextFloat() * 0.4f + 0.5f // width of the bolt

        positionX = startX
        positionY = startY - scaleY * 0.5f
    }
}
