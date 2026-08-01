#version 300 es

layout(location = 0) in vec2 aPosition;

out vec2 vTexCoord;

void main() {
    // Maps the fullscreen quad's clip-space position directly to the same
    // "background space" UV (V=0 at top, V=1 at bottom) that storm_cloud.frag's
    // bgTexCoord and background.frag's vTexCoord both use.
    vTexCoord = vec2(aPosition.x * 0.5 + 0.5, 0.5 - aPosition.y * 0.5);
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
