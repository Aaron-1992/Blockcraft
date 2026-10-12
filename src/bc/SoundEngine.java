package bc;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Blockcraft V4.2 Real-Life 3D Physical Acoustics & Minecraft Console Edition Music Engine.
 *
 * <h3>Real-Life Acoustic Physics</h3>
 * <ul>
 *   <li><b>3D Binaural Spatialization (ILD + ITD + Head Shadowing)</b>: Computes azimuth/elevation relative to the
 *       player's head orientation, applying constant-power stereo panning (Interaural Level Difference), sub-millisecond
 *       ear arrival delay up to 0.59 ms (Interaural Time Difference across a 21.5 cm head at 343 m/s), and high-frequency
 *       head-shadow filtering on the far ear.</li>
 *   <li><b>Distance Attenuation & Atmospheric Air Absorption</b>: Inverse-distance rolloff combined with distance-dependent
 *       low-pass filtering (air absorbs high frequencies faster than low frequencies over distance).</li>
 *   <li><b>Voxel Ray-Cast Sound Occlusion & Diffraction</b>: Traces 3D rays through {@link World} blocks from each sound
 *       source to the listener's ears (plus 4 diffraction rays around open doorways/windows). Solid walls attenuate
 *       volume and lower a 2-pole state-variable low-pass filter cutoff so sounds behind stone/brick/wood walls sound
 *       authentically muffled.</li>
 *   <li><b>3D Room / Cave Reverberation & Slapback Echo</b>: Casts 14 3D acoustic probe rays around the player to measure
 *       enclosure ratio, room dimensions, and material reflectivity (stone/brick/ice/quartz reflect strongly; wood reflects
 *       moderately; wool/snow/hay/leaves absorb sound like acoustic foam), driving a real-time stereo Schroeder reverb
 *       network (4 parallel comb filters + 2 series allpass diffusers + canyon slapback delay).</li>
 *   <li><b>Speed-of-Sound Propagation Delay (343 m/s) & Doppler Effect</b>: Distant explosions and lightning strikes arrive
 *       after a physical propagation delay {@code d / 343.0 s}, and moving sound sources / listeners shift pitch via
 *       {@code f' = f * (343 - vL) / (343 - vS)}.</li>
 *   <li><b>Underwater Hydrophone Muffling</b>: Submerging the player's ears underwater engages a 380 Hz low-pass filter
 *       across world sounds and music.</li>
 * </ul>
 */
public final class SoundEngine {
   public static final int MIX_RATE = 44100;
   public static final float SPEED_OF_SOUND = 343.0F;

   private static final int[] STEP_TABLE = {
      7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31,
      34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143,
      157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658,
      724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024,
      3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
      15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
   };
   private static final int[] INDEX_TABLE = {-1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8};

   /** Decoded PCM sample buffer from res/sounds/soundbank.bin or synthesized fallback. */
   public static final class Sample {
      public final String name;
      public final int sampleRate;
      public final short[] pcm;

      public Sample(String name, int sampleRate, short[] pcm) {
         this.name = name;
         this.sampleRate = sampleRate;
         this.pcm = pcm;
      }
   }

   /** Active 3D voice in the real-time acoustic mixer. */
   public static final class Voice {
      public Sample sample;
      public double pos;
      public double step;
      public float baseGain;
      public float basePitch;
      public boolean positional;
      public boolean loop;
      public double x;
      public double y;
      public double z;
      public double vx;
      public double vy;
      public double vz;
      public float maxDist = 28.0F;
      public float refDist = 2.5F;
      public float gainL;
      public float gainR;
      public int itdL;
      public int itdR;
      public float occlusion;
      public float lpCutoff = 1.0F;
      public float lpStateL;
      public float lpStateR;
      public boolean finished;
   }

   /** Delayed sound event travelling at the physical speed of sound (343 m/s). */
   private static final class DelayedSound {
      final String soundKey;
      final double x;
      final double y;
      final double z;
      final float volume;
      final float pitch;
      final double vx;
      final double vy;
      final double vz;
      final long triggerNano;

      DelayedSound(String soundKey, double x, double y, double z, float volume, float pitch, double vx, double vy, double vz, long triggerNano) {
         this.soundKey = soundKey;
         this.x = x;
         this.y = y;
         this.z = z;
         this.volume = volume;
         this.pitch = pitch;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.triggerNano = triggerNano;
      }
   }

   /** Full music & sound attribution entries displayed in the End Credits screen. */
   public static final String[][] CREDITS_SECTIONS = {
      {
         "MINECRAFT CONSOLE EDITION & TUMBLE / BATTLE SOUNDTRACK (33 TRACKS)",
         "Moog City 2, Mutation & Floating Trees (Menu OST) - C418 (Daniel Rosenfeld)",
         "Subwoofer Lullaby, Minecraft, Wet Hands, Sweden, Living Mice, Clark, Haggstrom, Mice on Venus - C418",
         "Aria Math & Blind Spots (Creative), Dead Voxel & Ballad of the Cats (Nether), Boss & Alpha - C418",
         "Double Time (Plastic 1), Nock (Plastic 2), Festive Tumble 1 & 2, Spooky Tumble 1 & 2 - Gareth Coker",
         "Standard Battle Themes 1-4 (Cove, Crucible, Cavern, Lair) & Shrink (Plastic 3) - Gareth Coker",
         "Frontier (Western), Fantasy, Greek Mythology, Chinese Mythology & Steampunk Battle Packs - Gareth Coker",
         "Legacy Console Edition Soundpack & Minigame Audio curated via Freshwaffle, Legacy-Edition-Minigames & Wilyicaro"
      },
      {
         "REALISTIC FOLEY & WORLD SOUND EFFECTS (CC-BY / CC0)",
         "Grass, Dirt, Wood, Gravel, Glass Footsteps & Block Dig/Place - Mito551 (CC BY-SA 3.0)",
         "Stone & Hard Surface Footsteps - Erdie / Freesound #41579 (CC BY 3.0)",
         "Sand Footsteps - worthahep88 / Freesound #319224 (CC0 1.0)",
         "Snow Crunch Footsteps - Ryding / Freesound #94337 (CC0 1.0)",
         "Ice Footsteps & Cracking - InspectorJ (#416967), dheming (#268023), Angel_Perez_Grandi (#49190) (CC BY 3.0)",
         "Metal Footsteps, Digging & Impacts - mypantsfelldown, yadronoff, Iwan 'qubodup' Gabovitch, Ogrebane",
         "Water Splashes & Wading - AGFX / Freesound Pack #1253 (CC BY 3.0)",
         "Wood Chopping & Foliage - Sheyvan (#476113), blukotek (#251660), lolamadeus (#179341) (CC0 1.0)",
         "Stone Mining & Flint Ignition - Benboncan / Freesound #71823 & #66457 (CC BY 3.0)",
         "Shattering Glass - cmusounddesign (#71947), Tomlija (#97669), lsprice (#88808) (CC BY 3.0)",
         "Wooden Doors, Iron Doors & Fence Gates - CGEffex (CC BY 3.0) & Auke 'sofar' Kok (CC BY-SA 3.0)",
         "Wooden Chests - Auke 'sofar' Kok, Sevin7, Percy Duke, kingsamas, bulbastre, Yoyodaman234",
         "Fire Crackle, Flowing Water & Lava - Dynamicell (#17548), Yuval (#197023), Halion (#17785), celeron55",
         "TNT Fuse & Explosion Shockwave - steveygos93 (#80401), theneedle.tv (#316682), TumeniNodes (CC0 1.0)",
         "Combat, Tool Break & Eating - sonictechtonic (#241872), HerbertBoland (#33206), Jordan 'AntumDeluge' Irwin"
      },
      {
         "3D REAL-LIFE PHYSICAL ACOUSTICS ENGINE",
         "Binaural Spatialization (Interaural Time & Level Difference + Head Shadowing)",
         "Voxel Ray-Cast Sound Occlusion, Wall Transmission Loss & Doorway Diffraction",
         "14-Ray 3D Room/Cave Size & Material Reflectivity Schroeder Reverb + Canyon Echo",
         "Speed-of-Sound Wave Propagation (343 m/s), Doppler Pitch Shift & Underwater Low-Pass Filter"
      }
   };

   private static final Map<String, Sample> BANK = new HashMap<>();
   private static final ConcurrentLinkedQueue<Voice> ACTIVE_VOICES = new ConcurrentLinkedQueue<>();
   private static final ConcurrentLinkedQueue<DelayedSound> DELAYED_QUEUE = new ConcurrentLinkedQueue<>();
   private static final Random RND = new Random();

   private static volatile boolean bankLoaded = false;
   private static volatile boolean audioThreadStarted = false;
   private static volatile boolean soundEnabled = true;
   private static volatile float masterSoundVol = 1.0F;
   private static volatile float masterMusicVol = 0.75F;
   private static volatile boolean realAcoustics = true;
   private static volatile boolean constantMusic = false;

   // Listener state
   private static volatile double listenerX;
   private static volatile double listenerY = 65.62;
   private static volatile double listenerZ;
   private static volatile double listenerVx;
   private static volatile double listenerVy;
   private static volatile double listenerVz;
   private static volatile float listenerYaw;
   private static volatile float listenerPitch;
   private static volatile boolean listenerUnderwater;

   // Real-life environment acoustic state (updated via 14-ray 3D voxel probing)
   private static volatile float roomEnclosure = 0.0F;
   private static volatile float roomSizeMeters = 24.0F;
   private static volatile float wallReflectivity = 0.35F;
   private static volatile float reverbWet = 0.08F;
   private static volatile float reverbFeedback = 0.45F;
   private static volatile float canyonEchoWet = 0.0F;

   // Minecraft Legacy Console Edition dynamic music state
   private static volatile String currentMusicKey = "lce_menu";
   private static volatile String targetMusicKey = "lce_menu";
   private static String pendingMusicKey = "lce_menu";
   private static int pendingMusicTicks = 0;
   private static volatile double musicPos = 0.0;
   private static volatile float musicFade = 1.0F;
   private static volatile int musicPauseSamples = 0;
   private static volatile int battleTrackIndex = 0;

   // Footstep & ambient tracking
   private static double stepDistAcc = 0.0;
   private static double prevPlayerX = Double.NaN;
   private static double prevPlayerY = Double.NaN;
   private static double prevPlayerZ = Double.NaN;
   private static double prevPlayerVy = 0.0;
   private static double fallStartY = Double.NaN;
   private static boolean wasOnGround = true;
   private static boolean wasInWater = false;
   private static int worldSettleTicks = 0;
   private static int stepCooldownTicks = 0;
   private static int ambientTick = 0;
   private static int digSoundCooldown = 0;
   private static String lastPlayedSound = "";

   private SoundEngine() {
   }

   /** Decodes 4-bit IMA ADPCM nibbles into 16-bit signed PCM samples. */
   public static short[] decodeAdpcm(byte[] data, int numSamples) {
      short[] out = new short[numSamples];
      int pred = 0;
      int idx = 0;
      for (int i = 0; i < numSamples; i++) {
         int b = data[i >> 1] & 0xFF;
         int nib = (i & 1) != 0 ? (b >>> 4) & 0x0F : b & 0x0F;
         int step = STEP_TABLE[idx];
         int vpdiff = step >> 3;
         if ((nib & 4) != 0) {
            vpdiff += step;
         }
         if ((nib & 2) != 0) {
            vpdiff += step >> 1;
         }
         if ((nib & 1) != 0) {
            vpdiff += step >> 2;
         }
         if ((nib & 8) != 0) {
            pred -= vpdiff;
         } else {
            pred += vpdiff;
         }
         if (pred < -32768) {
            pred = -32768;
         } else if (pred > 32767) {
            pred = 32767;
         }
         idx += INDEX_TABLE[nib];
         if (idx < 0) {
            idx = 0;
         } else if (idx > 88) {
            idx = 88;
         }
         out[i] = (short)pred;
      }
      return out;
   }

   /** Loads res/sounds/soundbank.bin and synthesizes procedural acoustic supplements. */
   public static synchronized void ensureLoaded() {
      if (bankLoaded) {
         return;
      }
      bankLoaded = true;
      InputStream stream = SoundEngine.class.getResourceAsStream("/sounds/soundbank.bin");
      if (stream == null) {
         try {
            java.io.File f = new java.io.File("res/sounds/soundbank.bin");
            if (f.isFile()) {
               stream = new java.io.FileInputStream(f);
            }
         } catch (Throwable ignored) {
         }
      }
      try (InputStream rawIn = stream) {
         if (rawIn != null) {
            DataInputStream in = new DataInputStream(rawIn);
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (magic[0] == 'B' && magic[1] == 'C' && magic[2] == 'S' && magic[3] == 'B') {
               int count = in.readInt();
               for (int i = 0; i < count; i++) {
                  int nameLen = in.readUnsignedShort();
                  int sr = in.readInt();
                  int nSamples = in.readInt();
                  byte[] nameBytes = new byte[nameLen];
                  in.readFully(nameBytes);
                  String name = new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8);
                  int dataLen = in.readInt();
                  byte[] adpcm = new byte[dataLen];
                  in.readFully(adpcm);
                  short[] pcm = decodeAdpcm(adpcm, nSamples);
                  if (name.startsWith("lce_")) {
                     pcm = normalizeAndTrimMusic(sr, pcm);
                  }
                  BANK.put(name, new Sample(name, sr, pcm));
               }
            }
         }
      } catch (Throwable t) {
         System.err.println("[SoundEngine] Failed to read soundbank.bin: " + t);
      }

      // Synthesize crisp acoustic supplements (UI click, bow twang, arrow whoosh, thunder clap, rain patter, note instruments)
      synthesizeAcousticSupplements();
      aliasSample("eat_1", "eat_bite");
      aliasSample("bow", "bow_shoot");
      aliasSample("tnt_prime", "tnt_ignite");
      aliasSample("explode", "tnt_explode");
      aliasSample("item_pickup", "item_pop");
      aliasSample("pop", "item_pop");

      if (System.getProperty("bc.test") == null && !Boolean.getBoolean("java.awt.headless")) {
         startAudioThread();
      }
   }

   /**
    * Trims long leading silence (> 0.15s) and peak-normalizes quiet Console Edition music tracks
    * (e.g. Sweden, Living Mice, Minecraft, Moog City 2) so all tracks start promptly at balanced volume.
    */
   private static short[] normalizeAndTrimMusic(int sr, short[] pcm) {
      if (pcm == null || pcm.length == 0) {
         return pcm;
      }
      int peak = 0;
      int firstAudible = 0;
      boolean foundStart = false;
      for (int i = 0; i < pcm.length; i++) {
         int a = Math.abs((int)pcm[i]);
         if (a > peak) {
            peak = a;
         }
         if (!foundStart && a > 220) {
            foundStart = true;
            firstAudible = Math.max(0, i - sr / 8);
         }
      }
      int trimStart = (foundStart && firstAudible > sr / 4 && pcm.length - firstAudible > sr * 5) ? firstAudible : 0;
      float gain = (peak > 100 && peak < 22000) ? Math.min(3.8F, 22000.0F / peak) : 1.0F;
      if (trimStart == 0 && gain <= 1.02F) {
         return pcm;
      }
      int newLen = pcm.length - trimStart;
      short[] out = new short[newLen];
      int fadeInLen = Math.min(newLen, sr / 10);
      for (int i = 0; i < newLen; i++) {
         float v = pcm[trimStart + i] * gain;
         if (trimStart > 0 && i < fadeInLen) {
            v *= (i / (float)fadeInLen);
         }
         int iv = Math.round(v);
         out[i] = (short)Math.max(-32767, Math.min(32767, iv));
      }
      return out;
   }

   private static void aliasSample(String alias, String target) {
      if (!BANK.containsKey(alias) && BANK.containsKey(target)) {
         BANK.put(alias, BANK.get(target));
      }
   }

   private static void synthesizeAcousticSupplements() {
      int sr = 22050;
      // 1. UI Click fallback (if not loaded from soundbank.bin)
      if (!BANK.containsKey("ui_click")) {
         short[] click = new short[sr / 25];
         for (int i = 0; i < click.length; i++) {
            double t = i / (double)sr;
            double env = Math.exp(-t * 140.0);
            double v = (Math.sin(2.0 * Math.PI * 1050.0 * t) * 0.65 + Math.sin(2.0 * Math.PI * 2100.0 * t) * 0.35) * env;
            click[i] = (short)(v * 18000.0);
         }
         BANK.put("ui_click", new Sample("ui_click", sr, click));
      }

      // 2. Bow Twang & Arrow Release
      short[] bow = new short[sr / 3];
      for (int i = 0; i < bow.length; i++) {
         double t = i / (double)sr;
         double env = Math.exp(-t * 14.0);
         double f = 195.0 + 45.0 * Math.exp(-t * 28.0);
         double stringWave = Math.sin(2.0 * Math.PI * f * t) + 0.45 * Math.sin(4.0 * Math.PI * f * t) + 0.2 * Math.sin(6.0 * Math.PI * f * t);
         double whoosh = (RND.nextDouble() * 2.0 - 1.0) * Math.exp(-Math.pow((t - 0.04) * 30.0, 2.0)) * 0.5;
         bow[i] = (short)(Math.max(-1.0, Math.min(1.0, (stringWave * 0.55 + whoosh) * env)) * 24000.0);
      }
      BANK.put("bow_shoot", new Sample("bow_shoot", sr, bow));

      // 3. Realistic Thunder Clap + Rolling Low-Frequency Rumble
      short[] thunder = new short[(int)(sr * 2.4)];
      double lp = 0.0;
      double lp2 = 0.0;
      Random tr = new Random(88421L);
      for (int i = 0; i < thunder.length; i++) {
         double t = i / (double)sr;
         double crackEnv = Math.exp(-t * 9.5);
         double rumbleEnv = Math.sin(Math.min(1.0, t / 0.15) * (Math.PI * 0.5)) * Math.exp(-t * 1.45) * (0.75 + 0.25 * Math.sin(t * 19.0));
         double n = tr.nextDouble() * 2.0 - 1.0;
         lp += 0.045 * (n - lp);
         lp2 += 0.018 * (lp - lp2);
         double sample = (n * 0.35 * crackEnv + lp2 * 2.8 * rumbleEnv);
         thunder[i] = (short)(Math.max(-1.0, Math.min(1.0, sample)) * 30000.0);
      }
      BANK.put("thunder", new Sample("thunder", sr, thunder));

      // 4. Rain Patter Loop (soft realistic raindrop impacts with smooth fade-in/out)
      short[] rain = new short[(int)(sr * 1.8)];
      double rlp = 0.0;
      for (int i = 0; i < rain.length; i++) {
         double env = Math.sin(Math.PI * i / (double)rain.length);
         double n = (tr.nextDouble() * 2.0 - 1.0) * 0.30;
         if (tr.nextInt(90) == 0) {
            n += (tr.nextDouble() * 2.0 - 1.0) * 0.70;
         }
         rlp += 0.22 * (n - rlp);
         rain[i] = (short)(Math.max(-1.0, Math.min(1.0, rlp * env)) * 12000.0);
      }
      BANK.put("rain_loop", new Sample("rain_loop", sr, rain));

      // 5. Note Block Acoustic Piano / Harp Pluck
      short[] note = new short[(int)(sr * 0.65)];
      for (int i = 0; i < note.length; i++) {
         double t = i / (double)sr;
         double env = Math.exp(-t * 6.5);
         double f = 440.0;
         double v = (Math.sin(2.0 * Math.PI * f * t)
            + 0.40 * Math.sin(4.0 * Math.PI * f * t) * Math.exp(-t * 10.0)
            + 0.18 * Math.sin(6.0 * Math.PI * f * t) * Math.exp(-t * 16.0)) * env;
         note[i] = (short)(Math.max(-1.0, Math.min(1.0, v * 0.68)) * 26000.0);
      }
      BANK.put("note_harp", new Sample("note_harp", sr, note));

      // 6. Item Pickup Pop fallback (if not loaded from soundbank.bin)
      if (!BANK.containsKey("item_pop")) {
         short[] pop = new short[sr / 12];
         for (int i = 0; i < pop.length; i++) {
            double t = i / (double)sr;
            double f = 380.0 + 480.0 * (t / (pop.length / (double)sr));
            double env = Math.sin(Math.PI * i / (double)pop.length);
            pop[i] = (short)(Math.sin(2.0 * Math.PI * f * t) * env * 20000.0);
         }
         BANK.put("item_pop", new Sample("item_pop", sr, pop));
      }
   }

   public static Sample getSample(String key) {
      ensureLoaded();
      return BANK.get(key);
   }

   public static boolean hasSample(String key) {
      ensureLoaded();
      return BANK.containsKey(key);
   }

   public static void loadSoundbank() {
      ensureLoaded();
   }

   public static int loadedSampleCount() {
      ensureLoaded();
      return BANK.size();
   }

   public static String currentMusicTrack() {
      return currentMusicKey;
   }

   public static String lastPlayedSound() {
      return lastPlayedSound;
   }

   public static float getRoomEnclosure() {
      return roomEnclosure;
   }

   public static float getRoomSizeMeters() {
      return roomSizeMeters;
   }

   public static float getWallReflectivity() {
      return wallReflectivity;
   }

   public static float getReverbWet() {
      return reverbWet;
   }

   public static boolean isListenerUnderwater() {
      return listenerUnderwater;
   }

   public static void setConstantMusic(boolean constant) {
      boolean was = constantMusic;
      constantMusic = constant;
      if (constant && !was && musicPauseSamples > MIX_RATE * 2) {
         musicPauseSamples = 0;
      }
   }

   public static boolean isConstantMusic() {
      return constantMusic;
   }

   public static void setVolumes(float soundVol, float musicVol, boolean acousticsOn) {
      masterSoundVol = Math.max(0.0F, Math.min(1.0F, soundVol));
      masterMusicVol = Math.max(0.0F, Math.min(1.0F, musicVol));
      realAcoustics = acousticsOn;
      soundEnabled = masterSoundVol > 0.001F || masterMusicVol > 0.001F;
   }

   /** Plays a non-positional 2D UI sound (e.g. button click, inventory action). */
   public static void playUi(String key, float volume, float pitch) {
      ensureLoaded();
      Sample s = BANK.get(key);
      if (s == null) {
         return;
      }
      lastPlayedSound = key;
      if (masterSoundVol <= 0.001F) {
         return;
      }
      Voice v = new Voice();
      v.sample = s;
      v.positional = false;
      v.baseGain = Math.max(0.0F, Math.min(1.5F, volume));
      v.basePitch = Math.max(0.4F, Math.min(2.2F, pitch));
      v.step = (s.sampleRate / (double)MIX_RATE) * v.basePitch;
      v.gainL = v.baseGain * 0.707F;
      v.gainR = v.baseGain * 0.707F;
      v.lpCutoff = 1.0F;
      ACTIVE_VOICES.add(v);
   }

   /** Plays a 3D world sound at (x, y, z) with real-life physical acoustics (spatialization, occlusion, Doppler, speed of sound). */
   public static Voice play3D(World w, String key, double x, double y, double z, float volume, float pitch) {
      return play3DMoving(w, key, x, y, z, 0.0, 0.0, 0.0, volume, pitch, false);
   }

   /** Plays a 3D sound with velocity (vx, vy, vz) in m/s for real-life Doppler shift and optional speed-of-sound delay. */
   public static Voice play3DMoving(
      World w,
      String key,
      double x,
      double y,
      double z,
      double vx,
      double vy,
      double vz,
      float volume,
      float pitch,
      boolean speedOfSoundDelay
   ) {
      ensureLoaded();
      Sample s = BANK.get(key);
      if (s == null) {
         return null;
      }
      lastPlayedSound = key;
      double dx = x - listenerX;
      double dy = y - listenerY;
      double dz = z - listenerZ;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      float maxDist = key.equals("tnt_explode") || key.equals("thunder") ? 96.0F : 28.0F;
      if (dist >= maxDist) {
         return null;
      }

      if (speedOfSoundDelay && realAcoustics && dist > 8.0) {
         long delayNanos = (long)(((dist - 6.0) / SPEED_OF_SOUND) * 1_000_000_000L);
         DELAYED_QUEUE.add(new DelayedSound(key, x, y, z, volume, pitch, vx, vy, vz, System.nanoTime() + delayNanos));
         return null;
      }

      Voice v = new Voice();
      v.sample = s;
      v.positional = true;
      v.x = x;
      v.y = y;
      v.z = z;
      v.vx = vx;
      v.vy = vy;
      v.vz = vz;
      v.baseGain = Math.max(0.0F, Math.min(2.0F, volume));
      v.basePitch = Math.max(0.4F, Math.min(2.2F, pitch));
      v.maxDist = maxDist;
      v.refDist = key.equals("tnt_explode") || key.equals("thunder") ? 8.0F : 2.5F;
      updateVoiceAcoustics(w, v);
      if (v.gainL > 0.001F || v.gainR > 0.001F) {
         ACTIVE_VOICES.add(v);
      }
      return v;
   }

   /**
    * Computes real-life 3D acoustic parameters for a voice:
    * - Inverse-distance attenuation + atmospheric high-frequency air absorption
    * - Binaural ILD (panning) + ITD (ear arrival sample offset) + head-shadow low-pass
    * - Voxel ray-cast wall occlusion & doorway diffraction
    * - Doppler pitch shift from source and listener radial velocities
    */
   public static void updateVoiceAcoustics(World w, Voice v) {
      if (!v.positional) {
         v.gainL = v.baseGain * 0.707F;
         v.gainR = v.baseGain * 0.707F;
         v.step = (v.sample.sampleRate / (double)MIX_RATE) * v.basePitch;
         return;
      }

      double dx = v.x - listenerX;
      double dy = v.y - listenerY;
      double dz = v.z - listenerZ;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist >= v.maxDist) {
         v.gainL = 0.0F;
         v.gainR = 0.0F;
         return;
      }

      // 1. Inverse-distance rolloff
      float normDist = (float)(dist / v.maxDist);
      float distAtten = (float)(1.0 / (1.0 + Math.pow(dist / v.refDist, 1.32))) * Math.max(0.0F, 1.0F - normDist);

      // 2. Voxel Ray-Cast Sound Occlusion & Doorway Diffraction
      float occ = 0.0F;
      if (realAcoustics && w != null && dist > 0.85) {
         occ = computeOcclusion(w, listenerX, listenerY, listenerZ, v.x, v.y, v.z);
      }
      v.occlusion = occ;
      float occGain = 1.0F - 0.72F * occ;

      // 3. Atmospheric Air Absorption + Wall Low-Pass Cutoff
      float airCutoff = 1.0F - 0.65F * normDist;
      float wallCutoff = 1.0F - 0.88F * occ;
      float underwaterCutoff = listenerUnderwater ? 0.14F : 1.0F;
      v.lpCutoff = Math.max(0.04F, Math.min(1.0F, airCutoff * wallCutoff * underwaterCutoff));

      // 4. Binaural Azimuth Panning (ILD) & Interaural Time Difference (ITD)
      float pan = 0.0F;
      if (dist > 0.05) {
         double invD = 1.0 / dist;
         double nx = dx * invD;
         double nz = dz * invD;
         // Listener right vector from yaw: forward = (-sin(yaw), cos(yaw)), right = (-cos(yaw), -sin(yaw))
         double yawRad = Math.toRadians(listenerYaw);
         double rx = -Math.cos(yawRad);
         double rz = -Math.sin(yawRad);
         pan = (float)Math.max(-1.0, Math.min(1.0, nx * rx + nz * rz));
      }

      float angle = (pan + 1.0F) * (float)(Math.PI * 0.25);
      float totalGain = v.baseGain * distAtten * occGain;
      v.gainL = (float)Math.cos(angle) * totalGain;
      v.gainR = (float)Math.sin(angle) * totalGain;

      // ITD: up to 26 samples (0.59 ms at 44.1 kHz) delay on the ear further from the sound source
      int itdSamples = realAcoustics ? Math.round(Math.abs(pan) * 26.0F) : 0;
      v.itdL = pan > 0.0F ? itdSamples : 0;
      v.itdR = pan < 0.0F ? itdSamples : 0;

      // 5. Real-Life Doppler Shift: f' = f * (c - vL) / (c - vS)
      float doppler = 1.0F;
      if (realAcoustics && dist > 0.25) {
         double invD = 1.0 / dist;
         double ux = dx * invD;
         double uy = dy * invD;
         double uz = dz * invD;
         double vListenerRadial = listenerVx * ux + listenerVy * uy + listenerVz * uz;
         double vSourceRadial = v.vx * ux + v.vy * uy + v.vz * uz;
         double num = SPEED_OF_SOUND + vListenerRadial;
         double den = Math.max(40.0, SPEED_OF_SOUND + vSourceRadial);
         doppler = (float)Math.max(0.55, Math.min(1.85, num / den));
      }

      v.step = (v.sample.sampleRate / (double)MIX_RATE) * v.basePitch * doppler;
   }

   /**
    * Traces a direct ray and 4 diffraction offset rays through {@link World} from listener (lx, ly, lz)
    * to source (sx, sy, sz). Returns an acoustic occlusion factor in {@code [0.0 .. 1.0]} where 0.0 is
    * unobstructed line-of-sight and 1.0 is heavily walled off.
    */
   public static float computeOcclusion(World w, double lx, double ly, double lz, double sx, double sy, double sz) {
      if (w == null) {
         return 0.0F;
      }
      float direct = traceRayOcclusion(w, lx, ly, lz, sx, sy, sz);
      if (direct <= 0.01F) {
         return 0.0F;
      }
      // Check 4 diffraction paths (simulating sound bending around open doorways, windows & corners)
      float dUp = traceRayOcclusion(w, lx, ly + 0.65, lz, sx, sy + 0.65, sz);
      float dEast = traceRayOcclusion(w, lx + 0.55, ly, lz, sx + 0.55, sy, sz);
      float dWest = traceRayOcclusion(w, lx - 0.55, ly, lz, sx - 0.55, sy, sz);
      float dNorth = traceRayOcclusion(w, lx, ly, lz - 0.55, sx, sy, sz - 0.55);
      float bestDiffraction = Math.min(Math.min(dUp, dEast), Math.min(dWest, dNorth));
      return Math.min(1.0F, direct * 0.55F + bestDiffraction * 0.45F + 0.08F);
   }

   private static float traceRayOcclusion(World w, double x0, double y0, double z0, double x1, double y1, double z1) {
      double dx = x1 - x0;
      double dy = y1 - y0;
      double dz = z1 - z0;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist < 0.8) {
         return 0.0F;
      }
      int steps = Math.min(48, (int)Math.ceil(dist * 1.6));
      int endBx = (int)Math.floor(x1);
      int endBy = (int)Math.floor(y1);
      int endBz = (int)Math.floor(z1);
      int startBx = (int)Math.floor(x0);
      int startBy = (int)Math.floor(y0);
      int startBz = (int)Math.floor(z0);
      int lastBx = Integer.MIN_VALUE;
      int lastBy = Integer.MIN_VALUE;
      int lastBz = Integer.MIN_VALUE;
      float occ = 0.0F;

      for (int i = 1; i < steps; i++) {
         double t = i / (double)steps;
         int bx = (int)Math.floor(x0 + dx * t);
         int by = (int)Math.floor(y0 + dy * t);
         int bz = (int)Math.floor(z0 + dz * t);
         if ((bx == startBx && by == startBy && bz == startBz)
            || (bx == endBx && by == endBy && bz == endBz)
            || (bx == lastBx && by == lastBy && bz == lastBz)) {
            continue;
         }
         lastBx = bx;
         lastBy = by;
         lastBz = bz;
         int id = w.getBlock(bx, by, bz);
         if (id != 0 && Blocks.solid[id] && !Blocks.isFluid(id)) {
            occ += blockOcclusionWeight(id);
            if (occ >= 1.0F) {
               return 1.0F;
            }
         }
      }
      return Math.min(1.0F, occ);
   }

   private static float blockOcclusionWeight(int id) {
      if (id == Blocks.WOOL0 || (id >= Blocks.WOOL0 && id < Blocks.WOOL0 + 16) || id == Blocks.HAY_BALE || id == Blocks.SNOW) {
         // Acoustic dampening materials absorb sound energy strongly
         return 0.36F;
      }
      if (id == Blocks.GLASS || (id >= Blocks.GLASS0 && id < Blocks.GLASS0 + 16) || Blocks.leafTable[id]) {
         return 0.14F;
      }
      if (Blocks.mineClass[id] == 1 || id == Blocks.OBSIDIAN || id == Blocks.BEDROCK) {
         // Dense stone / brick / metal / obsidian
         return 0.30F;
      }
      return 0.20F;
   }

   /** Returns the acoustic reflectivity [0.05 .. 0.92] of a block surface for cave/room reverb & echo calculation. */
   public static float blockReflectivity(int id) {
      if (id >= Blocks.WOOL0 && id < Blocks.WOOL0 + 16 || id == Blocks.SNOW || id == Blocks.HAY_BALE || Blocks.leafTable[id]) {
         return 0.08F; // Wool, snow, hay, and leaves absorb sound waves (minimal echo)
      }
      if (id == Blocks.GRASS || id == Blocks.DIRT || id == Blocks.SAND || id == Blocks.GRAVEL || id == Blocks.FARMLAND || id == Blocks.FARMLAND_WET) {
         return 0.24F;
      }
      if (id == Blocks.PLANKS || Blocks.logKind[id] != 0 || Blocks.mineClass[id] == 2) {
         return 0.52F; // Warm wooden room reflection
      }
      if (Blocks.mineClass[id] == 1 || id == Blocks.OBSIDIAN || id == Blocks.BEDROCK || id == Blocks313.ICE || id == Blocks313.PACKED_ICE) {
         return 0.88F; // Hard stone, brick, quartz, prismarine, ice reflect sound strongly (cave/hall reverb)
      }
      return 0.45F;
   }

   private static final double[][] PROBE_DIRS = {
      {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
      {0.577, 0.577, 0.577}, {-0.577, 0.577, 0.577}, {0.577, 0.577, -0.577}, {-0.577, 0.577, -0.577},
      {0.577, -0.35, 0.577}, {-0.577, -0.35, 0.577}, {0.577, -0.35, -0.577}, {-0.577, -0.35, -0.577}
   };

   /**
    * Casts 14 3D acoustic probe rays around the listener to measure room/cave enclosure, average wall
    * distance, and surface material reflectivity, updating the Schroeder Reverb & Canyon Echo parameters.
    */
   public static void updateEnvironmentAcoustics(World w, double lx, double ly, double lz) {
      if (w == null || !realAcoustics) {
         roomEnclosure = 0.0F;
         roomSizeMeters = 24.0F;
         wallReflectivity = 0.25F;
         reverbWet = 0.04F;
         reverbFeedback = 0.35F;
         canyonEchoWet = 0.0F;
         return;
      }

      int hits = 0;
      int farCliffHits = 0;
      float totalDist = 0.0F;
      float totalRefl = 0.0F;
      int maxRange = 28;

      for (double[] dir : PROBE_DIRS) {
         double dx = dir[0];
         double dy = dir[1];
         double dz = dir[2];
         int hitBlock = 0;
         int hitDist = maxRange;
         for (int d = 1; d <= maxRange; d++) {
            int bx = (int)Math.floor(lx + dx * d);
            int by = (int)Math.floor(ly + dy * d);
            int bz = (int)Math.floor(lz + dz * d);
            int id = w.getBlock(bx, by, bz);
            if (id != 0 && Blocks.solid[id] && !Blocks.isFluid(id)) {
               hitBlock = id;
               hitDist = d;
               break;
            }
         }
         totalDist += hitDist;
         if (hitBlock != 0) {
            if (hitDist <= 18) {
               hits++;
            } else if (blockReflectivity(hitBlock) > 0.7F) {
               farCliffHits++;
            }
            totalRefl += blockReflectivity(hitBlock);
         } else {
            totalRefl += 0.05F;
         }
      }

      float n = PROBE_DIRS.length;
      roomEnclosure = hits / n;
      roomSizeMeters = totalDist / n;
      wallReflectivity = totalRefl / n;

      // Compute Schroeder Reverb wet mix & decay feedback from physical enclosure + material reflectivity
      float sizeFactor = Math.max(0.25F, Math.min(1.0F, roomSizeMeters / 16.0F));
      reverbWet = Math.min(0.48F, roomEnclosure * wallReflectivity * 0.58F);
      reverbFeedback = Math.max(0.25F, Math.min(0.88F, 0.30F + roomEnclosure * wallReflectivity * sizeFactor * 0.62F));
      canyonEchoWet = roomEnclosure < 0.55F && farCliffHits >= 2 ? Math.min(0.28F, farCliffHits * 0.07F) : 0.0F;
   }

   /**
    * Maps ANY block ID to its physical surface sound category:
    * "grass", "leaves", "dirt", "sand", "gravel", "snow", "ice", "glass", "slime", "metal", "wood", or "stone".
    */
   public static String blockMaterialCategory(int id) {
      if (id <= 0) {
         return "stone";
      }
      if (id == Redstone319.SLIME_BLOCK) {
         return "slime";
      }
      // Leaves, vines, bushes, flowers, crops & foliage
      if (Blocks.leafTable[id]
         || id == Blocks.TALLGRASS
         || id == Blocks.FLOWER_RED
         || id == Blocks.FLOWER_YELLOW
         || id == Blocks.SUGAR_CANE
         || id == Blocks.CACTUS
         || id == Blocks313.FERN
         || id == Blocks313.DEAD_BUSH
         || id == Blocks313.LILY_PAD
         || id == Blocks313.MUSHROOM_BROWN
         || id == Blocks313.MUSHROOM_RED
         || Blocks313.isVine(id)
         || (id >= Blocks313.FLOWER0 && id < Blocks313.FLOWER0 + Blocks313.FLOWERS)
         || (id >= Blocks313.DOUBLE0 && id < Blocks313.DOUBLE0 + Blocks313.DOUBLES * 2)
         || (id >= Blocks.WHEAT && id <= Blocks.POTATO0 + 3)) {
         return "leaves";
      }
      // Grass, hay, wool, carpet & giant mushrooms
      if (id == Blocks.GRASS
         || id == Blocks.HAY_BALE
         || id == Blocks313.MUSH_BLOCK_RED
         || id == Blocks313.MUSH_BLOCK_BROWN
         || id == Blocks313.MUSH_STEM
         || (id >= Blocks.WOOL0 && id < Blocks.WOOL0 + 16)
         || Shapes.isCarpet(id)) {
         return "grass";
      }
      // Dirt, Coarse Dirt, Podzol, Mycelium, Farmland, Clay & Soul Sand
      if (id == Blocks.DIRT
         || id == Blocks313.COARSE_DIRT
         || id == Blocks313.PODZOL
         || id == Blocks313.MYCELIUM
         || id == Blocks.FARMLAND
         || id == Blocks.FARMLAND_WET
         || id == Blocks.CLAY
         || id == Blocks.SOUL_SAND) {
         return "dirt";
      }
      if (id == Blocks.SAND || id == Blocks313.RED_SAND) {
         return "sand";
      }
      if (id == Blocks.GRAVEL) {
         return "gravel";
      }
      if (id == Blocks.SNOW || id == Blocks313.SNOW_LAYER) {
         return "snow";
      }
      if (id == Blocks313.ICE || id == Blocks313.PACKED_ICE) {
         return "ice";
      }
      if (id == Blocks.GLASS || (id >= Blocks.GLASS0 && id < Blocks.GLASS0 + 16) || Shapes.isPane(id) || id == Blocks.GLOWSTONE || id == Redstone319.SEA_LANTERN) {
         return "glass";
      }
      if (id == Blocks.IRON_BLOCK || id == Blocks.GOLD_BLOCK || id == Blocks.DIAMOND_BLOCK || id == Blocks.EMERALD_BLOCK
         || id == Redstone319.REDSTONE_BLOCK || id == Shapes.BARS) {
         return "metal";
      }
      if (id == Blocks.PLANKS
         || Blocks313.isPlanks(id)
         || Blocks.logKind[id] != 0
         || Blocks.mineClass[id] == Items.C_AXE
         || id == Blocks.CRAFTING_TABLE
         || id == Blocks.CHEST
         || id == Blocks.CHEST_LOOT
         || id == Blocks.BOOKSHELF
         || id == Redstone319.NOTE_BLOCK
         || id == Blocks.PUMPKIN
         || id == Blocks.JACK_O_LANTERN
         || id == Blocks.MELON
         || id == Blocks.LADDER
         || id == Blocks.FENCE
         || Blocks.isFence(id)
         || Shapes.isGate(id)
         || Shapes.isTrap(id)
         || Blocks.isDoor(id)) {
         return "wood";
      }
      // Catch any remaining shovel-mined earth block as dirt
      if (Blocks.mineClass[id] == Items.C_SHOVEL) {
         return "dirt";
      }
      return "stone";
   }

   /** Plays a surface-matched footstep sound at (x, y, z). */
   public static void playFootstep(World w, int blockId, double x, double y, double z, float volumeScale) {
      String mat = blockMaterialCategory(blockId);
      String key = switch (mat) {
         case "grass" -> "step_grass_" + (1 + RND.nextInt(3));
         case "leaves" -> (hasSample("step_leaves_1") ? "step_leaves_" + (1 + RND.nextInt(2)) : "step_grass_" + (1 + RND.nextInt(3)));
         case "dirt" -> "step_dirt_" + (1 + RND.nextInt(2));
         case "wood" -> "step_wood_" + (1 + RND.nextInt(2));
         case "sand" -> "step_sand_" + (1 + RND.nextInt(2));
         case "gravel" -> "step_gravel_" + (1 + RND.nextInt(3));
         case "snow" -> "step_snow_" + (1 + RND.nextInt(3));
         case "ice" -> "step_ice_" + (1 + RND.nextInt(2));
         case "metal" -> "step_metal_" + (1 + RND.nextInt(2));
         case "glass" -> "step_glass_1";
         case "slime" -> (hasSample("slime_bounce") ? "slime_bounce" : "step_grass_1");
         default -> "step_stone_" + (1 + RND.nextInt(3));
      };
      float pitch = 0.92F + RND.nextFloat() * 0.16F;
      play3D(w, key, x, y, z, 0.55F * volumeScale, pitch);
   }

   /** Plays a block mining / hitting sound at (x, y, z). */
   public static void playBlockDig(World w, int blockId, int x, int y, int z) {
      if (digSoundCooldown > 0) {
         return;
      }
      digSoundCooldown = 4;
      String mat = blockMaterialCategory(blockId);
      String key = switch (mat) {
         case "wood" -> "dig_wood_" + (1 + RND.nextInt(2));
         case "leaves" -> (hasSample("step_leaves_1") ? "step_leaves_" + (1 + RND.nextInt(2)) : "dig_foliage_1");
         case "grass" -> "step_grass_" + (1 + RND.nextInt(3));
         case "dirt" -> "dig_dirt_1";
         case "sand" -> "step_sand_" + (1 + RND.nextInt(2));
         case "snow" -> "step_snow_" + (1 + RND.nextInt(3));
         case "gravel" -> "dig_gravel_1";
         case "ice", "glass" -> "dig_ice_1";
         case "metal" -> "dig_metal_1";
         case "slime" -> (hasSample("slime_bounce") ? "slime_bounce" : "dig_foliage_1");
         default -> "dig_stone_" + (1 + RND.nextInt(2));
      };
      play3D(w, key, x + 0.5, y + 0.5, z + 0.5, 0.55F, 0.90F + RND.nextFloat() * 0.16F);
   }

   /** Plays a block breaking sound at (x, y, z). */
   public static void playBlockBreak(World w, int blockId, int x, int y, int z) {
      String mat = blockMaterialCategory(blockId);
      String key = switch (mat) {
         case "glass" -> "break_glass_" + (1 + RND.nextInt(2));
         case "leaves" -> (hasSample("step_leaves_1") ? "step_leaves_" + (1 + RND.nextInt(2)) : "dig_foliage_1");
         case "grass" -> "step_grass_" + (1 + RND.nextInt(3));
         case "dirt" -> "dig_dirt_1";
         case "sand" -> "step_sand_" + (1 + RND.nextInt(2));
         case "snow" -> "step_snow_" + (1 + RND.nextInt(3));
         case "gravel" -> "break_gravel_1";
         case "wood" -> "dig_wood_" + (1 + RND.nextInt(2));
         case "ice" -> "break_ice_1";
         case "metal" -> "break_metal_1";
         case "slime" -> (hasSample("slime_bounce") ? "slime_bounce" : "break_block_1");
         default -> "break_block_" + (1 + RND.nextInt(2));
      };
      play3D(w, key, x + 0.5, y + 0.5, z + 0.5, 0.85F, 0.88F + RND.nextFloat() * 0.20F);
   }

   /** Plays a block placement sound at (x, y, z). */
   public static void playBlockPlace(World w, int blockId, int x, int y, int z) {
      String mat = blockMaterialCategory(blockId);
      String key = switch (mat) {
         case "metal" -> "place_metal_1";
         case "grass" -> "step_grass_" + (1 + RND.nextInt(3));
         case "leaves" -> (hasSample("step_leaves_1") ? "step_leaves_" + (1 + RND.nextInt(2)) : "dig_foliage_1");
         case "dirt" -> "step_dirt_" + (1 + RND.nextInt(2));
         case "sand" -> "step_sand_" + (1 + RND.nextInt(2));
         case "gravel" -> "step_gravel_" + (1 + RND.nextInt(3));
         case "snow" -> "step_snow_" + (1 + RND.nextInt(3));
         case "slime" -> (hasSample("slime_bounce") ? "slime_bounce" : "place_block_1");
         case "stone", "ice", "glass" -> "place_stone_1";
         default -> "place_block_" + (1 + RND.nextInt(2));
      };
      play3D(w, key, x + 0.5, y + 0.5, z + 0.5, 0.80F, 0.90F + RND.nextFloat() * 0.18F);
   }

   /** Plays a weapon or hand air-swing ("whoosh") sound at (x, y, z). */
   public static void playSwing(World w, double x, double y, double z) {
      String key = hasSample("swing_1") ? "swing_" + (1 + RND.nextInt(2)) : "bow_shoot";
      play3D(w, key, x, y, z, 0.55F, 0.92F + RND.nextFloat() * 0.18F);
   }

   /** Plays a melee attack impact sound (and critical-hit chime if crit is true) at (x, y, z). */
   public static void playHit(World w, double x, double y, double z, boolean crit) {
      String key = hasSample("hit_1") ? "hit_" + (1 + RND.nextInt(2)) : "player_hurt";
      play3D(w, key, x, y, z, crit ? 0.95F : 0.78F, (crit ? 0.85F : 0.95F) + RND.nextFloat() * 0.15F);
   }

   /** Plays a mechanical click for levers, buttons, pressure plates, dispensers & droppers. */
   public static void playClickBlock(World w, int x, int y, int z, boolean active) {
      String key = hasSample("click_block") ? "click_block" : "ui_click";
      play3D(w, key, x + 0.5, y + 0.5, z + 0.5, 0.65F, active ? 0.75F : 0.58F);
   }

   /** Plays a projectile throw sound (snowball, ender pearl, egg, etc.). */
   public static void playThrow(World w, double x, double y, double z) {
      String key = hasSample("swing_1") ? "swing_1" : "bow_shoot";
      play3D(w, key, x, y, z, 0.68F, 1.18F + RND.nextFloat() * 0.16F);
   }

   /** Plays an authentic Console Edition Minigame announcer/event sound ("mg_count_1..3", "mg_count_go", "mg_refill", "mg_showdown", "mg_win", "mg_lose"). */
   public static void playMinigameSound(String key) {
      if (hasSample(key)) {
         playUi(key, 0.85F, 1.0F);
      } else {
         playUi("ui_click", 0.75F, 1.0F);
      }
   }

   private static final Object MUSIC_LOCK = new Object();

   /** Advances to the next Console Edition Battle / Tumble / Overworld / Menu track immediately. */
   public static void nextBattleTrack() {
      synchronized (MUSIC_LOCK) {
         battleTrackIndex = (battleTrackIndex + 1) % BATTLE_TRACKS.length;
         menuTrackIndex = (menuTrackIndex + 1) % MENU_TRACKS.length;
         activeRandomMenuTrack = MENU_TRACKS[menuTrackIndex];
         activeRandomOverworldTrack = OVERWORLD_TRACKS[RND.nextInt(OVERWORLD_TRACKS.length)];
         musicPauseSamples = 0;
         musicPos = 0.0;
         musicFade = 1.0F;
         currentMusicKey = "";
      }
   }

   /** Plays a Note Block note at (x, y, z) pitched according to the block underneath and note pitch (0..24). */
   public static void playNoteBlock(World w, int x, int y, int z, int pitchStep) {
      float pitch = (float)Math.pow(2.0, (pitchStep - 12) / 12.0);
      play3D(w, "note_harp", x + 0.5, y + 0.5, z + 0.5, 1.0F, pitch);
   }

   private static boolean isSamplePlaying(String key) {
      for (Voice v : ACTIVE_VOICES) {
         if (!v.finished && v.sample != null && key.equals(v.sample.name)) {
            return true;
         }
      }
      return false;
   }

   private static final String[] MENU_TRACKS = {"lce_menu", "lce_menu_2", "lce_menu_3"};
   private static final String[] TUMBLE_TRACKS = {
      "lce_tumble_1", "lce_tumble_2", "lce_tumble_3", "lce_tumble_4", "lce_tumble_5"
   };
   private static final String[] BATTLE_TRACKS = {
      "lce_tumble_1",
      "lce_battle_4", "lce_battle_5", "lce_battle_6", "lce_battle_7",
      "lce_battle_1", "lce_battle_2", "lce_battle_3",
      "lce_battle_8", "lce_battle_9", "lce_battle_10", "lce_battle_11",
      "lce_tumble_2", "lce_tumble_3", "lce_tumble_4", "lce_tumble_5"
   };
   private static final String[] OVERWORLD_TRACKS = {
      "lce_morning", "lce_day", "lce_afternoon", "lce_sunset", "lce_night",
      "lce_overworld_6", "lce_overworld_7", "lce_overworld_8"
   };
   private static final String[] CREATIVE_TRACKS = {"lce_creative", "lce_creative_2"};
   private static final String[] NETHER_TRACKS = {"lce_nether", "lce_nether_2"};
   private static volatile String activeRandomOverworldTrack = "";
   private static volatile String activeRandomMenuTrack = "";
   private static volatile int menuTrackIndex = 0;

   public static int totalConsoleMusicTrackCount() {
      int n = 0;
      for (String k : BANK.keySet()) {
         if (k.startsWith("lce_")) {
            n++;
         }
      }
      return n;
   }

   public static String musicTrackTitle(String key) {
      if (key == null || key.isEmpty()) return "Silent";
      switch (key) {
         case "lce_menu":        return "C418 - Moog City 2";
         case "lce_menu_2":      return "C418 - Mutation";
         case "lce_menu_3":      return "C418 - Floating Trees";
         case "lce_morning":     return "C418 - Subwoofer Lullaby";
         case "lce_day":         return "C418 - Minecraft";
         case "lce_afternoon":   return "C418 - Wet Hands";
         case "lce_sunset":      return "C418 - Sweden";
         case "lce_night":       return "C418 - Living Mice";
         case "lce_overworld_6": return "C418 - Clark";
         case "lce_overworld_7": return "C418 - Haggstrom";
         case "lce_overworld_8": return "C418 - Mice on Venus";
         case "lce_creative":    return "C418 - Aria Math";
         case "lce_creative_2":  return "C418 - Blind Spots";
         case "lce_nether":      return "C418 - Dead Voxel";
         case "lce_nether_2":    return "C418 - Ballad of the Cats";
         case "lce_end":         return "C418 - Boss (The End)";
         case "lce_credits":     return "C418 - Alpha (Credits)";
         case "lce_tumble_1":    return "Gareth Coker - Double Time";
         case "lce_tumble_2":    return "Gareth Coker - Nock";
         case "lce_tumble_3":    return "Gareth Coker - Festive 1";
         case "lce_tumble_4":    return "Gareth Coker - Festive 2";
         case "lce_tumble_5":    return "Gareth Coker - Spooky 2";
         case "lce_battle_1":    return "Gareth Coker - Shrink";
         case "lce_battle_2":    return "Gareth Coker - Frontier";
         case "lce_battle_3":    return "Gareth Coker - Spooky 1";
         case "lce_battle_4":    return "Gareth Coker - Cove Battle";
         case "lce_battle_5":    return "Gareth Coker - Crucible Battle";
         case "lce_battle_6":    return "Gareth Coker - Cavern Battle";
         case "lce_battle_7":    return "Gareth Coker - Showdown";
         case "lce_battle_8":    return "Gareth Coker - Fantasy Pack";
         case "lce_battle_9":    return "Gareth Coker - Greek Pack";
         case "lce_battle_10":   return "Gareth Coker - Mythology Pack";
         case "lce_battle_11":   return "Gareth Coker - Steampunk Pack";
         default:                return key;
      }
   }

   public static String currentMusicDisplayName() {
      String k = currentMusicKey != null && !currentMusicKey.isEmpty() ? currentMusicKey : targetMusicKey;
      return musicTrackTitle(k);
   }

   /**
    * Selects the appropriate Minecraft Legacy Console Edition music track for the current screen,
    * minigame mode (Gareth Coker Tumble & Battle themes), dimension, creative mode, and time of day.
    */
   public static String selectConsoleMusic(Game g) {
      if (g == null) {
         return "lce_menu";
      }
      if (g.screen == Game.S.CREDITS) {
         return "lce_credits"; // C418 - Alpha (End Poem & Credits)
      }
      if (g.screen == Game.S.MINIGAMES) {
         return TUMBLE_TRACKS[0]; // Gareth Coker - Double Time (That One Tumble Song!)
      }
      if (g.pano != null
         || g.screen == Game.S.TITLE
         || g.screen == Game.S.WORLDS
         || g.screen == Game.S.NEWWORLD
         || g.screen == Game.S.MULTIPLAYER
         || g.screen == Game.S.PACKS
         || g.screen == Game.S.MODS
         || g.screen == Game.S.SKINS
         || g.world == null
         || g.player == null) {
         return "lce_menu"; // C418 - Moog City 2 (Title / Menus)
      }
      if (BackroomsStalkerMod.isActive()) {
         return ""; // Backrooms uses eerie fluorescent hum & stalker audio
      }

      // Console Edition Mini Games (Tumble, Battle, Bedwars, CS2 Strike & Murder Mystery): Gareth Coker's Tumble & Battle OST
      // Plays continuously across the Waiting Lobby, Active Round, and Post-Match Overview!
      if (g.world.settings != null && g.world.settings.isMinigame()) {
         int idx = Math.max(0, battleTrackIndex);
         if (g.world.settings.type == WorldSettings.TUMBLE) {
            return TUMBLE_TRACKS[idx % TUMBLE_TRACKS.length];
         }
         if (g.world.settings.type == WorldSettings.BATTLE) {
            return BATTLE_TRACKS[idx % BATTLE_TRACKS.length];
         }
         if (g.world.settings.type == WorldSettings.BEDWARS) {
            return BATTLE_TRACKS[(idx + 1) % BATTLE_TRACKS.length];
         }
         if (g.world.settings.type == WorldSettings.CS2) {
            return BATTLE_TRACKS[(idx + 2) % BATTLE_TRACKS.length];
         }
         if (g.world.settings.type == WorldSettings.MURDER) {
            return BATTLE_TRACKS[(idx + 3) % BATTLE_TRACKS.length];
         }
      }
      if (g.screen == Game.S.LOADING || g.screen == Game.S.WAITING) {
         return "lce_menu";
      }

      // Dimension-specific Legacy Console Edition themes
      if (g.world.dim == Dim.END) {
         return "lce_end"; // C418 - Boss / The End
      }
      if (g.world.dim == Dim.NETHER) {
         return NETHER_TRACKS[Math.max(0, battleTrackIndex) % NETHER_TRACKS.length]; // C418 - Dead Voxel & Ballad of the Cats
      }

      // Overworld by time of day & game mode (8 C418 Overworld tracks + 2 C418 Creative tracks)
      int tod = (int)((g.world.dayTime % 24000L + 24000L) % 24000L);
      if (tod < 2500) {
         return "lce_morning"; // C418 - Subwoofer Lullaby
      } else if (tod < 5500) {
         return g.player.creative ? CREATIVE_TRACKS[Math.max(0, battleTrackIndex) % CREATIVE_TRACKS.length] : "lce_day"; // Aria Math / Blind Spots or Minecraft
      } else if (tod < 8500) {
         return g.player.creative ? "lce_creative_2" : "lce_overworld_6"; // Blind Spots (Creative) / Clark (Survival)
      } else if (tod < 11500) {
         return "lce_afternoon"; // C418 - Wet Hands
      } else if (tod < 14500) {
         return "lce_sunset"; // C418 - Sweden
      } else if (tod < 17500) {
         return "lce_overworld_7"; // C418 - Haggstrom
      } else if (tod < 20500) {
         return "lce_night"; // C418 - Living Mice
      } else {
         return "lce_overworld_8"; // C418 - Mice on Venus
      }
   }

   /** Compatibility alias for selectConsoleMusic. */
   public static String selectAnimalCrossingMusic(Game g) {
      return selectConsoleMusic(g);
   }

   /** Per-tick update called from Game.tick() to drive footsteps, weather/block ambience, delayed speed-of-sound events, and music selection. */
   public static void tick(Game g) {
      ensureLoaded();
      if (g == null) {
         return;
      }
      if (g.opt != null) {
         setVolumes(g.opt.soundVolume, g.opt.musicVolume, g.opt.realAcoustics);
         setConstantMusic(g.opt.constantMusic);
      }

      String desiredMusic = selectConsoleMusic(g);
      boolean isMinigameTrack = desiredMusic.startsWith("lce_battle_") || desiredMusic.startsWith("lce_tumble_");
      if (desiredMusic.equals("lce_menu") && !activeRandomMenuTrack.isEmpty()) {
         desiredMusic = activeRandomMenuTrack;
      }
      boolean isMenuTrack = desiredMusic.startsWith("lce_menu");
      if (!isMinigameTrack && !desiredMusic.isEmpty() && !isMenuTrack && !desiredMusic.equals("lce_credits")
         && !desiredMusic.startsWith("lce_nether") && !desiredMusic.equals("lce_end") && !activeRandomOverworldTrack.isEmpty()) {
         desiredMusic = activeRandomOverworldTrack;
      } else if (isMenuTrack || desiredMusic.equals("lce_credits") || isMinigameTrack) {
         activeRandomOverworldTrack = "";
      }
      if (!isMenuTrack) {
         activeRandomMenuTrack = "";
      }

      boolean immediateSwitch = desiredMusic.isEmpty()
         || isMenuTrack
         || desiredMusic.equals("lce_credits")
         || isMinigameTrack
         || currentMusicKey.startsWith("lce_menu")
         || currentMusicKey.equals("lce_credits")
         || currentMusicKey.startsWith("lce_battle_")
         || currentMusicKey.startsWith("lce_tumble_")
         || currentMusicKey.isEmpty();
      synchronized (MUSIC_LOCK) {
         if (immediateSwitch) {
            targetMusicKey = desiredMusic;
            pendingMusicKey = desiredMusic;
            pendingMusicTicks = 0;
            if (currentMusicKey.isEmpty() && !targetMusicKey.isEmpty()) {
               currentMusicKey = targetMusicKey;
               musicPos = 0.0;
               musicPauseSamples = 0;
               musicFade = 1.0F;
            }
         } else if (!desiredMusic.equals(targetMusicKey)) {
            if (desiredMusic.equals(pendingMusicKey)) {
               pendingMusicTicks++;
               if (pendingMusicTicks >= 60) { // 3 seconds of stability before switching in-game track
                  targetMusicKey = desiredMusic;
                  pendingMusicTicks = 0;
               }
            } else {
               pendingMusicKey = desiredMusic;
               pendingMusicTicks = 1;
            }
         } else {
            pendingMusicKey = desiredMusic;
            pendingMusicTicks = 0;
         }
      }

      // Never run world sounds on Title/Menu Panorama screens or when no real world is active
      if (g.pano != null
         || g.world == null
         || g.player == null
         || g.screen == Game.S.TITLE
         || g.screen == Game.S.WORLDS
         || g.screen == Game.S.NEWWORLD
         || g.screen == Game.S.MINIGAMES
         || g.screen == Game.S.MULTIPLAYER
         || g.screen == Game.S.LOADING
         || g.screen == Game.S.CREDITS
         || g.screen == Game.S.WAITING) {
         listenerUnderwater = false;
         ACTIVE_VOICES.removeIf(v -> v.positional);
         DELAYED_QUEUE.clear();
         prevPlayerX = Double.NaN;
         worldSettleTicks = 0;
         return;
      }

      if (g.screen == Game.S.PAUSE || g.screen == Game.S.DEAD) {
         listenerUnderwater = false;
         prevPlayerX = g.player.x;
         prevPlayerY = g.player.y;
         prevPlayerZ = g.player.z;
         return;
      }

      if (digSoundCooldown > 0) {
         digSoundCooldown--;
      }
      if (stepCooldownTicks > 0) {
         stepCooldownTicks--;
      }

      Player p = g.player;
      World w = g.world;
      listenerX = p.x;
      listenerY = p.y + p.eye;
      listenerZ = p.z;
      listenerVx = p.vx * 20.0;
      listenerVy = p.vy * 20.0;
      listenerVz = p.vz * 20.0;
      listenerYaw = p.yaw;
      listenerPitch = p.pitch;

      int headBlock = w.getBlock((int)Math.floor(listenerX), (int)Math.floor(listenerY), (int)Math.floor(listenerZ));
      listenerUnderwater = Blocks.isWater(headBlock);

      // Update 14-ray 3D room/cave acoustics every 6 ticks
      ambientTick++;
      if ((ambientTick % 6) == 0) {
         updateEnvironmentAcoustics(w, listenerX, listenerY, listenerZ);
         for (Voice v : ACTIVE_VOICES) {
            if (!v.finished && v.positional) {
               updateVoiceAcoustics(w, v);
            }
         }
      }

      // Process speed-of-sound delayed queue (e.g. distant thunder & explosions)
      long now = System.nanoTime();
      DelayedSound ds;
      while ((ds = DELAYED_QUEUE.peek()) != null && ds.triggerNano <= now) {
         DELAYED_QUEUE.poll();
         play3DMoving(w, ds.soundKey, ds.x, ds.y, ds.z, ds.vx, ds.vy, ds.vz, ds.volume, ds.pitch, false);
      }

      // Player footsteps, landing & water wading splashes
      int feetBlock = w.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y + 0.1), (int)Math.floor(p.z));
      boolean inWater = Blocks.isWater(feetBlock);
      if (Double.isNaN(prevPlayerX)) {
         prevPlayerX = p.x;
         prevPlayerY = p.y;
         prevPlayerZ = p.z;
         prevPlayerVy = p.vy;
         fallStartY = p.y;
         wasOnGround = p.onGround;
         wasInWater = inWater;
         worldSettleTicks = 15;
         stepDistAcc = 0.0;
         return;
      }

      double dx = p.x - prevPlayerX;
      double dy = p.y - prevPlayerY;
      double dz = p.z - prevPlayerZ;
      double horiz = Math.hypot(dx, dz);

      // Teleport / respawn grace period
      if (horiz > 1.2 || Math.abs(dy) > 1.8) {
         prevPlayerX = p.x;
         prevPlayerY = p.y;
         prevPlayerZ = p.z;
         prevPlayerVy = p.vy;
         fallStartY = p.y;
         wasOnGround = p.onGround;
         wasInWater = inWater;
         worldSettleTicks = 12;
         stepDistAcc = 0.0;
         return;
      }

      if (worldSettleTicks > 0) {
         worldSettleTicks--;
         prevPlayerX = p.x;
         prevPlayerY = p.y;
         prevPlayerZ = p.z;
         prevPlayerVy = p.vy;
         fallStartY = p.y;
         wasOnGround = p.onGround;
         wasInWater = inWater;
         return;
      }

      if (!p.spectator && !p.dead && !p.flying) {
         if (inWater && !wasInWater && prevPlayerVy < -0.12) {
            play3D(w, "step_water_" + (1 + RND.nextInt(3)), p.x, p.y, p.z, 0.65F, 0.95F + RND.nextFloat() * 0.15F);
            stepCooldownTicks = 8;
         }

         if (!p.onGround) {
            if (wasOnGround || p.y > fallStartY) {
               fallStartY = p.y;
            }
         } else if (!wasOnGround) {
            double fallen = fallStartY - p.y;
            int groundBlock = w.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y - 0.2), (int)Math.floor(p.z));
            if (groundBlock == 0) {
               groundBlock = w.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y - 0.6), (int)Math.floor(p.z));
            }
            if ((fallen >= 0.20 || prevPlayerVy < -0.10) && groundBlock != 0 && !Blocks.isFluid(groundBlock)) {
               float landVol = (float)Math.min(1.0, 0.55 + Math.max(0.0, fallen) * 0.12);
               playFootstep(w, groundBlock, p.x, p.y, p.z, landVol);
               stepCooldownTicks = 6;
               stepDistAcc = 0.0;
            }
            fallStartY = p.y;
         }

         if ((p.onGround || inWater) && horiz > 0.015 && horiz < 1.2) {
            stepDistAcc += horiz;
            double stride = inWater ? 1.95 : (p.sneaking ? 1.65 : (p.sprinting ? 1.55 : 1.45));
            if (stepDistAcc >= stride && stepCooldownTicks == 0) {
               stepDistAcc = 0.0;
               if (inWater) {
                  play3D(w, "step_water_" + (1 + RND.nextInt(3)), p.x, p.y, p.z, 0.42F, 0.92F + RND.nextFloat() * 0.16F);
                  stepCooldownTicks = 10;
               } else {
                  int groundBlock = w.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y - 0.2), (int)Math.floor(p.z));
                  if (groundBlock == 0) {
                     groundBlock = w.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y - 0.6), (int)Math.floor(p.z));
                  }
                  if (groundBlock != 0 && !Blocks.isFluid(groundBlock)) {
                     playFootstep(w, groundBlock, p.x, p.y, p.z, p.sneaking ? 0.22F : 0.58F);
                     stepCooldownTicks = p.sprinting ? 6 : (p.sneaking ? 11 : 8);
                  }
               }
            }
         } else if (horiz < 0.005) {
            stepDistAcc = 0.0;
         }
      } else {
         stepDistAcc = 0.0;
         fallStartY = p.y;
      }

      wasInWater = inWater;
      wasOnGround = p.onGround;
      prevPlayerX = p.x;
      prevPlayerY = p.y;
      prevPlayerZ = p.z;
      prevPlayerVy = p.vy;

      // Subtle environmental ambience (only every 40 ticks = 2.0s, and never overlapping existing ambient clips)
      if ((ambientTick % 40) == 0) {
         int px = (int)Math.floor(p.x);
         int py = (int)Math.floor(p.y);
         int pz = (int)Math.floor(p.z);
         for (int i = 0; i < 5; i++) {
            int bx = px + RND.nextInt(13) - 6;
            int by = py + RND.nextInt(7) - 3;
            int bz = pz + RND.nextInt(13) - 6;
            int id = w.getBlock(bx, by, bz);
            if (id == 194 && !isSamplePlaying("fire_crackle")) { // Fire
               play3D(w, "fire_crackle", bx + 0.5, by + 0.5, bz + 0.5, 0.35F, 0.92F + RND.nextFloat() * 0.16F);
               break;
            } else if (Blocks.isLava(id) && !isSamplePlaying("lava_pop")) {
               play3D(w, "lava_pop", bx + 0.5, by + 0.5, bz + 0.5, 0.30F, 0.88F + RND.nextFloat() * 0.20F);
               break;
            } else if (Blocks.isWater(id) && Blocks.fluidLevel(id) > 0 && !isSamplePlaying("water_flow")) {
               play3D(w, "water_flow", bx + 0.5, by + 0.5, bz + 0.5, 0.25F, 0.94F + RND.nextFloat() * 0.12F);
               break;
            }
         }

         if (w.weather != null && w.dim == Dim.OVERWORLD && !isSamplePlaying("rain_loop")) {
            if (w.weather.rainingAt(px, py + 2, pz)) {
               double rx = p.x + (RND.nextDouble() - 0.5) * 4.0;
               double rz = p.z + (RND.nextDouble() - 0.5) * 4.0;
               play3D(w, "rain_loop", rx, p.y + 2.0, rz, 0.22F, 0.96F + RND.nextFloat() * 0.08F);
            }
         }
      }
   }

   // ====================================================================================================
   // REAL-TIME 44.1 kHz STEREO PHYSICAL ACOUSTICS & ANIMAL CROSSING MUSIC MIXER THREAD
   // ====================================================================================================

   private static synchronized void startAudioThread() {
      if (audioThreadStarted) {
         return;
      }
      audioThreadStarted = true;
      Thread t = new Thread(SoundEngine::audioLoop, "Blockcraft-Audio-DSP");
      t.setDaemon(true);
      t.setPriority(Thread.MAX_PRIORITY - 1);
      t.start();
   }

   // Animal Crossing cozy major-7th / add9 chord progressions (MIDI pitches)
   private static final int[][] AC_CHORDS = {
      {60, 64, 67, 71, 74}, // Cmaj9
      {57, 60, 64, 67, 72}, // Am9
      {53, 57, 60, 64, 69}, // Fmaj9
      {55, 59, 62, 65, 69}  // G13
   };

   private static void audioLoop() {
      SourceDataLine line = null;
      try {
         AudioFormat fmt = new AudioFormat(MIX_RATE, 16, 2, true, false);
         DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
         if (!AudioSystem.isLineSupported(info)) {
            return;
         }
         line = (SourceDataLine)AudioSystem.getLine(info);
         line.open(fmt, 4096 * 4);
         line.start();

         int frames = 512;
         byte[] outBytes = new byte[frames * 4];
         float[] mixL = new float[frames];
         float[] mixR = new float[frames];

         // Schroeder Reverb Comb & Allpass delay buffers
         float[] comb1L = new float[1557];
         float[] comb2L = new float[1617];
         float[] comb3R = new float[1491];
         float[] comb4R = new float[1422];
         float[] ap1L = new float[225];
         float[] ap2R = new float[556];
         float[] canyonBuf = new float[11025]; // 250 ms slapback echo
         int c1 = 0, c2 = 0, c3 = 0, c4 = 0, a1 = 0, a2 = 0, cEcho = 0;
         float masterLpL = 0.0F;
         float masterLpR = 0.0F;

         while (true) {
            if (!soundEnabled) {
               Thread.sleep(25L);
               continue;
            }
            java.util.Arrays.fill(mixL, 0.0F);
            java.util.Arrays.fill(mixR, 0.0F);

            // 1. Mix active 3D & UI sound effect voices
            float sVol = masterSoundVol;
            if (sVol > 0.001F) {
               for (Voice v : ACTIVE_VOICES) {
                  if (v.finished || v.sample == null) {
                     ACTIVE_VOICES.remove(v);
                     continue;
                  }
                  short[] pcm = v.sample.pcm;
                  int len = pcm.length;
                  float gL = v.gainL * sVol;
                  float gR = v.gainR * sVol;
                  float cut = v.lpCutoff;

                  for (int i = 0; i < frames; i++) {
                     int idxL = (int)(v.pos - v.itdL * v.step);
                     int idxR = (int)(v.pos - v.itdR * v.step);
                     if (idxL >= len && idxR >= len) {
                        if (v.loop) {
                           v.pos = 0.0;
                           idxL = 0;
                           idxR = 0;
                        } else {
                           v.finished = true;
                           break;
                        }
                     }
                     float sL = (idxL >= 0 && idxL < len) ? (pcm[idxL] / 32768.0F) : 0.0F;
                     float sR = (idxR >= 0 && idxR < len) ? (pcm[idxR] / 32768.0F) : 0.0F;

                     // Per-voice low-pass filter (wall occlusion + atmospheric air absorption + underwater)
                     v.lpStateL += cut * (sL - v.lpStateL);
                     v.lpStateR += cut * (sR - v.lpStateR);

                     mixL[i] += v.lpStateL * gL;
                     mixR[i] += v.lpStateR * gR;
                     v.pos += v.step;
                  }
                  if (v.finished) {
                     ACTIVE_VOICES.remove(v);
                  }
               }
            }

            // 2. Apply Real-Life 3D Room / Cave Schroeder Reverb & Canyon Echo to world sounds
            float wet = realAcoustics ? reverbWet : 0.0F;
            float fb = reverbFeedback;
            float echoWet = realAcoustics ? canyonEchoWet : 0.0F;
            if (wet > 0.005F || echoWet > 0.005F) {
               for (int i = 0; i < frames; i++) {
                  float inMono = (mixL[i] + mixR[i]) * 0.5F;

                  float o1 = comb1L[c1];
                  comb1L[c1] = inMono + o1 * fb;
                  if (++c1 >= comb1L.length) c1 = 0;

                  float o2 = comb2L[c2];
                  comb2L[c2] = inMono + o2 * fb;
                  if (++c2 >= comb2L.length) c2 = 0;

                  float o3 = comb3R[c3];
                  comb3R[c3] = inMono + o3 * fb;
                  if (++c3 >= comb3R.length) c3 = 0;

                  float o4 = comb4R[c4];
                  comb4R[c4] = inMono + o4 * fb;
                  if (++c4 >= comb4R.length) c4 = 0;

                  float revL = (o1 + o2) * 0.5F;
                  float revR = (o3 + o4) * 0.5F;

                  float apOutL = -revL + ap1L[a1];
                  ap1L[a1] = revL + apOutL * 0.5F;
                  if (++a1 >= ap1L.length) a1 = 0;

                  float apOutR = -revR + ap2R[a2];
                  ap2R[a2] = revR + apOutR * 0.5F;
                  if (++a2 >= ap2R.length) a2 = 0;

                  float echo = canyonBuf[cEcho];
                  canyonBuf[cEcho] = inMono + echo * 0.35F;
                  if (++cEcho >= canyonBuf.length) cEcho = 0;

                  mixL[i] += apOutL * wet + echo * echoWet;
                  mixR[i] += apOutR * wet + echo * echoWet;
               }
            }

            // 3. Apply Underwater Hydrophone Low-Pass Filter to 3D World Sounds BEFORE mixing 2D Music
            float masterCut = listenerUnderwater ? 0.08F : 1.0F;
            for (int i = 0; i < frames; i++) {
               masterLpL += masterCut * (mixL[i] - masterLpL);
               masterLpR += masterCut * (mixR[i] - masterLpR);
               mixL[i] = masterLpL;
               mixR[i] = masterLpR;
            }

            // 4. Mix Minecraft Legacy Console Edition Music Stream (single active track at a time, rotating Battle themes)
            float mVol = masterMusicVol;
            if (mVol > 0.001F) {
               String activeKey;
               float fade;
               synchronized (MUSIC_LOCK) {
                  if (!currentMusicKey.equals(targetMusicKey)) {
                     musicFade = Math.max(0.0F, musicFade - 0.025F);
                     if (musicFade <= 0.001F || currentMusicKey.isEmpty()) {
                        currentMusicKey = targetMusicKey;
                        musicPos = 0.0;
                        musicPauseSamples = 0;
                        musicFade = 1.0F;
                     }
                  } else if (musicFade < 1.0F) {
                     musicFade = Math.min(1.0F, musicFade + 0.025F);
                  }
                  activeKey = currentMusicKey;
                  fade = musicFade;
               }

               Sample mus = activeKey.isEmpty() ? null : BANK.get(activeKey);
               if (mus != null && mus.pcm.length > 0) {
                  short[] mpcm = mus.pcm;
                  int mlen = mpcm.length;
                  double mStep = mus.sampleRate / (double)MIX_RATE;
                  float effVol = mVol * fade * 0.68F;

                  for (int i = 0; i < frames; i++) {
                     if (musicPauseSamples > 0) {
                        musicPauseSamples--;
                        continue;
                     }
                     int idx0 = (int)musicPos;
                     if (idx0 >= mlen) {
                        synchronized (MUSIC_LOCK) {
                           if (!activeKey.equals(currentMusicKey)) {
                              break;
                           }
                           musicPos = 0.0;
                           if (activeKey.startsWith("lce_battle_") || activeKey.startsWith("lce_tumble_")) {
                              // Advance to the next Gareth Coker Tumble / Battle Mini Game theme!
                              battleTrackIndex = (battleTrackIndex + 1) % BATTLE_TRACKS.length;
                              musicPauseSamples = MIX_RATE * 2;
                           } else if (activeKey.startsWith("lce_menu")) {
                              // Rotate through C418 Legacy Console Edition Menu themes (Moog City 2, Mutation, Floating Trees)
                              menuTrackIndex = (menuTrackIndex + 1) % MENU_TRACKS.length;
                              activeRandomMenuTrack = MENU_TRACKS[menuTrackIndex];
                              musicPauseSamples = MIX_RATE * 2;
                           } else if (activeKey.equals("lce_credits")) {
                              musicPauseSamples = MIX_RATE * 2;
                           } else if (activeKey.startsWith("lce_nether")) {
                              battleTrackIndex = (battleTrackIndex + 1) % NETHER_TRACKS.length;
                              musicPauseSamples = MIX_RATE * 4;
                           } else {
                              // Pick a random Overworld track next (just like real Minecraft!)
                              String next = OVERWORLD_TRACKS[RND.nextInt(OVERWORLD_TRACKS.length)];
                              if (next.equals(activeKey)) {
                                 next = OVERWORLD_TRACKS[(RND.nextInt(OVERWORLD_TRACKS.length - 1) + 1) % OVERWORLD_TRACKS.length];
                              }
                              activeRandomOverworldTrack = next;
                              if (constantMusic) {
                                 musicPauseSamples = MIX_RATE * 2; // Constant Music ON: play continuously with a 2s gap
                              } else {
                                 // Real Minecraft random pause between songs (3 to 7 minutes of natural world ambience)
                                 musicPauseSamples = MIX_RATE * (180 + RND.nextInt(240));
                              }
                           }
                        }
                        break;
                     }
                     int idx1 = Math.min(mlen - 1, idx0 + 1);
                     float frac = (float)(musicPos - idx0);
                     float s = (mpcm[idx0] * (1.0F - frac) + mpcm[idx1] * frac) / 32768.0F;

                     float musSample = s * effVol;
                     mixL[i] += musSample;
                     mixR[i] += musSample;
                     musicPos += mStep;
                  }
               }
            }

            // 5. Soft Limiter -> 16-bit Stereo Little-Endian PCM
            int bi = 0;
            for (int i = 0; i < frames; i++) {
               float l = Math.max(-0.98F, Math.min(0.98F, mixL[i]));
               float r = Math.max(-0.98F, Math.min(0.98F, mixR[i]));
               short sl = (short)(l * 32767.0F);
               short sr = (short)(r * 32767.0F);
               outBytes[bi++] = (byte)(sl & 0xFF);
               outBytes[bi++] = (byte)((sl >>> 8) & 0xFF);
               outBytes[bi++] = (byte)(sr & 0xFF);
               outBytes[bi++] = (byte)((sr >>> 8) & 0xFF);
            }
            try {
               line.write(outBytes, 0, outBytes.length);
            } catch (Throwable ignored) {
               Thread.sleep(25L);
            }
         }
      } catch (Throwable ignored) {
      } finally {
         if (line != null) {
            try {
               line.close();
            } catch (Throwable ignored) {
            }
         }
      }
   }
   public static String currentMusicName() {
      return currentMusicKey != null ? currentMusicKey : "lce_menu";
   }
}
