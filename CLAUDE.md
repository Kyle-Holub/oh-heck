# Working on Oh Heck!

Read `ARCHITECTURE.md` first: it maps the modules, the table's state machine, saving,
the screenshot run, CI, the arena and the art tools.

## Conventions

- **Branches:** work on `feature/…` or `refactor/…` (CI previews the screenshots), then
  merge into the current `develop/alpha-X.Y.Z` (CI publishes a dev build). Don't commit
  straight to `develop/` or `main`.
- **Packages:** follow the map in `ARCHITECTURE.md`. A package may use those below it,
  never one above (no cycles). `ohheck.rules`, `ohheck.players`, `ohheck.records`,
  `ohheck.util` and `ohheck.anim` stay libGDX-free, so they run in plain JUnit. Put new game logic and timing
  there, or in small gdx-free classes like `GameEnd`, and test it.
- **Screens depend on `GameHost`**, not `OhHeckGame`; helpers that draw take a `Painter`,
  not a screen; table parts read `TableScene` and act through `TableActions`.
- **Saved formats must keep reading their older versions**, with a test for each:
  `SavedGame`, `Stats`, `Match.save`, high scores.
- **Player ids are permanent.** Look players up with `Player.byId`. A new player gets the
  next unused id.
- **AI changes:** measure them in the arena (`players`, `levels`) before and after, and
  keep each level's win rate in its band. Don't make Easy harder.
- **Game loop and motion:** screens implement `update(step)` and `draw()`, never `render`;
  timing goes in `update`, so it runs in fixed steps. Name each movement as a `Tween`
  (`ohheck.anim`), and move table cards by setting `CardSprites` targets. Don't make new
  strings or objects every frame: use `Numbers` and cached lines.
- **Text and spacing:** the game font has no Unicode minus sign. Use "-".
- **Generated files:** `BidTable.java` comes from the arena's `learn`; the images, fonts and
  launcher icons come from `python3 tools/build_art.py` (`--check` to compare). Regenerate
  them; don't edit by hand.
- **Facts shown on screen** (scores, odds, player names, hand counts) come from the rules
  and the roster, not copied text.
- **Extending:** a new difficulty is a `Difficulty` constant, a new habit a `Style`
  constant, a rules variant a `RuleSet` subclass listed in `RuleSet.ALL`, and a new reaction
  to table events a `TableFlow.Listener`, a new table state a `TableState` constant with
  its `StateView`. Code playing a game asks `match.rules()`, not
  `Rules`.

## Checking changes

- All tests run locally with plain `javac` and JUnit: compile `core/src/main`, `core/src/test`
  and `core/src/arena` against the JUnit jars plus libGDX, which you can take from the desktop
  jar a dev build publishes (its `com/` and `org/` folders). The arena then runs as
  `java ohheck.players.Arena players 60`.
- UI changes: push a feature branch and look at the preview's screenshots. For a quick
  look, add a commit-message line like `[shots: options,tutorial]`.
- If a change alters the scripted game, the screenshot seed may need replacing (see
  "Randomness" in `ARCHITECTURE.md`).
