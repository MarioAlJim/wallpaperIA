#version 300 es
precision mediump float;

in vec4 vColor;
out vec4 fragColor;

void main() {
    // Generate soft circular star point
    vec2 circCoord = gl_PointCoord - vec2(0.5);
    float dist = length(circCoord);
    
    // Smooth step to fade out towards the edges of the point
    float alpha = smoothstep(0.5, 0.15, dist);
    
    if (alpha <= 0.0) {
        discard;
    }
    
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}
