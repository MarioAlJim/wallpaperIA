package com.wolf.wallpaper

import kotlin.random.Random

class Lightning(
    var startPointX: Float = 0f,
    var startPointY: Float = 1.0f,
    var endPointX: Float = 0f,
    var endPointY: Float = -1.0f,
    var duration: Float = 0.25f,
    var intensity: Float = 1.0f,
    var branches: List<Branch> = emptyList()
) : StormObject {

    class Branch(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val isMain: Boolean
    )

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

    fun trigger(aspectRatio: Float) {
        isActive = true
        age = 0f
        intensity = 1.0f
        duration = Random.nextFloat() * 0.15f + 0.2f // between 200ms and 350ms
        
        val tempBranches = mutableListOf<Branch>()
        // Spawn lightning starting from top clouds at random horizontal position
        val startX = (Random.nextFloat() * 1.4f - 0.7f) * aspectRatio
        val startY = 0.8f // Cloud height
        val endX = startX + (Random.nextFloat() * 0.5f - 0.25f) * aspectRatio
        val endY = -1.0f // Hits ground level
        
        startPointX = startX
        startPointY = startY
        endPointX = endX
        endPointY = endY

        generateLightningPath(startX, startY, endX, endY, true, tempBranches, 0)
        branches = tempBranches
    }

    private fun generateLightningPath(
        sx: Float, sy: Float, ex: Float, ey: Float,
        isMain: Boolean, list: MutableList<Branch>, depth: Int
    ) {
        if (depth > 4 || sy <= ey) return
        
        // Midpoint displacement to make lightning look jagged
        val midY = (sy + ey) / 2f
        val maxDisplacement = 0.12f / (depth + 1)
        val displacement = (Random.nextFloat() * 2f - 1f) * maxDisplacement
        val midX = (sx + ex) / 2f + displacement
        
        // Add segment from start to mid
        list.add(Branch(sx, sy, midX, midY, isMain))
        
        // Procedurally branch off
        if (isMain && Random.nextFloat() < 0.25f) {
            val branchEndX = midX + (Random.nextFloat() * 0.3f - 0.15f)
            val branchEndY = midY - (Random.nextFloat() * 0.2f + 0.1f)
            generateLightningPath(midX, midY, branchEndX, branchEndY, false, list, depth + 1)
        }
        
        // Add segment from mid to end
        generateLightningPath(midX, midY, ex, ey, isMain, list, depth + 1)
    }
}
