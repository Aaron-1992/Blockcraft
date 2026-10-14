package bc;

import static bc.GameTests.check;
import static bc.GameTests.test;
import java.util.Arrays;
import java.util.HashSet;

/** CPU regressions for the HD atlas and shared first/third-person weapon math. */
final class TestsVisualRefresh {
   static void run() {
      test("pixel materials: all atlas cells, alpha masks, deterministic detail and mip chains", () -> {
         String old = ResourcePacks.activeId();
         try {
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
            Tex.initPixels();
            int[] hd = TexPixel32.atlas(Tex.atlasPixels);
            int width = Tex.COLS * TexPixel32.SIZE;
            check(hd.length == width * width, "1024x1024 default atlas");
            for (int tile = 0; tile < Tex.COLS * Tex.COLS; tile++) {
               int[] src = TexOverhaulV57.readTile(tile);
               int[] result = tile == Items.tile[Items.BOW] ? BowSprite.pixels(0, false) : TexPixel32.tile(tile, src, tile == Tex.WATER || tile == Tex.LAVA || tile == 235 || tile == TexArt312.PORTAL);
               for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
                  int c = result[y * 32 + x];
                  if (tile != Items.tile[Items.BOW] && !TexRemaining32.handles(tile)) check((c >>> 24) == (src[y / 2 * 16 + x / 2] >>> 24), "same silhouette/opacity " + tile);
                  check(c == hd[((tile / Tex.COLS) * 32 + y) * width + (tile % Tex.COLS) * 32 + x], "tile isolation " + tile);
               }
            }
            int[] stone = TexPixel32.tile(Tex.STONE, TexOverhaulV57.readTile(Tex.STONE), false);
            check(Arrays.equals(stone, TexPixel32.tile(Tex.STONE, TexOverhaulV57.readTile(Tex.STONE), false)), "deterministic");
            HashSet<Integer> shades = new HashSet<>();
            int largestStep = 0;
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
               shades.add(stone[y * 32 + x]);
               largestStep = Math.max(largestStep, Math.abs((stone[y * 32 + x] & 255) - (stone[y * 32 + (x + 1) % 32] & 255)));
            }
            check(shades.size() >= 5 && shades.size() <= 32 && largestStep < 65, "small stepped mineral palette, not thousands of interpolated shades");
            for (int size = 32; size > 2; size /= 2) {
               stone = Tex.downsampleTile(stone, size);
               check(stone.length == size * size / 4, "complete HD mip level");
               for (int c : stone) check((c >>> 24) == 255, "opaque stone stays opaque at distance");
            }
            ResourcePacks.select(ResourcePacks.ID_CLASSIC, null);
            check(!TexPixel32.enabled(), "classic retains its original 16px art");
            ResourcePacks.select(ResourcePacks.ID_V56_PAINTED, null);
            check(!TexPixel32.enabled(), "legacy painted pack is unchanged by HD reconstruction");
            ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
            check(TexPixel32.enabled(), "default enables HD again on reload");
         } finally { ResourcePacks.select(old, null); }
      });
      test("pixel materials: transparent neighbours cannot add black fringes or change white utility texel", () -> {
         int[] src = new int[256];
         src[8 * 16 + 8] = 0xFFCCAA88;
         int[] hd = TexPixel32.tile(999, src, true);
         for (int y = 16; y < 18; y++) for (int x = 16; x < 18; x++) {
            check(hd[y * 32 + x] == src[136], "alpha-normalised colour at silhouette");
         }
         Arrays.fill(src, -1);
         for (int c : TexPixel32.tile(Tex.WHITE, src, false)) check(c == -1, "white stays white");
         for (int i = 0; i < 256; i++) src[i] = (i & 1) == 0 ? 0xFFFF0000 : 0xFF0000FF;
         for (int c : TexPixel32.tile(999, src, false)) check(c == 0xFFFF0000 || c == 0xFF0000FF, "no purple interpolated texels at red/blue boundary");
      });
      test("complete pixel pack: approved 6.3 artwork is unchanged; every remaining cell has native art", () -> {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
         Tex.initPixels();
         java.util.Properties approved = new java.util.Properties();
         try (java.io.InputStream in = new java.io.FileInputStream("test/pixel32-approved.properties")) {
            approved.load(in);
         } catch (java.io.IOException e) { throw new RuntimeException(e); }
         int protectedCount=0, redrawnCount=0;
         for(int t=0;t<1024;t++) {
            int[] src=TexOverhaulV57.readTile(t);
            boolean populated=false;
            for(int c:src) if((c>>>24)!=0) populated=true;
            if(!populated) continue;
            int[] art=t==Items.tile[Items.BOW]?BowSprite.pixels(0,false):TexPixel32.tile(t,src,TexRemaining32.animated(t));
            String golden=approved.getProperty(Integer.toString(t));
            if(golden!=null) {
               check(Arrays.hashCode(art)==Integer.parseInt(golden), "approved pixels unchanged " + t);
               protectedCount++;
            } else if(t!=Tex.WHITE) {
               check(TexRemaining32.handles(t), "no silent legacy fallback for populated tile " + t);
               check(Arrays.equals(art,TexRemaining32.paint(t)), "native dispatch " + t);
               boolean changed=false, nativeDetail=false;
               for(int y=0;y<32;y++) for(int x=0;x<32;x++) {
                  int c=art[y*32+x];
                  changed |= c!=src[(y/2)*16+x/2];
                  nativeDetail |= c!=art[(y&~1)*32+(x&~1)];
                  check((c>>>24)!=0 || c==0,"no invisible coloured fringe " + t);
               }
               check(changed,"not the old 16px art enlarged " + t);
               check(nativeDetail,"uses native 32px detail " + t);
               redrawnCount++;
            }
         }
         check(protectedCount==approved.size(), "all approved fixtures exercised");
         check(redrawnCount>300, "coverage includes items, model-only faces and mod bottle");
         for(int t:new int[]{23,24,235,263}) {
            check(!Arrays.equals(TexRemaining32.frame(t,0),TexRemaining32.frame(t,3)),"animation moves " + t);
            check(Arrays.equals(TexRemaining32.frame(t,0),TexRemaining32.frame(t,16)),"animation loops exactly " + t);
         }
         check(!Arrays.equals(TexRemaining32.paint(196),TexRemaining32.paint(218)),"flint and steel is not shears");
         for(int t:new int[]{2,9,550,551,552,642,630,634,639,640,641}) for(int c:TexRemaining32.paint(t)) {
            check((c>>16&255)==(c>>8&255)&&(c>>8&255)==(c&255),"biome/dye tint masks stay neutral " + t);
         }
      });
      test("6.5 full redraw: all rejected cells differ from 6.4; icons have new silhouettes", () -> {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);
         Tex.initPixels();
         int changed=0, items=0, newSilhouettes=0;
         for(int t=0;t<1024;t++) {
            boolean populated=false;
            for(int c:TexOverhaulV57.readTile(t))if((c>>>24)!=0)populated=true;
            if(!populated||!TexRemaining32.handles(t))continue;
            int[] art=TexRemaining32.paint(t), old=TexRemaining32.previous(t);
            check(!Arrays.equals(art,old),"not the rejected 6.4 artwork "+t);
            check(Arrays.equals(art,TexRemaining32.paint(t)),"deterministic full redraw "+t);
            changed++;
            if(TexRemaining32.item(t)) {
               items++;int alphaChanges=0;
               for(int i=0;i<1024;i++) {
                  if((art[i]>>>24)!=(old[i]>>>24))alphaChanges++;
                  check((art[i]>>>24)==0||(art[i]>>>24)==255,"hard native sprite edge "+t);
               }
               if(alphaChanges>0)newSilhouettes++;
            }
         }
         check(changed==500,"456 redrawn cells plus 44 explicitly reopened reference cells");
         check(newSilhouettes==items,"every rejected item receives a new silhouette");
         for(int t:new int[]{23,24,235,263})for(float phase:new float[]{0,1,7,15.5F}) {
            int[] frame=TexRemaining32.frame(t,phase);
            check(Arrays.equals(frame,TexRemaining32.frame(t,phase+16)),"periodic animation "+t);
            HashSet<Integer> pigments=new HashSet<>();
            for(int c:frame)if(c!=0)pigments.add(c);
            check(pigments.size()>=2&&pigments.size()<=5,"five pigment animation; no interpolated pixels "+t);
         }
         for(int t:new int[]{574,576,578,580}) {
            int[] lower=TexRemaining32.paint(t), upper=TexRemaining32.paint(t+1);
            check((lower[15]>>>24)==255&&(upper[31*32+15]>>>24)==255,"continuous double-plant stem "+t);
         }
      });
      test("category reviews: only requested material families change from the published 6.5 atlas", () -> {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);Tex.initPixels();
         java.util.Properties before=new java.util.Properties();
         try(java.io.InputStream in=new java.io.FileInputStream("test/pixel32-v65.properties")){before.load(in);}
         catch(java.io.IOException e){throw new RuntimeException(e);}
         int changed=0;
         for(int t=0;t<1024;t++) {
            int[] art=t==77?BowSprite.pixels(0,false):TexPixel32.tile(t,TexOverhaulV57.readTile(t),TexRemaining32.animated(t));
            boolean differs=Arrays.hashCode(art)!=Integer.parseInt(before.getProperty(Integer.toString(t)));
            check(differs==(TexBuildingReview32.handles(t)||TexColorNature32.handles(t)),"change scope exactly matches requested faces: "+t);
            if(!differs)continue;
            changed++;
            check(art.length==1024&&Arrays.equals(art,TexColorNature32.handles(t)?TexColorNature32.paint(t):TexBuildingReview32.paint(t)),"native deterministic review painter "+t);
            for(int c:art)check((c>>>24)==255,"building material has no alpha holes "+t);
         }
         check(changed==118,"16 building faces plus 102 colors/nature faces");
         for(int t:new int[]{44,656}) {
            int[] rock=TexMaterials32.base(t==44?Tex.COBBLE:Tex.STONEBRICK),art=TexBuildingReview32.paint(t);
            int moss=0;
            for(int i=0;i<1024;i++)if(rock[i]!=art[i])moss++;
            check(moss>100&&moss<460,"fine moss preserves most underlying masonry "+t);
         }
         for(int c:TexBuildingReview32.paint(487))check((c&255)>(c>>8&255)&&(c&255)>(c>>16&255),"lapis has blue mineral veins, no yellow dots");
         for(int t:new int[]{44,522})check(Arrays.hashCode(TexPixel32.preReviewReference(t))==Integer.parseInt(before.getProperty(Integer.toString(t))),"accurate historical reference for requested exception "+t);
      });
      test("colors/nature review: narrow scope, rotated logs, embedded ore and snow-layer mapping", () -> {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);Tex.initPixels();
         java.util.Properties before=new java.util.Properties(), release=new java.util.Properties();
         try(java.io.InputStream in=new java.io.FileInputStream("test/pixel32-building-review.properties")) {before.load(in);}
         catch(java.io.IOException e){throw new RuntimeException(e);}
         try(java.io.InputStream in=new java.io.FileInputStream("test/pixel32-v65.properties")) {release.load(in);}
         catch(java.io.IOException e){throw new RuntimeException(e);}
         int changed=0;
         for(int t=0;t<1024;t++) {
            int[] art=BuildingBlockReviewPreview.pixels(t);
            boolean differs=Arrays.hashCode(art)!=Integer.parseInt(before.getProperty(Integer.toString(t)));
            check(differs==TexColorNature32.handles(t),"only this batch's listed cells change "+t);
            if(differs){changed++;check(Arrays.equals(art,TexColorNature32.paint(t)),"deterministic dispatch "+t);for(int c:art)check(c>>>24==255,"opaque native face "+t);}
            if(TexColorNature32.wasProtected(t))check(Arrays.hashCode(TexPixel32.preReviewReference(t))==Integer.parseInt(release.getProperty(Integer.toString(t))),"honest historical reference "+t);
         }
         check(changed==102,"all 102 color/nature cells revised; earlier building batch untouched");
         int[][] logs={{7,246},{33,247},{31,248},{540,546},{541,547},{542,548}};
         for(int[] pair:logs){int[] side=TexColorNature32.paint(pair[0]),turned=TexColorNature32.paint(pair[1]);for(int y=0;y<32;y++)for(int x=0;x<32;x++)check(side[y*32+x]==turned[x*32+y],"horizontal logs rotate the same native artwork");}
         for(int t:new int[]{12,13,14,15,16,17,258,488}) {
            int[] host=t==258?TexMaterials32.paint(256):TexMaterials32.base(Tex.STONE),art=TexColorNature32.paint(t);int changedRock=0;
            for(int i=0;i<1024;i++)if(host[i]!=art[i])changedRock++;
            check(changedRock>100&&changedRock<350,"ore seams leave the majority of the original host rock intact "+t);
         }
         for(int tile:Blocks.tiles[Blocks313.SNOW_LAYER])check(tile==Tex.SNOW,"loose snow is not snow-covered dirt on any face");
         check(Blocks.box[Blocks313.SNOW_LAYER][4]==.125,"snow remains a thin layer, not a full block");
         check(Arrays.hashCode(BuildingBlockReviewPreview.pixels(Tex.SNOW))==Integer.parseInt(release.getProperty("18")),"snow block texture unchanged");
         HashSet<Integer> designs=new HashSet<>();
         for(int dye=0;dye<16;dye++) {
            int[] wool=TexColorNature32.paint(48+dye),glaze=TexColorNature32.paint(449+dye);
            HashSet<Integer> shades=new HashSet<>();for(int c:wool)shades.add(c);
            check(shades.size()>2,"wool has more than the old two-tone checkerboard");designs.add(Arrays.hashCode(glaze));
            check(!Arrays.equals(TexColorNature32.paint(416+dye),TexMaterials32.paint(400+dye)),"loose powder differs from solid concrete");
         }
         check(designs.size()==16,"all glazed designs are distinct");
      });
      test("cohesive materials: shared timber, masonry grid and restrained cracks", () -> {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT,null);
         Tex.initPixels();
         int[] base=TexPixel32.tile(Tex.STONEBRICK,TexOverhaulV57.readTile(Tex.STONEBRICK),false);
         int[] cracked=TexMaterials32.paint(657);
         int changes=0;
         for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
            if(base[y*32+x]!=cracked[y*32+x])changes++;
            if(y%8==0||(x+(y/8%2)*8)%16==0)
               check(base[y*32+x]==cracked[y*32+x],"cracks preserve the approved mortar");
         }
         check(changes>15&&changes<70,"sparse local fractures, not stripes across the tile");
         int total=0;
         for(int c:TexMaterials32.paint(259)) {
            total+=(c>>16&255)+(c>>8&255)+(c&255);
            check((c>>>24)==255,"Nether brick opaque");
         }
         check(total/(1024*3)<55,"dark charcoal-burgundy rather than pale mauve");
         for(int t=0;t<1024;t++) if(TexMaterials32.handles(t)) {
            int[] art=TexMaterials32.paint(t);
            check(art.length==1024,"native 32px material "+t);
            check(Arrays.equals(art,TexRemaining32.paint(t)),"preview is actual game painter output "+t);
            check(Arrays.equals(art,TexMaterials32.paint(t)),"deterministic material "+t);
            for(int c:art)check(c==0||(c>>>24)==255,"hard, unblurred alpha "+t);
         }
         for(int species=0;species<6;species++) {
            int t=species==0?Tex.PLANKS:TexArt313.PLANKS0+species-1;
            check(Arrays.equals(TexMaterials32.timber(species),TexPixel32.tile(t,TexOverhaulV57.readTile(t),false)),"same approved timber on furniture, species "+species);
         }
         int[] oak=TexMaterials32.timber(0), chest=TexMaterials32.paint(42), top=TexMaterials32.paint(37);
         check(chest[22*32+8]==oak[22*32+8],"chest boards are the approved oak, not a separate paint style");
         check(top[22*32+8]==oak[22*32+8],"crafting surface uses the same timber");
         int[] copy=TexMaterials32.timber(0);copy[0]=0;
         check(TexMaterials32.timber(0)[0]==oak[0],"shared reference pixels cannot be mutated by another painter");
         check(TexMaterials32.paint(Tex.STONEBRICK)==null&&TexMaterials32.paint(Tex.PLANKS)==null
            &&TexMaterials32.paint(Tex.COBBLE)==null,"approved references are not overridden");
      });
      test("native items: extrusion follows the new 32px silhouette, not the legacy mask", () -> {
         ResourcePacks.select(ResourcePacks.ID_DEFAULT, null);
         Tex.initPixels();
         Renderer.itemGeo.clear();
         int tile=Items.tile[Items.toolId(4,4)];
         int[] pixels=TexPixel32.tile(tile,TexOverhaulV57.readTile(tile),false);
         int quads=0;
         for(int y=0;y<32;y++)for(int x=0;x<32;x++)if((pixels[y*32+x]>>>24)!=0){
            quads+=2;
            if(x==0||(pixels[y*32+x-1]>>>24)==0)quads++;
            if(x==31||(pixels[y*32+x+1]>>>24)==0)quads++;
            if(y==0||(pixels[(y-1)*32+x]>>>24)==0)quads++;
            if(y==31||(pixels[(y+1)*32+x]>>>24)==0)quads++;
         }
         float[] geo=Renderer.spriteGeometry(tile,-6400);
         check(geo.length==quads*24,"every visible texel and exposed edge has a face");
         for(int i=0;i<geo.length;i+=6){
            check(geo[i]>=-.5F&&geo[i]<=.5F&&geo[i+1]>=-.5F&&geo[i+1]<=.5F,"unchanged item scale");
            check(geo[i+3]>=Tex.tu(tile)&&geo[i+3]<=Tex.tu(tile)+Tex.TS,"u stays in tile");
            check(geo[i+4]>=Tex.tv(tile)&&geo[i+4]<=Tex.tv(tile)+Tex.TS,"v stays in tile");
         }
         Renderer.itemGeo.clear();
      });
      test("weapons: barrels face forward, forearm aim compensates elbow, recoil returns to rest", () -> {
         double yaw = Math.toRadians(WeaponPose.FIRST_PERSON_YAW);
         check(-Math.sin(yaw) < -0.99 && Math.cos(yaw) < 0, "muzzle points away from camera and slightly inward");
         for (int id : new int[]{Redstone319.PISTOL, Redstone319.MACHINE_GUN, Redstone319.SNIPER_RIFLE,
               Redstone319.SIEGE_CANNON, Redstone319.MORTAR_STRIKE}) {
            PlayerModel.Pose p = new PlayerModel.Pose();
            p.held = id;
            for (int pitch = -60; pitch <= 60; pitch += 10) {
               p.headPitch = pitch;
               p.swing = 0.4F; // firing must not apply the sword/mining swing
               PlayerAnim.Joints j = PlayerAnim.solve(p);
               check(Math.abs(j.sh[0] + j.el[0] + j.tPitch + j.pitch - (90 - pitch)) < 0.01F, "aim at pitch " + pitch);
            }
            check(WeaponPose.muzzleX(id) > 0 && WeaponPose.gripX(id) < 0, "muzzle in front of grip");
            check(WeaponPose.recoil(id, 0, 0) == 0, "rest");
            float previous = 1;
            for (float t = 0; t <= WeaponPose.cooldown(id); t += 0.05F) {
               float r = WeaponPose.recoil(id, WeaponPose.cooldown(id), t);
               check(r >= 0 && r <= previous + 0.00001F, "continuous recoil recovery");
               previous = r;
            }
         }
      });
      test("bow: 32px sprite stages attach string and arrow, release has no ghost arrow", () -> {
         check(BowSprite.stage(0) == 0 && BowSprite.stage(0.2F) == 1 && BowSprite.stage(0.5F) == 2 && BowSprite.stage(1) == 3, "idle plus three drawing stages");
         for (int stage = 0; stage <= 3; stage++) {
            int[] bare = BowSprite.pixels(stage, false), arrow = BowSprite.pixels(stage, true);
            check(bare.length == 32 * 32 && arrow.length == 32 * 32, "max 32px frames");
            int tx = BowSprite.tipX(stage), ty = BowSprite.tipY(stage), nx = BowSprite.nockX(stage);
            check((bare[ty * 32 + tx] >>> 24) == 255 && (bare[(31 - ty) * 32 + tx] >>> 24) == 255, "string meets both limb tips");
            check((bare[16 * 32 + nx] >>> 24) == 255 && (arrow[16 * 32 + nx] >>> 24) == 255, "arrow tail shares the string nock");
            check(arrow[16 * 32 + 1] != 0 && bare[16 * 32 + 1] == 0, "release removes arrow immediately");
            check(bare[16 * 32 + 8] == BowSprite.pixels(0, false)[16 * 32 + 8], "grip is fixed in every frame");
            for (int c : arrow) check((c >>> 24) == 0 || (c >>> 24) == 255, "no antialiased/fringed sprite edges");
         }
         check(WeaponPose.releaseDraw(1, 4, 0) == 1 && WeaponPose.releaseDraw(1, 0, 0) == 0, "release endpoints");
         for (int tick = 4; tick > 0; tick--) {
            check(Math.abs(WeaponPose.releaseDraw(1, tick, 1) - WeaponPose.releaseDraw(1, tick - 1, 0)) < 0.00001F, "release continuous across ticks");
         }
         Game g = GameTests.newGame(0);
         g.inv[g.sel] = new Stack(Items.BOW, 1);
         g.bowCharge = 10;
         check(g.pose(0.75F).bow > g.pose(0).bow, "third-person draw interpolates between ticks");
         check(g.bowVisualDraw(0.75F) == g.pose(0.75F).bow, "both views use same draw");
         g.bowCharge = 0;
         g.bowReleaseDraw = 1;
         g.bowReleaseTicks = 4;
         check(g.bowVisualDraw(0.5F) > 0 && g.pose(0.5F).bow < 0, "relaxing bow has no ghost nocked arrow");
         g.inv[g.sel] = new Stack(Items.ARROW, 1);
         check(g.bowVisualDraw(0) == 0, "switching items hides release");
      });
      test("hand lighting: one gamma-correct lightmap sample, independent of view direction", () -> {
         int[] lm = new int[256];
         lm[15] = 0xFFEAEFFF;
         lm[15 * 16] = 0xFFFFCD82;
         lm[0] = 0xFF15171B;
         check(HandLighting.sample(lm, 15.5F / 16, 0.5F / 16) == lm[15], "daylight sample");
         check(HandLighting.sample(lm, 0.5F / 16, 15.5F / 16) == lm[240], "warm block light, not a second grey brightness multiplier");
         check(HandLighting.sample(lm, -1, -1) == lm[0], "clamped dark sample");
         check(HandLighting.fallback(0.2F, 1) > HandLighting.fallback(0.2F, 0), "brightness option also affects shader-off hands");
         float[] lights = {2, 0, 0, 8}, colors = {1, 0.65F, 0.25F, 1};
         int warm = HandLighting.withNearby(lm[0], lights, colors);
         check((warm >> 16 & 255) > (warm >> 8 & 255) && (warm >> 8 & 255) > (warm & 255), "nearby torch warms held item even before block-light propagation");
         check(warm == HandLighting.withNearby(lm[0], new float[]{0, 0, 2, 8}, colors), "turning camera does not flip nearby light");
         check(HandLighting.withNearby(0xFFFFFFFF, lights, colors) == 0xFFFFFFFF, "no double-lighting in daylight");
         String fs = ShaderSrc.ENTITY_FS;
         check(fs.indexOf("if (handPass > 0.5)") < fs.indexOf("cross(dFdx(vRel)"), "viewmodel bypasses derivative-based world normal lighting");
         check(fs.contains("t.rgb * vCol.rgb * handLight"), "single sampled lighting factor");
      });
      test("showcase: extended block IDs in starter kit do not index the item-only stack table", () -> {
         Game g = GameTests.newGame(0);
         g.giveShowcaseStarter();
         check(g.inv[0].id == Items.BOW && g.inv[0].n == 1, "bow stack");
         check(g.inv[8].id == Redstone319.REDSTONE_TORCH && g.inv[8].n == 64, "extended-ID redstone torch stack");
         WorldSettings settings = new WorldSettings();
         settings.type = WorldSettings.ALL_BLOCKS;
         Chunk c = new AllBlocksGen(1L, settings).generate(0, 0);
         for (int i = 0; i < 256; i++) {
            check(c.grassColor[i] == Gen.grassColorFor(0.8, 0.4), "gallery grass is biome-tinted, not black");
            check(c.foliageColor[i] == Gen.foliageColorFor(0.8, 0.4), "gallery foliage has a nonzero tint");
         }
      });
      test("bow: actual draw/release consumes one arrow, menus cancel without firing", () -> {
         Game g = GameTests.newGame(0);
         g.inv[g.sel] = new Stack(Items.BOW, 1);
         g.inv[10] = new Stack(Items.ARROW, 3);
         g.rightDown = true;
         for (int i = 0; i < 20; i++) g.interact();
         check(g.bowCharge == 20, "charges while held");
         g.rightDown = false;
         g.interact();
         check(g.bowCharge == 0 && g.bowReleaseTicks == 4, "starts spring-back on release");
         check(g.mobs.arrows.size() == 1 && g.countItem(Items.ARROW) == 2, "one projectile, one arrow used");
         g.rightDown = true;
         g.interact();
         g.setScreen(Game.S.PAUSE);
         check(g.bowCharge == 0 && g.bowReleaseTicks == 0 && g.mobs.arrows.size() == 1, "menu cancels without shooting");
      });
   }
}
