#version 300 es

precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform float uFlashIntensity;
uniform int uIsTextured;
uniform vec4 uLightningColor;

out vec4 fragColor;

void main() {
    if (uIsTextured == 1) {
        vec4 texColor = texture(uTexture, vTexCoord);
        fragColor = vec4(texColor.rgb * uLightningColor.rgb, texColor.a * uFlashIntensity * uLightningColor.a);
    } else {
        fragColor = vec4(uLightningColor.rgb, uFlashIntensity * uLightningColor.a);
    }
}
