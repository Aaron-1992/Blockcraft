package bc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Update 3.9 - player skins. Minecraft skins are plain 64x64 (or old 64x32, or HD 128x128 ...) PNG files and work as they are:
 * <ul>
 *   <li>drag a skin .png onto the game window (anywhere), or use "Import skin file..." / the clipboard / "Import by username"
 *       on the Skins screen; the file is copied into {@code %USERPROFILE%\.blockcraft112\skins};</li>
 *   <li>old 64x32 skins are converted (the left limbs are the mirrored right limbs), HD skins are scaled down;</li>
 *   <li>classic (4 px) or slim (3 px, "Alex") arms: detected from the image (the unused pixel column of slim arms is transparent), and
 *       switchable by hand;</li>
 *   <li>no skin chosen = a default skin that is drawn in code (no Mojang assets).</li>
 * </ul>
 * Everything here is free of OpenGL so it can be tested headless; {@link PlayerModel} draws the result.
 */
public final class Skins {
   public static final int SIZE = 64;
   public static final String DEFAULT = "(default)";
   static final int MAX_BYTES = 2_000_000;
   final File dir;
   final File metaFile;
   final Properties meta = new Properties();
   private final HashMap<String, int[]> cache = new HashMap<>();
   private final HashMap<String, Long> cacheStamp = new HashMap<>();
   /** import-by-username progress, polled by the Skins screen */
   public volatile String status = "";
   public volatile boolean busy;
   /** name of the skin the last successful import produced (the screen selects it) */
   public volatile String imported;

   public Skins(File root) {
      this.dir = new File(root, "skins");
      this.dir.mkdirs();
      this.metaFile = new File(this.dir, "skins.properties");
      if (this.metaFile.exists()) {
         try (FileInputStream in = new FileInputStream(this.metaFile)) {
            this.meta.load(in);
         } catch (IOException e) {
            System.err.println("skins.properties: " + e);
         }
      }
   }

   // ------------------------------------------------------------------ listing, loading
   /** File names (x.png) of all skins in the folder, sorted. */
   public List<String> list() {
      ArrayList<String> out = new ArrayList<>();
      File[] files = this.dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
      if (files != null) {
         for (File f : files) {
            if (f.isFile() && f.length() > 0 && f.length() <= MAX_BYTES) {
               out.add(f.getName());
            }
         }
      }

      out.sort(String.CASE_INSENSITIVE_ORDER);
      return out;
   }

   public File file(String name) {
      return new File(this.dir, name);
   }

   /** The 64x64 ARGB pixels of a skin by name ({@link #DEFAULT} or a file in the folder); never null (falls back to the default skin). */
   public synchronized int[] pixels(String name) {
      if (name == null || name.equals(DEFAULT)) {
         return defaultPixels();
      }

      File f = this.file(safeName(name));
      long stamp = f.lastModified() * 31 + f.length();
      Long old = this.cacheStamp.get(name);
      if (old != null && old == stamp && this.cache.containsKey(name)) {
         return this.cache.get(name);
      }

      try {
         int[] px = load(f);
         this.cache.put(name, px);
         this.cacheStamp.put(name, stamp);
         return px;
      } catch (Exception e) {
         System.err.println("skin " + name + ": " + e);
         return defaultPixels();
      }
   }

   private static int[] defaultCache;

   public static synchronized int[] defaultPixels() {
      if (defaultCache == null) {
         defaultCache = defaultSkin();
      }

      return defaultCache;
   }

   /** Reads a skin PNG: 64x64, old 64x32 (converted), or HD (a square multiple of 64, scaled down). Throws if it is not a skin. */
   public static int[] load(File f) throws IOException {
      if (!f.isFile() || f.length() > MAX_BYTES) {
         throw new IOException("not a skin file (missing or larger than 2 MB)");
      }

      BufferedImage img = ImageIO.read(f);
      if (img == null) {
         throw new IOException("not a PNG image");
      }

      return fromImage(img);
   }

