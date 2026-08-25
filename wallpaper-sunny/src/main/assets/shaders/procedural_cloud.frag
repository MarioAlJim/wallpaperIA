#version 300 es

precision mediump float;

in vec2 vTexCoord;
in float vOpacity;
in float vVariation;
in vec2 vCloudPos;
in float vCloudScale;

uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;
uniform vec2 uMoonPos;
uniform float uSilverLiningIntensity; // 0.0 for day, > 0.0 for night
uniform vec2 uSunPos;
uniform float uSunBloomIntensity;

out vec4 fragColor;

// sin()-based hash needs highp: at mediump precision the seed (uVariation can be in the
// hundreds, see Cloud.shapeSeed) loses enough precision inside sin() that the result stops
// being uniformly distributed - it skews toward low values instead. That was invisible while
// w2/w3 were unconditionally 1.0 (pre-4-ellipse-composition shader), but the 1/2/3/4-ellipse
// split below reads a single hash() against three hard cutoffs (0.25/0.50/0.75), so any skew
// shows up directly as "almost every cloud lands in the first bucket" - i.e. lone ellipses.
highp float hash(highp float n) {
    return fract(sin(n) * 43758.5453123);
}

float ellipseAlpha(vec2 p, vec2 center, vec2 r) {
    vec2 diff = p - center;
    float dist = length(diff / r);
    // Smooth edge transition for anti-aliasing (soft fuzzy edge)
    return smoothstep(1.01, 0.99, dist);
}

