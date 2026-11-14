#!/usr/bin/env python3
"""Reject mislabeled/stale releases and EXEs with an old embedded game.

python3 tools/verify_release.py releases/Blockcraft-UPDATE6.2-patch.zip 6.2
Checks source fingerprint, class hashes, matching EXE/JAR contents and executes
--build-info from BOTH packaged launchers without starting OpenGL.
"""
import hashlib
import io
import os
import subprocess
import sys
import tempfile
import zipfile

import build as B

REQUIRED = ("Game", "Renderer", "PlayerModel", "Tex", "TexPixel32", "WeaponPose", "BowSprite", "HandLighting", "ShaderSrc", "Shaders")

def required(version):
    if tuple(map(int, version.split("."))) < (6, 3):
        return ("Game", "Renderer", "PlayerModel", "Tex", "TexSmooth", "WeaponPose", "BowModel")
    extra = ("TexRemaining32",) if tuple(map(int, version.split("."))) >= (6, 4) else ()
    if tuple(map(int, version.split("."))) >= (6, 5):
        extra += ("TexMaterials32", "TexItems32", "TexNature32", "PixelArt32")
    if tuple(map(int, version.split("."))) >= (6, 6):
        extra += ("TextureEditor", "ResourcePacks", "UtilityModels", "ChestLids", "TexUtility32", "TexTools32", "TexBuildingReview32", "TexColorNature32", "TexGlazedReview32")
    if tuple(map(int, version.split("."))) >= (6, 8):
        extra += ("Circuit", "CircuitComputer", "Facing", "BlockState", "Shapes", "Mesher", "World", "Net319", "Aquatic", "RedstoneLogic", "WaterPhysics")
    if tuple(map(int, version.split("."))) >= (6, 9):
        extra += ("RasterVM", "RasterComputer", "RasterPrograms", "RasterArcade")
    if tuple(map(int, version.split("."))) >= (6, 10):
        extra += ("RasterAudio", "RasterClip", "RasterMedia", "ShadowMachine", "SoundEngine")
    if tuple(map(int, version.split("."))) >= (6, 11):
        extra += ("MidiCompiler", "NoteBlocks", "Redstone319", "Blocks", "Creative")
    if tuple(map(int, version.split("."))) >= (6, 12):
        extra += ("MidiScreen",)
    if tuple(map(int, version.split("."))) >= (6, 13):
        extra += ("MidiPlayback",)
    if tuple(map(int, version.split("."))) >= (6, 14):
        extra += ("Kinetics", "KineticModels", "KineticMachines", "KineticArt", "KineticWorkshop", "Menu")
    if tuple(map(int, version.split("."))) >= (6, 15):
        extra += ("KineticRender", "KineticTransport", "Gui")
    if tuple(map(int, version.split("."))) >= (6, 20):
        extra += ("Options", "Screens", "Ui", "Panorama", "MapView")
    if tuple(map(int, version.split("."))) >= (6, 21):
        extra += ("CaveWorks", "WeatherFx", "TerraGen", "Sky", "WorldSettings", "WorldPreview")
    if tuple(map(int, version.split("."))) >= (6, 22):
        extra += ("CaveCliffs", "TerraTrees", "MapView", "Adv312", "Gen")
    if tuple(map(int, version.split("."))) >= (6, 24):
        extra += ("Lod",)
    if tuple(map(int, version.split("."))) >= (6, 25):
        extra += ("Tilesheet", "Trees")
    if tuple(map(int, version.split("."))) >= (6, 26):
        extra += ("Main",)
    if tuple(map(int, version.split("."))) >= (6, 27):
        extra += ("ResourcePacks",)
    if tuple(map(int, version.split("."))) >= (6, 30):
        extra += ("Trailer",)
    if tuple(map(int, version.split("."))) >= (6, 31):
        extra += ("Caves5", "CaveStructures5")
    if tuple(map(int, version.split("."))) >= (6, 32):
        extra += ("Caves6", "CaveStructures6")
    if tuple(map(int, version.split("."))) >= (6, 34):
        extra += ("Caves7", "CaveStructures7")
    if tuple(map(int, version.split("."))) >= (6, 35):
        extra += ("Caves8", "CaveStructures8", "Menus635", "Chunk", "Gen")
    if tuple(map(int, version.split("."))) >= (6, 38):
        extra += ("BodyTemp", "Seasons", "Wind")
    return REQUIRED + extra


def properties(raw):
    return dict(line.split("=", 1) for line in raw.decode("utf-8").splitlines() if "=" in line)


