package com.wolf.wallpaper.core

import android.content.Context

interface WallpaperRendererFactory {
    fun createRenderer(context: Context, weatherType: WeatherType): GLRenderer
}
