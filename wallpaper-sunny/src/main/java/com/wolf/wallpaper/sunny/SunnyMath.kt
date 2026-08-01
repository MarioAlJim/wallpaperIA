package com.wolf.wallpaper.sunny

/** Pure math helpers shared across the sunny renderer's fade/transition calculations. */
object SunnyMath {
    /** Hermite smoothstep interpolation of [x] between [edge0] and [edge1], clamped to [0,1]. */
    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
