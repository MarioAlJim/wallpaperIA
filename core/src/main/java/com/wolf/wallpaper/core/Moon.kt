package com.wolf.wallpaper.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Moon {
    var positionX = 0f
    var positionY = 0f
    var phase = 4        // 0-7, default full moon
    var pathDirection = 0 // 0=L2R, 1=R2L, 2=Static, 3=Random
    var moveSpeed = 30   // 0-100
    var stationaryPosition = 2 // 0-5

    // For L2R/R2L movement
    private var pathX = -1.3f

    // For random path
    private var randomStartX = 0f
    private var randomStartY = 0f
    private var randomEndX = 0f
    private var randomEndY = 0f
    private var randomProgress = 0f
    private var isRandomInitialized = false

    fun update(deltaTime: Float, aspectRatio: Float) {
        val speed = 0.008f + (moveSpeed / 100f) * 0.35f

        when (pathDirection) {
            0 -> { // Left to Right
                pathX += deltaTime * speed
                if (pathX > 1.3f) pathX = -1.3f
                positionX = pathX
                positionY = 0.9f - 1.2f * (positionX * positionX)
            }
            1 -> { // Right to Left
                pathX -= deltaTime * speed
                if (pathX < -1.3f) pathX = 1.3f
                positionX = pathX
                positionY = 0.9f - 1.2f * (positionX * positionX)
            }
            2 -> { // Stationary
                applyStationary(aspectRatio)
            }
            3 -> { // Random
                if (!isRandomInitialized) generateRandomPath(aspectRatio)
                val rSpeed = 0.04f + (moveSpeed / 100f) * 0.40f
                randomProgress += deltaTime * rSpeed
                if (randomProgress >= 1.0f) generateRandomPath(aspectRatio)
                positionX = randomStartX + (randomEndX - randomStartX) * randomProgress
                positionY = randomStartY + (randomEndY - randomStartY) * randomProgress
            }
        }
    }

    fun resetPath(direction: Int, aspectRatio: Float) {
        pathDirection = direction
        isRandomInitialized = false
        pathX = if (direction == 1) 1.3f else -1.3f
        if (direction == 2) applyStationary(aspectRatio)
    }

    private fun applyStationary(aspectRatio: Float) {
        when (stationaryPosition) {
            0 -> { positionX = -0.9f; positionY = 0.85f / aspectRatio }
            1 -> { positionX = 0.9f;  positionY = 0.85f / aspectRatio }
            3 -> { positionX = -0.75f; positionY = 0.3f }
            4 -> { positionX = 0.75f;  positionY = 0.3f }
            else -> { positionX = 0f; positionY = 0.5f } // Center (2 and default)
        }
    }

    private fun generateRandomPath(aspectRatio: Float) {
        val startEdge = Random.nextInt(4)
        var endEdge = Random.nextInt(4)
        while (endEdge == startEdge) endEdge = Random.nextInt(4)

        fun edgeCoords(edge: Int): Pair<Float, Float> {
            val maxY = 1.0f / aspectRatio
            return when (edge) {
                0 -> Pair(-1.5f, Random.nextFloat() * (maxY + 0.8f) - 0.8f)
                1 -> Pair(1.5f,  Random.nextFloat() * (maxY + 0.8f) - 0.8f)
                2 -> Pair(Random.nextFloat() * 2.4f - 1.2f, maxY + 0.1f)
                else -> Pair(Random.nextFloat() * 2.4f - 1.2f, -1.2f)
            }
        }

        val (sx, sy) = edgeCoords(startEdge)
        val (ex, ey) = edgeCoords(endEdge)
        randomStartX = sx; randomStartY = sy
        randomEndX = ex;   randomEndY = ey
        randomProgress = 0f
        isRandomInitialized = true
    }
}
