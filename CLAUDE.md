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
- **Screens depend on `GameHost`** (`Services` and `Navigator`), not `OhHeckGame`; helpers
  that draw take a `Painter`, not a screen; table parts read `TableScene` (the flow as a
  `FlowView`) and act through `TableActions`; taps find buttons in `OverlayTargets`.
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
  its `StateView`, a pause-menu choice a `MenuItem` constant, and a screenshot-tour stop a
  screen implementing `TourStop`. Code playing a game asks `match.rules()`, not `Rules`.
- **Seats:** you are `Seating.YOU`; who sits in the other seats is a `Seating`, not an
  `int[]`.

## Checking changes

Use the `verify` skill after every change and the `ship` skill to merge; the scripts are in
`tools/dev` (see "Developer tools" in `ARCHITECTURE.md`):

- `tools/dev/test.sh` compiles and runs the tests, no Gradle needed.
- `tools/dev/arena.sh` compares the bots with develop's: identical unless you meant to
  change them.
- UI changes: push a feature branch, wait for its Preview, then
  `python3 tools/dev/shots.py review`. Look at every `CHANGED` shot and at `phone-view.png`.
  For a quick run, add a commit-message line like `[shots: options,tutorial]`.
- A merge into `develop/` is gated by a hook (tests, plus a screenshot report with nothing
  unreviewed). Add `[shots-reviewed]` to the merge message only after looking.
- If a change alters the scripted game, the screenshot seed may need replacing (see
  "Randomness" in `ARCHITECTURE.md`).
