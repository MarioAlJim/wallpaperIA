#version 300 es

precision mediump float;

uniform vec4 uWindColor;

in float vDepth;
in float vAlpha;
out vec4 fragColor;

void main() {
    float alpha = uWindColor.a * vDepth * vAlpha;
    fragColor = vec4(uWindColor.rgb, alpha);
}
