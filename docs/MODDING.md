# Blockcraft Modding Guide & Developer Documentation (V4.1)

Blockcraft includes a built-in **Mod Loader** (`bc.ModLoader`), an in-game **Mods Screen**, a standalone **Desktop Mod & Resource Pack Hub** (`Blockcraft-mods.bat`), and **automatic Multiplayer Mod Synchronization**.

You can create mods for Blockcraft in **two ways**:

1. **Declarative Mods (`.bcmod`, `.properties`, or `.zip`)** — *No Java compiler required.* Create a simple text file in `mods/` to add custom crafting recipes, furnace smelting rules, and custom `/commands`.
2. **Java Code Mods (`.jar`)** — *Full engine access.* Implement the `bc.BlockcraftMod` interface to hook into world generation, 20 Hz game/world ticks, block placement & breaking, custom commands, player physics, mobs, HUD watermarks, and total conversions (such as the included `mods/infdev-2010.jar`).

---

## 1. Where Mods Live & How to Load Them

All mods are loaded from the **`mods/`** directory next to `Blockcraft.exe` / `blockcraft.jar`:

```text
Blockcraft/
├── Blockcraft.exe
├── Blockcraft-mods.bat          # Standalone Desktop Mod & Resource Pack Manager
├── mods/
│   ├── README.txt
│   ├── mods-state.properties    # Automatically saved install/enable state
│   ├── infdev-2010.jar          # Included Java Total-Conversion Mod
│   └── my-custom-mod.bcmod      # Your custom .bcmod / .zip / .jar mods
└── resourcepacks/
    └── README.txt
```

### Managing Mods
* **In-Game**: Click **Mods...** on the Title Screen, Options Menu, or Pause Menu (`Esc -> Mods...`). Click **Refresh** at any time to hot-reload new files placed in `mods/`.
* **Desktop Manager**: Run `Blockcraft-mods.bat` (or click **Desktop Hub** inside the in-game Mods screen) to manage mods and resource packs in a dedicated desktop window.
* **In-Game Chat Commands**:
  * `/mods` — Lists all currently active mods.
  * `/infdev [on|off]` — Toggles the pre-installed *Minecraft Infdev (June 2010)* total-conversion mod.
* **Multiplayer Sync**: When a Host starts a multiplayer server (`Host World` / `Host LAN`), the Host's active mod list is automatically synchronized (`MODS` packet) to all joining clients for the duration of the session and restored when they disconnect.

---

## 2. Option A: Declarative Mods (`.bcmod` / `.properties` / `.zip`)

If you want to add new crafting recipes, smelting recipes, or chat commands without writing or compiling Java code, create a `.bcmod` (or `.properties`) file inside `mods/`. You can also package a `mod.properties` file inside a `.zip` or `.jar` archive in `mods/`.

### Supported Properties

| Key | Description | Example |
| :--- | :--- | :--- |
| `id` | Unique lowercase mod identifier (no spaces) | `id=survival-plus` |
| `name` | Display title in the Mods menu | `name=Survival Plus` |
| `version` | Version string | `version=1.2.0` |
| `author` | Creator name | `author=YourName` |
| `category` | Category badge shown in the Mod Catalog | `category=Crafting & QoL` |
| `description` | Summary shown in the mod list | `description=Extra recipes and commands` |
| `shapeless.<key>` | Shapeless recipe: `<outItem>,<outCount>:<in1>,<in2>,...` (supports names or numeric IDs, up to 9 inputs) | `shapeless.flint=flint,1:gravel,gravel` |
| `smelt.<key>` | Furnace recipe: `<inItem>:<outItem>,<outCount>` (supports names or numeric IDs) | `smelt.leather=rotten_flesh:leather,1` |
| `command.<name>` | Registers `/<name>` in chat to print a formatted message | `command.rules=§eNo griefing! Have fun!` |

### Complete Declarative Mod Example (`mods/survival-plus.bcmod`)

Create `mods/survival-plus.bcmod` with the following contents:

