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
    fun getSunPathDirection(): Int // 0: Left-to-Right, 1: Right-to-Left, 2: Stationary, 3: Random, 4: Chaotic
    fun getSunMoveSpeed(): Int // 0-100
    fun getSunnyBackgroundIndex(): Int // 0: None, 1: Mountains, 2: Fields, 3: Lake/Forest, 4: Gallery
    fun getSunStationaryPosition(): Int // 0: Top Left, 1: Top Right, 2: Center, 3: Left Edge, 4: Right Edge
    fun getSunCustomX(): Int // 0-100
    fun getSunCustomY(): Int // 0-100
    fun getMoonCustomX(): Int // 0-100
    fun getMoonCustomY(): Int // 0-100
    fun getSunnyCustomSkyTopColor(): Int
    fun getSunnyCustomSkyBottomColor(): Int
    
    fun isSunnyGodRaysEnabled(): Boolean
    fun getSunnyGodRaysIntensity(): Int // 0-100
    fun isSunnyLensFlareEnabled(): Boolean
    fun isSunnyLensFlareChromaticEnabled(): Boolean
    fun getSunnyLensFlareIntensity(): Int // 0-100
    
    fun isSunnyGyroEnabled(): Boolean
    fun isSunnyTouchBurstEnabled(): Boolean
    
    fun isScreenDropletsEnabled(): Boolean
    fun getScreenDropletsSize(): Int
    fun getRainSpawnMode(): Int // 0: Top Edge, 1: Clouds, 2: Everywhere
    fun isWindLinesEnabled(): Boolean
    fun getWindLinesIntensity(): Int // 0-100

    fun getTimeMode(): Int              // 0 = Día, 1 = Noche, 2 = Combinado
    fun getCombinedSunSize(): Int       // 0–100
    fun getCombinedMoonSize(): Int      // 0–100
    fun getCombinedMoonPhase(): Int     // 0–7
    fun getCombinedStarDensity(): Int   // 0–100
    fun getCombinedStarColorIndex(): Int // 0–5
    fun getCombinedPathDirection(): Int // 0 = L2R both, 1 = R2L both, 2 = Sun L2R Moon R2L, 3 = Sun R2L Moon L2R
    fun isCombinedMoonPhaseAutoAdvanceEnabled(): Boolean // when true, the renderer advances the moon phase itself each completed day+night cycle, ignoring getCombinedMoonPhase() after the first read

    fun getMoonPhase(): Int             // 0–7
    fun getMoonPathDirection(): Int     // 0 = IzqaDer, 1 = DeraIzq, 2 = Estático, 3 = Aleatorio, 4 = Caótico
    fun getMoonMoveSpeed(): Int         // 0–100
    fun getMoonStationaryPosition(): Int // 0–5
    fun getStarColorIndex(): Int        // 0–5
    fun getStarDensity(): Int           // 0–100
    fun getStarMode(): Int              // 0: Static, 1: Random
    fun getNightCloudDensity(): Int     // 0–100
    fun getGradientCycleSpeed(): Int    // 0–100
    fun getNebulaIntensity(): Int       // 0–100
    fun getNebulaSeed(): Int            // stable per-install seed for the nebula's noise pattern, so each user sees a different-looking nebula
    fun getNebulaTheme(): Int           // 0: Púrpura Cósmico, 1: Esmeralda, 2: Fuego, 3: Azul Profundo
    fun getNebulaSpeed(): Int           // 0–100, drift speed multiplier (50 = original speed)
    fun getNebulaScale(): Int           // 0–100, pattern scale/size multiplier (50 = original scale)

    fun isShootingStarsEnabled(): Boolean
    fun getShootingStarsDensity(): Int   // 0–100
    fun isFirefliesEnabled(): Boolean
    fun isStormVariableEnabled(): Boolean
    fun getLightningSpawnMode(): Int
    fun getBgBrightness(): Int
    fun isRainbowAlwaysEnabled(): Boolean
}

