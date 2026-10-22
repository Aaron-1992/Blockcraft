#version 120

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform sampler2D depthtex0;
uniform sampler2D shadowtex0;
uniform vec3 sunPosition;
uniform mat4 gbufferModelViewInverse;
uniform vec4 fogColor;
uniform float fogStart;
uniform float fogEnd;
uniform float near;
uniform float far;
uniform float viewWidth;
uniform float viewHeight;
uniform float frameTimeCounter;
uniform float wetness;
uniform int worldTime;

varying vec2 vTexCoord;
varying vec2 vLightCoord;
varying vec4 vColor;
varying vec3 vViewPos;
varying vec3 vViewNormal;
varying vec3 vWorldPos;
varying vec3 vWorldRel;
varying vec4 vShadowPos;

const float PI = 3.14159265359;
const int shadowMapResolution = 2048;
const bool shadowtex0Nearest = true;

float dayHeight() {
    return sin(float(worldTime) * (2.0 * PI / 24000.0));
}

float linearDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * near * far) / max(0.0001, far + near - z * (far - near));
}

vec2 waveGradient(vec2 p, float t) {
    vec2 gradient = 0.34 * cos(dot(p, vec2(0.73, 0.31)) + t * 1.08) * vec2(0.73, 0.31);
    gradient += 0.26 * cos(dot(p, vec2(-0.21, 0.91)) - t * 0.83) * vec2(-0.21, 0.91);
    gradient += 0.18 * cos(dot(p, vec2(1.71, -1.17)) + t * 1.35) * vec2(1.71, -1.17);
    gradient += 0.12 * cos(dot(p, vec2(-1.34, -0.45)) - t * 1.17) * vec2(-1.34, -0.45);
    return gradient;
}

float shadowVisibility(vec3 normal, vec3 lightDir) {
    if (vShadowPos.w <= 0.0) return 1.0;

    vec3 shadowNdc = vShadowPos.xyz / vShadowPos.w;
    vec3 shadowCoord = shadowNdc * 0.5 + 0.5;
    if (shadowCoord.x <= 0.001 || shadowCoord.x >= 0.999 ||
        shadowCoord.y <= 0.001 || shadowCoord.y >= 0.999 ||
        shadowCoord.z <= 0.0 || shadowCoord.z >= 1.0) return 1.0;

    float nDotL = clamp(dot(normal, lightDir), 0.0, 1.0);
    float bias = 0.00065 + 0.0017 * (1.0 - nDotL);
    vec2 texelSize = vec2(1.0 / float(shadowMapResolution));
    vec2 minUV = texelSize * 0.5;
    vec2 maxUV = vec2(1.0) - minUV;
    float visibility = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize * 1.5;
            vec2 sampleUV = clamp(shadowCoord.xy + offset, minUV, maxUV);
            float storedDepth = texture2D(shadowtex0, sampleUV).r;
            visibility += step(shadowCoord.z - bias, storedDepth);
        }
    }
    return visibility / 9.0;
}

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec3 fogWithSun(vec3 fog, vec3 rel, vec3 lightDir, float h, float sunUp) {
    float twilight = 1.0 - smoothstep(0.0, 0.30, abs(h));
    vec3 glowColor = vec3(1.0, 0.80 - 0.26 * twilight, 0.55 - 0.26 * twilight);
    float glow = (0.08 + 0.20 * twilight) * pow(max(dot(normalize(rel), lightDir), 0.0), 5.0) * sunUp;
    return fog + glowColor * glow;
}

