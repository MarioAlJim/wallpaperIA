#version 300 es
precision highp float;

in vec2 vTexCoord;

uniform float uTime;
uniform float uAspectRatio;
uniform float uScreenDropletsEnabled;
uniform float uScreenDropletsSize;
uniform vec3 uRainColor;

out vec4 fragColor;

// This mask used to carry the on-screen droplets' lens-refraction offset for storm_cloud.frag
// to sample (so clouds behind wet glass would distort too). That refraction effect has been
// removed entirely — droplets are now shown purely via a color tint in background.frag, with
// no UV displacement — so there is nothing left for this pass to compute: it always writes the
// neutral value storm_cloud.frag already decodes back to zero distortion.
void main() {
    fragColor = vec4(0.5, 0.5, 0.0, 1.0);
}
