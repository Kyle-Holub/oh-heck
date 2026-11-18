# Working on Oh Heck!

Read `ARCHITECTURE.md` first: it maps the modules, the table's state machine, saving,
the screenshot run, CI, the arena and the art tools.

## Conventions

- **Branches:** work on `feature/…` or `refactor/…` (CI previews the screenshots), then
  merge into the current `develop/alpha-X.Y.Z` (CI publishes a dev build). Don't commit
  straight to `develop/` or `main`.
- **Rules code stays libGDX-free** (`core/src/main/java/rules`), so it runs in plain JUnit.
  Put new game logic and timing there, or in small gdx-free classes like `GameEnd`, and
  test it.
- **Saved formats must keep reading their older versions**, with a test for each:
  `SavedGame`, `Stats`, `Match.save`, high scores.
- **Player ids are permanent.** Look players up with `Player.byId`. A new player gets the
  next unused id.
- **AI changes:** measure them in the arena (`players`, `levels`) before and after, and
  keep each level's win rate in its band. Don't make Easy harder.
- **Text and spacing:** the game font has no Unicode minus sign. Use "-".
- **Generated files:** `BidTable.java` comes from the arena's `learn`; the images, fonts and
  launcher icons come from `python3 tools/build_art.py` (`--check` to compare). Regenerate
  them; don't edit by hand.
- **Facts shown on screen** (scores, odds, player names, hand counts) come from the rules
  and the roster, not copied text.
- **Extending:** a new difficulty is a `Difficulty` constant, a new habit a `Style`
  constant, a rules variant a `RuleSet` subclass listed in `RuleSet.ALL`, and a new reaction
  to table events a `TableFlow.Listener`. Code playing a game asks `match.rules()`, not
  `Rules`.

## Checking changes

- Rules tests run locally with plain JUnit: compile `core/src/main/java/rules` and
  `core/src/test/java/rules`.
- UI changes: push a feature branch and look at the preview's screenshots. For a quick
  look, add a commit-message line like `[shots: options,tutorial]`.
- If a change alters the scripted game, the screenshot seed may need replacing (see
  "Randomness" in `ARCHITECTURE.md`).
