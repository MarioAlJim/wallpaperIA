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
    fun isScreenDropletsEnabled(): Boolean
    fun getScreenDropletsSize(): Int
    fun getRainSpawnMode(): Int // 0: Top Edge, 1: Clouds, 2: Everywhere
    fun isWindLinesEnabled(): Boolean
    fun getWindLinesIntensity(): Int // 0-100
    fun isStormVariableEnabled(): Boolean
    fun getStormVariableDuration(): Int = 1 // 0: Corta, 1: Media, 2: Larga
    fun getLightningSpawnMode(): Int
    fun getBgBrightness(): Int
    fun isSunnyGyroEnabled(): Boolean = true
    fun getActiveEffect(): Int = 0
}