float softEllipseAlpha(vec2 p, vec2 center, vec2 r, float grow) {
    vec2 diff = p - center;
    float dist = length(diff / (r + grow));
    // Soft blend transition extending outward
    return smoothstep(1.0, 0.0, dist);
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
    // Widest ellipse pair on X is c2/c3 (extent ~0.855), updated to support c5/c6 (extent ~1.15).
    // Tallest on Y is c4 (extent ~0.52).
    const vec2 kMaxCloudExtent = vec2(1.15, 0.52) * 1.30;
    vec2 pn = p / kMaxCloudExtent;
    if (dot(pn, pn) > 1.0) {
        discard;
    }

    // Seed based on the variation uniform
    highp float seed = vVariation;

    // --- Weights to dynamically enable/disable ellipses (varying from 1 to 8 ellipses) ---
    float randVal = hash(seed + 99.0);
    float w1 = 1.0;                            // Siempre activo (Cuerpo Principal)
    float w2 = randVal > 0.125 ? 1.0 : 0.0;    // Activo para >= 2 elipses
    float w3 = randVal > 0.250 ? 1.0 : 0.0;    // Activo para >= 3 elipses
    float w4 = randVal > 0.375 ? 1.0 : 0.0;    // Activo para >= 4 elipses
    float w5 = randVal > 0.500 ? 1.0 : 0.0;    // Activo para >= 5 elipses
    float w6 = randVal > 0.625 ? 1.0 : 0.0;    // Activo para >= 6 elipses
    float w7 = randVal > 0.750 ? 1.0 : 0.0;    // Activo para >= 7 elipses
    float w8 = randVal > 0.875 ? 1.0 : 0.0;    // Activo para 8 elipses

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

    // Ellipse 5 (Left lower puff - E5)
    vec2 c5 = vec2(c2.x - r2.x * (0.3 + 0.2 * hash(seed + 14.0)), c2.y - r2.y * (0.2 + 0.2 * hash(seed + 15.0)));
    vec2 r5 = r2 * (0.6 + 0.3 * hash(seed + 16.0));

    // Ellipse 6 (Right lower puff - E6)
    vec2 c6 = vec2(c3.x + r3.x * (0.3 + 0.2 * hash(seed + 17.0)), c3.y - r3.y * (0.2 + 0.2 * hash(seed + 18.0)));
    vec2 r6 = r3 * (0.6 + 0.3 * hash(seed + 19.0));

    // Ellipse 7 (Middle-left top dome - E7)
    vec2 c7 = vec2(c2.x + r2.x * 0.2, c2.y + r2.y * (0.5 + 0.3 * hash(seed + 20.0)));
    vec2 r7 = r2 * (0.7 + 0.2 * hash(seed + 21.0));

    // Ellipse 8 (Middle-right top dome - E8)
    vec2 c8 = vec2(c3.x - r3.x * 0.2, c3.y + r3.y * (0.5 + 0.3 * hash(seed + 22.0)));
    vec2 r8 = r3 * (0.7 + 0.2 * hash(seed + 23.0));

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

    // --- Soft Body Ellipses for Sun Bloom (Light Bleeding) ---
    float softBodyAlpha = 0.0;
    if (uSunBloomIntensity > 0.001) {
        float grow = 0.12;
        softBodyAlpha = max(softBodyAlpha, w1 * softEllipseAlpha(p, c1, r1, grow));
        softBodyAlpha = max(softBodyAlpha, w2 * softEllipseAlpha(p, c2, r2, grow));
        softBodyAlpha = max(softBodyAlpha, w3 * softEllipseAlpha(p, c3, r3, grow));
        softBodyAlpha = max(softBodyAlpha, w4 * softEllipseAlpha(p, c4, r4, grow));
        softBodyAlpha = max(softBodyAlpha, w5 * softEllipseAlpha(p, c5, r5, grow));
        softBodyAlpha = max(softBodyAlpha, w6 * softEllipseAlpha(p, c6, r6, grow));
        softBodyAlpha = max(softBodyAlpha, w7 * softEllipseAlpha(p, c7, r7, grow));
        softBodyAlpha = max(softBodyAlpha, w8 * softEllipseAlpha(p, c8, r8, grow));
    }

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
    float sA = shadowAlpha * (vOpacity * 0.45);
    float bA = bodyAlpha * vOpacity;

    // Normal alpha blending: body on top of shadow
    float combinedAlpha = bA + sA * (1.0 - bA);
    vec3 mixedColor = mix(shadowColor, bodyColor, bA / max(combinedAlpha, 0.001));

    // --- Silver Lining (Moonlight edge glow) ---
    if (uSilverLiningIntensity > 0.001) {
        // Calculate the world position of the current pixel
        vec2 pixelWorldPos = vCloudPos + p * vCloudScale;
        
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
            combinedAlpha = max(combinedAlpha, liningStrength * vOpacity);
        }
    }

    // --- Sun Bloom (Light Bleeding) ---
    if (uSunBloomIntensity > 0.001) {
        // Calculate the world position of the current pixel
        vec2 pixelWorldPos = vCloudPos + p * vCloudScale;
        
        // Distance from this pixel to the sun
        float distToSun = distance(pixelWorldPos, uSunPos);
        
        // Sun influence: peaks when close to the sun (starts at 0.65, peaks at 0.15)
        float sunInfluence = smoothstep(0.65, 0.15, distToSun);
        
        if (sunInfluence > 0.001) {
            // Bleed outside the cloud: soft body alpha where the cloud body is transparent/thin
            float outsideBleed = softBodyAlpha * (1.0 - bodyAlpha);
            
            // Bleed inside the cloud: wash out the edges
            float insideBleed = bodyAlpha * (1.0 - bodyAlpha * bodyAlpha);
            
            float bleedStrength = max(outsideBleed * 0.85, insideBleed) * sunInfluence * uSunBloomIntensity;
            
            if (bleedStrength > 0.0) {
                // Bright warm gold/white light for sun bloom
                vec3 sunBloomColor = vec3(1.0, 0.96, 0.85);
                
                // Mix the sun bloom color onto the cloud
                mixedColor = mix(mixedColor, sunBloomColor, bleedStrength * 0.90);
                
                // Boost alpha of the bleed region so the glow is visible on the sky
                combinedAlpha = max(combinedAlpha, bleedStrength * vOpacity);
            }
        }
    }

    fragColor = vec4(mixedColor, combinedAlpha);
}
