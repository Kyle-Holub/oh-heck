# Oh Heck!

A trick-taking card game (also known as *Oh Hell*): bid the exact number of tricks you'll take, then take exactly that many. You play against three computer opponents over 8 hands of 1 to 8 cards.

Built with [libGDX](https://libgdx.com/) for Android and desktop. The original 2017 Java Swing version is preserved on the `release/alpha-1.0.6` branch.

## Playing on Windows

### Easiest: download the game

1. **Install Java.** Download the **JDK 21** installer (`.msi`) for Windows from [adoptium.net](https://adoptium.net/temurin/releases/?os=windows&package=jdk) and run it. On the *Custom Setup* screen, set **Associate .jar** and **Add to PATH** to "Will be installed on local hard drive". You only do this once.
2. **Download the game.** Open this repository's [Releases](../../releases) page, expand **Assets** under the newest **Dev build**, and download `oh-heck-desktop.jar`.
3. **Double-click `oh-heck-desktop.jar`.**

If Windows opens the jar with another program, right-click it and choose *Open with → OpenJDK Platform binary*, or open PowerShell in your Downloads folder and run:

```powershell
java -jar oh-heck-desktop.jar
```

### From the source code

1. Install Java as in step 1 above, and [Git for Windows](https://git-scm.com/download/win).
2. In PowerShell:

   ```powershell
   git clone https://github.com/Kyle-Holub/oh-heck.git
   cd oh-heck
   .\run.bat
   ```

   You can also double-click `run.bat` in File Explorer. The first run downloads Gradle and the game's libraries, which takes a few minutes; later runs start in seconds.

On macOS or Linux, run `./gradlew lwjgl3:run` instead.

## Playing on Android

Every push to a `develop/*` branch builds the app and publishes it as a "Dev build" pre-release. To install it and get updates automatically:

1. Install [Obtainium](https://github.com/ImranR98/Obtainium/releases).
2. Add the app with this repository's URL and turn on **Include prereleases**.
3. Install **Oh Heck!**. Obtainium offers each new build as an update.

## Controls

- **Bidding:** tap or click a number.
- **Playing:** the cards you may play are raised. Tap or click one to play it, or drag it onto the table.
- **Menu:** the Menu button, Android's back button or Esc pauses the game. Games in progress are saved, so you can continue later.

## Development

```sh
./gradlew core:test        # rules, computer players, game engine and saved games (thousands of simulated games)
./gradlew lwjgl3:run       # run on desktop
./gradlew lwjgl3:dist      # build/dist/oh-heck-desktop.jar
./gradlew android:assembleDebug   # needs the Android SDK; skipped automatically without one
```

GitHub runs the tests on every push. Pushes to `develop/*` also build the APK and desktop jar, and play a scripted game whose screenshots are attached to the dev build.

| Path | Contents |
| --- | --- |
| `core/src/main/java/rules` | Game rules, scoring, computer player decisions, the game engine and saving. No graphics |
| `core/src/main/java/ohheck` | Screens: title with 3D falling cards, card table, rules, options; sound and settings |
| `core/src/test/java/rules` | Tests for the rules package |
| `android` | Android launcher, icon and manifest |
| `lwjgl3` | Desktop launcher, plus the scripted screenshot run used on GitHub |
| `assets` | Images, music, sound effects, fonts and the rules text |
| `tools/make_fonts.py` | Bakes the fonts in `assets/fonts` |

All artwork, music and sound effects are by Kyle Holub. Fonts: DejaVu Sans and Serif (see `assets/fonts/DejaVu-LICENSE.txt`).
