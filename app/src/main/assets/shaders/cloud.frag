#version 300 es

precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform float uOpacity;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    fragColor = vec4(texColor.rgb, texColor.a * uOpacity);
}
