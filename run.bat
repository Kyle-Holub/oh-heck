@echo off
REM Build and run Oh Heck! on Windows. Requires a JDK (11+).
setlocal
cd /d "%~dp0"

if exist bin rmdir /s /q bin
mkdir bin
dir /s /b src\*.java > sources.txt
javac -d bin -cp "libraries/*" @sources.txt
set BUILD_ERR=%ERRORLEVEL%
del sources.txt
if not %BUILD_ERR%==0 (
  echo Build failed.
  pause
  exit /b %BUILD_ERR%
)
java -cp "bin;resources;libraries/*" game.main.Main
