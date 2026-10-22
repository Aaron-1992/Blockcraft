#version 120

varying vec2 vTexCoord;
varying vec4 vColor;

void main() {
    vTexCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    vColor = gl_Color;
    gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
}
