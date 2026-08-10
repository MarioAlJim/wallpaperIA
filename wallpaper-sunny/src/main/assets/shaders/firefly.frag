#version 300 es
precision mediump float;

in vec2 vTexCoord;
in vec3 vColor;
in float vOpacity;
out vec4 fragColor;

void main() {
    // Center is (0.5, 0.5)
    float dist = distance(vTexCoord, vec2(0.5, 0.5));
    if (dist > 0.5) {
        discard;
    }

    float alpha = 0.0;
    vec3 finalColor = vColor;

    // Two layers:
    // 1. Inner core: dist from 0.0 to 0.15 (brighter, blends to white-green)
    // 2. Outer halo: dist from 0.15 to 0.50 (translucent, fades to edge)
    if (dist <= 0.15) {
        float t = dist / 0.15;
        alpha = 1.0;
        finalColor = mix(vec3(0.8, 1.0, 0.8), vColor, t);
    } else {
        float t = (dist - 0.15) / (0.5 - 0.15);
        alpha = mix(0.4, 0.0, t);
    }

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(finalColor, alpha * vOpacity);
}
