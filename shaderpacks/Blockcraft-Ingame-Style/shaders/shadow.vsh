#version 120

// Stabilize the shadow camera and allocate more map precision near the player.
const float shadowDistance = 96.0;
const float shadowDistanceRenderMul = 1.0;
const float shadowIntervalSize = 1.0;
const float sunPathRotation = -40.0;

varying vec2 vTexCoord;
varying vec4 vColor;

vec3 distortShadowClipPos(vec3 p) {
    float radius = length(p.xy);
    float factor = radius + 0.10;
    p.xy /= factor;
    p.z *= 0.5;
    return p;
}

void main() {
    vTexCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    vColor = gl_Color;
    gl_Position = ftransform();
    gl_Position.xyz = distortShadowClipPos(gl_Position.xyz);
}
