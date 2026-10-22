#version 120

varying vec3 vViewPos;
varying vec4 vColor;

void main() {
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    vViewPos = viewPos.xyz;
    vColor = gl_Color;
    gl_Position = gl_ProjectionMatrix * viewPos;
}
