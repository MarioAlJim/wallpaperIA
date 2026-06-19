package com.wolf.wallpaper.storm

import kotlin.random.Random
import com.wolf.wallpaper.core.StormObject

class WindLine(
    var positionX: Float = 0f,
    var positionY: Float = 0f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var length: Float = 0f,
    var dirX: Float = 0f,
    var dirY: Float = -1f,
    var speed: Float = 0f
) : StormObject {
    var z: Float = 1.0f
    var isActive = false
    var alphaMultiplier: Float = 0f

    override fun update(deltaTime: Float) {
        positionX += velocityX * deltaTime
        positionY += velocityY * deltaTime
    }

    override fun render() {
        // Renderizado coordinado por StormRenderer
    }

    fun updateVelocity(windAngle: Float, windIntensity: Float) {
        // Las líneas de viento se mueven rápido
        val baseSpeed = 4.0f + (windIntensity / 100f) * 4.0f
        val speedFactor = z // Más lejos -> más lento
        val currentSpeed = baseSpeed * speedFactor

        val angleRad = Math.toRadians(windAngle.toDouble()).toFloat()
        velocityY = -currentSpeed * kotlin.math.cos(angleRad)
        velocityX = currentSpeed * kotlin.math.sin(angleRad)

        val dirLength = kotlin.math.sqrt(velocityX * velocityX + velocityY * velocityY)
        dirX = if (dirLength > 0f) velocityX / dirLength else 0f
        dirY = if (dirLength > 0f) velocityY / dirLength else -1f
    }

    fun reset(aspectRatio: Float, windAngle: Float, windIntensity: Float, startOnScreen: Boolean = false) {
        z = Random.nextFloat() * 0.7f + 0.3f // z entre 0.3 y 1.0
        
        // Líneas más largas y variables que las gotas normales
        length = (Random.nextFloat() * 0.20f + 0.15f) * z
        
        // Multiplicador de opacidad según profundidad para un efecto sutil
        alphaMultiplier = z * 0.25f

        updateVelocity(windAngle, windIntensity)

        val absHorizontalTravel = if (dirY != 0f) 2.1f * kotlin.math.abs(dirX / dirY) else 0f

        if (velocityX < 0f) {
            positionX = Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - aspectRatio
        } else if (velocityX > 0f) {
            positionX = Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - (aspectRatio + absHorizontalTravel)
        } else {
            positionX = Random.nextFloat() * 2f * aspectRatio - aspectRatio
        }

        if (startOnScreen) {
            positionY = Random.nextFloat() * 2.1f - 1.05f
            if (dirY != 0f) {
                val distanceFallen = 1.05f - positionY
                positionX -= distanceFallen * (dirX / dirY)
            }
        } else {
            positionY = 1.05f
        }
        isActive = true
    }
}
