#version 120

uniform sampler2D texture;
uniform vec3 sunPosition;
uniform vec4 fogColor;
uniform float rainStrength;
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
    float atten = 1.0 - 0.72 * rainStrength;
    float horizon = 1.0 - smoothstep(0.02, 0.48, abs(dir.y));
    float lift = smoothstep(0.0, 0.06, dir.y);
    float azimuth = 0.0;
    float horizontalLength = length(dir.xz);
    float sunHorizontalLength = length(sunDir.xz);
    if (horizontalLength > 0.0001 && sunHorizontalLength > 0.0001) {
        azimuth = dot(dir.xz, sunDir.xz) / (horizontalLength * sunHorizontalLength);
    }

    // Keep stars and sun/moon artwork intact; tint only the lower sky, with Blockcraft's dusk directions.
    vec3 color = texel.rgb;
    vec3 warm = vec3(1.0, 0.52, 0.30);
    color = mix(color, color * mix(vec3(1.0), warm, twilight * 0.22 * atten), horizon);
    float belt = twilight * atten * lift * exp(-(dir.y * dir.y) / (2.0 * 0.17 * 0.17))
        * (0.20 + 0.80 * max(azimuth, 0.0) * max(azimuth, 0.0));
    color = mix(color, color * vec3(1.0, 0.76, 0.64), belt * 0.24);
    float arch = twilight * atten * lift * max(-azimuth, 0.0)
        * exp(-((dir.y - 0.20) * (dir.y - 0.20)) / (2.0 * 0.11 * 0.11));
    color = mix(color, color * vec3(0.92, 0.78, 0.94), arch * 0.16);

    vec3 horizonColor = mix(fogColor.rgb, vec3(1.0, 0.52, 0.30), twilight * 0.55);
    float sunAmount = clamp((sunDir.y + 0.10) / 0.30, 0.0, 1.0) * atten;
    vec3 glowColor = vec3(1.0, 0.80 - 0.26 * twilight, 0.55 - 0.26 * twilight);
    float sunGlow = pow(max(dot(dir, sunDir), 0.0), 7.0) * (0.06 + 0.18 * twilight) * sunAmount;
    color += mix(horizonColor, glowColor, 0.70) * sunGlow;

    gl_FragColor = vec4(max(color, vec3(0.0)), texel.a);
}
