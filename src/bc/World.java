package bc;

import bc.Chunk.Section;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class World {
   public final long seed;
   /** Update 3.12: 0 = Overworld, -1 = Nether, 1 = End (see Dim). */
   public final int dim;
   public Gen gen;
   public final File dir;
   public final ConcurrentHashMap<Long, Chunk> chunks = new ConcurrentHashMap();
   public long tickCount;
   /** Invalidates LOD coverage caches when chunk residency or mesh-relevant block state changes. */
   public final AtomicLong meshCoverageEpoch = new AtomicLong();
   final RedstoneLogic.State redstone = new RedstoneLogic.State();
   final Kinetics.State kinetics=new Kinetics.State();
   final Circuit.State circuit = new Circuit.State();
   final RasterComputer.State raster = new RasterComputer.State();
   public long dayTime = 1000L;
   public Consumer<Chunk> unloadHook;
   public boolean saveEnabled = true;
   public Game game;
   public final HashMap<Long, TileEntity> tiles = new HashMap();
   public final ArrayList<World.Item> items = new ArrayList();
   /** Update 3.12: the fight against the Ender Dragon (only in the End). */
   public EndFight fight;
   /** Update 3.12: villages (region key) that have been given their villagers; saved in villages.dat. */
   public final java.util.HashSet<Long> populated = new java.util.HashSet<>();
   public final ArrayList<World.Falling> fallers = new ArrayList();
   /** Fire (Update 3.7): ages, spreading, burning out. */
   public final Fire fire = new Fire(this);
   /** Update 3.10: trees that are falling over (see Trees). */
   public final Trees trees = new Trees(this);
   /** Update 3.10: tornadoes, earthquakes, meteors (see Disasters). */
   public final Disasters disasters = new Disasters(this);
   /** Update 3.11: what the distant terrain shows for chunks that were changed (see LodMap). */
   public final LodMap lodMap = new LodMap();
   /** Update 3.8: weather field, rain, lightning. */
   public final Weather weather;
   private final Random rnd = new Random();
   private final ExecutorService pool;
   private final ConcurrentLinkedQueue<Chunk> ready = new ConcurrentLinkedQueue();
   private final Set<Long> requested = ConcurrentHashMap.newKeySet();
   private final ExecutorService lightingPool;
   /** Update 6.18: chunk files are written off the render thread (unless "Async chunk saves" is turned off). */
   private final ExecutorService savePool;
   /** Update 6.18: the timed save walks the chunks a handful at a time instead of stopping the game. */
   boolean savePending;
   private Iterator<Chunk> saveCursor;
   private long nextAutoSave = 1200L;
   /** Update 6.18: reused each tick for the tile-entity pass (used to copy the whole map into a new ArrayList). */
   private final ArrayList<TileEntity> tileSnapshot = new ArrayList();
   private final AtomicInteger lightingPending = new AtomicInteger();
   private static final int MAX_LIGHTING_PENDING = 8;
   private Iterator<Chunk> unloadCursor;
   private Iterator<Chunk> lodCursor;
   private static final ThreadLocal<LightScratch> LIGHT_SCRATCH = ThreadLocal.withInitial(LightScratch::new);
   private static final int[][] ORDER;
   private static final int OFF = 33554432;
   private final ReentrantLock lightingLock = new ReentrantLock();
   private static final class LightScratch {
      final LongQ addQ = new LongQ();
      final LongQ remQ = new LongQ();
      int bx0, by0, bz0, bx1, by1, bz1;
   }
   private LightScratch scratch() {
      return LIGHT_SCRATCH.get();
   }
   static final int[] DX;
   static final int[] DY;
   static final int[] DZ;
   private final TreeMap<Long, ArrayList<Long>> fluidDue = new TreeMap();
   private final HashSet<Long> fluidPending = new HashSet();
   private final ArrayDeque<Long> blockUpdates = new ArrayDeque();
   private final HashSet<Long> blockPending = new HashSet();
   static final int[][] LOOT;
   private final double[] res = new double[3];
   static final double[] FULL;
   public boolean rayFluids;
   public boolean inPassiveTick;
   /** Update 3.13: what the world was created with (generator type, structures). */
   public final WorldSettings settings;

   public World(long var1, File var3) {
      this(var1, var3, Dim.OVERWORLD);
   }

   public World(long var1, File var3, int dimension) {
      this(var1, var3, dimension, WorldSettings.classic());
   }

   /** Update 3.13: the world settings pick the Overworld generator (old worlds and the tests use the classic one). */
   public World(long var1, File var3, int dimension, WorldSettings st) {
      FastChunkCache.clear();
      this.seed = var1;
      this.dim = dimension;
      this.settings = st == null ? WorldSettings.classic() : st;
      this.gen = Dim.makeGen(dimension, var1, this.settings);
      this.weather = new Weather(this);
      if (dimension != Dim.OVERWORLD) {
         this.weather.enabled = false;
         this.disasters.enabled = false;
      }

      this.dir = var3;
      if (var3 != null) {
         var3.mkdirs();
      }

      this.fight = dimension == Dim.END ? new EndFight(this) : null;
      this.loadPopulated();
      this.loadTiles();
      RasterComputer.load(this);
      RasterMedia.load(this);NoteBlocks.load(this);KineticMachines.load(this);
      if (var3 != null && dimension == Dim.OVERWORLD) {
         this.lodMap.load(var3);
         // chunk files from before 3.11 have no summary yet: a background thread makes them (chunks that are loaded are summarised from live data)
         this.lodMap.scan(var3, this.gen, (cx, cz) -> this.chunks.containsKey(key(cx, cz)));
      }

      this.pool = Executors.newFixedThreadPool(genThreads(), var0 -> {
         Thread var1x = new Thread(var0, "chunk-gen");
         var1x.setDaemon(true);
         var1x.setPriority(Thread.NORM_PRIORITY - 1);
         return var1x;
      });
      this.lightingPool = Executors.newSingleThreadExecutor(var0 -> {
         Thread worker = new Thread(var0, "chunk-lighting");
         worker.setDaemon(true);
         worker.setPriority(Thread.MIN_PRIORITY);
         return worker;
      });
      this.savePool = Executors.newSingleThreadExecutor(var0 -> {
         Thread worker = new Thread(var0, "chunk-save");
         worker.setDaemon(true);
         worker.setPriority(Thread.MIN_PRIORITY);
         return worker;
      });
   }

   /** Update 6.18: the Advanced Video worker count; 0 means "let the CPU decide". Applied when a world/renderer is created. */
   private static volatile int genThreadsOverride = 0;

   public static void setThreadTuning(Options o) {
      genThreadsOverride = o == null ? 0 : Options.clamp(o.genThreads, 0, 8);
   }

   static int autoGenThreads() {
      return Math.max(2, Math.min(6, Runtime.getRuntime().availableProcessors() - 2));
   }

   static int genThreads() {
      return genThreadsOverride > 0 ? Math.min(8, genThreadsOverride) : autoGenThreads();
   }

   static int genThreads(Options o) {
      int fixed = o == null ? 0 : Options.clamp(o.genThreads, 0, 8);
      return fixed > 0 ? Math.min(8, fixed) : autoGenThreads();
   }

   /** Roughly 256 KB of primitive arrays per loaded chunk; the Advanced Video cache budget is in MiB. */
   static int maxLoadedChunks(int megabytes) {
      return Math.max(64, (int)((long)Options.clamp(megabytes, 32, 2048) * 1024L * 1024L / 262144L));
   }

   public static long key(int var0, int var1) {
      return (long)var0 << 32 | var1 & 4294967295L;
   }

   public Chunk chunk(int var1, int var2) {
      FastChunkCache cache = FastChunkCache.TL.get();
      int slot = ((var1 & 31) << 5) | (var2 & 31);
      if (cache.lastWorld == this) {
         Chunk c = cache.chunks[slot];
         if (c != null && !c.unloaded && c.cx == var1 && c.cz == var2) {
            return c;
         }
         c = (Chunk)this.chunks.get(key(var1, var2));
         if (c != null && !c.unloaded) {
            cache.chunks[slot] = c;
         }
         return c;
      }
      Chunk c = (Chunk)this.chunks.get(key(var1, var2));
      if (c != null && !c.unloaded) {
         cache.switchWorld(this, c, slot);
      }
      return c;
   }

   public Chunk chunkAt(int var1, int var2) {
      return this.chunk(var1 >> 4, var2 >> 4);
   }

   public boolean isLoaded(int var1, int var2) {
      return this.chunk(var1 >> 4, var2 >> 4) != null;
   }

   public int getBlock(int var1, int var2, int var3) {
      if (var2 >= 0 && var2 < Chunk.H) {
         Chunk var4 = this.chunk(var1 >> 4, var3 >> 4);
         return var4 == null ? 0 : var4.blocks[Chunk.idx(var1 & 15, var2, var3 & 15)] & BlockState.ID_MASK;
      } else {
         return 0;
      }
   }

   public int getSky(int var1, int var2, int var3) {
      if (var2 >= Chunk.H) {
         return 15;
      }

      if (var2 < 0) {
         return 0;
      }

      Chunk var4 = this.chunkAt(var1, var3);
      return var4 == null ? 15 : var4.sky(var1 & 15, var2, var3 & 15);
   }

   public int getBlk(int var1, int var2, int var3) {
      if (var2 >= 0 && var2 < Chunk.H) {
         Chunk var4 = this.chunkAt(var1, var3);
         return var4 == null ? 0 : var4.blk(var1 & 15, var2, var3 & 15);
      } else {
         return 0;
      }
   }

   private File chunkFile(int var1, int var2) {
      return new File(this.dir, "c." + var1 + "." + var2 + ".gz");
   }

   private void requestChunk(int var1, int var2) {
      long var3 = key(var1, var2);
      if (this.requested.add(var3)) {
         Gen genSnap = this.gen;
         this.pool.execute(() -> {
            if (this.gen != genSnap || !this.requested.contains(var3)) {
               return;
            }
            try {
               Chunk var5 = genSnap.generate(var1, var2);
               if (this.gen != genSnap || !this.requested.contains(var3)) {
                  return;
               }
               File var6 = this.dir == null ? null : this.chunkFile(var1, var2);
               if (var6 != null && var6.exists()) {
                  try {
                     DataInputStream var7 = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(var6))));

                     try {
                        Chunk.readBlocks(var7, var5.blocks);
                        var5.computeHeightmap();
                     } catch (Throwable var11) {
                        try {
                           var7.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }

                        throw var11;
                     }

                     var7.close();
                  } catch (Exception var12) {
                     System.err.println("Failed to read chunk " + var1 + "," + var2 + ": " + var12);
                  }
               }

               if (this.gen != genSnap || !this.requested.contains(var3)) {
                  return;
               }
               var5.recomputeSectionOccupancy();
               Net319.applyEditsToChunk(this, var5);
               CaveCull.computeChunk(var5);
               this.ready.add(var5);
            } catch (Throwable var13) {
               var13.printStackTrace();
               this.requested.remove(var3);
            }
         });
      }
   }

   private boolean reserveLightingSlot() {
      if (this.lightingPending.incrementAndGet() > MAX_LIGHTING_PENDING) {
         this.lightingPending.decrementAndGet();
         return false;
      }
      return true;
   }

   private void submitReservedLighting(Chunk chunk) {
      try {
         this.lightingPool.execute(() -> {
            try {
               this.lightingLock.lock();
               try {
                  if (!chunk.unloaded && this.chunks.get(key(chunk.cx, chunk.cz)) == chunk) {
                     this.fire.scanChunk(chunk);
                     this.lightChunkLocked(chunk);
                  }
               } finally {
                  this.lightingLock.unlock();
               }
            } finally {
               this.lightingPending.decrementAndGet();
            }
         });
      } catch (RuntimeException e) {
         this.lightingPending.decrementAndGet();
         throw e;
      }
   }

   public void updateChunks(int var1, int var2, int var3, int var4) {
      this.updateChunks(var1, var2, var3, var4, 3000000L);
   }

   public void updateChunks(int var1, int var2, int var3, int var4, long var5) {
      boolean isVoidArena = this.settings != null && this.settings.isVoidMode();
      int maxVoidCoord = 6;
      if (!isVoidArena && !this.requested.isEmpty()) {
         int maxReqDist = var3 + 2;
         this.requested.removeIf(rk -> {
            int rcx = (int)(rk.longValue() >> 32);
            int rcz = (int)rk.longValue();
            return Math.max(Math.abs(rcx - var1), Math.abs(rcz - var2)) > maxReqDist;
         });
      }
      int var7 = this.requested.size();
      int var8 = genThreads() * 6;
      long var9 = System.nanoTime();
      if (var7 < var8) {
         int effR = isVoidArena ? Math.max(var3, 10) : var3;
         int var11 = (effR + 1) * (effR + 1) + 2;
         int var12 = 0;

         for (int[] var16 : ORDER) {
            if (var16[0] * var16[0] + var16[1] * var16[1] > var11) {
               break;
            }

            int var17 = var1 + var16[0];
            int var18 = var2 + var16[1];
            if (isVoidArena && (var17 < -6 || var17 > 5 || var18 < -6 || var18 > 5)) {
               continue;
            }
            long reqKey = key(var17, var18);
            if (!this.chunks.containsKey(reqKey) && !this.requested.contains(reqKey)) {
               this.requestChunk(var17, var18);
               if (++var12 + var7 >= var8) {
                  break;
               }
            }
         }
      }

      for (int var20 = 0; var20 < var4 && (var20 <= 0 || System.nanoTime() - var9 <= var5); var20++) {
         Chunk var22 = (Chunk)this.ready.poll();
         if (var22 == null) {
            break;
         }

         long readyKey = key(var22.cx, var22.cz);
         this.requested.remove(readyKey);
         if (this.chunks.containsKey(readyKey)) {
            continue;
         }
         if (isVoidArena && (Math.abs(var22.cx) > maxVoidCoord || Math.abs(var22.cz) > maxVoidCoord)) {
            continue;
         }
         boolean inArenaBounds = isVoidArena && Math.abs(var22.cx) <= maxVoidCoord && Math.abs(var22.cz) <= maxVoidCoord;
         if (inArenaBounds || Math.max(Math.abs(var22.cx - var1), Math.abs(var22.cz - var2)) <= var3 + 3) {
            if (!this.reserveLightingSlot()) {
               this.ready.add(var22);
               this.requested.add(readyKey);
               break;
            }
            this.chunks.put(readyKey, var22);
            this.meshCoverageEpoch.incrementAndGet();
            RedstoneLogic.loaded(this, var22);
            Circuit.loaded(this,var22);Kinetics.loaded(this,var22);
            RasterComputer.loaded(this,var22);
            this.submitReservedLighting(var22);

            for (int var24 = -1; var24 <= 1; var24++) {
               for (int var25 = -1; var25 <= 1; var25++) {
                  Chunk var26 = this.chunk(var22.cx + var24, var22.cz + var25);
                  if (var26 != null) {
                     if (var26 == var22 || !var26.hasMeshed) {
                        for (Section var19 : var26.sections) {
                           var19.dirty = true;
                        }
                     } else if (Math.abs(var24) + Math.abs(var25) == 1) {
                        for (int sy = 0; sy < Chunk.SECTIONS; sy++) {
                           if (!var26.isSectionEmpty(sy) && !var22.isSectionEmpty(sy)) {
                              var26.sections[sy].dirty = true;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (this.unloadCursor == null) {
         this.unloadCursor = this.chunks.values().iterator();
      }
      int scanned = 0;
      int unloaded = 0;
      Options tuning = this.game == null ? null : this.game.opt;
      int unloadBudget = tuning == null ? 2 : Options.clamp(tuning.unloadBudget, 1, 32);
      // Update 6.18: a hard chunk-memory budget. When it is exceeded the unload radius grows a little, but the
      // per-frame work stays bounded so walking away from a huge base cannot produce a single giant frame.
      boolean pressure = this.chunks.size() > maxLoadedChunks(tuning == null ? 128 : tuning.chunkCacheMb);
      int unloadMargin = pressure ? 1 : 3;
      while (this.unloadCursor.hasNext() && scanned++ < 128 && unloaded < unloadBudget) {
         Chunk var23 = this.unloadCursor.next();
         if (isVoidArena && Math.abs(var23.cx) <= maxVoidCoord && Math.abs(var23.cz) <= maxVoidCoord) {
            continue; // Never unload active minigame arena chunks (-5..5 x -5..5)
         }
         boolean outside = (isVoidArena && (Math.abs(var23.cx) > maxVoidCoord || Math.abs(var23.cz) > maxVoidCoord))
            || Math.max(Math.abs(var23.cx - var1), Math.abs(var23.cz - var2)) > var3 + unloadMargin;
         if (!outside || !this.lightingLock.tryLock()) {
            continue;
         }
         boolean removed = false;
         try {
            long chunkKey = key(var23.cx, var23.cz);
            if (!var23.lighting && this.chunks.get(chunkKey) == var23) {
               Kinetics.unloaded(this,var23);
               RedstoneLogic.unloaded(this, var23);
               var23.unloaded = true;
               this.chunks.remove(chunkKey, var23);
               this.meshCoverageEpoch.incrementAndGet();
               this.raster.dirty=true;this.kinetics.dirty=true;
               removed = true;
            }
         } finally {
            this.lightingLock.unlock();
         }
         if (removed) {
            unloaded++;
            if (var23.modified) {
               this.saveChunk(var23);
            }
            if (this.unloadHook != null) {
               this.unloadHook.accept(var23);
            }
         }
      }
      if (!this.unloadCursor.hasNext()) {
         this.unloadCursor = null;
      }
   }

   /** Synchronously ensures that chunk (cx, cz) is generated, lit, and resident in this.chunks. */
   public Chunk ensureChunkLoaded(int cx, int cz) {
      Chunk existing = this.chunk(cx, cz);
      if (existing != null) {
         return existing;
      }
      long k = key(cx, cz);
      this.requested.remove(k);
      Chunk fresh = this.gen.generate(cx, cz);
      fresh.modified = false;
      fresh.unloaded = false;
      fresh.recomputeSectionOccupancy();
      Net319.applyEditsToChunk(this, fresh);
      CaveCull.computeChunk(fresh);

      this.lightingLock.lock();
      try {
         existing = this.chunk(cx, cz);
         if (existing != null) {
            return existing;
         }
         this.chunks.put(k, fresh);
         this.meshCoverageEpoch.incrementAndGet();
         this.fire.scanChunk(fresh);
         this.lightChunkLocked(fresh);
         Circuit.loaded(this,fresh);Kinetics.loaded(this,fresh);
         RasterComputer.loaded(this,fresh);
         for (Section s : fresh.sections) {
            s.dirty = true;
         }
      } finally {
         this.lightingLock.unlock();
      }
      return fresh;
   }

   /** Ensures all 12x12 minigame arena chunks (-6..5 x -6..5, including border ring around -5..4) are resident and lit. */
   public void ensureArenaChunksLoaded() {
      if (this.settings == null || !this.settings.isVoidMode()) {
         return;
      }
      for (int cz = -6; cz <= 5; cz++) {
         for (int cx = -6; cx <= 5; cx++) {
            if (this.chunk(cx, cz) == null) {
               this.ensureChunkLoaded(cx, cz);
            }
         }
      }
   }

   public int loadedAround(int var1, int var2, int var3) {
      int var4 = 0;

      for (int var5 = -var3; var5 <= var3; var5++) {
         for (int var6 = -var3; var6 <= var3; var6++) {
            Chunk var7 = this.chunk(var1 + var5, var2 + var6);
            if (var7 != null && var7.lit) {
               var4++;
            }
         }
      }

      return var4;
   }

   /**
    * Update 6.18: a chunk file is a GZIP write of 128 KB of block data. Doing that on the render thread was one of the
    * classic "one huge frame" spikes whenever you crossed a chunk border or quit the game. The async path takes a
    * snapshot, clears the dirty flag and lets a low-priority thread do the I/O; the LOD summary stays on this thread.
    */
   public void saveChunk(Chunk var1) {
      if (!this.saveEnabled || this.dir == null) return;
      Options o = this.game == null ? null : this.game.opt;
      boolean async = o == null || o.asyncSaves;
      if (!async) {
         this.writeChunk(var1, var1.blocks);
         return;
      }
      final short[] snapshot = var1.blocks.clone();
      final int cx = var1.cx, cz = var1.cz;
      final Chunk owner = var1;
      owner.modified = false;
      owner.lodDirty = 0L;
      if (this.dim == Dim.OVERWORLD) {
         this.lodMap.update(owner, this.gen);
         this.lodMap.stamp(cx, cz, System.currentTimeMillis());
      }
      Runnable write = () -> {
         if (!this.writeChunkBytes(cx, cz, snapshot)) {
            owner.modified = true; // retry at the next save
         }
      };
      try {
         this.savePool.execute(write);
      } catch (java.util.concurrent.RejectedExecutionException ex) {
         write.run();
      }
   }

   private void writeChunk(Chunk c, short[] blocks) {
      if (this.writeChunkBytes(c.cx, c.cz, blocks)) {
         c.modified = false;
         c.lodDirty = 0L;
         if (this.dim == Dim.OVERWORLD) {
            this.lodMap.update(c, this.gen);
            this.lodMap.stamp(c.cx, c.cz, this.chunkFile(c.cx, c.cz).lastModified());
         }
      }
   }

   private boolean writeChunkBytes(int cx, int cz, short[] blocks) {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(this.chunkFile(cx, cz)))))) {
         Chunk.writeBlocks(out, blocks);
         return true;
      } catch (IOException e) {
         System.err.println("Failed to save chunk: " + e);
         return false;
      }
   }

   /** Update 3.11: refresh only a small slice per tick to avoid a full-map scan spike. */
   void lodRefresh() {
      if (this.lodCursor == null) {
         this.lodCursor = this.chunks.values().iterator();
      }
      int scanned = 0;
      int refreshed = 0;
      while (this.lodCursor.hasNext() && scanned++ < 512 && refreshed < 4) {
         Chunk c = this.lodCursor.next();
         long d = c.lodDirty;
         if (d != 0L && this.tickCount - d >= 40L) {
            c.lodDirty = 0L;
            this.lodMap.update(c, this.gen);
            refreshed++;
         }
      }
      if (!this.lodCursor.hasNext()) {
         this.lodCursor = null;
      }
   }

   /**
    * Update 6.18: starting a save no longer writes every chunk in a single frame. The timed autosave calls
    * {@link #saveStep(int)} with a small budget each tick; a shutdown save passes an unlimited budget so the
    * world is complete before the process exits.
    */
   void startSave() {
      this.trees.finishAll(); // a tree in the air is put down first, or its blocks would be lost
      this.saveCursor = this.chunks.values().iterator();
      this.savePending = true;
   }

   /** Saves at most {@code maxChunks} chunks and, at the end of the walk, everything else that lives on disk. */
   void saveStep(int maxChunks) {
      if (!this.savePending) return;
      int scanned = 0;
      while (this.saveCursor != null && this.saveCursor.hasNext() && scanned++ < maxChunks) {
         Chunk c = this.saveCursor.next();
         if (c.modified) {
            this.saveChunk(c);
         }
      }
      if (this.saveCursor == null || !this.saveCursor.hasNext()) {
         this.saveCursor = null;
         this.saveTiles();
         RasterComputer.save(this);KineticMachines.save(this);
         RasterMedia.save(this);NoteBlocks.save(this);
         this.savePopulated();
         if (this.dim == Dim.OVERWORLD) {
            this.lodMap.save(this.dir);
         }
         this.savePending = false;
      }
   }

   public void saveAll() {
      this.startSave();
      this.saveStep(Integer.MAX_VALUE);
   }

   private void loadPopulated() {
      if (this.dir == null || this.dim != Dim.OVERWORLD) {
         return;
      }

      java.io.File f = new java.io.File(this.dir, "villages.dat");
      if (!f.exists()) {
         return;
      }

      try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(f))) {
         String line;
         while ((line = in.readLine()) != null) {
            if (!line.trim().isEmpty()) {
               this.populated.add(Long.parseLong(line.trim()));
            }
         }
      } catch (java.io.IOException | RuntimeException e) {
         e.printStackTrace();
      }
   }

   private void savePopulated() {
      if (this.dir == null || this.dim != Dim.OVERWORLD || this.populated.isEmpty()) {
         return;
      }

      try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(new java.io.File(this.dir, "villages.dat")))) {
         for (long k : this.populated) {
            out.println(k);
         }
      } catch (java.io.IOException e) {
         e.printStackTrace();
      }
   }

   public void shutdown() {
      RasterAudio.stopWorld(this);
      this.lodMap.stop = true;
      this.pool.shutdownNow();
      this.lightingPool.shutdownNow();
      // Let queued chunk files reach the disk before the world disappears; the queue is bounded by the save budget.
      this.savePool.shutdown();
      try {
         this.savePool.awaitTermination(5L, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   /**
    * The classic coordinate key. This format is PERSISTED in raster.dat,
    * circuit.dat and other save files, so it must never change: old worlds
    * (including the distributed V6.10 sound machines) load their saved keys.
    */
   static long pack(int var0, int var1, int var2) {
      return (long)(var0 + 33554432) << 34 | (long)(var2 + 33554432) << 8 | (var1 & 255L);
   }

   static int ux(long var0) {
      return (int)(var0 >>> 34 & 67108863L) - 33554432;
   }

   static int uz(long var0) {
      return (int)(var0 >>> 8 & 67108863L) - 33554432;
   }

   static int uy(long var0) {
      return (int)(var0 & 255L);
   }

   /**
    * V6.35: the internal light / fluid / block-update queues need 9 bits for Y
    * (the world is 384 tall). These keys are ephemeral and never saved, so a
    * separate packing keeps every old save file byte-for-byte compatible.
    */
   static final int QUEUE_OFF = 16777216;
   static long packH(int x, int y, int z) {
      return (long)(x + QUEUE_OFF) << 34 | (long)(z + QUEUE_OFF) << 9 | (y & 511L);
   }

   static int uxH(long v) {
      return (int)(v >>> 34 & 33554431L) - QUEUE_OFF;
   }

   static int uzH(long v) {
      return (int)(v >>> 9 & 33554431L) - QUEUE_OFF;
   }

   static int uyH(long v) {
      return (int)(v & 511L);
   }

   private void resetBounds() {
      LightScratch work = this.scratch();
      work.bx0 = work.by0 = work.bz0 = Integer.MAX_VALUE;
      work.bx1 = work.by1 = work.bz1 = Integer.MIN_VALUE;
   }

   private void touch(int x, int y, int z) {
      LightScratch work = this.scratch();
      if (x < work.bx0) work.bx0 = x;
      if (x > work.bx1) work.bx1 = x;
      if (y < work.by0) work.by0 = y;
      if (y > work.by1) work.by1 = y;
      if (z < work.bz0) work.bz0 = z;
      if (z > work.bz1) work.bz1 = z;
   }

   private void markBounds() {
      LightScratch work = this.scratch();
      if (work.bx0 != Integer.MAX_VALUE) {
         for (int cx = (work.bx0 - 1) >> 4; cx <= (work.bx1 + 1) >> 4; cx++) {
            for (int cz = (work.bz0 - 1) >> 4; cz <= (work.bz1 + 1) >> 4; cz++) {
               Chunk chunk = this.chunk(cx, cz);
               if (chunk != null) {
                  int minSection = Math.max(0, (work.by0 - 1) >> 4);
                  int maxSection = Math.min(Chunk.SECTIONS - 1, (work.by1 + 1) >> 4);
                  for (int sy = minSection; sy <= maxSection; sy++) {
                     chunk.sections[sy].dirty = true;
                  }
               }
            }
         }
      }
   }

   private int lightOf(Chunk var1, boolean var2, int var3, int var4, int var5) {
      return var2 ? var1.sky(var3, var4, var5) : var1.blk(var3, var4, var5);
   }

   private void propagate(boolean var1) {
      LightScratch work = this.scratch();
      int minLightY = (this.settings != null && this.settings.isVoidMode()) ? 36 : 0;
      Chunk lastC = null;
      int lastCx = Integer.MIN_VALUE;
      int lastCz = Integer.MIN_VALUE;
      while (!work.addQ.isEmpty()) {
         long var2 = work.addQ.poll();
         int var4 = uxH(var2);
         int var5 = uyH(var2);
         int var6 = uzH(var2);
         int cx = var4 >> 4;
         int cz = var6 >> 4;
         if (cx != lastCx || cz != lastCz) {
            lastC = this.chunk(cx, cz);
            lastCx = cx;
            lastCz = cz;
         }
         Chunk var7 = lastC;
         if (var7 != null) {
            int var8 = this.lightOf(var7, var1, var4 & 15, var5, var6 & 15);
            if (var8 > 1) {
               for (int var9 = 0; var9 < 6; var9++) {
                  int var10 = var4 + DX[var9];
                  int var11 = var5 + DY[var9];
                  int var12 = var6 + DZ[var9];
                  if (var11 >= minLightY && var11 < Chunk.H) {
                     int ncx = var10 >> 4;
                     int ncz = var12 >> 4;
                     Chunk var13;
                     if (ncx == cx && ncz == cz) {
                        var13 = var7;
                     } else if (ncx == lastCx && ncz == lastCz) {
                        var13 = lastC;
                     } else {
                        var13 = this.chunk(ncx, ncz);
                        lastC = var13;
                        lastCx = ncx;
                        lastCz = ncz;
                     }
                     if (var13 != null) {
                        int var14 = var10 & 15;
                        int var15 = var12 & 15;
                        int var16 = Blocks.opacity[var13.blocks[Chunk.idx(var14, var11, var15)] & BlockState.ID_MASK];
                        if (var16 < 15) {
                           int var17 = var8 - Math.max(1, var16);
                           if (var1 && var9 == 3 && var8 == 15 && var16 == 0) {
                              var17 = 15;
                           }

                           if (var17 > this.lightOf(var13, var1, var14, var11, var15)) {
                              if (var1) {
                                 var13.setSky(var14, var11, var15, var17);
                              } else {
                                 var13.setBlk(var14, var11, var15, var17);
                              }

                              this.touch(var10, var11, var12);
                              work.addQ.add(packH(var10, var11, var12));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void removeLight(boolean var1, int var2, int var3, int var4, int var5) {
      LightScratch work = this.scratch();
      int minLightY = (this.settings != null && this.settings.isVoidMode()) ? 36 : 0;
      Chunk var6 = this.chunkAt(var2, var4);
      if (var1) {
         var6.setSky(var2 & 15, var3, var4 & 15, 0);
      } else {
         var6.setBlk(var2 & 15, var3, var4 & 15, 0);
      }

      Chunk lastC = var6;
      int lastCx = var6.cx;
      int lastCz = var6.cz;
      work.remQ.add(packH(var2, var3, var4) | (long)var5 << 60);

      while (!work.remQ.isEmpty()) {
         long var7 = work.remQ.poll();
         int var9 = (int)(var7 >>> 60);
         int var10 = uxH(var7 & 1152921504606846975L);
         int var11 = uyH(var7);
         int var12 = uzH(var7 & 1152921504606846975L);

         for (int var13 = 0; var13 < 6; var13++) {
            int var14 = var10 + DX[var13];
            int var15 = var11 + DY[var13];
            int var16 = var12 + DZ[var13];
            if (var15 >= minLightY && var15 < Chunk.H) {
               int ncx = var14 >> 4;
               int ncz = var16 >> 4;
               if (ncx != lastCx || ncz != lastCz) {
                  lastC = this.chunk(ncx, ncz);
                  lastCx = ncx;
                  lastCz = ncz;
               }
               Chunk var17 = lastC;
               if (var17 != null) {
                  int var18 = var14 & 15;
                  int var19 = var16 & 15;
                  int var20 = this.lightOf(var17, var1, var18, var15, var19);
                  if (var20 != 0) {
                     if (var20 < var9 || var1 && var13 == 3 && var9 == 15 && var20 == 15) {
                        if (var1) {
                           var17.setSky(var18, var15, var19, 0);
                        } else {
                           var17.setBlk(var18, var15, var19, 0);
                        }

                        this.touch(var14, var15, var16);
                        work.remQ.add(packH(var14, var15, var16) | (long)var20 << 60);
                     } else {
                        work.addQ.add(packH(var14, var15, var16));
                     }
                  }
               }
            }
         }
      }
   }

   public void lightChunk(Chunk chunk) {
      this.lightingLock.lock();
      try {
         this.lightChunkLocked(chunk);
      } finally {
         this.lightingLock.unlock();
      }
   }

   private void lightChunkLocked(Chunk var1) {
      var1.lighting = true;
      try {
         LightScratch work = this.scratch();
         this.resetBounds();
         int var2 = var1.cx * 16;
         int var3 = var1.cz * 16;
         Chunk[] var4 = new Chunk[]{
            this.chunk(var1.cx + 1, var1.cz), this.chunk(var1.cx - 1, var1.cz), this.chunk(var1.cx, var1.cz + 1), this.chunk(var1.cx, var1.cz - 1)
         };

         boolean isVoid = this.settings != null && this.settings.isVoidMode();
         if (isVoid && this.dim == Dim.OVERWORLD && var1.isChunkEmpty()) {
            Arrays.fill(var1.light, (byte)0xF0);
            this.pullNeighbors(var1, false, var4);
            this.propagate(false);
            var1.lit = true;
            for (Section var25 : var1.sections) {
               var25.dirty = true;
            }
            this.markBounds();
            return;
         }

         if (this.dim == Dim.OVERWORLD) {
            int voidMinY = isVoid ? 36 : 0;
            int maxHm = 0;
            for (int i = 0; i < 256; i++) {
               if (var1.hm[i] > maxHm) {
                  maxHm = var1.hm[i];
               }
            }
            int bulkStartY = Math.max(voidMinY, maxHm + 1);
            if (bulkStartY < Chunk.H) {
               Arrays.fill(var1.light, bulkStartY << 8, Chunk.SIZE, (byte)0xF0);
            }
            // Update 3.12: only the Overworld has a sky - the Nether and the End have no sky light at all
            for (int var5 = 0; var5 < 16; var5++) {
               for (int var6 = 0; var6 < 16; var6++) {
                  int var7 = var1.hm[var5 * 16 + var6];
                  int colTopY = Math.min(Chunk.H, bulkStartY);
                  for (int var8 = Math.max(voidMinY, var7 + 1); var8 < colTopY; var8++) {
                     var1.setSky(var6, var8, var5, 15);
                  }

                  int var21 = var7;
                  int hE = var6 < 15 ? var1.hm[var5 * 16 + var6 + 1] : (var4[0] == null ? var7 : var4[0].hm[var5 * 16]);
                  if (hE > var21) var21 = hE;
                  int hW = var6 > 0 ? var1.hm[var5 * 16 + var6 - 1] : (var4[1] == null ? var7 : var4[1].hm[var5 * 16 + 15]);
                  if (hW > var21) var21 = hW;
                  int hS = var5 < 15 ? var1.hm[(var5 + 1) * 16 + var6] : (var4[2] == null ? var7 : var4[2].hm[var6]);
                  if (hS > var21) var21 = hS;
                  int hN = var5 > 0 ? var1.hm[(var5 - 1) * 16 + var6] : (var4[3] == null ? var7 : var4[3].hm[15 * 16 + var6]);
                  if (hN > var21) var21 = hN;

                  int var26 = Math.min(Chunk.H - 1, Math.max(var7 + 1, var21));
                  int seedStartY = isVoid ? Math.max(36, var7 + 1) : (var7 + 1);

                  for (int var28 = seedStartY; var28 <= var26; var28++) {
                     work.addQ.add(packH(var2 + var6, var28, var3 + var5));
                  }
               }
            }

            this.pullNeighbors(var1, true, var4);
            this.propagate(true);
         }

         short[] var16 = var1.blocks;

         for (int sy = 0; sy < Chunk.SECTIONS; sy++) {
            if (var1.isSectionEmpty(sy)) {
               continue;
            }
            int baseIdx = sy << 12;
            int endIdx = baseIdx + 4096;
            for (int var17 = baseIdx; var17 < endIdx; var17++) {
               int var19 = Blocks.emit[var16[var17] & BlockState.ID_MASK];
               if (var19 > 0) {
                  var1.light[var17] = (byte)(var1.light[var17] & 240 | var19);
                  int var22 = var17 >> 8;
                  int var24 = var17 >> 4 & 15;
                  int var27 = var17 & 15;
                  work.addQ.add(packH(var2 + var27, var22, var3 + var24));
               }
            }
         }

         this.pullNeighbors(var1, false, var4);
         this.propagate(false);
         var1.lit = true;

         for (Section var25 : var1.sections) {
            var25.dirty = true;
         }

         this.markBounds();
      } finally {
         var1.lighting = false;
      }
   }

   private void pullNeighbors(Chunk var1, boolean var2, Chunk[] var3) {
      LightScratch work = this.scratch();
      boolean isVoid = this.settings != null && this.settings.isVoidMode();
      int yLo = isVoid ? 36 : 0;
      int yHi = isVoid ? 148 : Chunk.H;
      for (int var4 = 0; var4 < 4; var4++) {
         Chunk var5 = var3[var4];
         if (var5 != null) {
            for (int var7 = 0; var7 < 16; var7++) {
               int var8;
               int var9;
               switch (var4) {
                  case 0:
                     var8 = 0;
                     var9 = var7;
                     break;
                  case 1:
                     var8 = 15;
                     var9 = var7;
                     break;
                  case 2:
                     var8 = var7;
                     var9 = 0;
                     break;
                  default:
                     var8 = var7;
                     var9 = 15;
               }
               byte var11 = (byte)(var4 == 0 ? 15 : (var4 == 1 ? 0 : var8));
               byte var12 = (byte)(var4 == 2 ? 15 : (var4 == 3 ? 0 : var9));
               int colYHi = var2 ? Math.min(yHi, Math.max(yLo, var1.hm[var12 * 16 + var11] + 1)) : yHi;
               for (int var6 = yLo; var6 < colYHi; var6++) {
                  int var10 = this.lightOf(var5, var2, var8, var6, var9);
                  if (var10 > 1) {
                     if (Blocks.opacity[var1.blocks[Chunk.idx(var11, var6, var12)] & BlockState.ID_MASK] < 15 && this.lightOf(var1, var2, var11, var6, var12) < var10 - 1) {
                        work.addQ.add(packH(var5.cx * 16 + var8, var6, var5.cz * 16 + var9));
                     }
                  }
               }
            }
         }
      }
   }

   /** Raw 16-bit saved state: 12-bit block ID plus block-specific property bits. */
   public int getState(int x, int y, int z) {
      Chunk c = y < 0 || y >= Chunk.H ? null : chunkAt(x, z);
      return c == null ? 0 : c.blocks[Chunk.idx(x & 15, y, z & 15)] & 0xFFFF;
   }

   public boolean setBlock(int x, int y, int z, int id) {
      return setState(x, y, z, BlockState.transition(getState(x, y, z), id));
   }

   public boolean setState(int var1, int var2, int var3, int state) {
      this.lightingLock.lock();
      try {
         return this.setStateLocked(var1, var2, var3, state);
      } finally {
         this.lightingLock.unlock();
      }
   }

   private boolean setStateLocked(int var1, int var2, int var3, int state) {
      if (state < 0 || state > 65535) return false;
      LightScratch work = this.scratch();
      int var4 = state & BlockState.ID_MASK;
      if (var2 >= 0 && var2 < Chunk.H) {
         Chunk var5 = this.chunkAt(var1, var3);
         if (var5 == null) {
            return false;
         }

         int var6 = var1 & 15;
         int var7 = var3 & 15;
         int var8 = var5.get(var6, var2, var7);
         if (getState(var1, var2, var3) == state) {
            return false;
         }

         var5.set(var6, var2, var7, state);
         this.meshCoverageEpoch.incrementAndGet();
         Kinetics.changed(this,var1,var2,var3,var8,var4);RasterComputer.changed(this,var1,var2,var3,var8,var4);NoteBlocks.changed(this,var1,var2,var3,var8,var4);
         var5.modified = true;
         var5.lodDirty = this.tickCount + 1L;
         if (var4 == Blocks.FIRE) {
            this.fire.added(var1, var2, var3);
            Portals.tryLight(this, var1, var2, var3);
         } else if (var8 == Blocks.FIRE) {
            this.fire.removed(var1, var2, var3);
         }

         this.resetBounds();
         this.touch(var1, var2, var3);

         int oldOp = Blocks.opacity[var8];
         int newOp = Blocks.opacity[var4];
         int oldEm = Blocks.emit[var8];
         int newEm = Blocks.emit[var4];

         for (int var9 = 0; var9 < 2; var9++) {
            boolean var10 = var9 == 0;
            if (var10 ? oldOp == newOp : (oldOp == newOp && oldEm == newEm)) {
               continue;
            }

            int var11 = this.lightOf(var5, var10, var6, var2, var7);
            if (var11 > 0) {
               this.removeLight(var10, var1, var2, var3, var11);
            }

            int var12 = 0;
            if (!var10) {
               var12 = Blocks.emit[var4];
            } else if (this.dim == Dim.OVERWORLD && Blocks.opacity[var4] == 0 && (var2 == Chunk.H - 1 || this.getSky(var1, var2 + 1, var3) == 15)) {
               var12 = 15;
            }

            if (var12 > 0) {
               if (var10) {
                  var5.setSky(var6, var2, var7, var12);
               } else {
                  var5.setBlk(var6, var2, var7, var12);
               }

               work.addQ.add(packH(var1, var2, var3));
            }

            for (int var13 = 0; var13 < 6; var13++) {
               int var14 = var2 + DY[var13];
               if (var14 >= 0 && var14 < Chunk.H) {
                  work.addQ.add(packH(var1 + DX[var13], var14, var3 + DZ[var13]));
               }
            }

            this.propagate(var10);
         }

         this.markBounds();
         int var15 = var7 * 16 + var6;
         if (Blocks.opacity[var4] > 0 && var2 > var5.hm[var15]) {
            var5.hm[var15] = var2;
         } else if (var2 == var5.hm[var15] && Blocks.opacity[var4] == 0) {
            int var16 = var2;

            while (var16 > 0 && Blocks.opacity[var5.get(var6, var16, var7)] == 0) {
               var16--;
            }

            var5.hm[var15] = var16;
         }

         this.notifyAround(var1, var2, var3);
         Net319.onSetBlock(this, var1, var2, var3, state);
         return true;
      } else {
         return false;
      }
   }

   void scheduleFluid(int var1, int var2, int var3, int var4) {
      long var5 = packH(var1, var2, var3);
      if (this.fluidPending.add(var5)) {
         ((ArrayList)this.fluidDue.computeIfAbsent(this.tickCount + var4, var0 -> new ArrayList())).add(var5);
      }
   }

   void scheduleBlock(int var1, int var2, int var3) {
      long var4 = packH(var1, var2, var3);
      if (this.blockPending.add(var4)) {
         this.blockUpdates.add(var4);
      }
   }

   void notifyAround(int var1, int var2, int var3) {
      for (int var4 = -1; var4 < 6; var4++) {
         int var5 = var4 < 0 ? var1 : var1 + DX[var4];
         int var6 = var4 < 0 ? var2 : var2 + DY[var4];
         int var7 = var4 < 0 ? var3 : var3 + DZ[var4];
         if (var6 >= 0 && var6 < Chunk.H) {
            int var8 = this.getBlock(var5, var6, var7);
            if (WaterPhysics.contains(this, var5, var6, var7)) scheduleFluid(var5, var6, var7, 5);
            if (Blocks.isFluid(var8)) {
               this.scheduleFluid(var5, var6, var7, Blocks.isWater(var8) ? 5 : 30);
            } else if (Aquatic.liveCoral(var8) || var8 == 7
               || var8 == 8
               || Blocks313.needsUpdate(var8)
               || Shapes.handlesUpdate(var8)
               || Color312.isPowder(var8)
               || Blocks.kind[var8] == 1
               || Blocks.isLadder(var8)
               || Blocks.isBed(var8)
               || Blocks.isFarmland(var8)
               || Blocks.isCrop(var8)
               || var8 == 32
               || var8 == 28
               || Blocks.isDoor(var8)
               || Blocks.isCake(var8)
               || Blocks.isPortal(var8)
               || Blocks.isWallTorch(var8)) {
               this.scheduleBlock(var5, var6, var7);
            }
         }
      }
      Circuit.changed(this,var1,var2,var3);
      Redstone319.onBlockChanged(this, var1, var2, var3);
   }

   public void resetAllChunks() {
      this.lightingLock.lock();
      try {
         this.resetAllChunksLocked();
      } finally {
         this.lightingLock.unlock();
      }
   }

   private void resetAllChunksLocked() {
      this.unloadCursor = null;
      this.lodCursor = null;
      FastChunkCache.clear();
      this.redstone.clear();
      this.circuit.clear();this.kinetics.clear();
      RasterAudio.stopWorld(this);this.raster.clear();
      this.fluidDue.clear();
      this.fluidPending.clear();
      this.blockUpdates.clear();
      this.blockPending.clear();
      this.fallers.clear();
      this.items.clear();
      this.tiles.clear();
      this.ready.clear();
      this.requested.clear();
      this.fire.cells.clear();
      if (this.lodMap != null) {
         this.lodMap.sums.clear();
      }
      boolean isVoidReset = this.settings != null && this.settings.isVoidMode();
      java.util.HashSet<Long> coords = new java.util.HashSet<Long>();
      for (Chunk old : this.chunks.values()) {
         old.unloaded = true;
         if (!isVoidReset || (old.cx >= -6 && old.cx <= 5 && old.cz >= -6 && old.cz <= 5)) {
            coords.add(key(old.cx, old.cz));
         }
      }
      if (isVoidReset) {
         for (int cx = -6; cx <= 5; cx++) {
            for (int cz = -6; cz <= 5; cz++) {
               coords.add(key(cx, cz));
            }
         }
      }
      this.chunks.clear();
      this.meshCoverageEpoch.incrementAndGet();
      ArrayList<Chunk> freshList = new ArrayList<Chunk>(coords.size());
      for (long k : coords) {
         int cx = (int)(k >> 32);
         int cz = (int)k;
         Chunk fresh = this.gen.generate(cx, cz);
         fresh.recomputeSectionOccupancy();
         CaveCull.computeChunk(fresh);
         fresh.modified = false;
         fresh.unloaded = false;
         this.chunks.put(k, fresh);
         freshList.add(fresh);
      }
      for (Chunk ch : freshList) {
         this.fire.scanChunk(ch);
         this.lightChunk(ch);
         for (Section s : ch.sections) {
            s.dirty = true;
            s.inFlight = false;
         }
      }
      FastChunkCache.clear();
   }

   public void tick() {
      this.tickCount++;
      // Update 6.18: a timed save spread over frames instead of one huge save on quit/dimension change.
      Options tuning = this.game == null ? null : this.game.opt;
      int interval = tuning == null ? 1200 : tuning.autosaveTicks;
      if (this.saveEnabled && this.dir != null && interval > 0 && !this.savePending && this.tickCount >= this.nextAutoSave) {
         this.nextAutoSave = this.tickCount + interval;
         this.startSave();
      }
      if (this.savePending) {
         this.saveStep(6);
      }
      Kinetics.tick(this);
      Circuit.tick(this);
      RasterComputer.tick(this);
      RedstoneLogic.tick(this);
      if (!Perf.FROZEN) {
         this.dayTime++; // (-Dbc.frozen=1, a test hook: the sun stands still, so that two pictures of one scene are one picture)
      }

      if (this.dim == Dim.OVERWORLD) {
         this.lodRefresh();
      }

      boolean remoteClientOnly = Net319.isClientConnected() && !Net319.isHosting();
      this.inPassiveTick = true;
      try {
         if (!remoteClientOnly) {
            int var1 = 0;

            while (!this.fluidDue.isEmpty() && this.fluidDue.firstKey() <= this.tickCount && var1 < 20000) {
               for (long var4 : (ArrayList<Long>)this.fluidDue.pollFirstEntry().getValue()) {
                  this.fluidPending.remove(var4);
                  this.fluidUpdate(uxH(var4), uyH(var4), uzH(var4));
                  var1++;
               }
            }

            int var7 = 0;

            while (!this.blockUpdates.isEmpty() && var7++ < 600) {
               long var8 = (Long)this.blockUpdates.poll();
               this.blockPending.remove(var8);
               this.blockUpdate(uxH(var8), uyH(var8), uzH(var8));
            }
         } else {
            this.redstone.clear(); // authoritative host owns scheduled events
            this.fluidDue.clear();
            this.fluidPending.clear();
            this.blockUpdates.clear();
            this.blockPending.clear();
         }

         for (int var9 = 0; var9 < this.fallers.size(); var9++) {
            World.Falling var12 = (World.Falling)this.fallers.get(var9);
            if (var12 == null) {
               continue;
            }
            var12.px = var12.x;
            var12.py = var12.y;
            var12.pz = var12.z;
            if (var12.fuse > 0) {
               this.tickTnt(var12);
            } else {
               this.tickFalling(var12);
            }
         }

         this.fallers.removeIf(var0 -> var0 == null || var0.dead);
         boolean isMinigame = this.settings != null && this.settings.isMinigame();
         if (!remoteClientOnly && !isMinigame) {
            this.tickCrops();
            this.fire.tick();
         }
         this.trees.tick();
         if (this.dim == Dim.OVERWORLD) {
            // no sky, no weather, no earthquakes in the other dimensions
            if (!remoteClientOnly && !isMinigame) {
               this.disasters.tick();
            }
            this.weather.tick();
         }

         for (int itemIdx = 0; itemIdx < this.items.size(); itemIdx++) {
            World.Item var13 = (World.Item)this.items.get(itemIdx);
            if (var13 != null) {
               this.tickItem(var13);
            }
         }

         this.items.removeIf(var0 -> var0 == null || var0.dead);
         if (!this.tiles.isEmpty()) {
            this.tileSnapshot.clear();
            this.tileSnapshot.addAll(this.tiles.values());
            for (TileEntity var14 : this.tileSnapshot) {
               if (var14.type == 1 && this.isLoaded(var14.x, var14.z)) {
                  int var5 = this.getBlock(var14.x, var14.y, var14.z);
                  if (var5 != 67 && var5 != 68) {
                     this.tiles.remove(tkey(var14.x, var14.y, var14.z));
                  } else if (!remoteClientOnly) {
                     int prevCook = var14.cook;
                     int prevBurn = var14.burn;
                     var14.tickFurnace(this);
                     int var6 = var14.burn > 0 ? 68 : 67;
                     if (var5 != var6) {
                        this.setBlock(var14.x, var14.y, var14.z, var6);
                     }
                     if ((prevCook == 0 && var14.cook > 0) || (prevCook > 0 && var14.cook == 0) || (prevBurn > 0 && var14.burn == 0)) {
                        Net319.onTileChanged(this, var14);
                     }
                  }
               }
            }
         }
      } finally {
         this.inPassiveTick = false;
      }
   }

   public void primeTnt(int var1, int var2, int var3, int var4) {
      World.Falling var5 = new World.Falling();
      var5.x = var1 + 0.5;
      var5.y = var2;
      var5.z = var3 + 0.5;
      var5.id = 132;
      var5.fuse = var4;
      var5.vy = 0.2;
      this.fallers.add(var5);
      Net319.onPrimeTnt(this, var1, var2, var3, var4);
   }

   public void throwTntGrenade(double x, double y, double z, double vx, double vy, double vz, int fuse) {
      World.Falling f = new World.Falling();
      f.x = x;
      f.y = y;
      f.z = z;
      f.px = x;
      f.py = y;
      f.pz = z;
      f.id = 132;
      f.fuse = fuse;
      f.vy = vy;
      SkyGen.registerThrownTnt(f, vx, vz);
      this.fallers.add(f);
      Net319.onPrimeTnt(this, (int)Math.floor(x + vx * 8.0), (int)Math.floor(y), (int)Math.floor(z + vz * 8.0), fuse);
   }

   private void tickTnt(World.Falling var1) {
      SkyGen.tickThrownTntHorizontal(this, var1);
      var1.vy -= 0.04;
      var1.y = var1.y + var1.vy;
      int var2 = (int)Math.floor(var1.x);
      int var3 = (int)Math.floor(var1.z);
      if (!this.isLoaded(var2, var3)) {
         var1.dead = true;
      } else {
         int var4 = (int)Math.floor(var1.y);
         if (var1.vy < 0.0 && Blocks.solid[this.getBlock(var2, var4, var3)]) {
            var1.y = var4 + 1;
            var1.vy = 0.0;
         }

         var1.vy *= 0.98;
         if (--var1.fuse <= 0) {
            var1.dead = true;
            if (Net319.isClientConnected() && !Net319.isHosting()) {
               SkyGen.removeThrownTnt(var1);
               if (this.game != null) {
                  this.game.explosionParticles(var1.x, var1.y + 0.5, var1.z);
               }
            } else if (this.settings != null && this.settings.type == WorldSettings.CASTLE_SIEGE) {
               SkyGen.onFallingTntDetonate(this.game, this, var1);
               SkyGen.removeThrownTnt(var1);
            } else if (this.settings != null && (this.settings.type == WorldSettings.CS2 || this.settings.type == WorldSettings.MURDER)) {
               SkyGen.removeThrownTnt(var1);
               if (this.game != null) {
                  this.game.explosionParticles(var1.x, var1.y + 0.5, var1.z);
                  SkyGen.onTntGrenadeDetonate(this.game, this, var1.x, var1.y + 0.5, var1.z);
               }
            } else if (this.game != null) {
               SkyGen.removeThrownTnt(var1);
               this.game.mobs.blast(var1.x, var1.y + 0.5, var1.z, 4, null);
            } else {
               SkyGen.removeThrownTnt(var1);
            }
         }
      }
   }

   /**
    * Random "farm ticks" around the player (like Minecraft's random block ticks, but only within 32 blocks of you):
    * farmland gets wet/dry depending on nearby water, dry empty farmland slowly turns back into dirt, and crops
    * grow - about 1.7x faster on wet farmland than before, and slower on dry farmland.
    */
   private void tickCrops() {
      if (this.game != null && this.game.player != null) {
         int px = (int)Math.floor(this.game.player.x);
         int pz = (int)Math.floor(this.game.player.z);

         for (int i = 0; i < 40; i++) {
            int x = px + this.rnd.nextInt(65) - 32;
            int z = pz + this.rnd.nextInt(65) - 32;
            Chunk c = this.chunkAt(x, z);
            if (c == null) {
               continue;
            }

            int y = c.hm[(z & 15) * 16 + (x & 15)];
            int b = this.getBlock(x, y, z);
            if (this.getBlock(x, y + 1, z) == Blocks.SUGAR_CANE) {
               if (!this.canCaneStay(x, y + 1, z)) {
                  this.scheduleBlock(x, y + 1, z); // e.g. the water next to it was removed diagonally: it pops off
               } else if (this.rnd.nextInt(16) == 0) {
                  this.growCane(x, y + 1, z);
               }

               continue;
            }

            if (!Blocks.isFarmland(b)) {
               continue;
            }

            boolean wet = this.hydrated(x, y, z);
            int above = this.getBlock(x, y + 1, z);
            boolean stem = Blocks.isStem(above);
            boolean crop = Blocks.isCrop(above) || stem;
            if (wet && b == Blocks.FARMLAND) {
               this.setBlock(x, y, z, Blocks.FARMLAND_WET);
            } else if (!wet && b == Blocks.FARMLAND_WET) {
               this.setBlock(x, y, z, Blocks.FARMLAND);
            } else if (!wet && !crop && this.rnd.nextInt(40) == 0) {
               this.setBlock(x, y, z, Blocks.DIRT);
               continue;
            }

            if (stem) {
               if (Blocks.stemStage(above) < 7) {
                  if (this.rnd.nextInt(wet ? 5 : 12) == 0) {
                     this.setBlock(x, y + 1, z, above + 1);
                  }
               } else if (this.rnd.nextInt(wet ? 12 : 24) == 0) {
                  this.tryFruit(x, y + 1, z, above);
               }
            } else if (crop && !Blocks.isRipe(above) && this.rnd.nextInt(wet ? 5 : 12) == 0) {
               this.setBlock(x, y + 1, z, above + 1);
            }
         }
      }
   }

   /**
    * Sugar cane can stand on another cane, or on grass / dirt / sand that has water right next to it (on the same level
    * as that ground block) - exactly like Minecraft.
    */
   public boolean canCaneStay(int x, int y, int z) {
      int below = this.getBlock(x, y - 1, z);
      if (below == Blocks.SUGAR_CANE) {
         return true;
      } else if (below != 2 && below != 3 && below != 7) {
         return false;
      } else {
         return Blocks.isWater(this.getBlock(x + 1, y - 1, z))
            || Blocks.isWater(this.getBlock(x - 1, y - 1, z))
            || Blocks.isWater(this.getBlock(x, y - 1, z + 1))
            || Blocks.isWater(this.getBlock(x, y - 1, z - 1));
      }
   }

   /** One random growth step of the sugar cane stack whose lowest block is at (x, y, z): +1 block on top, max 3 high. */
   void growCane(int x, int y, int z) {
      int top = y;
      while (this.getBlock(x, top + 1, z) == Blocks.SUGAR_CANE) {
         top++;
      }

      if (top - y < 2 && this.getBlock(x, top + 1, z) == 0) {
         this.setBlock(x, top + 1, z, Blocks.SUGAR_CANE);
      }
   }

   /**
    * A fully grown pumpkin / melon stem tries to put a fruit on a random side - only when it has no fruit next to it
    * yet and the spot is free air above farmland, dirt or grass (Minecraft 1.12 rules).
    */
   boolean tryFruit(int x, int y, int z, int stem) {
      int fruit = Blocks.stemFruit(stem);
      for (int i = 0; i < 6; i++) {
         if (DY[i] == 0 && this.getBlock(x + DX[i], y, z + DZ[i]) == fruit) {
            return false;
         }
      }

      int d = this.rnd.nextInt(4);
      int[] hx = new int[]{1, -1, 0, 0};
      int[] hz = new int[]{0, 0, 1, -1};
      int fx = x + hx[d];
      int fz = z + hz[d];
      int soil = this.getBlock(fx, y - 1, fz);
      if (this.getBlock(fx, y, fz) == 0 && (Blocks.isFarmland(soil) || soil == 2 || soil == 3)) {
         return this.setBlock(fx, y, fz, fruit);
      } else {
         return false;
      }
   }

   /** True when water is within 4 blocks (horizontally) of the farmland block at (x, y, z), on its level or one above. */
   public boolean hydrated(int x, int y, int z) {
      for (int dy = 0; dy <= 1; dy++) {
         for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
               if (Blocks.isWater(this.getBlock(x + dx, y + dy, z + dz))) {
                  return true;
               }
            }
         }
      }

      return this.weather.rainingAt(x, y + 1, z); // Update 3.8: rain waters the fields
   }

   /**
    * Something landed on block (x, y, z) after falling `fall` blocks. Farmland is trampled into dirt with a
    * probability of (fall - 0.5), exactly like Minecraft: a small hop is usually harmless, a real jump rarely is.
    * (Crops on it pop off through the normal block update.)
    */
   public void trample(int x, int y, int z, float fall) {
      if (Blocks.isFarmland(this.getBlock(x, y, z)) && this.rnd.nextFloat() < fall - 0.5F) {
         this.setBlock(x, y, z, Blocks.DIRT);
      }
   }

   private void blockUpdate(int var1, int var2, int var3) {
      int var4 = this.getBlock(var1, var2, var3);
      if (Aquatic.plant(var4) || Aquatic.liveCoral(var4)) { Aquatic.update(this,var1,var2,var3,var4); return; }
      if (Shapes.handlesUpdate(var4)) {
         Shapes.update(this, var1, var2, var3, var4);
         return;
      }

      if (Blocks313.isDouble(var4)) {
         // Update 3.13: tall plants are two blocks: the upper half needs the lower one, the lower one needs ground and the upper half
         if (Blocks313.isDoubleUpper(var4)) {
            if (this.getBlock(var1, var2 - 1, var3) != var4 - 1) {
               this.setBlock(var1, var2, var3, 0);
            }
         } else {
            int ground = this.getBlock(var1, var2 - 1, var3);
            if (!Blocks.solid[ground] || ground == 28 || this.getBlock(var1, var2 + 1, var3) != var4 + 1) {
               int item = Blocks.drop[var4];
               this.setBlock(var1, var2, var3, 0);
               if (item != 0) {
                  this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, item, 1);
               }
            }
         }

         return;
      }

      if (var4 == Blocks313.LILY_PAD) {
         int below = this.getBlock(var1, var2 - 1, var3);
         if (!Blocks.isWater(below) || Blocks.fluidLevel(below) != 0) {
            this.setBlock(var1, var2, var3, 0);
            this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Blocks313.LILY_PAD, 1);
         }

         return;
      }

      if (var4 == Blocks313.SNOW_LAYER) {
         int below = this.getBlock(var1, var2 - 1, var3);
         if (!Blocks.solid[below] || Blocks.kind[below] != 0 || below == Blocks313.ICE) {
            this.setBlock(var1, var2, var3, 0);
         }

         return;
      }

      if (Blocks313.isVine(var4)) {
         int mask = var4 - Blocks313.VINE0;
         int keep = 0;
         int[] dxs = new int[]{0, 1, 0, -1};
         int[] dzs = new int[]{-1, 0, 1, 0};
         for (int side = 0; side < 4; side++) {
            int nb = this.getBlock(var1 + dxs[side], var2, var3 + dzs[side]);
            if (Blocks.opaque[nb] && Blocks.solid[nb]) {
               keep |= 1 << side;
            }
         }

         int above = this.getBlock(var1, var2 + 1, var3);
         if (Blocks313.isVine(above)) {
            keep |= above - Blocks313.VINE0;
         }

         keep &= mask;
         if (keep == 0) {
            this.setBlock(var1, var2, var3, 0);
         } else if (keep != mask) {
            this.setBlock(var1, var2, var3, Blocks313.VINE0 + keep);
         }

         return;
      }

      if (Blocks.isLadder(var4)) {
         int var18 = var4 - 119;
         int var23 = var1 + (var18 == 2 ? 1 : (var18 == 3 ? -1 : 0));
         int var25 = var3 + (var18 == 0 ? -1 : (var18 == 1 ? 1 : 0));
         if (!Blocks.solid[this.getBlock(var23, var2, var25)]) {
            this.setBlock(var1, var2, var3, 0);
            this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, 119, 1);
         }
      } else if (Blocks.isBed(var4)) {
         boolean var17 = Blocks.isBedHead(var4);
         int var22 = Decor315.bedFacing(var4);
         int var24 = var22 == 2 ? 1 : (var22 == 3 ? -1 : 0);
         int var8 = var22 == 0 ? -1 : (var22 == 1 ? 1 : 0);
         int var9 = var17 ? var1 - var24 : var1 + var24;
         int var10 = var17 ? var3 - var8 : var3 + var8;
         int var11 = var17 ? Decor315.foot(Decor315.bedColor(var4), var22) : Decor315.head(Decor315.bedColor(var4), var22);
         if (this.getBlock(var9, var2, var10) != var11) {
            this.setBlock(var1, var2, var3, 0);
            if (!var17 && (this.settings == null || this.settings.type != WorldSettings.BEDWARS)) {
               this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Decor315.bedItem(Decor315.bedColor(var4)), 1);
            }
         }
      } else if (Blocks.isFarmland(var4)) {
         int var16 = this.getBlock(var1, var2 + 1, var3);
         if (Blocks.opaque[var16] || Blocks.isStair(var16) || Blocks.isSlab(var16)) {
            this.setBlock(var1, var2, var3, 3);
         }
      } else if (Blocks.isCrop(var4)) {
         if (!Blocks.isFarmland(this.getBlock(var1, var2 - 1, var3))) {
            this.setBlock(var1, var2, var3, 0);
            this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Blocks.cropSeedItem(var4), 1);
         }
      } else if (var4 == Blocks.SUGAR_CANE) {
         if (!this.canCaneStay(var1, var2, var3)) {
            this.setBlock(var1, var2, var3, 0);
            this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Blocks.SUGAR_CANE, 1);
         }
      } else if (Blocks.isStem(var4)) {
         if (!Blocks.isFarmland(this.getBlock(var1, var2 - 1, var3))) {
            this.setBlock(var1, var2, var3, 0);
            this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, Blocks.stemSeedItem(var4), 1);
         }
      } else if (Blocks.isCake(var4)) {
         if (!Blocks.solid[this.getBlock(var1, var2 - 1, var3)]) {
            this.setBlock(var1, var2, var3, 0);
         }
      } else if (Blocks.isFire(var4)) {
         this.fire.check(var1, var2, var3);
      } else if (Blocks.isPortal(var4)) {
         if (!Portals.intact(this, var1, var2, var3)) {
            this.setBlock(var1, var2, var3, 0);
         }
      } else {
         if (var4 != 7 && var4 != 8 && var4 != Blocks313.RED_SAND && !Color312.isPowder(var4)) {
            if (Blocks.kind[var4] == 1 || var4 == 32) {
               int var15 = this.getBlock(var1, var2 - 1, var3);
               if (!Blocks.solid[var15] || var15 == 28) {
                  int var21 = Blocks.drop[var4];
                  this.setBlock(var1, var2, var3, 0);
                  if (var21 != 0) {
                     this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, var21, 1);
                  }
               }
            } else if (Blocks.isWallTorch(var4)) {
               int var12 = var4 - 89;
               int var19 = var1 + (var12 == 2 ? 1 : (var12 == 3 ? -1 : 0));
               int var7 = var3 + (var12 == 0 ? -1 : (var12 == 1 ? 1 : 0));
               if (!Blocks.solid[this.getBlock(var19, var2, var7)]) {
                  this.setBlock(var1, var2, var3, 0);
                  this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, 32, 1);
               }
            } else if (Blocks.isDoor(var4)) {
               if (Blocks.isDoorLo(var4)) {
                  int var13 = this.getBlock(var1, var2 + 1, var3);
                  int var20 = this.getBlock(var1, var2 - 1, var3);
                  if (!Blocks.solid[var20] || !Blocks.isDoorHi(var13)) {
                     this.setBlock(var1, var2, var3, 0);
                     if (Blocks.isDoorHi(var13)) {
                        this.setBlock(var1, var2 + 1, var3, 0);
                     }

                     this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, 281, 1);
                  }
               } else if (!Blocks.isDoorLo(this.getBlock(var1, var2 - 1, var3))) {
                  this.setBlock(var1, var2, var3, 0);
               }
            } else if (var4 == 28) {
               int var14 = this.getBlock(var1, var2 - 1, var3);
               if (var14 != 7 && var14 != 28) {
                  this.setBlock(var1, var2, var3, 0);
                  this.dropItem(var1 + 0.5, var2 + 0.3, var3 + 0.5, 28, 1);
               }
            }
         } else {
            if (var2 <= 0) {
               return;
            }

            if (Color312.isPowder(var4) && Color312.wet(this, var1, var2, var3)) {
               this.setBlock(var1, var2, var3, Color312.hardened(var4)); // Update 3.12: concrete powder sets next to water
               if (this.game != null) {
                  this.game.adv.award(this.game, "story/concrete");
               }

               return;
            }

            int var5 = this.getBlock(var1, var2 - 1, var3);
            if (var5 == 0 || Blocks.isFluid(var5) || Blocks.replaceable[var5] || var5 == 32) {
               this.setBlock(var1, var2, var3, 0);
               World.Falling var6 = new World.Falling();
               var6.x = var1 + 0.5;
               var6.y = var2;
               var6.z = var3 + 0.5;
               var6.id = var4;
               this.fallers.add(var6);
            }
         }
      }
   }

   private void tickFalling(World.Falling var1) {
      double var2 = var1.vy - 0.04;
      var1.vy = var2 * 0.98;
      double var4 = var1.vy;
      int var6 = Math.max(1, (int)Math.ceil(Math.abs(var4) / 0.5));

      for (int var7 = 0; var7 < var6; var7++) {
         var1.y += var4 / var6;
         int var8 = (int)Math.floor(var1.x);
         int var9 = (int)Math.floor(var1.z);
         if (!this.isLoaded(var8, var9)) {
            var1.dead = true;
            return;
         }

         int var10 = this.getBlock(var8, (int)Math.floor(var1.y - 0.001), var9);
         if (var1.y <= 0.0 || Blocks.solid[var10] && var10 != 0) {
            int var11 = (int)Math.floor(var1.y - 0.001) + 1;
            if (var1.y <= 0.0) {
               var11 = 0;
            }

            int var12 = this.getBlock(var8, var11, var9);
            if (!Blocks.replaceable[var12] && var12 != 0) {
               this.dropItem(var1.x, var11 + 0.5, var1.z, var1.id, 1);
            } else {
               this.setBlock(var8, var11, var9, var1.id);
            }

            var1.dead = true;
            return;
         }
      }

      if (var1.y < -10.0) {
         var1.dead = true;
      }
   }

   public void dropItem(double var1, double var3, double var5, int var7, int var8) {
      this.dropItem(var1, var3, var5, var7, var8, 0);
   }

   public World.Item dropItem(double var1, double var3, double var5, int var7, int var8, int var9) {
      if (var7 != 0 && var8 > 0) {
         World.Item var10 = new World.Item();
         var10.x = var1;
         var10.y = var3;
         var10.z = var5;
         var10.vx = (this.rnd.nextDouble() - 0.5) * 0.2;
         var10.vy = 0.2;
         var10.vz = (this.rnd.nextDouble() - 0.5) * 0.2;
         var10.id = var7;
         var10.count = var8;
         var10.dmg = var9;
         var10.spin = this.rnd.nextFloat() * 360.0F;
         var10.netId = Net319.nextItemId();
         this.items.add(var10);
         Net319.onDropItem(this, var10);
         return var10;
      } else {
         return null;
      }
   }

   static long tkey(int var0, int var1, int var2) {
      return (long)(var0 & 67108863) << 38 | (long)(var2 & 67108863) << 12 | var1 & 4095;
   }

   public TileEntity tileAt(int var1, int var2, int var3) {
      return (TileEntity)this.tiles.get(tkey(var1, var2, var3));
   }

   public TileEntity getOrCreateTile(int var1, int var2, int var3) {
      int var4 = this.getBlock(var1, var2, var3);
      TileEntity var5 = (TileEntity)this.tiles.get(tkey(var1, var2, var3));
      if (var4 == 70) {
         this.setBlock(var1, var2, var3, 69);
         var5 = new TileEntity(0, var1, var2, var3);
         this.fillLoot(var5);
         Net319.applyPendingTile(this, var5);
         this.tiles.put(tkey(var1, var2, var3), var5);
         Net319.onTileChanged(this, var5);
         return var5;
      }

      if (var5 == null) {
         if (var4 == 69) {
            var5 = new TileEntity(0, var1, var2, var3);
         } else {
            if (var4 != 67 && var4 != 68) {
               return null;
            }

            var5 = new TileEntity(1, var1, var2, var3);
         }

         Net319.applyPendingTile(this, var5);
         this.tiles.put(tkey(var1, var2, var3), var5);
      }

      return var5;
   }

   public void removeTile(int var1, int var2, int var3, boolean var4) {
      TileEntity var5 = (TileEntity)this.tiles.remove(tkey(var1, var2, var3));
      if (var5 != null && var4) {
         for (Stack var9 : var5.slots) {
            if (var9 != null) {
               this.dropItem(var1 + 0.5, var2 + 0.5, var3 + 0.5, var9.id, var9.n, var9.dmg);
            }
         }
      }
   }

   void fillLoot(TileEntity var1) {
      if (this.settings != null && this.settings.isMinigame()) {
         SkyGen.fillRandomMinigameChest(this, var1);
         return;
      }
      Random var2 = new Random(this.seed * 31L + tkey(var1.x, var1.y, var1.z));
      int var3 = 0;
      int[][] table = this.dim == Dim.NETHER ? Items312.NETHER_LOOT : LOOT;

      for (int[] var7 : table) {
         var3 += var7[3];
      }

      int var14 = 4 + var2.nextInt(5);

      for (int var15 = 0; var15 < var14; var15++) {
         int var16 = var2.nextInt(var3);

         for (int[] var10 : table) {
            var16 -= var10[3];
            if (var16 < 0) {
               int var11 = var10[1] + var2.nextInt(var10[2] - var10[1] + 1);
               int var12 = Items.maxDamage(var10[0]) > 0 ? var2.nextInt(Items.maxDamage(var10[0]) / 3 + 1) : 0;
               int var13 = var2.nextInt(27);
               if (var1.slots[var13] == null) {
                  var1.slots[var13] = new Stack(var10[0], var11, var12);
               }
               break;
            }
         }
      }
   }

   void saveTiles() {
      if (this.dir != null) {
         try {
            DataOutputStream var1 = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(this.dir, "tiles.dat"))));

            try {
               var1.writeInt(this.tiles.size());

               for (TileEntity var3 : this.tiles.values()) {
                  var1.writeInt(var3.type);
                  var1.writeInt(var3.x);
                  var1.writeInt(var3.y);
                  var1.writeInt(var3.z);
                  var1.writeInt(var3.burn);
                  var1.writeInt(var3.burnMax);
                  var1.writeInt(var3.cook);

                  for (Stack var7 : var3.slots) {
                     if (var7 == null) {
                        var1.writeInt(-1);
                     } else {
                        var1.writeInt(var7.id);
                        var1.writeInt(var7.n);
                        var1.writeInt(var7.dmg);
                     }
                  }
               }
            } catch (Throwable var9) {
               try {
                  var1.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var1.close();
         } catch (IOException var10) {
            System.err.println("Failed to save tiles: " + var10);
         }
      }
   }

   void loadTiles() {
      if (this.dir != null) {
         File var1 = new File(this.dir, "tiles.dat");
         if (var1.exists()) {
            try {
               DataInputStream var2 = new DataInputStream(new BufferedInputStream(new FileInputStream(var1)));

               try {
                  int var3 = var2.readInt();

                  for (int var4 = 0; var4 < var3; var4++) {
                     TileEntity var5 = new TileEntity(var2.readInt(), var2.readInt(), var2.readInt(), var2.readInt());
                     var5.burn = var2.readInt();
                     var5.burnMax = var2.readInt();
                     var5.cook = var2.readInt();

                     for (int var6 = 0; var6 < var5.slots.length; var6++) {
                        int var7 = var2.readInt();
                        if (var7 >= 0) {
                           int var8 = var2.readInt();
                           int var9 = var2.readInt();
                           var5.slots[var6] = new Stack(var7, var8, var9);
                        }
                     }

                     this.tiles.put(tkey(var5.x, var5.y, var5.z), var5);
                  }
               } catch (Throwable var11) {
                  try {
                     var2.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               var2.close();
            } catch (IOException var12) {
               System.err.println("Failed to load tiles: " + var12);
            }
         }
      }
   }

   private void tickItem(World.Item var1) {
      if(KineticTransport.capture(this,var1))return;
      if (var1.eye) {
         EndPortals.tickEye(this, var1);
         return;
      }

      var1.age++;
      if (var1.delay > 0) {
         var1.delay--;
      }

      if (var1.age > 6000) {
         var1.dead = true;
      } else {
         int var2 = (int)Math.floor(var1.x);
         int var3 = (int)Math.floor(var1.z);
         if (this.isLoaded(var2, var3)) {
            int var4 = this.getBlock(var2, (int)Math.floor(var1.y + 0.1), var3);
            if (Blocks.isLava(var4) || Blocks.isFire(var4)) {
               var1.dead = true; // items burn up in lava and in fire
            } else {
               if (Blocks.isWater(var4)) {
                  var1.vy = Math.min(var1.vy + 0.02, 0.1);
                  var1.vx *= 0.9;
                  var1.vz *= 0.9;
               } else {
                  var1.vy -= 0.04;
               }

               double[] var5 = new double[]{var1.x - 0.125, var1.y, var1.z - 0.125, var1.x + 0.125, var1.y + 0.25, var1.z + 0.125};
               if (Phys.collides(this, var5)) {
                  var1.y += 0.1;
               } else {
                  Phys.move(this, var5, var1.vx, var1.vy, var1.vz, this.res);
                  boolean var6 = var1.vy < 0.0 && this.res[1] != var1.vy;
                  var1.x = (var5[0] + var5[3]) / 2.0;
                  var1.y = var5[1];
                  var1.z = (var5[2] + var5[5]) / 2.0;
                  if (this.res[0] != var1.vx) {
                     var1.vx = 0.0;
                  }

                  if (this.res[2] != var1.vz) {
                     var1.vz = 0.0;
                  }

                  float var7 = 0.98F;
                  if (var6) {
                     var7 = 0.588F;
                  }

                  var1.vx *= var7;
                  var1.vz *= var7;
                  var1.vy *= 0.98;
                  if (this.res[1] != var1.vy / 0.98 && Math.abs(this.res[1]) < Math.abs(var1.vy) && var6) {
                     var1.vy = 0.0;
                  }

                  if (var1.y < -20.0) {
                     var1.dead = true;
                  }

                  var1.spin += 3.0F;
               }
            }
         }
      }
   }

   private boolean touchesWater(int var1, int var2, int var3) {
      for (int var4 = 0; var4 < 6; var4++) {
         if (var4 != 3 && Blocks.isWater(this.getBlock(var1 + DX[var4], var2 + DY[var4], var3 + DZ[var4]))) {
            return true;
         }
      }

      return false;
   }

   private boolean canFlowInto(int var1, boolean var2, int var3) {
      if (var1 == 0) {
         return true;
      } else if (Blocks.isFluid(var1)) {
         return Blocks.isWater(var1) != var2 ? true : Blocks.fluidLevel(var1) > var3 && Blocks.fluidLevel(var1) != 0;
      } else {
         return Blocks.replaceable[var1];
      }
   }

   int cobbleOrSkyblockOre() {
      if (this.settings != null && this.settings.type == WorldSettings.SKYBLOCK) {
         int r = this.rnd.nextInt(100);
         if (r < 12) return Blocks.COAL_ORE;
         if (r < 19) return Blocks.IRON_ORE;
         if (r < 22) return Blocks.GOLD_ORE;
         if (r < 24) return Blocks.REDSTONE_ORE;
         if (r < 25) return Blocks.LAPIS_ORE;
         if (r < 26) return Blocks.DIAMOND_ORE;
      }
      return 4;
   }

   private void fluidUpdate(int var1, int var2, int var3) {
      if (!Boolean.getBoolean("bc.legacyfluids") && WaterPhysics.contains(this, var1, var2, var3)) {
         WaterPhysics.update(this, var1, var2, var3); return;
      }
      int var4 = this.getBlock(var1, var2, var3);
      if (Blocks.isFluid(var4)) {
         boolean var5 = Blocks.isWater(var4);
         int var6 = Blocks.fluidLevel(var4);
         int var7 = var5 ? 50 : 58;
         int var8 = var5 ? 7 : 3;
         int var9 = var5 ? 5 : 30;
         if (!var5 && this.touchesWater(var1, var2, var3)) {
            this.setBlock(var1, var2, var3, var6 == 0 ? 26 : this.cobbleOrSkyblockOre());
         } else {
            int var10 = this.getBlock(var1, var2 + 1, var3);
            if (var6 > 0) {
               int var11 = 99;
               if (Blocks.sameFluid(var10, var4)) {
                  var11 = 1;
               } else {
                  int var12 = 0;

                  for (int var13 = 0; var13 < 6; var13++) {
                     if (DY[var13] == 0) {
                        int var14 = this.getBlock(var1 + DX[var13], var2, var3 + DZ[var13]);
                        if (Blocks.sameFluid(var14, var4)) {
                           int var15 = Blocks.fluidLevel(var14);
                           if (var15 == 0) {
                              var12++;
                           }

                           var11 = Math.min(var11, var15 + 1);
                        }
                     }
                  }

                  if (var5 && var12 >= 2) {
                     int var19 = this.getBlock(var1, var2 - 1, var3);
                     if (Blocks.solid[var19] || Blocks.isWater(var19) && Blocks.fluidLevel(var19) == 0) {
                        this.setBlock(var1, var2, var3, 50);
                        return;
                     }
                  }
               }

               if (var11 > var8) {
                  this.setBlock(var1, var2, var3, 0);
                  return;
               }

               if (var11 != var6) {
                  this.setBlock(var1, var2, var3, var7 + var11);
                  return;
               }
            }

            int var17 = this.getBlock(var1, var2 - 1, var3);
            if (var2 > 0 && this.canFlowInto(var17, var5, 1)) {
               if (!var5 && Blocks.isWater(var17)) {
                  this.setBlock(var1, var2 - 1, var3, 1);
               } else if (var5 && Blocks.isLava(var17)) {
                  this.setBlock(var1, var2 - 1, var3, Blocks.isLava(var17) && Blocks.fluidLevel(var17) == 0 ? 26 : this.cobbleOrSkyblockOre());
               } else {
                  if (!Blocks.isFluid(var17) || Blocks.fluidLevel(var17) != 1) {
                     this.setBlock(var1, var2 - 1, var3, var7 + 1);
                  }
               }
            } else if (Blocks.isFluid(var17) && Blocks.sameFluid(var17, var4) && Blocks.fluidLevel(var17) != 0 && var2 > 0) {
               this.scheduleFluid(var1, var2 - 1, var3, var9);
            } else {
               int var18 = var6 + 1;
               if (var18 <= var8) {
                  for (int var20 = 0; var20 < 6; var20++) {
                     if (DY[var20] == 0) {
                        int var21 = var1 + DX[var20];
                        int var22 = var3 + DZ[var20];
                        int var16 = this.getBlock(var21, var2, var22);
                        if (this.canFlowInto(var16, var5, var18)) {
                           if (!var5 && Blocks.isWater(var16)) {
                              this.setBlock(var21, var2, var22, 1);
                           } else if (var5 && Blocks.isLava(var16)) {
                              this.setBlock(var21, var2, var22, Blocks.fluidLevel(var16) == 0 ? 26 : this.cobbleOrSkyblockOre());
                           } else if (!Blocks.isFluid(var16) || !Blocks.sameFluid(var16, var4) || Blocks.fluidLevel(var16) > var18) {
                              this.setBlock(var21, var2, var22, var7 + var18);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public double[] selectionBox(int var1) {
      if (Blocks.kind[var1] == 0) {
         return var1 == 28 ? Blocks.box[var1] : FULL;
      } else {
         return Blocks.box[var1] != null ? Blocks.box[var1] : FULL;
      }
   }

   public World.Hit raycast(double var1, double var3, double var5, double var7, double var9, double var11, double var13) {
      int var15 = (int)Math.floor(var1);
      int var16 = (int)Math.floor(var3);
      int var17 = (int)Math.floor(var5);
      int var18 = var7 > 0.0 ? 1 : -1;
      int var19 = var9 > 0.0 ? 1 : -1;
      int var20 = var11 > 0.0 ? 1 : -1;
      double var21 = var7 == 0.0 ? 1.0E30 : Math.abs(1.0 / var7);
      double var23 = var9 == 0.0 ? 1.0E30 : Math.abs(1.0 / var9);
      double var25 = var11 == 0.0 ? 1.0E30 : Math.abs(1.0 / var11);
      double var27 = var7 == 0.0 ? 1.0E30 : (var7 > 0.0 ? var15 + 1 - var1 : var1 - var15) * var21;
      double var29 = var9 == 0.0 ? 1.0E30 : (var9 > 0.0 ? var16 + 1 - var3 : var3 - var16) * var23;
      double var31 = var11 == 0.0 ? 1.0E30 : (var11 > 0.0 ? var17 + 1 - var5 : var5 - var17) * var25;

      for (int var33 = 0; var33 < 128; var33++) {
         int var34 = this.getBlock(var15, var16, var17);
         if (Blocks.targetable(var34) || this.rayFluids && Blocks.isFluid(var34)) {
            World.Hit var35 = Shapes.isDyn(var34)
               ? this.rayShape(var1, var3, var5, var7, var9, var11, var15, var16, var17, var34)
               : rayBox(var1, var3, var5, var7, var9, var11, var15, var16, var17, this.selectionBox(var34));
            if (var35 != null && var35.t <= var13) {
               return var35;
            }
         }

         double var37 = Math.min(var27, Math.min(var29, var31));
         if (var37 > var13) {
            return null;
         }

         if (var27 <= var29 && var27 <= var31) {
            var15 += var18;
            var27 += var21;
         } else if (var29 <= var31) {
            var16 += var19;
            var29 += var23;
         } else {
            var17 += var20;
            var31 += var25;
         }
      }

      return null;
   }

   /** Update 3.15: a ray against a block made of several boxes (panes, walls: they depend on what stands next to them); the nearest box wins. */
   private World.Hit rayShape(double ox, double oy, double oz, double dx, double dy, double dz, int x, int y, int z, int id) {
      World.Hit best = null;
      for (double[] box : Shapes.selectionBoxes(this, x, y, z, id)) {
         World.Hit h = rayBox(ox, oy, oz, dx, dy, dz, x, y, z, box);
         if (h != null && (best == null || h.t < best.t)) {
            best = h;
         }
      }

      return best;
   }

   static World.Hit rayBox(double var0, double var2, double var4, double var6, double var8, double var10, int var12, int var13, int var14, double[] var15) {
      double[] var16 = new double[]{var0 - var12, var2 - var13, var4 - var14};
      double[] var17 = new double[]{var6, var8, var10};
      double var18 = -1.0E30;
      double var20 = 1.0E30;
      int var22 = 1;
      int var23 = 1;

      for (int var24 = 0; var24 < 3; var24++) {
         double var25 = var15[var24];
         double var27 = var15[var24 + 3];
         if (Math.abs(var17[var24]) < 1.0E-12) {
            if (var16[var24] < var25 || var16[var24] > var27) {
               return null;
            }
         } else {
            double var29 = (var25 - var16[var24]) / var17[var24];
            double var31 = (var27 - var16[var24]) / var17[var24];
            int var33 = -1;
            if (var29 > var31) {
               double var34 = var29;
               var29 = var31;
               var31 = var34;
               var33 = 1;
            }

            if (var29 > var18) {
               var18 = var29;
               var22 = var24;
               var23 = var33;
            }

            if (var31 < var20) {
               var20 = var31;
            }

            if (var18 > var20) {
               return null;
            }
         }
      }

      if (var20 < 0.0) {
         return null;
      }

      World.Hit var36 = new World.Hit();
      var36.x = var12;
      var36.y = var13;
      var36.z = var14;
      var36.t = Math.max(var18, 0.0);
      var36.fx = Math.max(0.0, Math.min(1.0, var16[0] + var17[0] * var36.t));
      var36.fy = Math.max(0.0, Math.min(1.0, var16[1] + var17[1] * var36.t));
      var36.fz = Math.max(0.0, Math.min(1.0, var16[2] + var17[2] * var36.t));
      var36.nx = var36.ny = var36.nz = 0;
      if (var18 < 0.0) {
         var36.ny = 1;
         var36.face = 0;
         return var36;
      }

      if (var22 == 0) {
         var36.nx = var23;
      } else if (var22 == 1) {
         var36.ny = var23;
      } else {
         var36.nz = var23;
      }

      var36.face = var22 == 1 ? (var23 > 0 ? 0 : 1) : (var22 == 2 ? (var23 > 0 ? 3 : 2) : (var23 > 0 ? 4 : 5));
      return var36;
   }

   static {
      ArrayList<int[]> var0 = new ArrayList<>();

      for (int var1 = -32; var1 <= 32; var1++) {
         for (int var2 = -32; var2 <= 32; var2++) {
            var0.add(new int[]{var1, var2});
         }
      }

      var0.sort(Comparator.comparingInt(var0x -> var0x[0] * var0x[0] + var0x[1] * var0x[1]));
      ORDER = (int[][])var0.toArray(new int[0][]);
      DX = new int[]{1, -1, 0, 0, 0, 0};
      DY = new int[]{0, 0, 1, -1, 0, 0};
      DZ = new int[]{0, 0, 0, 0, 1, -1};
      LOOT = new int[][]{
         {258, 1, 3, 10},
         {259, 1, 2, 5},
         {260, 1, 2, 2},
         {257, 1, 4, 10},
         {270, 1, 3, 8},
         {256, 2, 6, 8},
         {263, 1, 4, 10},
         {264, 1, 4, 8},
         {265, 1, 4, 8},
         {32, 3, 10, 8},
         {5, 4, 12, 6},
         {261, 1, 2, 6},
         {267, 3, 8, 6},
         {273, 1, 3, 4},
         {275, 1, 3, 4},
         {276, 1, 2, 3},
         {Items.toolId(2, 4), 1, 1, 2},
         {Items.toolId(2, 1), 1, 1, 2},
         {Items.toolId(1, 2), 1, 1, 3},
         {Items.armorId(1, 0), 1, 1, 2},
         {Items.armorId(1, 3), 1, 1, 2},
         {Items.armorId(2, 1), 1, 1, 1},
         {268, 1, 1, 2},
         {341, 2, 4, 6},
         {342, 2, 4, 6},
         {4, 6, 14, 5}
      };
      FULL = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
   }

   public static final class Falling {
      public double x;
      public double y;
      public double z;
      public double vy;
      /** position at the start of the current tick (the renderer interpolates between it and x / y / z) */
      public double px = Double.NaN;
      public double py;
      public double pz;
      public int id;
      public boolean dead;
      public int fuse;
   }

   public static final class Hit {
      public int x;
      public int y;
      public int z;
      public int face;
      public double t;
      public int nx;
      public int ny;
      public int nz;
      /** where the ray hit, as a fraction of the block (0..1 in each direction) - decides top / bottom half when placing slabs and stairs */
      public double fx = 0.5;
      public double fy = 0.5;
      public double fz = 0.5;
   }

   public static final class Item {
      public double x;
      public double y;
      public double z;
      public double vx;
      public double vy;
      public double vz;
      public int id;
      public int count;
      public int age;
      public int delay = 10;
      public int dmg;
      public boolean dead;
      public float spin;
      public long netId;
      /** Update 3.12: an Eye of Ender in flight: it flies from (lx, ly, lz) to above (ax, az) and then drops like any other item. */
      public boolean eye;
      public int eyeT;
      public double lx;
      public double ly;
      public double lz;
      public double ax;
      public double az;
   }

   static final class LongQ {
      long[] a = new long[1024];
      int head;
      int tail;

      boolean isEmpty() {
         return this.head == this.tail;
      }

      void add(long var1) {
         if (this.tail == this.a.length) {
            if (this.head > 0) {
               System.arraycopy(this.a, this.head, this.a, 0, this.tail - this.head);
               this.tail = this.tail - this.head;
               this.head = 0;
            } else {
               this.a = Arrays.copyOf(this.a, this.a.length * 2);
            }
         }

         this.a[this.tail++] = var1;
      }

      long poll() {
         long var1 = this.a[this.head++];
         if (this.head == this.tail) {
            this.head = this.tail = 0;
         }

         return var1;
      }
   }
}

final class FastChunkCache {
   static final java.util.concurrent.ConcurrentLinkedQueue<java.lang.ref.WeakReference<FastChunkCache>> ALL =
      new java.util.concurrent.ConcurrentLinkedQueue<>();
   static final ThreadLocal<FastChunkCache> TL = ThreadLocal.withInitial(() -> {
      FastChunkCache c = new FastChunkCache();
      ALL.add(new java.lang.ref.WeakReference<>(c));
      return c;
   });

   final Chunk[] chunks = new Chunk[1024];
   World lastWorld;

   static void clear() {
      java.util.Iterator<java.lang.ref.WeakReference<FastChunkCache>> it = ALL.iterator();
      while (it.hasNext()) {
         FastChunkCache c = it.next().get();
         if (c == null) {
            it.remove();
         } else {
            c.lastWorld = null;
            Arrays.fill(c.chunks, null);
         }
      }
   }

   void switchWorld(World w, Chunk c, int slot) {
      if (this.lastWorld != w) {
         Arrays.fill(this.chunks, null);
         this.lastWorld = w;
      }
      this.chunks[slot] = c;
   }
}
