package com.wolf.wallpaper.sunny

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import com.wolf.wallpaper.core.ConfigProvider
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.Cloud
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

    // Handles for lens_flare.frag
    private var lfSunPosHandle = -1
    private var lfAspectHandle = -1
    private var lfSwipeOffsetHandle = -1
    private var lfIntensityHandle = -1
    
    private var time = 0f
    private var aspectRatio = 1.0f
    private var swipeOffset = 0f
    private var pathFadeFactor = 1.0f

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

    // Sun movement state
    private var sunX = 0.35f
    private var sunY = 0.45f
    private var sunPathX = 0f
    
    // Random path movement state
    private var randomStartX = 0f
    private var randomStartY = 0f
    private var randomEndX = 0f
    private var randomEndY = 0f
    private var randomProgress = 0f
    private var isRandomPathInitialized = false

    // Clouds
    private val clouds = mutableListOf<Cloud>()
    private val cloudTextures = mutableListOf<Int>()

    // Backgrounds
    private val backgroundTextures = IntArray(7)
    private val backgroundAspectRatios = FloatArray(7) { 1.0f }

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

        // Initialize sun path
        val dir = configProvider.getSunPathDirection()
        sunPathX = if (dir == 1) 1.3f else -1.3f

        // Load cloud textures
        cloudTextures.clear()
        try {
            val assetManager = context.assets
            val files = assetManager.list("clouds") ?: emptyArray()
            val sortedFiles = files.filter { it.endsWith(".png") }.sorted()
            for (file in sortedFiles) {
                val tex = loadTexture(context, "clouds/$file")
                if (tex != 0) {
                    cloudTextures.add(tex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (cloudTextures.isEmpty()) {
            val fallbackTex = loadTexture(context, "clouds/cloud_01.png")
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
        loadCustomBackgroundTexture(context, 6)

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
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        
        Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        
        // Adjust and align cloud positions for new aspect ratio
        for (cloud in clouds) {
            cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio, isSunny = true)
        }
    }

    override fun onUpdate(deltaTime: Float) {
        // 1. Sun translation along the path
        val direction = configProvider.getSunPathDirection()
        if (direction != 3) {
            isRandomPathInitialized = false
        }

        if (direction == 0) { // Left-to-Right
            val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
            sunPathX += deltaTime * moveSpeed
            if (sunPathX > 1.3f) {
                sunPathX = -1.3f
            }
            sunX = sunPathX
            sunY = 1.2f - 1.5f * (sunX * sunX)
        } else if (direction == 1) { // Right-to-Left
            val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
            sunPathX -= deltaTime * moveSpeed
            if (sunPathX < -1.3f) {
                sunPathX = 1.3f
            }
            sunX = sunPathX
            sunY = 1.2f - 1.5f * (sunX * sunX)
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
                    sunX = -1.0f
                    sunY = 1.0f / aspectRatio
                }
                1 -> { // Top Right (Esquina superior derecha)
                    sunX = 1.0f
                    sunY = 1.0f / aspectRatio
                }
                3 -> { // Left Edge (Borde izquierdo)
                    sunX = -0.8f
                    sunY = 0.25f
                }
                4 -> { // Right Edge (Borde derecho)
                    sunX = 0.8f
                    sunY = 0.25f
                }
                2 -> { // Center (Enmedio)
                    sunX = 0.0f
                    sunY = 0.45f
                }
                5 -> { // Custom / Free Position (Personalizado)
                    val customXVal = configProvider.getSunCustomX()
                    val customYVal = configProvider.getSunCustomY()
                    sunX = -1.0f + (customXVal / 100f) * 2.0f
                    val minY = -0.8f
                    val maxY = 1.0f / aspectRatio
                    sunY = minY + (customYVal / 100f) * (maxY - minY)
                }
                else -> { // Default / Center
                    sunX = 0.0f
                    sunY = 0.45f
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
                cloud.reset(0f, aspectRatio, isSunny = true)
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
            val maxTopY = 1.2f / aspectRatio
            return when (edge) {
                0 -> Pair(-1.5f, random.nextFloat() * (maxTopY - (-0.8f)) + (-0.8f)) // Left edge
                1 -> Pair(1.5f, random.nextFloat() * (maxTopY - (-0.8f)) + (-0.8f)) // Right edge
                2 -> Pair(random.nextFloat() * 2.4f - 1.2f, maxTopY) // Top edge
                else -> Pair(random.nextFloat() * 2.4f - 1.2f, -1.2f) // Bottom edge
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

        // 1. Capa de atrás: Cielo procedural y sol
        GLES30.glUseProgram(program)
        GLES30.glUniform1f(timeHandle, time)
        GLES30.glUniform1f(aspectHandle, aspectRatio)

        val sizeConfig = configProvider.getSunSize()
        val mappedSize = 0.05f + (sizeConfig / 100f) * 0.30f // Range 0.05 to 0.35
        GLES30.glUniform1f(sunSizeHandle, mappedSize)
        
        // Pass base speed uniform for pulsation scaling inside shader (reduced by 60%)
        val speedConfig = configProvider.getSunSpeed()
        val mappedSpeed = 0.5f + (speedConfig / 100f) * 1.5f // Pulsate frequency scaling factor
        GLES30.glUniform1f(sunSpeedHandle, mappedSpeed * 0.4f)
        
        val theme = configProvider.getSunnyTheme()
        GLES30.glUniform1i(themeHandle, theme)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.02f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.02f else 0f
        GLES30.glUniform2f(sunPosHandle, sunX + gyroX, sunY + gyroY)

        // Pass touch interactive sun pulse value
        GLES30.glUniform1f(sunPulseHandle, sunPulseTime)

        val godRaysRaw = if (configProvider.isSunnyGodRaysEnabled()) configProvider.getSunnyGodRaysIntensity() / 100f else 0f
        val lowSunFactor = (1.0f - smoothstep(0.3f, 0.7f, sunY + gyroY)) * smoothstep(-0.6f, -0.2f, sunY + gyroY)
        GLES30.glUniform1f(godRaysIntensityHandle, godRaysRaw * lowSunFactor)
        GLES30.glUniform1f(sunFadeFactorHandle, pathFadeFactor)

        if (theme == 3) {
            val topColor = configProvider.getSunnyCustomSkyTopColor()
            val bottomColor = configProvider.getSunnyCustomSkyBottomColor()
            val tr = android.graphics.Color.red(topColor) / 255f
            val tg = android.graphics.Color.green(topColor) / 255f
            val tb = android.graphics.Color.blue(topColor) / 255f
            val br = android.graphics.Color.red(bottomColor) / 255f
            val bg = android.graphics.Color.green(bottomColor) / 255f
            val bb = android.graphics.Color.blue(bottomColor) / 255f
            GLES30.glUniform3f(skyTopHandle, tr, tg, tb)
            GLES30.glUniform3f(skyBottomHandle, br, bg, bb)
        }

        fullscreenQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)

        // 2. Capa intermedia: Fondo seleccionado (siluetas con calado de cielo)
        val bgIndex = configProvider.getSunnyBackgroundIndex()
        drawBackground(bgIndex)

        // 3. Capa del frente: Nubes activas (sobrevolando el paisaje)
        drawClouds()

        // 4. Partículas (Ráfaga solar)
        drawParticles()

        // 5. Capa superior de post-procesado óptico: Destellos de lente
        drawLensFlare()
    }

    private fun drawClouds() {
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
            GLES30.glUniform1f(opacityHandle, cloud.opacity)

            if (cloudTextures.isNotEmpty()) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cloudTextures[cloud.textureIndex % cloudTextures.size])
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            }
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun drawParticles() {
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
            GLES30.glUniform1f(pOpacityHandle, p.alpha)

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

    private fun drawBackground(backgroundIndex: Int) {
        if (backgroundIndex <= 0 || backgroundIndex > 7) return
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
        GLES30.glUniform2f(bgSunPosHandle, sunX + bgGyroX, sunY + bgGyroY)
        GLES30.glUniform1f(bgAspectHandle, aspectRatio)
        GLES30.glUniform1i(bgIsCustomHandle, if (backgroundIndex == 7) 1 else 0)

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
                cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio, isSunny = true)
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

    private fun drawLensFlare() {
        if (lensFlareProgram == 0) return
        val enabled = configProvider.isSunnyLensFlareEnabled()
        if (!enabled) return

        val rawIntensity = configProvider.getSunnyLensFlareIntensity() / 100f
        val lowSunFactor = (1.0f - smoothstep(0.3f, 0.7f, sunY)) * smoothstep(-0.6f, -0.2f, sunY)
        val finalIntensity = rawIntensity * lowSunFactor * pathFadeFactor

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
