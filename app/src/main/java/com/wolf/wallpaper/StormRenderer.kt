package com.wolf.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class StormRenderer(private val context: Context) {

    private var cloudProgram = 0
    private var rainProgram = 0
    private var lightningProgram = 0

    // Texture IDs
    private val cloudTextures = mutableListOf<Int>()
    private var rainTexture = 0
    private val lightningTextures = mutableListOf<Int>()
    
    // MVP Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)

    // Buffers for geometric drawing
    private lateinit var fullscreenQuadBuffer: FloatBuffer
    private lateinit var cloudQuadBuffer: FloatBuffer
    private lateinit var lightningQuadBuffer: FloatBuffer

    init {
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(identityMatrix, 0)
    }

    fun onSurfaceCreated() {
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
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        // Setup orthographic camera matrix (RF-006: Keeps proportions in rotation/scale)
        Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
    }

    fun drawFrame(sceneManager: SceneManager) {
        // Clear background with deep dark storm color
        var clearR = 0.04f
        var clearG = 0.04f
        var clearB = 0.06f

        // Apply lightning flash peak color directly to background
        var maxIntensity = 0f
        var resolvedColor = floatArrayOf(0f, 0f, 0f, 0f)
        var activeCount = 0

        for (lightning in sceneManager.lightnings) {
            if (lightning.isActive) {
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

        GLES30.glClearColor(clearR, clearG, clearB, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Render layered elements in order: Lluvia -> Rayos -> Nubes
        drawRain(sceneManager.getRainDrops(), sceneManager.getRainColorIndex())
        drawLightning(sceneManager.lightnings)
        drawClouds(sceneManager.getClouds())
    }

    private fun drawClouds(clouds: List<Cloud>) {
        if (clouds.isEmpty()) return

        GLES30.glUseProgram(cloudProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(cloudProgram, "uMVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(cloudProgram, "uOpacity")
        val textureHandle = GLES30.glGetUniformLocation(cloudProgram, "uTexture")

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

        for (cloud in clouds) {
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

        // Construct coordinate buffer for all rain drop lines
        // Each raindrop is drawn as a line (start point to end point) to achieve motion blur
        val vertexData = FloatArray(rainDrops.size * 4)
        var idx = 0
        for (drop in rainDrops) {
            vertexData[idx++] = drop.positionX
            vertexData[idx++] = drop.positionY
            
            // Draw diagonal line along the drop's direction vector
            vertexData[idx++] = drop.positionX + drop.dirX * drop.length
            vertexData[idx++] = drop.positionY + drop.dirY * drop.length
        }

        val rainBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexData)
                position(0)
            }

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, rainBuffer)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glLineWidth(2.1f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, rainDrops.size * 2)

        GLES30.glDisableVertexAttribArray(0)
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

    private fun drawLightning(lightnings: List<Lightning>) {
        val activeLightnings = lightnings.filter { it.isActive }
        if (activeLightnings.isEmpty()) return

        GLES30.glUseProgram(lightningProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(lightningProgram, "uMVPMatrix")
        val flashIntensityHandle = GLES30.glGetUniformLocation(lightningProgram, "uFlashIntensity")
        val isTexturedHandle = GLES30.glGetUniformLocation(lightningProgram, "uIsTextured")
        val textureHandle = GLES30.glGetUniformLocation(lightningProgram, "uTexture")
        val colorHandle = GLES30.glGetUniformLocation(lightningProgram, "uLightningColor")
        val growthProgressHandle = GLES30.glGetUniformLocation(lightningProgram, "uGrowthProgress")

        // 1. Draw a single combined full screen overlay flash
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
}
