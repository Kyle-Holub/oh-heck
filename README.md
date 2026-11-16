# Oh Heck!

A desktop version of the trick-taking card game *Oh Heck* (also known as *Oh Hell*), written in Java Swing. You play against three computer opponents.

## Requirements

- A Java Development Kit (JDK), version 11 or newer, e.g. [Eclipse Temurin](https://adoptium.net/).
  Check with `java -version` and `javac -version`.

## Running

macOS / Linux:

```sh
./run.sh
```

Windows: double-click `run.bat`, or run it from a terminal.

The script compiles `src/` into `bin/` and launches `game.main.Main`.

### Running by hand

```sh
javac -d bin -cp "libraries/*" $(find src -name '*.java')
java -cp "bin:resources:libraries/*" game.main.Main      # use ; instead of : on Windows
```

## Opening in an IDE

No IDE project files are checked in. If you use one, mark `src` and `resources` as source folders, add the jars in `libraries/` to the classpath, and run `game.main.Main`.

## Project layout

| Path | Contents |
| --- | --- |
| `src/game` | Window, game loop and screens (menu, play, rules, options, game over) |
| `src/rules` | Game rules, scoring, computer player decisions and a headless game engine (no graphics) |
| `src/core` | On-screen cards, players and round flow for the desktop game |
| `src/util` | Input handling, audio (`JukeBox`) and helpers |
| `resources` | Images, music, sound effects and the rules text, loaded from the classpath |
| `libraries` | MP3 playback libraries (JLayer, MP3SPI, Tritonus) |

## Tests

The game rules, scoring and computer players live in `src/rules` with no graphics code, and are covered by JUnit 4 tests in `test/rules`, including thousands of simulated computer-only games.

```sh
javac -d build/rules src/rules/*.java
javac -d build/test -cp build/rules:junit-4.13.2.jar:hamcrest-core-1.3.jar test/rules/*.java
java -cp build/rules:build/test:junit-4.13.2.jar:hamcrest-core-1.3.jar org.junit.runner.JUnitCore rules.RulesTest rules.BotTest rules.MatchTest
```
