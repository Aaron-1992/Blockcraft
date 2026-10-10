Blockcraft Mods Folder (Full Developer Guide: docs/MODDING.md)
==============================================================
Place .jar mods (implementing bc.BlockcraftMod) or declarative .bcmod/.properties/.zip mods here,
or install curated mods directly from the in-game Mods screen or Blockcraft-mods.bat!
Note: Even if Windows Notepad adds a hidden .txt extension (e.g. my-mod.jar.txt or my-mod.bcmod.txt),
Blockcraft's ModLoader automatically detects and loads it!

1. Included External JAR Mods:
   infdev-2010.jar       - Reverts Blockcraft to Minecraft Infdev (June 2010): craggy overhangs,
                           Brick Pyramids, 3D blocky clouds, flat lighting, neon-green grass,
                           classic HUD (no hunger bar), and instant food healing (/infdev).
   backrooms-stalker.jar - VHS Analog Horror Shader, 5 Separate Prettier Backrooms Levels
                           (Level 0 Lobby, Level 1 Warehouse, Level 2 Pipe Dreams, Level 37
                           Poolrooms, Level ! Run), custom 3D Office Desks (hold Shift to
                           physically crawl underneath!), 3D Steel Hiding Lockers, 3D Almond
                           Water Coolers, 3D Illuminated Level Exit Doors on every level,
                           and balanced 3D Stalker Entity (/backrooms level 0|1|2|poolrooms|run).

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
     or mod.properties (mainClass) — or ModLoader will auto-scan all .class files in the JAR!
   - Compile with: javac --release 17 -cp build/blockcraft.jar -d out MyMod.java
   - Package with: jar cf mods/my-mod.jar -C out .
   - See docs/MODDING.md on GitHub for full examples and engine API tables.
