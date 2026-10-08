package bc;

import static bc.GameTests.check;
import static bc.GameTests.eq;
import static bc.GameTests.test;

import java.util.HashSet;
import java.util.Properties;

/**
 * Headless tests of Update 3.13: the new world generator ({@link TerraGen}, {@link FlatGen}, {@link WorldSettings}). The menus and the
 * inventory have their own suites ({@link TestsUi}, {@link TestsInv}).   python3 tools/runtests.py unit
 */
final class Tests313 {
   static void run() {
      terrain();
      soak();
      bonus();
      standing();
   }

   static Gen gen(long seed, int type) {
      WorldSettings st = new WorldSettings();
      st.type = type;
      return Dim.makeGen(Dim.OVERWORLD, seed, st);
   }

   /** A game in a world of this type (no renderer, nothing saved). */
   static Game terraGame(long seed, int type) throws Exception {
      Game g = GameTests.newGame(0);
      g.world.shutdown();
      WorldSettings st = new WorldSettings();
      st.type = type;
      World w = new World(seed, null, Dim.OVERWORLD, st);
      w.saveEnabled = false;
      w.weather.enabled = true;
      g.world = w;
      w.game = g;
      g.screen = Game.S.PLAY;
      g.player.setMode(0);
      return g;
   }

   /** The nearest column of this biome (or -1) within a radius, found with the generator's own column(). */
   static int[] findBiome(Gen g, int biome, int radius) {
      for (int ring = 0; ring < radius; ring += 24) {
         for (int k = 0; k < (ring == 0 ? 1 : ring / 6 + 8); k++) {
            double a = k * Math.PI * 2.0 / (ring == 0 ? 1 : ring / 6 + 8);
            int x = (int)(Math.cos(a) * ring);
            int z = (int)(Math.sin(a) * ring);
            Gen.Col c = g.column(x, z);
            if (c.biome == biome && (biome == Biomes.DEEP_OCEAN || biome == Biomes.OCEAN || c.h >= 63)) {
               return new int[]{x, z};
            }
         }
      }

      return null;
   }

   // ------------------------------------------------------------------ soak: a game in every kind of world, in every kind of place
   static void soak() {
      test("soak: a player in a new world walks through every biome at night and in the day; mobs spawn, the weather runs, nothing throws, the world stays loaded", () -> {
         int[] kinds = new int[]{Biomes.JUNGLE, Biomes.SNOWY_TAIGA, Biomes.BADLANDS, Biomes.SWAMP, Biomes.DEEP_OCEAN, Biomes.MOUNTAINS, Biomes.ICE_SPIKES, Biomes.SAVANNA,
            Biomes.DARK_FOREST, Biomes.MEADOW, Biomes.MUSHROOM, Biomes.FROZEN_PEAKS, Biomes.MEGA_TAIGA, Biomes.WARM_OCEAN, Biomes.FLOWER_FOREST, Biomes.RIVER};
         int visited = 0;
         int mobsSeen = 0;
         for (int type : new int[]{WorldSettings.DEFAULT, WorldSettings.FLAT, WorldSettings.AMPLIFIED}) {
            Game g = terraGame(31337L + type, type);
            try {
               g.mobs.rnd.setSeed(9L);
               int[] sp = g.world.gen.findSpawn();
               java.util.ArrayList<int[]> spots = new java.util.ArrayList<>();
               spots.add(new int[]{sp[0], sp[2]});
               if (type != WorldSettings.FLAT) {
                  for (int b : kinds) {
                     int[] at = findBiome(g.world.gen, b, 6000);
                     if (at != null) {
                        spots.add(at);
                     }
                  }
               }

               for (int[] s : spots) {
                  int cx = Math.floorDiv(s[0], 16);
                  int cz = Math.floorDiv(s[1], 16);
                  Tests312.load(g.world, cx, cz, 3);
                  int h = g.world.gen.column(s[0], s[1]).h;
                  g.player.setPos(s[0] + 0.5, Math.max(h, 63) + 1.5, s[1] + 0.5);
                  g.world.dayTime = visited % 2 == 0 ? 14500L : 4000L;
                  for (int t = 0; t < 300; t++) {
                     g.world.tick();
                     g.mobs.tick();
                     if (t % 50 == 0) {
                        g.world.updateChunks(cx, cz, 2, 4);
                     }
                  }

                  mobsSeen += g.mobs.list.size();
                  visited++;
                  check(g.world.chunk(cx, cz) != null, "the player's chunk is still loaded");
               }
            } finally {
               g.world.shutdown();
            }
         }

         check(visited >= 18, "visited " + visited + " places");
         check(mobsSeen > 0, "some mobs came to the places: " + mobsSeen);
      });
   }

