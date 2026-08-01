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
uniform vec3 uRainColor;

// New uniform for lightning position (Dark God Rays and Rim Lighting)
uniform highp vec2 uLightningPos;

// Small tileable grid of prebaked random values used by noise()/fbm() for the
// Dark God Rays shadowing. Sampling a texture (hardware-filtered) replaces the
// per-fragment sin()/fract() hash math previously used to compute value noise.
uniform sampler2D uNoiseTexture;
const float NOISE_TEX_SIZE = 64.0;

out vec4 fragColor;

// A simple pseudo-random hash function (used only by the on-screen droplet effect below)
float Hash(highp vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + vec2(45.32, 45.32));
    return fract(p.x * p.y);
}

float noise(highp vec2 p) {
    // GL_REPEAT wrapping tiles this every NOISE_TEX_SIZE units, and GL_LINEAR
    // filtering reproduces the same bilinear blend the old hash-based noise() did.
    return texture(uNoiseTexture, p / NOISE_TEX_SIZE).r;
}

float fbm(highp vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// Computes lens distortion, specular highlight, border shading, trails, and smooth metaball blending for raindrops on screen
vec4 GetDropletEffects(highp vec2 uv, highp float time, highp float aspect, float sizeFactor, vec3 rainColor, out float borderVal, out float fadeVal) {
    highp vec2 distortion = vec2(0.0, 0.0);
    float spec = 0.0;
    float border = 1.0;
    float dropMask = 0.0;
    float finalFade = 1.0;

    // Grid size to control droplet scale (12 vertical cells). Grid-cell math (st/ip/fp/pos)
    // stays highp: at mediump, this jitters visibly as the droplet grid "swims".
    highp vec2 scale = vec2(12.0 * aspect, 12.0);
    highp vec2 st = uv * scale;
    highp vec2 ip = floor(st);
    highp vec2 fp = fract(st) - vec2(0.5, 0.5);

    float sumHeight = 0.0;
    highp vec2 totalDistortion = vec2(0.0);
    float totalSpec = 0.0;
    float totalBorder = 0.0;
    float totalFade = 0.0;

    highp vec2 totalTrailDistortion = vec2(0.0);
    float totalTrailMask = 0.0;
    float totalTrailFade = 0.0;

    vec2 lightDir = normalize(vec2(0.5, 0.7));

    for (int y = -9; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            highp vec2 neighbor = vec2(float(x), float(y));
            highp vec2 cell = ip + neighbor;
            float h_cell = Hash(cell);

            // 55% density check
            if (h_cell < 0.55) continue;

            // Kept highp: this accumulates uTime (unbounded over hours of wallpaper
            // uptime), so mediump would eventually make the slide animation glitch.
            highp float loopId = 0.0;
            highp float wrappedLoopId = 0.0;
            highp float t = 0.0;

            // Calculate randomized sliding distance (1/4 to 3/4 of screen height)
            float slideDist = mix(3.0, 9.0, Hash(cell + vec2(12.3, 45.6)));

            // Droplet sliding velocity (cells per second)
            float velocity = (0.108 + 0.644 * Hash(cell + vec2(4.4, 4.4))) / 3.0;

            // Speed (rate of fract progression per second)
            float speed = velocity / slideDist;

            highp float slideTime = time * speed + h_cell * 20.0;
            loopId = floor(slideTime);
            wrappedLoopId = mod(loopId, 16.0);
            t = fract(slideTime);

            // Completely random offset per loop iteration
            highp vec2 offset = vec2(
                Hash(cell + vec2(wrappedLoopId * 7.7, wrappedLoopId * 7.7)),
                Hash(cell + vec2(wrappedLoopId * 11.1, wrappedLoopId * 11.1))
            ) - vec2(0.5, 0.5);
            offset *= 0.3;

            // Instantaneous appearance at start of slide, gradual fade out at the end
            float fade = 1.0 - smoothstep(0.55, 1.0, t);

            // Render check per loop to add further spawn randomization
            float h_render = Hash(cell + vec2(wrappedLoopId * 1.3, wrappedLoopId * 1.3));
            if (h_render < 0.45) continue;

            // Size of droplet
            float radius = mix(0.06, 0.13, Hash(cell + vec2(wrappedLoopId * 2.3, wrappedLoopId * 2.3))) * sizeFactor;
            radius *= 0.85; // moving drop size scaling
            radius *= fade;

            highp vec2 pos = fp - neighbor - offset;
            pos.y -= (t * slideDist - 0.5); // Slide down by slideDist

            // Bounding box check (pruning)
            if (abs(pos.x) > radius || pos.y > radius * 3.0 || pos.y < -slideDist - radius) continue;

            float currentYScale = mix(1.08, 0.4, t);
            vec2 ovalScale = vec2(1.2, currentYScale);
            highp vec2 ovalPos = pos * ovalScale;
            float r = length(ovalPos);

            if (r < radius && radius > 0.001) {
                // Smooth blending height factor (distance field)
                float h_height = radius - r;
                float nz = sqrt(max(0.0, radius * radius - r * r)) / radius;
                highp vec2 n = -ovalPos / radius;

                highp vec2 distK = n * nz * 0.022;
                float specK = pow(max(dot(n, lightDir), 0.0), 10.0) * nz * fade;
                float borderK = smoothstep(0.0, 0.15, nz);

                // Accumulate weighted properties for smooth blending
                totalDistortion += distK * h_height;
                totalSpec += specK * h_height;
                totalBorder += borderK * h_height;
                totalFade += fade * h_height;
                sumHeight += h_height;
            } else {
                // Trail refraction (thickness is 75% of the droplet size)
                float trailWidth = radius * 0.75;
                if (pos.y < 0.0 && pos.y > -t * slideDist) {
                    float trailDist = abs(pos.x);
                    if (trailDist < trailWidth) {
                        float trailFade = 1.0 + (pos.y / (t * slideDist));
                        float trailMask = smoothstep(trailWidth, 0.0, trailDist) * trailFade * fade;

                        // Stronger trail distortion
                        highp vec2 trailDistortion = vec2(sin(pos.y * 30.0 + time * 4.0) * 0.015, 0.004) * trailMask;

                        totalTrailDistortion += trailDistortion;
                        totalTrailMask = max(totalTrailMask, 0.5 * trailMask);
                        totalTrailFade = max(totalTrailFade, fade * trailFade);
                    }
                }
            }
        }
    }

    if (sumHeight > 0.0001) {
        distortion = totalDistortion / sumHeight;
        spec = totalSpec / sumHeight;
        border = totalBorder / sumHeight;
        finalFade = totalFade / sumHeight;
        dropMask = 1.0;
    } else if (totalTrailMask > 0.0001) {
        distortion = totalTrailDistortion;
        spec = 0.0;
        border = 1.0;
        finalFade = totalTrailFade;
        dropMask = totalTrailMask;
    }

    borderVal = border;
    fadeVal = finalFade;
    return vec4(distortion.x, distortion.y, spec, dropMask);
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

    highp vec2 currentPos = uv;
    for (int i = 0; i < SAMPLES; i++) {
        currentPos -= (rayDir / vec2(aspect, 1.0)) * stepSize;
        float density = fbm(currentPos * 3.5 - vec2(time * 0.08, 0.0));
        shadow += smoothstep(0.35, 0.75, density);
    }
    shadow /= float(SAMPLES);

    float decay = exp(-dist * 0.85);
    return 1.0 - (shadow * decay * flashIntensity * 0.75);
}

void main() {
    float edgeBorder = 1.0;
    float fadeVal = 1.0;

    // Use the original vTexCoord for background droplets to maintain the exact lens effect
    // 1. Get droplet refraction distortion and lighting factors
    vec4 droplet = GetDropletEffects(vTexCoord, uTime, uAspectRatio, uScreenDropletsSize, uRainColor, edgeBorder, fadeVal);

    // 2. Only apply distortion if droplets are enabled
    highp vec2 distortedUV = vTexCoord;
    if (uScreenDropletsEnabled > 0.5) {
        distortedUV = clamp(vTexCoord + droplet.xy, vec2(0.0, 0.0), vec2(1.0, 1.0));
    }

    // Sample the background texture (distorted if inside enabled droplet)
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
    float darkFactor = mix(0.32, 0.10, normY);
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

    if (uScreenDropletsEnabled > 0.5 && droplet.w > 0.5) {
        // Apply droplet edge shading (dark border) and specular highlight (reflection)
        float specHighlight = droplet.z;
        finalColor = finalColor * (0.6 + 0.4 * edgeBorder) + vec3(0.5) * specHighlight;
    }

    fragColor = vec4(finalColor, 1.0);
}
