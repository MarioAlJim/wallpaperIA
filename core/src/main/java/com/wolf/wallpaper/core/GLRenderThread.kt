package com.wolf.wallpaper.core

import android.view.SurfaceHolder

class GLRenderThread(
    private val surfaceHolder: SurfaceHolder,
    private val renderer: GLRenderer
) : Thread("GLRenderThread") {

    private val eglHelper = EglHelper()
    private val lock = Object()
    
    @Volatile private var running = true
    @Volatile private var visible = false
    @Volatile private var width = 0
    @Volatile private var height = 0
    @Volatile private var surfaceChanged = false
    @Volatile private var pendingTouch: Pair<Float, Float>? = null
    @Volatile private var xOffset = 0.5f
    @Volatile private var yOffset = 0.5f

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

    fun queueTouch(x: Float, y: Float) {
        synchronized(lock) {
            pendingTouch = Pair(x, y)
            lock.notifyAll()
        }
    }

    fun queueOffsets(xOffset: Float, yOffset: Float) {
        synchronized(lock) {
            this.xOffset = xOffset
            this.yOffset = yOffset
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
            
            synchronized(lock) {
                if (surfaceHolder.surface != null && surfaceHolder.surface.isValid) {
                    eglHelper.createSurface(surfaceHolder)
                }
            }

            renderer.onSurfaceCreated()
            
            var lastTime = System.nanoTime()
            
            while (running) {
                synchronized(lock) {
                    while (running && (!visible || surfaceHolder.surface == null || !surfaceHolder.surface.isValid)) {
                        eglHelper.destroySurface()
                        
                        try {
                            lock.wait()
                        } catch (e: InterruptedException) {
                            // ignore
                        }
                        
                        if (running && visible && surfaceHolder.surface != null && surfaceHolder.surface.isValid) {
                            eglHelper.createSurface(surfaceHolder)
                            eglHelper.makeCurrent()
                            surfaceChanged = true
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
                }

                val touch = pendingTouch
                if (touch != null) {
                    pendingTouch = null
                    renderer.onTouchEvent(touch.first, touch.second)
                }

                var localXOffset = 0.5f
                var localYOffset = 0.5f
                synchronized(lock) {
                    localXOffset = xOffset
                    localYOffset = yOffset
                }
                renderer.onOffsetsChanged(localXOffset, localYOffset)

                val currentTime = System.nanoTime()
                val deltaTime = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime
                val clampedDelta = deltaTime.coerceAtMost(0.1f)

                renderer.onUpdate(clampedDelta)
                renderer.onDrawFrame()

                eglHelper.swapBuffers()

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
