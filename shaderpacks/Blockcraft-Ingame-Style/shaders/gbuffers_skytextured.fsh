#version 120

uniform sampler2D texture;
uniform vec3 sunPosition;
uniform int worldTime;
varying vec2 vTexCoord;
varying vec3 vViewPos;
varying vec4 vColor;

const float PI = 3.14159265359;

void main() {
    vec4 texel = texture2D(texture, vTexCoord) * vColor;
    if (texel.a < 0.01) discard;
    vec3 dir = normalize(vViewPos);
    vec3 sunDir = normalize(sunPosition);
    float h = sin(float(worldTime) * (2.0 * PI / 24000.0));
    float twilight = 1.0 - smoothstep(0.0, 0.30, abs(h));
    float horizon = 1.0 - smoothstep(0.02, 0.48, abs(dir.y));
    vec3 warm = vec3(1.0, 0.52, 0.31);
    vec3 c = texel.rgb * mix(vec3(1.0), warm, horizon * twilight * 0.20);
    c += warm * pow(max(dot(dir, sunDir), 0.0), 12.0) * (0.05 + 0.12 * twilight);
    gl_FragColor = vec4(c, texel.a);
}
