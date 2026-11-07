#!/usr/bin/env python3
"""
Assembles what goes onto the GitHub *Releases* page, from the files that are committed in the repository:

    releases/Blockcraft-UPDATE<ver>-patch.zip      (built by tools/build.py --patch, smoke-tested on Windows)
    Blockcraft-1.12-UPDATE3.5-windows.zip          (the base kit with the bundled Java)

and writes into build/release/:

    Blockcraft-1.12-UPDATE<ver>-windows.zip   the complete game: base kit + the files of the update pack.
                                              This is what a player downloads - extract anywhere, run Blockcraft.exe.
    Blockcraft-UPDATE<ver>-patch.zip          the small update pack (copied unchanged)
    release-notes.md                          the "what's new" text of that version + install instructions
    VERSION                                   e.g. "3.6"

It deliberately does NOT compile anything: the released bytes are exactly the committed (and tested) update pack.
Used by .github/workflows/release.yml; can be run locally too:  python3 tools/make_release.py [version]
"""
import glob
import json
import hashlib
import os
import re
import shutil
import subprocess
import tempfile
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build as B  # noqa: E402

ROOT = B.ROOT
OUT = os.path.join(ROOT, "build", "release")


def version_key(path):
    m = re.search(r"UPDATE([0-9.]+)-patch\.zip$", path)
    return tuple(int(x) for x in m.group(1).split(".")) if m else (0,)


def find_patch(version=None):
    zips = glob.glob(os.path.join(ROOT, "releases", "Blockcraft-UPDATE*-patch.zip"))
    if version:
        zips = [z for z in zips if z.endswith("UPDATE%s-patch.zip" % version)]
    if not zips:
        print("Building update pack for version %s via tools/build.py --patch ..." % (version or "latest"))
        import subprocess
        subprocess.run([sys.executable, os.path.join(ROOT, "tools", "build.py"), "--patch"], check=True)
        zips = glob.glob(os.path.join(ROOT, "releases", "Blockcraft-UPDATE*-patch.zip"))
        if version:
            zips = [z for z in zips if z.endswith("UPDATE%s-patch.zip" % version)]
    if not zips:
        sys.exit("no update pack found in releases/ (version %s) - run: python3 tools/build.py --patch" % (version or "latest"))
    return sorted(zips, key=version_key)[-1]


def whats_new(version):
    """The 'WHAT'S NEW IN UPDATE x.y' or 'WHAT'S NEW IN V x.y' section of package/README.txt."""
    text = open(os.path.join(ROOT, "package", "README.txt"), encoding="utf-8", errors="replace").read().replace("\r\n", "\n")
    m = re.search(r"^WHAT'S NEW IN (?:UPDATE |V)%s\b.*?$(.*?)(?=^(?:WHAT'S NEW IN |NEW IN [0-9]|FIXES IN|CONTROLS)\b)" % re.escape(version), text, re.S | re.M)
    if not m:
        return "(see README.txt inside the zip)"
    heading = re.search(r"^WHAT'S NEW IN (?:UPDATE |V)%s\b.*?$" % re.escape(version), text, re.M).group(0).strip()
    return heading + "\n" + m.group(1).rstrip()


def notes(version, full_name, patch_name):
    return (
        "## Blockcraft V%(v)s\n\n"
        "### Install (Windows 64-bit, nothing else needed - Java is inside)\n\n"
        "1. Download **%(full)s** below (\"Assets\").\n"
        "2. Right-click it -> *Extract All...* into a **new folder** (anywhere you like).\n"
        "3. Open the extracted `Blockcraft` folder and run **Blockcraft.exe**.\n\n"
        "Your worlds and options live in `%%USERPROFILE%%\\.blockcraft112`, so they are **kept** when you use a new folder.\n"
        "If Windows SmartScreen warns about an unknown publisher: *More info -> Run anyway* (the game is not code-signed).\n"
        "If it doesn't start, run `Blockcraft-debug.bat` and read the messages (or open `%%USERPROFILE%%\\.blockcraft112\\crash.log`).\n\n"
        "*Small download instead?* `%(patch)s` only contains the changed files - extract it and copy the "
        "`Blockcraft` folder over an existing Blockcraft install of V3.5 or newer (\"Replace files\").\n\n"
        "### What's new\n\n```\n%(new)s\n```\n"
    ) % {"v": version, "full": full_name, "patch": patch_name, "new": whats_new(version)}


