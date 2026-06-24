#version 300 es
precision mediump float;

in vec2 vUV;
uniform float uIntensity;

out vec4 fragColor;

void main() {
    // Night sky gradient (dark blue-purple)
    vec3 nightSkyTop    = vec3(0.01, 0.01, 0.06);
    vec3 nightSkyBottom = vec3(0.04, 0.02, 0.12);
    vec3 nightSky = mix(nightSkyBottom, nightSkyTop, vUV.y);
    fragColor = vec4(nightSky, uIntensity);
}
