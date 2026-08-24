package com.wolf.wallpaper.storm

import kotlin.random.Random
import com.wolf.wallpaper.core.StormObject

class Lightning(
    var positionX: Float = 0f,
    var positionY: Float = 0f,
    var scaleX: Float = 1.0f,
    var scaleY: Float = 1.0f,
    var duration: Float = 0.25f,
    var intensity: Float = 1.0f,
    var selectedTextureIndex: Int = 0,
    var selectedColorIndex: Int = 0,
    var rotationAngle: Float = 0f,
    var growthProgress: Float = 1.0f,
    var isInternalOnly: Boolean = false
) : StormObject {

    var customColor: FloatArray? = null
    var parentCloud: StormCloud? = null
    var z: Float = 1.0f
    var relativeOffsetX: Float = 0f
    var relativeOffsetY: Float = 0f

    private var age = 0f
    var isActive = false
        private set
    override fun update(deltaTime: Float) {
        if (!isActive) return

        val parent = parentCloud
        if (parent != null && !parent.isFadingOut) {
            positionX = parent.positionX + relativeOffsetX
            positionY = (parent.positionY + relativeOffsetY) - scaleY * 0.5f
            z = parent.z
        }

        age += deltaTime
        if (age >= duration) {
            isActive = false
            parentCloud = null
        } else {
            // Calculate growth progress (takes first 20% of duration)
            val growthFraction = 0.20f
            growthProgress = if (duration > 0f) {
                (age / (duration * growthFraction)).coerceIn(0f, 1f)
            } else {
                1f
            }

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
    fun trigger(aspectRatio: Float, textureCount: Int, colorIndex: Int, durationPercentage: Int = 30, isInternalOnly: Boolean = false) {
        this.isInternalOnly = isInternalOnly
        isActive = true
        age = 0f
        intensity = 1.0f
        growthProgress = 0.0f
        parentCloud = null
        z = 1.0f
        relativeOffsetX = 0f
        relativeOffsetY = 0f
        
        val baseMin = if (durationPercentage <= 50) {
            0.15f + (durationPercentage / 50f) * 0.15f
        } else {
            0.30f + ((durationPercentage - 50) / 50f) * 0.70f
        }
        val baseMax = baseMin + 0.15f
        duration = kotlin.random.Random.nextFloat() * (baseMax - baseMin) + baseMin
        
        // Select alternating texture index
        selectedTextureIndex = getNextTextureIndex(textureCount)
        selectedColorIndex = colorIndex
        if (colorIndex == 6) {
            val r = kotlin.random.Random.nextFloat()
            val g = kotlin.random.Random.nextFloat()
            val b = kotlin.random.Random.nextFloat()
            val max = maxOf(r, maxOf(g, b))
            val normR = if (max > 0f) r / max else 1.0f
            val normG = if (max > 0f) g / max else 1.0f
            val normB = if (max > 0f) b / max else 1.0f
            customColor = floatArrayOf(normR, normG, normB, 1.0f)
        } else {
            customColor = null
        }
        
        val startX: Float
        val startY: Float
        
        val borderType = kotlin.random.Random.nextInt(3)
        when (borderType) {
            0 -> { // Top border
                startX = (kotlin.random.Random.nextFloat() * 2.0f - 1.0f) * aspectRatio
                startY = 1.0f
                rotationAngle = kotlin.random.Random.nextFloat() * 30f - 15f // -15 to +15 degrees
            }
            1 -> { // Left border
                startX = -aspectRatio
                startY = kotlin.random.Random.nextFloat() * 0.6f + 0.3f // upper part: 0.3 to 0.9
                rotationAngle = kotlin.random.Random.nextFloat() * 15f + 5f // +5 to +20 degrees (shoots down-right)
            }
            else -> { // Right border
                startX = aspectRatio
                startY = kotlin.random.Random.nextFloat() * 0.6f + 0.3f // upper part: 0.3 to 0.9
                rotationAngle = -(kotlin.random.Random.nextFloat() * 15f + 5f) // -5 to -20 degrees (shoots down-left)
            }
        }

        val minHeight = 2.0f * 0.30f // 30% of screen height (0.60f)
        val maxHeight = 2.0f * 1.50f // 150% of screen height (3.00f)
        scaleY = kotlin.random.Random.nextFloat() * (maxHeight - minHeight) + minHeight
        
        // Proper aspect ratio to prevent stretching
        scaleX = scaleY * 0.60f

        // Adjust positionX so that the lightning is cut off but always visible
        val maxOffset = (scaleX * 0.25f).coerceAtMost(aspectRatio * 0.5f)
        positionX = when (borderType) {
            1 -> -aspectRatio + maxOffset // Left border
            2 -> aspectRatio - maxOffset // Right border
            else -> startX.coerceIn(-aspectRatio + maxOffset, aspectRatio - maxOffset) // Top border
        }
        positionY = startY - scaleY * 0.5f
    }

    fun triggerAt(touchX: Float, touchY: Float, aspectRatio: Float, textureCount: Int, colorIndex: Int, durationPercentage: Int = 30, isInternalOnly: Boolean = false) {
        this.isInternalOnly = isInternalOnly
        isActive = true
        age = 0f
        intensity = 1.0f
        growthProgress = 0.0f
        parentCloud = null
        z = 1.0f
        relativeOffsetX = 0f
        relativeOffsetY = 0f
        
        val baseMin = if (durationPercentage <= 50) {
            0.15f + (durationPercentage / 50f) * 0.15f
        } else {
            0.30f + ((durationPercentage - 50) / 50f) * 0.70f
        }
        val baseMax = baseMin + 0.15f
        duration = kotlin.random.Random.nextFloat() * (baseMax - baseMin) + baseMin
        
        selectedTextureIndex = getNextTextureIndex(textureCount)
        selectedColorIndex = colorIndex
        if (colorIndex == 6) {
            val r = kotlin.random.Random.nextFloat()
            val g = kotlin.random.Random.nextFloat()
            val b = kotlin.random.Random.nextFloat()
            val max = maxOf(r, maxOf(g, b))
            val normR = if (max > 0f) r / max else 1.0f
            val normG = if (max > 0f) g / max else 1.0f
            val normB = if (max > 0f) b / max else 1.0f
            customColor = floatArrayOf(normR, normG, normB, 1.0f)
        } else {
            customColor = null
        }

        val minHeight = 2.0f * 0.30f // 30% of screen height (0.60f)
        val maxHeight = 2.0f * 1.50f // 150% of screen height (3.00f)

        val borderType = if (touchY > 0.4f) {
            0 // Top border only
        } else {
            val rand = kotlin.random.Random.nextInt(2)
            if (touchX < -aspectRatio / 3f) {
                if (rand == 0) 0 else 1 // Top or Left
            } else if (touchX > aspectRatio / 3f) {
                if (rand == 0) 0 else 2 // Top or Right
            } else {
                0
            }
        }

        val startX: Float
        val startY: Float

        when (borderType) {
            0 -> { // Top border
                rotationAngle = kotlin.random.Random.nextFloat() * 30f - 15f // -15 to +15 degrees
                val rad = Math.toRadians(rotationAngle.toDouble()).toFloat()
                scaleY = ((1.0f - touchY) / kotlin.math.cos(rad)).coerceIn(minHeight, maxHeight)
                startX = touchX - scaleY * kotlin.math.sin(rad)
                startY = 1.0f
            }
            1 -> { // Left border
                rotationAngle = kotlin.random.Random.nextFloat() * 15f + 10f // +10 to +25 degrees (shoots down-right)
                val rad = Math.toRadians(rotationAngle.toDouble()).toFloat()
                scaleY = ((touchX + aspectRatio) / kotlin.math.sin(rad)).coerceIn(minHeight, maxHeight)
                startY = (touchY + scaleY * kotlin.math.cos(rad)).coerceIn(0.3f, 1.0f)
                startX = -aspectRatio
            }
            else -> { // Right border
                rotationAngle = -(kotlin.random.Random.nextFloat() * 15f + 10f) // -10 to -25 degrees (shoots down-left)
                val rad = Math.toRadians(rotationAngle.toDouble()).toFloat()
                scaleY = ((touchX - aspectRatio) / kotlin.math.sin(rad)).coerceIn(minHeight, maxHeight)
                startY = (touchY + scaleY * kotlin.math.cos(rad)).coerceIn(0.3f, 1.0f)
                startX = aspectRatio
            }
        }

        // Proper aspect ratio to prevent stretching
        scaleX = scaleY * 0.60f

        // Adjust positionX so that the lightning is cut off but always visible
        val maxOffset = (scaleX * 0.25f).coerceAtMost(aspectRatio * 0.5f)
        positionX = when (borderType) {
            1 -> -aspectRatio + maxOffset // Left border
            2 -> aspectRatio - maxOffset // Right border
            else -> startX.coerceIn(-aspectRatio + maxOffset, aspectRatio - maxOffset) // Top border
        }
        positionY = startY - scaleY * 0.5f
    }

    fun triggerFromCloud(
        cloud: StormCloud,
        aspectRatio: Float,
        textureCount: Int,
        colorIndex: Int,
        durationPercentage: Int = 30,
        isInternalOnly: Boolean = false
    ) {
        this.isInternalOnly = isInternalOnly
        isActive = true
        age = 0f
        intensity = 1.0f
        growthProgress = 0.0f
        
        val baseMin = if (durationPercentage <= 50) {
            0.15f + (durationPercentage / 50f) * 0.15f
        } else {
            0.30f + ((durationPercentage - 50) / 50f) * 0.70f
        }
        val baseMax = baseMin + 0.15f
        duration = kotlin.random.Random.nextFloat() * (baseMax - baseMin) + baseMin
        
        selectedTextureIndex = getNextTextureIndex(textureCount)
        selectedColorIndex = colorIndex
        if (colorIndex == 6) {
            val r = kotlin.random.Random.nextFloat()
            val g = kotlin.random.Random.nextFloat()
            val b = kotlin.random.Random.nextFloat()
            val max = maxOf(r, maxOf(g, b))
            val normR = if (max > 0f) r / max else 1.0f
            val normG = if (max > 0f) g / max else 1.0f
            val normB = if (max > 0f) b / max else 1.0f
            customColor = floatArrayOf(normR, normG, normB, 1.0f)
        } else {
            customColor = null
        }
        
        parentCloud = cloud
        z = cloud.z
        
        // Center with slight random offset under the cloud
        val cloudWidth = cloud.scale * 2.4f
        relativeOffsetX = (kotlin.random.Random.nextFloat() * 2.0f - 1.0f) * (cloudWidth * 0.25f)
        relativeOffsetY = -cloud.scale * 0.1f // spawn slightly below the center
        
        positionX = cloud.positionX + relativeOffsetX
        positionY = cloud.positionY + relativeOffsetY
        
        // Scale vertical length based on depth z
        val minHeight = 2.0f * 0.30f * z
        val maxHeight = 2.0f * 1.50f * z
        scaleY = (kotlin.random.Random.nextFloat() * (maxHeight - minHeight) + minHeight).coerceAtLeast(0.15f)
        
        // Proper aspect ratio to prevent stretching
        scaleX = scaleY * 0.60f
        
        rotationAngle = kotlin.random.Random.nextFloat() * 20f - 10f // -10 to +10 degrees
        
        // Adjust positionY so that the lightning shoots downwards from its starting point
        positionY = positionY - scaleY * 0.5f
    }

    companion object {
        private var nextTextureIndex = 0

        @Synchronized
        fun getNextTextureIndex(textureCount: Int): Int {
            if (textureCount <= 0) return 0
            val index = nextTextureIndex
            nextTextureIndex = (nextTextureIndex + 1) % textureCount
            return index
        }
    }
}