   // ------------------------------------------------------------------ the spawn
   static void standing() {
      test("spawn: the player stands close to the spawn point (a tree may stand on it), on firm ground with room above - under snow and in tall grass too (that used to push him ten blocks away)", () -> {
         int snowy = 0;
         for (long seed = 1; seed <= 30; seed++) {
            int type = seed % 3 == 0 ? WorldSettings.LARGE : (seed % 3 == 1 ? WorldSettings.DEFAULT : WorldSettings.AMPLIFIED);
            Game g = terraGame(seed * 4001L + 5L, type);
            try {
               int[] sp = g.world.gen.findSpawn();
               Tests312.load(g.world, Math.floorDiv(sp[0], 16), Math.floorDiv(sp[2], 16), 3);
               double[] st = g.findStand(sp[0], sp[2]);
               check(st != null, "seed " + seed + ": a place to stand");
               if (st == null) {
                  continue;
               }

               int x = (int)Math.floor(st[0]);
               int y = (int)Math.floor(st[1]);
               int z = (int)Math.floor(st[2]);
               check(Math.abs(x - sp[0]) <= 6 && Math.abs(z - sp[2]) <= 6, "seed " + seed + ": at the spawn point, not " + (x - sp[0]) + "," + (z - sp[2]) + " away");
               int ground = g.world.getBlock(x, y - 1, z);
               check(Blocks.solid[ground] && !Blocks.isFluid(ground), "firm ground: " + Blocks.name[ground]);
               check(!Blocks.opaque[g.world.getBlock(x, y + 1, z)], "head room");
               if (Biomes.snowy(g.world.gen.column(sp[0], sp[2]).biome)) {
                  snowy++;
               }
            } finally {
               g.world.shutdown();
            }
         }

         check(snowy > 0, "some of the seeds start in the snow: " + snowy);
      });
   }

   // ------------------------------------------------------------------ bonus chest
   static void bonus() {
      test("bonus chest: next to the spawn, on the ground, with an axe, a pickaxe, wood and food; the same for every seed", () -> {
         for (int type : new int[]{WorldSettings.DEFAULT, WorldSettings.FLAT}) {
            for (long seed = 1; seed <= 6; seed++) {
               Game g = terraGame(seed * 77L, type);
               try {
                  int[] sp = g.world.gen.findSpawn();
                  Tests312.load(g.world, Math.floorDiv(sp[0], 16), Math.floorDiv(sp[2], 16), 3);
                  double[] stand = g.findStand(sp[0], sp[2]);
                  check(stand != null, "a place to stand");
                  int sx = (int)Math.floor(stand[0]);
                  int sy = (int)Math.floor(stand[1]);
                  int sz = (int)Math.floor(stand[2]);
                  int[] at = Bonus.place(g.world, sx, sy, sz, seed);
                  check(at != null, "seed " + seed + ": there is room for the chest");
                  if (at == null) {
                     continue;
                  }

                  check(Math.max(Math.abs(at[0] - sx), Math.abs(at[2] - sz)) <= 5, "within five blocks of the spawn");
                  eq(Blocks.CHEST, g.world.getBlock(at[0], at[1], at[2]), "a chest");
                  int below = g.world.getBlock(at[0], at[1] - 1, at[2]);
                  check(below != 0 && Blocks.solid[below], "on solid ground");
                  TileEntity t = g.world.tileAt(at[0], at[1], at[2]);
                  check(t != null, "with its contents");
                  boolean axe = false;
                  boolean pick = false;
                  boolean food = false;
                  boolean wood = false;
                  for (Stack s : t.slots) {
                     if (s == null) {
                        continue;
                     }

                     axe |= s.id == Items.toolId(Items.C_AXE, 0);
                     pick |= s.id == Items.toolId(Items.C_PICK, 0);
                     food |= s.id == Items.BREAD || s.id == Items.APPLE;
                     wood |= s.id == Blocks.LOG_OAK || s.id == Blocks.LOG_BIRCH || s.id == Blocks.LOG_SPRUCE;
                  }

                  check(axe && pick && food && wood, "an axe, a pickaxe, food and logs");
               } finally {
                  g.world.shutdown();
               }
            }
         }
      });
   }

