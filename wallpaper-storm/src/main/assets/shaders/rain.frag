#version 300 es

precision mediump float;

uniform vec4 uRainColor;

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
    // The circle is centered at y=1 (leading edge), so the front
    // is bulbous and the rear (y=0) tapers to a sharp point.
    float R   = 0.50;
    float y_t = 1.0 - R; // y where the tail region ends and head begins

    float width = 0.0;
    vec3 normal = vec3(0.0, 0.0, 1.0);

    if (y >= y_t) {
        // ---- Rounded head (front of travel, leading edge) ----
        // Circle centred at y=1. dy = distance from that centre.
        float dy  = 1.0 - y;
        width     = sqrt(max(0.0, R * R - dy * dy));

        // Spherical normal for 3-D ball lighting
        float r2 = (x * x + dy * dy) / (R * R);
        float nz = sqrt(max(0.0, 1.0 - r2));
        normal   = vec3(x / R, -dy / R, nz);
    } else {
        // ---- Tapered tail (trailing edge) ----
        // normalizedY: 0 = pointed tip (y=0), 1 = junction with head (y=y_t)
        float normalizedY = y / y_t;
        // Power 0.65: tail stays wide until very close to the tip,
        // then collapses to a sharp geometric point at y=0.
        width = R * pow(normalizedY, 0.65);

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

    // Fade the tail slightly so the pointed tip blends naturally.
    // Low power (1.1) keeps most of the tail opaque so the point is visible.
    float verticalGradient = pow(y, 1.1);

    // Lens glow: brightest at the centre of the head
    float lensGlow = 0.4 + 0.6 * normal.z;

    vec3 baseColor      = uRainColor.rgb;
    vec3 highlightColor = vec3(1.0);
    vec3 finalColor     = mix(baseColor, highlightColor, spec * 0.8)
                        + vec3(0.25) * fresnel * baseColor;

    float alpha = uRainColor.a * vDepth * vCloudOpacity * edgeAlpha * verticalGradient * lensGlow;

    fragColor = vec4(finalColor, alpha);
}
