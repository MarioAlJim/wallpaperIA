#version 300 es
precision mediump float;

in vec2 vUV;

uniform int   uPhase;       // 0-7: 0=new, 4=full
uniform vec3  uMoonColor;   // e.g. (1.0, 0.97, 0.88)
uniform float uIntensity;     // 0-1, for combined mode fade
uniform float uHaloIntensity; // Y-based halo fade

out vec4 fragColor;

void main() {
    // Center p in [-1,1]
    vec2 p = vec2((vUV.x - 0.5) * 2.0, (0.5 - vUV.y) * 2.0);

    float moonR = 0.44;
    float moonDist = length(p);
    float moonSDF = moonDist - moonR;

    // 3D sphere normal dot product calculation for moon phases
    int ph = clamp(uPhase, 0, 7);
    const float PI = 3.14159265359;
    float theta = float(ph) * (PI / 4.0);
    // z is the depth of the 3D sphere surface
    float z = sqrt(max(0.0, moonR * moonR - p.x * p.x - p.y * p.y));
    
    // Dot product between surface normal and sun light direction vector
    float dotProd = p.x * sin(theta) - z * cos(theta);
    
    // Smooth lit phase transition
    float phaseAlpha = smoothstep(-0.01, 0.01, dotProd);
    
    // Smooth circle boundary for the moon shape
    float moonAlpha = smoothstep(0.01, -0.01, moonSDF);
    
    // Lit region: inside moon circle AND in the illuminated phase
    float litAlpha = moonAlpha * phaseAlpha;

    // Soft halo glow (visible even for new moon)
    float baseHalo = smoothstep(moonR + 0.22, moonR + 0.04, moonDist) * 0.28;

    float haloMultipliers[8];
    haloMultipliers[0] = 0.025;  // New moon: almost no halo outside
    haloMultipliers[1] = 0.10;
    haloMultipliers[2] = 0.25;
    haloMultipliers[3] = 0.40;
    haloMultipliers[4] = 0.50;   // Full moon: max 50% halo outside
    haloMultipliers[5] = 0.40;
    haloMultipliers[6] = 0.25;
    haloMultipliers[7] = 0.10;

    float haloAlpha = baseHalo;
    if (moonDist > moonR) {
        haloAlpha = baseHalo * haloMultipliers[ph];
    }

    // Limb darkening on lit surface
    float limb = moonDist / moonR;
    vec3 surfaceColor = mix(uMoonColor, uMoonColor * 0.65, limb * limb * 0.6);

    float totalAlpha = (litAlpha + haloAlpha * uHaloIntensity) * uIntensity;
    vec3  totalColor = mix(uMoonColor * 0.3, surfaceColor, litAlpha / max(totalAlpha, 0.001));

    // Thin, slightly transparent halo ring around the moon at full moon (ph == 4)
    float ringR = 0.62;
    float ringThickness = 0.008;
    float ringSDF = abs(moonDist - ringR);
    float ringAlpha = smoothstep(ringThickness + 0.008, ringThickness - 0.008, ringSDF);
    float wRing = (ph == 4) ? 1.0 : 0.0;
    float finalRingAlpha = ringAlpha * 0.35 * wRing * uHaloIntensity * uIntensity;

    // Alpha blend the ring on top of the moon color
    totalColor = mix(totalColor, uMoonColor, finalRingAlpha / max(totalAlpha + finalRingAlpha, 0.001));
    totalAlpha = totalAlpha + finalRingAlpha * (1.0 - totalAlpha);

    fragColor = vec4(totalColor, totalAlpha);
}
