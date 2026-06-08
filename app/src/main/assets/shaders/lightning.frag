#version 300 es

precision mediump float;

uniform float uFlashIntensity;

out vec4 fragColor;

void main() {
    fragColor = vec4(1.0, 1.0, 1.0, uFlashIntensity);
}
