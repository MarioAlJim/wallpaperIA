package com.wolf.wallpaper.storm

import kotlin.random.Random
import com.wolf.wallpaper.core.StormObject
import com.wolf.wallpaper.core.Cloud

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

    var z: Float = 1.0f
    var isActive = false
    var spawnX: Float = 0f
    var spawnY: Float = 1.05f
    var spawnFromCloud: Boolean = false
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
            positionX = spawnX
            positionY = spawnY
        }
        positionX += velocityX * deltaTime
        positionY += velocityY * deltaTime
    }

    override fun render() {
        // Rendering is coordinated by StormRenderer
    }

    fun updateVelocity(windAngle: Float, rainSpeed: Float) {
        val speedFactor = (0.3f + (rainSpeed / 100f) * 1.5f) * z
        val speed = baseSpeed * speedFactor
        
        val angleDeg = windAngle + angleOffset
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        velocityY = -speed * kotlin.math.cos(angleRad)
        velocityX = speed * kotlin.math.sin(angleRad)
        
        val dirLength = kotlin.math.sqrt(velocityX * velocityX + velocityY * velocityY)
        dirX = if (dirLength > 0f) velocityX / dirLength else 0f
        dirY = if (dirLength > 0f) velocityY / dirLength else -1f
    }

    fun reset(aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean = false, spawnCloud: Cloud? = null) {
        z = Random.nextFloat() * 0.8f + 0.2f
        baseSpeed = Random.nextFloat() * 1.5f + 3.0f
        angleOffset = (Random.nextFloat() * 2f - 1f) * 2.5f // +/- 2.5 degrees deviation
        
        updateVelocity(windAngle, rainSpeed)
        
        // Random length scaled by z to simulate motion blur variety with depth
        length = (Random.nextFloat() * 0.07f + 0.05f) * z
        
        spawnFromCloud = spawnCloud != null
        if (spawnCloud != null) {
            val halfWidth = spawnCloud.scale * 1.2f
            val maxOffset = halfWidth * 0.80f // Spawns only in the middle 80% of the cloud (leaves 10% margin on each side)
            spawnX = spawnCloud.positionX + (Random.nextFloat() * 2f - 1f) * maxOffset
            spawnY = spawnCloud.positionY.coerceAtMost(1.05f) // Spawn from the center of the cloud
        } else {
            // Calculate horizontal travel based on the ratio dirX / dirY (dirY is negative)
            val absHorizontalTravel = if (dirY != 0f) 2.1f * kotlin.math.abs(dirX / dirY) else 0f
            
            // Spawn at the top with proper horizontal offset depending on velocity direction
            if (velocityX < 0f) {
                spawnX = Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - aspectRatio
            } else if (velocityX > 0f) {
                spawnX = Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - (aspectRatio + absHorizontalTravel)
            } else {
                spawnX = Random.nextFloat() * 2f * aspectRatio - aspectRatio
            }
            spawnY = 1.05f
        }
        
        if (startOnScreen) {
            spawnDelay = 0f
            isActive = true
            if (spawnCloud != null) {
                val minY = -1.05f
                val maxY = spawnCloud.positionY
                positionY = if (minY < maxY) Random.nextFloat() * (maxY - minY) + minY else -1.05f
                positionX = spawnX
                if (dirY != 0f) {
                    val distanceFallen = spawnY - positionY
                    positionX -= distanceFallen * (dirX / dirY)
                }
            } else {
                // Stagger position Y across the screen height
                positionY = Random.nextFloat() * 2.1f - 1.05f
                positionX = spawnX
                // Adjust positionX based on current positionY to keep it in the diagonal flow
                if (dirY != 0f) {
                    val distanceFallen = 1.05f - positionY
                    positionX -= distanceFallen * (dirX / dirY)
                }
            }
        } else {
            spawnDelay = Random.nextFloat() * 2.0f // up to 2 seconds delay
            isActive = false
            positionX = spawnX
            positionY = 999f
        }
    }
}
