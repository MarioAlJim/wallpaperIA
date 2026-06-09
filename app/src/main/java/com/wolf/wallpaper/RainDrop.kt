package com.wolf.wallpaper

import kotlin.random.Random

class RainDrop(
    var positionX: Float,
    var positionY: Float,
    var velocityX: Float,
    var velocityY: Float,
    var length: Float = 0f,
    var dirX: Float = 0f,
    var dirY: Float = -1f,
    var spawnDelay: Float = 0f
) : StormObject {

    var isActive = false
    private var baseSpeed: Float = 0f
    private var angleOffset: Float = 0f

    override fun update(deltaTime: Float) {
        if (spawnDelay > 0f) {
            spawnDelay -= deltaTime
            positionY = 999f // Keep it off-screen while waiting to spawn
            return
        }
        if (!isActive) {
            isActive = true
            positionY = 1.05f // Spawn just above the screen
        }
        positionX += velocityX * deltaTime
        positionY += velocityY * deltaTime
    }

    override fun render() {
        // Rendering is coordinated by StormRenderer
    }

    fun updateVelocity(windAngle: Float, rainSpeed: Float) {
        val speedFactor = 0.3f + (rainSpeed / 100f) * 1.5f
        val speed = baseSpeed * speedFactor
        
        val angleDeg = windAngle + angleOffset
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        velocityY = -speed * kotlin.math.cos(angleRad)
        velocityX = speed * kotlin.math.sin(angleRad)
        
        val dirLength = kotlin.math.sqrt(velocityX * velocityX + velocityY * velocityY)
        dirX = if (dirLength > 0f) velocityX / dirLength else 0f
        dirY = if (dirLength > 0f) velocityY / dirLength else -1f
    }

    fun reset(aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean = false) {
        baseSpeed = Random.nextFloat() * 1.5f + 3.0f
        angleOffset = (Random.nextFloat() * 2f - 1f) * 2.5f // +/- 2.5 degrees deviation
        
        updateVelocity(windAngle, rainSpeed)
        
        // Random length to simulate motion blur variety
        length = Random.nextFloat() * 0.08f + 0.06f
        
        // Calculate horizontal travel based on the ratio dirX / dirY (dirY is negative)
        val absHorizontalTravel = if (dirY != 0f) 2.1f * kotlin.math.abs(dirX / dirY) else 0f
        
        // Spawn at the top with proper horizontal offset depending on velocity direction
        if (velocityX < 0f) {
            positionX = Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - aspectRatio
        } else if (velocityX > 0f) {
            positionX = Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - (aspectRatio + absHorizontalTravel)
        } else {
            positionX = Random.nextFloat() * 2f * aspectRatio - aspectRatio
        }
        
        if (startOnScreen) {
            spawnDelay = 0f
            isActive = true
            // Stagger position Y across the screen height
            positionY = Random.nextFloat() * 2.1f - 1.05f
            // Adjust positionX based on current positionY to keep it in the diagonal flow
            if (dirY != 0f) {
                val distanceFallen = 1.05f - positionY
                positionX -= distanceFallen * (dirX / dirY)
            }
        } else {
            spawnDelay = Random.nextFloat() * 2.0f // up to 2 seconds delay
            isActive = false
            positionY = 999f
        }
    }
}
