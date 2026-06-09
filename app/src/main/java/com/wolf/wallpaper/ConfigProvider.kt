package com.wolf.wallpaper

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
}
