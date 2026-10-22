#version 120

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

varying vec2 vTexCoord;
varying vec2 vLightCoord;
varying vec4 vColor;
varying vec3 vViewPos;
varying vec3 vViewNormal;
varying vec3 vWorldPos;
varying vec3 vWorldRel;
varying vec4 vShadowPos;

vec3 distortShadowClipPos(vec3 p) {
    float radius = length(p.xy);
    float factor = radius + 0.10;
    p.xy /= factor;
    p.z *= 0.5;
    return p;
}

void main() {
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 worldRel = gbufferModelViewInverse * viewPos;
    vec3 worldPos = worldRel.xyz + cameraPosition;

    float t = frameTimeCounter;
    float wave = (sin(t * 1.3 + worldPos.x * 1.1 + worldPos.z * 0.4)
                + cos(t * 1.1 + worldPos.z * 1.3 - worldPos.x * 0.5)) * 0.011;
    worldRel.y += wave;
    viewPos = gbufferModelView * worldRel;

    vTexCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    vLightCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    vColor = gl_Color;
    vViewPos = viewPos.xyz;
    vViewNormal = normalize(gl_NormalMatrix * gl_Normal);
    vWorldPos = worldPos + vec3(0.0, wave, 0.0);
    vWorldRel = worldRel.xyz;
    vShadowPos = shadowProjection * shadowModelView * worldRel;
    vShadowPos.xyz = distortShadowClipPos(vShadowPos.xyz);

    gl_Position = gl_ProjectionMatrix * viewPos;
}
