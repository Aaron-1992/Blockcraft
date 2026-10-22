#version 120

uniform sampler2D texture;
varying vec2 vTexCoord;
varying vec4 vColor;

void main() {
    float alpha = texture2D(texture, vTexCoord).a * vColor.a;
    if (alpha < 0.10) discard;
    gl_FragColor = vec4(1.0);
}
