package com.wolf.wallpaper

import kotlin.random.Random

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

    override fun update(deltaTime: Float) {
        // Required by StormObject, we use update(deltaTime, windSpeed) in SceneManager
    }

    fun update(deltaTime: Float, windSpeed: Float) {
        positionX += windSpeed * speedFactor * z * deltaTime
    }

    override fun render() {
        // Rendering is coordinated by StormRenderer using the Cloud attributes
    }

    fun reset(startX: Float, aspectRatio: Float) {
        z = Random.nextFloat() * 0.7f + 0.3f
        positionX = startX
        speedFactor = Random.nextFloat() * 0.4f + 0.8f // Random speed factor between 0.8 and 1.2
        scale = (Random.nextFloat() * 0.5f + 0.3f) * z
        
        // Calculate Y range to keep the entire cloud body in the upper half (Y >= 0.0f and Y <= 1.0f)
        val minY = scale * 0.5f
        val maxY = 1.0f - scale * 0.5f
        positionY = if (minY < maxY) Random.nextFloat() * (maxY - minY) + minY else minY
        
        opacity = (Random.nextFloat() * 0.4f + 0.4f) * z
    }
}
