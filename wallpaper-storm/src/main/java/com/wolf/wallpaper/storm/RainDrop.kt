package com.wolf.wallpaper.storm

import kotlin.random.Random
import com.wolf.wallpaper.core.StormObject
import com.wolf.wallpaper.storm.StormCloud

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
    var parentCloud: StormCloud? = null
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

    fun reset(aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean = false, spawnCloud: StormCloud? = null, allowFallbackToEdge: Boolean = true) {
        if (spawnCloud != null) {
            resetCloudRain(spawnCloud, aspectRatio, windAngle, rainSpeed, startOnScreen)
        } else {
            resetEdgeRain(aspectRatio, windAngle, rainSpeed, startOnScreen, allowFallbackToEdge)
        }
    }

    private fun resetCloudRain(spawnCloud: StormCloud, aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean) {
        z = spawnCloud.z
        baseSpeed = Random.nextFloat() * 1.5f + 3.0f
        angleOffset = (Random.nextFloat() * 2f - 1f) * 2.5f // +/- 2.5 degrees deviation
        
        updateVelocity(windAngle, rainSpeed)
        
        length = spawnCloud.baseScale * 0.168f * z // 0.12 * 1.4 = 40% longer
        
        parentCloud = spawnCloud
        spawnFromCloud = true
        
        val halfWidth = spawnCloud.scale * 1.2f
        val offsetPercent = Random.nextFloat() * 0.50f + 0.25f // 25% to 75% of half-width
        val sign = if (Random.nextBoolean()) 1f else -1f
        spawnX = spawnCloud.positionX + sign * halfWidth * offsetPercent
        spawnY = spawnCloud.positionY.coerceAtMost(1.05f)
        
        if (startOnScreen) {
            spawnDelay = 0f
            isActive = true
            val minY = -1.05f
            val maxY = spawnCloud.positionY
            positionY = if (minY < maxY) Random.nextFloat() * (maxY - minY) + minY else -1.05f
            positionX = spawnX
            if (dirY != 0f) {
                val distanceFallen = spawnY - positionY
                positionX -= distanceFallen * (dirX / dirY)
            }
        } else {
            spawnDelay = Random.nextFloat() * 2.0f
            isActive = false
            positionX = spawnX
            positionY = 999f
        }
    }

    private fun resetEdgeRain(aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean, allowFallbackToEdge: Boolean) {
        z = Random.nextFloat() * 0.8f + 0.2f
        baseSpeed = Random.nextFloat() * 1.5f + 3.0f
        angleOffset = (Random.nextFloat() * 2f - 1f) * 2.5f
        
        updateVelocity(windAngle, rainSpeed)
        
        // Base length for edge-rain drops (increased 40%)
        length = (Random.nextFloat() * 0.0525f + 0.030f) * 1.4f * z
        
        parentCloud = null
        spawnFromCloud = false
        
        if (!allowFallbackToEdge) {
            length = 0f
            positionY = 999f
            spawnY = 999f
            spawnDelay = Random.nextFloat() * 0.5f + 0.1f // check again soon
            isActive = false
            spawnFromCloud = true
            return
        }
        
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
        
        if (startOnScreen) {
            spawnDelay = 0f
            isActive = true
            // Stagger position Y across the screen height
            positionY = Random.nextFloat() * 2.1f - 1.05f
            positionX = spawnX
            // Adjust positionX based on current positionY to keep it in the diagonal flow
            if (dirY != 0f) {
                val distanceFallen = 1.05f - positionY
                positionX -= distanceFallen * (dirX / dirY)
            }
        } else {
            spawnDelay = Random.nextFloat() * 2.0f // up to 2 seconds delay
            isActive = false
            positionX = spawnX
            positionY = 999f
        }
    }
}
