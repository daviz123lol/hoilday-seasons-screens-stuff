@echo off
rem Builds both jars into .\dist. Needs a JDK 25 (JAVA_HOME or PATH). First run downloads Minecraft: be patient.
setlocal
cd /d "%~dp0"
set ATM11_NEO=26.1.2.109
set LATEST_NEO=26.1.2.126
if exist dist rmdir /s /q dist
mkdir dist
for %%V in (%ATM11_NEO% %LATEST_NEO%) do (
  echo === Building for NeoForge %%V ===
  call gradlew.bat clean build -Pneo_version=%%V || exit /b 1
  copy /y build\libs\festivescreens-neoforge-%%V-*.jar dist\ >nul
)
echo Done. Jars are in .\dist
