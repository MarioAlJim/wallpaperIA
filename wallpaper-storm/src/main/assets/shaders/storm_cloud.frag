#version 300 es
// Default precision is mediump: color/blend/opacity math dominates this shader and
// runs faster (and uses fewer registers) at mediump on mobile GPUs. Anything that
// feeds a texture() coordinate is explicitly qualified highp below to avoid visible
// jitter/swimming, since mediump (~10-bit mantissa) is too coarse for texel-accurate
// UV math once textures approach a few hundred pixels or values accumulate over time.
precision mediump float;

in highp vec2 vTexCoord;
in highp vec4 vScreenPos;

uniform sampler2D uTexture;
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;
uniform float uHeightFactor;

// Refraction uniforms
uniform highp float uTime;
uniform highp float uAspectRatio;

// Scaling uniforms for matching background coordinates (feed texture-coordinate math)
uniform highp vec2 uBgScale;
uniform highp vec2 uCloudScale;

// Volumetric shadow rays uniform (feeds the noise-texture lookup in GetDarkGodRays)
uniform highp vec2 uLightningPos;

// Small tileable grid of prebaked random values used by noise()/fbm() for the
// Dark God Rays shadowing. Sampling a texture (hardware-filtered) replaces the
// per-fragment sin()/fract() hash math previously used to compute value noise.
uniform sampler2D uNoiseTexture;
const float NOISE_TEX_SIZE = 64.0;

out vec4 fragColor;

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
    highp vec2 ndc = vScreenPos.xy / vScreenPos.w;

    // Convert screen coordinates to background coordinate space (Y flipped: 0.5 - Y)
    highp vec2 bgTexCoord = vec2(ndc.x * uAspectRatio / uBgScale.x * 0.5 + 0.5, 0.5 - (ndc.y / uBgScale.y) * 0.5);

    highp vec2 distortedTexCoord = vTexCoord;

    // Depth of field: background clouds (low uHeightFactor) get progressively softer while
    // foreground clouds stay perfectly sharp, pushing distant clouds further back visually.
    vec4 texColor = texture(uTexture, distortedTexCoord);
    float depthBlur = (1.0 - uHeightFactor) * 1.6;
    if (depthBlur > 0.01) {
        highp vec2 texel = 1.0 / vec2(textureSize(uTexture, 0));
        highp vec2 offset = texel * depthBlur;
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
