# Blockcraft Ingame Style — Minecraft Java shaderpack

A Java Edition shaderpack port of Blockcraft's default **Vivid** look for OptiFine or Iris. The pack carries Blockcraft's warm low-sun light, cool blue shadows, directional twilight sky, soft cast shadows, and saturated color grade into Minecraft's shaderpack pipeline.

## What it does

- Stable 2048px shadow map with a 96-block shadow distance, one-block shadow snapping, matching shadow distortion in the write/read passes, and soft PCF filtering.
- Blockcraft-inspired daylight and baked lightmap shading, cool shadow tint, leaf back-scatter, and subtle close-range texture relief.
- Procedural moving cloud-shadow modulation over sunlit terrain, plus rain-darkened exposed ground and restrained puddle reflections.
- Depth-absorbing water with multi-directional animated ripples, Fresnel sky reflections, sun glints, and shoreline foam.
- Directional sunrise/sunset belt, pink arch and blue earth-shadow band, fog-matched horizon, and sun glow.
- Depth-based contact AO, three-radius bloom, FXAA, screen-space sun shafts, and the Vivid post-grade.

## Install

1. Use **Minecraft Java Edition** with OptiFine or Iris.
2. Copy `Blockcraft-Ingame-Style.zip` into `.minecraft/shaderpacks/` without extracting it.
3. Open **Options → Video Settings → Shaders** and select **Blockcraft Ingame Style**.
4. For the intended look, leave the pack's shadow settings at their defaults. It uses the settings listed above.

The shaders use GLSL 1.20 and the OptiFine/Iris shaderpack interface. They have been syntax-checked, but this workspace has not launched a Minecraft client; loader/version-specific differences may require small compatibility adjustments.

## Fidelity notes

Blockcraft's renderer has private pipeline inputs that shaderpacks cannot access at the same point: tile IDs, dynamic-light records, its volumetric cloud-shadow map, and a copied scene-color buffer for water refraction and screen-space reflections. The Java pack therefore approximates those features with Minecraft's lightmap, procedural cloud shading, depth-based water absorption, animated waves, and a sky-reflection fallback. It is a visual port, not a byte-for-byte replacement of Blockcraft's render pipeline. No in-game appearance or performance result is claimed without a Minecraft runtime test.

The grade in `final.fsh` follows Blockcraft's default **Vivid** settings in `src/bc/Look.java`: saturation 1.30, S-curve 0.30, exposure 1.05, vignette 0.30, blue-tinted shadows, and warm highlights.
