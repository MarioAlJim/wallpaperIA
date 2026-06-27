#version 300 es

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

uniform mat4 uMVPMatrix;

out vec2 vTexCoord;
out vec4 vScreenPos; // Pass screen position to fragment shader

void main() {
    vTexCoord = aTexCoord;
    gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
    vScreenPos = gl_Position;
}
