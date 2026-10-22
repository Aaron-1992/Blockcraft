#version 120

uniform vec3 sunPosition;
uniform int worldTime;
varying vec3 vViewPos;
varying vec3 vNormal;
varying vec4 vColor;

const float PI = 3.14159265359;

void main() {
    vec3 N = normalize(vNormal);
    if (!gl_FrontFacing) N = -N;
    vec3 L = normalize(sunPosition);
    float h = sin(float(worldTime) * (2.0 * PI / 24000.0));
    float day = smoothstep(-0.10, 0.15, h);
    float ndl = max(dot(N, L), 0.0);
    vec3 shade = mix(vec3(0.47, 0.55, 0.76), vec3(1.0), day * (0.48 + 0.52 * ndl));
    vec3 sunset = vec3(1.0, 0.60, 0.38) * (1.0 - smoothstep(0.0, 0.28, abs(h))) * 0.16;
    vec3 color = vColor.rgb * shade + sunset;
    gl_FragColor = vec4(color, vColor.a);
}
