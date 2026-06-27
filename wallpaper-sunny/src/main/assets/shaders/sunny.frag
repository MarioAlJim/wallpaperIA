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

// Pseudo-random noise for procedural nebula
float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    // 1. Sky Gradient based on Theme
    float tSky = (vPosition.y + 1.0) * 0.5; // ranges 0.0 to 1.0
    
    vec3 skyTop;
    vec3 skyBottom;
    vec3 sunColor;
    vec3 sunGlowColor;

    if (uTheme == 0) { // Noon Blue
        skyTop = vec3(0.05, 0.4, 0.85);      // Deep blue
        skyBottom = vec3(0.55, 0.8, 0.95);    // Light sky blue
        sunColor = vec3(1.0, 1.0, 0.9);       // Yellow-white
        sunGlowColor = vec3(1.0, 0.7, 0.2);   // Golden
    } else if (uTheme == 1) { // Sunset Orange
        skyTop = vec3(0.1, 0.05, 0.25);       // Dark indigo
        skyBottom = vec3(0.95, 0.45, 0.1);    // Bright orange
        sunColor = vec3(1.0, 0.95, 0.7);      // Warm yellow-white
        sunGlowColor = vec3(0.9, 0.2, 0.05);  // Deep red-orange
    } else if (uTheme == 2) { // Purple Dusk (Theme 2)
        skyTop = vec3(0.15, 0.1, 0.35);       // Violet
        skyBottom = vec3(0.9, 0.5, 0.55);     // Peach/pink
        sunColor = vec3(1.0, 0.9, 0.95);      // White pink
        sunGlowColor = vec3(0.8, 0.3, 0.6);   // Magenta
    } else { // Custom (Theme 3)
        skyTop = uSkyTop;
        skyBottom = uSkyBottom;
        sunColor = vec3(1.0, 0.98, 0.9);       // Warm white-yellow
        sunGlowColor = mix(skyBottom, vec3(1.0, 0.7, 0.3), 0.5); // Natural golden glow
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
        
        float nebulaNoiseVal = fbm(nebulaUV);
        float nebulaMask = smoothstep(0.35, 0.75, nebulaNoiseVal);
        
        // Deep purple-magenta colors for nebulae
        vec3 nebulaColor1 = vec3(0.16, 0.03, 0.28); // Dark purple
        vec3 nebulaColor2 = vec3(0.08, 0.02, 0.18); // Dark violet
        vec3 mixedNebula = mix(nebulaColor2, nebulaColor1, smoothstep(0.45, 0.70, nebulaNoiseVal));
        
        // Blend nebulae sutilmente (subtly, max 0.22 opacity) into night sky color
        nightSkyColor = mix(nightSkyColor, mixedNebula, nebulaMask * 0.22);

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

    // Dynamic God Rays (Crepuscular Rays)
    if (uGodRaysIntensity > 0.0) {
        float angle = atan(uv.y - uSunPos.y * uAspectRatio, uv.x - uSunPos.x * uAspectRatio);
        float rays = sin(angle * 12.0 + uTime * 0.15) * 0.5 + 0.5;
        rays += sin(angle * 27.0 - uTime * 0.25) * 0.3 + 0.3;
        rays += sin(angle * 53.0 + uTime * 0.4) * 0.15 + 0.15;
        rays += sin(angle * 89.0 - uTime * 0.6) * 0.05 + 0.05;
        rays = rays / 2.0; // Normalize [0, 1]

        float rayAttenuation = exp(-dist * 0.8);
        finalGlow += sunGlowColor * rays * rayAttenuation * uGodRaysIntensity * 1.5;
    }

    // Combine
    vec3 finalColor = mix(skyColor, sunColor, sunMask * uSunFadeFactor) + finalGlow * uSunFadeFactor * (1.0 - sunMask * uSunFadeFactor);
    
    fragColor = vec4(finalColor, 1.0);
}
