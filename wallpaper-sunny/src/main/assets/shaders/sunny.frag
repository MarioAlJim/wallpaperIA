#version 300 es
precision mediump float;

in vec2 vPosition;
out vec4 fragColor;

uniform float uTime;
uniform float uAspectRatio;
uniform float uSunSize; // 0.1 to 0.35
uniform float uSunSpeed; // speed coefficient
uniform int uTheme; // 0: Noon Blue, 1: Sunset Orange, 2: Purple Dusk
uniform vec2 uSunPos;
uniform vec3 uSkyTop;
uniform vec3 uSkyBottom;
uniform float uGodRaysIntensity;
uniform float uSunFadeFactor;
uniform float uSunPulse;
uniform float uNightIntensity; // 0.0 for full day, 1.0 for full night
uniform sampler2D uNoiseTex; // Precomputed tileable value-noise texture (GL_REPEAT, GL_LINEAR)
uniform float uCloudOcclusionNearSun; // 0..1, "cloud density near the sun" computed on the CPU (SunnyRenderer.computeCloudOcclusionNearSun)
uniform float uRainbowOpacity; // 0..1, CPU-driven rare-event fade in/hold/fade out (SunnyRenderer.updateRainbow)
uniform float uLensDirtOpacity; // 0..1, up when the sun is visible near screen center, 0 otherwise (mirrors the lens flare visibility gating)
uniform float uNebulaIntensity;

uniform int uBgActive;
uniform vec2 uBgTranslation;
uniform vec2 uBgScale;
uniform sampler2D uBackgroundTex;

// Approximates the old 4-octave hash/fract-based fbm() by summing a few samples of a
// precomputed, bilinearly-filtered noise texture at different scales/weights instead of
// computing the noise function in ALU per pixel. GL_LINEAR sampling already gives a
// smoothly interpolated value-noise result per sample.
float fbmTex(vec2 p) {
    float value = 0.0;
    value += 0.5 * texture(uNoiseTex, p).r;
    value += 0.25 * texture(uNoiseTex, p * 2.03 + 17.3).r;
    value += 0.125 * texture(uNoiseTex, p * 4.01 + 5.7).r;
    return value;
}

