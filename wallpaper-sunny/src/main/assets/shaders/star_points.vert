#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in float aSize;
layout(location = 2) in vec4 aColor;

uniform vec2 uOffset;

out vec4 vColor;

void main() {
    gl_Position = vec4(aPosition + uOffset, 0.0, 1.0);
    gl_PointSize = aSize;
    vColor = aColor;
}