void main() {
    vec4 texel = texture2D(texture, vTexCoord) * vColor;
    if (texel.a < 0.02) discard;

    vec3 N = normalize(vViewNormal);
    if (!gl_FrontFacing) N = -N;
    float t = frameTimeCounter;
    float d = length(vViewPos);
    float fade = 1.0 - smoothstep(24.0, 150.0, d);
    vec2 wavePos = vWorldPos.xz;
    vec2 slope = waveGradient(wavePos, t);
    N = normalize(N + vec3(-slope.x, 0.0, -slope.y) * 0.075 * fade);

    vec3 L = normalize(sunPosition);
    vec3 V = normalize(-vViewPos);
    float h = dayHeight();
    float sunUp = clamp((h - 0.02) / 0.25, 0.0, 1.0);
    float ndl = max(dot(N, L), 0.0);
    float shadow = shadowVisibility(N, L);
    float lit = shadow * mix(0.55, 1.0, smoothstep(0.0, 0.35, ndl));
    vec3 light = texture2D(lightmap, clamp(vLightCoord, 0.0, 1.0)).rgb;
    float lightAmt = clamp(max(light.r, max(light.g, light.b)), 0.04, 1.0);
    float skyExposure = smoothstep(0.45, 0.95, vLightCoord.y);

    vec2 screenUv = gl_FragCoord.xy / max(vec2(viewWidth, viewHeight), vec2(1.0));
    float sceneZ = texture2D(depthtex0, screenUv).r;
    float surfaceZ = linearDepth(gl_FragCoord.z);
    float behindZ = sceneZ < 0.99995 ? linearDepth(sceneZ) : surfaceZ + 40.0;
    float thickness = min(max(behindZ - surfaceZ, 0.0), 40.0);

    // OptiFine/Iris do not expose Blockcraft's scene-color copy, so this uses depth absorption and sky reflection.
    vec3 absorb = exp(-thickness * vec3(0.42, 0.15, 0.065));
    vec3 waterBody = vec3(0.028, 0.17, 0.32) * lightAmt * mix(0.52, 1.0, sunUp);
    vec3 bed = texel.rgb * light * mix(vec3(0.48), vec3(1.0), lit);
    vec3 throughWater = bed * absorb + waterBody * (1.0 - absorb);

    float cosView = clamp(dot(V, N), 0.0, 1.0);
    float fresnel = 0.02 + 0.98 * pow(1.0 - cosView, 5.0);
    vec3 reflected = reflect(-V, N);
    vec3 reflectedWorld = normalize((gbufferModelViewInverse * vec4(reflected, 0.0)).xyz);
    vec3 skyHorizon = fogColor.rgb;
    vec3 skyTop = mix(vec3(0.18, 0.32, 0.58), vec3(0.34, 0.56, 0.88), sunUp);
    vec3 skyReflection = mix(skyHorizon, skyTop, pow(clamp(reflectedWorld.y * 0.5 + 0.5, 0.0, 1.0), 0.55));

    float roughness = clamp(wetness, 0.0, 1.0);
    float sunHighlight = max(dot(reflect(-L, N), V), 0.0);
    float sunSpec = pow(sunHighlight, mix(112.0, 42.0, roughness)) * 2.5 * sunUp * lit;
    float broadSpec = pow(sunHighlight, 24.0) * 0.10 * sunUp * skyExposure;
    vec3 color = mix(throughWater, skyReflection, clamp(fresnel + 0.04, 0.0, 0.90));
    vec3 sunTint = mix(vec3(1.0, 0.70, 0.46), vec3(1.0, 0.975, 0.93), clamp(h * 3.0, 0.0, 1.0));
    color += sunTint * (sunSpec + broadSpec);

    // Depth-contoured shoreline foam gives water edges the game's pale, broken highlight.
    float foamNoise = hash21(floor(vWorldPos.xz * 2.2 + vec2(t * 0.2, -t * 0.15)));
    float foam = (1.0 - smoothstep(0.05, 0.55, thickness))
        * smoothstep(0.30, 0.75, foamNoise) * (1.0 - smoothstep(20.0, 90.0, d));
    color = mix(color, vec3(0.92, 0.96, 1.0) * (0.55 + 0.45 * lightAmt), foam * 0.75);

    vec3 warmFog = fogWithSun(fogColor.rgb, vViewPos, L, h, sunUp);
    float fogFactor = clamp((fogEnd - d) / max(0.001, fogEnd - fogStart), 0.0, 1.0);
    color = mix(warmFog, color, fogFactor);
    float alpha = clamp(mix(0.76, 0.94, fresnel) * texel.a, 0.55, 0.96);
    gl_FragColor = vec4(max(color, vec3(0.0)), alpha);
}
