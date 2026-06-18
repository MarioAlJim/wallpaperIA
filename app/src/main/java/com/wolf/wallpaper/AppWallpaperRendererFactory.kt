package com.wolf.wallpaper

import android.content.Context
import com.wolf.wallpaper.core.ConfigProvider
import com.wolf.wallpaper.core.GLRenderer
import com.wolf.wallpaper.core.WallpaperRendererFactory
import com.wolf.wallpaper.core.WeatherType
import com.wolf.wallpaper.storm.SceneManager
import com.wolf.wallpaper.storm.StormRenderer
import com.wolf.wallpaper.sunny.SunnyRenderer

class AppWallpaperRendererFactory(private val configProvider: ConfigProvider) : WallpaperRendererFactory {
    override fun createRenderer(context: Context, weatherType: WeatherType): GLRenderer {
        return when (weatherType) {
            WeatherType.STORM -> {
                val sceneManager = SceneManager(context, configProvider)
                StormRenderer(context, sceneManager)
            }
            WeatherType.SUNNY -> {
                SunnyRenderer(context, configProvider)
            }
        }
    }
}
