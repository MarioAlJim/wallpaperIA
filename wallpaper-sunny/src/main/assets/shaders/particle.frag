#version 300 es
precision mediump float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform vec3 uColor;
uniform float uOpacity;

void main() {
    // Draw a soft glowing circular particle procedurally
    float dist = distance(vTexCoord, vec2(0.5, 0.5));
    float alpha = smoothstep(0.5, 0.05, dist);
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(uColor, alpha * uOpacity);
}
