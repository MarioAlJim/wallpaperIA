package com.wolf.wallpaper

import android.view.SurfaceHolder

class GLRenderThread(
    private val surfaceHolder: SurfaceHolder,
    private val renderer: StormRenderer,
    private val sceneManager: SceneManager
) : Thread("GLRenderThread") {

    private val eglHelper = EglHelper()
    private val lock = Object()
    
    @Volatile private var running = true
    @Volatile private var visible = false
    @Volatile private var width = 0
    @Volatile private var height = 0
    @Volatile private var surfaceChanged = false

    fun setVisible(visible: Boolean) {
        synchronized(lock) {
            this.visible = visible
            lock.notifyAll()
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        synchronized(lock) {
            this.width = width
            this.height = height
            this.surfaceChanged = true
            lock.notifyAll()
        }
    }

    fun shutdown() {
        synchronized(lock) {
            running = false
            lock.notifyAll()
        }
    }

    override fun run() {
        try {
            eglHelper.initEgl()
            
            // Create the EGL surface immediately if we have a valid surface, regardless of visibility,
            // so we have a valid active OpenGL context to compile shaders and load textures in onSurfaceCreated().
            synchronized(lock) {
                if (surfaceHolder.surface != null && surfaceHolder.surface.isValid) {
                    eglHelper.createSurface(surfaceHolder)
                }
            }

            renderer.onSurfaceCreated()
            
            var lastTime = System.nanoTime()
            
            while (running) {
                synchronized(lock) {
                    // RF-007 and RNF-004: If invisible or surface invalid, sleep to conserve resources
                    while (running && (!visible || surfaceHolder.surface == null || !surfaceHolder.surface.isValid)) {
                        eglHelper.destroySurface() // Release EGL surface
                        
                        try {
                            lock.wait()
                        } catch (e: InterruptedException) {
                            // ignore
                        }
                        
                        // When waking up, recreate EGL surface if we are visible and have valid surface
                        if (running && visible && surfaceHolder.surface != null && surfaceHolder.surface.isValid) {
                            eglHelper.createSurface(surfaceHolder)
                            eglHelper.makeCurrent()
                            surfaceChanged = true // Trigger projection recalculation
                        }
                    }
                }
                
                if (!running) break

                var localWidth = 0
                var localHeight = 0
                var needsViewportUpdate = false

                synchronized(lock) {
                    if (surfaceChanged) {
                        localWidth = width
                        localHeight = height
                        needsViewportUpdate = true
                        surfaceChanged = false
                    }
                }

                if (needsViewportUpdate) {
                    renderer.onSurfaceChanged(localWidth, localHeight)
                    sceneManager.onSurfaceChanged(localWidth, localHeight)
                }

                val currentTime = System.nanoTime()
                // Convert nanoseconds to float seconds
                val deltaTime = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                // Clamp deltaTime to avoid sudden jumps when thread lags
                val clampedDelta = deltaTime.coerceAtMost(0.1f)

                // Update physics positions of objects
                sceneManager.update(clampedDelta)

                // Render current frame
                renderer.drawFrame(sceneManager)

                // Swap graphics buffers
                eglHelper.swapBuffers()

                // Sleep to maintain ~60 FPS (approx 16.6ms per frame)
                val elapsedMs = (System.nanoTime() - currentTime) / 1_000_000
                val sleepTime = 16 - elapsedMs
                if (sleepTime > 0) {
                    try {
                        sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        // ignore
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            eglHelper.destroySurface()
            eglHelper.destroyEgl()
        }
    }
}
