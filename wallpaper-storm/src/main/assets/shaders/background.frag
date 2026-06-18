#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform float uFlashIntensity;
uniform vec3 uFlashColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Base storm dimming (dark stormy night look)
    vec3 baseColor = texColor.rgb * vec3(0.18, 0.18, 0.22);
    
    // Add lightning illumination (up to +1.5 brightness with flash color)
    vec3 flashLight = texColor.rgb * uFlashColor * uFlashIntensity * 1.5;
    
    fragColor = vec4(baseColor + flashLight, 1.0);
}
