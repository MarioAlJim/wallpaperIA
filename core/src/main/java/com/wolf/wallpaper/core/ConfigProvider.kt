package com.wolf.wallpaper.core

interface ConfigProvider {
    fun getCloudDensity(): Int
    fun getRainIntensity(): Int
    fun getLightningFrequency(): Int
    fun getWindDirection(): Int
    fun getRainColorIndex(): Int
    fun getWindIntensity(): Int
    fun getRainSpeed(): Int
    fun getLightningColorIndex(): Int
    fun getLightningDuration(): Int
    fun getBackgroundIndex(): Int
    fun getCloudFlashFrequency(): Int
    fun getCloudFlashColorIndex(): Int
    fun getCloudDynamicsSpeed(): Int
    fun isLightningFlashEnabled(): Boolean
    fun isCloudFlashEnabled(): Boolean
    fun isInteractiveLightningEnabled(): Boolean
    
    // New extensible settings for Sunny and other weather modes
    fun getActiveEffect(): Int // 0: Storm, 1: Sunny
    fun getSunSize(): Int // 0-100
    fun getSunSpeed(): Int // 0-100
    fun getSunnyTheme(): Int // 0: Noon Blue, 1: Sunset Orange, 2: Purple Dusk
}