```properties
id=survival-plus
name=Survival Plus Pack
version=1.0.0
author=Community
category=Crafting
description=Adds gravel-to-flint, rotten-flesh-to-leather smelting, cobble-to-sand crushing, and /starter info.

# Shapeless Crafting Recipes: <output>,<count>:<ingredient1>,<ingredient2>,...
# You can use item/block names OR numeric IDs:
shapeless.flint=flint,1:gravel,gravel
shapeless.sand=sand,2:cobblestone,cobblestone
shapeless.string=263,4:34
shapeless.diamonds=diamond,1:coal,coal,coal,coal,gold_ingot,gold_ingot

# Furnace Smelting Recipes: <input>:<output>,<count>
smelt.flesh_to_leather=rotten_flesh:leather,1
smelt.gravel_to_glass=gravel:glass,1

# Custom Chat Commands: type /starter or /discord in-game
command.starter=Craft 2 Gravel into Flint, or smelt Rotten Flesh into Leather!
command.discord=Join our community server via Share Code!
```

Launch Blockcraft (or click **Refresh** in the **Mods** screen) and your recipes and `/starter` command are immediately active!

---

## 3. Option B: Java `.jar` Mods (`bc.BlockcraftMod`)

For full programmatic control over gameplay, blocks, entities, UI, and world generation, create a Java `.jar` mod that implements `bc.BlockcraftMod`.

### The `bc.BlockcraftMod` Interface

```java
package bc;

public interface BlockcraftMod {
   /** Unique lowercase identifier (e.g. "my-mod"). */
   String id();

   /** Display name shown in the Mods menu. */
   String name();

   default String version() { return "1.0.0"; }
   default String author() { return "Community"; }
   default String description() { return ""; }

   /**
    * Called when the mod is initialized or enabled, and whenever active mod recipes are rebuilt.
    * Use ctx to register shaped recipes, shapeless recipes, and furnace smelting recipes.
    */
   default void onInit(ModLoader.Context ctx) {}

   /** Called when the user enables the mod in the Mods screen or via command. */
   default void onEnable(Game game) {}

   /** Called when the user disables the mod. Restore any modified Game/Options state here. */
   default void onDisable(Game game) {}

   /** Called every game tick (20 times per second) while a world is open. */
   default void onGameTick(Game game) {}

   /** Called every world tick (20 times per second) with the active World instance. */
   default void onWorldTick(World world) {}

   /**
    * Called when the player runs a slash command (e.g. "/mycmd arg1 arg2").
    * Note: cmd includes the leading slash (e.g. "/mycmd"), and args[0] is the command name without slash.
    * Return true if your mod handled the command, or false to pass it on.
    */
   default boolean onCommand(Game game, String cmd, String[] args) { return false; }

   /** Called right after a player places a block at (x, y, z) with block ID id. */
   default void onBlockPlaced(World world, int x, int y, int z, int id) {}

   /** Called right after a player breaks a block at (x, y, z) that previously had block ID oldId. */
   default void onBlockBroken(World world, int x, int y, int z, int oldId) {}

   /**
    * Optional hook allowing a mod to supply a custom terrain generator (subclass of bc.Gen)
    * for a dimension (Dim.OVERWORLD = 0, Dim.NETHER = -1, Dim.END = 1). Return null to keep default terrain.
    */
   default Gen createWorldGen(int dim, long seed, WorldSettings settings) { return null; }

   /** Optional top-left HUD watermark text (return null for none). */
   default String hudWatermark() { return null; }

   /** Return true to use the classic pre-Beta 1.8 HUD (hearts + armor only, no hunger bar, instant food heal). */
   default boolean classicHudNoHunger() { return false; }

   /** Return false to hide specific block/item IDs from the Creative inventory when this mod is active. */
   default boolean allowPaletteItem(int id) { return true; }

   /** Return true only for total-conversion mods that should start disabled by default until toggled on. */
   default boolean totalConversionInfdev() { return false; }
}
```

