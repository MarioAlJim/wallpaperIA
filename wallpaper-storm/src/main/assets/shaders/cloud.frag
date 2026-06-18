#version 300 es

precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Base cloud color (dark stormy look by default)
    vec3 baseColor = texColor.rgb * 0.7;
    
    // Add internal lightning illumination (up to +1.5 brightness with flash color)
    vec3 flashLight = texColor.rgb * uFlashColor * uFlashIntensity * 1.5;
    
    fragColor = vec4(baseColor + flashLight, texColor.a * uOpacity);
}
