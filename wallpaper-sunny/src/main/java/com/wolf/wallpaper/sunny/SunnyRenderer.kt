package com.wolf.wallpaper.sunny

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import com.wolf.wallpaper.core.ConfigProvider
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.Cloud
import com.wolf.wallpaper.core.Moon
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class SunnyRenderer(
    private val context: Context,
    private val configProvider: ConfigProvider
) : GLRenderer {
    // Programs
    private var program = 0
    private var cloudProgram = 0
    private var backgroundProgram = 0
    private var lensFlareProgram = 0
    private var particleProgram = 0
    private var moonProgram = 0
    private var starsProgram = 0

    // Handles for sunny.frag
    private var timeHandle = -1
    private var aspectHandle = -1
    private var sunSizeHandle = -1
    private var sunSpeedHandle = -1
    private var themeHandle = -1
    private var sunPosHandle = -1
    private var skyTopHandle = -1
    private var skyBottomHandle = -1
    private var godRaysIntensityHandle = -1
    private var sunFadeFactorHandle = -1
    private var sunPulseHandle = -1

    // Handles for particle.frag
    private var pMVPMatrixHandle = -1
    private var pColorHandle = -1
    private var pOpacityHandle = -1

    // Handles for sunny_background.frag
    private var bgThemeHandle = -1
    private var bgSunPosHandle = -1
    private var bgAspectHandle = -1
    private var bgIsCustomHandle = -1
    private var bgSkyTopHandle = -1
    private var bgSkyBottomHandle = -1
    private var bgNightIntensityHandle = -1
    private var bgMoonPhaseMultiplierHandle = -1

    // Handles for lens_flare.frag
    private var lfSunPosHandle = -1
    private var lfAspectHandle = -1
    private var lfSwipeOffsetHandle = -1
    private var lfIntensityHandle = -1



    // moon handles
    private var moonMVPHandle = -1
    private var moonPhaseHandle = -1
    private var moonColorHandle = -1
    private var moonIntensityHandle = -1
    private var moonHaloIntensityHandle = -1

    // stars handles
    private var starsTimeHandle = -1
    private var starsDensityHandle = -1
    private var starsStarColorHandle = -1
    private var starsIntensityHandle = -1
    private var starsAspectHandle = -1
    private var starsMixedHandle = -1

    // Star points shader program
    private var starPointsProgram = 0
    private var spOffsetHandle = -1

    // Star buffers and arrays
    private var starPositionArray = FloatArray(300 * 2)
    private var starSizeArray = FloatArray(300)
    private var starColorArray = FloatArray(300 * 4)

    private lateinit var starPositionBuffer: FloatBuffer
    private lateinit var starSizeBuffer: FloatBuffer
    private lateinit var starColorBuffer: FloatBuffer

    private var time = 0f
    private var aspectRatio = 1.0f
    private var swipeOffset = 0f
    private var pathFadeFactor = 1.0f
    private var moonFadeFactor = 1.0f
    private val moonPhaseMultipliers = floatArrayOf(0.025f, 0.10f, 0.25f, 0.40f, 0.50f, 0.40f, 0.25f, 0.10f)

    // Gyro tilt state
    private var targetTiltX = 0f
    private var targetTiltY = 0f
    private var sensorTiltX = 0f
    private var sensorTiltY = 0f

    // Screen dimensions
    private var screenWidth = 1
    private var screenHeight = 1

    // Sun touch pulse and particles
    private var sunPulseTime = 0f
    private val particles = mutableListOf<SunnyParticle>()

    private class SunnyParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var alpha: Float,
        var life: Float,
        val maxLife: Float,
        val r: Float,
        val g: Float,
        val b: Float
    )

    private class Star(
        var x: Float,
        var y: Float,
        var size: Float,
        var alpha: Float,
        var targetAlpha: Float,
        var twinkleSpeed: Float,
        var r: Float,
        var g: Float,
        var b: Float,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var life: Float = 0f,
        var maxLife: Float = 0f
    )

    private val stars = mutableListOf<Star>()
    private var lastStarColorIndex = -1

    // Sun movement state
    private var sunX = 0.35f
    private var sunY = 0.45f
    private var sunPathX = 0f
    private var moonPathX = 0f
    
    // Random path movement state
    private var randomStartX = 0f
    private var randomStartY = 0f
    private var randomEndX = 0f
    private var randomEndY = 0f
    private var randomProgress = 0f
    private var isRandomPathInitialized = false

    // Night / Combined mode state
    private val moon = Moon()
    private val nightClouds = mutableListOf<Cloud>()
    private var cycleProgress = 0f
    private var prevMoonDirection = -1
    private var isCombinedNight = false
    private var combinedDayIntensity = 1f
    private var combinedNightIntensity = 0f

    // Clouds
    private val clouds = mutableListOf<Cloud>()
    private val cloudTextures = mutableListOf<Int>()

    // Backgrounds
    private val backgroundTextures = IntArray(8)
    private val backgroundAspectRatios = FloatArray(8) { 1.0f }

    // MVP Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    // Buffers
    private lateinit var fullscreenQuadBuffer: FloatBuffer
    private lateinit var cloudQuadBuffer: FloatBuffer
    private lateinit var backgroundQuadBuffer: FloatBuffer

    init {
        Matrix.setIdentityM(viewMatrix, 0)
    }

    override fun onSurfaceCreated() {
        // Enable blending for transparency (clouds and silhouette alpha cut-out)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Compile and link main sky shader
        val vertCode = readAssetFile(context, "shaders/sunny.vert")
        val fragCode = readAssetFile(context, "shaders/sunny.frag")
        program = createProgram(vertCode, fragCode)

        timeHandle = GLES30.glGetUniformLocation(program, "uTime")
        aspectHandle = GLES30.glGetUniformLocation(program, "uAspectRatio")
        sunSizeHandle = GLES30.glGetUniformLocation(program, "uSunSize")
        sunSpeedHandle = GLES30.glGetUniformLocation(program, "uSunSpeed")
        themeHandle = GLES30.glGetUniformLocation(program, "uTheme")
        sunPosHandle = GLES30.glGetUniformLocation(program, "uSunPos")
        skyTopHandle = GLES30.glGetUniformLocation(program, "uSkyTop")
        skyBottomHandle = GLES30.glGetUniformLocation(program, "uSkyBottom")
        godRaysIntensityHandle = GLES30.glGetUniformLocation(program, "uGodRaysIntensity")
        sunFadeFactorHandle = GLES30.glGetUniformLocation(program, "uSunFadeFactor")

        // Compile and link lens flare shader
        try {
            val lfVert = readAssetFile(context, "shaders/sunny.vert")
            val lfFrag = readAssetFile(context, "shaders/lens_flare.frag")
            lensFlareProgram = createProgram(lfVert, lfFrag)

            lfSunPosHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uSunPos")
            lfAspectHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uAspectRatio")
            lfSwipeOffsetHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uSwipeOffset")
            lfIntensityHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uIntensity")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Get uSunPulse handle from main shader
        sunPulseHandle = GLES30.glGetUniformLocation(program, "uSunPulse")

        // Compile and link particle shader
        try {
            val pVert = readAssetFile(context, "shaders/particle.vert")
            val pFrag = readAssetFile(context, "shaders/particle.frag")
            particleProgram = createProgram(pVert, pFrag)

            pMVPMatrixHandle = GLES30.glGetUniformLocation(particleProgram, "uMVPMatrix")
            pColorHandle = GLES30.glGetUniformLocation(particleProgram, "uColor")
            pOpacityHandle = GLES30.glGetUniformLocation(particleProgram, "uOpacity")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Compile and link cloud shader
        val cloudVert = readAssetFile(context, "shaders/cloud.vert")
        val cloudFrag = readAssetFile(context, "shaders/cloud.frag")
        cloudProgram = createProgram(cloudVert, cloudFrag)

        // Compile and link background shader
        val bgVert = readAssetFile(context, "shaders/sunny_background.vert")
        val bgFrag = readAssetFile(context, "shaders/sunny_background.frag")
        backgroundProgram = createProgram(bgVert, bgFrag)

        bgThemeHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTheme")
        bgSunPosHandle = GLES30.glGetUniformLocation(backgroundProgram, "uSunPos")
        bgAspectHandle = GLES30.glGetUniformLocation(backgroundProgram, "uAspectRatio")
        bgIsCustomHandle = GLES30.glGetUniformLocation(backgroundProgram, "uIsCustom")
        bgSkyTopHandle = GLES30.glGetUniformLocation(backgroundProgram, "uSkyTop")
        bgSkyBottomHandle = GLES30.glGetUniformLocation(backgroundProgram, "uSkyBottom")
        bgNightIntensityHandle = GLES30.glGetUniformLocation(backgroundProgram, "uNightIntensity")
        bgMoonPhaseMultiplierHandle = GLES30.glGetUniformLocation(backgroundProgram, "uMoonPhaseMultiplier")



        // Moon program
        try {
            val mVert = readAssetFile(context, "shaders/moon.vert")
            val mFrag = readAssetFile(context, "shaders/moon.frag")
            moonProgram = createProgram(mVert, mFrag)
            moonMVPHandle = GLES30.glGetUniformLocation(moonProgram, "uMVPMatrix")
            moonPhaseHandle = GLES30.glGetUniformLocation(moonProgram, "uPhase")
            moonColorHandle = GLES30.glGetUniformLocation(moonProgram, "uMoonColor")
            moonIntensityHandle = GLES30.glGetUniformLocation(moonProgram, "uIntensity")
            moonHaloIntensityHandle = GLES30.glGetUniformLocation(moonProgram, "uHaloIntensity")
        } catch (e: Exception) { e.printStackTrace() }

        // Stars program
        try {
            val sVert = readAssetFile(context, "shaders/stars.vert")
            val sFrag = readAssetFile(context, "shaders/stars.frag")
            starsProgram = createProgram(sVert, sFrag)
            starsTimeHandle = GLES30.glGetUniformLocation(starsProgram, "uTime")
            starsDensityHandle = GLES30.glGetUniformLocation(starsProgram, "uDensity")
            starsStarColorHandle = GLES30.glGetUniformLocation(starsProgram, "uStarColor")
            starsIntensityHandle = GLES30.glGetUniformLocation(starsProgram, "uIntensity")
            starsAspectHandle = GLES30.glGetUniformLocation(starsProgram, "uAspect")
            starsMixedHandle = GLES30.glGetUniformLocation(starsProgram, "uMixedColors")
        } catch (e: Exception) { e.printStackTrace() }

        // Init moon
        moon.phase = configProvider.getMoonPhase()
        moon.pathDirection = configProvider.getMoonPathDirection()
        moon.moveSpeed = configProvider.getMoonMoveSpeed()
        moon.stationaryPosition = configProvider.getMoonStationaryPosition()
        prevMoonDirection = moon.pathDirection
        moon.resetPath(moon.pathDirection, aspectRatio)

        // Initialize sun path
        val dir = configProvider.getSunPathDirection()
        sunPathX = if (dir == 1) 1.3f else -1.3f

        // Load cloud textures
        cloudTextures.clear()
        try {
            val assetManager = context.assets
            val files = assetManager.list("clouds_sunny") ?: emptyArray()
            val sortedFiles = files.filter { it.endsWith(".png") }.sorted()
            for (file in sortedFiles) {
                val tex = loadTexture(context, "clouds_sunny/$file")
                if (tex != 0) {
                    cloudTextures.add(tex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (cloudTextures.isEmpty()) {
            val fallbackTex = loadTexture(context, "clouds_sunny/cloud_01.png")
            if (fallbackTex != 0) {
                cloudTextures.add(fallbackTex)
            }
        }

        // Load background textures
        backgroundTextures.indices.forEach { backgroundTextures[it] = 0 }
        loadBackgroundTexture(context, 0, "background/sunny_background_01.png")
        loadBackgroundTexture(context, 1, "background/sunny_background_02.png")
        loadBackgroundTexture(context, 2, "background/sunny_background_03.png")
        loadBackgroundTexture(context, 3, "background/sunny_background_04.png")
        loadBackgroundTexture(context, 4, "background/sunny_background_05.png")
        loadBackgroundTexture(context, 5, "background/sunny_background_06.png")
        loadBackgroundTexture(context, 6, "background/sunny_background_07.png")
        loadCustomBackgroundTexture(context, 7)

        // Screen quad coordinates (standard fullscreen pass)
        val fullscreenCoords = floatArrayOf(
            -1f,  1f,
            -1f, -1f,
             1f,  1f,
             1f, -1f
        )
        fullscreenQuadBuffer = ByteBuffer.allocateDirect(fullscreenCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(fullscreenCoords)
                position(0)
            }

        // Cloud quad coordinates: (X, Y, U, V)
        val cloudCoords = floatArrayOf(
            -0.5f,  0.5f, 0f, 0f,
            -0.5f, -0.5f, 0f, 1f,
             0.5f,  0.5f, 1f, 0f,
             0.5f, -0.5f, 1f, 1f
        )
        cloudQuadBuffer = ByteBuffer.allocateDirect(cloudCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(cloudCoords)
                position(0)
            }

        // Background quad coordinates: (X, Y, U, V)
        val backgroundCoords = floatArrayOf(
            -1f,  1f, 0f, 0f,
            -1f, -1f, 0f, 1f,
             1f,  1f, 1f, 0f,
             1f, -1f, 1f, 1f
        )
        backgroundQuadBuffer = ByteBuffer.allocateDirect(backgroundCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(backgroundCoords)
                position(0)
            }

        // Compile and link star points shader
        try {
            val spVert = readAssetFile(context, "shaders/star_points.vert")
            val spFrag = readAssetFile(context, "shaders/star_points.frag")
            starPointsProgram = createProgram(spVert, spFrag)
            spOffsetHandle = GLES30.glGetUniformLocation(starPointsProgram, "uOffset")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize star points FloatBuffers (for max 300 stars)
        starPositionBuffer = ByteBuffer.allocateDirect(300 * 2 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        starSizeBuffer = ByteBuffer.allocateDirect(300 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        starColorBuffer = ByteBuffer.allocateDirect(300 * 4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        
        Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        
        // Adjust and align cloud positions for new aspect ratio
        for (cloud in clouds) {
            cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio, isSunny = true, textureCount = cloudTextures.size)
        }
    }

    override fun onUpdate(deltaTime: Float) {
        val timeMode = configProvider.getTimeMode()
        val combinedDir = if (timeMode == 2) configProvider.getCombinedPathDirection() else 0
        val direction = if (timeMode == 2) {
            if (combinedDir == 0 || combinedDir == 2) 0 else 1
        } else {
            configProvider.getSunPathDirection()
        }

        if (direction != 3) {
            isRandomPathInitialized = false
        }

        if (timeMode == 2) {
            // Speed is controlled by Combined cycle speed config
            val moveSpeed = 0.01f + (configProvider.getGradientCycleSpeed() / 100f) * 0.49f
            val sunL2R = (combinedDir == 0 || combinedDir == 2)
            val moonL2R = (combinedDir == 0 || combinedDir == 3)

            if (!isCombinedNight) {
                // Day Phase: Sun is moving
                if (sunL2R) {
                    sunPathX += deltaTime * moveSpeed
                    if (sunPathX > 1.3f) {
                        isCombinedNight = true
                        moonPathX = if (moonL2R) -1.3f else 1.3f
                    }
                } else {
                    sunPathX -= deltaTime * moveSpeed
                    if (sunPathX < -1.3f) {
                        isCombinedNight = true
                        moonPathX = if (moonL2R) -1.3f else 1.3f
                    }
                }
                
                // Calculate normalized progress p of the active Sun
                val sunStart = if (sunL2R) -1.3f else 1.3f
                val sunEnd = if (sunL2R) 1.3f else -1.3f
                val p = ((sunPathX - sunStart) / (sunEnd - sunStart)).coerceIn(0f, 1f)
                
                // Sunrise transition in first 15% of path, Sunset transition in last 15% of path
                combinedDayIntensity = if (p < 0.15f) {
                    smoothstep(0.0f, 0.15f, p)
                } else if (p > 0.85f) {
                    smoothstep(1.0f, 0.85f, p)
                } else {
                    1.0f
                }
                combinedNightIntensity = 1.0f - combinedDayIntensity

                // Keep inactive Moon off-screen at its start position
                moonPathX = if (moonL2R) -1.3f else 1.3f
            } else {
                // Night Phase: Moon is moving
                if (moonL2R) {
                    moonPathX += deltaTime * moveSpeed
                    if (moonPathX > 1.3f) {
                        isCombinedNight = false
                        sunPathX = if (sunL2R) -1.3f else 1.3f
                    }
                } else {
                    moonPathX -= deltaTime * moveSpeed
                    if (moonPathX < -1.3f) {
                        isCombinedNight = false
                        sunPathX = if (sunL2R) -1.3f else 1.3f
                    }
                }
                
                // Night phase sky is fully dark night
                combinedDayIntensity = 0.0f
                combinedNightIntensity = 1.0f

                // Keep inactive Sun off-screen at its start position
                sunPathX = if (sunL2R) -1.3f else 1.3f
            }

            sunX = sunPathX
            sunY = 0.6f - 1.1f * (sunX * sunX)
            moon.positionX = moonPathX
            moon.positionY = 0.6f - 1.1f * (moon.positionX * moon.positionX)
        } else {
            if (direction == 0) { // Left-to-Right
                val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
                sunPathX += deltaTime * moveSpeed
                if (sunPathX > 1.3f) {
                    sunPathX = -1.3f
                }
                sunX = sunPathX
                sunY = 0.6f - 1.1f * (sunX * sunX)
            } else if (direction == 1) { // Right-to-Left
                val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
                sunPathX -= deltaTime * moveSpeed
                if (sunPathX < -1.3f) {
                    sunPathX = 1.3f
                }
                sunX = sunPathX
                sunY = 0.6f - 1.1f * (sunX * sunX)
            } else if (direction == 3) { // Random (Aleatorio)
                if (!isRandomPathInitialized) {
                    generateRandomPath()
                }
                val moveSpeed = 0.05f + (configProvider.getSunMoveSpeed() / 100f) * 0.45f
                randomProgress += deltaTime * moveSpeed
                if (randomProgress >= 1.0f) {
                    generateRandomPath()
                }
                sunX = randomStartX + (randomEndX - randomStartX) * randomProgress
                sunY = randomStartY + (randomEndY - randomStartY) * randomProgress
            } else { // Stationary (2)
                when (configProvider.getSunStationaryPosition()) {
                    0 -> { // Top Left (Esquina superior izquierda)
                        sunX = -aspectRatio * 0.75f
                        sunY = 0.75f
                    }
                    1 -> { // Top Right (Esquina superior derecha)
                        sunX = aspectRatio * 0.75f
                        sunY = 0.75f
                    }
                    3 -> { // Left Edge (Borde izquierdo)
                        sunX = -aspectRatio * 0.75f
                        sunY = 0.25f
                    }
                    4 -> { // Right Edge (Borde derecho)
                        sunX = aspectRatio * 0.75f
                        sunY = 0.25f
                    }
                    2 -> { // Center (Enmedio)
                        sunX = 0.0f
                        sunY = 0.45f
                    }
                    5 -> { // Custom / Free Position (Personalizado)
                        val customXVal = configProvider.getSunCustomX()
                        val customYVal = configProvider.getSunCustomY()
                        sunX = -aspectRatio + (customXVal / 100f) * (2.0f * aspectRatio)
                        val minY = -0.8f
                        val maxY = 0.8f
                        sunY = minY + (customYVal / 100f) * (maxY - minY)
                    }
                    else -> { // Default / Center
                        sunX = 0.0f
                        sunY = 0.45f
                    }
                }
            }
        }
        
        // Clamp sunPathX to valid boundaries if switching directions dynamically
        if (sunPathX < -1.3f || sunPathX > 1.3f) {
            sunPathX = if (direction == 1) 1.3f else -1.3f
        }

        // 2. Sun pulsation / animation speed
        val speedConfig = configProvider.getSunSpeed()
        val speedFactor = 0.1f + (speedConfig / 100f) * 3.9f // Range 0.1 to 4.0
        time += deltaTime * speedFactor

        // 3. Clouds simulation updates
        val targetDensity = configProvider.getCloudDensity()
        adjustClouds(targetDensity)

        val windDirection = configProvider.getWindDirection()
        val windIntensity = configProvider.getWindIntensity()
        val windSpeed = when (windDirection) {
            0 -> -(windIntensity / 100f) * 0.15f
            2 -> (windIntensity / 100f) * 0.15f
            else -> 0f
        }
        val dynamicsSpeed = configProvider.getCloudDynamicsSpeed() / 100f

        for (cloud in clouds) {
            cloud.update(deltaTime, windSpeed, dynamicsSpeed)
            val halfWidth = cloud.scale * 1.2f
            val maxBound = aspectRatio + halfWidth
            if (cloud.positionX > maxBound || cloud.positionX < -maxBound) {
                cloud.reset(0f, aspectRatio, isSunny = true, textureCount = cloudTextures.size)
                val newHalfWidth = cloud.scale * 1.2f
                val windThreshold = 0.1f
                val driftInfluence = (1.0f - (kotlin.math.abs(windSpeed) / windThreshold)).coerceIn(0f, 1f)
                val netSpeed = windSpeed + (cloud.driftSpeed * driftInfluence)
                if (netSpeed >= 0f) {
                    cloud.positionX = -aspectRatio - newHalfWidth
                } else {
                    cloud.positionX = aspectRatio + newHalfWidth
                }
            }
        }
        clouds.removeAll { it.isFadingOut && it.opacity <= 0f }

        // Night / Combined mode: update moon and night clouds
        if (timeMode == 1 || timeMode == 2) {
            moon.phase = if (timeMode == 2) configProvider.getCombinedMoonPhase() else configProvider.getMoonPhase()
            moon.moveSpeed = configProvider.getMoonMoveSpeed()
            moon.stationaryPosition = configProvider.getMoonStationaryPosition()
            moon.customX = configProvider.getMoonCustomX()
            moon.customY = configProvider.getMoonCustomY()
            
            if (timeMode == 2) {
                // Moon position is already calculated in the first block of onUpdate
            } else {
                val newDir = configProvider.getMoonPathDirection()
                if (newDir != prevMoonDirection) {
                    moon.resetPath(newDir, aspectRatio)
                    prevMoonDirection = newDir
                }
                moon.update(deltaTime, aspectRatio)
            }

            adjustNightClouds(configProvider.getCloudDensity())
            for (nc in nightClouds) {
                nc.update(deltaTime, 0.02f, 0.5f)
                val halfW = nc.scale * 1.2f
                val maxB = aspectRatio + halfW
                if (nc.positionX > maxB || nc.positionX < -maxB) {
                    nc.reset(0f, aspectRatio, isSunny = true, textureCount = cloudTextures.size)
                    nc.positionX = -aspectRatio - nc.scale * 1.2f
                }
            }
            nightClouds.removeAll { it.isFadingOut && it.opacity <= 0f }
        }
        if (timeMode == 2) {
            moonFadeFactor = if (kotlin.math.abs(moonPathX) > 1.1f) {
                smoothstep(1.3f, 1.1f, kotlin.math.abs(moonPathX))
            } else {
                1.0f
            }
        } else {
            moonFadeFactor = 1.0f
        }

        // Calculate pathFadeFactor to smoothly fade sun elements at the start/end of paths
        pathFadeFactor = when (direction) {
            0, 1 -> {
                if (kotlin.math.abs(sunPathX) > 1.1f) {
                    smoothstep(1.3f, 1.1f, kotlin.math.abs(sunPathX))
                } else {
                    1.0f
                }
            }
            3 -> {
                if (randomProgress < 0.1f) {
                    smoothstep(0f, 0.1f, randomProgress)
                } else if (randomProgress > 0.9f) {
                    smoothstep(1.0f, 0.9f, randomProgress)
                } else {
                    1.0f
                }
            }
            else -> 1.0f
        }

        // Smoothly interpolate sensor tilt values to prevent jitter
        val lerpFactor = (deltaTime * 10.0f).coerceIn(0f, 1f)
        sensorTiltX += (targetTiltX - sensorTiltX) * lerpFactor
        sensorTiltY += (targetTiltY - sensorTiltY) * lerpFactor

        // Decay sun pulse time
        if (sunPulseTime > 0f) {
            sunPulseTime = (sunPulseTime - deltaTime * 2.0f).coerceAtLeast(0f)
        }

        // Update active particles
        val pIterator = particles.iterator()
        while (pIterator.hasNext()) {
            val p = pIterator.next()
            p.life -= deltaTime
            if (p.life <= 0f) {
                pIterator.remove()
            } else {
                p.x += p.vx * deltaTime
                p.y += p.vy * deltaTime
                p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                p.vx *= (1.0f - deltaTime * 0.5f)
                p.vy *= (1.0f - deltaTime * 0.5f)
            }
        }

        // Stars simulation updates
        val isCombined = configProvider.getTimeMode() == 2
        val starDensitySetting = if (isCombined) configProvider.getCombinedStarDensity() else configProvider.getStarDensity()
        adjustStars(starDensitySetting)

        val starColorSetting = if (isCombined) configProvider.getCombinedStarColorIndex() else configProvider.getStarColorIndex()
        if (starColorSetting != lastStarColorIndex) {
            lastStarColorIndex = starColorSetting
            for (star in stars) {
                val finalIndex = if (starColorSetting == 5) kotlin.random.Random.nextInt(5) else starColorSetting
                val (r, g, b) = getStarColorRGB(finalIndex)
                star.r = r
                star.g = g
                star.b = b
            }
        }

        val isStarModeRandom = configProvider.getStarMode() == 1
        for (star in stars) {
            star.life += deltaTime * star.twinkleSpeed
            if (star.life >= star.maxLife) {
                resetStar(star, randomizePosition = isStarModeRandom)
            } else {
                val progress = star.life / star.maxLife
                val normalizedAlpha = when {
                    progress < 0.2f -> progress / 0.2f
                    progress > 0.8f -> (1.0f - progress) / 0.2f
                    else -> 1.0f
                }
                star.alpha = normalizedAlpha * star.targetAlpha

                if (isStarModeRandom) {
                    star.x += star.vx * deltaTime
                    star.y += star.vy * deltaTime

                    if (star.x > 1.0f) star.x = -1.0f
                    if (star.x < -1.0f) star.x = 1.0f
                    if (star.y > 1.0f) star.y = -0.4f
                    if (star.y < -0.4f) star.y = 1.0f
                }
            }
        }
    }

    private fun generateRandomPath() {
        val random = kotlin.random.Random
        
        // Choose start edge (0: Left, 1: Right, 2: Top, 3: Bottom)
        val startEdge = random.nextInt(4)
        
        // Choose end edge (different from start)
        var endEdge = random.nextInt(4)
        while (endEdge == startEdge) {
            endEdge = random.nextInt(4)
        }

        // Helper to get coordinates on an edge
        fun getEdgeCoordinates(edge: Int): Pair<Float, Float> {
            val minY = 0.0f
            val maxY = 0.8f
            val xMargin = 0.25f
            return when (edge) {
                0 -> Pair(-aspectRatio - xMargin, random.nextFloat() * (maxY - minY) + minY) // Left edge
                1 -> Pair(aspectRatio + xMargin, random.nextFloat() * (maxY - minY) + minY) // Right edge
                2 -> Pair(random.nextFloat() * (2f * aspectRatio) - aspectRatio, 1.1f) // Top edge
                else -> Pair(random.nextFloat() * (2f * aspectRatio) - aspectRatio, 0.0f) // Bottom edge
            }
        }

        val start = getEdgeCoordinates(startEdge)
        randomStartX = start.first
        randomStartY = start.second

        val end = getEdgeCoordinates(endEdge)
        randomEndX = end.first
        randomEndY = end.second
        
        randomProgress = 0f
        isRandomPathInitialized = true
    }

    override fun onDrawFrame() {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        when (configProvider.getTimeMode()) {
            1 -> drawNightScene(1f)
            2 -> drawCombinedScene()
            else -> drawDayScene()
        }
    }

    private fun drawDaySky(intensity: Float) {
        // 1. Cielo procedural y sol
        GLES30.glUseProgram(program)
        GLES30.glUniform1f(timeHandle, time)
        GLES30.glUniform1f(aspectHandle, aspectRatio)

        val sizeConfig = if (configProvider.getTimeMode() == 2) {
            configProvider.getCombinedSunSize()
        } else {
            configProvider.getSunSize()
        }
        val mappedSize = 0.05f + (sizeConfig / 100f) * 0.30f
        GLES30.glUniform1f(sunSizeHandle, mappedSize)

        val speedConfig = configProvider.getSunSpeed()
        val mappedSpeed = 0.5f + (speedConfig / 100f) * 1.5f
        GLES30.glUniform1f(sunSpeedHandle, mappedSpeed * 0.4f)

        val theme = configProvider.getSunnyTheme()
        GLES30.glUniform1i(themeHandle, theme)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.02f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.02f else 0f
        GLES30.glUniform2f(sunPosHandle, sunX + gyroX, sunY + gyroY)
        GLES30.glUniform1f(sunPulseHandle, sunPulseTime)

        val godRaysRaw = if (configProvider.isSunnyGodRaysEnabled()) configProvider.getSunnyGodRaysIntensity() / 100f else 0f
        val lowSunFactor = (1.0f - smoothstep(0.3f, 0.7f, sunY + gyroY)) * smoothstep(-0.6f, -0.2f, sunY + gyroY)
        GLES30.glUniform1f(godRaysIntensityHandle, godRaysRaw * lowSunFactor)
        GLES30.glUniform1f(sunFadeFactorHandle, pathFadeFactor * intensity)

        if (theme == 3) {
            val topColor = configProvider.getSunnyCustomSkyTopColor()
            val bottomColor = configProvider.getSunnyCustomSkyBottomColor()
            GLES30.glUniform3f(skyTopHandle,
                android.graphics.Color.red(topColor) / 255f,
                android.graphics.Color.green(topColor) / 255f,
                android.graphics.Color.blue(topColor) / 255f)
            GLES30.glUniform3f(skyBottomHandle,
                android.graphics.Color.red(bottomColor) / 255f,
                android.graphics.Color.green(bottomColor) / 255f,
                android.graphics.Color.blue(bottomColor) / 255f)
        }

        fullscreenQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
    }

    private fun drawDayScene() {
        drawDaySky(1f)

        // 2. Fondo seleccionado
        val bgIndex = configProvider.getSunnyBackgroundIndex()
        drawBackground(bgIndex, 0f)

        // 3. Nubes activas
        drawClouds(1f)

        // 4. Partículas
        drawParticles(1f)

        // 5. Destellos de lente
        drawLensFlare(1f)
    }

    private fun drawNightScene(intensity: Float) {
        drawStars(intensity)
        drawMoon(intensity)
        drawNightClouds(intensity)
        val bgIndex = configProvider.getSunnyBackgroundIndex()
        drawBackground(bgIndex, intensity)
    }

    private fun drawCombinedScene() {
        val dayIntensity = combinedDayIntensity
        val nightIntensity = combinedNightIntensity

        // 1. Cielo (Día y/o Noche)
        if (dayIntensity > 0.01f) {
            drawDaySky(dayIntensity)
        }
        if (nightIntensity > 0.01f) {
            drawStars(nightIntensity)
        }

        // 2. Astro Nocturno (Luna)
        if (nightIntensity > 0.01f) {
            drawMoon(nightIntensity)
        }

        // 3. Nubes (Día y/o Noche)
        if (dayIntensity > 0.01f) {
            drawClouds(dayIntensity)
        }
        if (nightIntensity > 0.01f) {
            drawNightClouds(nightIntensity)
        }

        // 4. Paisaje Silueta (Montañas/Fondo - Dibujado al frente)
        val bgIndex = configProvider.getSunnyBackgroundIndex()
        if (bgIndex > 0) {
            drawBackground(bgIndex, nightIntensity)
        }

        // 5. Efectos Especiales de Día
        if (dayIntensity > 0.01f) {
            drawParticles(dayIntensity)
            drawLensFlare(dayIntensity)
        }
    }

    private fun drawStars(intensity: Float) {
        if (starsProgram == 0) return
        GLES30.glUseProgram(starsProgram)
        GLES30.glUniform1f(starsIntensityHandle, intensity)

        fullscreenQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)

        // Draw star points
        if (starPointsProgram == 0 || stars.isEmpty()) return

        var index = 0
        for (star in stars) {
            starPositionArray[index * 2] = star.x
            starPositionArray[index * 2 + 1] = star.y
            
            starSizeArray[index] = star.size
            
            starColorArray[index * 4] = star.r
            starColorArray[index * 4 + 1] = star.g
            starColorArray[index * 4 + 2] = star.b
            starColorArray[index * 4 + 3] = star.alpha * intensity
            index++
        }

        starPositionBuffer.clear()
        starPositionBuffer.put(starPositionArray, 0, index * 2)
        starPositionBuffer.position(0)

        starSizeBuffer.clear()
        starSizeBuffer.put(starSizeArray, 0, index)
        starSizeBuffer.position(0)

        starColorBuffer.clear()
        starColorBuffer.put(starColorArray, 0, index * 4)
        starColorBuffer.position(0)

        GLES30.glUseProgram(starPointsProgram)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.015f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.015f else 0f
        GLES30.glUniform2f(spOffsetHandle, swipeOffset * 0.05f + gyroX, gyroY)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, starPositionBuffer)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, 0, starSizeBuffer)

        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 0, starColorBuffer)

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, index)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    private fun drawMoon(intensity: Float) {
        if (moonProgram == 0) return
        GLES30.glUseProgram(moonProgram)

        val sizeConfig = if (configProvider.getTimeMode() == 2) {
            configProvider.getCombinedMoonSize()
        } else {
            50
        }
        val mappedMoonSize = if (configProvider.getTimeMode() == 2) {
            0.06f + (sizeConfig / 100f) * 0.34f
        } else {
            0.24f
        }
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, moon.positionX, moon.positionY, 0f)
        Matrix.scaleM(modelMatrix, 0, mappedMoonSize, mappedMoonSize, 1.0f)

        val mvp = FloatArray(16)
        Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(moonMVPHandle, 1, false, mvp, 0)
        GLES30.glUniform1i(moonPhaseHandle, moon.phase)
        GLES30.glUniform3f(moonColorHandle, 1.0f, 0.97f, 0.88f)
        val horizonFade = smoothstep(-0.4f, -0.1f, moon.positionY)
        val baseIntensity = if (configProvider.getTimeMode() == 2) intensity * moonFadeFactor else intensity
        val finalIntensity = baseIntensity * horizonFade
        GLES30.glUniform1f(moonIntensityHandle, finalIntensity)

        val haloHorizonFade = smoothstep(-0.4f, 0.8f, moon.positionY)
        GLES30.glUniform1f(moonHaloIntensityHandle, haloHorizonFade)

        cloudQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)
        cloudQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun drawNightClouds(intensity: Float) {
        if (nightClouds.isEmpty() || cloudProgram == 0) return
        GLES30.glUseProgram(cloudProgram)

        val mvpMatrixHandle = GLES30.glGetUniformLocation(cloudProgram, "uMVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(cloudProgram, "uOpacity")
        val textureHandle = GLES30.glGetUniformLocation(cloudProgram, "uTexture")
        val flashIntensityHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashIntensity")
        val flashColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashColor")
        val cloudColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uCloudColor")

        // Night clouds color: slightly darker/cooler/blue-ish gray tint
        GLES30.glUniform3f(cloudColorHandle, 0.35f, 0.42f, 0.55f)
        GLES30.glUniform1f(flashIntensityHandle, 0f)
        GLES30.glUniform3f(flashColorHandle, 1.0f, 1.0f, 1.0f)

        // Bind positions attribute (location 0)
        cloudQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        // Bind UV coordinates attribute (location 1)
        cloudQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glUniform1i(textureHandle, 0)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.10f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.10f else 0f

        for (cloud in nightClouds.sortedBy { it.z }) {
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cloud.positionX + gyroX, cloud.positionY + gyroY, 0f)
            Matrix.scaleM(modelMatrix, 0, cloud.scale * 2.4f, cloud.scale, 1.0f)

            val modelViewProjection = FloatArray(16)
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)
            GLES30.glUniform1f(opacityHandle, cloud.opacity * intensity)

            if (cloudTextures.isNotEmpty()) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cloudTextures[cloud.textureIndex % cloudTextures.size])
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            }
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun adjustNightClouds(density: Int) {
        val targetCount = when (density) {
            0 -> 0
            25 -> 1
            50 -> 2
            75 -> 4
            90 -> 6
            100 -> 8
            else -> (density / 100f * 8).toInt()
        }.coerceIn(0, 8)

        val textureCount = cloudTextures.size.coerceAtLeast(1)
        val activeClouds = nightClouds.filter { !it.isFadingOut }

        if (activeClouds.size < targetCount) {
            var needed = targetCount - activeClouds.size
            val fading = nightClouds.filter { it.isFadingOut }
            for (nc in fading) {
                if (needed > 0) { nc.isFadingOut = false; needed-- }
            }
            while (needed > 0) {
                val id = if (nightClouds.isNotEmpty()) nightClouds.maxOf { it.id } + 1 else 0
                val nc = Cloud(id, 0f, 0f, 0f, 0f, 0f, 0)
                nc.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio,
                    aspectRatio, isSunny = true, textureCount = textureCount)
                nc.opacity = 0f
                nightClouds.add(nc)
                needed--
            }
        } else if (activeClouds.size > targetCount) {
            var excess = activeClouds.size - targetCount
            for (i in activeClouds.indices.reversed()) {
                if (excess > 0) { activeClouds[i].isFadingOut = true; excess-- }
            }
        }
    }

    private fun drawClouds(intensity: Float = 1f) {
        if (clouds.isEmpty()) return

        GLES30.glUseProgram(cloudProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(cloudProgram, "uMVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(cloudProgram, "uOpacity")
        val textureHandle = GLES30.glGetUniformLocation(cloudProgram, "uTexture")
        val flashIntensityHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashIntensity")
        val flashColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashColor")
        val cloudColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uCloudColor")

        // In Sunny mode, warm color tinting matching the active sky theme
        val theme = configProvider.getSunnyTheme()
        val (cloudR, cloudG, cloudB) = when (theme) {
            0 -> Triple(1.0f, 1.0f, 1.0f) // Mediodía (Pure White)
            1 -> Triple(1.0f, 0.94f, 0.88f) // Atardecer (Whiter warm peach)
            2 -> Triple(0.98f, 0.92f, 0.96f) // Anochecer (Whiter lavender / pinkish magenta)
            else -> { // Custom (Theme 3)
                val bottomColor = configProvider.getSunnyCustomSkyBottomColor()
                val r = android.graphics.Color.red(bottomColor) / 255f
                val g = android.graphics.Color.green(bottomColor) / 255f
                val b = android.graphics.Color.blue(bottomColor) / 255f
                Triple(0.85f + r * 0.15f, 0.85f + g * 0.15f, 0.85f + b * 0.15f)
            }
        }
        GLES30.glUniform3f(cloudColorHandle, cloudR, cloudG, cloudB)

        // No lightning flashes in sunny mode
        GLES30.glUniform1f(flashIntensityHandle, 0f)
        GLES30.glUniform3f(flashColorHandle, 1.0f, 1.0f, 1.0f)

        // Bind positions attribute (location 0)
        cloudQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        // Bind UV coordinates attribute (location 1)
        cloudQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glUniform1i(textureHandle, 0)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.10f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.10f else 0f

        for (cloud in clouds.sortedBy { it.z }) {
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cloud.positionX + gyroX, cloud.positionY + gyroY, 0f)
            Matrix.scaleM(modelMatrix, 0, cloud.scale * 2.4f, cloud.scale, 1.0f)

            val modelViewProjection = FloatArray(16)
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)
            GLES30.glUniform1f(opacityHandle, cloud.opacity * intensity)

            if (cloudTextures.isNotEmpty()) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cloudTextures[cloud.textureIndex % cloudTextures.size])
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            }
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun drawParticles(intensity: Float = 1f) {
        if (particles.isEmpty() || particleProgram == 0) return

        GLES30.glUseProgram(particleProgram)

        // Save current blend configuration
        val blendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        val prevSrcFunc = IntArray(1)
        val prevDstFunc = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_BLEND_SRC_RGB, prevSrcFunc, 0)
        GLES30.glGetIntegerv(GLES30.GL_BLEND_DST_RGB, prevDstFunc, 0)

        // Enable additive blending for glow effect
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)

        // Bind positions attribute (location 0)
        cloudQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        // Bind UV coordinates attribute (location 1)
        cloudQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.02f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.02f else 0f

        for (p in particles) {
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x + gyroX, p.y + gyroY, 0f)
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, 1.0f)

            val modelViewProjection = FloatArray(16)
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

            GLES30.glUniformMatrix4fv(pMVPMatrixHandle, 1, false, modelViewProjection, 0)
            GLES30.glUniform3f(pColorHandle, p.r, p.g, p.b)
            GLES30.glUniform1f(pOpacityHandle, p.alpha * intensity)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)

        // Restore blend function to default to avoid breaking subsequent renders
        if (!blendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        } else {
            GLES30.glBlendFunc(prevSrcFunc[0], prevDstFunc[0])
        }
    }

    private fun drawBackground(backgroundIndex: Int, nightIntensity: Float = 0f) {
        if (backgroundIndex <= 0 || backgroundIndex > 8) return
        val texIndex = backgroundIndex - 1
        val textureId = backgroundTextures[texIndex]
        if (textureId == 0) return

        GLES30.glUseProgram(backgroundProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(backgroundProgram, "uMVPMatrix")
        val textureHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTexture")

        // Pass theme, sun position, aspect ratio, and custom image flag to the shader
        val theme = configProvider.getSunnyTheme()
        GLES30.glUniform1i(bgThemeHandle, theme)
        val bgGyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.02f else 0f
        val bgGyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.02f else 0f
        val lightX = sunX * (1f - nightIntensity) + moon.positionX * nightIntensity
        val lightY = sunY * (1f - nightIntensity) + moon.positionY * nightIntensity
        GLES30.glUniform2f(bgSunPosHandle, lightX + bgGyroX, lightY + bgGyroY)
        GLES30.glUniform1f(bgAspectHandle, aspectRatio)
        GLES30.glUniform1i(bgIsCustomHandle, if (backgroundIndex == 8) 1 else 0)
        GLES30.glUniform1f(bgNightIntensityHandle, nightIntensity)
        
        val currentPhase = if (configProvider.getTimeMode() == 2) {
            configProvider.getCombinedMoonPhase()
        } else {
            configProvider.getMoonPhase()
        }
        val clampedPhase = currentPhase.coerceIn(0, 7)
        val moonPhaseMultiplier = moonPhaseMultipliers[clampedPhase]
        GLES30.glUniform1f(bgMoonPhaseMultiplierHandle, moonPhaseMultiplier)

        if (theme == 3) {
            val topColor = configProvider.getSunnyCustomSkyTopColor()
            val bottomColor = configProvider.getSunnyCustomSkyBottomColor()
            val tr = android.graphics.Color.red(topColor) / 255f
            val tg = android.graphics.Color.green(topColor) / 255f
            val tb = android.graphics.Color.blue(topColor) / 255f
            val br = android.graphics.Color.red(bottomColor) / 255f
            val bg = android.graphics.Color.green(bottomColor) / 255f
            val bb = android.graphics.Color.blue(bottomColor) / 255f
            GLES30.glUniform3f(bgSkyTopHandle, tr, tg, tb)
            GLES30.glUniform3f(bgSkyBottomHandle, br, bg, bb)
        }

        backgroundQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, backgroundQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        backgroundQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, backgroundQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(textureHandle, 0)

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        val screenAspect = aspectRatio
        val bgAspect = backgroundAspectRatios[texIndex]
        var scaleX = 1.0f
        var scaleY = 1.0f

        if (screenAspect > bgAspect) {
            scaleX = screenAspect
            scaleY = screenAspect / bgAspect
        } else {
            scaleX = bgAspect
            scaleY = 1.0f
        }

        val bgShiftX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.05f else 0f
        val bgShiftY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.05f else 0f

        // Parallax background scrolling based on swipeOffset
        val maxShiftX = (scaleX - screenAspect).coerceAtLeast(0f)
        val shiftX = -swipeOffset * maxShiftX * 0.5f
        Matrix.translateM(modelMatrix, 0, shiftX + bgShiftX, bgShiftY, 0f)

        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1.0f)

        val modelViewProjection = FloatArray(16)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun adjustClouds(density: Int) {
        // Map 0-100 density to custom cloud count: 0, 1, 3, 6, 8, 10
        val targetCount = when (density) {
            0 -> 0
            25 -> 1
            50 -> 3
            75 -> 6
            90 -> 8
            100 -> 10
            else -> (density / 100f * 10).toInt()
        }.coerceIn(0, 10)

        val textureCount = cloudTextures.size
        if (textureCount == 0) return

        val activeClouds = clouds.filter { !it.isFadingOut }

        if (activeClouds.size < targetCount) {
            val fadingOutClouds = clouds.filter { it.isFadingOut }
            var needed = targetCount - activeClouds.size
            for (cloud in fadingOutClouds) {
                if (needed > 0) {
                    cloud.isFadingOut = false
                    needed--
                }
            }
            while (needed > 0) {
                val cloudId = if (clouds.isNotEmpty()) clouds.maxOf { it.id } + 1 else 0
                val textureIndex = kotlin.random.Random.nextInt(textureCount)
                val cloud = Cloud(cloudId, 0f, 0f, 0f, 0f, 0f, textureIndex)
                cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio, isSunny = true, textureCount = textureCount)
                cloud.opacity = 0f
                clouds.add(cloud)
                needed--
            }
        } else if (activeClouds.size > targetCount) {
            var excess = activeClouds.size - targetCount
            for (i in activeClouds.indices.reversed()) {
                if (excess > 0) {
                    activeClouds[i].isFadingOut = true
                    excess--
                }
            }
        }
    }

    private fun adjustStars(density: Int) {
        val targetCount = (density * 2.5f).toInt().coerceIn(0, 300)
        if (stars.size < targetCount) {
            val needed = targetCount - stars.size
            for (i in 0 until needed) {
                stars.add(createRandomStar())
            }
        } else if (stars.size > targetCount) {
            while (stars.size > targetCount) {
                stars.removeAt(stars.size - 1)
            }
        }
    }

    private fun createRandomStar(): Star {
        val star = Star(
            x = 0f, y = 0f, size = 0f, alpha = 0f, targetAlpha = 0f, twinkleSpeed = 0f, r = 1f, g = 1f, b = 1f
        )
        resetStar(star, randomizePosition = true)
        return star
    }

    private fun resetStar(star: Star, randomizePosition: Boolean) {
        val random = kotlin.random.Random
        if (randomizePosition) {
            star.x = random.nextFloat() * 2f - 1f // NDC bounds: -1.0 to 1.0
            star.y = random.nextFloat() * 1.4f - 0.4f // NDC bounds: -0.4 to 1.0 (sky region)
        }
        
        star.size = 3.0f + random.nextFloat() * 12.0f // pixel size: 3 to 15
        star.alpha = 0f
        star.targetAlpha = 0.2f + random.nextFloat() * 0.8f // max opacity: 0.2 to 1.0
        
        // Random duration between 2 and 6 seconds
        val duration = 2.0f + random.nextFloat() * 4.0f
        star.maxLife = duration
        star.twinkleSpeed = 1.0f
        star.life = 0f

        // Slow drift velocity for Random/Dynamic mode (-0.015 to 0.015 units/sec)
        star.vx = (random.nextFloat() * 2f - 1f) * 0.015f
        star.vy = (random.nextFloat() * 2f - 1f) * 0.015f

        // Star color configuration
        val isCombined = configProvider.getTimeMode() == 2
        val colorIndex = if (isCombined) configProvider.getCombinedStarColorIndex() else configProvider.getStarColorIndex()
        val finalIndex = if (colorIndex == 5) random.nextInt(5) else colorIndex
        val (r, g, b) = getStarColorRGB(finalIndex)
        star.r = r
        star.g = g
        star.b = b
    }

    private fun getStarColorRGB(colorIndex: Int): Triple<Float, Float, Float> {
        return when (colorIndex) {
            1 -> Triple(0.6f, 0.8f, 1.0f)   // Azul frío
            2 -> Triple(1.0f, 0.95f, 0.70f) // Amarillo cálido
            3 -> Triple(1.0f, 0.70f, 0.85f) // Rosa
            4 -> Triple(0.5f, 1.0f, 0.70f)  // Verde
            else -> Triple(1.0f, 1.0f, 1.0f) // Blanco
        }
    }

    override fun onSensorValuesChanged(tiltX: Float, tiltY: Float) {
        if (configProvider.isSunnyGyroEnabled()) {
            targetTiltX = tiltX
            targetTiltY = tiltY
        } else {
            targetTiltX = 0f
            targetTiltY = 0f
        }
    }

    override fun onTouchEvent(x: Float, y: Float) {
        if (!configProvider.isSunnyTouchBurstEnabled()) return

        // Proyectar coordenadas a espacio ortográfico del shader (uv space)
        val ndcX = -1f + (x / screenWidth) * 2f
        val ndcY = 1f - (y / screenHeight) * 2f

        val uvX = ndcX * aspectRatio
        val uvY = ndcY

        val sunUvX = sunX * aspectRatio
        val sunUvY = sunY * aspectRatio

        val dx = uvX - sunUvX
        val dy = uvY - sunUvY
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)

        // Comprobar si el toque cae dentro del disco solar (con buffer de tolerancia)
        val sizeConfig = configProvider.getSunSize()
        val mappedSize = 0.05f + (sizeConfig / 100f) * 0.30f
        val touchRadius = mappedSize + 0.12f

        if (dist <= touchRadius) {
            // Activar pulsación
            sunPulseTime = 1.0f

            // Generar ráfaga de 35 partículas radiales
            val random = kotlin.random.Random
            val theme = configProvider.getSunnyTheme()

            for (i in 0 until 35) {
                val angle = random.nextFloat() * 2.0f * Math.PI.toFloat()
                val speed = 0.3f + random.nextFloat() * 1.5f
                val vx = kotlin.math.cos(angle) * speed
                val vy = kotlin.math.sin(angle) * speed
                val pSize = 0.04f + random.nextFloat() * 0.08f
                val maxLife = 0.5f + random.nextFloat() * 0.7f

                // Asignar colores según el tema activo
                val (r, g, b) = when (theme) {
                    0 -> { // Mediodía (Blanco/Amarillo brillante)
                        Triple(1.0f, 0.9f + random.nextFloat() * 0.1f, 0.5f + random.nextFloat() * 0.4f)
                    }
                    1 -> { // Atardecer (Naranja/Rojo cálido)
                        Triple(1.0f, 0.25f + random.nextFloat() * 0.45f, 0.0f + random.nextFloat() * 0.15f)
                    }
                    2 -> { // Anochecer (Rosa/Lavanda/Púrpura)
                        Triple(0.9f + random.nextFloat() * 0.1f, 0.3f + random.nextFloat() * 0.4f, 0.7f + random.nextFloat() * 0.3f)
                    }
                    else -> { // Personalizado (Dorado/Fuego)
                        Triple(1.0f, 0.65f + random.nextFloat() * 0.25f, 0.15f + random.nextFloat() * 0.35f)
                    }
                }

                particles.add(
                    SunnyParticle(
                        x = sunUvX,
                        y = sunUvY,
                        vx = vx,
                        vy = vy,
                        size = pSize,
                        alpha = 1.0f,
                        life = maxLife,
                        maxLife = maxLife,
                        r = r,
                        g = g,
                        b = b
                    )
                )
            }
        }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        val clampedX = xOffset.coerceIn(0f, 1f)
        swipeOffset = (clampedX - 0.5f) * 2.0f
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun drawLensFlare(intensity: Float = 1f) {
        if (lensFlareProgram == 0) return
        val enabled = configProvider.isSunnyLensFlareEnabled()
        if (!enabled) return

        val rawIntensity = configProvider.getSunnyLensFlareIntensity() / 100f
        val lowSunFactor = (1.0f - smoothstep(0.3f, 0.7f, sunY)) * smoothstep(-0.6f, -0.2f, sunY)
        val finalIntensity = rawIntensity * lowSunFactor * pathFadeFactor * intensity

        if (finalIntensity <= 0.0f) return

        GLES30.glUseProgram(lensFlareProgram)
        GLES30.glUniform2f(lfSunPosHandle, sunX, sunY)
        GLES30.glUniform1f(lfAspectHandle, aspectRatio)
        GLES30.glUniform1f(lfSwipeOffsetHandle, swipeOffset)
        GLES30.glUniform1f(lfIntensityHandle, finalIntensity)

        fullscreenQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
    }

    private fun readAssetFile(context: Context, path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw RuntimeException("Could not open asset file: $path", e)
        }
    }

    private fun loadTexture(context: Context, assetPath: String): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) {
            return 0
        }

        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        try {
            context.assets.open(assetPath).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                if (bitmap != null) {
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                    GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                    bitmap.recycle()
                }
            }
        } catch (e: IOException) {
            GLES30.glDeleteTextures(1, textureIds, 0)
            throw RuntimeException("Could not load texture from asset: $assetPath", e)
        }
        return textureIds[0]
    }

    private fun loadBackgroundTexture(context: Context, index: Int, assetPath: String) {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return

        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        try {
            context.assets.open(assetPath).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                if (bitmap != null) {
                    backgroundAspectRatios[index] = bitmap.width.toFloat() / bitmap.height.toFloat()
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                    GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                    bitmap.recycle()
                    backgroundTextures[index] = textureIds[0]
                }
            }
        } catch (e: Exception) {
            GLES30.glDeleteTextures(1, textureIds, 0)
            e.printStackTrace()
        }
    }

    private fun loadCustomBackgroundTexture(context: Context, index: Int) {
        val file = java.io.File(context.filesDir, "custom_sunny_background.png")
        if (!file.exists()) return

        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return

        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            if (bitmap != null) {
                backgroundAspectRatios[index] = bitmap.width.toFloat() / bitmap.height.toFloat()
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                bitmap.recycle()
                backgroundTextures[index] = textureIds[0]
            }
        } catch (e: Exception) {
            GLES30.glDeleteTextures(1, textureIds, 0)
            e.printStackTrace()
        }
    }

    private fun wrapPathX(x: Float): Float {
        var v = x
        while (v > 1.3f) v -= 2.6f
        while (v < -1.3f) v += 2.6f
        return v
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader ($type): $log")
        }
        return shader
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw RuntimeException("Error linking program: $log")
        }
        return program
    }
}
