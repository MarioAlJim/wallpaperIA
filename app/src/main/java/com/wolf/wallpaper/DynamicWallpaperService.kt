package com.wolf.wallpaper

import android.content.SharedPreferences
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.wolf.wallpaper.core.ConfigManager
import com.wolf.wallpaper.core.GLRenderThread
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.WeatherType
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class DynamicWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DynamicEngine()
    }

    inner class DynamicEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
        private var renderThread: GLRenderThread? = null
        private lateinit var configManager: ConfigManager
        private lateinit var rendererFactory: AppWallpaperRendererFactory
        private var currentRenderer: GLRenderer? = null
        private var activeWeatherType = WeatherType.STORM
        private var currentHolder: SurfaceHolder? = null
        private var currentWidth = 0
        private var currentHeight = 0

        // Sensor variables for 3D tilt
        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null
        private var smoothedTiltX = 0f
        private var smoothedTiltY = 0f

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            configManager = ConfigManager(applicationContext)
            rendererFactory = AppWallpaperRendererFactory(configManager)
            
            activeWeatherType = WeatherType.fromIndex(configManager.getActiveEffect())
            currentRenderer = rendererFactory.createRenderer(applicationContext, activeWeatherType)
            
            setTouchEventsEnabled(true)
            
            applicationContext.getSharedPreferences(ConfigManager.PREFS_NAME, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this)

            sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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
            sensorManager?.unregisterListener(this)
            stopRenderThread()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            renderThread?.setVisible(visible)
            if (visible) {
                val acc = accelerometer
                if (acc != null) {
                    sensorManager?.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME)
                }
            } else {
                sensorManager?.unregisterListener(this)
            }
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

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                if (!configManager.isSunnyGyroEnabled()) {
                    smoothedTiltX = 0f
                    smoothedTiltY = 0f
                    renderThread?.queueSensorValues(0f, 0f)
                    return
                }

                val ax = event.values[0]
                val ay = event.values[1]

                // Standardize range: ax maps approx to [-1.0, 1.0]
                val targetTiltX = (ax / 9.8f).coerceIn(-1f, 1f)
                // When device is vertical, ay is ~9.8. When flat, ay is ~0.
                // Map ay ~ 4.9m/s^2 (45 degrees tilt) to 0.0
                val targetTiltY = ((ay - 4.9f) / 4.9f).coerceIn(-1f, 1f)

                // Low-pass filter to smooth movement
                val alpha = 0.1f
                smoothedTiltX = smoothedTiltX + alpha * (targetTiltX - smoothedTiltX)
                smoothedTiltY = smoothedTiltY + alpha * (targetTiltY - smoothedTiltY)

                renderThread?.queueSensorValues(smoothedTiltX, smoothedTiltY)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}