### Registering Recipes in `onInit(ModLoader.Context ctx)`

`ModLoader.Context` provides three methods that safely register recipes (and automatically clean them up if your mod is disabled):

```java
@Override
public void onInit(ModLoader.Context ctx) {
   // 1. Shapeless recipe: 2 Cobblestone -> 2 Sand
   ctx.addShapelessRecipe(Blocks.SAND, 2, Blocks.COBBLE, Blocks.COBBLE);

   // 2. Shaped recipe (up to 3x3 pattern): craft a Saddle from 5 Leather + 2 Iron Ingots
   ctx.addShapedRecipe(
      Items.SADDLE, 1,
      new String[]{
         "LLL",
         "L L",
         "I I"
      },
      'L', Items.LEATHER,
      'I', Items.IRON_INGOT
   );

   // 3. Furnace smelting recipe: Rotten Flesh -> Leather
   ctx.addSmelting(Items.ROTTEN_FLESH, Items.LEATHER, 1);
}
```

---

## 4. Step-by-Step Tutorial: Building Your First `.jar` Mod

### Step 1: Write Your Mod Class (`SuperMinerMod.java`)

Create a folder `my-mod-src/bc/` and save `my-mod-src/bc/SuperMinerMod.java`:

> **Tip**: Placing your mod class in `package bc;` gives you access to both `public` and package-private fields/methods of `Game`, `World`, `Player`, `Mobs`, `Blocks`, and `Items`! You can also use any custom package (e.g. `package com.example;`) if you stick to `public` members.

```java
package bc;

public final class SuperMinerMod implements BlockcraftMod {
   private boolean veinMode = true;
   private boolean miningVein = false;

   @Override
   public String id() {
      return "super-miner";
   }

   @Override
   public String name() {
      return "Super Miner & Vein Harvester";
   }

   @Override
   public String version() {
      return "1.0.0";
   }

   @Override
   public String author() {
      return "YourName";
   }

   @Override
   public String description() {
      return "Automatically mines connected ore veins, adds /vein toggle & /superjump, and crafts Torches from Flint.";
   }

   @Override
   public void onInit(ModLoader.Context ctx) {
      // Craft 4 Torches from 1 Flint + 1 Stick
      ctx.addShapelessRecipe(Blocks.TORCH, 4, Items.FLINT, Items.STICK);
      // Smelt Gravel into Flint
      ctx.addSmelting(Blocks.GRAVEL, Items.FLINT, 1);
   }

   @Override
   public void onEnable(Game g) {
      if (g != null) {
         g.chatMsg("§a[SuperMiner] Enabled! Mine any ore block to harvest the vein, or type /vein.");
      }
   }

   @Override
   public boolean onCommand(Game g, String cmd, String[] args) {
      if (cmd.equalsIgnoreCase("/vein")) {
         veinMode = !veinMode;
         g.chatMsg("§e[SuperMiner] Vein Mining is now " + (veinMode ? "§aON" : "§cOFF"));
         return true;
      }
      if (cmd.equalsIgnoreCase("/superjump")) {
         if (g.player != null) {
            g.player.vy = 1.25;
            g.player.fallDistance = 0.0F;
            g.chatMsg("§b[SuperMiner] Whoosh!");
         }
         return true;
      }
      return false;
   }

   @Override
   public void onBlockBroken(World w, int x, int y, int z, int oldId) {
      if (!veinMode || miningVein || !isOre(oldId)) {
         return;
      }
      miningVein = true;
      try {
         int extra = 0;
         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               for (int dz = -1; dz <= 1; dz++) {
                  if (dx == 0 && dy == 0 && dz == 0) continue;
                  int bx = x + dx, by = y + dy, bz = z + dz;
                  if (w.getBlock(bx, by, bz) == oldId) {
                     w.setBlock(bx, by, bz, Blocks.AIR);
                     if (w.game != null) {
                        w.game.meshNow(bx, by, bz);
                     }
                     extra++;
                  }
               }
            }
         }
         if (extra > 0) {
            int dropId = switch (oldId) {
               case Blocks.COAL_ORE -> Items.COAL;
               case Blocks.DIAMOND_ORE -> Items.DIAMOND;
               default -> oldId;
            };
            w.dropItem(x + 0.5, y + 0.5, z + 0.5, dropId, extra);
         }
      } finally {
         miningVein = false;
      }
   }

   private static boolean isOre(int id) {
      return id == Blocks.COAL_ORE
         || id == Blocks.IRON_ORE
         || id == Blocks.GOLD_ORE
         || id == Blocks.DIAMOND_ORE
         || id == Blocks.REDSTONE_ORE
         || id == Blocks.LAPIS_ORE;
   }
}
```

