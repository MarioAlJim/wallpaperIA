package com.wolf.wallpaper.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Moon {
    var positionX = 0f
    var positionY = 0f
    var phase = 4        // 0-7, default full moon
    var pathDirection = 0 // 0=L2R, 1=R2L, 2=Static, 3=Random, 4=Chaotic
    var moveSpeed = 30   // 0-100
    var stationaryPosition = 2 // 0-5
    var customX = 50     // 0-100
    var customY = 74     // 0-100

    var hasHaloRing = kotlin.random.Random.nextFloat() < 0.5f

    // For L2R/R2L movement
    private var pathX = -1.3f

    // For random path
    private var randomStartX = 0f
    private var randomStartY = 0f
    private var randomEndX = 0f
    private var randomEndY = 0f
    private var randomProgress = 0f
    private var isRandomInitialized = false

    // For chaotic path: a Lissajous-like drift with randomized phase/frequency per instance,
    // so it wanders erratically across the screen instead of jumping edge-to-edge like Random.
    private var chaoticTime = 0f
    private var chaoticSeedX = Random.nextFloat() * 6.2832f
    private var chaoticSeedY = Random.nextFloat() * 6.2832f
    private var chaoticFreqX = 0.13f + Random.nextFloat() * 0.17f
    private var chaoticFreqY = 0.09f + Random.nextFloat() * 0.15f

    fun update(deltaTime: Float, aspectRatio: Float) {
        val speed = 0.008f + (moveSpeed / 100f) * 0.35f

        when (pathDirection) {
            0 -> { // Left to Right
                pathX += deltaTime * speed
                if (pathX > 1.3f) {
                    pathX = -1.3f
                    hasHaloRing = kotlin.random.Random.nextFloat() < 0.5f
                }
                positionX = pathX
                positionY = 0.85f - 1.15f * (positionX * positionX)
            }
            1 -> { // Right to Left
                pathX -= deltaTime * speed
                if (pathX < -1.3f) {
                    pathX = 1.3f
                    hasHaloRing = kotlin.random.Random.nextFloat() < 0.5f
                }
                positionX = pathX
                positionY = 0.85f - 1.15f * (positionX * positionX)
            }
            2 -> { // Stationary
                applyStationary(aspectRatio)
            }
            3 -> { // Random
                if (!isRandomInitialized) generateRandomPath(aspectRatio)
                val rSpeed = 0.04f + (moveSpeed / 100f) * 0.40f
                randomProgress += deltaTime * rSpeed
                if (randomProgress >= 1.0f) {
                    generateRandomPath(aspectRatio)
                    hasHaloRing = kotlin.random.Random.nextFloat() < 0.5f
                }
                positionX = randomStartX + (randomEndX - randomStartX) * randomProgress
                positionY = randomStartY + (randomEndY - randomStartY) * randomProgress
            }
            4 -> { // Chaotic
                val cSpeed = 0.2f + (moveSpeed / 100f) * 0.8f
                chaoticTime += deltaTime * cSpeed
                val rangeX = aspectRatio * 0.9f
                positionX = sin(chaoticTime * chaoticFreqX * 6.2832f + chaoticSeedX) * rangeX
                positionY = 0.45f + sin(chaoticTime * chaoticFreqY * 6.2832f + chaoticSeedY) * 0.4f
            }
        }
    }

    fun resetPath(direction: Int, aspectRatio: Float) {
        pathDirection = direction
        isRandomInitialized = false
        pathX = if (direction == 1) 1.3f else -1.3f
        if (direction == 2) applyStationary(aspectRatio)
        if (direction == 4) {
            chaoticTime = 0f
            chaoticSeedX = Random.nextFloat() * 6.2832f
            chaoticSeedY = Random.nextFloat() * 6.2832f
            chaoticFreqX = 0.13f + Random.nextFloat() * 0.17f
            chaoticFreqY = 0.09f + Random.nextFloat() * 0.15f
        }
        hasHaloRing = kotlin.random.Random.nextFloat() < 0.5f
    }

    private fun applyStationary(aspectRatio: Float) {
        when (stationaryPosition) {
            0 -> { positionX = -aspectRatio * 0.75f; positionY = 0.75f }
            1 -> { positionX = aspectRatio * 0.75f;  positionY = 0.75f }
            3 -> { positionX = -aspectRatio * 0.75f; positionY = 0.3f }
            4 -> { positionX = aspectRatio * 0.75f;  positionY = 0.3f }
            5 -> { // Custom / Free Position (Personalizado)
                positionX = -aspectRatio + (customX / 100f) * (2.0f * aspectRatio)
                val minY = -0.8f
                val maxY = 0.8f
                positionY = minY + (customY / 100f) * (maxY - minY)
            }
            else -> { positionX = 0f; positionY = 0.45f } // Center (2 and default)
        }
    }

    private fun generateRandomPath(aspectRatio: Float) {
        val startEdge = Random.nextInt(4)
        var endEdge = Random.nextInt(4)
        while (endEdge == startEdge) endEdge = Random.nextInt(4)

        fun edgeCoords(edge: Int): Pair<Float, Float> {
            val minY = 0.0f
            val maxY = 0.8f
            val xMargin = 0.25f
            return when (edge) {
                0 -> Pair(-aspectRatio - xMargin, Random.nextFloat() * (maxY - minY) + minY)
                1 -> Pair(aspectRatio + xMargin, Random.nextFloat() * (maxY - minY) + minY)
                2 -> Pair(Random.nextFloat() * (2f * aspectRatio) - aspectRatio, 1.1f)
                else -> Pair(Random.nextFloat() * (2f * aspectRatio) - aspectRatio, 0.0f)
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
