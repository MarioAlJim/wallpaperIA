#version 300 es

precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture; // Declared for compatibility, not used
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;

out vec4 fragColor;

float ellipseAlpha(vec2 p, vec2 center, vec2 r) {
    vec2 diff = p - center;
    float dist = length(diff / r);
    // Smooth edge transition for anti-aliasing (soft fuzzy edge)
    return smoothstep(1.01, 0.99, dist);
}

void main() {
    // Normalise texture coordinates to a centered, aspect-ratio-corrected grid.
    // vTexCoord ranges from [0, 1] in both axes.
    // Since the quad is scaled by 2.4 horizontally relative to vertically, 
    // we multiply the X coordinate by 2.4 to get a 1:1 isotropic grid.
    vec2 p = vec2((vTexCoord.x - 0.5) * 2.4, vTexCoord.y - 0.5);

    // --- Body Ellipses ---
    float bodyAlpha = 0.0;
    // 1. Main body
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, vec2(0.0, 0.0), vec2(0.5125, 0.3125)));
    // 2. Left
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, vec2(-0.375, -0.0875), vec2(0.3625, 0.25)));
    // 3. Right
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, vec2(0.375, -0.0875), vec2(0.3625, 0.25)));
    // 4. Top-Left
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, vec2(-0.1875, 0.1875), vec2(0.325, 0.25)));
    // 5. Top-Right
    bodyAlpha = max(bodyAlpha, ellipseAlpha(p, vec2(0.1875, 0.15), vec2(0.30, 0.23125)));

    // --- Shadow Ellipses ---
    float shadowAlpha = 0.0;
    // 1. Main shadow
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, vec2(0.0, -0.0625), vec2(0.525, 0.325)));
    // 2. Left shadow
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, vec2(-0.375, -0.15), vec2(0.375, 0.2625)));
    // 3. Right shadow
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, vec2(0.375, -0.15), vec2(0.375, 0.2625)));
    // 4. Top-Left shadow
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, vec2(-0.1875, 0.0625), vec2(0.3375, 0.2625)));
    // 5. Top-Right shadow
    shadowAlpha = max(shadowAlpha, ellipseAlpha(p, vec2(0.1875, 0.025), vec2(0.3125, 0.24375)));

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
