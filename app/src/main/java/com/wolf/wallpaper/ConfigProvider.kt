package com.wolf.wallpaper

interface ConfigProvider {
    fun getCloudDensity(): Int
    fun getRainIntensity(): Int
    fun getLightningFrequency(): Int
    fun getWindDirection(): Int
}
