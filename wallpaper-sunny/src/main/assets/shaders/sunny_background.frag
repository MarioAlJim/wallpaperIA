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
uniform float uNightIntensity;
uniform float uMoonPhaseMultiplier;
uniform float uTime;
uniform vec4 uCloudShadowX;
uniform vec4 uCloudShadowWidth;
uniform vec4 uCloudShadowOpacity;

out vec4 fragColor;

void main() {
    vec2 sampleUv = vTexCoord;
    vec4 texColor = texture(uTexture, sampleUv);

    // Calculate luminance/luma to key out the white sky
    float luma = (texColor.r + texColor.g + texColor.b) / 3.0;

    // White pixels (luma >= 0.96) become fully transparent,
    // darker silhouette pixels (luma <= 0.90) remain fully opaque.
    float alpha = smoothstep(0.96, 0.90, luma);

    // --- Rim lighting setup: measure how fast alpha changes across neighboring fragments
    // (the silhouette's edge) using native ES3 derivatives, computed here - before any
    // `discard` - so every fragment in a 2x2 quad takes the same control-flow path and the
    // derivative stays well-defined.
    float edgeDerivative = fwidth(alpha);

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
    
    // Scale glare at night by moon phase multiplier
    float phaseMultiplier = mix(1.0, uMoonPhaseMultiplier, uNightIntensity);
    glare *= phaseMultiplier;
    
    // Fade out glare when the sun is below or near the horizon
    glare *= smoothstep(-0.4, -0.1, uSunPos.y);
    
    vec3 fgColor;
    vec3 bgColor;
    vec3 highlightColor;

    vec3 fgDay, bgDay, hlDay;
    vec3 fgSunset, bgSunset, hlSunset;
    vec3 fgDawn, bgDawn, hlDawn;

    if (uTheme == 0) { // Noon Blue
        fgDay = vec3(0.04, 0.15, 0.22);
        bgDay = vec3(0.20, 0.38, 0.46);
        hlDay = vec3(0.98, 0.98, 0.82);

        // Sunset/Dusk intermediate: Sunset Orange
        fgSunset = vec3(0.18, 0.04, 0.10);
        bgSunset = vec3(0.55, 0.20, 0.12);
        hlSunset = vec3(1.0, 0.70, 0.25);

        // Sunrise/Dawn intermediate: Purple Dusk
        fgDawn = vec3(0.12, 0.06, 0.20);
        bgDawn = vec3(0.50, 0.28, 0.44);
        hlDawn = vec3(1.0, 0.75, 0.85);
    } else if (uTheme == 1) { // Sunset Orange
        fgDay = vec3(0.18, 0.04, 0.10);
        bgDay = vec3(0.55, 0.20, 0.12);
        hlDay = vec3(1.0, 0.70, 0.25);

        // Sunset/Dusk intermediate: Purple Dusk
        fgSunset = vec3(0.12, 0.06, 0.20);
        bgSunset = vec3(0.50, 0.28, 0.44);
        hlSunset = vec3(1.0, 0.75, 0.85);

        // Sunrise/Dawn intermediate: Noon Blue / Gold
        fgDawn = vec3(0.04, 0.15, 0.22);
        bgDawn = vec3(0.20, 0.38, 0.46);
        hlDawn = vec3(0.98, 0.98, 0.82);
    } else if (uTheme == 2) { // Purple Dusk
        fgDay = vec3(0.12, 0.06, 0.20);
        bgDay = vec3(0.50, 0.28, 0.44);
        hlDay = vec3(1.0, 0.75, 0.85);

        // Sunset/Dusk intermediate: Deep dark blue-violet
        fgSunset = vec3(0.08, 0.04, 0.15);
        bgSunset = vec3(0.35, 0.18, 0.40);
        hlSunset = vec3(0.9, 0.7, 0.85);

        // Sunrise/Dawn intermediate: Warm Orange
        fgDawn = vec3(0.18, 0.08, 0.08);
        bgDawn = vec3(0.60, 0.30, 0.20);
        hlDawn = vec3(1.0, 0.8, 0.5);
    } else { // Custom
        fgDay = mix(uSkyBottom * 0.12, vec3(0.04, 0.04, 0.06), 0.4);
        bgDay = mix(uSkyBottom, vec3(0.25, 0.25, 0.3), 0.35) * 0.65;
        hlDay = mix(vec3(1.0, 0.95, 0.82), uSkyBottom, 0.4);

        // Intermediate blends
        fgSunset = mix(fgDay, vec3(0.18, 0.04, 0.10), 0.5);
        bgSunset = mix(bgDay, vec3(0.55, 0.20, 0.12), 0.5);
        hlSunset = mix(hlDay, vec3(1.0, 0.70, 0.25), 0.5);

        fgDawn = mix(fgDay, vec3(0.12, 0.06, 0.20), 0.5);
        bgDawn = mix(bgDay, vec3(0.50, 0.28, 0.44), 0.5);
        hlDawn = mix(hlDay, vec3(1.0, 0.75, 0.85), 0.5);
    }

    // Calculate transition weights based on uNightIntensity and sun position
    float wDay = clamp(1.0 - uNightIntensity * 2.0, 0.0, 1.0);
    float wSunset = 0.0;
    float wDawn = 0.0;
    
    if (uSunPos.x >= 0.0) {
        wSunset = clamp(1.0 - abs(uNightIntensity - 0.5) * 2.0, 0.0, 1.0);
    } else {
        wDawn = clamp(1.0 - abs(uNightIntensity - 0.5) * 2.0, 0.0, 1.0);
    }

    float sumWeights = wDay + wSunset + wDawn;
    if (sumWeights > 0.001) {
        fgColor = (fgDay * wDay + fgSunset * wSunset + fgDawn * wDawn) / sumWeights;
        bgColor = (bgDay * wDay + bgSunset * wSunset + bgDawn * wDawn) / sumWeights;
        highlightColor = (hlDay * wDay + hlSunset * wSunset + hlDawn * wDawn) / sumWeights;
    } else {
        fgColor = fgDay;
        bgColor = bgDay;
        highlightColor = hlDay;
    }

    // Interpolate colors to night values
    fgColor = mix(fgColor, vec3(0.005, 0.01, 0.02), uNightIntensity * 0.9);
    bgColor = mix(bgColor, vec3(0.02, 0.03, 0.07), uNightIntensity * 0.85);
    highlightColor = mix(highlightColor, vec3(0.9, 0.95, 1.0), uNightIntensity);

    vec3 finalHillColor;
    
    if (uIsCustom == 1) {
        // For custom gallery background, preserve its original colors but apply the sun glare and night darkening
        vec3 nightColor = texColor.rgb * 0.15;
        vec3 baseColor = mix(texColor.rgb, nightColor, uNightIntensity);
        finalHillColor = mix(baseColor, highlightColor, glare * 0.40);
    } else {
        // Detect if the asset is colored (chrominance is significant)
        float maxVal = max(texColor.r, max(texColor.g, texColor.b));
        float minVal = min(texColor.r, min(texColor.g, texColor.b));
        bool isColored = (maxVal - minVal) > 0.04;

        if (isColored) {
            // Apply theme tint to colored assets
            vec3 tintedColor = mix(texColor.rgb, bgColor, 0.40);
            // Darken it at night
            tintedColor = mix(tintedColor, tintedColor * 0.15, uNightIntensity);
            // Add sun glare/illumination dynamically
            finalHillColor = mix(tintedColor, highlightColor, glare * 0.45);
        } else {
            // For our monochrome backgrounds, apply flat vector shading:
            // Silhouette textures have multiple levels of brightness (foreground hills are darker, background hills are lighter).
            // Let's normalize luma of the silhouette pixels (originally <= 0.9) to a [0.0, 1.0] range.
            float normLuma = clamp(luma / 0.9, 0.0, 1.0);
            
            // Blend between foreground and background hill colors
            vec3 baseHillColor = mix(fgColor, bgColor, normLuma);
            
            // Add sun glare/illumination dynamically
            finalHillColor = mix(baseHillColor, highlightColor, glare * 0.50);
        }
    }

    // --- Cloud shadows ---
    float totalShadow = 0.0;
    for (int i = 0; i < 4; i++) {
        if (uCloudShadowOpacity[i] > 0.01) {
            // Shift shadow horizontally based on sun position to simulate angle
            float sunShift = (uSunPos.x - 0.5) * 0.05;
            float targetX = uCloudShadowX[i] + sunShift;
            float dist = abs(vTexCoord.x - targetX);
            
            float shadowFactor = smoothstep(uCloudShadowWidth[i], uCloudShadowWidth[i] * 0.4, dist);
            float heightFactor = mix(0.5, 1.0, vTexCoord.y);
            
            totalShadow = max(totalShadow, shadowFactor * uCloudShadowOpacity[i] * 0.28 * heightFactor);
        }
    }
    float shadowMult = 1.0 - (totalShadow * (1.0 - uNightIntensity));
    finalHillColor *= shadowMult;
    
    // --- Rim lighting: a thin, sun-tinted glow along the silhouette's edge, strongest on the
    // side of the horizon facing the sun. Reuses `glare` (already computed above from the
    // sun/moon-to-fragment distance, including its night/moon-phase/horizon fades) so the rim
    // only lights up where the general terrain glare already would. `rimEdge` is a narrow
    // band centered exactly on the alpha transition (peaks at alpha==0.5, tapering to 0 at
    // fully opaque/fully transparent), reinforced by the derivative so it stays a couple of
    // pixels wide regardless of how steep the source silhouette's alpha ramp is.
    const float kRimEdgeDerivativeThreshold = 0.35;
    const float kRimIntensity = 0.35; // overall rim-glow strength multiplier - kept subtle
    float rimEdge = smoothstep(0.0, kRimEdgeDerivativeThreshold, edgeDerivative) * (4.0 * alpha * (1.0 - alpha));
    vec3 rimColor = mix(highlightColor, vec3(1.0, 0.95, 0.85), 0.3);
    finalHillColor += rimColor * rimEdge * glare * kRimIntensity;

    fragColor = vec4(finalHillColor, finalAlpha);
}
