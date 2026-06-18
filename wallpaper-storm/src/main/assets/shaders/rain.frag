#version 300 es

precision mediump float;

uniform vec4 uRainColor;

in float vDepth;
out vec4 fragColor;

void main() {
    fragColor = vec4(uRainColor.rgb, uRainColor.a * vDepth);
}
