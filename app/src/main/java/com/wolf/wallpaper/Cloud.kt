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
        // Place clouds in the upper half of screen (Y coordinate from 0.1 to 0.7)
        positionY = Random.nextFloat() * 0.6f + 0.1f
        speedFactor = Random.nextFloat() * 0.4f + 0.8f // Random speed factor between 0.8 and 1.2
        scale = Random.nextFloat() * 0.6f + 0.3f // Size of cloud between 0.3 and 0.9
        opacity = Random.nextFloat() * 0.4f + 0.4f // Transparency
    }
}
