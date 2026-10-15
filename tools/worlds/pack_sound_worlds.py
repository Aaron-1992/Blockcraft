#!/usr/bin/env python3
"""Package verified V6.10 native saves, programs and a non-overwriting installer."""
from pathlib import Path
import hashlib,json,shutil,zipfile
ROOT=Path(__file__).resolve().parents[2]
BUNDLE=ROOT/'build/sound-bundle'
WORLDS={'Shadow_Signal_V610':81,'Block_Drop_Sound_V610':49}
for name,count in WORLDS.items():
    world=BUNDLE/name
    assert (world/'raster.dat').read_bytes()[:4]==b'RAS1'
    assert len(list(world.glob('c.*.*.gz')))==count
assert b'shadow-signal' in (BUNDLE/'Shadow_Signal_V610/media.properties').read_bytes()
for name in ('FallingBlocks-Sound.asm','MediaPlayer.asm'):
    assert (BUNDLE/name).read_bytes()==(ROOT/'package'/name).read_bytes()
for name in ('Sound-machines.txt','Raster-guide.txt','Make-media.py'):
    shutil.copyfile(ROOT/'package'/name,BUNDLE/name)
(BUNDLE/'START-HERE.txt').write_text('''BLOCKCRAFT V6.10 - TWO SOUND MACHINES

Install/update the GAME to V6.10 first. This ZIP contains worlds, not the game.
Extract the WHOLE zip, close Blockcraft, run Install-worlds.bat and confirm Y.
The installer refuses to overwrite either existing destination.
Your V6.9 Block Drop and Computer Lab saves are not replaced.

In Singleplayer, choose:
  Shadow Signal - Original Silhouette Cinema
  Block Drop - Sound Edition
Use view distance 5+. Empty-hand right-click the keypad on your left; ENTER.
Both saves start paused. Mouse still looks around; ESC leaves controls.

SHADOW SIGNAL: Space/P pause, Left/Right seek five seconds, Enter/R restart,
M mute. Original 32-second monochrome animation and synthesized soundtrack;
NOT the Bad Apple!! footage/song. 64x48 panels and 32x24 real lamps. Lamps have
normal redstone afterglow; the panel wall is sharper. Look left/fly closer.

BLOCK DROP: Arrows move/rotate, Down soft-drop, Space hard-drop, P pause,
Enter/R restart, M mute. The supplied ROM generates its own sound effects.

Manual install: copy BOTH native folders into
  %USERPROFILE%\\.blockcraft112\\saves\\  (Windows)
  ~/.blockcraft112/saves/              (Linux)
Keep all files: chunks, raster.dat and media.properties. Do not open in an
older game. Back up saves. Source programs and instructions are included.

For your EXISTING V6.9 Block Drop world: look at a connected component in
Creative and /arcadesound. It resets the supplied game and adds a speaker,
but refuses to overwrite custom ROMs or blocks. See Sound-machines.txt.
''',encoding='utf-8')
installer=r'''@echo off
setlocal
cd /d "%~dp0"
set "BASE=%USERPROFILE%\.blockcraft112\saves"
for %%W in (Shadow_Signal_V610 Block_Drop_Sound_V610) do (
  if not exist "%%W\level.properties" goto missing
  if not exist "%%W\raster.dat" goto missing
  if exist "%BASE%\%%W" goto exists
)
echo Requires Blockcraft V6.10. Close the game before installing.
echo This installs TWO NEW worlds in %BASE%
echo Your existing V6.9 world is not replaced.
choice /C YN /N /M "Install these NEW worlds? [Y/N] "
if errorlevel 2 exit /b 0
for %%W in (Shadow_Signal_V610 Block_Drop_Sound_V610) do (
  mkdir "%BASE%\%%W"
  if errorlevel 1 goto failed
  xcopy "%%W\*" "%BASE%\%%W\" /E /I /H /K /Y
  if errorlevel 1 goto failed
)
echo Installed. Open Singleplayer in Blockcraft V6.10.
echo Right-click the keypad on your left, then ENTER. M mutes. ESC leaves.
pause
exit /b 0
:missing
echo Extract the WHOLE zip first. Native world files are missing.
pause
exit /b 1
:exists
echo Not installed: at least one destination already exists. NOTHING was replaced.
echo Back up or rename that destination yourself before installing a fresh copy.
pause
exit /b 1
:failed
echo Copy failed. Check permissions and disk space. Partial NEW folders may remain.
echo No pre-existing world was overwritten.
pause
exit /b 1
'''
(BUNDLE/'Install-worlds.bat').write_bytes(installer.replace('\n','\r\n').encode('ascii'))
hashes={str(p.relative_to(BUNDLE)):hashlib.sha256(p.read_bytes()).hexdigest() for name in WORLDS for p in sorted((BUNDLE/name).rglob('*')) if p.is_file()}
(BUNDLE/'World-SHA256.json').write_text(json.dumps(hashes,indent=2)+'\n')
out=ROOT/'Blockcraft-Sound-Machines.zip'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for p in sorted(BUNDLE.rglob('*')):
        if p.is_file():
            i=zipfile.ZipInfo(str(p.relative_to(BUNDLE)),(2026,1,1,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o644<<16;z.writestr(i,p.read_bytes())
with zipfile.ZipFile(out) as z:
    assert z.testzip() is None
    assert sum(n.endswith('.gz') for n in z.namelist())==130
    for n,h in hashes.items():assert hashlib.sha256(z.read(n)).hexdigest()==h
print(out.name,out.stat().st_size,'bytes',hashlib.sha256(out.read_bytes()).hexdigest())
