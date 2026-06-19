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

    // 2. Sun setup
    vec2 uv = vec2(vPosition.x * uAspectRatio, vPosition.y);
    
    float dist = distance(uv, uSunPos * uAspectRatio);
    
    // Pulsating radius based on uSunSize and uSunSpeed
    float pulse = sin(uTime * uSunSpeed) * 0.02 * (uSunSize / 0.20);
    float radius = uSunSize + pulse;
    
    // Smooth sun edge
    float sunMask = smoothstep(radius, radius - 0.008, dist);
    
    // Soft outer glow extending further
    float glowMask = smoothstep(radius * 3.5, radius, dist);
    vec3 finalGlow = sunGlowColor * (glowMask * 0.65);

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
