#version 300 es
precision mediump float;

in vec2 vTexCoord;

uniform float uTime;
uniform float uFlapSpeed;
uniform float uFlapOffset;
uniform float uOpacity;

out vec4 fragColor;

void main() {
    // Normalize coordinates: uv in [-1.0, 1.0]
    vec2 uv = vTexCoord * 2.0 - 1.0;
    
    // Smooth flapping wing motion
    float flap = sin(uTime * uFlapSpeed + uFlapOffset);
    
    // V-shaped flapping wing curve
    float wingY = flap * (1.0 - abs(uv.x)) * 0.35 + abs(uv.x) * 0.15;
    
    // Thickness: thick body in the center, tapering to wingtips
    float bodyGlow = exp(-12.0 * uv.x * uv.x);
    float thickness = mix(0.03, 0.11, bodyGlow);
    
    // Distance from the wing curve
    float dist = abs(uv.y - wingY);
    
    // Smooth rendering of the bird silhouette
    float alpha = smoothstep(thickness, thickness - 0.03, dist);
    
    // Taper at the very edge of the wings
    alpha *= smoothstep(1.0, 0.95, 1.0 - abs(uv.x));
    
    if (alpha <= 0.0) {
        discard;
    }
    
    // Birds are dark silhouettes (dark blue-gray)
    vec3 birdColor = vec3(0.04, 0.05, 0.08);
    
    fragColor = vec4(birdColor, alpha * uOpacity);
}
