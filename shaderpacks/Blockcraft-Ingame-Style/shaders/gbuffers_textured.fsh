#version 120

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform sampler2D shadowtex0;
uniform vec3 sunPosition;
uniform vec4 fogColor;
uniform float fogStart;
uniform float fogEnd;
uniform int worldTime;

varying vec2 vTexCoord;
varying vec2 vLightCoord;
varying vec4 vColor;
varying vec3 vViewPos;
varying vec3 vViewNormal;
varying vec4 vShadowPos;

const float PI = 3.14159265359;
const float SHADOW_TEXEL = 1.0 / 2048.0;

float shadowVisibility(vec3 N, vec3 L) {
    if (vShadowPos.w <= 0.0) return 1.0;
    vec3 coord = (vShadowPos.xyz / vShadowPos.w) * 0.5 + 0.5;
    if (coord.x <= 0.001 || coord.x >= 0.999 || coord.y <= 0.001 || coord.y >= 0.999 || coord.z <= 0.0 || coord.z >= 1.0) return 1.0;
    float bias = 0.0005 + 0.0015 * (1.0 - clamp(dot(N, L), 0.0, 1.0));
    float sum = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float d = texture2D(shadowtex0, coord.xy + vec2(float(x), float(y)) * SHADOW_TEXEL).r;
            sum += (coord.z - bias <= d) ? 1.0 : 0.0;
        }
    }
    return sum / 9.0;
}

void main() {
    vec4 texel = texture2D(texture, vTexCoord) * vColor;
    if (texel.a < 0.10) discard;

    vec3 N = normalize(vViewNormal);
    if (!gl_FrontFacing) N = -N;
    vec3 L = normalize(sunPosition);
    float h = sin(float(worldTime) * (2.0 * PI / 24000.0));
    float sunUp = smoothstep(-0.10, 0.15, h);
    float ndl = clamp(dot(N, L), 0.0, 1.0);
    float direct = shadowVisibility(N, L) * mix(0.55, 1.0, smoothstep(0.0, 0.35, ndl));
    float skyAmt = smoothstep(0.55, 0.97, vLightCoord.x) * sunUp;

    vec3 sunTint = mix(vec3(1.0, 0.70, 0.46), vec3(1.0, 0.975, 0.93), clamp(h * 3.0, 0.0, 1.0));
    vec3 shadeTint = vec3(0.45, 0.53, 0.78);
    vec3 shade = mix(vec3(1.0), mix(shadeTint, sunTint * 1.04, direct), skyAmt);
    vec3 blockLight = texture2D(lightmap, clamp(vLightCoord, 0.0, 1.0)).rgb;
    vec3 color = texel.rgb * blockLight * shade;

    float d = length(vViewPos);
    float fogFactor = clamp((fogEnd - d) / max(0.001, fogEnd - fogStart), 0.0, 1.0);
    float twilight = 1.0 - smoothstep(0.0, 0.30, abs(h));
    float glow = pow(max(dot(normalize(vViewPos), L), 0.0), 5.0) * (0.08 + 0.20 * twilight) * sunUp;
    vec3 warmFog = fogColor.rgb + vec3(1.0, 0.54, 0.29) * glow;
    color = mix(warmFog, color, fogFactor);

    gl_FragColor = vec4(max(color, vec3(0.0)), texel.a);
}
