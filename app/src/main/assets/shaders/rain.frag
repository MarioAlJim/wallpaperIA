#version 300 es

precision mediump float;

uniform vec4 uRainColor;

out vec4 fragColor;

void main() {
    fragColor = uRainColor;
}
