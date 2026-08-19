package com.wolf.wallpaper.storm

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.GLRenderUtils
import com.wolf.wallpaper.storm.StormCloud
import com.wolf.wallpaper.core.StormObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class StormRenderer(
    private val context: Context,
    private val sceneManager: SceneManager
) : GLRenderer {

    private var cloudProgram = 0
    private var rainProgram = 0
    private var lightningProgram = 0
    private var backgroundProgram = 0
    private var windProgram = 0
    private var elapsedTime = 0f

    // Texture IDs
    private val cloudTextures = mutableListOf<Int>()
    private var rainTexture = 0
    private val lightningTextures = mutableListOf<Int>()
    private val backgroundTextures = IntArray(8)

    private var viewportWidth = 0
    private var viewportHeight = 0

    // Small prebaked noise grid sampled by storm_cloud.frag/background.frag's noise()/fbm()
    // for the Dark God Rays effect, replacing per-fragment sin()/fract() hash math.
    private var noiseTexture = 0
    private val noiseTextureSize = 64
    
    // MVP Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)

    // Aspect ratio and scaling
    private var aspectRatio = 1.0f
    private val backgroundAspectRatios = FloatArray(8) { 1.0f }

    // Buffers for geometric drawing
    private lateinit var fullscreenQuadBuffer: FloatBuffer
    private lateinit var cloudQuadBuffer: FloatBuffer
    private lateinit var lightningQuadBuffer: FloatBuffer
    private lateinit var backgroundQuadBuffer: FloatBuffer

    // Gyroscope tilt variables for cloud slicing parallax
    private var sensorTiltX = 0f
    private var sensorTiltY = 0f
    private var targetTiltX = 0f
    private var targetTiltY = 0f
    private var swipeOffset = 0f
    private var targetSwipeOffset = 0f

    // Reusable buffers to avoid allocations in drawRain and drawWindLines
    private var rainVertexBuffer: FloatBuffer? = null
    private var rainByteBuffer: ByteBuffer? = null
    private var rainCapacityBytes = 0

    private var windVertexBuffer: FloatBuffer? = null
    private var windByteBuffer: ByteBuffer? = null
    private var windCapacityBytes = 0

    // Reusable Kotlin arrays backing the vertex data before it's copied into the direct buffers above
    private var rainVertexData = FloatArray(0)
    private var windVertexData = FloatArray(0)

    // Reusable lists to avoid allocating new lists every frame when partitioning rain drops
    private val cloudRainBuffer = mutableListOf<RainDrop>()
    private val edgeRainBuffer = mutableListOf<RainDrop>()
    private val activeLightningsBuffer = mutableListOf<Lightning>()

    // Scratch matrices reused across per-object draw calls (clouds, lightning, background)
    private val scratchModelMatrix = FloatArray(16)
    private val scratchMVPMatrix = FloatArray(16)

    // Cached uniform locations, resolved once in onSurfaceCreated instead of being
    // re-queried via glGetUniformLocation (a string-hash lookup) on every single frame.

    private var cloudMvpHandle = -1
    private var cloudOpacityHandle = -1
    private var cloudTextureHandle = -1
    private var cloudFlashIntensityHandle = -1
    private var cloudFlashColorHandle = -1
    private var cloudCloudColorHandle = -1
    private var cloudHeightFactorHandle = -1
    private var cloudTimeHandle = -1
    private var cloudAspectHandle = -1
    private var cloudLightningPosHandle = -1
    private var cloudBgScaleHandle = -1
    private var cloudCloudScaleHandle = -1
    private var cloudNoiseTextureHandle = -1

    private var rainMvpHandle = -1
    private var rainColorHandleCached = -1

    private var windMvpHandle = -1
    private var windColorHandleCached = -1

    private var lightningMvpHandle = -1
    private var lightningFlashIntensityHandle = -1
    private var lightningIsTexturedHandle = -1
    private var lightningTextureHandle = -1
    private var lightningColorHandleCached = -1
    private var lightningGrowthProgressHandle = -1

    private var bgMvpHandle = -1
    private var bgFlashIntensityHandle = -1
    private var bgFlashColorHandle = -1
    private var bgTextureHandle = -1
    private var bgTimeHandle = -1
    private var bgAspectHandle = -1
    private var bgDropletsEnabledHandle = -1
    private var bgDropletsSizeHandle = -1
    private var bgRainColorHandle = -1
    private var bgRainIntensityHandle = -1
    private var bgLightningPosHandle = -1
    private var bgNoiseTextureHandle = -1
    private var bgTexCoordScaleHandle = -1

    init {
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(identityMatrix, 0)
    }

    override fun onSurfaceCreated() {
        // Enable blending for textures and transparency effects
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Compile and link shaders
        val cloudVert = readAssetFile(context, "shaders/storm_cloud.vert")
        val cloudFrag = readAssetFile(context, "shaders/storm_cloud.frag")
        cloudProgram = createProgram(cloudVert, cloudFrag)

        val rainVert = readAssetFile(context, "shaders/rain.vert")
        val rainFrag = readAssetFile(context, "shaders/rain.frag")
        rainProgram = createProgram(rainVert, rainFrag)

        val lightningVert = readAssetFile(context, "shaders/lightning.vert")
        val lightningFrag = readAssetFile(context, "shaders/lightning.frag")
        lightningProgram = createProgram(lightningVert, lightningFrag)

        val backgroundVert = readAssetFile(context, "shaders/background.vert")
        val backgroundFrag = readAssetFile(context, "shaders/background.frag")
        backgroundProgram = createProgram(backgroundVert, backgroundFrag)

        val windVert = readAssetFile(context, "shaders/wind.vert")
        val windFrag = readAssetFile(context, "shaders/wind.frag")
        windProgram = createProgram(windVert, windFrag)

        cacheUniformLocations()

        // Load textures from assets
        cloudTextures.clear()
        try {
            val assetManager = context.assets
            val files = assetManager.list("clouds") ?: emptyArray()
            val sortedFiles = files.filter { it.endsWith(".png") }.sorted()
            for (file in sortedFiles) {
                val tex = loadTexture(context, "clouds/$file", maxSize = 512)
                if (tex != 0) {
                    cloudTextures.add(tex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (cloudTextures.isEmpty()) {
            val fallbackTex = loadTexture(context, "clouds/cloud_01.png", maxSize = 512)
            if (fallbackTex != 0) {
                cloudTextures.add(fallbackTex)
            }
        }
        rainTexture = loadTexture(context, "rain/rain_particle.png")

        // Dynamically load lightning textures from assets/lightning
        lightningTextures.clear()
        try {
            val assetManager = context.assets
            val files = assetManager.list("lightning") ?: emptyArray()
            for (file in files) {
                if (file.endsWith(".png")) {
                    val tex = loadTexture(context, "lightning/$file", maxSize = 1024)
                    if (tex != 0) {
                        lightningTextures.add(tex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Load background textures
        backgroundTextures.indices.forEach { backgroundTextures[it] = 0 }
        loadBackgroundTexture(context, 0, "background/background.jpg")
        loadBackgroundTexture(context, 1, "background/background_02.png")
        loadBackgroundTexture(context, 2, "background/background_03.png")
        loadBackgroundTexture(context, 3, "background/background_04.png")
        loadBackgroundTexture(context, 4, "background/background_05.png")
        loadBackgroundTexture(context, 5, "background/background_06.png")
        loadBackgroundTexture(context, 6, "background/background_07.png")
        loadCustomBackgroundTexture(context, 7)

        noiseTexture = createNoiseTexture(noiseTextureSize)

        // Screen quad coordinates (for full-screen flash using identity matrix)
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

        // Cloud quad coordinates with UV coordinates: (X, Y, U, V)
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

        // Lightning quad coordinates with UV coordinates: (X, Y, U, V)
        val lightningCoords = floatArrayOf(
            -0.5f,  0.5f, 0f, 0f,
            -0.5f, -0.5f, 0f, 1f,
             0.5f,  0.5f, 1f, 0f,
             0.5f, -0.5f, 1f, 1f
        )
        lightningQuadBuffer = ByteBuffer.allocateDirect(lightningCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(lightningCoords)
                position(0)
            }

        // Background quad coordinates with UV coordinates: (X, Y, U, V)
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

    private fun cacheUniformLocations() {
        cloudMvpHandle = GLES30.glGetUniformLocation(cloudProgram, "uMVPMatrix")
        cloudOpacityHandle = GLES30.glGetUniformLocation(cloudProgram, "uOpacity")
        cloudTextureHandle = GLES30.glGetUniformLocation(cloudProgram, "uTexture")
        cloudFlashIntensityHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashIntensity")
        cloudFlashColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashColor")
        cloudCloudColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uCloudColor")
        cloudHeightFactorHandle = GLES30.glGetUniformLocation(cloudProgram, "uHeightFactor")
        cloudTimeHandle = GLES30.glGetUniformLocation(cloudProgram, "uTime")
        cloudAspectHandle = GLES30.glGetUniformLocation(cloudProgram, "uAspectRatio")
        cloudLightningPosHandle = GLES30.glGetUniformLocation(cloudProgram, "uLightningPos")
        cloudBgScaleHandle = GLES30.glGetUniformLocation(cloudProgram, "uBgScale")
        cloudCloudScaleHandle = GLES30.glGetUniformLocation(cloudProgram, "uCloudScale")
        cloudNoiseTextureHandle = GLES30.glGetUniformLocation(cloudProgram, "uNoiseTexture")

        rainMvpHandle = GLES30.glGetUniformLocation(rainProgram, "uMVPMatrix")
        rainColorHandleCached = GLES30.glGetUniformLocation(rainProgram, "uRainColor")

        windMvpHandle = GLES30.glGetUniformLocation(windProgram, "uMVPMatrix")
        windColorHandleCached = GLES30.glGetUniformLocation(windProgram, "uWindColor")

        lightningMvpHandle = GLES30.glGetUniformLocation(lightningProgram, "uMVPMatrix")
        lightningFlashIntensityHandle = GLES30.glGetUniformLocation(lightningProgram, "uFlashIntensity")
        lightningIsTexturedHandle = GLES30.glGetUniformLocation(lightningProgram, "uIsTextured")
        lightningTextureHandle = GLES30.glGetUniformLocation(lightningProgram, "uTexture")
        lightningColorHandleCached = GLES30.glGetUniformLocation(lightningProgram, "uLightningColor")
        lightningGrowthProgressHandle = GLES30.glGetUniformLocation(lightningProgram, "uGrowthProgress")

        bgMvpHandle = GLES30.glGetUniformLocation(backgroundProgram, "uMVPMatrix")
        bgFlashIntensityHandle = GLES30.glGetUniformLocation(backgroundProgram, "uFlashIntensity")
        bgFlashColorHandle = GLES30.glGetUniformLocation(backgroundProgram, "uFlashColor")
        bgTextureHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTexture")
        bgTimeHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTime")
        bgAspectHandle = GLES30.glGetUniformLocation(backgroundProgram, "uAspectRatio")
        bgDropletsEnabledHandle = GLES30.glGetUniformLocation(backgroundProgram, "uScreenDropletsEnabled")
        bgDropletsSizeHandle = GLES30.glGetUniformLocation(backgroundProgram, "uScreenDropletsSize")
        bgRainColorHandle = GLES30.glGetUniformLocation(backgroundProgram, "uRainColor")
        bgRainIntensityHandle = GLES30.glGetUniformLocation(backgroundProgram, "uRainIntensity")
        bgLightningPosHandle = GLES30.glGetUniformLocation(backgroundProgram, "uLightningPos")
        bgNoiseTextureHandle = GLES30.glGetUniformLocation(backgroundProgram, "uNoiseTexture")
        bgTexCoordScaleHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTexCoordScale")
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        // Setup orthographic camera matrix (RF-006: Keeps proportions in rotation/scale)
        Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        sceneManager.onSurfaceChanged(width, height)
    }

    // Builds a small tileable grid of random single-channel values sampled by
    // storm_cloud.frag/background.frag's noise()/fbm() for the Dark God Rays effect.
    // GL_LINEAR + GL_REPEAT reproduces the same bilinear-interpolated value noise the
    // shaders used to compute per-fragment via sin()/fract() hash math, but as one
    // hardware-filtered texture fetch instead of four hash calls per octave.
    private fun createNoiseTexture(size: Int): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]
        if (textureId == 0) return 0

        val random = kotlin.random.Random(0)
        val buffer = ByteBuffer.allocateDirect(size * size).order(ByteOrder.nativeOrder())
        repeat(size * size) {
            buffer.put(random.nextInt(256).toByte())
        }
        buffer.position(0)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R8, size, size, 0, GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, buffer)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return textureId
    }
    override fun onUpdate(deltaTime: Float) {
        elapsedTime += deltaTime
        val lerpFactor = 5f * deltaTime
        sensorTiltX += (targetTiltX - sensorTiltX) * lerpFactor
        sensorTiltY += (targetTiltY - sensorTiltY) * lerpFactor
        
        // Smoothly interpolate the swipe offset to make dragging/swiping look soft and fluid
        val swipeLerpFactor = 4f * deltaTime
        swipeOffset += (targetSwipeOffset - swipeOffset) * swipeLerpFactor
        
        sceneManager.update(deltaTime)
    }

    override fun onDrawFrame() {
        drawFrame(sceneManager)
    }

    override fun onTouchEvent(x: Float, y: Float) {
        sceneManager.queueTouch(x, y)
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        val clampedX = xOffset.coerceIn(0f, 1f)
        targetSwipeOffset = (clampedX - 0.5f) * 2.0f
    }

    override fun onSensorValuesChanged(tiltX: Float, tiltY: Float) {
        if (sceneManager.configProvider.isSunnyGyroEnabled()) {
            targetTiltX = tiltX
            targetTiltY = tiltY
        } else {
            targetTiltX = 0f
            targetTiltY = 0f
        }
    }

    /**
     * Aggregated lightning-flash contribution over some subset of [Lightning]s (the exact
     * subset depends on the caller's predicate — see [aggregateLightningFlash]). Computed once
     * per frame and reused wherever the same subset would otherwise be re-aggregated.
     */
    private data class LightningFlashAggregate(
        val maxIntensity: Float = 0f,
        val resolvedR: Float = 0f,
        val resolvedG: Float = 0f,
        val resolvedB: Float = 0f,
        val activeCount: Int = 0,
        val brightest: Lightning? = null
    )

    private val emptyLightningFlash = LightningFlashAggregate()

    private inline fun aggregateLightningFlash(
        lightnings: List<Lightning>,
        predicate: (Lightning) -> Boolean
    ): LightningFlashAggregate {
        var maxIntensity = 0f
        var resolvedR = 0f
        var resolvedG = 0f
        var resolvedB = 0f
        var activeCount = 0
        var brightest: Lightning? = null
        for (lightning in lightnings) {
            if (predicate(lightning)) {
                if (lightning.intensity > maxIntensity) {
                    maxIntensity = lightning.intensity
                    brightest = lightning
                }
                val color = lightning.customColor ?: getLightningColor(lightning.selectedColorIndex)
                resolvedR += color[0] * lightning.intensity
                resolvedG += color[1] * lightning.intensity
                resolvedB += color[2] * lightning.intensity
                activeCount++
            }
        }
        return LightningFlashAggregate(maxIntensity, resolvedR, resolvedG, resolvedB, activeCount, brightest)
    }

    private fun drawFrame(sceneManager: SceneManager) {

        // Clear background with deep dark storm color
        var clearR = 0.04f
        var clearG = 0.04f
        var clearB = 0.06f

        val lightningFlashEnabled = sceneManager.isLightningFlashEnabled()
        val cloudFlashEnabled = sceneManager.isCloudFlashEnabled()

        // Apply lightning flash peak color directly to background if enabled. drawBackground()
        // and drawLightning() need this exact same aggregate (same predicate, same lightning
        // list) below, so it's computed once here and threaded through instead of each of the
        // three re-running the same loop over sceneManager.lightnings every frame.
        val stormFlash = if (lightningFlashEnabled) {
            aggregateLightningFlash(sceneManager.lightnings) { it.isActive && !it.isInternalOnly }
        } else {
            emptyLightningFlash
        }

        if (stormFlash.activeCount > 0) {
            val flashCoeff = stormFlash.maxIntensity * 0.45f
            clearR += (stormFlash.resolvedR / stormFlash.activeCount) * flashCoeff
            clearG += (stormFlash.resolvedG / stormFlash.activeCount) * flashCoeff
            clearB += (stormFlash.resolvedB / stormFlash.activeCount) * flashCoeff
        }

        GLES30.glClearColor(clearR, clearG, clearB, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Render layered elements in order: Fondo -> Gotas de Nube -> Rayos -> Nubes -> Gotas de Borde
        val allRain = sceneManager.getRainDrops()
        cloudRainBuffer.clear()
        edgeRainBuffer.clear()
        for (drop in allRain) {
            if (drop.spawnFromCloud) cloudRainBuffer.add(drop) else edgeRainBuffer.add(drop)
        }

        drawBackground(sceneManager.getBackgroundIndex(), stormFlash)
        drawRain(cloudRainBuffer, sceneManager.getRainColorIndex())
        drawLightning(sceneManager.lightnings, stormFlash)
        if (sceneManager.isWindLinesEnabled() && sceneManager.getWindIntensity() >= 50) {
            sceneManager.sortWindLinesByDepth()
            drawWindLines(sceneManager.getWindLines())
        }
        sceneManager.sortCloudsByDepth()
        drawClouds(sceneManager.getClouds(), sceneManager.lightnings, lightningFlashEnabled, cloudFlashEnabled)
        drawRain(edgeRainBuffer, sceneManager.getRainColorIndex())
    }

    private fun drawClouds(clouds: List<StormCloud>, lightnings: List<Lightning>, lightningFlashEnabled: Boolean, cloudFlashEnabled: Boolean) {
        if (clouds.isEmpty()) return

        GLES30.glUseProgram(cloudProgram)
        val mvpMatrixHandle = cloudMvpHandle
        val opacityHandle = cloudOpacityHandle
        val textureHandle = cloudTextureHandle
        val flashIntensityHandle = cloudFlashIntensityHandle
        val flashColorHandle = cloudFlashColorHandle
        val cloudColorHandle = cloudCloudColorHandle
        val heightFactorHandle = cloudHeightFactorHandle
        GLES30.glUniform3f(cloudColorHandle, 0.7f, 0.7f, 0.7f)

        // Clouds react to global real lightnings (not internalOnly)
        val globalLightningFlash = aggregateLightningFlash(lightnings) { lightning ->
            lightning.isActive && !lightning.isInternalOnly && lightningFlashEnabled
        }

        // Bind additional uniforms for refraction and dark god rays
        val timeHandle = cloudTimeHandle
        val aspectHandle = cloudAspectHandle
        val lightningPosHandle = cloudLightningPosHandle
        val bgScaleHandle = cloudBgScaleHandle
        val cloudScaleHandle = cloudCloudScaleHandle

        GLES30.glUniform1f(timeHandle, elapsedTime)
        GLES30.glUniform1f(aspectHandle, aspectRatio)

        val cloudBrightest = globalLightningFlash.brightest
        if (cloudBrightest != null) {
            GLES30.glUniform2f(lightningPosHandle, cloudBrightest.positionX, cloudBrightest.positionY)
        } else {
            GLES30.glUniform2f(lightningPosHandle, 0f, 1f)
        }

        // Pass background scale to keep coordinates matched
        val bgIndex = sceneManager.getBackgroundIndex()
        val texIndex = (bgIndex - 1).coerceIn(0, backgroundAspectRatios.size - 1)
        val bgAspect = backgroundAspectRatios[texIndex]
        val screenAspect = aspectRatio
        var scaleX = 1.0f
        var scaleY = 1.0f
        if (screenAspect > bgAspect) {
            scaleX = screenAspect
            scaleY = screenAspect / bgAspect
        } else {
            scaleX = bgAspect
            scaleY = 1.0f
        }
        GLES30.glUniform2f(bgScaleHandle, scaleX, scaleY)

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

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTexture)
        GLES30.glUniform1i(cloudNoiseTextureHandle, 1)

        val gyroX = if (sceneManager.configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.12f else 0f
        val gyroY = if (sceneManager.configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.12f else 0f

        // Already sorted by depth in place by SceneManager.sortCloudsByDepth()
        for (cloud in clouds) {
            val finalIntensity = (globalLightningFlash.maxIntensity + cloud.flashIntensity).coerceIn(0f, 1.5f)
            val finalR: Float
            val finalG: Float
            val finalB: Float
            if (globalLightningFlash.maxIntensity > cloud.flashIntensity) {
                finalR = if (globalLightningFlash.activeCount > 0) (globalLightningFlash.resolvedR / globalLightningFlash.activeCount).coerceIn(0f, 1f) else 1f
                finalG = if (globalLightningFlash.activeCount > 0) (globalLightningFlash.resolvedG / globalLightningFlash.activeCount).coerceIn(0f, 1f) else 1f
                finalB = if (globalLightningFlash.activeCount > 0) (globalLightningFlash.resolvedB / globalLightningFlash.activeCount).coerceIn(0f, 1f) else 1f
            } else {
                finalR = cloud.flashColor[0]
                finalG = cloud.flashColor[1]
                finalB = cloud.flashColor[2]
            }
            GLES30.glUniform1f(flashIntensityHandle, finalIntensity)
            GLES30.glUniform3f(flashColorHandle, finalR, finalG, finalB)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            if (cloudTextures.isNotEmpty()) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cloudTextures[cloud.textureIndex % cloudTextures.size])
            }
            GLES30.glUniform2f(cloudScaleHandle, cloud.scale * 2.4f, cloud.scale)

            // Gyroscope depth-based parallax shift
            // Foreground clouds (z = 1.0) shift more; background clouds (z = 0.3) shift less
            val depthFactor = 0.2f + 0.8f * ((cloud.z - 0.3f) / 0.7f).coerceIn(0f, 1f)
            val cloudGyroX = gyroX * depthFactor
            val cloudGyroY = gyroY * depthFactor

            // Swipe parallax shift (moving in the same direction as the user's swipe/drag)
            val maxSwipeShift = aspectRatio * 0.3f
            val cloudSwipeX = -swipeOffset * maxSwipeShift * depthFactor

            val posX = cloud.positionX + cloudGyroX + cloudSwipeX
            val posY = cloud.positionY + cloudGyroY

            val modelMatrix = scratchModelMatrix
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, posX, posY, 0f)
            Matrix.scaleM(modelMatrix, 0, cloud.scale * 2.4f, cloud.scale, 1.0f)

            val modelViewProjection = scratchMVPMatrix
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)
            
            // Restore original cloud color shading, height factor, and opacity
            GLES30.glUniform3f(cloudColorHandle, 0.7f, 0.7f, 0.7f)
            val heightFactor = ((cloud.z - 0.3f) / 0.7f).coerceIn(0f, 1f)
            GLES30.glUniform1f(heightFactorHandle, heightFactor)
            GLES30.glUniform1f(opacityHandle, cloud.opacity)

            if (cloudTextures.isNotEmpty()) {
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            }
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun drawRain(rainDrops: List<RainDrop>, colorIndex: Int) {
        if (rainDrops.isEmpty()) return

        GLES30.glUseProgram(rainProgram)
        val mvpMatrixHandle = rainMvpHandle
        val colorHandle = rainColorHandleCached
        
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        val color = getRainColor(colorIndex)
        GLES30.glUniform4fv(colorHandle, 1, color, 0)

        // Rain drops are already sorted by depth (z) in place by SceneManager.sortRainDropsByDepth()
        val sortedDrops = rainDrops

        // Construct interleaved coordinate buffer (X, Y, Depth, U, V, Opacity) for all rain drop quads
        // Each vertex has 6 floats. 6 vertices (2 triangles) per drop.
        val requiredSize = sortedDrops.size * 36
        if (rainVertexData.size < requiredSize) {
            rainVertexData = FloatArray(requiredSize)
        }
        val vertexData = rainVertexData
        var idx = 0
        for (drop in sortedDrops) {
            val zDepth = drop.z
            // halfW is set to R * drop.length (R=0.50 in rain.frag) so the
            // head circle maps to a round shape. Multiplier 0.08 narrows the
            // drop significantly so it looks thin and elegant.
            val halfW = (drop.length * 0.11f).coerceAtLeast(0.001f)
            
            val x1 = drop.positionX
            val y1 = drop.positionY
            val x2 = drop.positionX + drop.dirX * drop.length
            val y2 = drop.positionY + drop.dirY * drop.length
            
            // Tail tapers to a narrow point (10% of head width) so geometry
            // itself forms a teardrop — no more square tip at low resolutions.
            val tailHalfW = halfW * 0.10f
            val tailOffsetX = -drop.dirY * tailHalfW
            val tailOffsetY = drop.dirX * tailHalfW
            // Head keeps full width for the round bulb
            val headOffsetX = -drop.dirY * halfW
            val headOffsetY = drop.dirX * halfW
            
            // Calculate opacity based on parent cloud (if any)
            val opacity = drop.parentCloud?.opacity ?: 1.0f
            
            // Triangle 1: Tail Left, Tail Right, Head Left
            // Vertex 1: Tail Left
            vertexData[idx++] = x1 - tailOffsetX
            vertexData[idx++] = y1 - tailOffsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.45f  // near center U for narrow tip
            vertexData[idx++] = 0.0f
            vertexData[idx++] = opacity
            
            // Vertex 2: Tail Right
            vertexData[idx++] = x1 + tailOffsetX
            vertexData[idx++] = y1 + tailOffsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.55f  // near center U for narrow tip
            vertexData[idx++] = 0.0f
            vertexData[idx++] = opacity
            
            // Vertex 3: Head Left
            vertexData[idx++] = x2 - headOffsetX
            vertexData[idx++] = y2 - headOffsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.0f
            vertexData[idx++] = 1.0f
            vertexData[idx++] = opacity
            
            // Triangle 2: Head Left, Tail Right, Head Right
            // Vertex 4: Head Left
            vertexData[idx++] = x2 - headOffsetX
            vertexData[idx++] = y2 - headOffsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.0f
            vertexData[idx++] = 1.0f
            vertexData[idx++] = opacity
            
            // Vertex 5: Tail Right
            vertexData[idx++] = x1 + tailOffsetX
            vertexData[idx++] = y1 + tailOffsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.55f  // near center U for narrow tip
            vertexData[idx++] = 0.0f
            vertexData[idx++] = opacity
            
            // Vertex 6: Head Right
            vertexData[idx++] = x2 + headOffsetX
            vertexData[idx++] = y2 + headOffsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 1.0f
            vertexData[idx++] = 1.0f
            vertexData[idx++] = opacity
        }

        val rainBuffer = getRainBuffer(requiredSize).apply {
            put(vertexData, 0, requiredSize)
            position(0)
        }

        // Bind positions attribute (location 0, stride 24 bytes: 6 floats)
        rainBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 24, rainBuffer)
        GLES30.glEnableVertexAttribArray(0)

        // Bind depth attribute (location 1, stride 24 bytes: 6 floats)
        rainBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, 24, rainBuffer)
        GLES30.glEnableVertexAttribArray(1)

        // Bind texCoord attribute (location 2, stride 24 bytes: 6 floats)
        rainBuffer.position(3)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 24, rainBuffer)
        GLES30.glEnableVertexAttribArray(2)

        // Bind cloudOpacity attribute (location 3, stride 24 bytes: 6 floats)
        rainBuffer.position(5)
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, 24, rainBuffer)
        GLES30.glEnableVertexAttribArray(3)

        // Draw all drops as quads using a single draw call (GL_TRIANGLES)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, sortedDrops.size * 6)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
        GLES30.glDisableVertexAttribArray(3)
    }

    private fun drawWindLines(windLines: List<WindLine>) {
        if (windLines.isEmpty()) return

        GLES30.glUseProgram(windProgram)
        val mvpMatrixHandle = windMvpHandle
        val colorHandle = windColorHandleCached

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES30.glUniform4f(colorHandle, 0.85f, 0.90f, 0.98f, 0.35f)

        // Wind lines are already sorted by depth (z) in place by SceneManager.sortWindLinesByDepth()
        val sortedLines = windLines

        val requiredSize = sortedLines.size * 8
        if (windVertexData.size < requiredSize) {
            windVertexData = FloatArray(requiredSize)
        }
        val vertexData = windVertexData
        var idx = 0
        // Lines are already sorted by depth (z ascending), so the far/medium/near bucket
        // counts needed below for the three depth-ordered draw calls are accumulated in this
        // same pass instead of re-iterating sortedLines a second time just to count them.
        var countFar = 0
        var countMedium = 0
        var countNear = 0
        for (line in sortedLines) {
            val progress = line.lifetime / line.maxLifetime
            val fadeIn = (progress / 0.15f).coerceIn(0f, 1f)
            val fadeOut = ((1f - progress) / 0.15f).coerceIn(0f, 1f)
            val lineAlpha = fadeIn * fadeOut * line.alphaMultiplier

            // Start vertex
            vertexData[idx++] = line.positionX
            vertexData[idx++] = line.positionY
            vertexData[idx++] = line.z
            vertexData[idx++] = lineAlpha

            // End vertex
            vertexData[idx++] = line.positionX + line.dirX * line.length
            vertexData[idx++] = line.positionY + line.dirY * line.length
            vertexData[idx++] = line.z
            vertexData[idx++] = lineAlpha * 0.25f

            when {
                line.z < 0.4f -> countFar++
                line.z < 0.7f -> countMedium++
                else -> countNear++
            }
        }

        val windBuffer = getWindBuffer(requiredSize).apply {
            put(vertexData, 0, requiredSize)
            position(0)
        }

        windBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, windBuffer)
        GLES30.glEnableVertexAttribArray(0)

        windBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, 16, windBuffer)
        GLES30.glEnableVertexAttribArray(1)

        windBuffer.position(3)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, 16, windBuffer)
        GLES30.glEnableVertexAttribArray(2)

        if (countFar > 0) {
            GLES30.glLineWidth(1.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, countFar * 2)
        }

        if (countMedium > 0) {
            GLES30.glLineWidth(2.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, countFar * 2, countMedium * 2)
        }

        if (countNear > 0) {
            GLES30.glLineWidth(3.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, (countFar + countMedium) * 2, countNear * 2)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    // Precomputed once instead of allocating a new FloatArray on every call (called
    // multiple times per frame, e.g. once per active lightning, every frame).
    private val rainColors = arrayOf(
        floatArrayOf(0.7f, 0.8f, 1.0f, 0.7f), // Azul Claro
        floatArrayOf(1.0f, 1.0f, 1.0f, 0.7f), // Blanco
        floatArrayOf(1.0f, 0.2f, 0.2f, 0.7f), // Rojo
        floatArrayOf(0.2f, 1.0f, 0.2f, 0.7f), // Verde
        floatArrayOf(1.0f, 0.9f, 0.0f, 0.7f), // Amarillo
        floatArrayOf(0.7f, 0.3f, 1.0f, 0.7f)  // Morado
    )

    private fun getRainColor(index: Int): FloatArray = rainColors.getOrElse(index) { rainColors[0] }

    private fun drawLightning(lightnings: List<Lightning>, flash: LightningFlashAggregate) {
        activeLightningsBuffer.clear()
        for (lightning in lightnings) {
            if (lightning.isActive && !lightning.isInternalOnly) activeLightningsBuffer.add(lightning)
        }
        val activeLightnings = activeLightningsBuffer
        if (activeLightnings.isEmpty()) return

        GLES30.glUseProgram(lightningProgram)
        val mvpMatrixHandle = lightningMvpHandle
        val flashIntensityHandle = lightningFlashIntensityHandle
        val isTexturedHandle = lightningIsTexturedHandle
        val textureHandle = lightningTextureHandle
        val colorHandle = lightningColorHandleCached
        val growthProgressHandle = lightningGrowthProgressHandle

        // 1. Draw a single combined full screen overlay flash.
        // `flash` is the same isActive && !isInternalOnly aggregate drawFrame() already
        // computed over this exact list, so activeLightnings.size == flash.activeCount here.
        if (flash.activeCount > 0) {
            GLES30.glUniform1i(isTexturedHandle, 0)
            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, identityMatrix, 0)
            GLES30.glUniform1f(flashIntensityHandle, flash.maxIntensity * 0.20f)
            GLES30.glUniform1f(growthProgressHandle, 1.0f)

            GLES30.glUniform4f(
                colorHandle,
                (flash.resolvedR / flash.activeCount).coerceIn(0f, 1f),
                (flash.resolvedG / flash.activeCount).coerceIn(0f, 1f),
                (flash.resolvedB / flash.activeCount).coerceIn(0f, 1f),
                1.0f
            )

            fullscreenQuadBuffer.position(0)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }

        // 2. Draw textured lightning bolt for each active lightning
        GLES30.glUniform1i(isTexturedHandle, 1)

        // Bind position coordinates (location 0)
        lightningQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, lightningQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        // Bind UV texture coordinates (location 1)
        lightningQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, lightningQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        val gyroX = if (sceneManager.configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.12f else 0f
        val gyroY = if (sceneManager.configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.12f else 0f
        val maxSwipeShift = aspectRatio * 0.3f

        for (lightning in activeLightnings) {
            if (lightningTextures.isNotEmpty() && lightning.selectedTextureIndex < lightningTextures.size) {
                GLES30.glUniform1f(flashIntensityHandle, lightning.intensity)
                val color = lightning.customColor ?: getLightningColor(lightning.selectedColorIndex)
                GLES30.glUniform4fv(colorHandle, 1, color, 0)
                GLES30.glUniform1f(growthProgressHandle, lightning.growthProgress)

                val modelMatrix = scratchModelMatrix
                Matrix.setIdentityM(modelMatrix, 0)

                // Gyroscope depth-based parallax shift
                val depthFactor = 0.2f + 0.8f * ((lightning.z - 0.3f) / 0.7f).coerceIn(0f, 1f)
                val lightningGyroX = gyroX * depthFactor
                val lightningGyroY = gyroY * depthFactor
                val lightningSwipeX = -swipeOffset * maxSwipeShift * depthFactor

                val posX = lightning.positionX + lightningGyroX + lightningSwipeX
                val posY = lightning.positionY + lightningGyroY

                // Apply world translation
                Matrix.translateM(modelMatrix, 0, posX, posY, 0f)

                if (lightning.rotationAngle != 0f) {
                    val halfHeight = lightning.scaleY * 0.5f
                    Matrix.translateM(modelMatrix, 0, 0f, halfHeight, 0f)
                    Matrix.rotateM(modelMatrix, 0, lightning.rotationAngle, 0f, 0f, 1f)
                    Matrix.translateM(modelMatrix, 0, 0f, -halfHeight, 0f)
                }

                // Apply scale
                Matrix.scaleM(modelMatrix, 0, lightning.scaleX, lightning.scaleY, 1.0f)

                val modelViewProjection = scratchMVPMatrix
                Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lightningTextures[lightning.selectedTextureIndex])
                GLES30.glUniform1i(textureHandle, 0)

                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            }
        }
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(0)
    }

    // Precomputed once instead of allocating a new FloatArray on every call.
    private val lightningColors = arrayOf(
        floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), // Blanco
        floatArrayOf(0.4f, 0.6f, 1.0f, 1.0f), // Azul
        floatArrayOf(1.0f, 0.9f, 0.2f, 1.0f), // Amarillo
        floatArrayOf(1.0f, 0.2f, 0.2f, 1.0f), // Rojo
        floatArrayOf(0.2f, 1.0f, 0.2f, 1.0f), // Verde
        floatArrayOf(0.8f, 0.3f, 1.0f, 1.0f)  // Morado
    )

    private fun getLightningColor(index: Int): FloatArray = lightningColors.getOrElse(index) { lightningColors[0] }

    private fun readAssetFile(context: Context, path: String): String =
        GLRenderUtils.readAssetFile(context, path)

    private fun loadTexture(context: Context, assetPath: String, maxSize: Int = 0): Int =
        GLRenderUtils.loadTexture(context, assetPath, maxSize)

    private fun createProgram(vertexCode: String, fragmentCode: String): Int =
        GLRenderUtils.createProgram(vertexCode, fragmentCode)

    private fun loadBackgroundTexture(context: Context, index: Int, assetPath: String) {
        val result = GLRenderUtils.loadBackgroundTextureFromAsset(context, assetPath) ?: return
        val (textureId, aspectRatio) = result
        backgroundAspectRatios[index] = aspectRatio
        backgroundTextures[index] = textureId
    }

    private fun loadCustomBackgroundTexture(context: Context, index: Int) {
        val file = java.io.File(context.filesDir, "custom_background.png")
        val result = GLRenderUtils.loadBackgroundTextureFromFile(file) ?: return
        val (textureId, aspectRatio) = result
        backgroundAspectRatios[index] = aspectRatio
        backgroundTextures[index] = textureId
    }

    private fun drawBackground(backgroundIndex: Int, flash: LightningFlashAggregate) {
        if (backgroundIndex <= 0 || backgroundIndex > backgroundTextures.size) return
        val texIndex = backgroundIndex - 1
        val textureId = backgroundTextures[texIndex]
        if (textureId == 0) return

        GLES30.glUseProgram(backgroundProgram)
        val mvpMatrixHandle = bgMvpHandle
        val flashIntensityHandle = bgFlashIntensityHandle
        val flashColorHandle = bgFlashColorHandle
        val textureHandle = bgTextureHandle
        val timeHandle = bgTimeHandle
        val aspectHandle = bgAspectHandle
        val dropletsEnabledHandle = bgDropletsEnabledHandle
        val dropletsSizeHandle = bgDropletsSizeHandle
        val rainColorHandle = bgRainColorHandle
        val rainIntensityHandle = bgRainIntensityHandle

        backgroundQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, backgroundQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        backgroundQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, backgroundQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(textureHandle, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTexture)
        GLES30.glUniform1i(bgNoiseTextureHandle, 1)

        val modelMatrix = scratchModelMatrix
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

        val texCoordScaleX = screenAspect / scaleX
        val texCoordScaleY = 1.0f / scaleY

        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1.0f)

        val modelViewProjection = scratchMVPMatrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)

        // `flash` is precomputed once in drawFrame() over this exact predicate
        // (isActive && !isInternalOnly) instead of being re-aggregated here every frame.
        GLES30.glUniform1f(flashIntensityHandle, flash.maxIntensity)
        if (flash.activeCount > 0) {
            GLES30.glUniform3f(
                flashColorHandle,
                (flash.resolvedR / flash.activeCount).coerceIn(0f, 1f),
                (flash.resolvedG / flash.activeCount).coerceIn(0f, 1f),
                (flash.resolvedB / flash.activeCount).coerceIn(0f, 1f)
            )
        } else {
            GLES30.glUniform3f(flashColorHandle, 1.0f, 1.0f, 1.0f)
        }
        GLES30.glUniform1f(timeHandle, elapsedTime)
        GLES30.glUniform1f(aspectHandle, screenAspect)
        GLES30.glUniform2f(bgTexCoordScaleHandle, texCoordScaleX, texCoordScaleY)
        GLES30.glUniform1f(dropletsEnabledHandle, if (sceneManager.isScreenDropletsEnabled()) 1.0f else 0.0f)
        val sizeFactor = (sceneManager.getScreenDropletsSize() / 100f) * 3.0f
        GLES30.glUniform1f(dropletsSizeHandle, sizeFactor)
        val rainColor = getRainColor(sceneManager.getRainColorIndex())
        GLES30.glUniform3f(rainColorHandle, rainColor[0], rainColor[1], rainColor[2])
        GLES30.glUniform1f(rainIntensityHandle, sceneManager.getRainIntensity() / 100.0f)

        // Pass lightning position for Rim Lighting and Dark God Rays
        val bgBrightest = flash.brightest
        if (bgBrightest != null) {
            GLES30.glUniform2f(bgLightningPosHandle, bgBrightest.positionX, bgBrightest.positionY)
        } else {
            GLES30.glUniform2f(bgLightningPosHandle, 0f, 1f)
        }

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun getRainBuffer(requiredFloats: Int): FloatBuffer {
        val requiredBytes = requiredFloats * 4
        var currentByteBuf = rainByteBuffer
        var currentFloatBuf = rainVertexBuffer
        if (currentByteBuf == null || rainCapacityBytes < requiredBytes) {
            val newCapacity = requiredBytes * 2
            currentByteBuf = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.nativeOrder())
            currentFloatBuf = currentByteBuf.asFloatBuffer()
            rainByteBuffer = currentByteBuf
            rainVertexBuffer = currentFloatBuf
            rainCapacityBytes = newCapacity
        }
        currentFloatBuf!!.clear()
        return currentFloatBuf
    }

    private fun getWindBuffer(requiredFloats: Int): FloatBuffer {
        val requiredBytes = requiredFloats * 4
        var currentByteBuf = windByteBuffer
        var currentFloatBuf = windVertexBuffer
        if (currentByteBuf == null || windCapacityBytes < requiredBytes) {
            val newCapacity = requiredBytes * 2
            currentByteBuf = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.nativeOrder())
            currentFloatBuf = currentByteBuf.asFloatBuffer()
            windByteBuffer = currentByteBuf
            windVertexBuffer = currentFloatBuf
            windCapacityBytes = newCapacity
        }
        currentFloatBuf!!.clear()
        return currentFloatBuf
    }
}
