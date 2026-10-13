@echo off
cd /d "%~dp0"
echo Checking the game embedded INSIDE this folder's Blockcraft.exe...
"%~dp0jre\bin\java.exe" -jar "%~dp0Blockcraft.exe" --build-info
if errorlevel 1 echo Build verification failed. See the message above.
pause
