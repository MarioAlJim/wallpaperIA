#version 300 es
precision highp float;

in vec2 vTexCoord;
in vec4 vScreenPos;

uniform sampler2D uTexture;
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;
uniform float uHeightFactor;

// Refraction uniforms
uniform float uTime;
uniform float uAspectRatio;
uniform float uScreenDropletsEnabled;
uniform float uScreenDropletsSize;
uniform vec3 uRainColor;

// Scaling uniforms for matching background coordinates
uniform vec2 uBgScale;
uniform vec2 uCloudScale;

// Volumetric shadow rays uniform
uniform vec2 uLightningPos;

// Precomputed screen-space raindrop distortion mask (rendered once per frame by
// raindrop_mask.frag) instead of recomputing the droplet loop per cloud fragment.
uniform sampler2D uDropletMask;
// Must match raindrop_mask.frag's DISTORTION_ENCODE_SCALE.
const float DISTORTION_ENCODE_SCALE = 10.0;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

float GetDarkGodRays(vec2 uv, vec2 lightPos, float flashIntensity, float time, float aspect) {
    if (flashIntensity <= 0.01) return 1.0;
    
    // Scale for isotropic distances
    vec2 scaledUV = vec2(uv.x * aspect, uv.y);
    vec2 scaledLight = vec2(lightPos.x * aspect, lightPos.y);
    
    vec2 rayVec = scaledUV - scaledLight;
    float dist = length(rayVec);
    vec2 rayDir = normalize(rayVec);
    
    float shadow = 0.0;
    const int SAMPLES = 10;
    float stepSize = dist / float(SAMPLES);
    
    vec2 currentPos = uv;
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
    vec2 ndc = vScreenPos.xy / vScreenPos.w;
    
    // Convert screen coordinates to background coordinate space (Y flipped: 0.5 - Y)
    vec2 bgTexCoord = vec2(ndc.x * uAspectRatio / uBgScale.x * 0.5 + 0.5, 0.5 - (ndc.y / uBgScale.y) * 0.5);
    
    vec2 distortedTexCoord = vTexCoord;
    if (uScreenDropletsEnabled > 0.5) {
        // Sample the precomputed droplet mask (rendered once per frame) instead of
        // recomputing the droplet loop for every overlapping cloud fragment.
        vec2 encodedDistortion = texture(uDropletMask, bgTexCoord).rg;
        vec2 dropletDistortion = (encodedDistortion - 0.5) / DISTORTION_ENCODE_SCALE;

        // Scale the background-space displacement to fit the cloud texture coordinates
        vec2 cloudDistortion = dropletDistortion * (uBgScale * 2.0 / uCloudScale);
        distortedTexCoord = clamp(vTexCoord + cloudDistortion, vec2(0.0), vec2(1.0));
    }
    
    // Depth of field: background clouds (low uHeightFactor) get progressively softer while
    // foreground clouds stay perfectly sharp, pushing distant clouds further back visually.
    vec4 texColor = texture(uTexture, distortedTexCoord);
    float depthBlur = (1.0 - uHeightFactor) * 1.6;
    if (depthBlur > 0.01) {
        vec2 texel = 1.0 / vec2(textureSize(uTexture, 0));
        vec2 offset = texel * depthBlur;
        vec4 blurred = texColor;
        blurred += texture(uTexture, distortedTexCoord + vec2(offset.x, 0.0));
        blurred += texture(uTexture, distortedTexCoord - vec2(offset.x, 0.0));
        blurred += texture(uTexture, distortedTexCoord + vec2(0.0, offset.y));
        blurred += texture(uTexture, distortedTexCoord - vec2(0.0, offset.y));
        blurred += texture(uTexture, distortedTexCoord + offset);
        blurred += texture(uTexture, distortedTexCoord - offset);
        blurred += texture(uTexture, distortedTexCoord + vec2(offset.x, -offset.y));
        blurred += texture(uTexture, distortedTexCoord + vec2(-offset.x, offset.y));
        texColor = blurred / 9.0;
    }

    // Base cloud color tinted by uCloudColor
    vec3 baseColor = texColor.rgb * uCloudColor;
    
    // Vertical gradient (darker at bottom of cloud, lighter at top of cloud)
    // vTexCoord.y (or distortedTexCoord.y) goes from 0.0 (top) to 1.0 (bottom) of the cloud quad.
    // Low cloud (uHeightFactor = 0.0, furthest back/background) ranges from 0.85 (bottom) to 1.25 (top) -> dominating in white.
    // High cloud (uHeightFactor = 1.0, nearest front/foreground) ranges from 0.15 (bottom) to 0.5 (top) -> very dark/shadowed.
    float gradBottom = mix(0.85, 0.15, uHeightFactor);
    float gradTop = mix(1.25, 0.5, uHeightFactor);
    float verticalGrad = mix(gradBottom, gradTop, 1.0 - distortedTexCoord.y);
    baseColor *= verticalGrad;
    
    // Screen-space vertical gradient: darker at the top of the screen, lighter at the bottom
    float normY = (ndc.y + 1.0) * 0.5;
    float screenGrad = mix(1.15, 0.75, normY);
    baseColor *= screenGrad;
    
    // Add internal lightning illumination
    vec3 flashLight = texColor.rgb * uFlashColor * uFlashIntensity * 1.5;
    
    vec3 finalColor = baseColor + flashLight;
    
    // Apply Dark God Rays shadowing
    float darkRay = GetDarkGodRays(ndc, uLightningPos, uFlashIntensity, uTime, uAspectRatio);
    finalColor *= darkRay;
    
    fragColor = vec4(finalColor, texColor.a * uOpacity);
}
