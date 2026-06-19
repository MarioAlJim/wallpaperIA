package com.wolf.wallpaper

import android.content.SharedPreferences
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.wolf.wallpaper.core.ConfigManager
import com.wolf.wallpaper.core.GLRenderThread
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.WeatherType

class DynamicWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DynamicEngine()
    }

    inner class DynamicEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var renderThread: GLRenderThread? = null
        private lateinit var configManager: ConfigManager
        private lateinit var rendererFactory: AppWallpaperRendererFactory
        private var currentRenderer: GLRenderer? = null
        private var activeWeatherType = WeatherType.STORM
        private var currentHolder: SurfaceHolder? = null
        private var currentWidth = 0
        private var currentHeight = 0

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            configManager = ConfigManager(applicationContext)
            rendererFactory = AppWallpaperRendererFactory(configManager)
            
            activeWeatherType = WeatherType.fromIndex(configManager.getActiveEffect())
            currentRenderer = rendererFactory.createRenderer(applicationContext, activeWeatherType)
            
            setTouchEventsEnabled(true)
            
            applicationContext.getSharedPreferences(ConfigManager.PREFS_NAME, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this)
        }

        private var isDragging = false
        private var startX = 0f
        private var virtualXOffset = 0.5f
        private var startOffset = 0.5f

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event != null) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = true
                        startX = event.x
                        startOffset = virtualXOffset
                        renderThread?.queueTouch(event.x, event.y)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            val diffX = event.x - startX
                            val screenWidth = currentWidth.toFloat()
                            if (screenWidth > 0f) {
                                val deltaOffset = -diffX / screenWidth
                                virtualXOffset = (startOffset + deltaOffset).coerceIn(0f, 1f)
                                renderThread?.queueOffsets(virtualXOffset, 0.5f)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                    }
                }
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xStep: Float,
            yStep: Float,
            xPixels: Int,
            yPixels: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels)
            val clampedX = xOffset.coerceIn(0f, 1f)
            virtualXOffset = clampedX // Keep touch simulation state synchronized with launcher state
            renderThread?.queueOffsets(clampedX, yOffset)
        }

        override fun onDestroy() {
            super.onDestroy()
            applicationContext.getSharedPreferences(ConfigManager.PREFS_NAME, MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this)
            stopRenderThread()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            renderThread?.setVisible(visible)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            startRenderThread(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            currentWidth = width
            currentHeight = height
            renderThread?.onSurfaceChanged(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            currentHolder = null
            stopRenderThread()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == ConfigManager.KEY_ACTIVE_EFFECT) {
                val newIndex = configManager.getActiveEffect()
                val newWeatherType = WeatherType.fromIndex(newIndex)
                if (newWeatherType != activeWeatherType) {
                    activeWeatherType = newWeatherType
                    
                    // Detener e inicializar con el nuevo renderizador en caliente
                    val holder = currentHolder
                    if (holder != null) {
                        currentRenderer = rendererFactory.createRenderer(applicationContext, activeWeatherType)
                        startRenderThread(holder)
                    } else {
                        currentRenderer = rendererFactory.createRenderer(applicationContext, activeWeatherType)
                    }
                }
            }
        }

        private fun startRenderThread(holder: SurfaceHolder) {
            stopRenderThread()
            val renderer = currentRenderer ?: return
            renderThread = GLRenderThread(holder, renderer).apply {
                setVisible(isVisible)
                if (currentWidth > 0 && currentHeight > 0) {
                    onSurfaceChanged(currentWidth, currentHeight)
                }
                start()
            }
        }

        private fun stopRenderThread() {
            renderThread?.apply {
                shutdown()
                try {
                    join(1000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            renderThread = null
        }
    }
}
