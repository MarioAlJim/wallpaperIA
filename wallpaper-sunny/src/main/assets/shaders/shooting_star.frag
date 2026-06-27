#version 300 es
precision mediump float;

in vec2 vUV;
out vec4 fragColor;

uniform vec2 uTail;
uniform vec2 uHead;
uniform float uOpacity;
uniform float uAspect;
uniform vec3 uColor;

void main() {
    // Convert vUV to aspect-ratio corrected coordinates
    vec2 P = vec2((vUV.x - 0.5) * 2.0 * uAspect, (vUV.y - 0.5) * 2.0);

    // Segment from uTail to uHead
    vec2 A = uTail;
    vec2 B = uHead;
    vec2 AB = B - A;
    
    // Project P onto segment AB
    float abLenSq = dot(AB, AB);
    float t = 0.0;
    if (abLenSq > 0.0001) {
        t = clamp(dot(P - A, AB) / abLenSq, 0.0, 1.0);
    }
    vec2 C = A + t * AB;
    float d = length(P - C);

    // Tail width and fading
    // Weight (width) at head (t = 1.0) is thicker, at tail (t = 0.0) is thinner.
    // In OpenGL coordinates, typical line thickness is very small
    float r_tail = 0.0008;
    float r_head = 0.0025;
    float localRadius = mix(r_tail, r_head, t);

    // Tail alpha fades out as t goes to 0 (tail end)
    float edgeVal = smoothstep(localRadius + 0.0015, localRadius - 0.0015, d);
    float tailAlpha = t * 0.85 * edgeVal;

    // Head drawing at uHead (B)
    float distToHead = length(P - B);
    
    // External halo (diameter 25 in JS relative coordinates. Let's make it radius 0.024)
    float R_ext = 0.024;
    float extHalo = smoothstep(R_ext, R_ext * 0.2, distToHead) * 0.15;
    
    // Bright interior halo (diameter 14 in JS. Let's make it radius 0.013)
    float R_mid = 0.013;
    float midHalo = smoothstep(R_mid, R_mid * 0.2, distToHead) * 0.4;
    
    // Core (diameter 5.5 in JS. Let's make it radius 0.005)
    float R_core = 0.005;
    float coreVal = smoothstep(R_core, R_core * 0.1, distToHead);
    
    float headAlpha = max(extHalo, max(midHalo, coreVal));
    
    // Final alpha is maximum of tail and head alphas, modulated by global opacity
    float finalAlpha = max(tailAlpha, headAlpha) * uOpacity;
    
    if (finalAlpha <= 0.001) {
        discard;
    }
    
    // Tint color based on uColor, blending to white core
    vec3 color = mix(uColor, vec3(1.0, 1.0, 1.0), smoothstep(R_mid, 0.0, distToHead));
    
    fragColor = vec4(color, finalAlpha);
}
