#version 300 es
// Default precision is mediump: color/blend/opacity math dominates this shader and
// runs faster (and uses fewer registers) at mediump on mobile GPUs. Anything that
// derives a texture-sampling coordinate (including the droplet grid cell math, whose
// jitter would show up as swimming droplets) is explicitly qualified highp below.
precision mediump float;

in highp vec2 vTexCoord;
in highp vec2 vScreenPos; // Screen coordinates from vertex shader

uniform sampler2D uTexture;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform highp float uTime;
uniform highp float uAspectRatio;

// New uniforms for configurable screen droplets
uniform float uScreenDropletsEnabled;
uniform float uScreenDropletsSize;
uniform float uRainIntensity;
uniform vec3 uRainColor;

// New uniform for lightning position (Dark God Rays and Rim Lighting)
uniform highp vec2 uLightningPos;

uniform vec2 uTexCoordScale;
uniform float uBgBrightness;

// Small tileable grid of prebaked random values used by noise()/fbm() for the
// Dark God Rays shadowing. Sampling a texture (hardware-filtered) replaces the
// per-fragment sin()/fract() hash math previously used to compute value noise.
uniform sampler2D uNoiseTexture;
const float NOISE_TEX_SIZE = 64.0;

out vec4 fragColor;

// Sample the prebaked noise texture to get a pseudo-random value between 0.0 and 1.0,
// replacing the expensive per-pixel Hash math with cached hardware texture lookups.
float Hash(highp vec2 p) {
    return texture(uNoiseTexture, (p + vec2(0.5)) / NOISE_TEX_SIZE).r;
}

float noise(highp vec2 p) {
    // GL_REPEAT wrapping tiles this every NOISE_TEX_SIZE units, and GL_LINEAR
    // filtering reproduces the same bilinear blend the old hash-based noise() did.
    return texture(uNoiseTexture, p / NOISE_TEX_SIZE).r;
}

float fbm(highp vec2 p) {
    float value = 0.5 * noise(p);
    value += 0.25 * noise(p * 2.0);
    value += 0.125 * noise(p * 4.0);
    return value;
}

