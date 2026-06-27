#version 300 es
precision mediump float;

in vec2 vPosition; // NDC coordinates from -1.0 to 1.0
out vec4 fragColor;

uniform vec2 uMoonPos;      // Moon position on screen (-1.0 to 1.0)
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
    
    // Scale moon position by uAspectRatio to match screen coordinates
    vec2 moonPos = uMoonPos * uAspectRatio;

    // Apply swipe offset to the apparent position of the moon for lens flare
    vec2 flareMoonPos = moonPos + vec2(uSwipeOffset * 0.25 * uAspectRatio, 0.0);

    vec2 center = vec2(0.0, 0.0);
    vec2 dir = center - flareMoonPos; // Optical axis vector

    vec3 color = vec3(0.0);

    // 1. Central ray/streak (anamorphic streak passing through the moon)
    vec2 dirNorm = normalize(dir);
    vec2 streakDir = vec2(-dirNorm.y, dirNorm.x);
    float distToStreak = abs(dot(uv - flareMoonPos, streakDir));
    float streak = smoothstep(0.015, 0.0, distToStreak) * exp(-distance(uv, flareMoonPos) * 1.8);
    // Cool lunar colors: light blue/cyan (0.65, 0.85, 1.0)
    color += streak * vec3(0.65, 0.85, 1.0) * 0.35;

    // 2. Flare Elements along the optical axis: flareMoonPos + dir * t
    // Element A: Soft cyan halo behind the moon (t = -0.2)
    color += drawSpot(uv, flareMoonPos + dir * -0.2, 0.22, 0.22) * vec3(0.3, 0.6, 0.9) * 0.12;

    // Element B: Moon-centered secondary soft glow (t = 0.0)
    color += drawSpot(uv, flareMoonPos, 0.15, 0.15) * vec3(0.7, 0.9, 1.0) * 0.18;

    // Element C: Main halo ring centered at t = 0.4
    color += drawRing(uv, flareMoonPos + dir * 0.4, 0.28, 0.012, 0.07) * vec3(0.4, 0.6, 1.0) * 0.22;

    // Element D: Small bright cyan-white spot (t = 0.7)
    color += drawSpot(uv, flareMoonPos + dir * 0.7, 0.035, 0.015) * vec3(0.8, 0.95, 1.0) * 0.35;

    // Element E: Small violet spot (t = 0.9)
    color += drawSpot(uv, flareMoonPos + dir * 0.9, 0.022, 0.012) * vec3(0.6, 0.4, 0.9) * 0.4;

    // Element F: Medium soft blue-green spot (t = 1.25)
    color += drawSpot(uv, flareMoonPos + dir * 1.25, 0.08, 0.06) * vec3(0.3, 0.75, 0.65) * 0.2;

    // Element G: Large soft blue-violet spot (t = 1.5)
    color += drawSpot(uv, flareMoonPos + dir * 1.5, 0.20, 0.16) * vec3(0.4, 0.4, 0.85) * 0.1;

    // Apply intensity and screen boundaries fade
    float edgeFade = smoothstep(1.0, 0.8, abs(vPosition.x)) * smoothstep(1.0, 0.8, abs(vPosition.y));
    
    vec3 finalColor = color * uIntensity * edgeFade;

    float maxVal = max(finalColor.r, max(finalColor.g, finalColor.b));
    if (maxVal < 0.001) {
        discard;
    }
    fragColor = vec4(finalColor, maxVal);
}
