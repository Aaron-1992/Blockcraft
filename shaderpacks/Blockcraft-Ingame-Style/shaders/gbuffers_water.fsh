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
const float SHADOW_TEXEL = 1.0 / 2048.0;

float dayHeight() {
    return sin(float(worldTime) * (2.0 * PI / 24000.0));
}

float linearDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * near * far) / max(0.0001, far + near - z * (far - near));
}

float shadowVisibility(vec3 normal, vec3 lightDir) {
    if (vShadowPos.w <= 0.0) return 1.0;
    vec3 coord = (vShadowPos.xyz / vShadowPos.w) * 0.5 + 0.5;
    if (coord.x <= 0.001 || coord.x >= 0.999 || coord.y <= 0.001 || coord.y >= 0.999 || coord.z <= 0.0 || coord.z >= 1.0) return 1.0;
    float bias = 0.0005 + 0.0015 * (1.0 - clamp(dot(normal, lightDir), 0.0, 1.0));
    float sum = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float d = texture2D(shadowtex0, coord.xy + vec2(float(x), float(y)) * SHADOW_TEXEL).r;
            sum += (coord.z - bias <= d) ? 1.0 : 0.0;
        }
    }
    return sum / 9.0;
}

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec4 texel = texture2D(texture, vTexCoord) * vColor;
    if (texel.a < 0.02) discard;

    vec3 N = normalize(vViewNormal);
    if (!gl_FrontFacing) N = -N;
    float t = frameTimeCounter;
    float d = length(vViewPos);
    float fade = 1.0 - smoothstep(24.0, 150.0, d);
    float rippleA = cos(vWorldPos.x * 1.3 + t * 1.2) * 0.07 * fade;
    float rippleB = cos((vWorldPos.x + vWorldPos.z) * 2.3 + t * 1.9) * 0.045 * fade;
    float rippleC = cos(vWorldPos.z * 1.1 + t) * 0.07 * fade;
    N = normalize(N + vec3(rippleA + rippleB, 0.0, rippleC - rippleB));

    vec3 L = normalize(sunPosition);
    vec3 V = normalize(-vViewPos);
    float h = dayHeight();
    float sunUp = smoothstep(-0.10, 0.15, h);
    float ndl = max(dot(N, L), 0.0);
    float shadow = shadowVisibility(N, L);
    float lit = shadow * mix(0.55, 1.0, smoothstep(0.0, 0.35, ndl));
    vec3 light = texture2D(lightmap, clamp(vLightCoord, 0.0, 1.0)).rgb;
    float lightAmt = clamp(max(light.r, max(light.g, light.b)), 0.04, 1.0);

    vec2 screenUv = gl_FragCoord.xy / max(vec2(viewWidth, viewHeight), vec2(1.0));
    float sceneZ = texture2D(depthtex0, screenUv).r;
    float surfaceZ = linearDepth(gl_FragCoord.z);
    float behindZ = sceneZ < 0.99995 ? linearDepth(sceneZ) : surfaceZ + 40.0;
    float thickness = min(max(behindZ - surfaceZ, 0.0), 40.0);

    // The game uses a scene-color/depth copy for true refraction and screen-space reflections.
    // This portable shaderpack keeps the same Fresnel, absorption, ripples and sun glints, with a sky reflection fallback.
    vec3 absorb = exp(-thickness * vec3(0.50, 0.16, 0.07));
    vec3 waterBody = vec3(0.035, 0.20, 0.36) * lightAmt * mix(0.55, 1.0, sunUp);
    vec3 bed = texel.rgb * light * mix(vec3(0.65), vec3(1.0), lit);
    vec3 throughWater = bed * absorb + waterBody * (1.0 - absorb);

    float cosView = clamp(dot(V, N), 0.0, 1.0);
    float fresnel = 0.02 + 0.98 * pow(1.0 - cosView, 5.0);
    vec3 reflected = reflect(-V, N);
    vec3 reflectedWorld = normalize((gbufferModelViewInverse * vec4(reflected, 0.0)).xyz);
    vec3 skyHorizon = fogColor.rgb;
    vec3 skyTop = mix(vec3(0.20, 0.36, 0.62), vec3(0.34, 0.56, 0.88), sunUp);
    vec3 skyReflection = mix(skyHorizon, skyTop, pow(clamp(reflectedWorld.y * 0.5 + 0.5, 0.0, 1.0), 0.55));

    float sunSpec = pow(max(dot(reflect(-L, N), V), 0.0), 96.0) * 2.8 * sunUp * lit;
    float broadSpec = pow(max(dot(reflect(-L, N), V), 0.0), 28.0) * 0.12 * sunUp;
    vec3 color = mix(throughWater, skyReflection, clamp(fresnel + 0.04, 0.0, 0.90));
    vec3 sunTint = mix(vec3(1.0, 0.70, 0.46), vec3(1.0, 0.975, 0.93), clamp(h * 3.0, 0.0, 1.0));
    color += sunTint * (sunSpec + broadSpec);

    float foamNoise = hash21(floor(vWorldPos.xz * 2.2 + vec2(t * 0.2, -t * 0.15)));
    float foam = (1.0 - smoothstep(0.05, 0.55, thickness))
        * smoothstep(0.30, 0.75, foamNoise) * (1.0 - smoothstep(20.0, 90.0, d));
    color = mix(color, vec3(0.92, 0.96, 1.0) * (0.55 + 0.45 * lightAmt), foam * 0.75);

    float fogFactor = clamp((fogEnd - d) / max(0.001, fogEnd - fogStart), 0.0, 1.0);
    color = mix(fogColor.rgb, color, fogFactor);
    float alpha = clamp(mix(0.76, 0.94, fresnel) * texel.a, 0.55, 0.96);
    gl_FragColor = vec4(max(color, vec3(0.0)), alpha);
}
