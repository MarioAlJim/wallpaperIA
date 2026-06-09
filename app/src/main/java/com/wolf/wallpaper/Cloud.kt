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

    override fun update(deltaTime: Float) {
        // Required by StormObject, we use update(deltaTime, windSpeed) in SceneManager
    }

    fun update(deltaTime: Float, windSpeed: Float) {
        positionX += windSpeed * speedFactor * deltaTime
    }

    override fun render() {
        // Rendering is coordinated by StormRenderer using the Cloud attributes
    }

    fun reset(startX: Float, aspectRatio: Float) {
        positionX = startX
        speedFactor = Random.nextFloat() * 0.4f + 0.8f // Random speed factor between 0.8 and 1.2
        scale = Random.nextFloat() * 0.5f + 0.3f // Size of cloud: base size 0.3, variation 0.5 (range 0.3 to 0.8)
        
        // Calculate Y range to keep the entire cloud body in the upper half (Y >= 0.0f and Y <= 1.0f)
        val minY = scale * 0.5f
        val maxY = 1.0f - scale * 0.5f
        positionY = if (minY < maxY) Random.nextFloat() * (maxY - minY) + minY else minY
        
        opacity = Random.nextFloat() * 0.4f + 0.4f // Transparency
    }
}
