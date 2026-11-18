@echo off
REM Builds and runs Oh Heck! from the source code. Needs Java (JDK 17 or newer).
REM The first run downloads Gradle and the game libraries, which takes a few minutes.
cd /d "%~dp0"
where java >/dev/null 2>nul
if errorlevel 1 (
  echo Java was not found. Install a JDK 17 or newer from https://adoptium.net/
  echo and make sure "Add to PATH" is selected, then run this again.
  pause
  exit /b 1
)
call gradlew.bat lwjgl3:run
if errorlevel 1 pause
