package com.wolf.wallpaper

import kotlin.random.Random

class Cloud(
    val id: Int,
    var positionX: Float,
    var positionY: Float,
    var speed: Float,
    var scale: Float,
    var opacity: Float,
    val textureIndex: Int
) : StormObject {

    override fun update(deltaTime: Float) {
        positionX += speed * deltaTime
    }

    override fun render() {
        // Rendering is coordinated by StormRenderer using the Cloud attributes
    }

    fun reset(startX: Float, aspectRatio: Float) {
        positionX = startX
        // Place clouds in the upper half of screen (Y coordinate from 0.1 to 0.7)
        positionY = Random.nextFloat() * 0.6f + 0.1f
        speed = Random.nextFloat() * 0.08f + 0.03f // Random slow movement speed
        scale = Random.nextFloat() * 0.4f + 0.4f // Size of cloud
        opacity = Random.nextFloat() * 0.4f + 0.4f // Transparency
    }
}
