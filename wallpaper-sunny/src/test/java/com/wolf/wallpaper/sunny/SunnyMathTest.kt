package com.wolf.wallpaper.sunny

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SunnyMathTest {

    @Test
    fun smoothstep_belowEdge0_returnsZero() {
        assertEquals(0f, SunnyMath.smoothstep(0f, 1f, -0.5f), 0.0001f)
    }

    @Test
    fun smoothstep_aboveEdge1_returnsOne() {
        assertEquals(1f, SunnyMath.smoothstep(0f, 1f, 1.5f), 0.0001f)
    }

    @Test
    fun smoothstep_atMidpoint_returnsHalf() {
        assertEquals(0.5f, SunnyMath.smoothstep(0f, 1f, 0.5f), 0.0001f)
    }

    @Test
    fun smoothstep_isMonotonicIncreasing_forIncreasingEdges() {
        var previous = SunnyMath.smoothstep(0f, 1f, 0f)
        for (i in 1..10) {
            val x = i / 10f
            val current = SunnyMath.smoothstep(0f, 1f, x)
            assertTrue("smoothstep should not decrease as x increases", current >= previous)
            previous = current
        }
    }

    @Test
    fun smoothstep_supportsDescendingEdges() {
        // Some call sites pass edge0 > edge1 to invert the fade direction (e.g. smoothstep(1.3f, 1.1f, x))
        assertEquals(0f, SunnyMath.smoothstep(1.3f, 1.1f, 1.3f), 0.0001f)
        assertEquals(1f, SunnyMath.smoothstep(1.3f, 1.1f, 1.1f), 0.0001f)
    }
}
