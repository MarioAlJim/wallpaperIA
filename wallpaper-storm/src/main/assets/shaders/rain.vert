#version 300 es

layout(location = 0) in vec2 aPosition;
layout(location = 1) in float aDepth;
layout(location = 2) in vec2 aTexCoord;

uniform mat4 uMVPMatrix;

out float vDepth;
out vec2 vTexCoord;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
    vDepth = aDepth;
    vTexCoord = aTexCoord;
}
