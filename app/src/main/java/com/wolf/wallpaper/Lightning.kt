package com.wolf.wallpaper

import kotlin.random.Random

class Lightning(
    var positionX: Float = 0f,
    var positionY: Float = 0f,
    var scaleX: Float = 1.0f,
    var scaleY: Float = 1.0f,
    var duration: Float = 0.25f,
    var intensity: Float = 1.0f,
    var selectedTextureIndex: Int = 0
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

    fun trigger(aspectRatio: Float, textureCount: Int) {
        isActive = true
        age = 0f
        intensity = 1.0f
        duration = Random.nextFloat() * 0.15f + 0.2f // between 200ms and 350ms
        
        // Select random texture index
        selectedTextureIndex = if (textureCount > 0) Random.nextInt(textureCount) else 0
        
        // Spans the full height (scaleY = 2f, positionY = 0f)
        positionX = (Random.nextFloat() * 1.4f - 0.7f) * aspectRatio
        positionY = 0f
        scaleY = 2f
        scaleX = Random.nextFloat() * 0.4f + 0.5f // width of the bolt
    }
}
