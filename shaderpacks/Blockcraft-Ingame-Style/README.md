# Blockcraft Ingame Style — Minecraft Java shaderpack

A Minecraft Java shaderpack port of Blockcraft's in-game **Vivid** graphics look. It keeps the same recognizable palette and lighting: warm low-angle sunlight, cool blue shade, soft PCF shadows, tinted fog with a sunset glow, rain-darkened ground, moving ripples, water Fresnel/reflection, screen-space bloom, ambient occlusion, and the Vivid post-grade.

## Install

1. Use **Minecraft Java Edition** with OptiFine or Iris (Iris must have its normal OptiFine shaderpack compatibility enabled).
2. Copy `Blockcraft-Ingame-Style.zip` into `.minecraft/shaderpacks/` without extracting it.
3. Open **Options → Video Settings → Shaders** and select **Blockcraft Ingame Style**.
4. For the intended look, leave Minecraft's color settings and shaderpack shadow settings at their defaults. The pack uses a 2048px shadow map, a 96-block distance, stabilized 1-block shadow snapping, and a light-space distortion that reserves more map detail near the player.

The sources use GLSL 1.20 and the OptiFine/Iris shaderpack interface. This package was syntax-checked, but was not launched inside a Minecraft client in this workspace; loader/version-specific differences may require small compatibility adjustments.

## Fidelity notes

The original renderer uses game-specific inputs that Minecraft shaderpacks do not expose at the same point in the pipeline: its custom block-tile IDs, twelve dynamic-light records, volumetric cloud-shadow map, and a copied scene color/depth buffer for water refraction and screen-space reflections. This port reproduces the visible style with Minecraft's lightmap, shadow map, rain state, procedural ripples, depth-based water absorption, Fresnel sky reflections, and screen-space post-processing. As a result it is a faithful visual port, not a byte-for-byte replacement of Blockcraft's private render pipeline.

The grade in `final.fsh` copies the game's default **Vivid** look from `src/bc/Look.java`: saturation 1.30, S-curve 0.30, exposure 1.05, vignette 0.30, blue-tinted shadows, and warm highlights.
