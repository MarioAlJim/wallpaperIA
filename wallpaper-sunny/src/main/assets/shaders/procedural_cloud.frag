#version 300 es

precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture; // Declared for compatibility, not used
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;
uniform float uVariation; // Seed for cloud shape variation
uniform vec2 uMoonPos;
uniform vec2 uCloudPos;
uniform float uCloudScale;
uniform float uSilverLiningIntensity; // 0.0 for day, > 0.0 for night

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float ellipseAlpha(vec2 p, vec2 center, vec2 r) {
    vec2 diff = p - center;
    float dist = length(diff / r);
    // Smooth edge transition for anti-aliasing (soft fuzzy edge)
    return smoothstep(1.01, 0.99, dist);
}

void main() {
    // Normalise texture coordinates to a centered, aspect-ratio-corrected grid.
    // vTexCoord ranges from [0, 1] in both axes.
    // Invert the Y axis (0.5 - vTexCoord.y) to correct the vertical orientation of the cloud shape.
    vec2 p = vec2((vTexCoord.x - 0.5) * 2.4, 0.5 - vTexCoord.y);

    // Seed based on the variation uniform
    float seed = uVariation;

    // --- Weights to dynamically enable/disable ellipses (adding more or fewer ellipses) ---
    float w1 = 1.0; // Main body (always present)
    float w2 = 1.0; // Left body (always present)
    float w3 = 1.0; // Right body (always present)
    
    // Ellipses 4 & 5 are optional (present in ~70% and ~65% of clouds respectively)
    float w4 = hash(seed + 40.0) > 0.3 ? 1.0 : 0.0;
    float w5 = hash(seed + 50.0) > 0.35 ? 1.0 : 0.0;
    
    // Ellipses 6, 7 & 8 are extra optional domes to add complexity to some clouds
    float w6 = hash(seed + 60.0) > 0.6 ? 1.0 : 0.0; // Extra far-left dome (~40% chance)
    float w7 = hash(seed + 70.0) > 0.6 ? 1.0 : 0.0; // Extra far-right dome (~40% chance)
    float w8 = hash(seed + 80.0) > 0.7 ? 1.0 : 0.0; // Extra upper-center dome (~30% chance)

    // --- Dynamic variations based on uVariation ---
    // Ellipse 1 (Main body)
    vec2 c1 = vec2(0.0, 0.0);
    vec2 r1 = vec2(0.5125, 0.3125) * (0.9 + 0.2 * hash(seed + 1.15));

    // Ellipse 2 (Left)
    vec2 c2 = vec2(-0.375 - 0.05 * hash(seed + 2.25), -0.0875 + 0.04 * hash(seed + 3.35));
    vec2 r2 = vec2(0.3625, 0.25) * (0.85 + 0.3 * hash(seed + 4.45));

    // Ellipse 3 (Right)
    vec2 c3 = vec2(0.375 + 0.05 * hash(seed + 5.55), -0.0875 + 0.04 * hash(seed + 6.65));
    vec2 r3 = vec2(0.3625, 0.25) * (0.85 + 0.3 * hash(seed + 7.75));

    // Ellipse 4 (Top-Left)
    vec2 c4 = vec2(-0.1875 - 0.04 * hash(seed + 8.85), 0.1875 + 0.04 * hash(seed + 9.95));
    vec2 r4 = vec2(0.325, 0.25) * (0.85 + 0.3 * hash(seed + 10.05));

    // Ellipse 5 (Top-Right)
    vec2 c5 = vec2(0.1875 + 0.04 * hash(seed + 11.15), 0.15 + 0.04 * hash(seed + 12.25));
    vec2 r5 = vec2(0.30, 0.23125) * (0.85 + 0.3 * hash(seed + 13.35));

    // Ellipse 6 (Extra far-left dome)
    vec2 c6 = vec2(-0.45 - 0.03 * hash(seed + 61.15), -0.05 + 0.03 * hash(seed + 62.25));
    vec2 r6 = vec2(0.24, 0.18) * (0.8 + 0.3 * hash(seed + 63.35));

    // Ellipse 7 (Extra far-right dome)
    vec2 c7 = vec2(0.45 + 0.03 * hash(seed + 71.15), -0.05 + 0.03 * hash(seed + 72.25));
    vec2 r7 = vec2(0.24, 0.18) * (0.8 + 0.3 * hash(seed + 73.35));

    // Ellipse 8 (Extra upper-center dome)
    vec2 c8 = vec2(0.0 + 0.05 * (hash(seed + 81.15) - 0.5), 0.22 + 0.03 * hash(seed + 82.25));
    vec2 r8 = vec2(0.28, 0.20) * (0.8 + 0.3 * hash(seed + 83.35));

    // --- Body Ellipses ---
    float bodyAlpha = 0.0;
    bodyAlpha = max(bodyAlpha, w1 * ellipseAlpha(p, c1, r1));
    bodyAlpha = max(bodyAlpha, w2 * ellipseAlpha(p, c2, r2));
    bodyAlpha = max(bodyAlpha, w3 * ellipseAlpha(p, c3, r3));
    bodyAlpha = max(bodyAlpha, w4 * ellipseAlpha(p, c4, r4));
    bodyAlpha = max(bodyAlpha, w5 * ellipseAlpha(p, c5, r5));
    bodyAlpha = max(bodyAlpha, w6 * ellipseAlpha(p, c6, r6));
    bodyAlpha = max(bodyAlpha, w7 * ellipseAlpha(p, c7, r7));
    bodyAlpha = max(bodyAlpha, w8 * ellipseAlpha(p, c8, r8));

    // --- Shadow Ellipses ---
    // The shadow is shifted by vec2(0.0, -0.0625) and is slightly larger by vec2(0.0125, 0.0125)
    vec2 sShift = vec2(0.0, -0.0625);
    vec2 sGrow = vec2(0.0125, 0.0125);

    float shadowAlpha = 0.0;
    shadowAlpha = max(shadowAlpha, w1 * ellipseAlpha(p, c1 + sShift, r1 + sGrow));
    shadowAlpha = max(shadowAlpha, w2 * ellipseAlpha(p, c2 + sShift, r2 + sGrow));
    shadowAlpha = max(shadowAlpha, w3 * ellipseAlpha(p, c3 + sShift, r3 + sGrow));
    shadowAlpha = max(shadowAlpha, w4 * ellipseAlpha(p, c4 + sShift, r4 + sGrow));
    shadowAlpha = max(shadowAlpha, w5 * ellipseAlpha(p, c5 + sShift, r5 + sGrow));
    shadowAlpha = max(shadowAlpha, w6 * ellipseAlpha(p, c6 + sShift, r6 + sGrow));
    shadowAlpha = max(shadowAlpha, w7 * ellipseAlpha(p, c7 + sShift, r7 + sGrow));
    shadowAlpha = max(shadowAlpha, w8 * ellipseAlpha(p, c8 + sShift, r8 + sGrow));

    // --- Color Blending ---
    // Shadow color in JS: rgb(160, 195, 230). Tinted by cloud color to adapt to night/day transitions.
    vec3 baseShadowColor = vec3(160.0 / 255.0, 195.0 / 255.0, 230.0 / 255.0);
    vec3 shadowColor = uCloudColor * baseShadowColor;
    vec3 bodyColor = uCloudColor;

    // Apply flash intensity (lightning effect) if present
    vec3 flashLight = bodyColor * uFlashColor * uFlashIntensity * 1.5;
    bodyColor += flashLight;

    // Calculate opacities
    float sA = shadowAlpha * (uOpacity * 0.45);
    float bA = bodyAlpha * uOpacity;

    // Normal alpha blending: body on top of shadow
    float combinedAlpha = bA + sA * (1.0 - bA);
    vec3 mixedColor = mix(shadowColor, bodyColor, bA / max(combinedAlpha, 0.001));

    // --- Silver Lining (Moonlight edge glow) ---
    if (uSilverLiningIntensity > 0.001) {
        // Calculate the world position of the current pixel
        vec2 pixelWorldPos = uCloudPos + p * uCloudScale;
        
        // Distance from this pixel to the moon
        float distToMoon = distance(pixelWorldPos, uMoonPos);
        
        // Moon influence: starts glowing at distance 0.75, peaks at 0.20
        float moonInfluence = smoothstep(0.75, 0.20, distToMoon);
        
        // Bell-shaped curve around the cloud body edge (peaks at bodyAlpha = 0.25)
        float edge = smoothstep(0.01, 0.25, bodyAlpha) * (1.0 - smoothstep(0.25, 0.95, bodyAlpha));
        
        float liningStrength = edge * moonInfluence * uSilverLiningIntensity;
        
        if (liningStrength > 0.0) {
            // Bright white/silver light (rgb 0.95, 0.98, 1.0)
            vec3 silverLiningColor = vec3(0.95, 0.98, 1.0);
            
            // Mix the silver lining color onto the edge of the cloud
            mixedColor = mix(mixedColor, silverLiningColor, liningStrength * 0.95);
            
            // Ensure the glowing edge is opaque enough to stand out
            combinedAlpha = max(combinedAlpha, liningStrength * uOpacity);
        }
    }

    fragColor = vec4(mixedColor, combinedAlpha);
}
