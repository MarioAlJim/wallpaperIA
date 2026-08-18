#version 300 es

precision mediump float;

in vec2 vTexCoord;

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

    // Early discard: every ellipse used to build the cloud body/shadow (c1..c8 with
    // r1..r8, including their random growth factors and the shadow shift/grow) stays
    // within a known maximum extent along each axis. A fragment further out than that
    // (plus a safety margin for the soft edge falloff and the silver-lining bloom)
    // can never contribute to the final color, so bail out before the ellipse/fbm math.
    // Widest ellipse pair on X is c2/c3 (center ~0.425, radius ~0.3625*1.15+0.0125 shadow
    // grow ~0.43) -> extent ~0.855. Tallest on Y is c4 (center ~0.2275, radius ~0.325*1.15
    // +0.0125 grow -0.0625 shadow shift ~0.30) -> extent ~0.52.
    const vec2 kMaxCloudExtent = vec2(0.855, 0.52) * 1.15;
    vec2 pn = p / kMaxCloudExtent;
    if (dot(pn, pn) > 1.0) {
        discard;
    }

    // Seed based on the variation uniform
    float seed = uVariation;

    // --- Weights to dynamically enable/disable ellipses (varying from 1 to 4 ellipses) ---
    float randVal = hash(seed + 99.0);
    float w1 = 1.0;                            // Siempre activo (Cuerpo Principal)
    float w2 = randVal > 0.25 ? 1.0 : 0.0;     // Activo para >= 2 elipses
    float w3 = randVal > 0.50 ? 1.0 : 0.0;     // Activo para >= 3 elipses
    float w4 = randVal > 0.75 ? 1.0 : 0.0;     // Activo para 4 elipses

    // --- Dynamic variations based on uVariation ---
    // Ellipse 1 (Main body - E1)
    vec2 c1 = vec2((hash(seed + 1.0) - 0.5) * 0.08, (hash(seed + 2.0) - 0.5) * 0.04);
    vec2 r1 = vec2(0.42 + 0.16 * hash(seed + 3.0), 0.24 + 0.08 * hash(seed + 4.0));

    // Ellipse 2 (Left body - E2)
    vec2 c2 = vec2(c1.x - r1.x * (0.45 + 0.25 * hash(seed + 5.0)), c1.y - 0.05 + 0.05 * hash(seed + 6.0));
    vec2 r2 = r1 * (0.55 + 0.35 * hash(seed + 7.0));

    // Ellipse 3 (Right body - E3)
    vec2 c3 = vec2(c1.x + r1.x * (0.45 + 0.25 * hash(seed + 8.0)), c1.y - 0.05 + 0.05 * hash(seed + 9.0));
    vec2 r3 = r1 * (0.55 + 0.35 * hash(seed + 10.0));

    // Ellipse 4 (Top dome - E4)
    vec2 c4 = vec2(c1.x + (hash(seed + 11.0) - 0.5) * r1.x * 0.5, c1.y + r1.y * (0.35 + 0.35 * hash(seed + 12.0)));
    vec2 r4 = r1 * (0.45 + 0.35 * hash(seed + 13.0));

    // --- Body Ellipses ---
    float bodyAlpha = 0.0;
    bodyAlpha = max(bodyAlpha, w1 * ellipseAlpha(p, c1, r1));
    bodyAlpha = max(bodyAlpha, w2 * ellipseAlpha(p, c2, r2));
    bodyAlpha = max(bodyAlpha, w3 * ellipseAlpha(p, c3, r3));
    bodyAlpha = max(bodyAlpha, w4 * ellipseAlpha(p, c4, r4));

    // --- Shadow Ellipses ---
    // The shadow is shifted by vec2(0.0, -0.0625) and is slightly larger by vec2(0.0125, 0.0125)
    vec2 sShift = vec2(0.0, -0.0625);
    vec2 sGrow = vec2(0.0125, 0.0125);

    float shadowAlpha = 0.0;
    shadowAlpha = max(shadowAlpha, w1 * ellipseAlpha(p, c1 + sShift, r1 + sGrow));
    shadowAlpha = max(shadowAlpha, w2 * ellipseAlpha(p, c2 + sShift, r2 + sGrow));
    shadowAlpha = max(shadowAlpha, w3 * ellipseAlpha(p, c3 + sShift, r3 + sGrow));
    shadowAlpha = max(shadowAlpha, w4 * ellipseAlpha(p, c4 + sShift, r4 + sGrow));

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
