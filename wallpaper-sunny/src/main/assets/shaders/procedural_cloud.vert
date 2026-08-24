#version 300 es

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

// Instanced attributes (divisor = 1)
layout(location = 2) in vec2 aInstancePos;
layout(location = 3) in float aInstanceScale;
layout(location = 4) in float aInstanceOpacity;
layout(location = 5) in float aInstanceVariation;

uniform mat4 uMVPMatrix; // Acts as projectionMatrix here

out vec2 vTexCoord;
out float vOpacity;
out float vVariation;
out vec2 vCloudPos;
out float vCloudScale;

void main() {
    vTexCoord = aTexCoord;
    vOpacity = aInstanceOpacity;
    vVariation = aInstanceVariation;
    vCloudPos = aInstancePos;
    vCloudScale = aInstanceScale;
    
    // Scale and translate the quad in world coordinates before applying projection
    vec2 scaledPos = aPosition * vec2(aInstanceScale * 2.4f, aInstanceScale);
    vec2 worldPos = scaledPos + aInstancePos;
    
    gl_Position = uMVPMatrix * vec4(worldPos, 0.0, 1.0);
}
