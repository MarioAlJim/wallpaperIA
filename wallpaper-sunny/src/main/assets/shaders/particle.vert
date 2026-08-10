#version 300 es
// Shared instanced vertex shader for particle.frag and firefly.frag: the quad geometry
// (aPosition/aTexCoord) is the same for every instance (attribute divisor 0), while
// per-instance position/scale/color/opacity travel as divisor-1 attributes uploaded
// once per frame for the whole batch, enabling a single glDrawArraysInstanced call
// instead of one glDrawArrays + uniform update per particle/firefly.
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec2 aInstancePos;
layout(location = 3) in float aInstanceScale;
layout(location = 4) in vec3 aInstanceColor;
layout(location = 5) in float aInstanceOpacity;

uniform mat4 uProjectionMatrix;

out vec2 vTexCoord;
out vec3 vColor;
out float vOpacity;

void main() {
    vTexCoord = aTexCoord;
    vColor = aInstanceColor;
    vOpacity = aInstanceOpacity;
    vec2 worldPos = aInstancePos + aPosition * aInstanceScale;
    gl_Position = uProjectionMatrix * vec4(worldPos, 0.0, 1.0);
}
