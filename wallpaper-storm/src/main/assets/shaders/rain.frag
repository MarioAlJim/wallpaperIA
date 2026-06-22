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
    
    // Parameters for the teardrop shape
    float R = 0.40; // Radius of the circular head (adjusts thickness)
    float y_c = 1.0 - R; // Center of the circular head
    
    float width = 0.0;
    vec3 normal = vec3(0.0, 0.0, 1.0);
    
    if (y > y_c) {
        // 1. Semicircular head (perfect circle in texture coordinates)
        float dy = y - y_c;
        width = sqrt(max(0.0, R * R - dy * dy));
        
        // Spherical normal calculation for the head
        float r2 = (x * x + dy * dy) / (R * R);
        float nz = sqrt(max(0.0, 1.0 - r2));
        normal = vec3(x / R, dy / R, nz);
    } else {
        // 2. Tapered tail (pointy, curving smoothly into the head)
        // Uses a sinus-power taper to make it very thin and pointy at the end (y=0)
        // while maintaining a smooth derivative transition at the head interface (y=y_c)
        float normalizedY = y / y_c;
        width = R * sin(1.57079632679 * pow(normalizedY, 2.0));
        
        // Cylindrical normal calculation for the tail
        float x_norm = x / max(width, 0.001);
        float nz = sqrt(max(0.0, 1.0 - x_norm * x_norm));
        normal = vec3(x_norm, 0.0, nz);
    }
    
    // Calculate distance from center line normalized by local width
    float dist = abs(x) / max(width, 0.001);
    
    // Soft horizontal profile for the raindrop's edges (anti-aliased)
    float edgeAlpha = 1.0 - smoothstep(0.7, 1.0, dist);
    
    // Specular highlight representing light reflection on the water droplet surface.
    // Light is coming slightly from top-right-front.
    vec3 lightDir = normalize(vec3(0.3, 0.5, 1.0));
    vec3 viewDir = vec3(0.0, 0.0, 1.0);
    vec3 halfDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfDir), 0.0), 20.0);
    
    // Fresnel reflection at grazing angles (edges of the droplet)
    float fresnel = pow(1.0 - normal.z, 3.0);
    
    // Vertical gradient: tip (y=1) is saturated/opaque, tail (y=0) is transparent
    float verticalGradient = pow(y, 0.9);
    
    // Combine base color, specular highlight, and fresnel reflection
    vec3 baseColor = uRainColor.rgb;
    vec3 highlightColor = vec3(1.0);
    vec3 finalColor = mix(baseColor, highlightColor, spec * 0.7) + vec3(0.3) * fresnel * baseColor;
    
    // Center glow: water acts as a lens, focusing light in the center (normal.z is highest at center)
    float lensGlow = 0.5 + 0.5 * normal.z;
    
    // Calculate final alpha
    float alpha = uRainColor.a * vDepth * edgeAlpha * verticalGradient * lensGlow;
    
    // Output the fragment color
    fragColor = vec4(finalColor, alpha);
}
