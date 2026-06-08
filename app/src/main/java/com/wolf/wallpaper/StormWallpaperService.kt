package com.wolf.wallpaper

import android.content.SharedPreferences
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class StormWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return StormEngine()
    }

    inner class StormEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var renderThread: GLRenderThread? = null
        private lateinit var configManager: ConfigManager
        private lateinit var sceneManager: SceneManager
        private lateinit var renderer: StormRenderer

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            configManager = ConfigManager(applicationContext)
            sceneManager = SceneManager(applicationContext, configManager)
            renderer = StormRenderer(applicationContext)
            
            // Listen for shared preference changes from the Settings activity to update variables dynamically
            applicationContext.getSharedPreferences(ConfigManager.PREFS_NAME, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this)
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
            startRenderThread(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            renderThread?.onSurfaceChanged(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopRenderThread()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            // Configuration is queried on every frame in SceneManager.update(), so updates apply automatically.
        }

        private fun startRenderThread(holder: SurfaceHolder) {
            stopRenderThread()
            renderThread = GLRenderThread(holder, renderer, sceneManager).apply {
                setVisible(isVisible)
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
