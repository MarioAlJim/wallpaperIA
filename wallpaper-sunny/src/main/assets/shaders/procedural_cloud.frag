#version 300 es

precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture; // Declared for compatibility, not used
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;
uniform float uVariation; // Seed for cloud shape variation

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

    // --- Body Ellipses ---
    float bodyAlpha = 0.0;
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, c1, r1));
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, c2, r2));
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, c3, r3));
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, c4, r4));
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, c5, r5));

    // --- Shadow Ellipses ---
    // The shadow is shifted by vec2(0.0, -0.0625) and is slightly larger by vec2(0.0125, 0.0125)
    vec2 sShift = vec2(0.0, -0.0625);
    vec2 sGrow = vec2(0.0125, 0.0125);

    float shadowAlpha = 0.0;
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, c1 + sShift, r1 + sGrow));
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, c2 + sShift, r2 + sGrow));
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, c3 + sShift, r3 + sGrow));
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, c4 + sShift, r4 + sGrow));
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, c5 + sShift, r5 + sGrow));

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

    fragColor = vec4(mixedColor, combinedAlpha);
}