// Marks which on-screen fragments fall inside a droplet's silhouette (droplet.w, used by
// main() below to tint them toward the rain color) and how strongly (fadeVal), plus a "lupa"
// glow amount (droplet.z, see below). There is no refraction/distortion here anymore — an
// earlier version's lens effect is gone entirely, so distortion.xy is always (0,0) and nothing
// here ever displaces what's sampled from the background texture.
//
// The magnifying-glass ("lupa") look is instead faked purely with color: a soft brightening
// toward the droplet's center plus a small bright glint offset toward one side, the way light
// catches a curved glass surface — no UV sampling is involved, so it can't be mistaken for
// real refraction.
//
// Shape: an asymmetric teardrop. The leading half (the direction of fall) is a fixed rounded
// cap the size of the droplet's own radius; the trailing half (where it came from) starts
// flush with that cap — i.e. the droplet begins life as a plain semicircle — and gradually
// grows a tail as it falls, reaching 100% further than the radius by the end of its life.
//
// Lifecycle: droplets never cut off abruptly. Near the end of their life, radius and opacity
// both ease down together (see `shrink` below) so a droplet visibly shrinks and fades out at
// the same time instead of vanishing at a hard cutoff.
vec4 GetDropletEffects(highp vec2 uv, highp float time, highp float aspect, float sizeFactor, vec3 rainColor, out float borderVal, out float fadeVal) {
    // 12 lanes tall on screen regardless of aspect ratio. Grid-cell math (st/ip/fp/localPos)
    // stays highp: at mediump, this jitters visibly as the droplet grid "swims".
    highp vec2 scale = vec2(12.0 * aspect, 12.0);
    highp vec2 st = uv * scale;
    highp vec2 ip = floor(st);
    highp vec2 fp = fract(st) - vec2(0.5, 0.5);

    float totalFade = 0.0;
    float totalGlow = 0.0;
    float sumWeight = 0.0;
    highp vec2 totalDistortion = vec2(0.0);

    // Determine the light direction for specular reflections and lens glow.
    // Default light is top-left (classic desktop shading)
    highp vec2 activeLightDir = normalize(vec2(-0.5, 0.5));
    if (uFlashIntensity > 0.05) {
        // Shift light direction towards the lightning position during a flash
        highp vec2 lightVec = uLightningPos - vScreenPos;
        if (length(lightVec) > 0.001) {
            activeLightDir = normalize(lightVec);
        }
    }

    // Droplets only ever move straight down (never sideways), but a lane's horizontal jitter
    // (laneOffsetX below) plus a large enough radius can still push a droplet's footprint past
    // its own column's half-width — and each column hashes to a fully independent lane, so a
    // fragment in the next column over won't render any part of a neighbor's droplet unless we
    // explicitly check that neighboring column too. Skipping that (checking x=0 only) is what
    // was clipping wide/offset droplets to a hard vertical cut at the column boundary, so the
    // 3 nearest columns are checked for every row instead.
    // We also check one row below (y = 1) to prevent the top half of spawning droplets from
    // being clipped by the cell boundary at t=0, making them appear instantaneously.
    for (int y = -1; y <= 9; y++) {
        for (int x = -1; x <= 1; x++) {
            highp vec2 neighbor = vec2(float(x), float(y));
            highp vec2 cell = ip + neighbor;
            float h_cell = Hash(cell);

            // Droplets density scales proportionally with rain intensity (up to 45% active lanes at maximum intensity)
            float threshold = 1.0 - (0.45 * uRainIntensity);
            if (h_cell < threshold) continue;

            // This lane's fall distance and speed are fixed for its whole lifetime; only its
            // vertical position (via t below) changes from one frame to the next. Kept highp:
            // fallTime accumulates uTime (unbounded over hours of wallpaper uptime), so mediump
            // would eventually make the fall animation glitch. The extra / 2.0 doubles each
            // droplet's on-screen lifetime (half the per-second progression rate = twice as long
            // to fall the same fallDist) without changing anything else about its motion.
            float fallDist = mix(3.0, 8.0, Hash(cell + vec2(12.3, 45.6)));
            float speed = mix(0.12, 0.4, Hash(cell + vec2(4.4, 4.4))) / fallDist / 2.0;

            highp float fallTime = time * speed + h_cell * 20.0;
            highp float loopId = floor(fallTime);
            highp float t = fract(fallTime);
            float wrappedLoopId = mod(loopId, 16.0);

            // Small per-spawn horizontal jitter so lanes don't all sit dead-center in their
            // column. Fixed for the whole fall (doesn't change with t), so it can't read as
            // sideways drift — only the vertical position below does.
            float laneOffsetX = (Hash(cell + vec2(wrappedLoopId * 7.7, wrappedLoopId * 7.7)) - 0.5) * 0.5;

            float baseRadius = mix(0.045, 0.10, Hash(cell + vec2(wrappedLoopId * 2.3, wrappedLoopId * 2.3))) * sizeFactor;
            if (baseRadius <= 0.001) continue;

            // No fade-in: a droplet appears instantly at full strength, already shaped as a
            // semicircle (see the trailing-edge math below, which starts flush with the leading
            // cap at t=0) — a gradual fade-in made it look like it was materializing out of its
            // spawn row's grid line instead of just popping into existence. Shrink starts
            // immediately at that same instant, easing continuously toward 0 across the whole
            // fall, so the droplet is visibly getting smaller the entire time it's moving, not
            // just at the end — and fade tracks that same curve so opacity and size shrink
            // together, never popping out of existence either.
            float shrink = 1.0 - smoothstep(0.0, 1.0, t);
            float fade = shrink;
            if (fade <= 0.001) continue;

            float radius = baseRadius * shrink;
            if (radius <= 0.001) continue;

            // Improved stretching effect: the leading edge shrinks from baseRadius to 0.
            // The trailing edge (tail) starts flush with the leading cap (forming a semicircle at t=0)
            // and stretches up to 100% more than the initial radius (baseRadius * 2.0) as it falls.
            float stretchT = smoothstep(0.0, 1.0, t);
            float trailingReach = mix(radius, baseRadius * 2.0, stretchT);

            // Position relative to the droplet's current center: starts at the top of its
            // spawn cell and moves straight down by fallDist cells as t goes 0 -> 1.
            highp vec2 localPos = fp - neighbor - vec2(laneOffsetX, 0.0);
            localPos.y += (t * fallDist - 0.5);

            // Bounding box check (pruning): wider on the trailing (positive-y) side to cover
            // the tail's maximum reach (up to trailingReach), plus a margin so the tail's soft
            // edge (edgeFade below) is never culled before it has faded out.
            if (abs(localPos.x) > radius * 1.3 || localPos.y < -radius * 1.3 || localPos.y > trailingReach * 1.1) continue;

            float yComp = localPos.y;
            if (yComp > 0.0) {
                yComp *= radius / trailingReach;
            }
            highp vec2 shapePos = vec2(localPos.x * 1.2, yComp);
            float dist = length(shapePos);
            if (dist >= radius) continue;

            float normDist = dist / radius;

            // Softens only the silhouette's edge (not a color/darkening effect) so it isn't a
            // hard-edged shape.
            float edgeFade = 1.0 - smoothstep(0.82, 1.0, normDist);
            float weight = fade * edgeFade;

            // Shading calculations to mimic a lens with refraction:
            // 1. Specular reflection (glint) on the side facing the light
            highp vec2 glintOffset = activeLightDir * radius * 0.35;
            float glintDist = length(shapePos - glintOffset);
            float glint = 1.0 - smoothstep(0.0, radius * 0.22, glintDist);
            
            // 2. Fresnel rim reflection around the edges (surface normal N on a hemisphere)
            float nz = sqrt(1.0 - clamp(normDist * normDist, 0.0, 1.0));
            float fresnel = pow(1.0 - nz, 3.0) * 0.40;
            
            // Combine surface reflection components (specular glint and Fresnel rim, no interior focal glow)
            float lensGlow = (glint * 0.85 + fresnel) * fade;

            // 3. Lens refraction distortion:
            // Displace texture coordinates towards the center to create a convex magnifying lens effect.
            // Distorts smoothly from 0 at the center (normDist=0) to 0 at the outer boundary (normDist=1).
            float refractStrength = normDist * (1.0 - normDist) * 1.5 * fade;
            highp vec2 distortion = (shapePos / scale) * refractStrength;
            totalDistortion += distortion * weight;

            totalFade += fade * weight;
            totalGlow += lensGlow * weight;
            sumWeight += weight;
        }
    }

    highp vec2 finalDistortion = vec2(0.0);
    float dropMask = 0.0;
    float finalFade = 0.0;
    float finalGlow = 0.0;
    if (sumWeight > 0.0001) {
        finalDistortion = totalDistortion / sumWeight;
        finalFade = totalFade / sumWeight;
        finalGlow = totalGlow / sumWeight;
        dropMask = 1.0;
    }

    borderVal = 1.0;
    fadeVal = finalFade;
    return vec4(finalDistortion.x, finalDistortion.y, finalGlow, dropMask);
}

