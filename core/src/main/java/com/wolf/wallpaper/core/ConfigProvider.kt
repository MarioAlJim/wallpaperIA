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
    fun getSunPathDirection(): Int // 0: Left-to-Right, 1: Right-to-Left, 2: Stationary
    fun getSunMoveSpeed(): Int // 0-100
    fun getSunnyBackgroundIndex(): Int // 0: None, 1: Mountains, 2: Fields, 3: Lake/Forest, 4: Gallery
    fun getSunStationaryPosition(): Int // 0: Top Left, 1: Top Right, 2: Center, 3: Left Edge, 4: Right Edge
    fun getSunCustomX(): Int // 0-100
    fun getSunCustomY(): Int // 0-100
    fun getSunnyCustomSkyTopColor(): Int
    fun getSunnyCustomSkyBottomColor(): Int
    
    fun isSunnyGodRaysEnabled(): Boolean
    fun getSunnyGodRaysIntensity(): Int // 0-100
    fun isSunnyLensFlareEnabled(): Boolean
    fun getSunnyLensFlareIntensity(): Int // 0-100
    
    fun isSunnyGyroEnabled(): Boolean
    fun isSunnyTouchBurstEnabled(): Boolean
    
    fun isScreenDropletsEnabled(): Boolean
    fun getScreenDropletsSize(): Int
}

