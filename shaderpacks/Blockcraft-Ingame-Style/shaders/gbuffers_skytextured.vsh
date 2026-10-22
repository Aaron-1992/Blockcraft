#version 120

varying vec2 vTexCoord;
varying vec3 vViewPos;
varying vec4 vColor;

void main() {
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vViewPos = viewPos.xyz;
    vTexCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    vColor = gl_Color;
    gl_Position = gl_ProjectionMatrix * viewPos;
}