float GetDarkGodRays(highp vec2 uv, highp vec2 lightPos, float flashIntensity, highp float time, highp float aspect) {
    if (flashIntensity <= 0.01) return 1.0;

    // Scale for isotropic distances
    highp vec2 scaledUV = vec2(uv.x * aspect, uv.y);
    highp vec2 scaledLight = vec2(lightPos.x * aspect, lightPos.y);

    highp vec2 rayVec = scaledUV - scaledLight;
    highp float dist = length(rayVec);
    highp vec2 rayDir = normalize(rayVec);

    float shadow = 0.0;
    const int SAMPLES = 6;
    highp float stepSize = dist / float(SAMPLES);

    // Precompute step vector and initial sampling position to reduce arithmetic operations inside the loop
    highp vec2 stepVec = (rayDir / vec2(aspect, 1.0)) * stepSize;
    highp vec2 samplePos = uv * 3.5 - vec2(time * 0.08, 0.0);
    highp vec2 sampleStep = stepVec * 3.5;

    for (int i = 0; i < SAMPLES; i++) {
        samplePos -= sampleStep;
        float density = fbm(samplePos);
        shadow += smoothstep(0.35, 0.75, density);
    }
    shadow /= float(SAMPLES);

    float decay = exp(-dist * 0.85);
    return 1.0 - (shadow * decay * flashIntensity * 0.75);
}

