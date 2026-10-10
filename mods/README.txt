Blockcraft Mods Folder (Full Developer Guide: docs/MODDING.md)
==============================================================
Place .jar mods (implementing bc.BlockcraftMod) or declarative .bcmod/.properties/.zip mods here,
or install curated mods directly from the in-game Mods screen or Blockcraft-mods.bat!

1. Included External Total-Conversion JAR Mod:
   infdev-2010.jar - Reverts Blockcraft to Minecraft Infdev (June 2010): craggy overhangs,
                     Brick Pyramids, 3D blocky clouds, flat lighting, neon-green grass,
                     classic HUD (no hunger bar), and instant food healing.

2. Zero-Code Declarative .bcmod / .properties Example (save as mods/my-mod.bcmod):
   id=my-mod
   name=My Custom Mod
   version=1.0.0
   author=Player
   description=Adds custom recipes, smelting, and commands
   shapeless.flint=flint,1:gravel,gravel
   smelt.leather=rotten_flesh:leather,1
   command.greet=Hello from My Custom Mod!

3. Java .jar Mod API (bc.BlockcraftMod):
   - Implement bc.BlockcraftMod (onInit, onEnable, onDisable, onGameTick, onWorldTick,
     onCommand, onBlockPlaced, onBlockBroken, createWorldGen, hudWatermark).
   - Register your class in META-INF/services/bc.BlockcraftMod, META-INF/MANIFEST.MF (Mod-Class),
     or mod.properties (mainClass).
   - Compile with: javac --release 17 -cp build/blockcraft.jar -d out MyMod.java
   - Package with: jar cf mods/my-mod.jar -C out .
   - See docs/MODDING.md on GitHub for full examples and engine API tables.
