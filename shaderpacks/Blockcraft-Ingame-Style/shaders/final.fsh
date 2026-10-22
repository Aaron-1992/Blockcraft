#version 120

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform mat4 gbufferProjection;
uniform vec3 sunPosition;
uniform float viewWidth;
uniform float viewHeight;
uniform float near;
uniform float far;
uniform float rainStrength;
uniform int worldTime;
varying vec2 vTexCoord;

const float PI = 3.14159265359;

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

float dayHeight() {
    return sin(float(worldTime) * (2.0 * PI / 24000.0));
}

float linearDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * near * far) / max(0.0001, far + near - z * (far - near));
}

vec3 fxaa(vec2 uv, vec2 pixel) {
    vec3 rgbNW = texture2D(colortex0, clamp(uv + vec2(-1.0, -1.0) * pixel, vec2(0.001), vec2(0.999))).rgb;
    vec3 rgbNE = texture2D(colortex0, clamp(uv + vec2( 1.0, -1.0) * pixel, vec2(0.001), vec2(0.999))).rgb;
    vec3 rgbSW = texture2D(colortex0, clamp(uv + vec2(-1.0,  1.0) * pixel, vec2(0.001), vec2(0.999))).rgb;
    vec3 rgbSE = texture2D(colortex0, clamp(uv + vec2( 1.0,  1.0) * pixel, vec2(0.001), vec2(0.999))).rgb;
    vec3 rgbM = texture2D(colortex0, uv).rgb;
    float lNW = luma(rgbNW);
    float lNE = luma(rgbNE);
    float lSW = luma(rgbSW);
    float lSE = luma(rgbSE);
    float lM = luma(rgbM);
    float lMin = min(lM, min(min(lNW, lNE), min(lSW, lSE)));
    float lMax = max(lM, max(max(lNW, lNE), max(lSW, lSE)));

    vec2 direction;
    direction.x = -((lNW + lNE) - (lSW + lSE));
    direction.y =  ((lNW + lSW) - (lNE + lSE));
    float reduce = max((lNW + lNE + lSW + lSE) * 0.03125, 0.0078125);
    float inverseMin = 1.0 / (min(abs(direction.x), abs(direction.y)) + reduce);
    direction = clamp(direction * inverseMin, vec2(-8.0), vec2(8.0)) * pixel;

    vec3 rgbA = 0.5 * (
        texture2D(colortex0, clamp(uv + direction * (1.0 / 3.0 - 0.5), vec2(0.001), vec2(0.999))).rgb +
        texture2D(colortex0, clamp(uv + direction * (2.0 / 3.0 - 0.5), vec2(0.001), vec2(0.999))).rgb);
    vec3 rgbB = rgbA * 0.5 + 0.25 * (
        texture2D(colortex0, clamp(uv + direction * -0.5, vec2(0.001), vec2(0.999))).rgb +
        texture2D(colortex0, clamp(uv + direction *  0.5, vec2(0.001), vec2(0.999))).rgb);
    float lB = luma(rgbB);
    return (lB < lMin || lB > lMax) ? rgbA : rgbB;
}

float ambientOcclusion(vec2 uv, vec2 pixel) {
    float depth = texture2D(depthtex0, uv).r;
    if (depth > 0.99995) return 1.0;

    float centerZ = linearDepth(depth);
    float occ = 0.0;
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        float angle = fi * 0.7853981634 + 0.43;
        float radius = 2.0 + mod(fi * 1.7, 3.5);
        vec2 sampleUv = clamp(uv + vec2(cos(angle), sin(angle)) * pixel * radius, vec2(0.001), vec2(0.999));
        float sampleDepth = texture2D(depthtex0, sampleUv).r;
        if (sampleDepth < 0.99995) {
            float sampleZ = linearDepth(sampleDepth);
            float nearer = centerZ - sampleZ;
            occ += smoothstep(0.12, 1.35, nearer) * (1.0 - smoothstep(2.0, 5.0, centerZ));
        }
    }
    return clamp(1.0 - 0.22 * occ / 8.0, 0.76, 1.0);
}