void main() {
    float unusedBorder = 1.0;
    float fadeVal = 1.0;

    vec4 droplet = vec4(0.0);
    highp vec2 distortedUV = vTexCoord;
    if (uScreenDropletsEnabled > 0.5) {
        // Convert screen coordinates to screen-space UV (0 to 1) for isotropic droplet calculations
        highp vec2 screenUV = vec2((vScreenPos.x + 1.0) * 0.5, (vScreenPos.y + 1.0) * 0.5);
        // Get the droplet mask/fade (droplet.xy contains the lens refraction distortion offset in screen-space UV).
        droplet = GetDropletEffects(screenUV, uTime, uAspectRatio, uScreenDropletsSize, uRainColor, unusedBorder, fadeVal);
        // Convert distortion from screen-space UV to texture coordinate space using uTexCoordScale
        distortedUV = clamp(vTexCoord - droplet.xy * uTexCoordScale, vec2(0.0), vec2(1.0));
    }

    // Sample the background texture
    vec4 texColor = texture(uTexture, distortedUV);

    // Calculate luma and final alpha for silhouette masking
    float luma = (texColor.r + texColor.g + texColor.b) / 3.0;
    float alpha = smoothstep(0.96, 0.90, luma);
    float finalAlpha = min(texColor.a, alpha);

    if (finalAlpha <= 0.0) {
        discard;
    }

    // Tint color contribution inside active droplet
    vec3 baseTexColor = texColor.rgb;
    if (uScreenDropletsEnabled > 0.5 && droplet.w > 0.5) {
        float lumaRain = dot(uRainColor, vec3(0.299, 0.587, 0.114));
        vec3 desaturatedRainColor = mix(vec3(lumaRain), uRainColor, 0.5);
        baseTexColor = mix(baseTexColor, desaturatedRainColor, 0.15 * fadeVal);
    }

    // Base storm dimming (dark stormy night look, with a vertical gradient: darker at the top, lighter at the bottom)
    float normY = (vScreenPos.y + 1.0) * 0.5;
    float darkFactor = mix(0.32, 0.10, normY) * uBgBrightness;
    vec3 baseColor = baseTexColor * vec3(darkFactor, darkFactor, darkFactor * 1.22);

    // Add lightning illumination (up to +1.5 brightness with flash color)
    vec3 flashLight = baseTexColor * uFlashColor * uFlashIntensity * 1.5;

    vec3 finalColor = baseColor + flashLight;

    // Apply Dark God Rays shadowing
    float darkRay = GetDarkGodRays(vScreenPos, uLightningPos, uFlashIntensity, uTime, uAspectRatio);
    finalColor *= darkRay;

    // Rim Lighting: highlights the upper edge of the silhouettes facing the lightning
    vec3 rimColor = vec3(0.0);
    if (uFlashIntensity > 0.01) {
        highp vec2 rimOffset = normalize(vec2(vScreenPos.x - uLightningPos.x, 1.2)) * 0.008;
        vec4 texAbove = texture(uTexture, distortedUV + rimOffset);
        float lumaAbove = (texAbove.r + texAbove.g + texAbove.b) / 3.0;
        float alphaAbove = smoothstep(0.96, 0.90, lumaAbove);
        float rimMask = clamp(finalAlpha - alphaAbove, 0.0, 1.0);
        rimColor = uFlashColor * uFlashIntensity * rimMask * 2.8;
    }
    finalColor += rimColor;

    // "Lupa" glass glow (see GetDropletEffects' comment): a pure-color brightening, never a
    // UV sample, applied after the storm dimming so it still reads as glass catching light
    // even in the darkest parts of the scene.
    if (uScreenDropletsEnabled > 0.5 && droplet.w > 0.5) {
        finalColor += baseTexColor * droplet.z * 0.9 * clamp(uBgBrightness, 0.35, 1.5);
    }

    fragColor = vec4(finalColor, 1.0);
}