   public static int[] fromImage(BufferedImage img) throws IOException {
      int w = img.getWidth();
      int h = img.getHeight();
      boolean legacy = w == h * 2;
      if (w < 64 || w % 64 != 0 || !(h == w || legacy)) {
         throw new IOException("not a Minecraft skin: expected 64x64 or 64x32 pixels (or a multiple of that), this is " + w + "x" + h);
      }

      int k = w / 64;
      int rows = legacy ? 32 : 64;
      int[] px = new int[SIZE * SIZE];
      for (int y = 0; y < rows; y++) {
         for (int x = 0; x < SIZE; x++) {
            // HD skins: take the middle pixel of every k x k block
            px[y * SIZE + x] = img.getRGB(x * k + k / 2, y * k + k / 2);
         }
      }

      if (legacy) {
         legacyToModern(px);
      }

      return px;
   }

   /**
    * 64x32 skins have no left arm / left leg: the game used the right ones mirrored. Fills the missing 64x64 regions the same way:
    * every face of the left limb is the mirror image of the matching face of the right limb, and the two side faces swap places.
    */
   public static void legacyToModern(int[] px) {
      mirrorLimb(px, 0, 16, 16, 48); // right leg -> left leg
      mirrorLimb(px, 40, 16, 32, 48); // right arm -> left arm
   }

   private static void mirrorLimb(int[] px, int sx, int sy, int dx, int dy) {
      // limb block: 16 wide x 16 tall = [right 4][front 4][left 4][back 4] x [top/bottom 4 + body 12]; faces of the 4x12x4 limb
      final int d = 4;
      final int w = 4;
      final int h = 12;
      // body faces: right (0,d, d,h), front (d,d, w,h), left (d+w,d, d,h), back (d+w+d,d, w,h); top (d,0, w,d), bottom (d+w,0, w,d)
      int[][] src = {{0, d, d, h}, {d, d, w, h}, {d + w, d, d, h}, {d + w + d, d, w, h}, {d, 0, w, d}, {d + w, 0, w, d}};
      int[][] dst = {{d + w, d, d, h}, {d, d, w, h}, {0, d, d, h}, {d + w + d, d, w, h}, {d, 0, w, d}, {d + w, 0, w, d}};
      for (int i = 0; i < 6; i++) {
         for (int y = 0; y < src[i][3]; y++) {
            for (int x = 0; x < src[i][2]; x++) {
               int fromX = sx + src[i][0] + (src[i][2] - 1 - x);
               int fromY = sy + src[i][1] + y;
               int toX = dx + dst[i][0] + x;
               int toY = dy + dst[i][1] + y;
               px[toY * SIZE + toX] = px[fromY * SIZE + fromX];
            }
         }
      }
   }

   /** Slim ("Alex") arms leave one pixel column of every arm face unused; if that column is empty the skin is slim. */
   public static boolean looksSlim(int[] px) {
      for (int y = 20; y < 32; y++) {
         for (int x = 54; x < 56; x++) {
            if (px[y * SIZE + x] >>> 24 != 0) {
               return false;
            }
         }
      }

      for (int y = 52; y < 64; y++) {
         for (int x = 46; x < 48; x++) {
            if (px[y * SIZE + x] >>> 24 != 0) {
               return false;
            }
         }
      }

      return true;
   }

   // ------------------------------------------------------------------ slim / classic per skin
   public synchronized boolean slim(String name) {
      if (name == null || name.equals(DEFAULT)) {
         return false;
      }

      String v = this.meta.getProperty(name + ".slim");
      if (v != null) {
         return Boolean.parseBoolean(v);
      }

      return looksSlim(this.pixels(name));
   }

   public synchronized void setSlim(String name, boolean slim) {
      if (name == null || name.equals(DEFAULT)) {
         return;
      }

      this.meta.setProperty(name + ".slim", Boolean.toString(slim));
      this.saveMeta();
   }

   private void saveMeta() {
      try (FileOutputStream out = new FileOutputStream(this.metaFile)) {
         this.meta.store(out, "Blockcraft skins: <file>.slim = slim (3 px) arms");
      } catch (IOException e) {
         System.err.println("skins.properties: " + e);
      }
   }

