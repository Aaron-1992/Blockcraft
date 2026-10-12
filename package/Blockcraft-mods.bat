@echo off
cd /d "%~dp0"
if exist "jre\bin\javaw.exe" (
    start "" "jre\bin\javaw.exe" -Xmx1G -cp "blockcraft.jar" bc.ModManagerMain %*
) else (
    start "" javaw -Xmx1G -cp "blockcraft.jar" bc.ModManagerMain %*
)