def inspect_payload(exe, jar, version, source_hash):
    with zipfile.ZipFile(io.BytesIO(jar)) as j, zipfile.ZipFile(io.BytesIO(exe)) as e:
        if e.testzip() or j.testzip():
            raise ValueError("Corrupt executable/JAR")
        if set(j.namelist()) != set(e.namelist()):
            raise ValueError("Executable contains a different game from the JAR")
        for name in j.namelist():
            if j.read(name) != e.read(name):
                raise ValueError("Executable/JAR disagree: " + name)
        if "bc-build.properties" not in j.namelist():
            raise ValueError("Missing build receipt (old executable)")
        receipt = properties(j.read("bc-build.properties"))
        if receipt.get("version") != version:
            raise ValueError("Packaged version does not match requested release " + version)
        if receipt.get("source.sha256") != source_hash:
            raise ValueError("Update pack is stale: source fingerprint differs; rebuild with tools/build.py --patch")
        for cls in required(version):
            actual = hashlib.sha256(j.read("bc/" + cls + ".class")).hexdigest()
            if actual != receipt.get("class." + cls + ".sha256"):
                raise ValueError("Missing/modified visual update class: " + cls)
        if tuple(map(int, version.split("."))) >= (6, 10):
            if hashlib.sha256(j.read("media/shadow-signal.bcv")).hexdigest() != receipt.get("media.shadow-signal.sha256"):
                raise ValueError("Missing/modified original media cartridge")
        return receipt


def verify(archive, version):
    with zipfile.ZipFile(archive) as z:
        exe, jar = (z.read("Blockcraft/" + n) for n in ("Blockcraft.exe", "blockcraft.jar"))
        receipt = inspect_payload(exe, jar, version, B.source_fingerprint())
        if tuple(map(int, version.split("."))) >= (6, 6):
            with zipfile.ZipFile(io.BytesIO(jar)) as game:
                for name in ("texture-editing.txt", "texture-names.tsv"):
                    with open(os.path.join(B.ROOT, "res", name), "rb") as f:
                        if game.read(name) != f.read():
                            raise ValueError("Stale texture editor resource: " + name)
            guides=("Edit-textures.bat", "Texture-editing.txt", "README.txt")
            if tuple(map(int, version.split("."))) >= (6, 8): guides += ("Computer-guide.txt",)
            if tuple(map(int, version.split("."))) >= (6, 9): guides += ("Raster-guide.txt", "FallingBlocks.asm",)
            if tuple(map(int, version.split("."))) >= (6, 10):
                guides += ("Sound-machines.txt", "FallingBlocks-Sound.asm", "MediaPlayer.asm", "Make-media.py")
                with zipfile.ZipFile(io.BytesIO(jar)) as game:
                    with open(os.path.join(B.ROOT,"res/media/shadow-signal.bcv"),"rb") as media:
                        if game.read("media/shadow-signal.bcv") != media.read(): raise ValueError("Stale original media cartridge")
            if tuple(map(int, version.split("."))) >= (6, 11): guides += ("MIDI-guide.txt",)
            if tuple(map(int, version.split("."))) >= (6, 29):
                # the game must ship the real recorded foley both loose and inside the jar
                for name, data in B.foley_files().items():
                    if z.read("Blockcraft/" + name) != data:
                        raise ValueError("Stale/missing recorded foley: " + name)
                    with zipfile.ZipFile(io.BytesIO(jar)) as game:
                        if game.read(name) != data:
                            raise ValueError("Recorded foley missing from jar: " + name)
            if tuple(map(int, version.split("."))) >= (6, 14): guides += ("Create-foundation.txt", "Create-code-license.txt")
            if tuple(map(int, version.split("."))) >= (6, 30): guides += ("Trailer-guide.txt",)
            for name in guides:
                with open(os.path.join(B.ROOT, "package", name), "rb") as f:
                    if z.read("Blockcraft/" + name) != f.read():
                        raise ValueError("Stale packaged instructions/shortcut: " + name)
    with tempfile.TemporaryDirectory() as tmp:
        for name, data in (("Blockcraft.exe", exe), ("blockcraft.jar", jar)):
            path = os.path.join(tmp, name)
            with open(path, "wb") as f:
                f.write(data)
            result = subprocess.run([B.java_exe(), "-Djava.awt.headless=true", "-jar", path, "--build-info"],
                                    capture_output=True, text=True, timeout=30)
            info = properties(result.stdout.encode("utf-8"))
            expected = {"version": version, "source.sha256": receipt["source.sha256"],
                        "default.texture.tileSize": "32" if tuple(map(int, version.split("."))) >= (6, 3) else "64",
                        "gun.firstPersonYaw": "96.0",
                        "bow.model": "pixel-sprite" if tuple(map(int, version.split("."))) >= (6, 3) else "articulated-3d"}
            if tuple(map(int, version.split("."))) >= (6, 3):
                expected["hand.lighting"] = "single-lightmap"
            if result.returncode or any(info.get(k) != v for k, v in expected.items()):
                raise ValueError("Packaged %s failed runtime check:\n%s\n%s" % (name, result.stdout, result.stderr))
            print("[verify] %s: version=%s, %spx atlas, %s bow" % (name, version, expected["default.texture.tileSize"], expected["bow.model"]))
    print("[verify] source.sha256=" + receipt["source.sha256"])
    return receipt


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: verify_release.py <patch-or-full.zip> <version>")
    verify(sys.argv[1], sys.argv[2])
