@echo off
title Blockcraft V4.0 Dedicated Server
cd /d "%~dp0"

if exist "runtime\bin\java.exe" (
    set "JAVA_EXE=runtime\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

if not "%~1"=="" (
    "%JAVA_EXE%" -Xmx2G -cp "blockcraft.jar;lib\*" bc.ServerMain %*
    goto :eof
)

echo ================================================================
echo  Blockcraft V4.0 Dedicated Server - World Creation Setup
echo ================================================================
echo  Select World Type / Game Mode:
echo    [1] Default Overworld (28 Biomes, Villages, Caves)
echo    [2] Skyblock (Floating Island, Cobble Gen, Void Survival)
echo    [3] Skywars (8 Spawn Islands + Central Feast Arena)
echo    [4] Bedwars (4 Team Bases, Beds, Iron/Gold/Diamond/Emerald Gens)
echo    [5] Superflat
echo    [6] Amplified Mountains
echo    [7] Large Biomes
echo    [8] Classic / Infdev
echo ----------------------------------------------------------------
set "WTYPE_CHOICE=1"
set /p "WTYPE_CHOICE=Choose World Type [1-8, default=1]: "

set "WTYPE=default"
if "%WTYPE_CHOICE%"=="2" set "WTYPE=skyblock"
if "%WTYPE_CHOICE%"=="3" set "WTYPE=skywars"
if "%WTYPE_CHOICE%"=="4" set "WTYPE=bedwars"
if "%WTYPE_CHOICE%"=="5" set "WTYPE=flat"
if "%WTYPE_CHOICE%"=="6" set "WTYPE=amplified"
if "%WTYPE_CHOICE%"=="7" set "WTYPE=large"
if "%WTYPE_CHOICE%"=="8" set "WTYPE=classic"

set "GMODE=survival"
set /p "GMODE=Game Mode [survival/creative/adventure, default=survival]: "

set "WSEED=12345"
set /p "WSEED=World Seed [default=12345]: "

set "SPORT=25565"
set /p "SPORT=Server Port [default=25565]: "

set "WW_CHOICE=Y"
set /p "WW_CHOICE=Enable Worldwide Access (UPnP + Public Tunnel, no port forwarding) [Y/n]: "
set "WW_FLAG=--worldwide"
if /i "%WW_CHOICE%"=="n" set "WW_FLAG=--lan"

echo.
echo Starting Blockcraft V4.0 Server (%WTYPE%, %GMODE%, port %SPORT%, %WW_FLAG%)...
"%JAVA_EXE%" -Xmx2G -cp "blockcraft.jar;lib\*" bc.ServerMain %SPORT% --type %WTYPE% --mode %GMODE% --seed "%WSEED%" %WW_FLAG%
pause
