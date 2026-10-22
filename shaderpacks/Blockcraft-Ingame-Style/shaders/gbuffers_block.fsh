#version 120

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform sampler2D shadowtex0;
uniform vec3 sunPosition;
uniform vec3 cameraPosition;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec4 fogColor;
uniform float fogStart;
uniform float fogEnd;
uniform float wetness;
uniform float frameTimeCounter;
uniform int worldTime;

varying vec2 vTexCoord;
varying vec2 vLightCoord;
varying vec4 vColor;
varying vec3 vViewPos;
varying vec3 vViewNormal;
varying vec3 vWorldRel;
varying vec4 vShadowPos;

const float PI = 3.14159265359;
const int shadowMapResolution = 2048;
const bool shadowtex0Nearest = true;

float dayHeight() {
    return sin(float(worldTime) * (2.0 * PI / 24000.0));
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

float periodicHash(vec2 p, float period) {
    p = mod(p, period);
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float periodicNoise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = periodicHash(i, period);
    float b = periodicHash(i + vec2(1.0, 0.0), period);
    float c = periodicHash(i + vec2(0.0, 1.0), period);
    float d = periodicHash(i + vec2(1.0, 1.0), period);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
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

float proceduralCloudShade(vec3 rel, float worldY, float skyExposure, float sunUp, float wet) {
    vec3 sunWorld = normalize((gbufferModelViewInverse * vec4(normalize(sunPosition), 0.0)).xyz);
    float sunHeight = max(sunWorld.y, 0.15);
    float heightFade = smoothstep(0.12, 0.55, sunWorld.y);
    vec2 stableXZ = mod(cameraPosition.xz, 2048.0) + rel.xz;
    vec2 projected = stableXZ + sunWorld.xz * (max(180.0 - worldY, 0.0) / sunHeight);
    vec2 uv = projected / 512.0 + vec2(frameTimeCounter * 0.0012, -frameTimeCounter * 0.0008);
    float cover = periodicNoise(uv, 4.0);
    cover = smoothstep(0.48, 0.76, cover);
    float amount = cover * skyExposure * sunUp * heightFade * (1.0 - 0.35 * wet);
    return 1.0 - amount * 0.22;
}

vec3 fogWithSun(vec3 fog, vec3 rel, vec3 lightDir, float h, float sunUp) {
    float twilight = 1.0 - smoothstep(0.0, 0.30, abs(h));
    vec3 glowColor = vec3(1.0, 0.80 - 0.26 * twilight, 0.55 - 0.26 * twilight);
    float glow = (0.08 + 0.20 * twilight) * pow(max(dot(normalize(rel), lightDir), 0.0), 5.0) * sunUp;
    return fog + glowColor * glow;
}

void main() {
    vec4 albedo = texture2D(texture, vTexCoord) * vColor;
    if (albedo.a < 0.10) discard;

    vec3 Ngeom = normalize(vViewNormal);
    if (!gl_FrontFacing) Ngeom = -Ngeom;
    vec3 Nworld = normalize((gbufferModelViewInverse * vec4(Ngeom, 0.0)).xyz);
    vec3 L = normalize(sunPosition);
    vec3 V = normalize(-vViewPos);

    float h = dayHeight();
    float sunUp = clamp((h - 0.02) / 0.25, 0.0, 1.0);
    float skyExposure = smoothstep(0.45, 0.95, vLightCoord.y);
    float skyAmt = smoothstep(0.55, 0.97, vLightCoord.y) * sunUp;

    // Green-biased texels stand in for Blockcraft's tile-ID-aware leaf/frond lighting.
    float greenExcess = albedo.g - max(albedo.r * 1.08, albedo.b * 1.18);
    float leafHint = smoothstep(0.015, 0.11, greenExcess);
    vec3 NleafWorld = normalize(mix(Nworld, vec3(0.0, 1.0, 0.0), leafHint * 0.42));
    vec3 Nshade = normalize((gbufferModelView * vec4(NleafWorld, 0.0)).xyz);

    // Small albedo-driven relief follows Blockcraft's close-range texel normal perturbation.
    float texLuma = luma(albedo.rgb);
    vec3 dPdx = dFdx(vViewPos);
    vec3 dPdy = dFdy(vViewPos);
    vec3 r1 = cross(dPdy, Ngeom);
    vec3 r2 = cross(Ngeom, dPdx);
    float det = dot(dPdx, r1);
    float distanceToCamera = length(vViewPos);
    if (abs(det) > 1e-7 && distanceToCamera < 36.0 && leafHint < 0.5) {
        vec3 surfaceGradient = clamp((r1 * dFdx(texLuma) + r2 * dFdy(texLuma)) / det, vec3(-3.0), vec3(3.0));
        Nshade = normalize(Nshade - surfaceGradient * 0.035 * (1.0 - smoothstep(14.0, 36.0, distanceToCamera)));
    }

    float ndl = clamp(dot(Nshade, L), 0.0, 1.0);
    float shadow = shadowVisibility(Ngeom, L);
    float cloudShade = proceduralCloudShade(vWorldRel, cameraPosition.y + vWorldRel.y, skyExposure, sunUp, wetness);
    float direct = shadow * cloudShade * mix(leafHint > 0.5 ? 0.68 : 0.55, 1.0, smoothstep(0.0, 0.35, ndl));

    vec3 sunTint = mix(vec3(1.0, 0.70, 0.46), vec3(1.0, 0.975, 0.93), clamp(h * 3.0, 0.0, 1.0));
    vec3 shadeTint = vec3(0.45, 0.53, 0.78);
    vec3 shade = mix(vec3(1.0), mix(shadeTint, sunTint * 1.04, direct), skyAmt);
    vec3 bakedLight = texture2D(lightmap, clamp(vLightCoord, 0.0, 1.0)).rgb;
    vec3 color = albedo.rgb * bakedLight * shade;

    // Subsurface sunlight through foliage, matching the game's warm leaf back-scatter.
    float backScatter = leafHint * skyAmt * pow(max(dot(normalize(vViewPos), L), 0.0), 2.6) * 0.32;
    color += color * vec3(1.05, 1.18, 0.45) * backScatter;

    // Rain darkens exposed ground, saturates its color, and adds restrained puddle reflections.
    float upFacing = smoothstep(0.55, 0.95, Nworld.y);
    float wet = clamp(wetness, 0.0, 1.0) * skyExposure * mix(0.55, 1.0, upFacing);
    vec3 H = normalize(V + L);
    float specular = pow(max(dot(Nshade, H), 0.0), 28.0) * direct * skyAmt * 0.11;
    specular += pow(max(dot(Nshade, H), 0.0), 52.0) * wet * 0.16;
    color += sunTint * specular;
    color = mix(color, mix(vec3(luma(color)), color, 1.25) * 0.70, wet * 0.72);

    if (wet > 0.02 && upFacing > 0.3) {
        vec2 stableXZ = mod(cameraPosition.xz, 2048.0) + vWorldRel.xz;
        float puddleNoise = periodicNoise(stableXZ / 32.0, 64.0);
        float puddle = smoothstep(0.50, 0.73, puddleNoise) * wet * upFacing;
        vec3 Vworld = normalize((gbufferModelViewInverse * vec4(V, 0.0)).xyz);
        vec3 reflectedWorld = reflect(-Vworld, Nworld);
        float fresnel = 0.08 + 0.92 * pow(1.0 - clamp(dot(Nworld, Vworld), 0.0, 1.0), 4.0);
        vec3 skyReflection = mix(fogColor.rgb, vec3(0.22, 0.39, 0.68), pow(clamp(reflectedWorld.y * 0.5 + 0.5, 0.0, 1.0), 0.55));
        color = mix(color, mix(color * 0.58, skyReflection, clamp(fresnel + 0.26, 0.0, 0.76)), puddle * 0.36);
    }

    vec3 warmFog = fogWithSun(fogColor.rgb, vViewPos, L, h, sunUp);
    float fogFactor = clamp((fogEnd - distanceToCamera) / max(0.001, fogEnd - fogStart), 0.0, 1.0);
    color = mix(warmFog, color, fogFactor);

    gl_FragColor = vec4(max(color, vec3(0.0)), albedo.a);
}
