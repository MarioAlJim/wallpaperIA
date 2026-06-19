#version 300 es
precision mediump float;

in vec2 vTexCoord;
in vec2 vScreenPos;

uniform sampler2D uTexture;
uniform int uTheme;
uniform vec2 uSunPos;
uniform float uAspectRatio;
uniform int uIsCustom; // 1 if custom image, 0 if asset image
uniform vec3 uSkyTop;
uniform vec3 uSkyBottom;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(uTexture, vTexCoord);
    
    // Calculate luminance/luma to key out the white sky
    float luma = (texColor.r + texColor.g + texColor.b) / 3.0;
    
    // White pixels (luma >= 0.96) become fully transparent,
    // darker silhouette pixels (luma <= 0.90) remain fully opaque.
    float alpha = smoothstep(0.96, 0.90, luma);
    
    // Combine with the texture's original alpha channel
    float finalAlpha = min(texColor.a, alpha);
    
    if (finalAlpha <= 0.0) {
        discard;
    }
    
    // Calculate lighting from the sun
    vec2 uv = vec2(vScreenPos.x * uAspectRatio, vScreenPos.y);
    float distToSun = distance(uv, uSunPos * uAspectRatio);
    
    // Smooth glare/illumination based on distance to the sun
    float glare = smoothstep(0.75, 0.05, distToSun);
    
    // Fade out glare when the sun is below or near the horizon
    glare *= smoothstep(-0.6, -0.1, uSunPos.y);
    
    vec3 fgColor;
    vec3 bgColor;
    vec3 highlightColor;
    
    if (uTheme == 0) { // Noon Blue
        fgColor = vec3(0.04, 0.15, 0.22);       // Deep slate-teal
        bgColor = vec3(0.20, 0.38, 0.46);       // Soft misty teal-blue
        highlightColor = vec3(0.98, 0.98, 0.82); // Warm white-yellow sun highlight
    } else if (uTheme == 1) { // Sunset Orange
        fgColor = vec3(0.18, 0.04, 0.10);       // Deep warm plum/burgundy
        bgColor = vec3(0.55, 0.20, 0.12);       // Warm terracotta/orange
        highlightColor = vec3(1.0, 0.70, 0.25);  // Golden orange sun highlight
    } else if (uTheme == 2) { // Purple Dusk (Theme 2)
        fgColor = vec3(0.12, 0.06, 0.20);       // Deep indigo/violet
        bgColor = vec3(0.50, 0.28, 0.44);       // Soft misty mauve/pink
        highlightColor = vec3(1.0, 0.75, 0.85);  // Pale lavender-pink sun highlight
    } else { // Custom (Theme 3)
        // Silhouette foreground: very dark, slightly tinted with sky bottom color
        fgColor = mix(uSkyBottom * 0.12, vec3(0.04, 0.04, 0.06), 0.4);
        // Silhouette background/haze: desaturated, darkened bottom sky color
        bgColor = mix(uSkyBottom, vec3(0.25, 0.25, 0.3), 0.35) * 0.65;
        // Glare highlight: blend warm white-yellow with sky bottom color
        highlightColor = mix(vec3(1.0, 0.95, 0.82), uSkyBottom, 0.4);
    }
    
    vec3 finalHillColor;
    
    if (uIsCustom == 1) {
        // For custom gallery background, preserve its original colors but apply the sun glare
        finalHillColor = mix(texColor.rgb, highlightColor, glare * 0.40);
    } else {
        // For our minimalist backgrounds, apply flat vector shading:
        // Silhouette textures have multiple levels of brightness (foreground hills are darker, background hills are lighter).
        // Let's normalize luma of the silhouette pixels (originally <= 0.9) to a [0.0, 1.0] range.
        float normLuma = clamp(luma / 0.9, 0.0, 1.0);
        
        // Blend between foreground and background hill colors
        vec3 baseHillColor = mix(fgColor, bgColor, normLuma);
        
        // Add sun glare/illumination dynamically
        finalHillColor = mix(baseHillColor, highlightColor, glare * 0.50);
    }
    
    fragColor = vec4(finalHillColor, finalAlpha);
}