### Step 2: Register Your Mod Entrypoint

`ModLoader` supports **three** ways to discover your `BlockcraftMod` class inside a `.jar` (use whichever you prefer):

1. **ServiceLoader file (`META-INF/services/bc.BlockcraftMod`)** — Create a text file at `META-INF/services/bc.BlockcraftMod` containing the fully-qualified class name:
   ```text
   bc.SuperMinerMod
   ```
2. **JAR Manifest (`META-INF/MANIFEST.MF`)** — Add the `Mod-Class` attribute:
   ```text
   Manifest-Version: 1.0
   Mod-Class: bc.SuperMinerMod
   ```
3. **`mod.properties` at the root of the `.jar`**:
   ```properties
   mainClass=bc.SuperMinerMod
   ```

### Step 3: Compile & Package into `mods/super-miner.jar`

Using any JDK (Java 17+) and `build/blockcraft.jar` (or `Blockcraft.exe`, which is a valid ZIP/JAR at its tail):

```bash
# 1. Compile against blockcraft.jar
mkdir -p out/META-INF/services
javac --release 17 -cp build/blockcraft.jar -d out my-mod-src/bc/SuperMinerMod.java

# 2. Register entrypoint
echo "bc.SuperMinerMod" > out/META-INF/services/bc.BlockcraftMod

# 3. Package into mods/super-miner.jar
jar cf mods/super-miner.jar -C out .
```

Start Blockcraft (or click **Refresh** in the **Mods** menu) and **Super Miner & Vein Harvester** will appear in your mod list!

---

## 5. Advanced Example: Custom World Generator Mod (`createWorldGen`)

Mods can override Overworld, Nether, or End terrain generation by returning a `bc.Gen` subclass from `createWorldGen(int dim, long seed, WorldSettings settings)`.

```java
package bc;

public final class CheckerWorldMod implements BlockcraftMod {
   @Override
   public String id() { return "checker-world"; }

   @Override
   public String name() { return "Checkerboard Islands WorldGen"; }

   @Override
   public String description() { return "Generates floating quartz and obsidian checkerboard islands."; }

   @Override
   public Gen createWorldGen(int dim, long seed, WorldSettings settings) {
      if (dim != Dim.OVERWORLD || (settings != null && settings.isVoidMode())) {
         return null;
      }
      return new Gen(seed, settings) {
         @Override
         public Chunk generate(int cx, int cz) {
            Chunk c = new Chunk(cx, cz);
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  c.biome[z * 16 + x] = (byte)Biomes.PLAINS;
                  int wx = cx * 16 + x;
                  int wz = cz * 16 + z;
                  int block = ((wx >> 3) + (wz >> 3)) % 2 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.OBSIDIAN;
                  c.set(x, 60, z, Blocks.BEDROCK);
                  for (int y = 61; y <= 64; y++) {
                     c.set(x, y, z, block);
                  }
               }
            }
            c.computeHeightmap();
            return c;
         }
      };
   }
}
```

For a complete production example of a Total Conversion mod in the repository, see:
* [`src/bc/InfdevMod.java`](../src/bc/InfdevMod.java) — Mod entrypoint, HUD/settings preset switching, and palette filtering.
* [`src/bc/InfdevGen.java`](../src/bc/InfdevGen.java) — Custom 3D/2D Perlin overhang terrain generator and Brick Pyramid structure generator.

