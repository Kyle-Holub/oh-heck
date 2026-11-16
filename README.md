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

- **IntelliJ IDEA:** open the folder; `oh-heck.iml` already marks `src` and `resources` as source roots and adds the jars in `libraries/`.
- **Eclipse:** *File → Import → Existing Projects into Workspace*, then choose this folder.

Run `game.main.Main`.

## Project layout

| Path | Contents |
| --- | --- |
| `src/game` | Window, game loop and screens (menu, play, rules, options, game over) |
| `src/core` | Cards, deck, players (human and AI) and round scoring |
| `src/util` | Input handling, audio (`JukeBox`) and helpers |
| `resources` | Images, music, sound effects and the rules text, loaded from the classpath |
| `libraries` | MP3 playback libraries (JLayer, MP3SPI, Tritonus) |
