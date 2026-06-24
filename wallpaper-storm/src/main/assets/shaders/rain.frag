#version 300 es

precision mediump float;

uniform vec4 uRainColor;

in float vDepth;
in vec2 vTexCoord;

out vec4 fragColor;

void main() {
    // vTexCoord.x: [0,1] across width (U), mapped to [-1,1]
    // vTexCoord.y: [0,1] along length; 0 = tail tip, 1 = head (front of travel)
    float x = vTexCoord.x * 2.0 - 1.0;
    float y = vTexCoord.y;

    // --- Teardrop shape parameters ---
    // R: fraction of the total drop length occupied by the circular head.
    // A larger R produces a rounder, more prominent head.
    float R   = 0.55;
    float y_c = 1.0 - R; // y-coordinate of the circle centre

    float width = 0.0;
    vec3 normal = vec3(0.0, 0.0, 1.0);

    if (y > y_c) {
        // ---- Circular head (bulbous front) ----
        float dy = y - y_c;
        width = sqrt(max(0.0, R * R - dy * dy));

        // Spherical normal so lighting gives a 3-D ball appearance
        float r2 = (x * x + dy * dy) / (R * R);
        float nz = sqrt(max(0.0, 1.0 - r2));
        normal = vec3(x / R, dy / R, nz);
    } else {
        // ---- Tapered tail (sharp and transparent at tip) ----
        // normalizedY goes from 0 (tail tip) to 1 (where tail meets head)
        float normalizedY = y / y_c;
        // Power 3.0 makes the taper very aggressive – needle-thin near y=0
        width = R * pow(normalizedY, 3.0);

        // Cylindrical normal for realistic edge lighting on the tail
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
    // Specular highlight – light from top-right-front
    vec3 lightDir = normalize(vec3(0.3, 0.6, 1.0));
    vec3 viewDir  = vec3(0.0, 0.0, 1.0);
    vec3 halfDir  = normalize(lightDir + viewDir);
    float spec    = pow(max(dot(normal, halfDir), 0.0), 32.0);

    // Fresnel rim at grazing angles (edges look brighter, like water)
    float fresnel = pow(1.0 - abs(normal.z), 2.5);

    // Vertical fade: tail is nearly invisible, head is fully opaque.
    // Power 2.5 concentrates the fade near the tip.
    float verticalGradient = pow(y, 2.5);

    // Lens glow: water converges light, so the drop centre is brightest
    float lensGlow = 0.4 + 0.6 * normal.z;

    // Combine colours
    vec3 baseColor      = uRainColor.rgb;
    vec3 highlightColor = vec3(1.0);
    vec3 finalColor     = mix(baseColor, highlightColor, spec * 0.8)
                        + vec3(0.25) * fresnel * baseColor;

    // Final alpha
    float alpha = uRainColor.a * vDepth * edgeAlpha * verticalGradient * lensGlow;

    fragColor = vec4(finalColor, alpha);
}