---

## 6. Core Engine API Reference

### `bc.Game` (Main Game & UI State)
* `game.player` (`bc.Player`) — The local player entity.
* `game.world` (`bc.World`) — The currently loaded world (or `null` on the Title Screen).
* `game.mobs` (`bc.Mobs`) — Mob manager (`game.mobs.list`, `game.mobs.spawn(type, x, y, z)`).
* `game.inv` (`Stack[36]`) — Player inventory (`0..8` hotbar, `9..35` main inventory).
* `game.armor` (`Stack[4]`) — Equipped armor (`0` = Helmet, `1` = Chestplate, `2` = Leggings, `3` = Boots).
* `game.addItem(new Stack(id, count))` — Gives an item stack to the player's inventory.
* `game.chatMsg(String msg)` / `game.say(String msg)` — Prints a message to the in-game chat (supports `§0`..`§f` color codes).
* `game.meshNow(int x, int y, int z)` — Immediately rebuilds the 3D chunk mesh around `(x, y, z)`.
* `game.remeshAll()` — Marks all loaded chunk sections dirty so the world redraws.

### `bc.World` (Blocks, Items, Lighting & Time)
* `world.getBlock(int x, int y, int z)` — Returns the block ID at `(x, y, z)`.
* `world.setBlock(int x, int y, int z, int id)` — Sets the block at `(x, y, z)`, updates lighting, schedules fluid/block updates, and broadcasts the change in Multiplayer.
* `world.dropItem(double x, double y, double z, int id, int count)` — Spawns a dropped item entity in the world.
* `world.primeTnt(int x, int y, int z, int fuseTicks)` — Spawns an ignited TNT entity (`80` ticks = 4 seconds).
* `world.dayTime` (`long`) — World clock in ticks (`24000` ticks per day; `1000` = morning, `6000` = noon, `13000` = night).
* `world.seed` (`long`) — World generation seed.
* `world.dim` (`int`) — Dimension ID (`Dim.OVERWORLD = 0`, `Dim.NETHER = -1`, `Dim.END = 1`).

### `bc.Player` (Position, Velocity, Health & Modes)
* `player.x`, `player.y`, `player.z` (`double`) — World coordinates (feet position).
* `player.vx`, `player.vy`, `player.vz` (`double`) — Velocity in blocks/tick.
* `player.yaw`, `player.pitch` (`float`) — Look angles in degrees.
* `player.health` (`int`, `0..20`), `player.food` (`int`, `0..20`), `player.sat` (`float`).
* `player.setPos(double x, double y, double z)` — Teleports the player and updates their bounding box.
* `player.setMode(int mode)` — Switches game mode (`0` = Survival, `1` = Creative, `2` = Adventure, `3` = Spectator, `4` = Hardcore).

### Common Block & Item IDs (`bc.Blocks` & `bc.Items`)

