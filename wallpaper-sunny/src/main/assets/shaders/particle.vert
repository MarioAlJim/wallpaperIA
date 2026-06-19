#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;
uniform mat4 uMVPMatrix;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
}
