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
./gradlew core:test        # rules, computer players, saved games, the table's flow (thousands of simulated games)
./gradlew lwjgl3:run       # run on desktop
./gradlew lwjgl3:dist      # build/dist/oh-heck-desktop.jar
./gradlew android:assembleDebug   # needs the Android SDK; skipped automatically without one
```

### The computer players' arena

When changing how the computer players bid or play, measure it in the arena: it plays thousands of games in duplicate (each deal four times, the player under test in every seat, so card luck cancels out) and reports points per hand, exact bids by round and win rates. It isn't part of the build or the tests.

```
./gradlew core:arena                         # each level at a table of its own, by round
./gradlew core:arena -Parena="vs 400"        # each level against the one below
./gradlew core:arena -Parena="effort 100"    # Hard's strength and speed at half, normal and double thinking
./gradlew core:arena -Parena="learn 4000"    # regenerate core/src/main/java/rules/BidTable.java
```

The Normal players bid from `BidTable`: how often each card took a trick in simulated games, by hand size, trump or not, rank, how many cards of its suit the holder had, and their place in the bidding. Regenerate it (twice, since each round learns from the play of the last) after changing how they play. Without a computer, the same experiments can be run on GitHub: Actions, then **Arena**, then **Run workflow**; the results appear in the run's summary.

GitHub runs the tests on every push. Pushes to `develop/*` also build the APK (signed with the key in the repository's secrets) and the desktop jar, publish them as a dev build for Obtainium, and play a scripted game whose screenshots are attached to it. Pushes to `refactor/*` or `feature/*` only play the scripted game, attaching the screenshots to a private draft release, to check a change before it ships.

| Path | Contents |
| --- | --- |
| `core/src/main/java/rules` | Game rules, scoring, computer player decisions, the game engine and saving. No graphics |
| `core/src/main/java/ohheck` | Screens (title with 3D falling cards, card table, how to play, options, high scores), sound, settings and effects |
| `.../ohheck/PlayScreen.java` | The card table: touch, and drawing the seats and your hand. With `TableFlow` (its states and timing, no drawing), `TableLayout` (where things go), `DealAnimation`, `TrickView`, `TableOverlays` (panels, scores, menus) and `Autoplay` (the scripted screenshot run) |
| `core/src/test/java` | Tests for the rules, saved games, high scores, the rules text and the table's flow |
| `android` | Android launcher, icon and manifest |
| `lwjgl3` | Desktop launcher, plus the scripted screenshot run used on GitHub |
| `assets` | Images, music, sound effects, fonts and the rules text |
| `tools/` | Scripts that draw the cards, icons and frames (`make_art.py`), the app icon (`make_icon.py`) and bake the fonts (`make_fonts.py`) |

All artwork, music and sound effects are by Kyle Holub. Fonts: DejaVu Sans and Serif (see `assets/fonts/DejaVu-LICENSE.txt`).
