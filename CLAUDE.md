# Working on Oh Heck!

Read `ARCHITECTURE.md` first: it maps the modules, the table's state machine, saving,
the screenshot run, CI, the arena and the art tools.

## Conventions

- **Keep it simple (KISS):** write the simplest code that does the job. A plain method or
  class beats a new interface, pattern or layer; add an abstraction only when two real uses
  need it today, not for a future that may not come. Delete what isn't used, and prefer
  fewer files and fewer hops between them to clever generality. When a change makes the
  code simpler, that's a reason to make it.
- **Branches:** work on `feature/…` or `refactor/…` (CI previews the screenshots), then
  merge into the current `develop/alpha-X.Y.Z` (CI publishes a dev build). Don't commit
  straight to `develop/` or `main`.
- **Packages:** follow the map in `ARCHITECTURE.md`. A package may use those above it in
  the map, never one below (no cycles); anything may use `ohheck.util` and `ohheck.anim`. `ohheck.rules`, `ohheck.players`, `ohheck.records`,
  `ohheck.util` and `ohheck.anim` stay libGDX-free, so they run in plain JUnit. Put new game logic and timing
  there, or in small gdx-free classes like `GameEnd`, and test it.
- **Screens depend on `GameHost`**, not `OhHeckGame`; helpers that draw take a `Painter`,
  not a screen; table parts read `TableScene` and act through `TableActions`; taps find
  buttons in `OverlayTargets`. A menu screen with categories uses `SideTabs`.
- **Saved data has no backward compatibility:** `SavedGame`, `Stats`, `Match.save` and high
  scores read only their current format. Anything unreadable (an older save included) is
  reported through `Log` and set aside. Test that each format reads back what it writes.
- **Player ids are permanent.** Look players up with `Player.byId`. A new player gets the
  next unused id.
- **AI changes:** measure them in the arena (`players`, `levels`) before and after, and
  keep each level's win rate in its band. Don't make Easy harder.
- **Game loop and motion:** screens implement `update(step)` and `draw()`, never `render`;
  timing goes in `update`, so it runs in fixed steps. Name each movement as a `Tween`
  (`ohheck.anim`), and move table cards by setting `CardSprites` targets. Don't make new
  strings or objects every frame: use `Numbers` and cached lines.
- **Comments:** only one `/* */` block at the top of a class, saying plainly what the
  class is for, and only when its name doesn't already say it. No comments inside a
  class: no `//` notes, no section dividers, no commented-out code. If code seems to
  need a comment, rename or restructure it until it doesn't; a rule the code can't show
  (a saved format's order, a platform limit) goes in the class comment.
- **No javadoc:** never write `/** */`, `{@link}`, `{@code}`, `@param` or HTML in comments.
  Name things so the signature says what a method takes and returns.
- Generated files (`BidTable.java`) are exempt from the comment rules.
- **Text and spacing:** the game font has no Unicode minus sign. Use "-".
- **Generated files:** `BidTable.java` comes from the arena's `learn`; the images, fonts and
  launcher icons come from `python3 tools/build_art.py` (`--check` to compare). Regenerate
  them; don't edit by hand.
- **Facts shown on screen** (scores, odds, player names, hand counts) come from the rules
  and the roster, not copied text.
- **Extending:** a new difficulty is a `Difficulty` constant, a new habit a `Style`
  constant, a rule a field of `RuleSet` (in its code) with a toggle on the Game page, a preset a
  `GameMode` constant, and a new reaction
  to table events a `TableFlow.Listener`, a new table state a `TableState` constant, a pause-menu choice a `MenuItem` constant, a setting an `OptionItem` on its `OptionsPage`
  (a new category is one more page), and a screenshot-run stop a
  step in `ScreenshotLauncher.Walk`. Code playing a game asks `match.rules()`.
- **Seats:** you are `Seating.YOU`; who sits in the other seats is a `Seating`, not an
  `int[]`.

## Checking changes

**Ship every finished change without asking:** once a request's change is verified, merge
it into the current `develop/` and publish the dev build (the `ship` skill), then report
the build number. Once the build is out and the request is done, send a push notification
naming the build and what it brings (one line, e.g. "dev-0094 ready: Poker Face mode").

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