   // ------------------------------------------------------------------ terrain
   static void terrain() {
      test("terrain: a chunk is a function of the seed and its place; every id in it is a real block; bedrock below, air above", () -> {
         for (int type : new int[]{WorldSettings.DEFAULT, WorldSettings.LARGE, WorldSettings.AMPLIFIED, WorldSettings.FLAT}) {
            Gen a = gen(777L, type);
            Gen b = gen(777L, type);
            for (int k = 0; k < 6; k++) {
               int cx = k * 37 - 90;
               int cz = 211 - k * 53;
               Chunk x = a.generate(cx, cz);
               Chunk y = b.generate(cx, cz);
               check(java.util.Arrays.equals(x.blocks, y.blocks), "type " + type + " chunk " + cx + "," + cz + " is the same twice");
               check(java.util.Arrays.equals(x.biome, y.biome), "and its biomes");
               boolean bad = false;
               boolean bedrock = true;
               for (int i = 0; i < x.blocks.length; i++) {
                  int id = x.blocks[i] & 0xFFFF;
                  if (id != 0 && !Blocks.isBlockId(id)) {
                     bad = true;
                  }
               }

               for (int zz = 0; zz < 16; zz++) {
                  for (int xx = 0; xx < 16; xx++) {
                     bedrock &= x.get(xx, 0, zz) == Blocks.BEDROCK;
                     check(x.get(xx, 255, zz) == 0, "nothing at the build limit");
                  }
               }

               check(!bad, "type " + type + ": only block ids");
               check(bedrock, "type " + type + ": a bedrock floor");
            }
         }
      });

      test("terrain: column() - what the LOD, the villages and the weather read - is the height of the real ground, and the ground below sea level is under water", () -> {
         for (int type : new int[]{WorldSettings.DEFAULT, WorldSettings.LARGE, WorldSettings.AMPLIFIED}) {
            Gen g = gen(4242L, type);
            int wrong = 0;
            int total = 0;
            int dry = 0;
            for (int cz = -6; cz < 6; cz++) {
               for (int cx = -6; cx < 6; cx++) {
                  Chunk c = g.generate(cx * 5, cz * 5);
                  for (int z = 0; z < 16; z += 3) {
                     for (int x = 0; x < 16; x += 3) {
                        int wx = c.cx * 16 + x;
                        int wz = c.cz * 16 + z;
                        Gen.Col col = g.column(wx, wz);
                        if (g.entrance(wx, wz) || ((TerraGen)g).ravine(wx, wz) > 0.0) {
                           continue; // a cave mouth or a canyon: the real ground is lower than the LOD's
                        }

                        total++;
                        int ground = c.get(x, col.h, z);
                        if (ground == 0 || !Blocks.solid[ground] || Blocks.isFluid(ground)) {
                           wrong++;
                        }

                        if (col.h < Gen.SEA) {
                           for (int y = col.h + 1; y < Gen.SEA; y++) {
                              int id = c.get(x, y, z);
                              if (!Blocks.isFluid(id) && id != Blocks313.ICE) {
                                 dry++;
                              }
                           }
                        }
                     }
                  }
               }
            }

            check(wrong * 200 < total, "type " + type + ": column().h is solid ground (" + wrong + " of " + total + " are not)");
            eq(0, dry, "type " + type + ": no air pockets between the sea floor and sea level");
         }
      });

      test("terrain: 28 biomes, a believable share of land, rivers and oceans, no biome id outside the table", () -> {
         long[] count = new long[Biomes.N];
         long land = 0;
         long total = 0;
         for (long seed = 1; seed <= 6; seed++) {
            Gen g = gen(seed * 7919L, WorldSettings.DEFAULT);
            for (int j = -120; j < 120; j++) {
               for (int i = -120; i < 120; i++) {
                  Gen.Col c = g.column(i * 24, j * 24);
                  check(c.biome >= 0 && c.biome < Biomes.N, "biome in the table");
                  count[c.biome]++;
                  total++;
                  if (c.h >= Gen.SEA) {
                     land++;
                  }
               }
            }
         }

         int seen = 0;
         for (int b = 0; b < Biomes.N; b++) {
            if (count[b] > 0) {
               seen++;
            } else {
               check(false, "biome " + Biomes.NAME[b] + " never appears");
            }
         }

         eq(Biomes.N, seen, "every biome appears");
         double share = land * 100.0 / total;
         check(share > 45.0 && share < 85.0, "land share " + share);
         check(count[Biomes.RIVER] * 100.0 / total > 1.0, "rivers exist");
         check((count[Biomes.OCEAN] + count[Biomes.DEEP_OCEAN]) * 100.0 / total > 8.0, "oceans exist");
         for (int b = 0; b < Biomes.N; b++) {
            check(count[b] * 100.0 / total < 30.0, Biomes.NAME[b] + " does not cover a third of the world");
         }
      });

      test("terrain: the world spawn is on dry land, not in a river, and near the middle; the same for many seeds and every world type", () -> {
         for (int type : new int[]{WorldSettings.DEFAULT, WorldSettings.LARGE, WorldSettings.AMPLIFIED, WorldSettings.FLAT}) {
            for (long seed = 1; seed <= 25; seed++) {
               Gen g = gen(seed * 104729L + 13L, type);
               int[] sp = g.findSpawn();
               Gen.Col c = g.column(sp[0], sp[2]);
               check(c.h >= Gen.SEA, "type " + type + " seed " + seed + ": the spawn is on land (h " + c.h + ", " + Biomes.name(c.biome) + ")");
               check(!Biomes.ocean(c.biome) && !Biomes.river(c.biome), "not in an ocean or river: " + Biomes.name(c.biome));
               check(Math.abs(sp[0]) < 4000 && Math.abs(sp[2]) < 4000, "near the middle: " + sp[0] + "," + sp[2]);
               check(sp[1] == c.h + 1 || type == WorldSettings.FLAT, "the spawn stands on the ground");
            }
         }
      });

      test("terrain: Superflat is flat and follows its preset; Large Biomes are bigger; Amplified is taller", () -> {
         String[] top = new String[WorldSettings.FLAT_PRESETS];
         for (int p = 0; p < WorldSettings.FLAT_PRESETS; p++) {
            WorldSettings st = new WorldSettings();
            st.type = WorldSettings.FLAT;
            st.flat = p;
            Gen g = Dim.makeGen(Dim.OVERWORLD, 5L, st);
            Chunk c = g.generate(3, -4);
            int h = -1;
            for (int y = 255; y > 0; y--) {
               if (c.get(8, y, 8) != 0) {
                  h = y;
                  break;
               }
            }

            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  int hh = -1;
                  for (int y = 255; y > 0; y--) {
                     if (c.get(x, y, z) != 0) {
                        hh = y;
                        break;
                     }
                  }

                  eq(h, hh, "preset " + p + " is flat");
               }
            }

            top[p] = Blocks.name[c.get(8, h, 8)];
            eq(Blocks.BEDROCK, c.get(8, 0, 8), "bedrock floor");
         }

         check(!top[0].equals(top[1]) || !top[1].equals(top[2]), "the presets differ: " + String.join(",", top));
         // large biomes: along a line there are fewer biome changes; amplified: a bigger spread of heights
         int[] changes = new int[2];
         int[] spread = new int[2];
         for (int k = 0; k < 2; k++) {
            Gen g = gen(31L, k == 0 ? WorldSettings.DEFAULT : WorldSettings.LARGE);
            int last = -1;
            for (int i = 0; i < 6000; i++) {
               int b = g.column(i * 8, 5000).biome;
               if (b != last) {
                  changes[k]++;
                  last = b;
               }
            }
         }

         check(changes[1] * 2 < changes[0], "large biomes change " + changes[1] + " times, default " + changes[0]);
         for (int k = 0; k < 2; k++) {
            Gen g = gen(31L, k == 0 ? WorldSettings.DEFAULT : WorldSettings.AMPLIFIED);
            int lo = 999;
            int hi = 0;
            for (int i = 0; i < 4000; i++) {
               int h = g.column(i * 13 - 20000, i * 7).h;
               lo = Math.min(lo, h);
               hi = Math.max(hi, h);
            }

            spread[k] = hi - lo;
         }

         check(spread[1] > spread[0], "amplified spans " + spread[1] + " blocks, default " + spread[0]);
      });

      test("terrain: world settings are saved with the world; a world without them is a classic one (its chunks must not change)", () -> {
         WorldSettings st = new WorldSettings();
         st.type = WorldSettings.AMPLIFIED;
         st.structures = false;
         st.bonusChest = true;
         st.flat = 2;
         Properties p = new Properties();
         st.save(p);
         WorldSettings back = WorldSettings.load(p);
         eq(WorldSettings.AMPLIFIED, back.type, "type");
         check(!back.structures && back.bonusChest, "switches");
         eq(2, back.flat, "preset");
         eq(WorldSettings.CLASSIC, WorldSettings.load(new Properties()).type, "no gentype: classic");
         eq(WorldSettings.CLASSIC, WorldSettings.load(null).type, "no file: classic");
         Properties bad = new Properties();
         bad.setProperty("gentype", "banana");
         eq(WorldSettings.CLASSIC, WorldSettings.load(bad).type, "garbage: classic");
         bad.setProperty("gentype", "99");
         check(WorldSettings.load(bad).type < WorldSettings.TYPES, "a type out of range is clamped");
         // the classic generator is the old one, bit for bit
         Gen old = Dim.makeGen(Dim.OVERWORLD, 12345L, WorldSettings.classic());
         check(old.getClass() == Gen.class, "classic = Gen");
      });

      test("terrain: every block a generated chunk contains has a picture and a colour for the distant terrain (no black tiles)", () -> {
         Tex.initPixels();
         HashSet<Integer> surface = new HashSet<>();
         for (int type : new int[]{WorldSettings.DEFAULT, WorldSettings.LARGE}) {
            Gen g = gen(99L + type, type);
            for (int k = 0; k < 160; k++) {
               int cx = (k % 16 - 8) * 23 + 5;
               int cz = (k / 16 - 5) * 29 - 9;
               Chunk c = g.generate(cx, cz);
               for (int z = 0; z < 16; z += 2) {
                  for (int x = 0; x < 16; x += 2) {
                     for (int y = 255; y > 0; y--) {
                        int id = c.get(x, y, z);
                        if (id != 0) {
                           surface.add(id);
                           break;
                        }
                     }
                  }
               }
            }
         }

         check(surface.size() > 25, "a lot of different surfaces: " + surface.size());
         for (int id : surface) {
            check(Blocks.isBlockId(id), "block " + id);
            check(Blocks.name[id] != null && !Blocks.name[id].isEmpty(), "block " + id + " has a name");
            check(Blocks.tiles[id] != null, "block " + id + " (" + Blocks.name[id] + ") has a picture");
            if (Blocks.opaque[id] && !Blocks.isFluid(id)) {
               // (LodMap.averages() is cached by the first test that asks, maybe before the atlas existed: measure the tiles themselves)
               check(LodMap.avgTile(Blocks.tiles[id][0]) >= 0 && LodMap.avgTile(Blocks.tiles[id][Math.min(2, Blocks.tiles[id].length - 1)]) >= 0, "the picture of " + Blocks.name[id] + " (" + id + ") is not empty");
            }
         }
      });

      test("terrain: villages, strongholds and the like only when the world was made with structures; trees stand on earth, not in water", () -> {
         int villagesOn = 0;
         int villagesOff = 0;
         for (long seed = 1; seed <= 4; seed++) {
            WorldSettings on = new WorldSettings();
            WorldSettings off = new WorldSettings();
            off.structures = false;
            Gen a = Dim.makeGen(Dim.OVERWORLD, seed * 31L, on);
            Gen b = Dim.makeGen(Dim.OVERWORLD, seed * 31L, off);
            for (int cz = -20; cz < 20; cz += 1) {
               for (int cx = -20; cx < 20; cx += 1) {
                  Chunk ca = a.generate(cx * 2, cz * 2);
                  Chunk cb = b.generate(cx * 2, cz * 2);
                  for (int y = 55; y < 100; y++) {
                     if (ca.get(7, y, 7) == Blocks.PLANKS || ca.get(3, y, 11) == Blocks.PLANKS) {
                        villagesOn++;
                        break;
                     }
                  }

                  for (int y = 55; y < 100; y++) {
                     if (cb.get(7, y, 7) == Blocks.PLANKS || cb.get(3, y, 11) == Blocks.PLANKS) {
                        villagesOff++;
                        break;
                     }
                  }
               }
            }
         }

         check(villagesOn > 0, "with structures there are villages' planks: " + villagesOn);
         eq(0, villagesOff, "without structures there are no planks lying about");
      });
   }
}