| Constant | ID | Name | Constant | ID | Name |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `Blocks.AIR` | `0` | Air | `Items.STICK` | `256` | Stick |
| `Blocks.STONE` | `1` | Stone | `Items.COAL` | `257` | Coal |
| `Blocks.GRASS` | `2` | Grass Block | `Items.IRON_INGOT` | `258` | Iron Ingot |
| `Blocks.DIRT` | `3` | Dirt | `Items.GOLD_INGOT` | `259` | Gold Ingot |
| `Blocks.COBBLE` | `4` | Cobblestone | `Items.DIAMOND` | `260` | Diamond |
| `Blocks.PLANKS` | `5` | Oak Planks | `Items.LEATHER` | `261` | Leather |
| `Blocks.BEDROCK` | `6` | Bedrock | `Items.FEATHER` | `262` | Feather |
| `Blocks.SAND` | `7` | Sand | `Items.STRING` | `263` | String |
| `Blocks.GRAVEL` | `8` | Gravel | `Items.BONE` | `264` | Bone |
| `Blocks.LOG_OAK` | `9` | Oak Log | `Items.GUNPOWDER` | `265` | Gunpowder |
| `Blocks.GLASS` | `15` | Glass | `Items.FLINT` | `266` | Flint |
| `Blocks.BRICKS` | `16` | Bricks | `Items.ARROW` | `267` | Arrow |
| `Blocks.SANDSTONE` | `17` | Sandstone | `Items.BOW` | `268` | Bow |
| `Blocks.COAL_ORE` | `19` | Coal Ore | `Items.APPLE` | `270` | Apple |
| `Blocks.IRON_ORE` | `20` | Iron Ore | `Items.STEAK` | `275` | Steak |
| `Blocks.GOLD_ORE` | `21` | Gold Ore | `Items.ROTTEN_FLESH` | `280` | Rotten Flesh |
| `Blocks.DIAMOND_ORE` | `22` | Diamond Ore | `Items.BUCKET` | `282` | Bucket |
| `Blocks.GLOWSTONE` | `25` | Glowstone | `Items.WATER_BUCKET` | `283` | Water Bucket |
| `Blocks.OBSIDIAN` | `26` | Obsidian | `Items.LAVA_BUCKET` | `284` | Lava Bucket |
| `Blocks.STONEBRICK` | `27` | Stone Bricks | `Items.BREAD` | `287` | Bread |
| `Blocks.TORCH` | `32` | Torch | `Items.FLINT_STEEL` | `288` | Flint and Steel |
| `Blocks.WOOL0` | `34` | Wool (`34..49`) | `Items.ENDER_PEARL` | `348` | Ender Pearl |
| `Blocks.TNT` | `132` | TNT | `Items.BLAZE_ROD` | `349` | Blaze Rod |
| `Blocks.IRON_BLOCK` | `1132` | Block of Iron | `Items.EMERALD` | `360` | Emerald |
| `Blocks.GOLD_BLOCK` | `1133` | Block of Gold | `Blocks.DIAMOND_BLOCK` | `1134` | Block of Diamond |

* **Tools (`Items.toolId(tier, kind)`)**:
  * `tier`: `0` = Wood, `1` = Stone, `2` = Iron, `3` = Gold, `4` = Diamond
  * `kind`: `Items.C_PICK` (`1`), `Items.C_AXE` (`2`), `Items.C_SHOVEL` (`3`), `Items.C_SWORD` (`4`), `Items.C_HOE` (`5`)
* **Armor (`Items.armorId(tier, slot)`)**:
  * `tier`: `0` = Leather, `1` = Iron, `2` = Gold, `3` = Diamond, `4` = Chainmail
  * `slot`: `0` = Helmet, `1` = Chestplate, `2` = Leggings, `3` = Boots
* **Mob Types (`Mobs.spawn(type, x, y, z)`)**:
  * `0` = Pig, `1` = Cow, `2` = Sheep, `3` = Chicken, `4` = Zombie, `5` = Skeleton, `6` = Creeper, `7` = Spider, `8` = Zombie Pigman, `9` = Ghast, `10` = Blaze, `11` = Magma Cube, `12` = Enderman, `13` = Ender Dragon, `15` = Villager, `16` = Iron Golem, `17` = Parrot.

---

## 7. Creating Custom Resource Packs (`resourcepacks/`)

Mods can be paired with custom texture packs placed in the **`resourcepacks/`** folder (as either a subfolder or a `.zip` archive):

```text
resourcepacks/
└── my-pack/
    ├── pack.properties
    ├── terrain.png          # Optional: full 512x512 atlas override (32x32 grid of 16x16 tiles)
    └── blocks/
        ├── 1.png            # Optional: individual 16x16 tile override by tile index
        └── 4.png
```

Example `pack.properties`:
```properties
title=My Retro Texture Pack
author=YourName
description=Custom 16x16 block and item textures
```
