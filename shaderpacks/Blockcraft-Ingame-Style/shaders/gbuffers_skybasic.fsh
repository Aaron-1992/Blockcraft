#version 120

uniform vec3 sunPosition;
uniform vec4 fogColor;
uniform float rainStrength;
uniform int worldTime;
varying vec3 vViewPos;
varying vec4 vColor;

const float PI = 3.14159265359;

void main() {
    vec3 dir = normalize(vViewPos);
    vec3 sunDir = normalize(sunPosition);
    float h = sin(float(worldTime) * (2.0 * PI / 24000.0));
    float twilight = 1.0 - smoothstep(0.0, 0.30, abs(h));
    float day = clamp(0.5 + h * 1.3, 0.0, 1.0);
    float atten = 1.0 - 0.72 * rainStrength;

    // Blockcraft's Sky.base palette: the horizon also serves as the terrain fog colour.
    vec3 zenith = mix(vec3(0.01, 0.015, 0.04), vec3(0.40, 0.60, 0.98), day);
    vec3 horizon = mix(vec3(0.02, 0.025, 0.05), vec3(0.76, 0.85, 1.0), day);
    horizon = mix(horizon, vec3(1.0, 0.52, 0.30), twilight * 0.55);
    zenith = mix(zenith, vec3(0.40, 0.30, 0.58), twilight * 0.16);

    float skyLuma = dot(zenith, vec3(0.299, 0.587, 0.114));
    zenith = mix(vec3(skyLuma * 0.66, skyLuma * 0.71, skyLuma * 0.80), zenith, atten);
    skyLuma = dot(horizon, vec3(0.299, 0.587, 0.114));
    horizon = mix(vec3(skyLuma * 0.80, skyLuma * 0.84, skyLuma * 0.90), horizon, atten);
    horizon = mix(horizon, fogColor.rgb, 0.42);

    float vertical = pow(max(dir.y, 0.0), 0.6);
    vec3 color = mix(horizon, zenith, vertical);

    float sunAmount = clamp((sunDir.y + 0.10) / 0.30, 0.0, 1.0) * atten;
    vec3 glowColor = vec3(1.0, 0.80 - 0.26 * twilight, 0.55 - 0.26 * twilight);
    float sunGlow = pow(max(dot(dir, sunDir), 0.0), 5.0) * (0.08 + 0.20 * twilight) * sunAmount;
    color += glowColor * sunGlow;

    // The sunset belt, pink arch and blue earth-shadow band are directional in Blockcraft.
    float lift = smoothstep(0.0, 0.06, dir.y);
    float horizontalLength = length(dir.xz);
    float sunHorizontalLength = length(sunDir.xz);
    float azimuth = 0.0;
    if (horizontalLength > 0.0001 && sunHorizontalLength > 0.0001) {
        azimuth = dot(dir.xz, sunDir.xz) / (horizontalLength * sunHorizontalLength);
    }
    float side = max(azimuth, 0.0);
    float anti = max(-azimuth, 0.0);
    float belt = twilight * atten * lift * exp(-(dir.y * dir.y) / (2.0 * 0.17 * 0.17)) * (0.20 + 0.80 * side * side);
    color = mix(color, vec3(1.0, 0.50, 0.20), belt * 0.60);
    float arch = twilight * atten * lift * anti * exp(-((dir.y - 0.20) * (dir.y - 0.20)) / (2.0 * 0.11 * 0.11));
    color = mix(color, vec3(0.80, 0.52, 0.72), arch * 0.38);
    float earthShadow = twilight * atten * lift * anti * exp(-((dir.y - 0.03) * (dir.y - 0.03)) / (2.0 * 0.05 * 0.05));
    color = mix(color, vec3(0.20, 0.26, 0.46), earthShadow * 0.40);

    float night = 1.0 - day;
    float moonGlow = pow(max(dot(dir, -sunDir), 0.0), 7.0) * 0.10 * night * lift;
    color += vec3(0.50, 0.62, 0.95) * moonGlow;
    gl_FragColor = vec4(max(color, vec3(0.0)), vColor.a);
}
