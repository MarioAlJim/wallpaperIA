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
        // Disperse clouds in the upper half of the screen (from 0.0 to 0.95)
        positionY = Random.nextFloat() * 0.95f
        speedFactor = Random.nextFloat() * 0.4f + 0.8f // Random speed factor between 0.8 and 1.2
        scale = Random.nextFloat() * 0.5f + 0.3f // Size of cloud: base size 0.3, variation 0.5 (range 0.3 to 0.8)
        opacity = Random.nextFloat() * 0.4f + 0.4f // Transparency
    }
}
