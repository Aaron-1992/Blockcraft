#!/usr/bin/env python3
"""Package the generated native world; no downloaded or generated renderer images are implied."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[2]
BUNDLE = ROOT / "build/arcade-bundle"
WORLD = BUNDLE / "Block_Drop_V69"
assert (WORLD / "raster.dat").read_bytes()[:4] == b"RAS1"
assert len(list(WORLD.glob("c.*.*.gz"))) == 49
assert (BUNDLE / "FallingBlocks.asm").read_bytes() == (ROOT / "package/FallingBlocks.asm").read_bytes()
shutil.copyfile(ROOT / "package/Raster-guide.txt", BUNDLE / "Raster-guide.txt")
(BUNDLE / "START-HERE.txt").write_text("""BLOCK DROP - LAMP ARCADE AND MODULAR DISPLAYS
Requires Blockcraft V6.9 or later. Install/update the GAME first.

1. Extract this WHOLE zip. Keep Block_Drop_V69 next to Install-world.bat.
2. Close Blockcraft. Run Install-world.bat and confirm with Y.
   It refuses to replace any existing Block_Drop_V69 world.
3. Launch the V6.9 game and open Singleplayer:
   'Block Drop - Lamp Arcade and Modular Displays'. Use view distance 5+.
4. Empty-hand right-click the keypad just to your LEFT on the glass deck.
   Press ENTER to start. Arrows move/rotate, Down soft-drops, Space hard-drops,
   P pauses, Enter restarts, Escape leaves controls. Mouse can still look around.

Manual install: copy the Block_Drop_V69 folder into
  %USERPROFILE%\\.blockcraft112\\saves\\
On Linux: ~/.blockcraft112/saves/
Do not copy only the chunk files: raster.dat holds the ROM, RAM and screens.
Do not open this world in V6.8 or older. Back up saves before updating.

The huge screen uses 448 real redstone lamps. The smaller color screen uses
28 connected panels: 4x4 pixels each, 16x28 combined. Both are driven by the
same editable ROM program and separate processor/RAM/GPU blocks.

The saved game starts PAUSED with an empty board. Full instructions, exact
hardware coordinates and the assembly instruction set are in Raster-guide.txt.
FallingBlocks.asm is the actual program. No hidden Tetris controller is used.

If you already installed this world and want a fresh copy, rename or back up
the existing save folder yourself; the installer deliberately never overwrites it.
""", encoding="utf-8")
installer = r'''@echo off
setlocal
cd /d "%~dp0"
set "DEST=%USERPROFILE%\.blockcraft112\saves\Block_Drop_V69"
if not exist "Block_Drop_V69\level.properties" goto missing
if not exist "Block_Drop_V69\raster.dat" goto missing
if exist "%DEST%" goto exists
echo Requires Blockcraft V6.9 or later. Close the game before installing.
echo This copies the world to:
echo %DEST%
choice /C YN /N /M "Install this NEW world? [Y/N] "
if errorlevel 2 exit /b 0
mkdir "%DEST%"
if errorlevel 1 goto failed
xcopy "Block_Drop_V69\*" "%DEST%\" /E /I /H /K /Y
if errorlevel 1 goto failed
echo Installed. Open Singleplayer in Blockcraft V6.9.
echo Right-click the keypad on your left, then press ENTER.
pause
exit /b 0
:missing
echo Extract the WHOLE zip first; the native world files are missing.
pause
exit /b 1
:exists
echo Not installed: that world already exists. Nothing was replaced.
echo Back up or rename the existing world if you want a fresh copy.
pause
exit /b 1
:failed
echo Copy failed. Check permissions and available disk space.
echo A partial NEW folder may remain; no existing world was overwritten.
pause
exit /b 1
'''
(BUNDLE / "Install-world.bat").write_bytes(installer.replace("\n", "\r\n").encode("ascii"))
hashes = {str(p.relative_to(BUNDLE)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(WORLD.iterdir()) if p.is_file()}
(BUNDLE / "World-SHA256.json").write_text(json.dumps(hashes, indent=2) + "\n")
out = ROOT / "Blockcraft-Block-Drop-World.zip"
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for p in sorted(BUNDLE.rglob("*")):
        if p.is_file():
            i = zipfile.ZipInfo(str(p.relative_to(BUNDLE)), (2026, 1, 1, 0, 0, 0))
            i.compress_type = zipfile.ZIP_DEFLATED
            i.external_attr = 0o644 << 16
            z.writestr(i, p.read_bytes())
with zipfile.ZipFile(out) as z:
    assert z.testzip() is None
    assert len([n for n in z.namelist() if n.endswith(".gz")]) == 49
    for n, digest in hashes.items():
        assert hashlib.sha256(z.read(n)).hexdigest() == digest
print(out.name, out.stat().st_size, "bytes", hashlib.sha256(out.read_bytes()).hexdigest())
