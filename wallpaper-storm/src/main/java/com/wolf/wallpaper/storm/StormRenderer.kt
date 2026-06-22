package com.wolf.wallpaper.storm

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.Cloud
import com.wolf.wallpaper.core.StormObject
import java.io.IOException
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
    private var elapsedTime = 0f

    // Texture IDs
    private val cloudTextures = mutableListOf<Int>()
    private var rainTexture = 0
    private val lightningTextures = mutableListOf<Int>()
    private val backgroundTextures = IntArray(8)
    
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

    init {
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(identityMatrix, 0)
    }

    override fun onSurfaceCreated() {
        // Enable blending for textures and transparency effects
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Compile and link shaders
        val cloudVert = readAssetFile(context, "shaders/cloud.vert")
        val cloudFrag = readAssetFile(context, "shaders/cloud.frag")
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

        // Load textures from assets
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
        rainTexture = loadTexture(context, "rain/rain_particle.png")

        // Dynamically load lightning textures from assets/lightning
        lightningTextures.clear()
        try {
            val assetManager = context.assets
            val files = assetManager.list("lightning") ?: emptyArray()
            for (file in files) {
                if (file.endsWith(".png")) {
                    val tex = loadTexture(context, "lightning/$file")
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

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        // Setup orthographic camera matrix (RF-006: Keeps proportions in rotation/scale)
        Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        sceneManager.onSurfaceChanged(width, height)
    }

    override fun onUpdate(deltaTime: Float) {
        elapsedTime += deltaTime
        sceneManager.update(deltaTime)
    }

    override fun onDrawFrame() {
        drawFrame(sceneManager)
    }

    override fun onTouchEvent(x: Float, y: Float) {
        sceneManager.queueTouch(x, y)
    }

    private fun drawFrame(sceneManager: SceneManager) {
        // Clear background with deep dark storm color
        var clearR = 0.04f
        var clearG = 0.04f
        var clearB = 0.06f

        val lightningFlashEnabled = sceneManager.isLightningFlashEnabled()
        val cloudFlashEnabled = sceneManager.isCloudFlashEnabled()

        // Apply lightning flash peak color directly to background if enabled
        var maxIntensity = 0f
        var resolvedColor = floatArrayOf(0f, 0f, 0f, 0f)
        var activeCount = 0

        if (lightningFlashEnabled) {
            for (lightning in sceneManager.lightnings) {
                if (lightning.isActive && !lightning.isInternalOnly) {
                    if (lightning.intensity > maxIntensity) {
                        maxIntensity = lightning.intensity
                    }
                    val color = getLightningColor(lightning.selectedColorIndex)
                    resolvedColor[0] += color[0] * lightning.intensity
                    resolvedColor[1] += color[1] * lightning.intensity
                    resolvedColor[2] += color[2] * lightning.intensity
                    activeCount++
                }
            }

            if (activeCount > 0) {
                val flashCoeff = maxIntensity * 0.45f
                val r = (resolvedColor[0] / activeCount) * flashCoeff
                val g = (resolvedColor[1] / activeCount) * flashCoeff
                val b = (resolvedColor[2] / activeCount) * flashCoeff
                clearR += r
                clearG += g
                clearB += b
            }
        }

        GLES30.glClearColor(clearR, clearG, clearB, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Render layered elements in order: Fondo -> Lluvia -> Rayos -> Nubes
        drawBackground(sceneManager.getBackgroundIndex(), sceneManager.lightnings, lightningFlashEnabled)
        drawRain(sceneManager.getRainDrops(), sceneManager.getRainColorIndex())
        drawLightning(sceneManager.lightnings, lightningFlashEnabled)
        drawClouds(sceneManager.getClouds(), sceneManager.lightnings, lightningFlashEnabled, cloudFlashEnabled)
    }

    private fun drawClouds(clouds: List<Cloud>, lightnings: List<Lightning>, lightningFlashEnabled: Boolean, cloudFlashEnabled: Boolean) {
        if (clouds.isEmpty()) return

        GLES30.glUseProgram(cloudProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(cloudProgram, "uMVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(cloudProgram, "uOpacity")
        val textureHandle = GLES30.glGetUniformLocation(cloudProgram, "uTexture")
        val flashIntensityHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashIntensity")
        val flashColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uFlashColor")
        val cloudColorHandle = GLES30.glGetUniformLocation(cloudProgram, "uCloudColor")
        GLES30.glUniform3f(cloudColorHandle, 0.7f, 0.7f, 0.7f)

        var maxIntensity = 0f
        var resolvedColor = floatArrayOf(0f, 0f, 0f)
        val activeLightnings = lightnings.filter { lightning ->
            lightning.isActive && (
                (!lightning.isInternalOnly && lightningFlashEnabled) ||
                (lightning.isInternalOnly && cloudFlashEnabled)
            )
        }
        for (lightning in activeLightnings) {
            if (lightning.intensity > maxIntensity) {
                maxIntensity = lightning.intensity
            }
            val color = getLightningColor(lightning.selectedColorIndex)
            resolvedColor[0] += color[0] * lightning.intensity
            resolvedColor[1] += color[1] * lightning.intensity
            resolvedColor[2] += color[2] * lightning.intensity
        }

        val avgColor = if (activeLightnings.isNotEmpty()) {
            floatArrayOf(
                (resolvedColor[0] / activeLightnings.size).coerceIn(0f, 1f),
                (resolvedColor[1] / activeLightnings.size).coerceIn(0f, 1f),
                (resolvedColor[2] / activeLightnings.size).coerceIn(0f, 1f)
            )
        } else {
            floatArrayOf(1.0f, 1.0f, 1.0f)
        }

        GLES30.glUniform1f(flashIntensityHandle, maxIntensity)
        GLES30.glUniform3fv(flashColorHandle, 1, avgColor, 0)

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

        for (cloud in clouds.sortedBy { it.z }) {
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cloud.positionX, cloud.positionY, 0f)
            // Stretches clouds landscape-wise (width = scale * 2.4, height = scale)
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

    private fun drawRain(rainDrops: List<RainDrop>, colorIndex: Int) {
        if (rainDrops.isEmpty()) return

        GLES30.glUseProgram(rainProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(rainProgram, "uMVPMatrix")
        val colorHandle = GLES30.glGetUniformLocation(rainProgram, "uRainColor")
        
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        val color = getRainColor(colorIndex)
        GLES30.glUniform4fv(colorHandle, 1, color, 0)

        // 1. Sort the rain drops by depth (z) ascending for layers/depth ordering
        val sortedDrops = rainDrops.sortedBy { it.z }

        // 2. Construct interleaved coordinate buffer (X, Y, Depth, U, V) for all rain drop quads
        // Each vertex has 5 floats. 6 vertices (2 triangles) per drop.
        val vertexData = FloatArray(sortedDrops.size * 30)
        var idx = 0
        for (drop in sortedDrops) {
            val zDepth = drop.z
            // Varying width with depth, ensuring a minimum width for far drops
            val halfW = (0.006f + 0.014f * zDepth) / 2f
            
            val x1 = drop.positionX
            val y1 = drop.positionY
            val x2 = drop.positionX + drop.dirX * drop.length
            val y2 = drop.positionY + drop.dirY * drop.length
            
            // Perpendicular offset vector: perp = (-dirY, dirX)
            val offsetX = -drop.dirY * halfW
            val offsetY = drop.dirX * halfW
            
            // Triangle 1: Tail Left, Tail Right, Head Left
            // Vertex 1: Tail Left
            vertexData[idx++] = x1 - offsetX
            vertexData[idx++] = y1 - offsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.0f
            vertexData[idx++] = 0.0f
            
            // Vertex 2: Tail Right
            vertexData[idx++] = x1 + offsetX
            vertexData[idx++] = y1 + offsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 1.0f
            vertexData[idx++] = 0.0f
            
            // Vertex 3: Head Left
            vertexData[idx++] = x2 - offsetX
            vertexData[idx++] = y2 - offsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.0f
            vertexData[idx++] = 1.0f
            
            // Triangle 2: Head Left, Tail Right, Head Right
            // Vertex 4: Head Left
            vertexData[idx++] = x2 - offsetX
            vertexData[idx++] = y2 - offsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 0.0f
            vertexData[idx++] = 1.0f
            
            // Vertex 5: Tail Right
            vertexData[idx++] = x1 + offsetX
            vertexData[idx++] = y1 + offsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 1.0f
            vertexData[idx++] = 0.0f
            
            // Vertex 6: Head Right
            vertexData[idx++] = x2 + offsetX
            vertexData[idx++] = y2 + offsetY
            vertexData[idx++] = zDepth
            vertexData[idx++] = 1.0f
            vertexData[idx++] = 1.0f
        }

        val rainBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexData)
                position(0)
            }

        // Bind positions attribute (location 0, stride 20 bytes: 5 floats)
        rainBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 20, rainBuffer)
        GLES30.glEnableVertexAttribArray(0)

        // Bind depth attribute (location 1, stride 20 bytes: 5 floats)
        rainBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, 20, rainBuffer)
        GLES30.glEnableVertexAttribArray(1)

        // Bind texCoord attribute (location 2, stride 20 bytes: 5 floats)
        rainBuffer.position(3)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 20, rainBuffer)
        GLES30.glEnableVertexAttribArray(2)

        // Draw all drops as quads using a single draw call (GL_TRIANGLES)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, sortedDrops.size * 6)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    private fun getRainColor(index: Int): FloatArray {
        return when (index) {
            0 -> floatArrayOf(0.7f, 0.8f, 1.0f, 0.7f) // Azul Claro
            1 -> floatArrayOf(1.0f, 1.0f, 1.0f, 0.7f) // Blanco
            2 -> floatArrayOf(1.0f, 0.2f, 0.2f, 0.7f) // Rojo
            3 -> floatArrayOf(0.2f, 1.0f, 0.2f, 0.7f) // Verde
            4 -> floatArrayOf(1.0f, 0.9f, 0.0f, 0.7f) // Amarillo
            5 -> floatArrayOf(0.7f, 0.3f, 1.0f, 0.7f) // Morado
            else -> floatArrayOf(0.7f, 0.8f, 1.0f, 0.7f)
        }
    }

    private fun drawLightning(lightnings: List<Lightning>, flashEnabled: Boolean) {
        val activeLightnings = lightnings.filter { it.isActive && !it.isInternalOnly }
        if (activeLightnings.isEmpty()) return

        GLES30.glUseProgram(lightningProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(lightningProgram, "uMVPMatrix")
        val flashIntensityHandle = GLES30.glGetUniformLocation(lightningProgram, "uFlashIntensity")
        val isTexturedHandle = GLES30.glGetUniformLocation(lightningProgram, "uIsTextured")
        val textureHandle = GLES30.glGetUniformLocation(lightningProgram, "uTexture")
        val colorHandle = GLES30.glGetUniformLocation(lightningProgram, "uLightningColor")
        val growthProgressHandle = GLES30.glGetUniformLocation(lightningProgram, "uGrowthProgress")

        // 1. Draw a single combined full screen overlay flash
        if (flashEnabled) {
            var maxIntensity = 0f
            var resolvedColor = floatArrayOf(0f, 0f, 0f, 0f)
            for (lightning in activeLightnings) {
                if (lightning.intensity > maxIntensity) {
                    maxIntensity = lightning.intensity
                }
                val color = getLightningColor(lightning.selectedColorIndex)
                resolvedColor[0] += color[0] * lightning.intensity
                resolvedColor[1] += color[1] * lightning.intensity
                resolvedColor[2] += color[2] * lightning.intensity
            }

            GLES30.glUniform1i(isTexturedHandle, 0)
            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, identityMatrix, 0)
            GLES30.glUniform1f(flashIntensityHandle, maxIntensity * 0.20f)
            GLES30.glUniform1f(growthProgressHandle, 1.0f)

            val avgColor = floatArrayOf(
                (resolvedColor[0] / activeLightnings.size).coerceIn(0f, 1f),
                (resolvedColor[1] / activeLightnings.size).coerceIn(0f, 1f),
                (resolvedColor[2] / activeLightnings.size).coerceIn(0f, 1f),
                1.0f
            )
            GLES30.glUniform4fv(colorHandle, 1, avgColor, 0)

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

        for (lightning in activeLightnings) {
            if (lightningTextures.isNotEmpty() && lightning.selectedTextureIndex < lightningTextures.size) {
                GLES30.glUniform1f(flashIntensityHandle, lightning.intensity)
                val color = getLightningColor(lightning.selectedColorIndex)
                GLES30.glUniform4fv(colorHandle, 1, color, 0)
                GLES30.glUniform1f(growthProgressHandle, lightning.growthProgress)

                val modelMatrix = FloatArray(16)
                Matrix.setIdentityM(modelMatrix, 0)

                // Apply world translation
                Matrix.translateM(modelMatrix, 0, lightning.positionX, lightning.positionY, 0f)

                if (lightning.rotationAngle != 0f) {
                    val halfHeight = lightning.scaleY * 0.5f
                    Matrix.translateM(modelMatrix, 0, 0f, halfHeight, 0f)
                    Matrix.rotateM(modelMatrix, 0, lightning.rotationAngle, 0f, 0f, 1f)
                    Matrix.translateM(modelMatrix, 0, 0f, -halfHeight, 0f)
                }

                // Apply scale
                Matrix.scaleM(modelMatrix, 0, lightning.scaleX, lightning.scaleY, 1.0f)

                val modelViewProjection = FloatArray(16)
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

    private fun getLightningColor(index: Int): FloatArray {
        return when (index) {
            0 -> floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // Blanco
            1 -> floatArrayOf(0.4f, 0.6f, 1.0f, 1.0f) // Azul
            2 -> floatArrayOf(1.0f, 0.9f, 0.2f, 1.0f) // Amarillo
            3 -> floatArrayOf(1.0f, 0.2f, 0.2f, 1.0f) // Rojo
            4 -> floatArrayOf(0.2f, 1.0f, 0.2f, 1.0f) // Verde
            5 -> floatArrayOf(0.8f, 0.3f, 1.0f, 1.0f) // Morado
            else -> floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        }
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
            inScaled = false // Disable pre-scaling to preserve pixel integrity
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
        val file = java.io.File(context.filesDir, "custom_background.png")
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

    private fun drawBackground(backgroundIndex: Int, lightnings: List<Lightning>, flashEnabled: Boolean) {
        if (backgroundIndex <= 0 || backgroundIndex > backgroundTextures.size) return
        val texIndex = backgroundIndex - 1
        val textureId = backgroundTextures[texIndex]
        if (textureId == 0) return

        GLES30.glUseProgram(backgroundProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(backgroundProgram, "uMVPMatrix")
        val flashIntensityHandle = GLES30.glGetUniformLocation(backgroundProgram, "uFlashIntensity")
        val flashColorHandle = GLES30.glGetUniformLocation(backgroundProgram, "uFlashColor")
        val textureHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTexture")
        val timeHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTime")
        val aspectHandle = GLES30.glGetUniformLocation(backgroundProgram, "uAspectRatio")
        val dropletsEnabledHandle = GLES30.glGetUniformLocation(backgroundProgram, "uScreenDropletsEnabled")
        val dropletsSizeHandle = GLES30.glGetUniformLocation(backgroundProgram, "uScreenDropletsSize")
        val rainColorHandle = GLES30.glGetUniformLocation(backgroundProgram, "uRainColor")

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

        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1.0f)

        val modelViewProjection = FloatArray(16)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)

        var maxIntensity = 0f
        var resolvedColor = floatArrayOf(0f, 0f, 0f)
        val activeLightnings = if (flashEnabled) lightnings.filter { it.isActive && !it.isInternalOnly } else emptyList()
        for (lightning in activeLightnings) {
            if (lightning.intensity > maxIntensity) {
                maxIntensity = lightning.intensity
            }
            val color = getLightningColor(lightning.selectedColorIndex)
            resolvedColor[0] += color[0] * lightning.intensity
            resolvedColor[1] += color[1] * lightning.intensity
            resolvedColor[2] += color[2] * lightning.intensity
        }

        val avgColor = if (activeLightnings.isNotEmpty()) {
            floatArrayOf(
                (resolvedColor[0] / activeLightnings.size).coerceIn(0f, 1f),
                (resolvedColor[1] / activeLightnings.size).coerceIn(0f, 1f),
                (resolvedColor[2] / activeLightnings.size).coerceIn(0f, 1f)
            )
        } else {
            floatArrayOf(1.0f, 1.0f, 1.0f)
        }

        GLES30.glUniform1f(flashIntensityHandle, maxIntensity)
        GLES30.glUniform3fv(flashColorHandle, 1, avgColor, 0)
        GLES30.glUniform1f(timeHandle, elapsedTime)
        GLES30.glUniform1f(aspectHandle, screenAspect)
        GLES30.glUniform1f(dropletsEnabledHandle, if (sceneManager.isScreenDropletsEnabled()) 1.0f else 0.0f)
        val sizeFactor = sceneManager.getScreenDropletsSize() / 100f
        GLES30.glUniform1f(dropletsSizeHandle, sizeFactor)
        val rainColor = getRainColor(sceneManager.getRainColorIndex())
        GLES30.glUniform3f(rainColorHandle, rainColor[0], rainColor[1], rainColor[2])

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }
}
