#version 300 es
precision highp float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform float uTime;
uniform float uAspectRatio;

// New uniforms for configurable screen droplets
uniform float uScreenDropletsEnabled;
uniform float uScreenDropletsSize;
uniform vec3 uRainColor;

out vec4 fragColor;

// A simple pseudo-random hash function
float Hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + vec2(45.32, 45.32));
    return fract(p.x * p.y);
}

// Computes lens distortion, specular highlight, and border shading for raindrops on screen
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
    
    float minR = 1.0; // Track closest drop
    
    for (int y = -9; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 cell = ip + neighbor;
            float h = Hash(cell);
            
            // 45% density check
            if (h < 0.55) continue;
            
            float loopId = 0.0;
            float wrappedLoopId = 0.0;
            float t = 0.0;
            
            // Calculate randomized sliding distance (1/4 to 3/4 of screen height)
            // Screen height is 12 cells, so distance is between 3.0 and 9.0 cells.
            float slideDist = mix(3.0, 9.0, Hash(cell + vec2(12.3, 45.6)));
            
            // Droplet sliding velocity (cells per second)
            float velocity = (0.108 + 0.644 * Hash(cell + vec2(4.4, 4.4))) / 3.0;
            
            // Speed (rate of fract progression per second) is velocity / distance,
            // so lifetime (duration of one cycle) is proportional to distance (T = slideDist / velocity).
            float speed = velocity / slideDist;
            
            float slideTime = time * speed + h * 20.0;
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
            
            // Size of droplet (increased base size to mix(0.06, 0.13), scaled by uniform)
            float radius = mix(0.06, 0.13, Hash(cell + vec2(wrappedLoopId * 2.3, wrappedLoopId * 2.3))) * sizeFactor;
            radius *= 0.85; // moving drop size scaling
            radius *= fade;
            
            vec2 pos = fp - neighbor - offset;
            pos.y -= (t * slideDist - 0.5); // Slide down by slideDist
            
            // Double the stretching effect: it went from 2.0 (V=0.6) to 3.0 (V=0.4) as it slides down (t -> 1.0)
            // Starts as 75% circular (V=0.9) instead of 100% circular (V=1.2) at spawn
            float currentYScale = mix(0.9, 0.4, t);
            
            // Quick distance check to prune calculations:
            // Since currentYScale >= 0.4, maximum vertical extent of the droplet is radius / 0.4 = 2.5 * radius.
            if (abs(pos.x) > radius || abs(pos.y) > radius * 3.0) continue;
            
            vec2 ovalScale = vec2(1.2, currentYScale);
            vec2 ovalPos = pos * ovalScale;
            float r = length(ovalPos);
            
            if (r < radius && radius > 0.001) {
                // Spherical normal/depth inside the droplet
                float nz = sqrt(max(0.0, radius * radius - r * r)) / radius;
                vec2 n = -ovalPos / radius;
                
                // Use properties of the closest droplet in this pixel
                if (r < minR) {
                    minR = r;
                    distortion = n * nz * 0.022;
                    
                    // Specular highlight from top-right light source
                    vec2 lightDir = normalize(vec2(0.5, 0.7));
                    spec = pow(max(dot(n, lightDir), 0.0), 10.0) * nz * fade;
                    
                    // Refraction loss edge border
                    border = smoothstep(0.0, 0.15, nz);
                    dropMask = 1.0;
                    finalFade = fade;
                }
            }
        }
    }
    
    borderVal = border;
    fadeVal = finalFade;
    return vec4(distortion.x, distortion.y, spec, dropMask);
}

void main() {
    float edgeBorder = 1.0;
    float fadeVal = 1.0;
    
    // 1. Get droplet refraction distortion and lighting factors
    vec4 droplet = GetDropletEffects(vTexCoord, uTime, uAspectRatio, uScreenDropletsSize, uRainColor, edgeBorder, fadeVal);
    
    // 2. Only apply distortion if droplets are enabled
    vec2 distortedUV = vTexCoord;
    if (uScreenDropletsEnabled > 0.5) {
        distortedUV = clamp(vTexCoord + droplet.xy, vec2(0.0, 0.0), vec2(1.0, 1.0));
    }
    
    // Sample the background texture (distorted if inside enabled droplet)
    vec4 texColor = texture(uTexture, distortedUV);
    
    // Tint color contribution inside active droplet
    vec3 baseTexColor = texColor.rgb;
    if (uScreenDropletsEnabled > 0.5 && droplet.w > 0.5) {
        // Decrease color saturation by 50% by mixing it with its grayscale luminance value
        float luma = dot(uRainColor, vec3(0.299, 0.587, 0.114));
        vec3 desaturatedRainColor = mix(vec3(luma), uRainColor, 0.5);
        
        // Subtle color tint (15% mix) depending on the selected rain color
        baseTexColor = mix(baseTexColor, desaturatedRainColor, 0.15 * fadeVal);
    }
    
    // Base storm dimming (dark stormy night look)
    vec3 baseColor = baseTexColor * vec3(0.18, 0.18, 0.22);
    
    // Add lightning illumination (up to +1.5 brightness with flash color)
    vec3 flashLight = baseTexColor * uFlashColor * uFlashIntensity * 1.5;
    
    vec3 finalColor = baseColor + flashLight;
    
    if (uScreenDropletsEnabled > 0.5 && droplet.w > 0.5) {
        // Apply droplet edge shading (dark border) and specular highlight (reflection)
        float specHighlight = droplet.z;
        finalColor = finalColor * (0.6 + 0.4 * edgeBorder) + vec3(0.5) * specHighlight;
    }
    
    fragColor = vec4(finalColor, 1.0);
}
