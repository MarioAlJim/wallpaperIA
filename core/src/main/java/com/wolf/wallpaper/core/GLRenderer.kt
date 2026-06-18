package com.wolf.wallpaper.core

interface GLRenderer {
    fun onSurfaceCreated()
    fun onSurfaceChanged(width: Int, height: Int)
    fun onUpdate(deltaTime: Float)
    fun onDrawFrame()
    fun onTouchEvent(x: Float, y: Float) {}
}
