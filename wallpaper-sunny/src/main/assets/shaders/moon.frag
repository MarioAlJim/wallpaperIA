#version 300 es
precision mediump float;

in vec2 vUV;

uniform int   uPhase;       // 0-7: 0=new, 4=full
uniform vec3  uMoonColor;   // e.g. (1.0, 0.97, 0.88)
uniform float uIntensity;   // 0-1, for combined mode fade

out vec4 fragColor;

void main() {
    // Center p in [-1,1]
    vec2 p = vec2((vUV.x - 0.5) * 2.0, (0.5 - vUV.y) * 2.0);

    float moonR = 0.44;
    float moonDist = length(p);
    float moonSDF = moonDist - moonR;

    // Shadow circle offset factor per phase (multiplied by moonR)
    // Phase 0=new(shadow covers all), 4=full(no shadow), 7=waning crescent
    float offsets[8];
    offsets[0] = 0.0;    // New moon: shadow at center -> all dark
    offsets[1] = -0.3;   // Waxing crescent: thin right sliver
    offsets[2] = -1.0;   // First quarter: right half lit
    offsets[3] = -3.0;   // Waxing gibbous: mostly lit
    offsets[4] = -10.0;  // Full moon: shadow far left, all lit
    offsets[5] = 3.0;    // Waning gibbous: mostly lit
    offsets[6] = 1.0;    // Last quarter: left half lit
    offsets[7] = 0.3;    // Waning crescent: thin left sliver

    int ph = clamp(uPhase, 0, 7);
    float shadowX = offsets[ph] * moonR;

    float shadowDist = length(p - vec2(shadowX, 0.0));
    float shadowSDF  = shadowDist - moonR;

    // Lit region: inside moon AND outside shadow
    float litSDF   = max(moonSDF, -shadowSDF);
    float litAlpha = smoothstep(0.025, -0.025, litSDF);

    // Soft halo glow (visible even for new moon)
    float haloAlpha = smoothstep(moonR + 0.22, moonR + 0.04, moonDist) * 0.28;
    // For new moon, halo is dimmer
    float haloFade = (ph == 0) ? 0.35 : 1.0;

    // Limb darkening on lit surface
    float limb = moonDist / moonR;
    vec3 surfaceColor = mix(uMoonColor, uMoonColor * 0.65, limb * limb * 0.6);

    float totalAlpha = (litAlpha + haloAlpha * haloFade) * uIntensity;
    vec3  totalColor = mix(uMoonColor * 0.3, surfaceColor, litAlpha / max(totalAlpha, 0.001));

    fragColor = vec4(totalColor, totalAlpha);
}
