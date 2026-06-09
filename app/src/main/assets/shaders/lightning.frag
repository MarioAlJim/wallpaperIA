#version 300 es

precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform float uFlashIntensity;
uniform int uIsTextured;

out vec4 fragColor;

void main() {
    if (uIsTextured == 1) {
        vec4 texColor = texture(uTexture, vTexCoord);
        fragColor = vec4(texColor.rgb, texColor.a * uFlashIntensity);
    } else {
        fragColor = vec4(1.0, 1.0, 1.0, uFlashIntensity);
    }
}
