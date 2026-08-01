#version 300 es
precision highp float;

in vec2 vTexCoord;

uniform float uTime;
uniform float uAspectRatio;
uniform float uScreenDropletsEnabled;
uniform float uScreenDropletsSize;
uniform vec3 uRainColor;

out vec4 fragColor;

// Distortion components are small (well under +/-0.1) so they're encoded into
// the [0,1] RGBA8 render target as (distortion * SCALE + 0.5) and decoded the
// same way on the reading side (storm_cloud.frag).
const float DISTORTION_ENCODE_SCALE = 10.0;

float Hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + vec2(45.32, 45.32));
    return fract(p.x * p.y);
}

// Computes lens distortion, specular highlight, and border shading for a single droplet
// per grid cell that impacts, slides down, and stretches vertically as it slides (no trail —
// removed for performance; the trail's per-pixel sin()-based refraction was the most
// expensive part of this loop for comparatively little visual payoff).
vec4 GetDropletEffects(vec2 uv, float time, float aspect, float sizeFactor, vec3 rainColor, out float borderVal, out float fadeVal) {
    vec2 distortion = vec2(0.0, 0.0);
    float spec = 0.0;
    float border = 1.0;
    float dropMask = 0.0;
    float finalFade = 1.0;

    // Grid size to control droplet scale (12 vertical cells)
    vec2 scale = vec2(12.0 * aspect, 12.0);
    vec2 st = uv * scale;
    vec2 ip = floor(st);
    vec2 fp = fract(st) - vec2(0.5, 0.5);

    float sumHeight = 0.0;
    vec2 totalDistortion = vec2(0.0);
    float totalSpec = 0.0;
    float totalBorder = 0.0;
    float totalFade = 0.0;

    vec2 lightDir = normalize(vec2(0.5, 0.7));

    for (int y = -9; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 cell = ip + neighbor;
            float h_cell = Hash(cell);

            // 55% density check
            if (h_cell < 0.55) continue;

            float loopId = 0.0;
            float wrappedLoopId = 0.0;
            float t = 0.0;

            // Calculate randomized sliding distance (1/4 to 3/4 of screen height)
            float slideDist = mix(3.0, 9.0, Hash(cell + vec2(12.3, 45.6)));

            // Droplet sliding velocity (cells per second)
            float velocity = (0.108 + 0.644 * Hash(cell + vec2(4.4, 4.4))) / 3.0;

            // Speed (rate of fract progression per second)
            float speed = velocity / slideDist;

            float slideTime = time * speed + h_cell * 20.0;
            loopId = floor(slideTime);
            wrappedLoopId = mod(loopId, 16.0);
            t = fract(slideTime);

            // Completely random offset per loop iteration
            vec2 offset = vec2(
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

            vec2 pos = fp - neighbor - offset;
            pos.y -= (t * slideDist - 0.5); // Slide down by slideDist

            // Bounding box check (pruning): only the droplet's own footprint now that
            // there's no trail extending back up to the slide's starting point.
            if (abs(pos.x) > radius * 1.3 || abs(pos.y) > radius * 3.0) continue;

            // Stretches vertically as it slides (currentYScale shrinks from 1.08 to 0.4)
            float currentYScale = mix(1.08, 0.4, t);
            vec2 ovalScale = vec2(1.2, currentYScale);
            vec2 ovalPos = pos * ovalScale;
            float r = length(ovalPos);

            if (r < radius && radius > 0.001) {
                // Smooth blending height factor (distance field)
                float h_height = radius - r;
                float nz = sqrt(max(0.0, radius * radius - r * r)) / radius;
                vec2 n = -ovalPos / radius;

                vec2 distK = n * nz * 0.022;
                float specK = pow(max(dot(n, lightDir), 0.0), 10.0) * nz * fade;
                float borderK = smoothstep(0.0, 0.15, nz);

                // Accumulate weighted properties for smooth blending
                totalDistortion += distK * h_height;
                totalSpec += specK * h_height;
                totalBorder += borderK * h_height;
                totalFade += fade * h_height;
                sumHeight += h_height;
            }
        }
    }

    if (sumHeight > 0.0001) {
        distortion = totalDistortion / sumHeight;
        spec = totalSpec / sumHeight;
        border = totalBorder / sumHeight;
        finalFade = totalFade / sumHeight;
        dropMask = 1.0;
    }

    borderVal = border;
    fadeVal = finalFade;
    return vec4(distortion.x, distortion.y, spec, dropMask);
}

void main() {
    if (uScreenDropletsEnabled <= 0.5) {
        // Neutral: decodes back to zero distortion in the reading shader
        fragColor = vec4(0.5, 0.5, 0.0, 0.0);
        return;
    }

    float borderVal = 1.0;
    float fadeVal = 1.0;
    vec4 droplet = GetDropletEffects(vTexCoord, uTime, uAspectRatio, uScreenDropletsSize, uRainColor, borderVal, fadeVal);

    vec2 encoded = clamp(droplet.xy * DISTORTION_ENCODE_SCALE + 0.5, 0.0, 1.0);
    fragColor = vec4(encoded, 0.0, 1.0);
}
