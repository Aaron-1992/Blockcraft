#version 120

uniform vec3 sunPosition;
uniform int worldTime;
varying vec3 vViewPos;
varying vec4 vColor;

const float PI = 3.14159265359;

void main() {
    vec3 dir = normalize(vViewPos);
    vec3 sunDir = normalize(sunPosition);
    float h = sin(float(worldTime) * (2.0 * PI / 24000.0));
    float twilight = 1.0 - smoothstep(0.0, 0.30, abs(h));
    float horizon = 1.0 - smoothstep(0.02, 0.48, abs(dir.y));
    vec3 c = vColor.rgb;
    vec3 warm = vec3(1.0, 0.46, 0.24);
    c = mix(c, c * mix(vec3(1.0), warm, twilight * 0.24), horizon);
    float sunGlow = pow(max(dot(dir, sunDir), 0.0), 7.0) * (0.06 + 0.18 * twilight);
    c += warm * sunGlow;
    gl_FragColor = vec4(c, vColor.a);
}
