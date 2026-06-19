#version 300 es
precision mediump float;

in vec2 vPosition; // NDC coordinates from -1.0 to 1.0
out vec4 fragColor;

uniform vec2 uSunPos;      // Sun position on screen (-1.0 to 1.0)
uniform float uAspectRatio;
uniform float uSwipeOffset; // Swipe offset (-1.0 to 1.0)
uniform float uIntensity;   // Overall intensity (0.0 to 1.0)

// Function to draw a soft circular spot
float drawSpot(vec2 uv, vec2 center, float radius, float blur) {
    float d = distance(uv, center);
    return smoothstep(radius, radius - blur, d);
}

// Function to draw a ring/halo
float drawRing(vec2 uv, vec2 center, float radius, float thickness, float blur) {
    float d = distance(uv, center);
    return smoothstep(thickness + blur, thickness, abs(d - radius));
}

void main() {
    if (uIntensity <= 0.0) {
        discard;
    }

    vec2 uv = vec2(vPosition.x * uAspectRatio, vPosition.y);
    
    // Scale sun position by uAspectRatio to match screen coordinates
    vec2 sunPos = uSunPos * uAspectRatio;

    // Apply swipe offset to the apparent position of the sun for lens flare
    // Moving the flare center in the opposite direction of sun position shift.
    vec2 flareSunPos = sunPos + vec2(uSwipeOffset * 0.25 * uAspectRatio, 0.0);

    vec2 center = vec2(0.0, 0.0);
    vec2 dir = center - flareSunPos; // Optical axis vector

    vec3 color = vec3(0.0);

    // 1. Central ray/streak (anamorphic streak passing through the sun)
    vec2 dirNorm = normalize(dir);
    vec2 streakDir = vec2(-dirNorm.y, dirNorm.x);
    float distToStreak = abs(dot(uv - flareSunPos, streakDir));
    float streak = smoothstep(0.02, 0.0, distToStreak) * exp(-distance(uv, flareSunPos) * 1.5);
    color += streak * vec3(1.0, 0.70, 0.35) * 0.40;

    // 2. Flare Elements along the optical axis: flareSunPos + dir * t
    // Element A: Soft red/pink halo behind the sun (t = -0.3)
    color += drawSpot(uv, flareSunPos + dir * -0.3, 0.25, 0.25) * vec3(0.8, 0.15, 0.3) * 0.15;

    // Element B: Sun-centered secondary glow (t = 0.0)
    color += drawSpot(uv, flareSunPos, 0.18, 0.15) * vec3(1.0, 0.8, 0.4) * 0.2;

    // Element C: Main halo ring centered at t = 0.5
    color += drawRing(uv, flareSunPos + dir * 0.5, 0.32, 0.015, 0.08) * vec3(0.6, 0.3, 0.8) * 0.25;

    // Element D: Small bright yellow spot (t = 0.75)
    color += drawSpot(uv, flareSunPos + dir * 0.75, 0.04, 0.02) * vec3(1.0, 0.9, 0.3) * 0.4;

    // Element E: Small blue-green spot (t = 0.95)
    color += drawSpot(uv, flareSunPos + dir * 0.95, 0.025, 0.015) * vec3(0.2, 0.7, 0.9) * 0.5;

    // Element F: Medium soft green spot (t = 1.3)
    color += drawSpot(uv, flareSunPos + dir * 1.3, 0.09, 0.07) * vec3(0.3, 0.8, 0.5) * 0.25;

    // Element G: Large soft violet spot (t = 1.6)
    color += drawSpot(uv, flareSunPos + dir * 1.6, 0.22, 0.18) * vec3(0.5, 0.3, 0.8) * 0.12;

    // Apply intensity and screen boundaries fade
    // Fade flare near the screen edges to avoid harsh clipping
    float edgeFade = smoothstep(1.0, 0.8, abs(vPosition.x)) * smoothstep(1.0, 0.8, abs(vPosition.y));
    
    vec3 finalColor = color * uIntensity * edgeFade;

    float maxVal = max(finalColor.r, max(finalColor.g, finalColor.b));
    if (maxVal < 0.001) {
        discard;
    }
    fragColor = vec4(finalColor, maxVal);
}
