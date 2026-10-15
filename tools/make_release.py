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
            expected_tiles = 545 if tuple(map(int, version.split("."))) >= (6, 7) else 532
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
            "Earlier releases remain available unchanged. SHA256SUMS.txt checks all downloads.\n" % (545 if tuple(map(int, version.split("."))) >= (6, 7) else 532, kit))
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
