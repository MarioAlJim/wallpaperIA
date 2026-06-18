#version 300 es
precision mediump float;

in vec2 vPosition;
out vec4 fragColor;

uniform float uTime;
uniform float uAspectRatio;
uniform float uSunSize; // 0.1 to 0.35
uniform float uSunSpeed; // speed coefficient
uniform int uTheme; // 0: Noon Blue, 1: Sunset Orange, 2: Purple Dusk

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
    } else { // Purple Dusk (Theme 2)
        skyTop = vec3(0.15, 0.1, 0.35);       // Violet
        skyBottom = vec3(0.9, 0.5, 0.55);     // Peach/pink
        sunColor = vec3(1.0, 0.9, 0.95);      // White pink
        sunGlowColor = vec3(0.8, 0.3, 0.6);   // Magenta
    }

    vec3 skyColor = mix(skyBottom, skyTop, tSky);

    // 2. Sun setup
    vec2 sunPos = vec2(0.35, 0.45);
    vec2 uv = vec2(vPosition.x * uAspectRatio, vPosition.y);
    
    float dist = distance(uv, sunPos * uAspectRatio);
    
    // Pulsating radius based on uSunSize and uSunSpeed
    float pulse = sin(uTime * uSunSpeed) * 0.02 * (uSunSize / 0.20);
    float radius = uSunSize + pulse;
    
    // Smooth sun edge
    float sunMask = smoothstep(radius, radius - 0.008, dist);
    
    // Soft outer glow extending further
    float glowMask = smoothstep(radius * 3.5, radius, dist);
    vec3 finalGlow = sunGlowColor * (glowMask * 0.65);

    // Combine
    vec3 finalColor = mix(skyColor, sunColor, sunMask) + finalGlow * (1.0 - sunMask);
    
    fragColor = vec4(finalColor, 1.0);
}
