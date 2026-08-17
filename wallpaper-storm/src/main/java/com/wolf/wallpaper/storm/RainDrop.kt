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

    companion object {
        // Cloud-spawned drops scale with both cloud scale and depth (z), which otherwise
        // makes drops from close-up (high z) clouds look oversized. Cap their length at the
        // same maximum edge-rain drops can reach — (0.16422 + 0.04368) * zMax(1.0), see
        // resetEdgeRain() below — so neither source can produce a bigger drop than the other.
        private const val MAX_CLOUD_RAIN_LENGTH = 0.2079f

        // updateVelocity() runs for every active raindrop (up to MAX_RAIN_DROP_COUNT) every
        // frame. Math.toRadians() takes/returns Double, so calling it here would promote to
        // double precision and back for no benefit — this constant keeps the conversion in
        // Float end to end.
        private const val DEG_TO_RAD_F = (Math.PI / 180.0).toFloat()
    }

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
        val speedFactor = 0.3f + (rainSpeed / 100f) * 0.6f
        val speed = baseSpeed * speedFactor
        
        val angleDeg = windAngle + angleOffset
        val angleRad = angleDeg * DEG_TO_RAD_F
        val sinA = kotlin.math.sin(angleRad)
        val cosA = kotlin.math.cos(angleRad)
        
        velocityY = -speed * cosA
        velocityX = speed * sinA
        
        // dirLength mathematically equals speed. Since speed is positive (baseSpeed > 0),
        // we can cancel out speed in velocityX/dirLength and velocityY/dirLength,
        // avoiding a sqrt() and division calculations on every raindrop.
        dirX = sinA
        dirY = -cosA
    }

    fun reset(aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean = false, spawnCloud: StormCloud? = null, allowFallbackToEdge: Boolean = true, existingSpawnX: List<Float> = emptyList()) {
        if (spawnCloud != null) {
            resetCloudRain(spawnCloud, aspectRatio, windAngle, rainSpeed, startOnScreen, existingSpawnX)
        } else {
            resetEdgeRain(aspectRatio, windAngle, rainSpeed, startOnScreen, allowFallbackToEdge, existingSpawnX)
        }
    }

    private fun resetCloudRain(spawnCloud: StormCloud, aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean, existingSpawnX: List<Float>) {
        z = spawnCloud.z
        baseSpeed = Random.nextFloat() * 1.5f + 3.0f
        angleOffset = (Random.nextFloat() * 2f - 1f) * 2.5f // +/- 2.5 degrees deviation
        
        updateVelocity(windAngle, rainSpeed)
        
        length = (spawnCloud.baseScale * 0.3024f * z).coerceAtMost(MAX_CLOUD_RAIN_LENGTH) // 0.2016 * 1.5 = 0.3024 (capped for close-up clouds)
        
        parentCloud = spawnCloud
        spawnFromCloud = true
        
        val halfWidth = spawnCloud.scale * 1.2f
        
        // Mitchell's Best-Candidate algorithm to find best spawnX to avoid overlapping paths
        var bestSpawnX = spawnCloud.positionX
        var bestMinDist = -1f
        val numCandidates = 5
        for (cand in 0 until numCandidates) {
            val offsetPercent = Random.nextFloat() * 2.0f - 1.0f // Uniformly between -1 and 1
            val candidateX = spawnCloud.positionX + halfWidth * offsetPercent
            
            if (existingSpawnX.isEmpty()) {
                bestSpawnX = candidateX
                break
            }
            
            var minDist = Float.MAX_VALUE
            for (otherX in existingSpawnX) {
                val dist = kotlin.math.abs(candidateX - otherX)
                if (dist < minDist) {
                    minDist = dist
                }
            }
            
            if (minDist > bestMinDist) {
                bestMinDist = minDist
                bestSpawnX = candidateX
            }
        }
        
        spawnX = bestSpawnX
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

    private fun resetEdgeRain(aspectRatio: Float, windAngle: Float, rainSpeed: Float, startOnScreen: Boolean, allowFallbackToEdge: Boolean, existingSpawnX: List<Float>) {
        z = Random.nextFloat() * 0.8f + 0.2f
        baseSpeed = Random.nextFloat() * 1.5f + 3.0f
        angleOffset = (Random.nextFloat() * 2f - 1f) * 2.5f
        
        updateVelocity(windAngle, rainSpeed)
        
        // Base length for edge-rain drops: min increased by 30% to 0.04368, max increased by another 50% to 0.2079.
        length = (Random.nextFloat() * 0.16422f + 0.04368f) * z
        
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
        
        // Mitchell's Best-Candidate algorithm to find best spawnX to avoid overlapping paths
        var bestSpawnX = 0f
        var bestMinDist = -1f
        val numCandidates = 5
        for (cand in 0 until numCandidates) {
            val candidateX = if (velocityX < 0f) {
                Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - aspectRatio
            } else if (velocityX > 0f) {
                Random.nextFloat() * (2f * aspectRatio + absHorizontalTravel) - (aspectRatio + absHorizontalTravel)
            } else {
                Random.nextFloat() * 2f * aspectRatio - aspectRatio
            }
            
            if (existingSpawnX.isEmpty()) {
                bestSpawnX = candidateX
                break
            }
            
            var minDist = Float.MAX_VALUE
            for (otherX in existingSpawnX) {
                val dist = kotlin.math.abs(candidateX - otherX)
                if (dist < minDist) {
                    minDist = dist
                }
            }
            
            if (minDist > bestMinDist) {
                bestMinDist = minDist
                bestSpawnX = candidateX
            }
        }
        
        spawnX = bestSpawnX
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
