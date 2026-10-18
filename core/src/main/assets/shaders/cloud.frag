#version 300 es

precision mediump float;

in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform float uOpacity;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;
uniform vec3 uCloudColor;
uniform float uBgBrightness;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Base cloud color tinted by uCloudColor and scaled by uBgBrightness
    vec3 baseColor = texColor.rgb * uCloudColor * uBgBrightness;
    
    // Add internal lightning illumination (up to +1.5 brightness with flash color)
    vec3 flashLight = texColor.rgb * uFlashColor * uFlashIntensity * 1.5;

    float flashAlphaBoost = clamp(uFlashIntensity * 2.0, 0.0, 1.0);
    float cloudAlphaFactor = clamp(mix(0.35, 1.0, clamp(uBgBrightness, 0.0, 1.0)) + flashAlphaBoost, 0.0, 1.0);
    
    fragColor = vec4(baseColor + flashLight, texColor.a * uOpacity * cloudAlphaFactor);
}