def texture_kit(patch, version):
    """Export from the verified, packaged JAR, never from separate/stale build classes."""
    name = "Blockcraft-V%s-Aseprite-textures.zip" % version
    with tempfile.TemporaryDirectory(prefix="texture-kit-", dir=B.BUILD) as tmp:
        jar = os.path.join(tmp, "blockcraft.jar")
        with zipfile.ZipFile(patch) as z:
            with open(jar, "wb") as f:
                f.write(z.read("Blockcraft/blockcraft.jar"))
        pack = os.path.join(tmp, "resourcepacks", "MyTextures")
        subprocess.run([B.java_exe(), "-Djava.awt.headless=true", "-jar", jar, "--export-textures", pack], check=True, timeout=120)
        with zipfile.ZipFile(os.path.join(OUT, name), "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
            for folder, dirs, files in os.walk(pack):
                dirs.sort()
                for fn in sorted(files):
                    path = os.path.join(folder, fn)
                    entry = zipfile.ZipInfo(os.path.relpath(path, tmp).replace(os.sep, "/"), B.FIXED_TIME)
                    entry.compress_type = zipfile.ZIP_DEFLATED
                    entry.external_attr = 0o644 << 16
                    with open(path, "rb") as f:
                        z.writestr(entry, f.read())
        with zipfile.ZipFile(os.path.join(OUT, name)) as z:
            assert z.testzip() is None
            expected_tiles = 558 if tuple(map(int, version.split("."))) >= (6, 15) else 550 if tuple(map(int, version.split("."))) >= (6, 14) else 545 if tuple(map(int, version.split("."))) >= (6, 7) else 532
            assert len([n for n in z.namelist() if "/tiles/" in n]) == expected_tiles
            assert len([n for n in z.namelist() if "/animations/" in n]) == 6
    return name


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else B.game_version()
    patch = find_patch(version)
    version = re.search(r"UPDATE([0-9.]+)-patch\.zip$", patch).group(1)
    if tuple(map(int, version.split("."))) >= (6, 2):
        from verify_release import verify
        verify(patch, version) # fail BEFORE assembling/publishing any stale or mislabeled bytes

    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT)

    # files of the update pack -> replacements inside the base kit
    rep = {}
    with zipfile.ZipFile(patch) as z:
        for i in z.infolist():
            if i.is_dir() or not i.filename.startswith("Blockcraft/"):
                continue  # HOW-TO-INSTALL-THIS-UPDATE.txt is only for the patch
            mode = (i.external_attr >> 16) & 0o777 or 0o644
            rep[i.filename] = (z.read(i), mode)
    for need in ("Blockcraft/Blockcraft.exe", "Blockcraft/blockcraft.jar", "Blockcraft/README.txt"):
        if need not in rep:
            sys.exit("update pack %s lacks %s" % (os.path.basename(patch), need))

    full_name = "Blockcraft-V%s-windows.zip" % version
    patch_name = "Blockcraft-V%s-patch.zip" % version
    full = B.compose_full_zip(os.path.join(OUT, full_name), rep)
    shutil.copy(patch, os.path.join(OUT, patch_name))

    # ---- self-check: the zip a player gets must be complete and contain exactly the tested files ----
    with zipfile.ZipFile(full) as z, zipfile.ZipFile(B.BASE_ZIP) as base:
        assert z.testzip() is None, "corrupt zip"
        names = set(z.namelist())
        missing = [n for n in base.namelist() if n not in names]
        assert not missing, "missing from the full zip: %s" % missing[:5]
        for n, (data, _mode) in rep.items():
            assert z.read(n) == data, "%s differs from the update pack" % n
        for n in base.namelist():
            if n not in rep and not n.endswith("/"):
                assert z.read(n) == base.read(n), "%s differs from the base kit" % n
        assert "Blockcraft/jre/bin/javaw.exe" in names, "bundled Java is missing"
        n_files = len([n for n in names if not n.endswith("/")])
    with zipfile.ZipFile(patch) as z:
        assert z.testzip() is None

    release_notes = notes(version, full_name, patch_name)
    if tuple(map(int, version.split("."))) >= (6, 6):
        kit = texture_kit(patch, version)
        release_notes += (
            "\n### Edit the textures in Aseprite\n\n"
            "1. Run **Edit-textures.bat** inside the extracted game folder. It exports the actual 32px PNGs and opens a searchable index.\n"
            "2. Open a PNG in Aseprite and save/export your edits to the same PNG.\n"
            "3. In-game: **Options > Resource Packs > Reload PNGs**, then select **My Aseprite Textures**. "
            "After later saves, just press **Reload PNGs** again.\n\n"
            "The shortcut never overwrites an existing pack. Includes %d individual tiles and six animation sheets "
            "(water, lava, fire, portal, lit furnace and all eight flat-bow poses). "
            "See `Texture-editing.txt` for Aseprite sheet settings, file locations and backups.\n\n"
            "**Optional ready-made editor kit:** `%s` — extract into the folder containing Blockcraft.exe, "
            "without replacing an already edited MyTextures folder. The game download can generate this same kit itself.\n\n"
            "Earlier releases remain available unchanged. SHA256SUMS.txt checks all downloads.\n" % (558 if tuple(map(int, version.split("."))) >= (6, 15) else 550 if tuple(map(int, version.split("."))) >= (6, 14) else 545 if tuple(map(int, version.split("."))) >= (6, 7) else 532, kit))
    if version == "6.9":
        world_name = "Blockcraft-Block-Drop-World.zip"
        world = os.path.join(ROOT, world_name)
        with zipfile.ZipFile(world) as z:
            assert z.testzip() is None
            assert z.read("Block_Drop_V69/raster.dat")[:4] == b"RAS1"
            assert z.read("FallingBlocks.asm") == open(os.path.join(ROOT, "package/FallingBlocks.asm"), "rb").read()
        shutil.copy(world, os.path.join(OUT, world_name))
        release_notes += ("\n### Play the lamp-screen arcade\n\n"
            "Download **Blockcraft-Block-Drop-World.zip** as well as the V6.9 game/patch. "
            "Extract it and run **Install-world.bat** (never overwrites an existing world), "
            "then open **Block Drop - Lamp Arcade and Modular Displays** in Singleplayer. "
            "Right-click the keypad on your left and press **Enter**. "
            "Arrows move/rotate, Down soft-drops, Space hard-drops, P pauses, Escape leaves controls.\n\n"
            "28 joined panels provide a 16x28 color screen; 448 ordinary redstone lamps mirror it. "
            "Separate CPU, ROM, RAM and GPU blocks run the included editable FallingBlocks.asm program. "
            "Add panels for more pixels, up to 64x64. The existing game image is not automatically stretched. "
            "Use view distance 5+; do not open the new save in an older game version. "
            "See Raster-guide.txt for circuit layout, instruction set, crafting and limitations.\n")
    if version == "6.10":
        world_name = "Blockcraft-Sound-Machines.zip"
        world = os.path.join(ROOT,world_name)
        with zipfile.ZipFile(world) as z:
            assert z.testzip() is None
            for folder,count in (("Shadow_Signal_V610",81),("Block_Drop_Sound_V610",49)):
                assert z.read(folder+"/raster.dat")[:4] == b"RAS1"
                assert sum(n.startswith(folder+"/") and n.endswith(".gz") for n in z.namelist()) == count
            assert b"shadow-signal" in z.read("Shadow_Signal_V610/media.properties")
            for name in ("FallingBlocks-Sound.asm","MediaPlayer.asm","Sound-machines.txt","Raster-guide.txt","Make-media.py"):
                assert z.read(name) == open(os.path.join(ROOT,"package",name),"rb").read()
            for name,digest in json.loads(z.read("World-SHA256.json")).items():
                assert hashlib.sha256(z.read(name)).hexdigest() == digest
        shutil.copy(world,os.path.join(OUT,world_name))
        release_notes += ("\n### Two new sound-machine worlds\n\n"
            "Download **Blockcraft-Sound-Machines.zip**, update the game to **V6.10**, extract the whole ZIP and run **Install-worlds.bat**. "
            "It installs two separate saves and refuses to overwrite existing destinations. Your V6.9 world is not replaced.\n\n"
            "* **Shadow Signal — Original Silhouette Cinema:** a new 32-second black-and-white animation and synthesized score, "
            "**not the Bad Apple!! video/song**. A 64×48 panel wall and 32×24 real redstone lamps are driven by an editable media ROM. "
            "Right-click the left keypad, then **Enter**. Space/P pauses, arrows seek five seconds, Enter/R restarts, M mutes, Escape leaves. "
            "The real lamps retain redstone afterglow; look left/fly closer to the panel wall for the sharper image.\n"
            "* **Block Drop — Sound Edition:** the existing ROM game now emits its own sound cues through a physical speaker. "
            "Its controls are unchanged; M toggles audio.\n\n"
            "Both saves start paused. Use view distance 5+. Speaker, cartridge, lamp sampling, custom PNG/WAV conversion and instruction-set details "
            "are in **Sound-machines.txt**. The original clip is already inside the game. **MediaPlayer.asm** and **FallingBlocks-Sound.asm** "
            "are editable programs, not a hidden video/game controller. No texture/bow/lighting redesign in this update.\n\n"
            "**Existing V6.9 world:** back it up, look at a connected Block Drop component in Creative and type `/arcadesound`. "
            "It adds a speaker and upgrades only the exact supplied ROM; **this resets the current game**. Custom ROMs and existing blocks are not overwritten. "
            "`/shadow` builds a new cinema; `/arcade` builds the sound edition.\n\n"
            "Audio is driven by the displayed ROM frame and bounded so it cannot run through a paused/stalled movie. "
            "Device buffering, renderer updates, lamp afterglow and network latency still add delay. "
            "Custom media must also be copied to multiplayer clients; no large-file streaming. "
            "See the source verification report for automated tests and real-renderer evidence; no physical speaker audition or two-PC multiplayer test is claimed.\n")
    if version == "6.11":
        release_notes += ("\n### Turn a MIDI into a real redstone build\n\n"
            "Find **MIDI Build Compiler** in Creative (search MIDI). Run `/mididemo` for an original example and the input folder, "
            "or copy your own `.mid`/`.midi` into `%USERPROFILE%\\.blockcraft112\\midi`. "
            "Place the compiler on a short pillar above a clear area, right-click it and enter `/midi Workshop.mid`. "
            "Read the preview, then `/midibuild` to confirm. Right-click the generated **stone button** to play. "
            "The build uses ordinary repeaters, dust and tuned note blocks, and still works after removing the compiler.\n\n"
            "**16 note-block instruments** now respond to the block beneath them. Wood gives bass; sand snare; glass hi-hat; "
            "stone bass drum; clay flute; gold bell; wool guitar; packed ice chime; bone/iron xylophones; soul sand cowbell; "
            "pumpkin didgeridoo; emerald bit; hay banjo; glowstone pling; default harp. "
            "Tuning persists through saves and pistons; keep the space above clear. All sounds are locally synthesized.\n\n"
            "**Limits:** Creative host imports/builds; MIDI type 0/1 and tempo maps/SMPTE; 100ms quantized timing plus a fixed 100ms lead-in; "
            "six-note chords; octave folding into each instrument's two-octave range; fixed strikes rather than MIDI sustain/velocity/bends. "
            "The straight physical track is capped at 256 blocks and must stay loaded. For long files choose sections, e.g. "
            "`/midi MySong.mid 30 8`. Invalid input, oversized chords, occupied space and unloaded build areas are refused instead of silently truncated/overwritten. "
            "Follow the track to hear distant notes. **MIDI-guide.txt** in the game has complete instructions.\n\n"
            "Existing V6.10 worlds/hardware and the prior artwork/sprite-bow/lighting changes are retained. Back up worlds before updating; "
            "this release adds `notes.properties` for persistent tuning. Audio data and actual redstone timing are tested; no physical-device or two-PC audition is claimed.\n")
    if version == "6.12":
        release_notes += ("\n### MIDI importing without command typing\n\n"
            "In Creative, **right-click a MIDI Build Compiler**, browse directly to your `.mid`/`.midi`, "
            "preview the section, then click **Build circuit**. Home, Downloads, drives, folders and pagination "
            "are built in. Files can stay where they are; spaces, Unicode names and uppercase extensions work.\n\n"
            "The default selection is the **first 5 seconds**, not the whole song. Edit Start/Length and Preview "
            "for another section. The screen reports notes, instruments, omitted/outside-section notes, octave folds, "
            "repeaters, footprint and button coordinates. The default height finds empty space **above terrain**, "
            "with a final no-overwrite check when building. **Go to button** brings you to the physical start.\n\n"
            "Fixed generic 'Invalid arguments' MIDI errors and the need to keep aiming at the compiler after preview. "
            "Invalid files, permissions, clearance, unloaded areas and conversion limits now have specific diagnostics. "
            "Changing input invalidates stale previews; builds recheck host permissions and the original compiler.\n\n"
            "Playback remains real redstone with all **16 material-selected note instruments**; the compiler and MIDI "
            "are unnecessary afterward. Limits remain: Creative host, 1 MiB/type 0 or 1 MIDI, 100ms timing, "
            "512 notes, six-note chords, 120s selected duration and a **256-block rail**. Dense/long tracks may need "
            "much shorter sections. No velocity/sustain/bend reproduction; follow the track for positional sound.\n\n"
            "Includes MIDI-guide.txt. Back up saves; use V6.12 on multiplayer peers. Existing worlds, display/audio "
            "hardware, artwork, flat sprite bow and held lighting are retained. The user's DSi Shop file was not "
            "provided, so that particular MIDI has not been validated.\n")
    if version == "6.13":
        release_notes += ("\n### Play the MIDI with note-block sounds, without building a track\n\n"
            "Right-click a **MIDI Build Compiler** in Creative, select your MIDI and click **Play full**. "
            "The entire file plays using the game's actual note-block sample bank, centered on your speakers/headphones. "
            "**No 256-block rail, no terrain clearance and no flying after the music.**\n\n"
            "**Play section** uses Start/Length; **Play full** ignores the default five-second build section. "
            "**Stop** cancels notes and their ringing tails. Play again restarts. Keep the menu open: Close/Escape, "
            "editing input, changing files/worlds or losing compiler access stops playback. Listening is **local only**, "
            "not broadcast to other players. Sounds volume applies and background music is lowered while playing.\n\n"
            "Keeps the 16 note-block sounds, two-octave/25-pitch ranges, octave folding, 100ms timing and fixed strikes "
            "instead of MIDI sustain/velocity/bends. The audio device drives timing even with the world paused. "
            "Listening bypasses the 512-note build cap and six physical lanes. Software safety limits are "
            "**1 MiB/type 0 or 1 MIDI, 100,000 events, 50,000 converted notes, 60 minutes and 32 notes per chord**. "
            "Oversized input is explained, not silently truncated.\n\n"
            "**Build circuit** remains a separate button with unchanged physical limits and independent redstone playback. "
            "Existing worlds, note tuning, display/audio hardware, artwork, sprite bow and held lighting are retained. "
            "Back up saves before updating; see MIDI-guide.txt. Automated checks verify the mixer and full-song timing; "
            "no physical-speaker audition is claimed.\n")
    if version == "6.14":
        release_notes += ("\n### Native Create project: foundation milestone — NOT full Create\n\n"
            "This begins a native mechanical expansion inside Blockcraft, without Minecraft or Forge. "
            "**Full Create is not implemented.** Belts, moving contraptions, trains, logistics, fluid/heat systems, "
            "most processing and full multiplayer remain unfinished. See the source `docs/create/STATUS.md` for the pinned scope.\n\n"
            "Working foundation: signed rotational networks; three-axis shafts; small/large cogs with direction and "
            "2:1 ratios; bevel gearing and gearboxes; Creative Motor and compact water wheel; stress/overload checks; "
            "redstone clutch/gearshift; speed/stress gauges; prototype mechanical press and millstone with real "
            "inputs/outputs and saved inventory/progress. **These prototypes do not yet use Create's depot/belt processing.**\n\n"
            "Search **create** in Creative. Place parts against the desired shaft face; use the **Kinetic Wrench** to "
            "rotate them. Right-click the motor to choose RPM and machines to open their inventory. "
            "**/kinetics** builds an ordinary-block example factory in clear loaded air, with a 2:1 press branch and "
            "redstone-controlled mill. Breaking a drive connection stops its actual production.\n\n"
            "Native safety limits: 256 RPM, 4096 loaded kinetic blocks, host-only configuration/inventories. "
            "The initial compact wheel accepts water beside its rim; exact flow scoring/multiblock geometry, complete "
            "recipes and collision/visual parity are not claimed. Conflicts stall instead of destroying blocks. "
            "Back up saves, including new `kinetics.dat`, and use V6.14 on all peers.\n\n"
            "Reference target: Create 6.0.10. MIT code attribution ships in **Create-code-license.txt**; upstream "
            "All Rights Reserved assets are NOT redistributed. Models/sprites are original Blockcraft work. "
            "**Create-foundation.txt** explains all current controls, provisional recipes and missing systems. "
            "Prior MIDI, redstone computers, sound/display worlds, approved art, sprite bow and held lighting are retained.\n")
    if version == "6.15":
        release_notes += ("\n### Visible machinery — no normal machine menus\n\n"
            "Dedicated original component textures use constant texel density instead of stretched planks/stone. "
            "Machines now render with partial-tick interpolation every frame, independently of chunk rebuilds.\n\n"
            "**Straight horizontal belts and depots** carry visible, persistent stacks. An overhead press stamps whole "
            "batches on the belt/depot two blocks below it. Drop ingredients into a millstone or insert them by hand; "
            "empty-hand use collects output. Routine motor/wrench/wheel interactions no longer post status in chat.\n\n"
            "**Water-current direction drives the wheel:** still water stops it; opposite falling/horizontal currents "
            "reverse it, and opposing torque cancels. Manual wheel reversal is removed.\n\n"
            "Search **create** in Creative, or run **/kinetics** in clear loaded air for a belt/press/depot factory "
            "and waterfall-driven wheel. Place belts segment by segment in a straight line; power the axle side. "
            "Use with an item to insert one, sneak-use to insert the stack, empty hand to collect. "
            "Read **Create-foundation.txt** for the full controls.\n\n"
            "**Still not full Create:** no two-pulley belt connector/inclines/funnels yet, and contraptions, trains, "
            "logistics and many machines remain unfinished. Wheel geometry is compact; machine edits/processing "
            "are host-only. Old press inventories are retained for extraction/finishing, but no new press inputs "
            "enter the old hidden slots. Back up `kinetics.dat`; do not downgrade a belt world to V6.14. "
            "Original 32px component art only; no upstream restricted assets redistributed.\n")
    if version == "6.16":
        release_notes += (
            "\n### How to test the PC performance update\n\n"
            "This build keeps the V6.15 gameplay and does not lower render distance or graphics quality. "
            "Back up your world, then try it at the settings where chunk-load lag spikes were noticeable; no world reset is needed. "
            "Please report your CPU/GPU, memory, render distance and whether the spikes improved. "
            "The changes are regression-tested, but no measured frame-time improvement is claimed yet.\n"
        )
    if version == "6.20":
        release_notes += (
            "\n### Try the maps and the fixed panorama\n\n"
            "Start a world: the minimap is already in the top-right corner. Press **M** for the big map, "
            "pan with **WASD** / arrow keys or a click, zoom with the wheel or **+/-**, press **R** to recentre, "
            "**G** for grid and **B** for mob dots; **M** or **Esc** closes it. Options > Video > Debug Video > Advanced "
            "exposes every map setting (size, radius, rotation, refresh, zoom, mob dots, grid), and Options > Game has a quick "
            "Minimap switch. The title screen now shows a landscape immediately instead of a black background.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with the eight new V6.20 regression groups; strict equivalence still matches the "
            "original Update 3.5 terrain/world/tick fingerprints; all GLSL programs parse; and `tools/verify_release.py` "
            "checks the packaged exe and jar (including the new map and panorama classes). A real-GPU/Windows playtest "
            "is not claimed here.\n"
        )
    if version == "6.21":
        release_notes += (
            "\n### The caves were rebuilt, and the weather reaches the horizon\n\n"
            "**New worlds only** (Create New World > Terrain \"V6.21 Caves\"; old saves keep their exact "
            "old terrain). The old swiss-cheese carving is gone: caves are now layered worm tunnels and "
            "sparse chambers with real horizontal rock strata, lava provinces, stalactites, glowstone, "
            "ore pockets, glow-lichen and mushroom growths. Deep down there are six kinds of deterministic "
            "underground sites - ruined shrine, crystal geode, lava forge, mineshaft hall, mushroom grotto and "
            "flooded cistern - each with a loot chest, lights, pillars, beams, a pedestal and corridors. Sites are placed below the *real* "
            "surface height, so they are never left floating in an ocean. Underground structures are "
            "independent of the surface Structures switch.\n\n"
            "**Weather is prettier everywhere, including far away.** Rain and snow curtains now extend to the "
            "horizon in a reach-scaled grid, lean with the actual wind, glow near storm cores and lightning, "
            "and fade with distance; soft ground haze drifts under distant rain. Clearing rain with a low sun "
            "produces a seven-band rainbow opposite the sun. Clouds get a stronger silver lining, a warm "
            "sunset rim and a cold ice halo. Detail level, distance, wind, mist, lightning glow and rainbows "
            "are all configurable in Options > Video > Debug Video > Advanced and persist in options.txt.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with the ten V6.21 regression groups (7717157 checks); strict "
            "equivalence still matches the original Update 3.5 terrain/world/tick fingerprints; all 21 GLSL "
            "programs parse; the packaged exe and jar are verified by `tools/verify_release.py` including the "
            "new CaveWorks/WeatherFx classes. `docs/screenshots/v6.21-caves.png` is a real generator "
            "cross-section and `v6.21-weather.png` is a headless palette preview - neither is a GPU "
            "screenshot, and no real-GPU/Windows playtest is claimed.\n"
        )
    if version == "6.22":
        release_notes += (
            "\n### Caves & Cliffs, smoother world generation, live shadows and a movable minimap\n\n"
            "**New worlds only** (Debug Video > Advanced > Caves & Cliffs; old saves keep their exact "
            "old terrain). The V6.21 tunnel carver is replaced again by an original Caves & Cliffs-style "
            "engine: noodle / spaghetti / cheese caves, giant halls and vertical wells, hashed local "
            "aquifers that flood the bottom of a cavern, warped lush / dripstone / ice / amethyst / "
            "deep-dark cave biomes, and amethyst geodes with crystal buds. The six V6.21 underground "
            "site types and their loot chests still generate on top of the new carver.\n\n"
            "**Taiga is no longer the heaviest biome.** Spruce crowns were nearly solid cones of "
            "roughly a thousand leaves; they are now sparse tiered conifers with hollow interiors "
            "and drooping tips, mega spruces use half the levels, tree density and a per-chunk cap "
            "stop mesh spikes, and bushy-leaf meshing no longer allocates per leaf block. "
            "Headless 3x3 taiga meshing at seed 6120: 593 -> 297 spruce leaves, 199744 -> 165648 "
            "vertices, 62.9 ms -> 36.2 ms. A Tree Density slider is provided. These are headless "
            "meshing timings, not FPS claims.\n\n"
            "**Borders and terrain look better.** Climate coordinates are warped by two slow noises; "
            "near a biome border the neighbouring biome's surface, plants, trees and map colours "
            "blend in deterministically instead of flipping on one line. Mountains gain soft "
            "terraces, rivers broad valleys, shorelines wider beaches and the snow line a broken "
            "edge. Biome Borders has Sharp / Smooth / Very Smooth.\n\n"
            "**Mob shadows update every frame.** A terrain-only copy of the shadow depth is "
            "restored with one depth blit per frame and the current mob silhouettes are stamped "
            "in, instead of lagging behind the 8..150 frame shadow-map refresh. **The minimap has "
            "four corners** (Top Right / Top Left / Bottom Right / Bottom Left) and **Move for Toasts** "
            "slides a top-right minimap down while an advancement toast is showing.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with the ten new V6.22 regression groups (8007440 checks); strict "
            "equivalence still matches the original Update 3.5 terrain/world/tick fingerprints; all 21 GLSL "
            "programs parse; the packaged exe and jar are verified by `tools/verify_release.py` including "
            "the new CaveCliffs class. `docs/screenshots/v6.22-caves.png` and `v6.22-borders.png` are "
            "headless generator / palette previews - neither is a GPU screenshot, and no real-GPU/Windows "
            "playtest is claimed.\n"
        )
    if version == "6.23":
        release_notes += (
            "\n### More tree variants and bigger trees for EVERY species\n\n"
            "**New worlds only** (old saves keep their exact old trees). All eight tree species now "
            "have several distinct silhouettes and three size tiers - 34 shapes in total: lobed, "
            "columnar, umbrella, forked and weeping oaks; clump, weeping and grand birches; tiered, "
            "pine, columnar, 2x2 giant, 2x2 grand and candle spruces; layered, buttressed and emergent "
            "jungle trees; bent, double-bend, wide and forked acacias; grand and spreading dark oaks; "
            "cypress and willow swamps; two bushes. Giant mushrooms and ice spikes scale too. Roughly "
            "58% Normal, 30% Large and 12% Giant, all derived from the seed-based tree hash so chunk "
            "seams still agree.\n\n"
            "**Bigger does not mean laggy.** Giant conifers grow tall with spaced-out hollow tiers "
            "rather than solid crowns, the per-chunk tree cap remains, and a hard 900-leaf budget "
            "bounds every single tree. Headless 3x3 taiga meshing at seed 6120: V6.22 = 297 spruce "
            "leaves / 165648 vertices / 36.2 ms, V6.23 = 229 leaves / 162548 vertices / 34.3 ms - "
            "more variety and bigger trees at slightly less cost. These are headless meshing timings, "
            "not FPS claims.\n\n"
            "**Two new settings** in Options > Video > Debug Video > Advanced: **Tree Size** "
            "(0.70x-1.50x, height and crown of every species) and **Giant Trees** (on/off, removing "
            "the rare Giant tier). Both persist in options.txt, are saved with the world and are part "
            "of the Create New World preview cache key.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with the nine new V6.23 regression groups (8065834 checks); strict "
            "equivalence still matches the original Update 3.5 terrain/world/tick fingerprints; all 21 GLSL "
            "programs parse; the packaged exe and jar are verified by `tools/verify_release.py`. "
            "`docs/screenshots/v6.23-tree-variants.png` and `v6.23-tree-sizes.png` are headless renders "
            "of the real tree geometry - neither is a GPU screenshot, and no real-GPU/Windows playtest "
            "is claimed.\n"
        )
    if version == "6.24":
        release_notes += (
            "\n### Trees, LOD and entity shadows were fixed\n\n"
            "**V6.23's flat, hole-riddled tree crowns are gone.** Every crown is now one closed, "
            "rounded 3D volume built from a vertical radius profile; conifers are continuous tapered "
            "cones instead of isolated rings; acacia, dark oak and jungle canopies have real thickness. "
            "All 34 silhouettes, three size tiers and eight species are kept. Crowns are hollow shells "
            "with solid caps, so even a Giant at Tree Size 1.5x stays inside the leaf budget (headless "
            "worst case 938 of 1200 leaves, zero truncations).\n\n"
            "**Distant LOD tiles now depict the actual trees.** They replay the exact chunk dice the "
            "generator uses (same seed, 44 attempts, same plan, variant and size tier) and draw "
            "silhouettes measured from the real geometry - trunk height, crown bottom/top and a "
            "six-band radius profile. Umbrellas stay flat, conifers taper, giants are taller. Distant "
            "forests are thinned by a hash rather than a fixed stride, so there are no empty stripes. "
            "`docs/screenshots/v6.24-trees-and-lod.png` shows real voxels beside their LOD silhouette "
            "(headless render, not a GPU screenshot).\n\n"
            "**Entity shadows are no longer big rectangular boxes.** Each mob casts one soft radial "
            "billboard facing the sun. The pass is anchored to the camera and sun angle the shadow map "
            "was built with - previously it used the live camera, so shadows drifted up to 12 blocks "
            "as you walked. Shadows still re-stamp every frame; with Live Entity Shadows off they are "
            "stamped once per refresh instead of disappearing.\n\n"
            "Also fixed: the lopsided disc loop, hole-riddled crown shells, conifer tip gaps, detached "
            "jungle side lobes, clipped birch crowns, LOD's unrelated decorative forest and missing "
            "static mob shadows.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with the seven new V6.24 regression groups; strict equivalence "
            "still matches the original Update 3.5 terrain/world/tick fingerprints; all 21 GLSL "
            "programs parse; `tools/verify_release.py` now also checks `bc/Lod.class` in the packaged "
            "jar/exe. The previews are headless renders of the real geometry - no real-GPU/Windows "
            "playtest is claimed.\n"
        )
    if version == "6.25":
        release_notes += (
            "\n### Trees rebuilt as real trees, and they fall over\n\n"
            "**V6.24's geometric domes are gone.** Every tree now grows a 6-connected log "
            "skeleton - a trunk that may lean, primary branches and twigs - with separate "
            "irregular leaf bunches hanging on those twigs. Visible trunks, branch gaps, "
            "lumpy asymmetric crowns, whorled conifers, flat acacia umbrellas and tall "
            "clean jungle boles. All 34 silhouettes, three size tiers and eight species "
            "remain; LOD silhouettes are re-measured from the new geometry. "
            "`docs/screenshots/v6.25-trees.png` is a headless render of the real voxel "
            "geometry, not a GPU screenshot.\n\n"
            "**`/treefall` now works on all of them.** Four real bugs were fixed: leaning "
            "trunks jumped diagonally and disconnected the crown; a conifer whorl above "
            "the trunk top floated; 2x2 trunk bases pinned the whole tree upright; and the "
            "falling planner's 160-log / 700-leaf / 4-step limits were far too small. "
            "Elbow logs anchor every lean, branches anchor their origin, a genuine 2x2 "
            "base falls with the tree (a lone log on a stone still does NOT fall), and the "
            "limits are 480 logs / 1400 leaves / 6 leaf steps / 18-block span. The headless "
            "chop test fell every species at Normal and Giant size with no trunk left "
            "standing.\n\n"
            "### ONE editable tilesheet with ALL 1024 textures\n\n"
            "`tilesheet.png` beside Blockcraft.exe is a single 1024x1024 PNG: cell "
            "(row, col) is atlas tile row*32+col. Edit it in Aseprite keeping the canvas "
            "size and alpha, save, then type **`/tilesheet`** or press **Options > "
            "Resource Packs > Tilesheet** - your edits apply immediately at full "
            "32x32 resolution. `/tilesheet export` regenerates the master plus indexes. "
            "Only cells that differ from built-in art are applied, so an untouched sheet "
            "is a complete no-op; animated tiles are left alone so water/lava/portal keep "
            "animating. Indexes: `TILESHEET.tsv`, `tilesheet-index.html` (searchable), "
            "`tilesheet-map.png` (2x numbered map), and **TILESHEET.txt** in the game folder. "
            "Developer import: `python3 tools/tilesheet.py import <edited.png>`.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with the six new V6.25 regression groups and the "
            "updated falling-tree tests; strict equivalence still matches the original "
            "Update 3.5 fingerprints; all 21 GLSL programs parse; `tools/verify_release.py` "
            "now also checks `bc/Tilesheet.class` and `bc/Trees.class`. No real-GPU/Windows "
            "playtest is claimed.\n"
        )
    if version == "6.26":
        release_notes += (
            "\n### LOD pops and random crashes\n\n"
            "**LOD tiles now fade in.** A tile used to appear the instant its mesh finished "
            "uploading, so walking or turning produced hard pops as distant chunks snapped "
            "into view. New tiles blend in over 18 frames using per-tile constant-alpha "
            "blending (works with and without the LOD shader; at full opacity the output "
            "is unchanged). The outermost 10% of the LOD horizon also fades out, so CPU "
            "culling never cuts off a visible tile, and a 6% hysteresis band around every "
            "split distance stops tiles flickering between detail levels.\n\n"
            "**Random crashes hardened.** LOD upload buffers are sized from their real byte "
            "length (no more BufferOverflowException on a partial mesh); a rejected worker "
            "task can no longer leak an inflight slot and freeze all LOD requests; evicted "
            "tiles are marked retired so a late worker cannot queue or draw a deleted VBO; "
            "failed uploads, failed per-tile draws and a failed LOD render are contained and "
            "self-heal instead of killing the game; entity-shadow mob iteration uses a "
            "snapshot taken BEFORE glBegin so a concurrent mob change cannot throw; and "
            "every uncaught exception from ANY thread is now written to "
            "`%USERPROFILE%\\.blockcraft112\\crash.log` with the thread name and full "
            "stack trace so any remaining crash is diagnosable.\n\n"
            "V6.25 branch-skeleton trees, falling trees and the single editable tilesheet "
            "are unchanged. Headless verification: six new V6.26 regression groups "
            "including a four-thread LOD build stress test, strict equivalence, all 21 "
            "GLSL programs, and the packaged exe/jar check. No real-GPU/Windows playtest "
            "is claimed.\n"
        )
    if version == "6.27":
        release_notes += (
            "\n### The old resource-pack texture editing works again\n\n"
            "**Root cause found and fixed.** V6.25 added the single editable tilesheet, but "
            "it compared each sheet cell against the CURRENT render atlas. With a resource "
            "pack active, the atlas holds your edited art while `tilesheet.png` holds the "
            "default art, so an UNTOUCHED sheet silently overwrote every pack edit. That is "
            "why `Edit-textures.bat` / `tiles/0003_stone.png` / Reload PNGs appeared to do "
            "nothing. Reproduced headlessly: tile 3 was `FFFF00FF` (pack edit) before the "
            "overlay and `FF878984` (default stone) after it.\n\n"
            "The game now ships a **pristine reference** `tilesheet-default.png` beside "
            "`tilesheet.png`. Only cells that differ from that reference are applied, so an "
            "untouched sheet is a true no-op and per-tile PNGs, `atlas.png` and animation "
            "strips all work again. A cell you actually edit in the tilesheet still wins. "
            "Priority: built-in art -> resource pack -> your edited tilesheet cells. "
            "`import` never overwrites the reference.\n\n"
            "Also: PNG overrides now apply to every pack kind (including Vibrant / Retro / "
            "classic base), **Reload PNGs** prints how many overrides were loaded, and "
            "`/tilesheet` reports edited cells + pack overrides + the active pack id, so a "
            "mis-selected pack is obvious.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with five new V6.27 regression groups (including the "
            "exact reproduction and its inverse), strict equivalence, all 21 GLSL programs, "
            "and `tools/verify_release.py` now also checks `bc/ResourcePacks.class`. No "
            "real-GPU/Windows playtest is claimed.\n"
        )
    if version == "6.32":
        release_notes += (
            "\n### Caves rebuilt to Minecraft density\n\n"
            "The V6.31 carver hollowed out **19% of the ground within 20 blocks of the "
            "surface** and opened 10% of all surface columns, so the world was basically "
            "non-existent. `caveVer = 6` (new worlds only) is a new original carver that "
            "matches Minecraft 1.18/1.20 **density and shape**: an intact 12-block surface "
            "crust, thin spaghetti/noodle worm tunnels, sparse clustered cheese chambers, "
            "rare giant halls, local aquifers and lava lakes. Only a strict noise gate may "
            "break through the crust, producing rare, differently shaped entrances whose "
            "funnel always connects to a real cave below. Headless 96x96 land sample: "
            "**5.0% carved overall, 1.2% near the surface, 40 surface-open columns "
            "(0.43%) and 40/40 entrances connected** - versus 16.1% / 19.3% / 930 for V6.31. "
            "No Mojang code or assets are used.\n\n"
            "### Structures rebuilt like Minecraft\n\n"
            "V6.31's huge rotundas and 20-block corridors are gone. `CaveStructures6` builds "
            "six small, recognisable structures on a 128-block grid: **mineshaft** (3-wide "
            "plank corridors, oak supports, torches, iron track, loot chest), **dungeon** "
            "(small cobble/mossy room, spawner, chests), **stronghold** (stone-brick rooms, "
            "bookshelf library, obsidian portal frame), **amethyst geode** (spherical "
            "obsidian/purpur shell with a hollow crystal interior), **ancient city** "
            "(dark-prismarine floor, sea lantern) and **ruined portal** (obsidian frame, "
            "cracked-brick rubble). Each is deterministic across chunk seams and gets a "
            "short tunnel into a real cave.\n\n"
            "### Verification\n\n"
            "Headless unit suite with four new V6.32 regression groups (density, entrances, "
            "structures and release wiring), strict equivalence, all 21 GLSL programs and "
            "`tools/verify_release.py`. `docs/screenshots/v6.32-caves.png` is a headless "
            "generated preview, not a GPU screenshot. No real-GPU/Windows playtest is claimed.\n"
        )
    if version == "6.31":
        release_notes += (
            "\n### LOD popping fixed at the root\n\n"
            "Tile uploads, rebuilds and LOD level changes now **cross-fade**: the old mesh stays "
            "fully opaque while the new one blends in over 18 frames, a coarse fallback is kept "
            "for the whole fade-in, and the split decision became a continuous 28% distance band "
            "that only exposes children once all four meshes exist. V6.26 only faded newly "
            "uploaded tiles; the one-frame swap on rebuild/split was still a pop.\n\n"
            "### The trailer is now a montage\n\n"
            "V6.30 was one long camera flythrough. It is now **24 hard cuts in exactly 60 "
            "seconds** with eight camera moves (glide, dolly, orbit, crane, dive, static pan, "
            "top-down, chase), eight different locations, big word cards, storms, night, a "
            "four-act soundtrack, real CC0 foley and the block-built logo finale. "
            "`Watch Trailer (1:00)` on the title screen or `/trailer`.\n\n"
            "### Borderless fullscreen, cleaner menus\n\n"
            "**Options > Video > Borderless** (or `/borderless`) makes an undecorated "
            "desktop-sized window: looks fullscreen, Alt-Tab and overlays keep working. It is "
            "saved in `options.txt` and applied at startup. The title screen is now five clear "
            "buttons with everything secondary behind **More...**.\n\n"
            "### Caves and structures completely reworked (new worlds only)\n\n"
            "`caveVer = 5` has **no built cave entrances at all**. An all-new cheese / spaghetti "
            "/ noodle / aquifer carver runs up to the terrain surface and breaks through "
            "wherever two 2D noise fields allow, so openings are irregular craters and ragged "
            "slits of completely different shapes, all leading into a real cave. Eight new "
            "underground structures (mineshaft, geode cathedral, ancient hall, lava foundry, "
            "mushroom grotto, flooded cistern, crystal vault, dungeon) come with rotundas, side "
            "chambers, corridors, pillars, lights, loot and a tunnel into the cave network. "
            "Old saves keep their old caves. `docs/screenshots/v6.31-caves.png` and "
            "`v6.31-trailer.png` are headless generated previews, not GPU screenshots.\n\n"
            "### Verification\n\n"
            "Headless unit suite with seven new V6.31 regression groups, strict equivalence, "
            "all 21 GLSL programs, `--trailer-plan` and `tools/verify_release.py`. No real-GPU "
            "or Windows playtest is claimed.\n"
        )
    if version == "6.30":
        release_notes += (
            "\n### One-minute cinematic trailer, in game\n\n"
            "**Watch Trailer (1:00)** on the title screen, or **`/trailer`** while playing. "
            "This is not a video file: eight scripted camera moves fly through a real freshly "
            "generated world (fixed seed) with 2.39:1 letterboxing, hard cuts, title cards, a "
            "dawn-to-night-to-dawn time-lapse, a forced thunderstorm, a four-act soundtrack and "
            "foley stings from the real CC0 WAV recordings. It runs **exactly 60 seconds**, is "
            "skippable with any key or click, and restores the screen, camera, daylight, weather, "
            "colour grading and options afterwards - including from inside a live world. "
            "`java -jar blockcraft.jar --trailer-plan` prints the deterministic shot list with no "
            "window or GPU; `docs/screenshots/v6.30-trailer.png` is a headless storyboard, not a "
            "GPU screenshot. No Mojang assets are used.\n\n"
            "### Verification\n\n"
            "Headless unit suite with five new V6.30 regression groups (exact 60-second shot table, "
            "camera/terrain maths, in-game start/update/skip/restore, wiring and release files), "
            "strict equivalence, all 21 GLSL programs, `--trailer-plan` smoke test and "
            "`tools/verify_release.py`. No real-GPU/Windows playtest is claimed.\n"
        )
    if version == "6.29":
        release_notes += (
            "\n### Normal walk-in cave entrances (new worlds only)\n\n"
            "V6.28's entrances were long diagonal cracks. `caveVer = 4` now grows **wide, "
            "irregular round cave mouths** with a stone lip and a **solid sloped floor** you "
            "walk down from the grass into a chamber; the deep end keeps a rock roof so it "
            "reads as an arch. Every mouth has a horizontal gallery, a shaft and a straight "
            "tunnel that is placed on a block the noise carver really hollowed: a headless "
            "scan of 47 entrances near spawn found **47/47 linked** to the cave network. "
            "The nine 64-block cells around world spawn always get an entrance, and the old "
            "noise sinkholes, vertical shafts and most ravines are switched off so the "
            "surface no longer looks like endless cracks. V6.28 worlds keep `caveVer = 3` "
            "and are never rewritten. `docs/screenshots/v6.29-caves.png` is a headless "
            "generated cross-section, not a GPU screenshot.\n\n"
            "### Real recorded sounds instead of generated tones\n\n"
            "All eight procedural V6.28 tones were **removed**. Ten world cues are now real "
            "CC0 1.0 field/foley recordings: **tree_fall, tree_crash, tree_creak, "
            "leaf_rustle, wind_gust, cave_drip, cave_wind, cave_rumble, geode_chime, "
            "stone_crumble**. They ship as 22,050 Hz mono 16-bit WAVs in `sounds/foley/` "
            "next to the exe AND inside `blockcraft.jar`; the game reads loose WAVs first, "
            "so you can replace any recording with your own. `sounds/foley/CREDITS.txt` "
            "lists every recordist, Freesound link and the CC0 licence. No Mojang audio "
            "is used. Tree falls/crashes still fire from `Trees`, leaves still rustle on "
            "break, and cave ambience now also picks wind gusts.\n\n"
            "### Verification\n\n"
            "Headless unit suite with five new V6.29 regression groups (cave version, walk-in "
            "geometry, cave-network links, real WAV decoding and release wiring), strict "
            "equivalence, all 21 GLSL programs, and `tools/verify_release.py` now checks "
            "that every foley WAV is identical loose and inside the jar. No real-GPU/Windows "
            "playtest is claimed.\n"
        )
    if version == "6.28":
        release_notes += (
            "\n### 1.20-style caves with real entrances\n\n"
            "**New worlds only** (`caveVer = 3`; old saves are untouched). Cheese caverns now "
            "reach y=8..72 and use two overlapping large-scale noise fields for multi-room "
            "chambers, plus a third very long spaghetti tunnel scale that joins separate "
            "pockets into continuous systems. In a headless 6x6-chunk scan the carved volume "
            "went from 32,490 to 52,790 blocks (+62%), with more aquifer water and lava "
            "lakes. Lush / dripstone / ice / amethyst / deep-dark biomes and geodes remain.\n\n"
            "**Actual cave entrances**, not just the endless ravine cracks or tiny sinkholes: "
            "hashed 96-block regions grow a wide irregular surface mouth with a stone lip, a "
            "shallow sloped ramp down through the rock, a rounded bottom chamber and a low "
            "gallery that joins the underground network. Trees and the world spawn are kept "
            "out of the openings. `docs/screenshots/v6.28-caves.png` is a headless generated "
            "cross-section (old carver above, new entrance below); it is not a GPU screenshot.\n\n"
            "### More sounds\n\n"
            "Eight original procedural samples (no Mojang audio): **tree_fall** (creak + canopy "
            "whoosh when a tree topples, audible to 48 blocks), **tree_crash** (thud, wood crack, "
            "settling leaves), **leaf_rustle** when breaking leaves, and positional cave "
            "ambience - **cave_drip, cave_wind, cave_rumble, geode_chime, stone_crumble** - "
            "while under a rock ceiling. Aliases: tree_creak, water_drip, cave_ambience.\n\n"
            "### Verification\n\n"
            "Headless unit suite passes with five new V6.28 regression groups (entrance "
            "continuity, cavern volume, new samples and hooks), strict equivalence, all 21 "
            "GLSL programs, and the packaged exe/jar check. No real-GPU/Windows playtest is "
            "claimed.\n"
        )
    with open(os.path.join(OUT, "release-notes.md"), "w", encoding="utf-8") as f:
        f.write(release_notes)
    with open(os.path.join(OUT, "SHA256SUMS.txt"), "w", encoding="utf-8") as f:
        for name in sorted(glob.glob(os.path.join(OUT, "*.zip"))):
            f.write(hashlib.sha256(open(name, "rb").read()).hexdigest() + "  " + os.path.basename(name) + "\n")
    with open(os.path.join(OUT, "VERSION"), "w") as f:
        f.write(version)

    for name in sorted(os.listdir(OUT)):
        p = os.path.join(OUT, name)
        h = hashlib.sha256(open(p, "rb").read()).hexdigest()[:16]
        print("[release] %-44s %8.2f MB  sha256 %s" % (name, os.path.getsize(p) / 1e6, h))
    print("[release] OK - Update %s, full zip has %d files, self-check passed" % (version, n_files))


if __name__ == "__main__":
    main()
