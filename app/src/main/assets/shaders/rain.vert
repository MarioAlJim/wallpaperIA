#version 300 es

layout(location = 0) in vec2 aPosition;

uniform mat4 uMVPMatrix;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
    gl_PointSize = 3.0;
}
