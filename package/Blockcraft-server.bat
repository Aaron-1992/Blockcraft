@echo off
cd /d "%~dp0"
if exist "jre\bin\java.exe" (
    "jre\bin\java.exe" -Xmx2G -cp "blockcraft.jar" bc.ServerMain %*
) else (
    java -Xmx2G -cp "blockcraft.jar" bc.ServerMain %*
)
pause
