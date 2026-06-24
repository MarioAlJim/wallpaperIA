#version 300 es
precision mediump float;

in vec4 vColor;
out vec4 fragColor;

void main() {
    // Center coordinate from -0.5 to 0.5
    vec2 p = gl_PointCoord - vec2(0.5);
    
    // 4-pointed astroid distance function
    float d = pow(abs(p.x), 0.5) + pow(abs(p.y), 0.5);
    float astroidGlow = smoothstep(0.65, 0.2, d);
    
    // Central circular core glow to prevent empty center
    float circularGlow = smoothstep(0.3, 0.0, length(p));
    
    float alpha = max(astroidGlow, circularGlow);
    
    if (alpha <= 0.0) {
        discard;
    }
    
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}
