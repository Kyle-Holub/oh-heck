# Oh Heck!

A trick-taking card game (also known as *Oh Hell*): bid the exact number of tricks you'll take, then take exactly that many. You play against three computer opponents over 8 hands of 1 to 8 cards.

The game exists in two versions that share the same rules code:

- **Phone and desktop (libGDX)**, in `gdx/`: the current version. Android test builds are published automatically on every push.
- **Original desktop (Java Swing, 2017)**, in `src/`: the original game, still runnable. The untouched 2017 version is kept on the `release/alpha-1.0.6` branch.

## Playing on Android

Every push to a `develop/*` branch builds the app and publishes it as a "Dev build" pre-release on GitHub. To install and get updates automatically:

1. Install [Obtainium](https://github.com/ImranR98/Obtainium/releases).
2. Add the app with this repository's URL and turn on **Include prereleases**.
3. Install **Oh Heck! (dev)**. Obtainium offers each new build as an update.

Games in progress are saved on the phone, so you can pick up where you left off.

## Running on a computer

Both versions need a Java Development Kit, version 17 or newer, e.g. [Eclipse Temurin](https://adoptium.net/).

**libGDX version** (no other installs needed; the Gradle wrapper downloads what it needs the first time):

```sh
gdx/gradlew -p gdx lwjgl3:run        # macOS / Linux
gdx\gradlew.bat -p gdx lwjgl3:run    # Windows
```

**Original Swing version:** run `./run.sh` (macOS / Linux) or `run.bat` (Windows).

## Tests

The rules, scoring, computer players, game engine and saved games are covered by JUnit tests in `test/rules`, including thousands of simulated games:

```sh
gdx/gradlew -p gdx core:test
```

GitHub runs them on every push, along with a scripted game whose screenshots are attached to each dev build.

## Project layout

| Path | Contents |
| --- | --- |
| `src/rules` | Game rules, scoring, computer player decisions, the game engine and saving. No graphics; shared by both versions |
| `gdx/core` | The libGDX game: title screen with 3D falling cards, card table, rules, options, sound |
| `gdx/android` | Android launcher, icon and manifest |
| `gdx/lwjgl3` | Desktop launcher, plus the scripted screenshot run used on GitHub |
| `gdx/tools` | Script that bakes the fonts in `resources/fonts` |
| `src/core`, `src/game`, `src/util` | The original Swing game's screens, players and input |
| `resources` | Images, music, sound effects, fonts and rules text, used by both versions |
| `libraries` | MP3 playback libraries for the Swing version |
| `test/rules` | Tests for `src/rules` |

All artwork, music and sound effects are by Kyle Holub. Fonts: DejaVu Sans and Serif (see `resources/fonts/DejaVu-LICENSE.txt`).
