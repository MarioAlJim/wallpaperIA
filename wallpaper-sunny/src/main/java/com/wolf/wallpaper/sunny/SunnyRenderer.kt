package com.wolf.wallpaper.sunny

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.wolf.wallpaper.core.ConfigProvider
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.GLRenderUtils
import com.wolf.wallpaper.core.Cloud
import com.wolf.wallpaper.core.Moon
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
    private var moonLensFlareProgram = 0
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

    // Handles for the new subtle effects added to sunny.frag (cloud shafts / rainbow / lens dirt)
    private var cloudOcclusionHandle = -1
    private var rainbowOpacityHandle = -1
    private var lensDirtOpacityHandle = -1
    private var skyBgActiveHandle = -1
    private var skyBgTranslationHandle = -1
    private var skyBgScaleHandle = -1
    private var skyBgTextureHandle = -1

    // Handles for particle.vert/particle.frag (instanced: per-instance position/scale/
    // color/opacity travel as vertex attributes, not per-draw uniforms; uProjectionMatrix
    // is the only remaining uniform, shared across the whole instanced batch)
    private var pProjMatrixHandle = -1

    // Handles for firefly.vert(particle.vert)/firefly.frag (instanced, same layout as particles)
    private var fireflyProgram = 0
    private var fProjMatrixHandle = -1

    // Instanced-draw geometry for particles/fireflies: 4-vertex GL_TRIANGLE_STRIP quad
    // (locations 0/1, divisor 0) plus per-instance attributes packed as
    // [x, y, scale, r, g, b, opacity] (locations 2-5, divisor 1) uploaded once per frame.
    private val instanceFloatsPerEntry = 7
    private val maxParticleInstances = 512
    private val maxFireflyInstances = 64
    private lateinit var particleInstanceBuffer: FloatBuffer
    private lateinit var fireflyInstanceBuffer: FloatBuffer

    // Instanced-draw geometry for clouds: 4-vertex GL_TRIANGLE_STRIP quad
    // (locations 0/1, divisor 0) plus per-instance attributes packed as
    // [posX, posY, scale, opacity, variation] (locations 2-5, divisor 1) uploaded once per frame.
    private val cloudFloatsPerEntry = 5
    private val maxCloudInstances = 128
    private lateinit var cloudInstanceBuffer: FloatBuffer

    // Handles for sunny_background.frag
    private var bgThemeHandle = -1
    private var bgSunPosHandle = -1
    private var bgAspectHandle = -1
    private var bgIsCustomHandle = -1
    private var bgSkyTopHandle = -1
    private var bgSkyBottomHandle = -1
    private var bgNightIntensityHandle = -1
    private var bgMoonPhaseMultiplierHandle = -1
    private var bgMvpMatrixHandle = -1
    private var bgTextureHandleCached = -1
    private var bgCloudShadowXHandle = -1
    private var bgCloudShadowWidthHandle = -1
    private var bgCloudShadowOpacityHandle = -1
    private var bgTimeHandle = -1 // uTime, used by sunny_background.frag's heat-haze effect (Noon theme only)

    // Handle for sunny.frag's uNightIntensity (day sky night-fade blend, distinct from
    // the background program's own bgNightIntensityHandle above)
    private var dayNightIntensityHandle = -1

    // Precomputed noise texture used by sunny.frag's nebula fbm approximation
    // (dedicated texture unit: GL_TEXTURE1, so it never clashes with the cloud/
    // background/moon programs which all bind their own texture at GL_TEXTURE0).
    private var noiseTexture = 0
    private var noiseTexHandle = -1

    // Cloud handles (cloudProgram is shared by drawNightClouds and drawClouds; both
    // uniform sets are identical since it's the same shader program)
    private var cloudMvpMatrixHandle = -1
    private var cloudOpacityHandleCached = -1
    private var cloudFlashIntensityHandleCached = -1
    private var cloudFlashColorHandleCached = -1
    private var cloudCloudColorHandleCached = -1
    private var cloudVariationHandle = -1
    private var cloudMoonPosHandle = -1
    private var cloudCloudPosHandle = -1
    private var cloudCloudScaleHandleCached = -1
    private var cloudSilverLiningIntensityHandle = -1

    // Handles for lens_flare.frag
    private var lfSunPosHandle = -1
    private var lfAspectHandle = -1
    private var lfSwipeOffsetHandle = -1
    private var lfIntensityHandle = -1
    private var lfChromaticAberrationHandle = -1

    // Handles for moon_lens_flare.frag
    private var mlfMoonPosHandle = -1
    private var mlfAspectHandle = -1
    private var mlfSwipeOffsetHandle = -1
    private var mlfIntensityHandle = -1
    private var mlfChromaticAberrationHandle = -1

    // moon handles
    private var moonMVPHandle = -1
    private var moonPhaseHandle = -1
    private var moonColorHandle = -1
    private var moonIntensityHandle = -1
    private var moonHaloIntensityHandle = -1
    private var moonRingFadeHandle = -1

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
    private var moonRingFadeValue = 0f
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
        val b: Float,
        // Simulated downward acceleration applied each frame (units/sec^2). Defaults to 0 so
        // the existing touch-burst confetti (which never sets it) is completely unaffected;
        // only the shooting-star trail burst opts in (see spawnShootingStarTrailBurst).
        val gravity: Float = 0f
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

    // Shooting Star state and handles
    private class ShootingStar(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var length: Float,
        var opacity: Float,
        var life: Float, // duration in seconds
        var age: Float, // current age in seconds
        val r: Float,
        val g: Float,
        val b: Float
    )
    private var shootingStar: ShootingStar? = null
    private var shootingStarProgram = 0
    private var ssTailHandle = -1
    private var ssHeadHandle = -1
    private var ssOpacityHandle = -1
    private var ssAspectHandle = -1
    private var ssColorHandle = -1

    // Bird Shader Handles
    private var birdProgram = 0
    private var birdMvpMatrixHandle = -1
    private var birdTimeHandle = -1
    private var birdFlapSpeedHandle = -1
    private var birdFlapOffsetHandle = -1
    private var birdOpacityHandle = -1

    // Bird state
    private class Bird(
        val isLeader: Boolean,
        val offsetLeaderX: Float,
        val offsetLeaderY: Float
    ) {
        var x = 0f
        var y = 0f
        var speed = 0f
        var scale = 0f
        var flapSpeed = 0f
        var flapOffset = 0f
        val isLone = (offsetLeaderX == 999f)

        fun reset(aspect: Float) {
            if (isLeader) {
                x = -aspect - 0.4f - kotlin.random.Random.nextFloat() * 0.4f
                y = kotlin.random.Random.nextFloat() * 0.4f + 0.3f
                speed = kotlin.random.Random.nextFloat() * 0.05f + 0.12f
                scale = (kotlin.random.Random.nextFloat() * 0.008f + 0.012f) * 2.0f
                flapSpeed = speed * 45f
                flapOffset = kotlin.random.Random.nextFloat() * 10f
            } else if (isLone) {
                x = -aspect - 0.4f - kotlin.random.Random.nextFloat() * 1.5f
                y = kotlin.random.Random.nextFloat() * 0.5f + 0.2f
                speed = kotlin.random.Random.nextFloat() * 0.06f + 0.10f
                scale = (kotlin.random.Random.nextFloat() * 0.008f + 0.010f) * 2.0f
                flapSpeed = speed * 45f
                flapOffset = kotlin.random.Random.nextFloat() * 10f
            }
        }

        fun update(deltaTime: Float, aspect: Float, touchSpeedBoost: Float, leader: Bird?) {
            if (isLeader || isLone) {
                val currentSpeed = speed * (1.0f + touchSpeedBoost)
                x += currentSpeed * deltaTime
                if (isLeader && x > aspect + 0.6f) {
                    reset(aspect)
                } else if (isLone && x > aspect + 0.3f) {
                    reset(aspect)
                }
            } else {
                // Follower
                if (leader != null) {
                    x = leader.x + offsetLeaderX
                    y = leader.y + offsetLeaderY
                    scale = leader.scale
                    flapSpeed = leader.flapSpeed
                    flapOffset = leader.flapOffset + offsetLeaderX * 2.0f
                }
            }
        }
    }
    private val birds = mutableListOf<Bird>()
    private var birdSpeedBoost = 0f

    // Firefly state
    private class Firefly(
        var x: Float,
        var y: Float,
        var baseX: Float,
        var baseY: Float,
        var noiseSeedX: Float,
        var noiseSeedY: Float,
        var speed: Float,
        var size: Float,
        var alpha: Float,
        var targetAlpha: Float,
        val r: Float,
        val g: Float,
        val b: Float
    )
    private val fireflies = mutableListOf<Firefly>()

    // Rare subtle-rainbow state (see updateRainbow): a simple 4-state fade in/hold/fade out
    // timer, following the same "random per-frame probability" spawn pattern already used
    // for shooting stars above.
    private var rainbowOpacity = 0f
    private var rainbowState = RAINBOW_STATE_IDLE
    private var rainbowTimer = 0f
    private var rainbowHoldDuration = 0f

    // Touch-wind state (see onOffsetsChanged/applyTouchWindImpulse): tracks when the last
    // horizontal-offset sample arrived so a swipe's velocity can be derived from consecutive
    // samples, independently of the render loop's own frame timing.
    private var lastSwipeOffsetSampleTimeNanos = 0L

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

    // Chaotic path movement state: a Lissajous-like drift with randomized phase/frequency,
    // wandering erratically instead of jumping edge-to-edge like Random (see Moon.kt for the
    // equivalent moon implementation).
    private var chaoticTime = 0f
    private var chaoticSeedX = kotlin.random.Random.nextFloat() * 6.2832f
    private var chaoticSeedY = kotlin.random.Random.nextFloat() * 6.2832f
    private var chaoticFreqX = 0.13f + kotlin.random.Random.nextFloat() * 0.17f
    private var chaoticFreqY = 0.09f + kotlin.random.Random.nextFloat() * 0.15f

    // Night / Combined mode state
    private val moon = Moon()
    private val nightClouds = mutableListOf<Cloud>()
    private var cycleProgress = 0f
    private var prevMoonDirection = -1
    private var isCombinedNight = false
    private var combinedDayIntensity = 1f
    private var combinedNightIntensity = 0f

    // Combined-mode moon phase auto-advance (see ConfigProvider.isCombinedMoonPhaseAutoAdvanceEnabled):
    // when enabled, the phase is no longer read from config every frame - it's tracked here and
    // bumped by one each time a full day+night cycle completes (see completeCombinedNightCycle).
    // -1 means "not yet initialized since the toggle was last off", so it re-syncs from the saved
    // config value the next time it's needed instead of carrying over a stale in-memory phase.
    private var autoAdvanceMoonPhase = -1

    // Tracks the last density passed to adjustClouds/adjustNightClouds so they only
    // re-run their cloud-count bookkeeping (filtering, spawning/fading) when the
    // configured density actually changes, instead of every frame regardless.
    private var lastDayCloudDensity = -1
    private var lastNightCloudDensity = -1

    // Clouds
    private val clouds = mutableListOf<Cloud>()

    // Depth-sort caches: rebuilt only when a cloud is added/removed/reset (z changes),
    // instead of re-sorting the whole list every single frame in the draw loop.
    private var cloudsDirty = true
    private var nightCloudsDirty = true
    private val sortedClouds = mutableListOf<Cloud>()
    private val sortedNightClouds = mutableListOf<Cloud>()

    // Backgrounds
    private val backgroundTextures = IntArray(8)
    private val backgroundAspectRatios = FloatArray(8) { 1.0f }

    // MVP Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    // Scratch matrices reused across per-object draw calls (clouds, moon, particles, fireflies, background)
    private val scratchModelMatrix = FloatArray(16)
    private val scratchMVPMatrix = FloatArray(16)

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
        dayNightIntensityHandle = GLES30.glGetUniformLocation(program, "uNightIntensity")
        noiseTexHandle = GLES30.glGetUniformLocation(program, "uNoiseTex")
        cloudOcclusionHandle = GLES30.glGetUniformLocation(program, "uCloudOcclusionNearSun")
        rainbowOpacityHandle = GLES30.glGetUniformLocation(program, "uRainbowOpacity")
        lensDirtOpacityHandle = GLES30.glGetUniformLocation(program, "uLensDirtOpacity")
        skyBgActiveHandle = GLES30.glGetUniformLocation(program, "uBgActive")
        skyBgTranslationHandle = GLES30.glGetUniformLocation(program, "uBgTranslation")
        skyBgScaleHandle = GLES30.glGetUniformLocation(program, "uBgScale")
        skyBgTextureHandle = GLES30.glGetUniformLocation(program, "uBackgroundTex")
        noiseTexture = createNoiseTexture(128)

        // Compile and link lens flare shader
        try {
            val lfVert = readAssetFile(context, "shaders/sunny.vert")
            val lfFrag = readAssetFile(context, "shaders/lens_flare.frag")
            lensFlareProgram = createProgram(lfVert, lfFrag)

            lfSunPosHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uSunPos")
            lfAspectHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uAspectRatio")
            lfSwipeOffsetHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uSwipeOffset")
            lfIntensityHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uIntensity")
            lfChromaticAberrationHandle = GLES30.glGetUniformLocation(lensFlareProgram, "uChromaticAberration")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Compile and link moon lens flare shader
        try {
            val mlfVert = readAssetFile(context, "shaders/sunny.vert")
            val mlfFrag = readAssetFile(context, "shaders/moon_lens_flare.frag")
            moonLensFlareProgram = createProgram(mlfVert, mlfFrag)

            mlfMoonPosHandle = GLES30.glGetUniformLocation(moonLensFlareProgram, "uMoonPos")
            mlfAspectHandle = GLES30.glGetUniformLocation(moonLensFlareProgram, "uAspectRatio")
            mlfSwipeOffsetHandle = GLES30.glGetUniformLocation(moonLensFlareProgram, "uSwipeOffset")
            mlfIntensityHandle = GLES30.glGetUniformLocation(moonLensFlareProgram, "uIntensity")
            mlfChromaticAberrationHandle = GLES30.glGetUniformLocation(moonLensFlareProgram, "uChromaticAberration")
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

            pProjMatrixHandle = GLES30.glGetUniformLocation(particleProgram, "uProjectionMatrix")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Compile and link firefly shader
        try {
            val fVert = readAssetFile(context, "shaders/particle.vert")
            val fFrag = readAssetFile(context, "shaders/firefly.frag")
            fireflyProgram = createProgram(fVert, fFrag)

            fProjMatrixHandle = GLES30.glGetUniformLocation(fireflyProgram, "uProjectionMatrix")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Instanced per-instance attribute buffers for particles/fireflies
        particleInstanceBuffer = ByteBuffer.allocateDirect(maxParticleInstances * instanceFloatsPerEntry * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        fireflyInstanceBuffer = ByteBuffer.allocateDirect(maxFireflyInstances * instanceFloatsPerEntry * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        cloudInstanceBuffer = ByteBuffer.allocateDirect(maxCloudInstances * cloudFloatsPerEntry * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        // Compile and link cloud shader
        val cloudVert = readAssetFile(context, "shaders/procedural_cloud.vert")
        val cloudFrag = readAssetFile(context, "shaders/procedural_cloud.frag")
        cloudProgram = createProgram(cloudVert, cloudFrag)

        cloudMvpMatrixHandle = GLES30.glGetUniformLocation(cloudProgram, "uMVPMatrix")
        cloudFlashIntensityHandleCached = GLES30.glGetUniformLocation(cloudProgram, "uFlashIntensity")
        cloudFlashColorHandleCached = GLES30.glGetUniformLocation(cloudProgram, "uFlashColor")
        cloudCloudColorHandleCached = GLES30.glGetUniformLocation(cloudProgram, "uCloudColor")
        cloudMoonPosHandle = GLES30.glGetUniformLocation(cloudProgram, "uMoonPos")
        cloudSilverLiningIntensityHandle = GLES30.glGetUniformLocation(cloudProgram, "uSilverLiningIntensity")

        // Compile and link bird shader
        try {
            val birdVert = readAssetFile(context, "shaders/bird.vert")
            val birdFrag = readAssetFile(context, "shaders/bird.frag")
            birdProgram = createProgram(birdVert, birdFrag)

            birdMvpMatrixHandle = GLES30.glGetUniformLocation(birdProgram, "uMVPMatrix")
            birdTimeHandle = GLES30.glGetUniformLocation(birdProgram, "uTime")
            birdFlapSpeedHandle = GLES30.glGetUniformLocation(birdProgram, "uFlapSpeed")
            birdFlapOffsetHandle = GLES30.glGetUniformLocation(birdProgram, "uFlapOffset")
            birdOpacityHandle = GLES30.glGetUniformLocation(birdProgram, "uOpacity")
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        bgMvpMatrixHandle = GLES30.glGetUniformLocation(backgroundProgram, "uMVPMatrix")
        bgTextureHandleCached = GLES30.glGetUniformLocation(backgroundProgram, "uTexture")
        bgTimeHandle = GLES30.glGetUniformLocation(backgroundProgram, "uTime")
        bgCloudShadowXHandle = GLES30.glGetUniformLocation(backgroundProgram, "uCloudShadowX")
        bgCloudShadowWidthHandle = GLES30.glGetUniformLocation(backgroundProgram, "uCloudShadowWidth")
        bgCloudShadowOpacityHandle = GLES30.glGetUniformLocation(backgroundProgram, "uCloudShadowOpacity")



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
            moonRingFadeHandle = GLES30.glGetUniformLocation(moonProgram, "uRingFade")
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

        // Compile and link shooting star shader
        try {
            val ssVert = readAssetFile(context, "shaders/shooting_star.vert")
            val ssFrag = readAssetFile(context, "shaders/shooting_star.frag")
            shootingStarProgram = createProgram(ssVert, ssFrag)
            ssTailHandle = GLES30.glGetUniformLocation(shootingStarProgram, "uTail")
            ssHeadHandle = GLES30.glGetUniformLocation(shootingStarProgram, "uHead")
            ssOpacityHandle = GLES30.glGetUniformLocation(shootingStarProgram, "uOpacity")
            ssAspectHandle = GLES30.glGetUniformLocation(shootingStarProgram, "uAspect")
            ssColorHandle = GLES30.glGetUniformLocation(shootingStarProgram, "uColor")
        } catch (e: Exception) {
            e.printStackTrace()
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
            cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio)
        }
        cloudsDirty = true
        initBirds()
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

        if (timeMode == 3) {
            // Clock Real-time Mode: Sun and Moon position and Day/Night intensities based on phone's clock
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            val second = calendar.get(java.util.Calendar.SECOND)
            val timeAsFloat = hour + minute / 60f + second / 3600f // 0.0 to 24.0

            // Day phase is between 6:00 (6.0) and 18:00 (18.0)
            if (timeAsFloat in 6.0f..18.0f) {
                isCombinedNight = false
                val p = (timeAsFloat - 6.0f) / 12.0f // ranges 0.0 to 1.0
                sunPathX = -1.3f + p * 2.6f
                moonPathX = 1.3f // keep moon offscreen
            } else {
                isCombinedNight = true
                val relativeTime = if (timeAsFloat >= 18.0f) timeAsFloat - 18.0f else timeAsFloat + 6.0f
                val p = relativeTime / 12.0f // ranges 0.0 to 1.0
                moonPathX = -1.3f + p * 2.6f
                sunPathX = 1.3f // keep sun offscreen
            }

            // Sunrise transition: 5.5 to 7.0 (5:30 AM to 7:00 AM)
            // Sunset transition: 17.0 to 18.5 (5:00 PM to 6:30 PM)
            combinedDayIntensity = when {
                timeAsFloat < 5.5f -> 0.0f
                timeAsFloat in 5.5f..7.0f -> (timeAsFloat - 5.5f) / 1.5f
                timeAsFloat in 7.0f..17.0f -> 1.0f
                timeAsFloat in 17.0f..18.5f -> 1.0f - (timeAsFloat - 17.0f) / 1.5f
                else -> 0.0f
            }
            combinedNightIntensity = 1.0f - combinedDayIntensity

            sunX = sunPathX
            sunY = 1.0f - 1.3f * (sunX * sunX)
            moon.positionX = moonPathX
            moon.positionY = 0.85f - 1.15f * (moon.positionX * moon.positionX)
        } else if (timeMode == 2) {
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
                        moon.hasHaloRing = kotlin.random.Random.nextFloat() < 0.5f
                    }
                } else {
                    sunPathX -= deltaTime * moveSpeed
                    if (sunPathX < -1.3f) {
                        isCombinedNight = true
                        moonPathX = if (moonL2R) -1.3f else 1.3f
                        moon.hasHaloRing = kotlin.random.Random.nextFloat() < 0.5f
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
                        advanceCombinedMoonPhaseIfEnabled()
                    }
                } else {
                    moonPathX -= deltaTime * moveSpeed
                    if (moonPathX < -1.3f) {
                        isCombinedNight = false
                        sunPathX = if (sunL2R) -1.3f else 1.3f
                        advanceCombinedMoonPhaseIfEnabled()
                    }
                }
                
                // Night phase sky is fully dark night
                combinedDayIntensity = 0.0f
                combinedNightIntensity = 1.0f

                // Keep inactive Sun off-screen at its start position
                sunPathX = if (sunL2R) -1.3f else 1.3f
            }

            sunX = sunPathX
            sunY = 1.0f - 1.3f * (sunX * sunX)
            moon.positionX = moonPathX
            moon.positionY = 0.85f - 1.15f * (moon.positionX * moon.positionX)
        } else {
            if (direction == 0) { // Left-to-Right
                val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
                sunPathX += deltaTime * moveSpeed
                if (sunPathX > 1.3f) {
                    sunPathX = -1.3f
                }
                sunX = sunPathX
                sunY = 1.0f - 1.3f * (sunX * sunX)
            } else if (direction == 1) { // Right-to-Left
                val moveSpeed = 0.01f + (configProvider.getSunMoveSpeed() / 100f) * 0.49f
                sunPathX -= deltaTime * moveSpeed
                if (sunPathX < -1.3f) {
                    sunPathX = 1.3f
                }
                sunX = sunPathX
                sunY = 1.0f - 1.3f * (sunX * sunX)
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
            } else if (direction == 4) { // Chaotic
                val cSpeed = 0.2f + (configProvider.getSunMoveSpeed() / 100f) * 0.8f
                chaoticTime += deltaTime * cSpeed
                val rangeX = aspectRatio * 0.9f
                sunX = kotlin.math.sin(chaoticTime * chaoticFreqX * 6.2832f + chaoticSeedX) * rangeX
                sunY = 0.45f + kotlin.math.sin(chaoticTime * chaoticFreqY * 6.2832f + chaoticSeedY) * 0.4f
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

        val windDirection = configProvider.getWindDirection()
        val windIntensity = configProvider.getWindIntensity()
        val windSpeed = when (windDirection) {
            0 -> -(windIntensity / 100f) * 0.15f
            2 -> (windIntensity / 100f) * 0.15f
            else -> 0f
        }
        val dynamicsSpeed = configProvider.getCloudDynamicsSpeed() / 100f

        // 3. Clouds simulation updates (Day/Combined)
        val dayCloudsActive = (timeMode == 0 || timeMode == 2 || timeMode == 3)
        val dayDensity = if (dayCloudsActive) configProvider.getCloudDensity() else 0
        if (dayDensity != lastDayCloudDensity) {
            adjustClouds(dayDensity)
            lastDayCloudDensity = dayDensity
        }

        for (cloud in clouds) {
            cloud.update(deltaTime, windSpeed, dynamicsSpeed)
            val halfWidth = cloud.scale * 1.2f
            val maxBound = aspectRatio + halfWidth
            if (cloud.positionX > maxBound || cloud.positionX < -maxBound) {
                cloud.reset(0f, aspectRatio)
                cloudsDirty = true
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
        if (clouds.removeAll { it.isFadingOut && it.opacity <= 0f }) {
            cloudsDirty = true
        }

        // Night / Combined mode: update moon and night clouds
        if (timeMode == 1 || timeMode == 2 || timeMode == 3) {
            moon.phase = if (timeMode == 2 || timeMode == 3) effectiveCombinedMoonPhase() else configProvider.getMoonPhase()
            moon.moveSpeed = configProvider.getMoonMoveSpeed()
            moon.stationaryPosition = configProvider.getMoonStationaryPosition()
            moon.customX = configProvider.getMoonCustomX()
            moon.customY = configProvider.getMoonCustomY()
            
            if (timeMode == 2 || timeMode == 3) {
                // Moon position is already calculated in the first block of onUpdate
            } else {
                val newDir = configProvider.getMoonPathDirection()
                if (newDir != prevMoonDirection) {
                    moon.resetPath(newDir, aspectRatio)
                    prevMoonDirection = newDir
                }
                moon.update(deltaTime, aspectRatio)
            }
        }

        // Night clouds simulation updates (Night/Combined)
        val nightCloudsActive = (timeMode == 1 || timeMode == 2 || timeMode == 3)
        val nightDensity = if (nightCloudsActive) configProvider.getCloudDensity() else 0
        if (nightDensity != lastNightCloudDensity) {
            adjustNightClouds(nightDensity)
            lastNightCloudDensity = nightDensity
        }

        if (nightCloudsActive || nightClouds.isNotEmpty()) {
            for (nc in nightClouds) {
                nc.update(deltaTime, windSpeed, dynamicsSpeed)
                val halfW = nc.scale * 1.2f
                val maxB = aspectRatio + halfW
                if (nc.positionX > maxB || nc.positionX < -maxB) {
                    nc.reset(0f, aspectRatio)
                    nightCloudsDirty = true
                    val newHalfW = nc.scale * 1.2f
                    val windThreshold = 0.1f
                    val driftInfluence = (1.0f - (kotlin.math.abs(windSpeed) / windThreshold)).coerceIn(0f, 1f)
                    val netSpeed = windSpeed + (nc.driftSpeed * driftInfluence)
                    if (netSpeed >= 0f) {
                        nc.positionX = -aspectRatio - newHalfW
                    } else {
                        nc.positionX = aspectRatio + newHalfW
                    }
                }
            }
            if (nightClouds.removeAll { it.isFadingOut && it.opacity <= 0f }) {
                nightCloudsDirty = true
            }
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
                // Simulated gravity: only shooting-star trail particles set p.gravity != 0
                // (touch-burst confetti keeps its existing drag-only behavior unchanged).
                if (p.gravity != 0f) {
                    p.vy -= p.gravity * deltaTime
                }
                p.x += p.vx * deltaTime
                p.y += p.vy * deltaTime
                p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                p.vx *= (1.0f - deltaTime * 0.5f)
                p.vy *= (1.0f - deltaTime * 0.5f)
            }
        }

        // Stars simulation updates
        val isCombined = configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3
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

        // Shooting Star simulation update
        val currentStarIntensity = when (timeMode) {
            1 -> 1.0f
            2 -> combinedNightIntensity
            else -> 0.0f
        }

        if (currentStarIntensity < 0.12f || !configProvider.isShootingStarsEnabled()) {
            shootingStar = null
        } else {
            val activeStar = shootingStar
            if (activeStar != null) {
                activeStar.age += deltaTime
                if (activeStar.age >= activeStar.life) {
                    shootingStar = null
                } else {
                    activeStar.x += activeStar.vx * deltaTime
                    activeStar.y += activeStar.vy * deltaTime
                    activeStar.opacity = 1.0f - (activeStar.age / activeStar.life)
                }
            } else {
                val density = configProvider.getShootingStarsDensity()
                // Whole curve scaled by 1.5x (was 0.2..1.6/sec) so the max density ("Lluvia de
                // estrellas", density=100) spawns 50% more shooting stars/sec, in both Night and
                // Combined mode (this is the single shared spawn point for both).
                val spawnRate = 0.3f + (density / 100f) * 2.1f
                if (kotlin.random.Random.nextFloat() < spawnRate * deltaTime) {
                    val rand = kotlin.random.Random
                    val startX = rand.nextFloat() * (aspectRatio * 1.6f) - (aspectRatio * 0.8f)
                    val startY = rand.nextFloat() * (0.95f - 0.5f) + 0.5f
                    val vxVal = (rand.nextFloat() * -0.9f - 0.9f) * aspectRatio
                    val vyVal = rand.nextFloat() * -0.45f - 0.45f
                    val lengthVal = rand.nextFloat() * 0.12f + 0.12f
                    val lifeVal = rand.nextFloat() * 0.35f + 0.4f
                    
                    val isCombined = configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3
                    val starColorSetting = if (isCombined) configProvider.getCombinedStarColorIndex() else configProvider.getStarColorIndex()
                    val finalColorIndex = if (starColorSetting == 5) rand.nextInt(5) else starColorSetting
                    val (rVal, gVal, bVal) = getStarColorRGB(finalColorIndex)

                    shootingStar = ShootingStar(
                        x = startX,
                        y = startY,
                        vx = vxVal,
                        vy = vyVal,
                        length = lengthVal,
                        opacity = 1.0f,
                        life = lifeVal,
                        age = 0f,
                        r = rVal,
                        g = gVal,
                        b = bVal
                    )
                    spawnShootingStarTrailBurst(startX, startY, vxVal, vyVal, rVal, gVal, bVal)
                }
            }
        }

        // Fireflies simulation update
        val firefliesActive = configProvider.isFirefliesEnabled() && (timeMode == 1 || (timeMode == 2 && combinedNightIntensity > 0.01f))
        updateFireflies(deltaTime, firefliesActive)

        // Rare subtle-rainbow simulation update (day skies only, see updateRainbow)
        updateRainbow(deltaTime)

        // Birds simulation update
        birdSpeedBoost = (birdSpeedBoost - deltaTime * 1.5f).coerceAtLeast(0f)
        val leader = birds.firstOrNull { it.isLeader }
        birds.forEach { it.update(deltaTime, aspectRatio, birdSpeedBoost, leader) }
    }

    /**
     * The moon phase (0-7) to actually render/use for lighting in Combined mode: the configured
     * [ConfigProvider.getCombinedMoonPhase] normally, or the auto-advancing in-memory phase when
     * [ConfigProvider.isCombinedMoonPhaseAutoAdvanceEnabled] is on (see [autoAdvanceMoonPhase]).
     * Re-syncs from the saved config value the first time it's called after the toggle turns on
     * (or on first use), so flipping the switch never starts from a stale/arbitrary phase.
     */
    private fun effectiveCombinedMoonPhase(): Int {
        if (!configProvider.isCombinedMoonPhaseAutoAdvanceEnabled()) {
            autoAdvanceMoonPhase = -1
            return configProvider.getCombinedMoonPhase()
        }
        if (autoAdvanceMoonPhase < 0) {
            autoAdvanceMoonPhase = configProvider.getCombinedMoonPhase()
        }
        return autoAdvanceMoonPhase
    }

    /** Bumps the auto-advance phase by one (wrapping 0-7) once a full day+night cycle completes, if enabled. */
    private fun advanceCombinedMoonPhaseIfEnabled() {
        if (!configProvider.isCombinedMoonPhaseAutoAdvanceEnabled()) return
        autoAdvanceMoonPhase = (effectiveCombinedMoonPhase() + 1) % 8
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

    /**
     * Aggregates how much day-cloud "density" currently sits near the sun into a single 0..1
     * scalar (see kCloudShaftInfluenceRadius/kCloudShaftScaleFactor), used to widen/soften the
     * analytic god rays in sunny.frag into an approximate "cloud shafts" look. Each cloud's
     * own influence radius grows with its scale so bigger/closer clouds register from farther
     * away, matching their actual on-screen footprint; contributions from multiple clouds
     * stack (clamped to 1) so several partial clouds near the sun can add up to full occlusion.
     */
    private fun computeCloudOcclusionNearSun(): Float {
        if (clouds.isEmpty()) return 0f
        var occlusion = 0f
        for (cloud in clouds) {
            if (cloud.opacity <= 0f) continue
            val dx = cloud.positionX - sunX
            val dy = cloud.positionY - sunY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            val influenceRadius = kCloudShaftInfluenceRadius + cloud.scale * kCloudShaftScaleFactor
            val proximity = smoothstep(influenceRadius, 0f, dist)
            occlusion += proximity * cloud.opacity
        }
        return occlusion.coerceIn(0f, 1f)
    }

    /**
     * Rare subtle-rainbow timer: a simple idle -> fade-in -> hold -> fade-out state machine.
     * Eligibility (day mode or Combined with high day intensity, and never the Anochecer/Dusk
     * theme) is re-checked every frame; if it stops being eligible mid-display, the rainbow is
     * pushed straight into its fade-out phase rather than popping off instantly.
     */
    private fun updateRainbow(deltaTime: Float) {
        val timeMode = configProvider.getTimeMode()
        val theme = configProvider.getSunnyTheme()
        val dayIntensity = when (timeMode) {
            0 -> 1f
            2, 3 -> combinedDayIntensity
            else -> 0f
        }
        val eligible = theme != 2 && dayIntensity > kRainbowMinDayIntensity

        if (!eligible && rainbowState != RAINBOW_STATE_IDLE && rainbowState != RAINBOW_STATE_FADE_OUT) {
            rainbowState = RAINBOW_STATE_FADE_OUT
            rainbowTimer = kRainbowFadeOutDuration * (1f - rainbowOpacity)
        }

        when (rainbowState) {
            RAINBOW_STATE_IDLE -> {
                if (eligible && kotlin.random.Random.nextFloat() < kRainbowSpawnRatePerSecond * deltaTime) {
                    rainbowState = RAINBOW_STATE_FADE_IN
                    rainbowTimer = 0f
                    rainbowHoldDuration = kRainbowHoldMin + kotlin.random.Random.nextFloat() * kRainbowHoldRange
                }
            }
            RAINBOW_STATE_FADE_IN -> {
                rainbowTimer += deltaTime
                rainbowOpacity = (rainbowTimer / kRainbowFadeInDuration).coerceIn(0f, 1f)
                if (rainbowTimer >= kRainbowFadeInDuration) {
                    rainbowState = RAINBOW_STATE_HOLD
                    rainbowTimer = 0f
                }
            }
            RAINBOW_STATE_HOLD -> {
                rainbowTimer += deltaTime
                rainbowOpacity = 1f
                if (rainbowTimer >= rainbowHoldDuration) {
                    rainbowState = RAINBOW_STATE_FADE_OUT
                    rainbowTimer = 0f
                }
            }
            RAINBOW_STATE_FADE_OUT -> {
                rainbowTimer += deltaTime
                rainbowOpacity = (1f - rainbowTimer / kRainbowFadeOutDuration).coerceIn(0f, 1f)
                if (rainbowTimer >= kRainbowFadeOutDuration || rainbowOpacity <= 0f) {
                    rainbowState = RAINBOW_STATE_IDLE
                    rainbowTimer = 0f
                    rainbowOpacity = 0f
                }
            }
        }
    }

    override fun onDrawFrame() {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        when (configProvider.getTimeMode()) {
            1 -> drawNightScene(1f)
            2, 3 -> drawCombinedScene()
            else -> drawDayScene()
        }
    }

    private fun drawDaySky(intensity: Float, nightIntensity: Float = 0f) {
        // 1. Cielo procedural y sol
        GLES30.glUseProgram(program)
        GLES30.glUniform1f(timeHandle, time)
        GLES30.glUniform1f(aspectHandle, aspectRatio)

        val sizeConfig = if (configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3) {
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

        // Pass night intensity uniform to sky shader
        GLES30.glUniform1f(dayNightIntensityHandle, nightIntensity)

        // Bind the precomputed noise texture used by the nebula fbm approximation
        // (only sampled by the shader's night branch, but binding is cheap either way)
        if (noiseTexture != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTexture)
            GLES30.glUniform1i(noiseTexHandle, 1)
        }

        // Cloud shafts: aggregate day-cloud occlusion near the sun into a single scalar (see
        // computeCloudOcclusionNearSun) so the shader can spread/soften the existing god rays.
        GLES30.glUniform1f(cloudOcclusionHandle, computeCloudOcclusionNearSun())

        // Subtle rare rainbow (see updateRainbow): CPU-driven fade in/hold/fade out, already
        // forced to 0 outside eligible day conditions by updateRainbow itself.
        GLES30.glUniform1f(rainbowOpacityHandle, rainbowOpacity)

        // Lens dirt: reuse the same "is the sun visible near screen center" gating already
        // used for the lens flare (isSunnyLensFlareEnabled + lowSunFactor + pathFadeFactor),
        // plus a screen-center-proximity falloff so the smudge only shows while the sun sits
        // near the middle of the screen.
        val lensDirtBase = if (configProvider.isSunnyLensFlareEnabled()) lowSunFactor * pathFadeFactor * intensity else 0f
        val centerDist = kotlin.math.sqrt((sunX + gyroX) * (sunX + gyroX) + (sunY + gyroY) * (sunY + gyroY))
        val centerProximity = (1f - smoothstep(0f, kLensDirtCenterFalloff, centerDist)).coerceIn(0f, 1f)
        GLES30.glUniform1f(lensDirtOpacityHandle, (lensDirtBase * centerProximity).coerceIn(0f, 1f))

        // Pass background occlusion uniforms to sky shader for volumetric God Rays occlusion
        val bgIndex = configProvider.getSunnyBackgroundIndex()
        val transform = getBgTransform(bgIndex)
        GLES30.glUniform1i(skyBgActiveHandle, if (bgIndex > 0 && bgIndex < 8) 1 else 0)
        GLES30.glUniform2f(skyBgTranslationHandle, transform.translationX, transform.translationY)
        GLES30.glUniform2f(skyBgScaleHandle, transform.scaleX, transform.scaleY)
        
        if (bgIndex > 0 && bgIndex < 8) {
            val textureId = backgroundTextures[bgIndex - 1]
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            GLES30.glUniform1i(skyBgTextureHandle, 2)
        }

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

    private fun initBirds() {
        birds.clear()
        
        // 1. Formation V (5 birds)
        // Leader
        birds.add(Bird(isLeader = true, offsetLeaderX = 0f, offsetLeaderY = 0f))
        // Left wing followers
        birds.add(Bird(isLeader = false, offsetLeaderX = -0.12f, offsetLeaderY = 0.07f))
        birds.add(Bird(isLeader = false, offsetLeaderX = -0.24f, offsetLeaderY = 0.14f))
        // Right wing followers
        birds.add(Bird(isLeader = false, offsetLeaderX = -0.12f, offsetLeaderY = -0.07f))
        birds.add(Bird(isLeader = false, offsetLeaderX = -0.24f, offsetLeaderY = -0.14f))
        
        // 2. Lone random birds (3 birds)
        birds.add(Bird(isLeader = false, offsetLeaderX = 999f, offsetLeaderY = 999f))
        birds.add(Bird(isLeader = false, offsetLeaderX = 999f, offsetLeaderY = 999f))
        birds.add(Bird(isLeader = false, offsetLeaderX = 999f, offsetLeaderY = 999f))
        
        // Reset all birds to their initial offscreen positions
        birds.forEach { it.reset(aspectRatio) }
    }

    private fun drawBirds(dayIntensity: Float, nightIntensity: Float) {
        if (birds.isEmpty() || birdProgram == 0 || dayIntensity <= 0.01f) return

        GLES30.glUseProgram(birdProgram)

        // Bind attributes
        cloudQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)
        
        cloudQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        // Bind shared uniforms
        GLES30.glUniform1f(birdTimeHandle, time)
        GLES30.glUniform1f(birdOpacityHandle, dayIntensity)

        // Gyro offsets (consistent with clouds)
        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.10f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.10f else 0f

        for (bird in birds) {
            // Determine active coordinates on screen
            val activeX: Float
            val activeY: Float
            if (bird.isLeader || bird.isLone) {
                activeX = bird.x
                activeY = bird.y
            } else {
                val leader = birds.firstOrNull { it.isLeader }
                if (leader != null) {
                    activeX = leader.x + bird.offsetLeaderX
                    activeY = leader.y + bird.offsetLeaderY
                } else {
                    continue
                }
            }

            // Build model matrix
            val modelMatrix = scratchModelMatrix
            Matrix.setIdentityM(modelMatrix, 0)
            // Apply translation with gyro offset
            Matrix.translateM(modelMatrix, 0, activeX + gyroX, activeY + gyroY, 0f)
            // Scale the quad (keep it 1:1 in aspect ratio)
            Matrix.scaleM(modelMatrix, 0, bird.scale, bird.scale, 1.0f)

            // Multiply with projection matrix
            val modelViewProjection = scratchMVPMatrix
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

            GLES30.glUniformMatrix4fv(birdMvpMatrixHandle, 1, false, modelViewProjection, 0)
            GLES30.glUniform1f(birdFlapSpeedHandle, bird.flapSpeed * (1.0f + birdSpeedBoost * 0.8f))
            GLES30.glUniform1f(birdFlapOffsetHandle, bird.flapOffset)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun drawDayScene() {
        drawDaySky(1f, 0f)

        // Draw birds (before background so they fly behind mountains/hills)
        drawBirds(1f, 0f)

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
        // Draw the night sky background with nebulae and horizon haze
        drawDaySky(0f, intensity)
        
        drawStars(intensity)
        drawMoon(intensity)
        drawNightClouds(intensity)

        // Draw birds (they will fade out to 0 opacity at night, but structure remains sound)
        drawBirds(0f, intensity)

        val bgIndex = configProvider.getSunnyBackgroundIndex()
        drawBackground(bgIndex, intensity)
        drawFireflies(intensity) // Draw fireflies in front of background
        drawMoonLensFlare(intensity)
    }

    private fun drawCombinedScene() {
        val dayIntensity = combinedDayIntensity
        val nightIntensity = combinedNightIntensity

        // 1. Cielo (Día y Noche combinados en un solo renderizado)
        drawDaySky(dayIntensity, nightIntensity)
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

        // Draw birds (visible during day, fade out at night)
        drawBirds(dayIntensity, nightIntensity)

        // 4. Paisaje Silueta (Montañas/Fondo - Dibujado al frente)
        val bgIndex = configProvider.getSunnyBackgroundIndex()
        if (bgIndex > 0) {
            drawBackground(bgIndex, nightIntensity)
        }

        // Draw fireflies in front of silhouettes if night mode is active in combined scene
        if (nightIntensity > 0.01f) {
            drawFireflies(nightIntensity)
        }

        // 5. Efectos Especiales de Día
        if (dayIntensity > 0.01f) {
            drawParticles(dayIntensity)
            drawLensFlare(dayIntensity)
        }
        if (nightIntensity > 0.01f) {
            drawMoonLensFlare(nightIntensity)
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

        // Draw shooting star if active
        val star = shootingStar
        if (star != null && shootingStarProgram != 0) {
            GLES30.glUseProgram(shootingStarProgram)
            
            // Enable blending for transparency
            val blendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
            if (!blendEnabled) GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

            // Pass uniforms
            GLES30.glUniform1f(ssOpacityHandle, star.opacity * intensity)
            GLES30.glUniform1f(ssAspectHandle, aspectRatio)
            GLES30.glUniform3f(ssColorHandle, star.r, star.g, star.b)

            val speed = kotlin.math.sqrt(star.vx * star.vx + star.vy * star.vy)
            val ux = if (speed > 0.0001f) star.vx / speed else -0.8f
            val uy = if (speed > 0.0001f) star.vy / speed else -0.6f
            val tailX = star.x - ux * star.length
            val tailY = star.y - uy * star.length

            GLES30.glUniform2f(ssHeadHandle, star.x, star.y)
            GLES30.glUniform2f(ssTailHandle, tailX, tailY)

            // Bind fullscreen quad coordinates
            fullscreenQuadBuffer.position(0)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
            GLES30.glEnableVertexAttribArray(0)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

            GLES30.glDisableVertexAttribArray(0)

            if (!blendEnabled) GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private fun drawMoon(intensity: Float) {
        moonRingFadeValue = 0f
        if (moonProgram == 0) return
        GLES30.glUseProgram(moonProgram)

        val sizeConfig = if (configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3) {
            configProvider.getCombinedMoonSize()
        } else {
            50
        }
        val mappedMoonSize = if (configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3) {
            0.06f + (sizeConfig / 100f) * 0.34f
        } else {
            0.24f
        }
        val modelMatrix = scratchModelMatrix
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, moon.positionX, moon.positionY, 0f)
        Matrix.scaleM(modelMatrix, 0, mappedMoonSize * 3.0f, mappedMoonSize * 3.0f, 1.0f)

        val mvp = scratchMVPMatrix
        Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(moonMVPHandle, 1, false, mvp, 0)
        GLES30.glUniform1i(moonPhaseHandle, moon.phase)
        GLES30.glUniform3f(moonColorHandle, 1.0f, 0.97f, 0.88f)
        val horizonFade = smoothstep(-0.4f, -0.1f, moon.positionY)
        val baseIntensity = if (configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3) intensity * moonFadeFactor else intensity
        val finalIntensity = baseIntensity * horizonFade
        GLES30.glUniform1f(moonIntensityHandle, finalIntensity)

        val haloHorizonFade = smoothstep(-0.4f, 0.8f, moon.positionY)
        GLES30.glUniform1f(moonHaloIntensityHandle, haloHorizonFade)

        val moonDir = if (configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3) {
            val combinedDir = configProvider.getCombinedPathDirection()
            if (combinedDir == 0 || combinedDir == 3) 0 else 1
        } else {
            moon.pathDirection
        }

        val t = if (moonDir == 0) { // Left-to-Right
            (moon.positionX + 1.3f) / 2.6f
        } else if (moonDir == 1) { // Right-to-Left
            (1.3f - moon.positionX) / 2.6f
        } else {
            0.5f // Stationary / Random (fully visible)
        }

        val ringFade = when {
            !moon.hasHaloRing -> 0f
            t < 0.40f || t > 0.60f -> 0f
            t in 0.40f..0.43f -> (t - 0.40f) / 0.03f
            t in 0.57f..0.60f -> (0.60f - t) / 0.03f
            else -> 1.0f
        }
        moonRingFadeValue = ringFade
        GLES30.glUniform1f(moonRingFadeHandle, ringFade)

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

        val mvpMatrixHandle = cloudMvpMatrixHandle
        val flashIntensityHandle = cloudFlashIntensityHandleCached
        val flashColorHandle = cloudFlashColorHandleCached
        val cloudColorHandle = cloudCloudColorHandleCached
        val moonPosHandle = cloudMoonPosHandle
        val silverLiningIntensityHandle = cloudSilverLiningIntensityHandle

        // Night clouds color: slightly darker/cooler/blue-ish gray tint
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projectionMatrix, 0)
        GLES30.glUniform3f(cloudColorHandle, 0.35f, 0.42f, 0.55f)
        GLES30.glUniform1f(flashIntensityHandle, 0f)
        GLES30.glUniform3f(flashColorHandle, 1.0f, 1.0f, 1.0f)
        GLES30.glUniform2f(moonPosHandle, moon.positionX, moon.positionY)
        GLES30.glUniform1f(silverLiningIntensityHandle, intensity)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.10f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.10f else 0f

        if (nightCloudsDirty) {
            sortedNightClouds.clear()
            sortedNightClouds.addAll(nightClouds)
            sortedNightClouds.sortBy { it.z }
            nightCloudsDirty = false
        }

        val count = sortedNightClouds.size.coerceAtMost(maxCloudInstances)
        cloudInstanceBuffer.clear()
        for (i in 0 until count) {
            val cloud = sortedNightClouds[i]
            cloudInstanceBuffer.put(cloud.positionX + gyroX)
            cloudInstanceBuffer.put(cloud.positionY + gyroY)
            cloudInstanceBuffer.put(cloud.scale)
            cloudInstanceBuffer.put(cloud.opacity * intensity)
            cloudInstanceBuffer.put(cloud.shapeSeed)
        }
        cloudInstanceBuffer.position(0)

        drawInstancedClouds(cloudInstanceBuffer, count)
    }

    private fun adjustNightClouds(density: Int) {
        val targetCount = when (density) {
            25 -> 1
            50 -> 3
            75 -> 6
            100 -> 10
            else -> if (density <= 25) 1 else if (density <= 50) 3 else if (density <= 75) 6 else 10
        }

        val activeClouds = nightClouds.filter { !it.isFadingOut }

        if (activeClouds.size < targetCount) {
            var needed = targetCount - activeClouds.size
            val fading = nightClouds.filter { it.isFadingOut }
            for (nc in fading) {
                if (needed > 0) { nc.isFadingOut = false; needed-- }
            }
            while (needed > 0) {
                val id = if (nightClouds.isNotEmpty()) nightClouds.maxOf { it.id } + 1 else 0
                val nc = Cloud(id, 0f, 0f, 0f, 0f, 0f)
                nc.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio)
                nc.opacity = 0f
                nightClouds.add(nc)
                nightCloudsDirty = true
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
        if (clouds.isEmpty() || cloudProgram == 0) return

        GLES30.glUseProgram(cloudProgram)

        val mvpMatrixHandle = cloudMvpMatrixHandle
        val flashIntensityHandle = cloudFlashIntensityHandleCached
        val flashColorHandle = cloudFlashColorHandleCached
        val cloudColorHandle = cloudCloudColorHandleCached
        val moonPosHandle = cloudMoonPosHandle
        val silverLiningIntensityHandle = cloudSilverLiningIntensityHandle

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projectionMatrix, 0)
        GLES30.glUniform2f(moonPosHandle, 0f, 0f)
        GLES30.glUniform1f(silverLiningIntensityHandle, 0f) // Disabled during day

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

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.10f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.10f else 0f

        if (cloudsDirty) {
            sortedClouds.clear()
            sortedClouds.addAll(clouds)
            sortedClouds.sortBy { it.z }
            cloudsDirty = false
        }

        val count = sortedClouds.size.coerceAtMost(maxCloudInstances)
        cloudInstanceBuffer.clear()
        for (i in 0 until count) {
            val cloud = sortedClouds[i]
            cloudInstanceBuffer.put(cloud.positionX + gyroX)
            cloudInstanceBuffer.put(cloud.positionY + gyroY)
            cloudInstanceBuffer.put(cloud.scale)
            cloudInstanceBuffer.put(cloud.opacity * intensity)
            cloudInstanceBuffer.put(cloud.shapeSeed)
        }
        cloudInstanceBuffer.position(0)

        drawInstancedClouds(cloudInstanceBuffer, count)
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

        GLES30.glUniformMatrix4fv(pProjMatrixHandle, 1, false, projectionMatrix, 0)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.02f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.02f else 0f

        // Silently drop the tail past the buffer's capacity (only reachable under a
        // pathological burst of touch events); this keeps the instance buffer fixed-size.
        val count = particles.size.coerceAtMost(maxParticleInstances)
        particleInstanceBuffer.clear()
        for (i in 0 until count) {
            val p = particles[i]
            particleInstanceBuffer.put(p.x + gyroX)
            particleInstanceBuffer.put(p.y + gyroY)
            particleInstanceBuffer.put(p.size)
            particleInstanceBuffer.put(p.r)
            particleInstanceBuffer.put(p.g)
            particleInstanceBuffer.put(p.b)
            particleInstanceBuffer.put(p.alpha * intensity)
        }
        particleInstanceBuffer.position(0)

        drawInstancedQuads(particleInstanceBuffer, count)

        // Restore blend function to default to avoid breaking subsequent renders
        if (!blendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        } else {
            GLES30.glBlendFunc(prevSrcFunc[0], prevDstFunc[0])
        }
    }

    /**
     * Draws [count] instances of the shared unit quad (cloudQuadBuffer geometry) using
     * [instanceBuffer]'s per-instance [x, y, scale, r, g, b, opacity] attributes (locations
     * 2-5, divisor 1) in a single glDrawArraysInstanced call. Used by drawParticles and
     * drawFireflies, whose vertex/fragment shaders (particle.vert + particle.frag/firefly.frag)
     * share this exact attribute layout.
     */
    private fun drawInstancedQuads(instanceBuffer: FloatBuffer, count: Int) {
        if (count <= 0) return

        // Shared quad geometry (locations 0/1, divisor 0 -> same 4 vertices for every instance)
        cloudQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)
        cloudQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        val stride = instanceFloatsPerEntry * 4
        instanceBuffer.position(0)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribDivisor(2, 1)

        instanceBuffer.position(2)
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribDivisor(3, 1)

        instanceBuffer.position(3)
        GLES30.glVertexAttribPointer(4, 3, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribDivisor(4, 1)

        instanceBuffer.position(6)
        GLES30.glVertexAttribPointer(5, 1, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(5)
        GLES30.glVertexAttribDivisor(5, 1)

        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, count)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
        GLES30.glDisableVertexAttribArray(3)
        GLES30.glDisableVertexAttribArray(4)
        GLES30.glDisableVertexAttribArray(5)
        // Reset divisors so these locations behave as regular per-vertex attributes if
        // reused by a subsequent non-instanced draw call using the same slots.
        GLES30.glVertexAttribDivisor(2, 0)
        GLES30.glVertexAttribDivisor(3, 0)
        GLES30.glVertexAttribDivisor(4, 0)
        GLES30.glVertexAttribDivisor(5, 0)
    }

    private fun drawInstancedClouds(instanceBuffer: FloatBuffer, count: Int) {
        if (count <= 0) return

        // Shared quad geometry (locations 0/1, divisor 0 -> same 4 vertices for every instance)
        cloudQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)
        cloudQuadBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, cloudQuadBuffer)
        GLES30.glEnableVertexAttribArray(1)

        val stride = cloudFloatsPerEntry * 4

        // aInstancePos (location 2, size 2)
        instanceBuffer.position(0)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribDivisor(2, 1)

        // aInstanceScale (location 3, size 1)
        instanceBuffer.position(2)
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribDivisor(3, 1)

        // aInstanceOpacity (location 4, size 1)
        instanceBuffer.position(3)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribDivisor(4, 1)

        // aInstanceVariation (location 5, size 1)
        instanceBuffer.position(4)
        GLES30.glVertexAttribPointer(5, 1, GLES30.GL_FLOAT, false, stride, instanceBuffer)
        GLES30.glEnableVertexAttribArray(5)
        GLES30.glVertexAttribDivisor(5, 1)

        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, count)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
        GLES30.glDisableVertexAttribArray(3)
        GLES30.glDisableVertexAttribArray(4)
        GLES30.glDisableVertexAttribArray(5)

        // Reset divisors
        GLES30.glVertexAttribDivisor(2, 0)
        GLES30.glVertexAttribDivisor(3, 0)
        GLES30.glVertexAttribDivisor(4, 0)
        GLES30.glVertexAttribDivisor(5, 0)
    }

    private fun updateFireflies(deltaTime: Float, active: Boolean) {
        val targetCount = if (active) 35 else 0
        if (fireflies.size < targetCount) {
            val needed = targetCount - fireflies.size
            for (i in 0 until needed) {
                fireflies.add(createRandomFirefly())
            }
        }
        val iterator = fireflies.iterator()
        while (iterator.hasNext()) {
            val f = iterator.next()
            if (!active) {
                f.alpha -= deltaTime * 2.0f
                if (f.alpha <= 0f) {
                    iterator.remove()
                    continue
                }
            } else {
                if (f.alpha < f.targetAlpha) {
                    f.alpha = (f.alpha + deltaTime * 1.5f).coerceAtMost(f.targetAlpha)
                }
            }
            f.noiseSeedX += deltaTime * f.speed
            f.noiseSeedY += deltaTime * f.speed
            val dx = (kotlin.math.sin(f.noiseSeedX * 1.6f) * 0.12f + kotlin.math.cos(f.noiseSeedX * 3.3f) * 0.06f)
            val dy = (kotlin.math.cos(f.noiseSeedY * 1.1f) * 0.08f + kotlin.math.sin(f.noiseSeedY * 2.6f) * 0.04f)
            f.baseY += deltaTime * 0.04f
            f.baseX += deltaTime * 0.02f
            if (f.baseY > -0.2f) {
                f.baseY = -1.0f - kotlin.random.Random.nextFloat() * 0.1f
                f.baseX = kotlin.random.Random.nextFloat() * (aspectRatio * 2.2f) - (aspectRatio * 1.1f)
            }
            if (f.baseX > aspectRatio * 1.1f) {
                f.baseX = -aspectRatio * 1.1f
            } else if (f.baseX < -aspectRatio * 1.1f) {
                f.baseX = aspectRatio * 1.1f
            }
            f.x = f.baseX + dx
            f.y = f.baseY + dy
        }
    }

    private fun createRandomFirefly(): Firefly {
        val rand = kotlin.random.Random
        val startX = rand.nextFloat() * (aspectRatio * 2.2f) - (aspectRatio * 1.1f)
        val startY = rand.nextFloat() * 0.7f - 1.0f
        val seedX = rand.nextFloat() * 1000f
        val seedY = rand.nextFloat() * 1000f
        val speed = 0.5f + rand.nextFloat() * 1.0f
        val size = 0.012f + rand.nextFloat() * 0.012f
        val targetAlpha = 0.6f + rand.nextFloat() * 0.4f
        val r = 0.1f + rand.nextFloat() * 0.15f  // Low red for luminous green
        val g = 0.9f + rand.nextFloat() * 0.1f   // High green
        val b = 0.0f + rand.nextFloat() * 0.05f  // Very low blue
        return Firefly(
            x = startX, y = startY, baseX = startX, baseY = startY,
            noiseSeedX = seedX, noiseSeedY = seedY, speed = speed, size = size,
            alpha = 0f, targetAlpha = targetAlpha, r = r, g = g, b = b
        )
    }

    private fun drawFireflies(intensity: Float) {
        if (fireflies.isEmpty() || fireflyProgram == 0) return
        val blendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        val prevSrcFunc = IntArray(1)
        val prevDstFunc = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_BLEND_SRC_RGB, prevSrcFunc, 0)
        GLES30.glGetIntegerv(GLES30.GL_BLEND_DST_RGB, prevDstFunc, 0)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)

        GLES30.glUseProgram(fireflyProgram)
        GLES30.glUniformMatrix4fv(fProjMatrixHandle, 1, false, projectionMatrix, 0)

        val gyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.02f else 0f
        val gyroY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.02f else 0f

        val count = fireflies.size.coerceAtMost(maxFireflyInstances)
        fireflyInstanceBuffer.clear()
        for (i in 0 until count) {
            val f = fireflies[i]
            fireflyInstanceBuffer.put(f.x + gyroX)
            fireflyInstanceBuffer.put(f.y + gyroY)
            fireflyInstanceBuffer.put(f.size)
            fireflyInstanceBuffer.put(f.r)
            fireflyInstanceBuffer.put(f.g)
            fireflyInstanceBuffer.put(f.b)
            fireflyInstanceBuffer.put(f.alpha * intensity)
        }
        fireflyInstanceBuffer.position(0)

        drawInstancedQuads(fireflyInstanceBuffer, count)

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
        val mvpMatrixHandle = bgMvpMatrixHandle
        val textureHandle = bgTextureHandleCached

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
        GLES30.glUniform1f(bgTimeHandle, time) // drives sunny_background.frag's Noon-only heat-haze wobble
        GLES30.glUniform1f(bgNightIntensityHandle, nightIntensity)

        // Compute and pass top 4 cloud shadow parameters in screen NDC space
        val cloudGyroX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.10f else 0f
        val activeShadowList = if (nightIntensity > 0.5f) sortedNightClouds else sortedClouds
        val activeShadowClouds = activeShadowList.filter { it.opacity > 0.05f }
            .sortedByDescending { it.scale }
            .take(4)
        
        val shadowX = FloatArray(4)
        val shadowWidth = FloatArray(4)
        val shadowOpacity = FloatArray(4)
        
        for (i in 0 until 4) {
            if (i < activeShadowClouds.size) {
                val cloud = activeShadowClouds[i]
                shadowX[i] = (cloud.positionX + cloudGyroX) / aspectRatio
                shadowWidth[i] = (cloud.scale * 2.4f) / aspectRatio * 0.7f
                shadowOpacity[i] = cloud.opacity
            } else {
                shadowX[i] = -99f
                shadowWidth[i] = 0f
                shadowOpacity[i] = 0f
            }
        }
        
        GLES30.glUniform4fv(bgCloudShadowXHandle, 1, shadowX, 0)
        GLES30.glUniform4fv(bgCloudShadowWidthHandle, 1, shadowWidth, 0)
        GLES30.glUniform4fv(bgCloudShadowOpacityHandle, 1, shadowOpacity, 0)
        
        val currentPhase = if (configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3) {
            effectiveCombinedMoonPhase()
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

        val modelMatrix = scratchModelMatrix
        Matrix.setIdentityM(modelMatrix, 0)

        val transform = getBgTransform(backgroundIndex)
        Matrix.translateM(modelMatrix, 0, transform.translationX, transform.translationY, 0f)
        Matrix.scaleM(modelMatrix, 0, transform.scaleX, transform.scaleY, 1.0f)

        val modelViewProjection = scratchMVPMatrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private class BgTransform(
        val translationX: Float,
        val translationY: Float,
        val scaleX: Float,
        val scaleY: Float
    )

    private fun getBgTransform(bgIndex: Int): BgTransform {
        if (bgIndex <= 0 || bgIndex > 8) return BgTransform(0f, 0f, 1f, 1f)
        val texIndex = bgIndex - 1
        val screenAspect = aspectRatio
        val bgAspect = backgroundAspectRatios[texIndex]
        
        val scaleX: Float
        val scaleY: Float
        if (screenAspect > bgAspect) {
            scaleX = screenAspect
            scaleY = screenAspect / bgAspect
        } else {
            scaleX = bgAspect
            scaleY = 1.0f
        }
        
        val bgShiftX = if (configProvider.isSunnyGyroEnabled()) sensorTiltX * 0.05f else 0f
        val bgShiftY = if (configProvider.isSunnyGyroEnabled()) sensorTiltY * 0.05f else 0f
        
        val maxShiftX = (scaleX - screenAspect).coerceAtLeast(0f)
        val shiftX = -swipeOffset * maxShiftX * 0.5f
        
        return BgTransform(shiftX + bgShiftX, bgShiftY, scaleX, scaleY)
    }

    private fun adjustClouds(density: Int) {
        val targetCount = when (density) {
            25 -> 2
            50 -> 5
            75 -> 9
            100 -> 14
            else -> if (density <= 25) 2 else if (density <= 50) 5 else if (density <= 75) 9 else 14
        }

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
                val cloud = Cloud(cloudId, 0f, 0f, 0f, 0f, 0f)
                cloud.reset(kotlin.random.Random.nextFloat() * aspectRatio * 2 - aspectRatio, aspectRatio)
                cloud.opacity = 0f
                clouds.add(cloud)
                cloudsDirty = true
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
        val isCombined = configProvider.getTimeMode() == 2 || configProvider.getTimeMode() == 3
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
        birdSpeedBoost = 2.5f

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

    /**
     * Spawns a small burst of debris particles trailing a freshly-created shooting star,
     * reusing the same instanced particle pool/buffer as the touch-burst (drawParticles /
     * drawInstancedQuads), just with `gravity` set so they arc downward and fade out as they
     * fall, instead of the touch-burst's plain radial drag-only decay. The shared pool is
     * sized for 512 instances with ample headroom over the touch-burst's 35 + this burst's
     * handful, so no separate pool/buffer is needed.
     */
    private fun spawnShootingStarTrailBurst(
        startX: Float, startY: Float, starVx: Float, starVy: Float,
        r: Float, g: Float, b: Float
    ) {
        val roomLeft = (maxParticleInstances - particles.size).coerceAtLeast(0)
        val spawnCount = kShootingStarTrailParticleCount.coerceAtMost(roomLeft)
        if (spawnCount <= 0) return

        val rand = kotlin.random.Random
        val speed = kotlin.math.sqrt(starVx * starVx + starVy * starVy).coerceAtLeast(0.0001f)
        val dirX = starVx / speed
        val dirY = starVy / speed

        for (i in 0 until spawnCount) {
            // Distribute the burst slightly behind the star's head, along its path, with a
            // bit of lateral scatter so it reads as a debris trail rather than a single line.
            val along = rand.nextFloat() * kShootingStarTrailSpread
            val lateral = (rand.nextFloat() * 2f - 1f) * kShootingStarTrailLateralScatter
            val px = startX - dirX * along - dirY * lateral
            val py = startY - dirY * along + dirX * lateral

            val speedScatter = kShootingStarTrailSpeedScatterMin + rand.nextFloat() * kShootingStarTrailSpeedScatterRange
            val vx = starVx * speedScatter + (rand.nextFloat() * 2f - 1f) * kShootingStarTrailVelocityJitter
            val vy = starVy * speedScatter + (rand.nextFloat() * 2f - 1f) * kShootingStarTrailVelocityJitter

            val pSize = kShootingStarTrailMinSize + rand.nextFloat() * kShootingStarTrailSizeRange
            val maxLife = kShootingStarTrailMinLife + rand.nextFloat() * kShootingStarTrailLifeRange

            particles.add(
                SunnyParticle(
                    x = px, y = py, vx = vx, vy = vy,
                    size = pSize, alpha = 1.0f, life = maxLife, maxLife = maxLife,
                    r = r, g = g, b = b, gravity = kShootingStarTrailGravity
                )
            )
        }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float) {
        val clampedX = xOffset.coerceIn(0f, 1f)
        val newSwipeOffset = (clampedX - 0.5f) * 2.0f

        // Touch wind: this callback already receives a continuous stream of horizontal-offset
        // samples while the user drags (both from the in-wallpaper touch-drag emulation and
        // from launcher home-screen paging), so a swipe's velocity can be derived directly
        // from consecutive samples here without needing per-pointer ACTION_MOVE plumbing.
        // Fast enough deltas give visible clouds a temporary wind kick in the swipe direction.
        val nowNanos = System.nanoTime()
        if (lastSwipeOffsetSampleTimeNanos != 0L) {
            val dt = (nowNanos - lastSwipeOffsetSampleTimeNanos) / 1_000_000_000f
            if (dt in kWindSwipeMinDt..kWindSwipeMaxDt) {
                val velocity = (newSwipeOffset - swipeOffset) / dt
                if (kotlin.math.abs(velocity) > kWindSwipeMinVelocity) {
                    val impulse = (velocity * kWindSwipeImpulseScale).coerceIn(-kWindSwipeMaxImpulse, kWindSwipeMaxImpulse)
                    applyTouchWindImpulse(impulse)
                }
            }
        }
        lastSwipeOffsetSampleTimeNanos = nowNanos

        swipeOffset = newSwipeOffset
    }

    /** Gives every currently-visible cloud list (per active time mode) a temporary horizontal
     *  wind kick; the impulse itself decays back to 0 inside [Cloud.update]. */
    private fun applyTouchWindImpulse(impulse: Float) {
        val timeMode = configProvider.getTimeMode()
        if (timeMode == 0 || timeMode == 2) {
            for (cloud in clouds) cloud.applyWindImpulse(impulse)
        }
        if (timeMode == 1 || timeMode == 2) {
            for (cloud in nightClouds) cloud.applyWindImpulse(impulse)
        }
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float =
        SunnyMath.smoothstep(edge0, edge1, x)

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
        
        val aberrationEnabled = if (configProvider.isSunnyLensFlareChromaticEnabled()) 1 else 0
        GLES30.glUniform1i(lfChromaticAberrationHandle, aberrationEnabled)

        fullscreenQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
    }

    private fun drawMoonLensFlare(intensity: Float) {
        if (moonLensFlareProgram == 0) return
        val enabled = configProvider.isSunnyLensFlareEnabled()
        if (!enabled) return
        if (moon.phase != 4) return
        if (moonRingFadeValue <= 0.0f) return

        val finalIntensity = 0.6f * moonRingFadeValue * intensity

        if (finalIntensity <= 0.0f) return

        GLES30.glUseProgram(moonLensFlareProgram)
        GLES30.glUniform2f(mlfMoonPosHandle, moon.positionX, moon.positionY)
        GLES30.glUniform1f(mlfAspectHandle, aspectRatio)
        GLES30.glUniform1f(mlfSwipeOffsetHandle, swipeOffset)
        GLES30.glUniform1f(mlfIntensityHandle, finalIntensity)
        
        val aberrationEnabled = if (configProvider.isSunnyLensFlareChromaticEnabled()) 1 else 0
        GLES30.glUniform1i(mlfChromaticAberrationHandle, aberrationEnabled)

        fullscreenQuadBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, fullscreenQuadBuffer)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
    }

    private fun readAssetFile(context: Context, path: String): String =
        GLRenderUtils.readAssetFile(context, path)

    /**
     * Generates a small single-channel, tileable noise texture once at startup, used by
     * sunny.frag to approximate multi-octave fbm noise via a few bilinearly-filtered
     * texture samples instead of per-pixel hash/fract math. Each texel is an independent
     * random value; GL_LINEAR sampling + GL_REPEAT wrapping produces smooth, continuous
     * value-noise when sampled at frequencies below the texture's own resolution.
     */
    private fun createNoiseTexture(size: Int): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return 0

        val random = kotlin.random.Random(1337)
        val data = ByteArray(size * size)
        for (i in data.indices) {
            data[i] = random.nextInt(0, 256).toByte()
        }
        val buffer = ByteBuffer.allocateDirect(data.size)
            .order(ByteOrder.nativeOrder())
            .apply {
                put(data)
                position(0)
            }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R8, size, size, 0,
            GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, buffer
        )
        return textureIds[0]
    }

    private fun loadBackgroundTexture(context: Context, index: Int, assetPath: String) {
        val result = GLRenderUtils.loadBackgroundTextureFromAsset(context, assetPath) ?: return
        val (textureId, aspectRatio) = result
        backgroundAspectRatios[index] = aspectRatio
        backgroundTextures[index] = textureId
    }

    private fun loadCustomBackgroundTexture(context: Context, index: Int) {
        val file = java.io.File(context.filesDir, "custom_sunny_background.png")
        val result = GLRenderUtils.loadBackgroundTextureFromFile(file) ?: return
        val (textureId, aspectRatio) = result
        backgroundAspectRatios[index] = aspectRatio
        backgroundTextures[index] = textureId
    }

    private fun wrapPathX(x: Float): Float {
        var v = x
        while (v > 1.3f) v -= 2.6f
        while (v < -1.3f) v += 2.6f
        return v
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int =
        GLRenderUtils.createProgram(vertexCode, fragmentCode)

    companion object {
        // --- Rainbow state machine (effect: subtle rare rainbow) ---
        private const val RAINBOW_STATE_IDLE = 0
        private const val RAINBOW_STATE_FADE_IN = 1
        private const val RAINBOW_STATE_HOLD = 2
        private const val RAINBOW_STATE_FADE_OUT = 3

        // Average of one rainbow every ~15 minutes while conditions are eligible (rare, as
        // required). Uses the same "probability * deltaTime per frame" spawn pattern already
        // used for shooting stars, so no separate interval-timer bookkeeping is needed.
        private const val kRainbowSpawnRatePerSecond = 1f / 900f
        private const val kRainbowMinDayIntensity = 0.6f // required day-intensity floor in Combined mode
        private const val kRainbowFadeInDuration = 4f    // seconds
        private const val kRainbowFadeOutDuration = 6f   // seconds
        private const val kRainbowHoldMin = 20f          // minimum seconds held at full opacity
        private const val kRainbowHoldRange = 25f         // + up to this many extra random seconds (20-45s total)

        // --- Cloud shafts (effect: approximate cloud-occluded god rays) ---
        private const val kCloudShaftInfluenceRadius = 0.35f // base "near the sun" radius, in world units
        private const val kCloudShaftScaleFactor = 0.6f      // extra influence radius per unit of cloud.scale

        // --- Touch wind (effect: swipe gives clouds a temporary wind kick) ---
        private const val kWindSwipeMinVelocity = 1.2f   // minimum |d(swipeOffset)/dt| to count as a deliberate swipe
        private const val kWindSwipeImpulseScale = 0.12f // converts swipe velocity into a cloud wind-impulse magnitude
        private const val kWindSwipeMaxImpulse = 0.45f   // caps the kick near a cloud's natural top speed (~0.27 units/s), so it reads as a gentle gust rather than a fling
        private const val kWindSwipeMaxDt = 0.5f         // ignore stale/first-sample deltas larger than this (seconds)
        private const val kWindSwipeMinDt = 0.0005f      // ignore near-zero/duplicate-timestamp samples

        // --- Lens dirt (effect: subtle screen-space smudge near the sun) ---
        private const val kLensDirtCenterFalloff = 0.9f // how far (world units) the sun can be from center before dirt opacity reaches 0

        // --- Shooting star trail burst (effect: particle trail + gravity) ---
        private const val kShootingStarTrailParticleCount = 14      // particles spawned per shooting-star burst
        private const val kShootingStarTrailSpread = 0.05f          // how far back along the path particles start, in world units
        private const val kShootingStarTrailLateralScatter = 0.012f // sideways jitter off the path line
        private const val kShootingStarTrailSpeedScatterMin = 0.15f
        private const val kShootingStarTrailSpeedScatterRange = 0.35f
        private const val kShootingStarTrailVelocityJitter = 0.15f
        private const val kShootingStarTrailMinSize = 0.012f
        private const val kShootingStarTrailSizeRange = 0.012f
        private const val kShootingStarTrailMinLife = 0.35f
        private const val kShootingStarTrailLifeRange = 0.45f
        private const val kShootingStarTrailGravity = 0.55f // simulated gravity (units/sec^2), trail particles only
    }
}
