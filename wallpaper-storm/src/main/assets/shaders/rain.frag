#version 300 es

precision mediump float;

uniform vec4 uRainColor;
uniform float uBgBrightness;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;

in float vDepth;
in vec2 vTexCoord;
in float vCloudOpacity;

out vec4 fragColor;

void main() {
    // vTexCoord.x: [0,1] across width, mapped to [-1,1]
    // vTexCoord.y: [0,1] along length
    //   y=0 → tail tip (trailing edge, TOP of falling drop)   ← POINT
    //   y=1 → head front (leading edge, BOTTOM of falling drop) ← ROUND
    float x = vTexCoord.x * 2.0 - 1.0;
    float y = vTexCoord.y;

    // R: radius of the circular head in texture space.
    // The circle is centered at y=0.8 (junction), so the front
    // is bulbous and the rear (y=0) tapers to a sharp point.
    float R   = 0.60;
    float y_t = 0.80; // The head occupies the bottom 20%, the tail occupies the top 80%

    float width = 0.0;
    vec3 normal = vec3(0.0, 0.0, 1.0);

    if (y >= y_t) {
        // ---- Rounded head (front of travel, leading edge) ----
        // dy normalized in the range [0, 1] from the junction (y=0.8) to the front (y=1.0)
        float dy  = (y - y_t) / 0.20;
        width     = R * sqrt(max(0.0, 1.0 - dy * dy));

        // Spherical normal for 3-D ball lighting
        float r2 = (x / R) * (x / R) + dy * dy;
        float nz = sqrt(max(0.0, 1.0 - r2));
        normal   = vec3(x / R, dy, nz);
    } else {
        // ---- Tapered tail (trailing edge) ----
        // normalizedY: 0 = pointed tip (y=0), 1 = junction with head (y=y_t)
        float normalizedY = y / y_t;
        // Power 0.65: tail stays wide until very close to the tip,
        // then collapses to a sharp geometric point at y=0.
        width = R * pow(max(0.0, normalizedY), 0.65);

        // Cylindrical normal for edge lighting on the tail
        float x_norm = x / max(width, 0.001);
        float nz     = sqrt(max(0.0, 1.0 - x_norm * x_norm));
        normal = vec3(x_norm, 0.0, nz);
    }

    // Discard pixels outside the teardrop silhouette
    float dist = abs(x) / max(width, 0.001);
    if (dist > 1.0) discard;

    // Anti-aliased soft edge
    float edgeAlpha = 1.0 - smoothstep(0.75, 1.0, dist);

    // --- Lighting ---
    vec3 lightDir = normalize(vec3(0.3, 0.6, 1.0));
    vec3 viewDir  = vec3(0.0, 0.0, 1.0);
    vec3 halfDir  = normalize(lightDir + viewDir);
    float spec    = pow(max(dot(normal, halfDir), 0.0), 32.0);

    // Fresnel rim at grazing angles
    float fresnel = pow(1.0 - abs(normal.z), 2.5);

    // Fade the tail to near-transparent: head (y=1) stays at full 100% opacity,
    // tail tip (y=0) fades to 10% opacity (90% transparent).
    // Using pow with exponent 0.4 keeps the body mostly opaque and
    // only the very tip becomes strongly transparent.
    float verticalGradient = 0.10 + 0.90 * pow(max(0.0, y), 0.4);

    // Lens glow: brightest at the centre of the head
    float lensGlow = 0.4 + 0.6 * normal.z;

    vec3 baseColor      = uRainColor.rgb * uBgBrightness;
    vec3 highlightColor = vec3(1.0);
    vec3 finalColor     = mix(baseColor, highlightColor, spec * 0.8 * uBgBrightness)
                        + vec3(0.25) * fresnel * baseColor;

    // Lightning flash illuminates the rain drops with the flash's intensity and color
    vec3 flashLight = uFlashColor * uFlashIntensity * 1.5;
    finalColor += flashLight * (spec * 0.9 + 0.35 * fresnel + 0.25);

    // Rain visibility factor: at low brightness, rain is subtle and dims, surging with lightning flash
    float flashAlphaBoost = clamp(uFlashIntensity * 2.0, 0.0, 1.0);
    float rainAlphaFactor = clamp(mix(0.30, 1.0, clamp(uBgBrightness, 0.0, 1.0)) + flashAlphaBoost, 0.0, 1.0);

    float alpha = uRainColor.a * vDepth * vCloudOpacity * edgeAlpha * verticalGradient * lensGlow * rainAlphaFactor;

    fragColor = vec4(finalColor, alpha);
}
