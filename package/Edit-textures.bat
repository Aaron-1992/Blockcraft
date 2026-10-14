@echo off
setlocal
cd /d "%~dp0"
if exist "resourcepacks\MyTextures\pack.properties" goto open
if exist "resourcepacks\MyTextures" (
  echo MyTextures already exists but has no pack.properties. Nothing was overwritten.
  echo Back it up or rename it before exporting a new pack.
  pause
  exit /b 1
)
"%~dp0jre\bin\java.exe" -Djava.awt.headless=true -jar "%~dp0blockcraft.jar" --export-textures "resourcepacks\MyTextures"
if errorlevel 1 (
  echo Texture export failed. See the message above. No existing pack was overwritten.
  pause
  exit /b 1
)
:open
start "" "%~dp0resourcepacks\MyTextures"
if exist "resourcepacks\MyTextures\index.html" start "" "%~dp0resourcepacks\MyTextures\index.html"
echo Edit PNGs in Aseprite, then use Options - Resource Packs - Reload PNGs.
echo Select My Aseprite Textures the first time. See Texture-editing.txt for details.
pause