void main() {
    // 1. Sky Gradient based on Theme
    float tSky = (vPosition.y + 1.0) * 0.5; // ranges 0.0 to 1.0
    
    vec3 skyTop;
    vec3 skyBottom;
    vec3 sunColor;
    vec3 sunGlowColor;

    vec3 dayTop, dayBottom, daySun, dayGlow;
    vec3 sunsetTop, sunsetBottom, sunsetSun, sunsetGlow;
    vec3 dawnTop, dawnBottom, dawnSun, dawnGlow;

    if (uTheme == 0) { // Noon Blue
        dayTop = vec3(0.05, 0.4, 0.85);
        dayBottom = vec3(0.55, 0.8, 0.95);
        daySun = vec3(1.0, 1.0, 0.9);
        dayGlow = vec3(1.0, 0.7, 0.2);

        // Sunset/Dusk intermediate: Sunset Orange
        sunsetTop = vec3(0.1, 0.05, 0.25);
        sunsetBottom = vec3(0.95, 0.45, 0.1);
        sunsetSun = vec3(1.0, 0.95, 0.7);
        sunsetGlow = vec3(0.9, 0.2, 0.05);

        // Sunrise/Dawn intermediate: Purple Dusk
        dawnTop = vec3(0.15, 0.1, 0.35);
        dawnBottom = vec3(0.9, 0.5, 0.55);
        dawnSun = vec3(1.0, 0.9, 0.95);
        dawnGlow = vec3(0.8, 0.3, 0.6);
    } else if (uTheme == 1) { // Sunset Orange
        dayTop = vec3(0.1, 0.05, 0.25);
        dayBottom = vec3(0.95, 0.45, 0.1);
        daySun = vec3(1.0, 0.95, 0.7);
        dayGlow = vec3(0.9, 0.2, 0.05);

        // Sunset/Dusk intermediate: Purple Dusk
        sunsetTop = vec3(0.15, 0.1, 0.35);
        sunsetBottom = vec3(0.9, 0.5, 0.55);
        sunsetSun = vec3(1.0, 0.9, 0.95);
        sunsetGlow = vec3(0.8, 0.3, 0.6);

        // Sunrise/Dawn intermediate: Noon Blue / Gold
        dawnTop = vec3(0.05, 0.4, 0.85);
        dawnBottom = vec3(0.55, 0.8, 0.95);
        dawnSun = vec3(1.0, 1.0, 0.9);
        dawnGlow = vec3(1.0, 0.7, 0.2);
    } else if (uTheme == 2) { // Purple Dusk
        dayTop = vec3(0.15, 0.1, 0.35);
        dayBottom = vec3(0.9, 0.5, 0.55);
        daySun = vec3(1.0, 0.9, 0.95);
        dayGlow = vec3(0.8, 0.3, 0.6);

        // Sunset/Dusk intermediate: Dark blue-violet
        sunsetTop = vec3(0.05, 0.05, 0.20);
        sunsetBottom = vec3(0.40, 0.20, 0.50);
        sunsetSun = vec3(0.9, 0.8, 0.95);
        sunsetGlow = vec3(0.5, 0.1, 0.5);

        // Sunrise/Dawn intermediate: Warm Orange / Gold
        dawnTop = vec3(0.1, 0.2, 0.5);
        dawnBottom = vec3(0.95, 0.6, 0.4);
        dawnSun = vec3(1.0, 0.95, 0.7);
        dawnGlow = vec3(0.9, 0.4, 0.2);
    } else { // Custom
        dayTop = uSkyTop;
        dayBottom = uSkyBottom;
        daySun = vec3(1.0, 0.98, 0.9);
        dayGlow = mix(dayBottom, vec3(1.0, 0.7, 0.3), 0.5);

        // Custom intermediates
        sunsetTop = mix(dayTop, vec3(0.1, 0.05, 0.25), 0.5);
        sunsetBottom = mix(dayBottom, vec3(0.95, 0.45, 0.1), 0.6);
        sunsetSun = vec3(1.0, 0.95, 0.8);
        sunsetGlow = vec3(0.9, 0.4, 0.2);

        dawnTop = mix(dayTop, vec3(0.15, 0.1, 0.35), 0.5);
        dawnBottom = mix(dayBottom, vec3(0.9, 0.5, 0.55), 0.6);
        dawnSun = vec3(1.0, 0.9, 0.9);
        dawnGlow = vec3(0.8, 0.4, 0.5);
    }

    // Calculate transition weights based on uNightIntensity and sun position
    float wDay = clamp(1.0 - uNightIntensity * 2.0, 0.0, 1.0);
    float wSunset = 0.0;
    float wDawn = 0.0;
    
    if (uSunPos.x >= 0.0) {
        wSunset = clamp(1.0 - abs(uNightIntensity - 0.5) * 2.0, 0.0, 1.0);
    } else {
        wDawn = clamp(1.0 - abs(uNightIntensity - 0.5) * 2.0, 0.0, 1.0);
    }

    float sumWeights = wDay + wSunset + wDawn;
    if (sumWeights > 0.001) {
        skyTop = (dayTop * wDay + sunsetTop * wSunset + dawnTop * wDawn) / sumWeights;
        skyBottom = (dayBottom * wDay + sunsetBottom * wSunset + dawnBottom * wDawn) / sumWeights;
        sunColor = (daySun * wDay + sunsetSun * wSunset + dawnSun * wDawn) / sumWeights;
        sunGlowColor = (dayGlow * wDay + sunsetGlow * wSunset + dawnGlow * wDawn) / sumWeights;
    } else {
        skyTop = dayTop;
        skyBottom = dayBottom;
        sunColor = daySun;
        sunGlowColor = dayGlow;
    }

    vec3 skyColor = mix(skyBottom, skyTop, tSky);

    // Transition to Night Sky Colors if uNightIntensity > 0.0
    if (uNightIntensity > 0.001) {
        vec3 nightSkyTop = vec3(0.005, 0.005, 0.02);    // Very dark blue
        vec3 nightSkyBottom = vec3(0.015, 0.01, 0.035); // Slightly lighter indigo
        vec3 nightSkyColor = mix(nightSkyBottom, nightSkyTop, tSky);

        // --- Procedural Nebulae (Vía Láctea / Polvo Galáctico) ---
        vec2 uvCoords = vec2(vPosition.x * uAspectRatio, vPosition.y);
        vec2 nebulaUV = uvCoords * 1.6;
        // Ultra-slow movement over time
        nebulaUV.x += uTime * 0.0012;
        nebulaUV.y += uTime * 0.0006;
        
        float nebulaNoiseVal = fbmTex(nebulaUV);
        
        // Add a bit of fine detail to the nebula using another octave of noise for cosmic dust texture
        float fineNoise = texture(uNoiseTex, nebulaUV * 3.2 - vec2(uTime * 0.0018, uTime * 0.0009)).r;
        float combinedNoise = nebulaNoiseVal * 0.85 + fineNoise * 0.15;
        float nebulaMask = smoothstep(0.28, 0.72, combinedNoise);
        
        // Vibrant cosmic colors for nebulae (mixture of purple, indigo-blue, and magenta/pink)
        vec3 nebulaColor1 = vec3(0.38, 0.08, 0.58); // Rich purple-magenta
        vec3 nebulaColor2 = vec3(0.06, 0.14, 0.44); // Deep galactic blue-indigo
        vec3 nebulaColor3 = vec3(0.62, 0.15, 0.42); // Vibrant magenta highlights
        
        vec3 mixedNebula = mix(nebulaColor2, nebulaColor1, smoothstep(0.35, 0.65, combinedNoise));
        mixedNebula = mix(mixedNebula, nebulaColor3, smoothstep(0.55, 0.80, combinedNoise));
        
        // Blend nebulae into night sky color based on uNebulaIntensity
        // Max opacity is 0.75 for a gorgeous visible galactic dust effect
        nightSkyColor = mix(nightSkyColor, mixedNebula, nebulaMask * uNebulaIntensity * 0.75);

        // --- Night Haze (Bruma Nocturna) ---
        // Soft fog band near the horizon (around y = -0.45)
        float hazeMask = smoothstep(-0.85, -0.42, vPosition.y) * (1.0 - smoothstep(-0.42, 0.08, vPosition.y));
        // Slow movement for the haze
        float hazeOffset = sin(uvCoords.x * 2.5 + uTime * 0.03) * 0.03 * cos(uvCoords.x * 0.9 + uTime * 0.02);
        float animatedHaze = smoothstep(-0.85 + hazeOffset, -0.42 + hazeOffset, vPosition.y) * (1.0 - smoothstep(-0.42 + hazeOffset, 0.08 + hazeOffset, vPosition.y));
        
        // Very subtle horizon glow at night (soft blue-gray mist)
        vec3 hazeColor = vec3(0.08, 0.09, 0.16); 
        nightSkyColor = mix(nightSkyColor, hazeColor, animatedHaze * 0.16);

        skyColor = mix(skyColor, nightSkyColor, uNightIntensity);
    }

    // 2. Sun setup
    vec2 uv = vec2(vPosition.x * uAspectRatio, vPosition.y);
    
    float dist = distance(uv, uSunPos * uAspectRatio);
    
    // Pulsating radius based on uSunSize and uSunSpeed
    float pulse = sin(uTime * uSunSpeed) * 0.02 * (uSunSize / 0.20);
    float radius = uSunSize + pulse + uSunPulse * 0.08;
    
    // Smooth sun edge
    float sunMask = smoothstep(radius, radius - 0.008, dist);
    
    // Soft outer glow extending further
    float glowMask = smoothstep(radius * (3.5 + uSunPulse * 1.5), radius, dist);
    vec3 finalGlow = sunGlowColor * (glowMask * (0.65 + uSunPulse * 0.5));

    // --- Enhanced Rayleigh halo: a soft warm (white -> yellow) ring around the sun, wider
    // than the glow above, that fuses into the surrounding sky. Implemented as a Gaussian
    // "ring" band in normalized sun-distance space so it fades in AND out smoothly without
    // ever touching the sharp sun disc. Added on top of finalGlow (additive, subtle) so it
    // only ever brightens, never saturates/washes out the sky.
    const float kHaloPeakDist = 5.0;  // ring center, in multiples of the sun radius
    const float kHaloWidth = 2.2;     // ring softness/width
    const float kHaloIntensityNoon = 0.22;
    const float kHaloIntensitySunset = 0.26;
    const float kHaloIntensityDusk = 0.09;
    const float kHaloIntensityCustom = 0.18;
    float haloDistNorm = dist / max(radius, 0.0001);
    float haloDelta = (haloDistNorm - kHaloPeakDist) / kHaloWidth;
    // Avoid pow(x, 2.0) here: GLSL ES defines pow() as undefined for a negative base
    // (haloDelta is negative for most of the screen), so square it via a plain multiply.
    float haloBand = exp(-(haloDelta * haloDelta));
    float haloThemeIntensity;
    if (uTheme == 0) {
        haloThemeIntensity = kHaloIntensityNoon;
    } else if (uTheme == 1) {
        haloThemeIntensity = kHaloIntensitySunset;
    } else if (uTheme == 2) {
        haloThemeIntensity = kHaloIntensityDusk;
    } else {
        haloThemeIntensity = kHaloIntensityCustom;
    }
    haloThemeIntensity *= (1.0 - uNightIntensity); // vanish as the sky goes dark
    vec3 haloColor = mix(vec3(1.0, 0.98, 0.85), sunGlowColor, 0.35); // white -> yellow, tinted toward the theme's own glow color
    finalGlow += haloColor * haloBand * haloThemeIntensity;

    // Dynamic God Rays (Crepuscular Rays), extended with an approximate "cloud shafts" look:
    // when uCloudOcclusionNearSun (computed on the CPU from clouds near the sun) is > 0, the
    // rays blend toward a lower-frequency pattern and spread further/softer, as if emanating
    // from behind the clouds, while staying identical to the original look when it's 0.
    if (uGodRaysIntensity > 0.0) {
        float angle = atan(uv.y - uSunPos.y * uAspectRatio, uv.x - uSunPos.x * uAspectRatio);
        float rays = sin(angle * 12.0 + uTime * 0.15) * 0.5 + 0.5;
        rays += sin(angle * 27.0 - uTime * 0.25) * 0.3 + 0.3;
        rays += sin(angle * 53.0 + uTime * 0.4) * 0.15 + 0.15;
        rays += sin(angle * 89.0 - uTime * 0.6) * 0.05 + 0.05;
        rays = rays / 2.0; // Normalize [0, 1]

        const float kCloudShaftLowFreqA = 4.0;
        const float kCloudShaftLowFreqB = 7.0;
        const float kCloudShaftRayBlend = 0.6;   // how much of the shaft pattern replaces the base rays at full occlusion
        const float kCloudShaftMaxBoost = 0.9;   // extra ray strength contributed at full occlusion
        const float kCloudShaftMinAttenuation = 0.55; // wider spread (slower falloff) near clouds vs the base 0.8
        float shaftRays = sin(angle * kCloudShaftLowFreqA + uTime * 0.08) * 0.5 + 0.5;
        shaftRays += sin(angle * kCloudShaftLowFreqB - uTime * 0.12) * 0.5 + 0.5;
        shaftRays *= 0.5;
        rays = mix(rays, mix(rays, shaftRays, kCloudShaftRayBlend), uCloudOcclusionNearSun);

        float rayAttenuation = exp(-dist * mix(0.8, kCloudShaftMinAttenuation, uCloudOcclusionNearSun));
        float occlusionBoost = 1.0 + uCloudOcclusionNearSun * kCloudShaftMaxBoost;

        // Raymarching crepuscular rays occlusion by background mountains
        float bgOcclusion = 1.0;
        if (uBgActive == 1) {
            for (int i = 1; i <= 4; i++) {
                float t = float(i) / 4.0;
                vec2 Q = mix(uSunPos, vPosition, t);
                
                // Convert Q to background texture coordinates
                vec2 worldPos = vec2(Q.x * uAspectRatio, Q.y);
                vec2 localPos = (worldPos - uBgTranslation) / uBgScale;
                vec2 uv = vec2(localPos.x * 0.5 + 0.5, 0.5 - localPos.y * 0.5);
                
                if (uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0) {
                    vec4 tex = texture(uBackgroundTex, uv);
                    float luma = (tex.r + tex.g + tex.b) / 3.0;
                    if (luma < 0.90) {
                        bgOcclusion *= 0.35;
                    }
                }
            }
        }

        finalGlow += sunGlowColor * rays * rayAttenuation * uGodRaysIntensity * 1.5 * occlusionBoost * bgOcclusion;
    }

    // Combine
    vec3 finalColor = mix(skyColor, sunColor, sunMask * uSunFadeFactor) + finalGlow * uSunFadeFactor * (1.0 - sunMask * uSunFadeFactor);

    // --- Subtle rainbow (rare event, day skies only): a thin spectral ring around the
    // stylized anti-solar point (the sun's position reflected through the screen center).
    // uRainbowOpacity is 0 almost always (rare CPU-driven timer with fade in/hold/fade out)
    // and this branch is a uniform (not per-fragment) condition, so it costs nothing on the
    // overwhelming majority of frames where no rainbow is active.
    if (uRainbowOpacity > 0.001) {
        const float kRainbowRadius = 1.55;    // ring radius (same uv units as the sun distance above)
        const float kRainbowBandWidth = 0.10; // total ring thickness across all color bands
        const float kRainbowMaxIntensity = 0.45; // global cap - visible and beautiful
        vec2 antiSolarPos = -(uSunPos * uAspectRatio);
        float distToAntiSolar = distance(uv, antiSolarPos);
        float ringDist = abs(distToAntiSolar - kRainbowRadius);
        float ringMask = smoothstep(kRainbowBandWidth, 0.0, ringDist);
        if (ringMask > 0.0) {
            float bandT = clamp((distToAntiSolar - (kRainbowRadius - kRainbowBandWidth * 0.5)) / kRainbowBandWidth, 0.0, 1.0);
            // 7-color spectral gradient: red on the outside (bandT=0) -> violet inside (bandT=1)
            vec3 rainbowColor;
            if (bandT < 0.166) {
                rainbowColor = mix(vec3(1.0, 0.0, 0.0), vec3(1.0, 0.5, 0.0), bandT / 0.166);
            } else if (bandT < 0.333) {
                rainbowColor = mix(vec3(1.0, 0.5, 0.0), vec3(1.0, 1.0, 0.0), (bandT - 0.166) / 0.167);
            } else if (bandT < 0.5) {
                rainbowColor = mix(vec3(1.0, 1.0, 0.0), vec3(0.0, 1.0, 0.0), (bandT - 0.333) / 0.167);
            } else if (bandT < 0.666) {
                rainbowColor = mix(vec3(0.0, 1.0, 0.0), vec3(0.0, 0.4, 1.0), (bandT - 0.5) / 0.166);
            } else if (bandT < 0.833) {
                rainbowColor = mix(vec3(0.0, 0.4, 1.0), vec3(0.29, 0.0, 0.51), (bandT - 0.666) / 0.167);
            } else {
                rainbowColor = mix(vec3(0.29, 0.0, 0.51), vec3(0.56, 0.0, 1.0), (bandT - 0.833) / 0.167);
            }
            finalColor += rainbowColor * ringMask * uRainbowOpacity * kRainbowMaxIntensity;
        }
    }

    // --- Lens dirt: a very subtle, screen-space-fixed smudge/dust pattern that darkens the
    // final image slightly, reusing the precomputed noise texture (uNoiseTex) at a much
    // finer tiling than the nebula use above so it reads as fine grain rather than soft
    // blobs. Opacity (uLensDirtOpacity) is computed on the CPU from the same "is the sun
    // visible near screen center" logic that gates the lens flare, so it is 0 whenever the
    // sun itself is not visible.
    if (uLensDirtOpacity > 0.001) {
        const float kLensDirtMaxDarken = 0.10; // max fractional darkening at full opacity+mask
        vec2 dirtUv = vPosition * 0.5 + 0.5; // fixed to screen space, not world space, so it reads as "on the lens" rather than moving with the scene
        float dirtNoiseA = texture(uNoiseTex, dirtUv * 5.0).r;
        float dirtNoiseB = texture(uNoiseTex, dirtUv * 11.0 + 31.7).r;
        float dirtMask = dirtNoiseA * 0.6 + dirtNoiseB * 0.4;
        finalColor *= (1.0 - dirtMask * uLensDirtOpacity * kLensDirtMaxDarken);
    }

    fragColor = vec4(finalColor, 1.0);
}
