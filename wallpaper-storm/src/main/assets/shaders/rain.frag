#version 300 es

precision mediump float;

uniform vec4 uRainColor;

in float vDepth;
in vec2 vTexCoord;

out vec4 fragColor;

void main() {
    // Map local texture coordinate x from [0, 1] to [-1, 1]
    float x = vTexCoord.x * 2.0 - 1.0;
    // Local texture coordinate y goes from 0 (tail) to 1 (head)
    float y = vTexCoord.y;
    
    // Parameters for procedural teardrop shape
    float R = 0.35; // Radius of the circular head
    float headCenterY = 1.0 - R;
    
    float dist = 0.0;
    if (y > headCenterY) {
        // Semicircular head of the raindrop
        float dx = x;
        float dy = y - headCenterY;
        dist = sqrt(dx * dx + dy * dy) / R;
    } else {
        // Tapered tail of the raindrop
        // Avoid division by zero when y is close to 0
        float width = R * pow(max(y, 0.001) / headCenterY, 1.5);
        dist = abs(x) / width;
    }
    
    // Smooth boundary check using smoothstep for anti-aliasing
    float edgeAlpha = 1.0 - smoothstep(0.8, 1.0, dist);
    
    // Vertical gradient: tip (y=1) is saturated/opaque, tail (y=0) is transparent
    float verticalGradient = pow(y, 0.8);
    
    float alpha = uRainColor.a * vDepth * edgeAlpha * verticalGradient;
    
    fragColor = vec4(uRainColor.rgb, alpha);
}
