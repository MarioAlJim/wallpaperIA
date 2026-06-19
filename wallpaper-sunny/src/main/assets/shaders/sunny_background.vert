#version 300 es

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

uniform mat4 uMVPMatrix;

out vec2 vTexCoord;
out vec2 vScreenPos;

void main() {
    vTexCoord = aTexCoord;
    vec4 pos = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
    vScreenPos = pos.xy / pos.w;
    gl_Position = pos;
}
