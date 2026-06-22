package com.wolf.wallpaper.storm

import kotlin.random.Random
import com.wolf.wallpaper.core.StormObject

class WindLine(
    var positionX: Float = 0f,
    var positionY: Float = 0f,
    var length: Float = 0f,
    var dirX: Float = 0f,
    var dirY: Float = 0f,
    var speed: Float = 0f,
    var z: Float = 1f,
    var alphaMultiplier: Float = 1f,
    var lifetime: Float = 0f,
    var maxLifetime: Float = 0f,
    var isActive: Boolean = false
) : StormObject {

    override fun update(deltaTime: Float) {
        if (!isActive) return
        
        lifetime += deltaTime
        if (lifetime >= maxLifetime) {
            isActive = false
            return
        }

        // Move line horizontally/diagonally in wind direction
        positionX += dirX * speed * deltaTime
        positionY += dirY * speed * deltaTime

        // Dynamic stretch: length grows at the beginning, stays, then shrinks at the end
        val progress = lifetime / maxLifetime
        val baseLength = 0.3f + z * 0.4f // longer lines closer to viewer
        length = when {
            progress < 0.2f -> baseLength * (progress / 0.2f) // stretch out
            progress > 0.8f -> baseLength * ((1f - progress) / 0.2f) // shrink back
            else -> baseLength
        }
    }

    override fun render() {
        // Rendered by StormRenderer
    }

    fun reset(aspectRatio: Float, windAngle: Float, windIntensity: Int) {
        z = Random.nextFloat() * 0.8f + 0.2f // depth layer 0.2 to 1.0
        
        // Spawn scattered vertically on screen, mostly in top 2/3 where nubes/rain start
        positionY = Random.nextFloat() * 1.5f - 0.5f // from -0.5 to 1.0
        
        val angleRad = Math.toRadians(windAngle.toDouble()).toFloat()
        // Direction of wind: lines are horizontal but slightly tilted following wind angle
        dirX = kotlin.math.sin(angleRad)
        dirY = -kotlin.math.cos(angleRad)

        // Force dirX to be horizontal (mostly) or go in wind direction
        if (kotlin.math.abs(dirX) < 0.1f) {
            dirX = if (Random.nextBoolean()) 1f else -1f
            dirY = 0f
        } else {
            // Normalize to make it horizontal direction mostly
            val len = kotlin.math.sqrt(dirX * dirX + dirY * dirY)
            if (len > 0f) {
                dirX /= len
                dirY /= len
            }
            // Slightly tilt lines with wind angle but keep them mostly horizontal
            dirY = dirY * 0.2f
            val len2 = kotlin.math.sqrt(dirX * dirX + dirY * dirY)
            if (len2 > 0f) {
                dirX /= len2
                dirY /= len2
            }
        }

        // Speed depends on wind intensity and depth
        speed = (1.5f + (windIntensity / 100f) * 3.5f) * (0.6f + 0.4f * z)

        // Starting position: spawn off-screen on the opposite side of wind direction so it travels across
        if (dirX > 0f) {
            positionX = -aspectRatio - 0.5f
        } else {
            positionX = aspectRatio + 0.5f
        }

        lifetime = 0f
        maxLifetime = 0.8f + Random.nextFloat() * 1.2f // 0.8 to 2.0 seconds
        alphaMultiplier = 0.25f + Random.nextFloat() * 0.35f // subtle semi-transparent line
        isActive = true
        length = 0f
    }
}
