#version 300 es
precision mediump float;

in vec2 vTexCoord;
in vec3 vColor;
in float vOpacity;
out vec4 fragColor;

void main() {
    // Draw a soft glowing circular particle procedurally
    float dist = distance(vTexCoord, vec2(0.5, 0.5));
    float alpha = smoothstep(0.5, 0.05, dist);
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vColor, alpha * vOpacity);
}
