#version 300 es

precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Base cloud color tinted by uCloudColor
    vec3 baseColor = texColor.rgb * uCloudColor;
    
    // Add internal lightning illumination (up to +1.5 brightness with flash color)
    vec3 flashLight = texColor.rgb * uFlashColor * uFlashIntensity * 1.5;
    
    fragColor = vec4(baseColor + flashLight, texColor.a * uOpacity);
}