   // ------------------------------------------------------------------ importing
   /** A safe file name: letters, digits, '_' and '-', at most 40 characters, always ending in .png. */
   public static String safeName(String name) {
      String base = name == null ? "" : name;
      int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
      if (slash >= 0) {
         base = base.substring(slash + 1);
      }

      if (base.toLowerCase().endsWith(".png")) {
         base = base.substring(0, base.length() - 4);
      }

      StringBuilder sb = new StringBuilder();
      for (char c : base.toCharArray()) {
         if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
            sb.append(c);
         } else if (c == ' ' || c == '.') {
            sb.append('_');
         }
      }

      if (sb.length() == 0) {
         sb.append("skin");
      }

      if (sb.length() > 40) {
         sb.setLength(40);
      }

      return sb.append(".png").toString();
   }

   private File freeFile(String wanted) {
      String name = safeName(wanted);
      File f = new File(this.dir, name);
      int n = 2;
      while (f.exists()) {
         f = new File(this.dir, name.substring(0, name.length() - 4) + "-" + n++ + ".png");
      }

      return f;
   }

   /** Copies a skin PNG from anywhere into the skins folder (after checking that it really is a skin). Returns the new file name. */
   public synchronized String importFile(File src) throws IOException {
      int[] px = load(src); // throws for anything that is not a skin
      File dst = src.getParentFile() != null && src.getParentFile().getCanonicalFile().equals(this.dir.getCanonicalFile()) ? src : this.freeFile(src.getName());
      if (dst != src) {
         // store the normal 64x64 form (HD and 64x32 skins are converted once, here)
         BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
         out.setRGB(0, 0, SIZE, SIZE, px, 0, SIZE);
         if (!ImageIO.write(out, "png", dst)) {
            throw new IOException("could not write " + dst);
         }
      }

      this.cache.remove(dst.getName());
      return dst.getName();
   }

   /** Imports the skin of a Minecraft account by username (asks Mojang's servers; runs in the background, see status / imported / busy). */
   public void importUser(String username) {
      final String name = username == null ? "" : username.trim();
      if (this.busy) {
         return;
      }

      if (!name.matches("[A-Za-z0-9_]{1,16}") && uuidOf(name) == null) {
         this.status = "A Minecraft username has 1-16 letters, digits or _";
         return;
      }

      this.busy = true;
      this.imported = null;
      this.status = "Looking up " + name + " ...";
      Thread t = new Thread(() -> {
         try {
            String got = this.downloadUser(name);
            this.imported = got;
            this.status = "Imported " + got;
         } catch (Exception e) {
            this.status = "Could not import: " + e.getMessage();
         } finally {
            this.busy = false;
         }
      }, "skin-import");
      t.setDaemon(true);
      t.start();
   }

   /** Imports a skin from a link to a PNG (runs in the background like {@link #importUser}). */
   public void importUrl(String url) {
      if (this.busy) {
         return;
      }

      if (!url.matches("https?://[^\\s\"<>]{4,300}")) {
         this.status = "That does not look like a link";
         return;
      }

      this.busy = true;
      this.imported = null;
      this.status = "Downloading ...";
      Thread t = new Thread(() -> {
         File tmp = null;
         try {
            byte[] png = fetch(client(), url);
            if (png == null) {
               throw new IOException("nothing there (404)");
            }

            String base = url.substring(url.lastIndexOf('/') + 1);
            tmp = new File(Files.createTempDirectory("skin").toFile(), safeName(base.isEmpty() ? "skin" : base));
            Files.write(tmp.toPath(), png);
            String got = this.importFile(tmp);
            this.imported = got;
            this.status = "Imported " + got;
         } catch (Exception e) {
            this.status = "Could not import: " + e.getMessage();
         } finally {
            if (tmp != null) {
               tmp.delete();
               tmp.getParentFile().delete();
            }

            this.busy = false;
         }
      }, "skin-import");
      t.setDaemon(true);
      t.start();
   }

   /**
    * Whatever was pasted: a path to a PNG (Windows' "Copy as path" puts it in quotes), a link to a PNG, or a Minecraft username.
    * Returns the file if it was a path (the caller imports it) or null when something was started in the background / rejected.
    */
   public File importFromText(String text) {
      String t = text == null ? "" : text.trim();
      if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
         t = t.substring(1, t.length() - 1);
      }

      if (t.isEmpty()) {
         this.status = "The clipboard is empty";
         return null;
      }

      File f = new File(t);
      if (f.isFile()) {
         return f;
      }

      if (t.startsWith("http://") || t.startsWith("https://")) {
         this.importUrl(t);
      } else {
         this.importUser(t);
      }

      return null;
   }

   static HttpClient client() {
      return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
   }

   static byte[] fetch(HttpClient c, String url) throws IOException, InterruptedException {
      HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).header("User-Agent", "Blockcraft-skin-import").GET().build();
      HttpResponse<byte[]> r = c.send(rq, HttpResponse.BodyHandlers.ofByteArray());
      if (r.statusCode() == 404 || r.statusCode() == 204) {
         return null;
      }

      if (r.statusCode() != 200) {
         throw new IOException("the server answered " + r.statusCode());
      }

      if (r.body().length > MAX_BYTES) {
         throw new IOException("answer too large");
      }

      return r.body();
   }

   String downloadUser(String name) throws Exception {
      HttpClient c = client();
      String id = uuidOf(name);
      if (id == null) {
         byte[] prof = fetch(c, "https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name);
         if (prof == null) {
            prof = fetch(c, "https://api.mojang.com/users/profiles/minecraft/" + name);
         }

         if (prof == null) {
            throw new IOException("no Minecraft account called " + name);
         }

         id = profileId(new String(prof, StandardCharsets.UTF_8));
         if (id == null) {
            throw new IOException("unexpected answer from the profile server");
         }
      }

      this.status = "Downloading the skin of " + name + " ...";
      byte[] session = fetch(c, "https://sessionserver.mojang.com/session/minecraft/profile/" + id);
      if (session == null) {
         throw new IOException("the skin server has no profile for " + name);
      }

      String sessionJson = new String(session, StandardCharsets.UTF_8);
      String[] tex = textureInfo(sessionJson);
      if (tex == null) {
         throw new IOException(name + " has no custom skin");
      }

      String account = profileName(sessionJson);
      final String fileName = account != null ? account : (uuidOf(name) != null ? "account" : name);
      byte[] png = fetch(c, tex[0].replaceFirst("^http:", "https:"));
      if (png == null) {
         throw new IOException("the skin image is gone");
      }

      File tmp = Files.createTempFile("skin", ".png").toFile();
      try {
         Files.write(tmp.toPath(), png);
         String got;
         synchronized (this) {
            // keep the account's name as the file name
            File dst = this.freeFile(fileName);
            int[] px = load(tmp);
            BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            out.setRGB(0, 0, SIZE, SIZE, px, 0, SIZE);
            ImageIO.write(out, "png", dst);
            got = dst.getName();
            this.meta.setProperty(got + ".slim", Boolean.toString("slim".equals(tex[1])));
            this.saveMeta();
         }

         return got;
      } finally {
         tmp.delete();
      }
   }

   // ------------------------------------------------------------------ Mojang JSON (tiny extraction, no JSON library)
   static final Pattern P_ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
   static final Pattern P_VALUE = Pattern.compile("\"name\"\\s*:\\s*\"textures\"\\s*,\\s*\"value\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
   static final Pattern P_VALUE2 = Pattern.compile("\"value\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
   static final Pattern P_SKIN = Pattern.compile("\"SKIN\"\\s*:\\s*\\{\\s*\"url\"\\s*:\\s*\"(https?://[^\"]+)\"(?:\\s*,\\s*\"metadata\"\\s*:\\s*\\{\\s*\"model\"\\s*:\\s*\"([a-z]+)\")?");

   static final Pattern P_PROFILE_NAME = Pattern.compile("\"id\"\\s*:\\s*\"[0-9a-fA-F]{32}\"\\s*,\\s*\"name\"\\s*:\\s*\"([A-Za-z0-9_]{1,16})\"");

   /** An account id written as 32 hex digits or the dashed 8-4-4-4-12 form, as 32 lower-case hex digits; null for anything else. */
   static String uuidOf(String text) {
      if (text == null) {
         return null;
      }

      String t = text.trim();
      if (t.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
         t = t.replace("-", "");
      }

      return t.matches("[0-9a-fA-F]{32}") ? t.toLowerCase() : null;
   }

   /** The account name in a session-server answer ({"id":"...","name":"...",...}), or null. */
   static String profileName(String json) {
      Matcher m = P_PROFILE_NAME.matcher(json);
      return m.find() ? m.group(1) : null;
   }

   /** The account id in a profile answer ({"id":"...","name":"..."}), or null. */
   static String profileId(String json) {
      Matcher m = P_ID.matcher(json);
      return m.find() ? m.group(1).toLowerCase() : null;
   }

   /** {skin url, model ("slim" or "default")} from a session-server profile answer, or null if the account has no custom skin. */
   static String[] textureInfo(String profileJson) {
      Matcher m = P_VALUE.matcher(profileJson);
      if (!m.find()) {
         m = P_VALUE2.matcher(profileJson);
         if (!m.find()) {
            return null;
         }
      }

      String decoded;
      try {
         decoded = new String(Base64.getDecoder().decode(m.group(1)), StandardCharsets.UTF_8);
      } catch (IllegalArgumentException e) {
         return null;
      }

      Matcher s = P_SKIN.matcher(decoded);
      if (!s.find()) {
         return null;
      }

      return new String[]{s.group(1), s.group(2) == null ? "default" : s.group(2)};
   }

   // ------------------------------------------------------------------ the default skin (drawn in code, an original design)
   static void fill(int[] px, int x0, int y0, int w, int h, int argb) {
      for (int y = y0; y < y0 + h; y++) {
         for (int x = x0; x < x0 + w; x++) {
            px[y * SIZE + x] = argb;
         }
      }
   }

   static int shade(int argb, int d) {
      int r = Math.max(0, Math.min(255, (argb >> 16 & 255) + d));
      int g = Math.max(0, Math.min(255, (argb >> 8 & 255) + d));
      int b = Math.max(0, Math.min(255, (argb & 255) + d));
      return argb & 0xFF000000 | r << 16 | g << 8 | b;
   }

   /** Paints all six faces of a skin box (u, v origin, w x h x d) with one colour plus a little noise. */
   static void paintBox(int[] px, int u, int v, int w, int h, int d, int top, int bottom, int side, Random rnd) {
      int[][] reg = {{u + d, v, w, d, top}, {u + d + w, v, w, d, bottom}, {u, v + d, d, h, side}, {u + d, v + d, w, h, side}, {u + d + w, v + d, d, h, side}, {u + d + w + d, v + d, w, h, side}};
      for (int[] r : reg) {
         for (int y = 0; y < r[3]; y++) {
            for (int x = 0; x < r[2]; x++) {
               px[(r[1] + y) * SIZE + r[0] + x] = shade(r[4], rnd.nextInt(9) - 4);
            }
         }
      }
   }

   static int[] defaultSkin() {
      int[] px = new int[SIZE * SIZE];
      Random rnd = new Random(7);
      int skin = 0xFFD9A47E;
      int hair = 0xFF5A3A22;
      int shirt = 0xFF3F8F5A;
      int sleeve = 0xFF36794D;
      int pants = 0xFF3A4A86;
      int shoe = 0xFF4A4A4F;
      // head (0,0) 8x8x8: skin everywhere, hair on top / back / sides' upper part
      paintBox(px, 0, 0, 8, 8, 8, hair, skin, skin, rnd);
      for (int x = 8; x < 16; x++) { // front: hair line and fringe
         for (int y = 8; y < 10; y++) {
            px[y * SIZE + x] = shade(hair, rnd.nextInt(7) - 3);
         }
      }

      for (int y = 8; y < 12; y++) { // sides and back: hair down to the ears
         for (int x : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31}) {
            px[y * SIZE + x] = shade(hair, rnd.nextInt(7) - 3);
         }
      }

      // face: eyes (white + blue), nose, mouth
      px[12 * SIZE + 9] = 0xFFFFFFFF;
      px[12 * SIZE + 10] = 0xFF2E4F9E;
      px[12 * SIZE + 13] = 0xFF2E4F9E;
      px[12 * SIZE + 14] = 0xFFFFFFFF;
      px[13 * SIZE + 11] = shade(skin, -28);
      px[13 * SIZE + 12] = shade(skin, -28);
      for (int x = 10; x <= 13; x++) {
         px[14 * SIZE + x] = 0xFF9C4B3F;
      }

      // body (16,16) 4x12... w8 h12 d4: shirt
      paintBox(px, 16, 16, 8, 12, 4, shirt, shade(shirt, -30), shirt, rnd);
      // right arm (40,16) 4x12x4: sleeve on the upper part, skin hand; left arm (32,48)
      for (int[] arm : new int[][]{{40, 16}, {32, 48}}) {
         paintBox(px, arm[0], arm[1], 4, 12, 4, sleeve, skin, skin, rnd);
         for (int[] face : new int[][]{{arm[0], arm[1] + 4}, {arm[0] + 4, arm[1] + 4}, {arm[0] + 8, arm[1] + 4}, {arm[0] + 12, arm[1] + 4}}) {
            for (int y = 0; y < 7; y++) {
               for (int x = 0; x < 4; x++) {
                  px[(face[1] + y) * SIZE + face[0] + x] = shade(sleeve, rnd.nextInt(7) - 3);
               }
            }
         }
      }

      // legs: right (0,16), left (16,48): trousers with shoes
      for (int[] leg : new int[][]{{0, 16}, {16, 48}}) {
         paintBox(px, leg[0], leg[1], 4, 12, 4, pants, shoe, pants, rnd);
         for (int[] face : new int[][]{{leg[0], leg[1] + 4}, {leg[0] + 4, leg[1] + 4}, {leg[0] + 8, leg[1] + 4}, {leg[0] + 12, leg[1] + 4}}) {
            for (int y = 9; y < 12; y++) {
               for (int x = 0; x < 4; x++) {
                  px[(face[1] + y) * SIZE + face[0] + x] = shade(shoe, rnd.nextInt(7) - 3);
               }
            }
         }
      }

      // the overlay layers (hat, jacket, sleeves, trousers) stay transparent
      return px;
   }

   /** A second, different skin for tests and screenshots: slim arms, a blonde head with a red cap and a vest on the second layer. */
   public static int[] demoSkin() {
      int[] px = defaultSkin().clone();
      Random rnd = new Random(11);
      int hair = 0xFFD8B44A;
      int shirt = 0xFF7B4FB8;
      int pants = 0xFF55555B;
      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 32; x++) {
            int c = px[y * SIZE + x];
            if ((c & 0xFFFFFF) == (0x5A3A22) || Math.abs((c >> 16 & 255) - 0x5A) < 8 && Math.abs((c >> 8 & 255) - 0x3A) < 8 && Math.abs((c & 255) - 0x22) < 8) {
               px[y * SIZE + x] = shade(hair, rnd.nextInt(7) - 3);
            }
         }
      }

      paintBox(px, 16, 16, 8, 12, 4, shirt, shade(shirt, -30), shirt, rnd);
      paintBox(px, 0, 16, 4, 12, 4, pants, shade(pants, -20), pants, rnd);
      paintBox(px, 16, 48, 4, 12, 4, pants, shade(pants, -20), pants, rnd);
      // hat layer (32,0): a red cap over the top of the head
      int red = 0xFFC2362F;
      for (int[] r : new int[][]{{40, 0, 8, 8}, {32, 8, 8, 3}, {40, 8, 8, 3}, {48, 8, 8, 3}, {56, 8, 8, 3}}) {
         fill(px, r[0], r[1], r[2], r[3], red);
      }

      // jacket layer (16,32): a dark vest on the body's front
      fill(px, 20, 36, 8, 12, 0xFF2B2B33);
      // slim arms: the unused fourth pixel column stays empty
      for (int y = 20; y < 32; y++) {
         px[y * SIZE + 54] = 0;
         px[y * SIZE + 55] = 0;
      }

      for (int y = 52; y < 64; y++) {
         px[y * SIZE + 46] = 0;
         px[y * SIZE + 47] = 0;
      }

      return px;
   }

   /** All skin names a player can pick: the default first, then the files. */
   public List<String> choices() {
      ArrayList<String> out = new ArrayList<>();
      out.add(DEFAULT);
      out.addAll(this.list());
      return out;
   }

   @Override
   public String toString() {
      return "Skins" + Arrays.toString(this.list().toArray());
   }
}
