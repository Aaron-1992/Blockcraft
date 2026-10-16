package bc;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static bc.GameTests.*;

/** Export the real default pixels, load them through both production loaders, edit and reload them. */
final class TestsTextureEditor {
   static void run() {
      test("texture editor: shipped and embedded guides stay identical", () -> {
         eq(java.nio.file.Files.readString(java.nio.file.Path.of("package/Texture-editing.txt")),
            java.nio.file.Files.readString(java.nio.file.Path.of("res/texture-editing.txt")),"CLI export and Windows shortcut use the same manual");
      });

      test("Aseprite: lossless native export, safe re-export, folder/ZIP load and animations", () -> {
         Path root = Files.createTempDirectory("blockcraft-texture-editor-");
         try {
            ResourcePacks.setDirForTest(root);
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
            int[] before = TexPixel32.atlas(Tex.atlasPixels);
            Path pack = root.resolve("MyTextures");
            TextureEditor.export(pack);
            try (var files = Files.list(pack.resolve("tiles"))) { eq(550, (int)files.count(), "all static art tiles exported"); }
            try (var files = Files.list(pack.resolve("animations"))) { eq(6, (int)files.count(), "all animated atlas slots exported"); }
            check(Files.isRegularFile(pack.resolve("index.html")), "searchable catalog exported");
            byte[] manifest = Files.readAllBytes(pack.resolve("TEXTURES.tsv"));
            boolean refused = false;
            try { TextureEditor.export(pack); } catch (FileAlreadyExistsException expected) { refused = true; }
            check(refused && Arrays.equals(manifest, Files.readAllBytes(pack.resolve("TEXTURES.tsv"))), "never overwrite an artist pack");
            ResourcePacks.refresh();
            ResourcePacks.select("MyTextures", null);
            check(TexPixel32.enabled(), "native custom pack uses 32px rendering and extrusion");
            int[] reloaded = TexPixel32.atlas(Tex.atlasPixels);
            check(Arrays.equals(before, reloaded), "every atlas pixel survives PNG export/import without blur, cropping or repaints");
            for (int tile : TextureEditor.ANIMATED) for (int frame = 0; frame < (tile == 77 ? 8 : 16); frame++) {
               int[] expected = TextureEditor.frame(tile, frame);
               int[] actual = tile == 77 ? BowSprite.renderPixels(frame % 4, frame >= 4)
                  : Tex.animationPixels(tile, new int[256], frame, 32);
               check(Arrays.equals(expected, actual), "animated native frame " + tile + "/" + frame);
            }
            Path zip = root.resolve("Zipped.zip");
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip)); var paths = Files.walk(pack)) {
               for (Path f : paths.filter(Files::isRegularFile).sorted().toList()) {
                  out.putNextEntry(new ZipEntry(pack.relativize(f).toString().replace('\\', '/')));
                  Files.copy(f, out); out.closeEntry();
               }
            }
            ResourcePacks.refresh(); ResourcePacks.select("Zipped", null);
            check(Arrays.equals(before, TexPixel32.atlas(Tex.atlasPixels)), "ZIP pack matches folder and built-in atlas");
            for (int tile : TextureEditor.ANIMATED)
               check(Arrays.equals(TextureEditor.frame(tile, 7), ResourcePacks.overridePixels(tile, 7, 32)), "ZIP animations " + tile);
            Path stone;
            try (var files = Files.list(pack.resolve("tiles"))) { stone = files.filter(p -> p.getFileName().toString().startsWith("0003_")).findFirst().orElseThrow(); }
            int[] edit = new int[1024]; Arrays.fill(edit, 0xFFDE3782); edit[31 * 32 + 31] = 0xFF123456; edit[1] = 0x80123456;
            TextureEditor.png(stone, edit, 32, 32);
            ResourcePacks.refresh(); ResourcePacks.select("MyTextures", null);
            check(Arrays.equals(edit, TexPixel32.tile(3, new int[256], false)), "native pixels and alpha are exact, including bottom-right pixel");
            eq(0xFFDE3782, Tex.atlasPixels[Tex.ty(3) * Tex.PX + Tex.tx(3)], "CPU/LOD fallback also sees edits");
            // Deliberate transparency is different from a missing override.
            TextureEditor.png(stone, new int[1024], 32, 32);
            ResourcePacks.refresh(); ResourcePacks.select("MyTextures", null);
            check(Arrays.equals(new int[1024], TexPixel32.tile(3, new int[256], false)), "fully transparent PNG overrides procedural art");
            Files.delete(stone);
            ResourcePacks.refresh(); ResourcePacks.select("MyTextures", null);
            int[] fallback = TexPixel32.tile(3, TexOverhaulV57.readTile(3), false);
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
            check(Arrays.equals(fallback, TexPixel32.tile(3, TexOverhaulV57.readTile(3), false)), "deleting an edit restores built-in fallback");
            TextureEditor.png(pack.resolve("tiles/0023_static_water.png"), edit, 32, 32);
            ResourcePacks.refresh(); ResourcePacks.select("MyTextures", null);
            check(Arrays.equals(edit, Tex.animationPixels(23, new int[256], 13, 32)), "static edit cannot be overwritten by animation tick");
            // Malformed dimensions are rejected, never truncated to the upper-left 16 pixels.
            TextureEditor.png(stone, new int[33 * 32], 33, 32);
            ResourcePacks.refresh(); ResourcePacks.select("MyTextures", null);
            check(ResourcePacks.overridePixels(3, 0, 32) == null, "invalid dimensions rejected");
            eq(-1, ResourcePacks.resolveTileName("2047_bad.png"), "out of actual atlas bounds rejected");
            eq(3, ResourcePacks.resolveTileName("0003_stone.png"), "descriptive numeric ID filenames");
            Path atlasPack = root.resolve("NativeAtlas"); Files.createDirectories(atlasPack.resolve("tiles"));
            Files.writeString(atlasPack.resolve("pack.properties"), "tileSize=32\nbase=Default\n");
            int[] highAtlas = new int[1024 * 1024];
            for (int y = 0; y < 32; y++) System.arraycopy(edit, y * 32, highAtlas, y * 1024 + 3 * 32, 32);
            TextureEditor.png(atlasPack.resolve("atlas.png"), highAtlas, 1024, 1024);
            ResourcePacks.refresh(); ResourcePacks.select("NativeAtlas", null);
            check(Arrays.equals(edit, TexPixel32.tile(3, new int[256], false)), "native 32px atlas imports exact tile grid");
            TextureEditor.png(atlasPack.resolve("tiles/0003_stone.png"), new int[1024], 32, 32);
            ResourcePacks.reload(null);
            check(Arrays.equals(new int[1024], TexPixel32.tile(3, new int[256], false)), "Reload PNGs keeps active pack and individual PNG overrides atlas");
            eq("NativeAtlas", ResourcePacks.activeId(), "reload retains chosen pack");
            Path legacy = root.resolve("Legacy"); Files.createDirectories(legacy.resolve("tiles"));
            int[] old = new int[256]; Arrays.fill(old, 0xFF765432);
            TextureEditor.png(legacy.resolve("tiles/3.png"), old, 16, 16);
            ResourcePacks.refresh(); ResourcePacks.select("Legacy", null);
            check(!TexPixel32.enabled(), "old pack stays 16px without explicit opt-in");
            eq(0xFF765432, Tex.atlasPixels[Tex.ty(3) * Tex.PX + Tex.tx(3)], "legacy 16px PNG still loads");
         } finally {
            ResourcePacks.setDirForTest(Path.of("resourcepacks"));
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
            try (var files = Files.walk(root)) { for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
         }
      });
   }
}