vec3 glowSample(vec2 uv) {
    vec3 c = texture2D(colortex0, clamp(uv, vec2(0.001), vec2(0.999))).rgb;
    float k = smoothstep(0.72, 1.08, luma(c));
    return c * k;
}

vec3 bloom(vec2 uv, vec2 pixel) {
    vec3 small = vec3(0.0);
    vec3 medium = vec3(0.0);
    vec3 wide = vec3(0.0);
    for (int i = 0; i < 8; i++) {
        float a = float(i) * (PI / 4.0);
        vec2 axis = vec2(cos(a), sin(a));
        small += glowSample(uv + axis * pixel * 2.0);
        medium += glowSample(uv + axis * pixel * 5.5);
        wide += glowSample(uv + axis * pixel * 12.0);
    }
    return (small * 0.55 + medium * 0.30 + wide * 0.15) / 8.0;
}

void main() {
    vec2 pixel = 1.0 / max(vec2(viewWidth, viewHeight), vec2(1.0));
    vec3 color = fxaa(vTexCoord, pixel);

    // Compact depth-only SSAO, preserving the game's soft contact shading without darkening open sky.
    float ao = ambientOcclusion(vTexCoord, pixel);
    float emissiveMask = smoothstep(0.82, 1.35, luma(color));
    color *= mix(1.0, ao, 0.72 * (1.0 - emissiveMask));

    // Three bloom radii for tight block glints and a softer ambient glow.
    color += bloom(vTexCoord, pixel) * 1.25;

    // Screen-space shafts are depth-masked and fade naturally in rain, at night, and near screen edges.
    float h = dayHeight();
    float sunUp = clamp((h - 0.02) / 0.25, 0.0, 1.0);
    vec4 sunClip = gbufferProjection * vec4(normalize(sunPosition) * 96.0, 1.0);
    if (sunClip.w > 0.0) {
        vec2 sunUv = sunClip.xy / sunClip.w * 0.5 + 0.5;
        if (sunUv.x > 0.0 && sunUv.x < 1.0 && sunUv.y > 0.0 && sunUv.y < 1.0) {
            vec2 stepUv = (sunUv - vTexCoord) * (0.88 / 24.0);
            float jitter = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
            vec2 rayUv = vTexCoord + stepUv * jitter;
            float shafts = 0.0;
            float decay = 1.0;
            for (int i = 0; i < 24; i++) {
                rayUv += stepUv;
                float d = texture2D(depthtex0, clamp(rayUv, vec2(0.001), vec2(0.999))).r;
                shafts += step(0.99985, d) * decay;
                decay *= 0.96;
            }
            float tw = 1.0 - smoothstep(0.0, 0.30, abs(h));
            float edge = smoothstep(0.0, 0.08, sunUv.x) * smoothstep(0.0, 0.08, sunUv.y)
                * smoothstep(0.0, 0.08, 1.0 - sunUv.x) * smoothstep(0.0, 0.08, 1.0 - sunUv.y);
            vec3 rayColor = mix(vec3(1.0, 0.82, 0.62), vec3(1.0, 0.95, 0.84), sunUp);
            color += rayColor * (shafts / 24.0) * (0.055 + 0.025 * tw) * sunUp * (1.0 - 0.75 * rainStrength) * edge;
        }
    }

    // Blockcraft's default Vivid grade (Look.java): saturation, split tint, S-curve, exposure and vignette.
    float mx = max(color.r, max(color.g, color.b));
    if (mx > 1.0) color = mix(color / mx, vec3(1.0), clamp((mx - 1.0) * 0.5, 0.0, 0.7));
    float l = luma(color);
    color = mix(vec3(l), color, 1.30);
    color *= mix(vec3(0.93, 0.98, 1.07), vec3(1.06, 1.02, 0.95), smoothstep(0.04, 0.80, l));
    color = clamp(color, 0.0, 1.0);
    color = mix(color, color * color * (3.0 - 2.0 * color), 0.30);
    color = color * 1.05 - 0.010;

    vec2 q = vTexCoord - 0.5;
    color *= 1.0 - dot(q, q) * 0.30;
    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
