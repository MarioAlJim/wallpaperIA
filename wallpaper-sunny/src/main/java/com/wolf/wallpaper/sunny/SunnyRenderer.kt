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

    // Handles for sunny.frag
    private var timeHandle = -1
    private var aspectHandle = -1
    private var sunSizeHandle = -1
    private var sunSpeedHandle = -1
    private var themeHandle = -1
    private var sunPosHandle = -1

    // Handles for sunny_background.frag
    private var bgThemeHandle = -1
    private var bgSunPosHandle = -1
    private var bgAspectHandle = -1
    private var bgIsCustomHandle = -1
    
    private var time = 0f
    private var aspectRatio = 1.0f

    // Sun movement state
    private var sunX = 0.35f
    private var sunY = 0.45f
    private var sunPathX = 0f

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
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
        
        Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        
        // Adjust and align cloud positions for new aspect ratio
        for (cloud in clouds) {
            cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio)
        }
    }

    override fun onUpdate(deltaTime: Float) {
        // 1. Sun translation along the parabola
        val direction = configProvider.getSunPathDirection()
        if (direction == 0) { // Left-to-Right
            val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
            sunPathX += deltaTime * moveSpeed
            if (sunPathX > 1.3f) {
                sunPathX = -1.3f
            }
            sunX = sunPathX
            sunY = 0.5f - 0.7f * (sunX * sunX)
        } else if (direction == 1) { // Right-to-Left
            val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
            sunPathX -= deltaTime * moveSpeed
            if (sunPathX < -1.3f) {
                sunPathX = 1.3f
            }
            sunX = sunPathX
            sunY = 0.5f - 0.7f * (sunX * sunX)
        } else { // Stationary
            sunX = 0.35f
            sunY = 0.45f
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
                cloud.reset(0f, aspectRatio)
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
    }

    override fun onDrawFrame() {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // 1. Draw procedural sky and sun
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
        
        GLES30.glUniform1i(themeHandle, configProvider.getSunnyTheme())
        GLES30.glUniform2f(sunPosHandle, sunX, sunY)

        fullscreenQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)

        // 2. Draw active clouds (behind foreground landscapes)
        drawClouds()

        // 3. Draw foreground layout (keyed out white sky)
        val bgIndex = configProvider.getSunnyBackgroundIndex()
        drawBackground(bgIndex)
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
            else -> Triple(0.98f, 0.92f, 0.96f) // Anochecer (Whiter lavender / pinkish magenta)
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

        for (cloud in clouds.sortedBy { it.z }) {
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cloud.positionX, cloud.positionY, 0f)
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

    private fun drawBackground(backgroundIndex: Int) {
        if (backgroundIndex <= 0 || backgroundIndex > 7) return
        val texIndex = backgroundIndex - 1
        val textureId = backgroundTextures[texIndex]
        if (textureId == 0) return

        GLES30.glUseProgram(backgroundProgram)
        val mvpMatrixHandle = GLES30.glGetUniformLocation(backgroundProgram, "uMVPMatrix")
        val textureHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTexture")

        // Pass theme, sun position, aspect ratio, and custom image flag to the shader
        GLES30.glUniform1i(bgThemeHandle, configProvider.getSunnyTheme())
        GLES30.glUniform2f(bgSunPosHandle, sunX, sunY)
        GLES30.glUniform1f(bgAspectHandle, aspectRatio)
        GLES30.glUniform1i(bgIsCustomHandle, if (backgroundIndex == 7) 1 else 0)

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

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun adjustClouds(density: Int) {
        // Map 0-100 density to custom cloud count: 0, 2, 5, 10, 13, 15
        val targetCount = when (density) {
            0 -> 0
            25 -> 2
            50 -> 5
            75 -> 10
            90 -> 13
            100 -> 15
            else -> (density / 100f * 15).toInt()
        }.coerceIn(0, 15)

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
                cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio)
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

    override fun onTouchEvent(x: Float, y: Float) {
        // Interactive sun positioning or ray burst effects on touch coordinates could go here
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
