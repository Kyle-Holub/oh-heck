# How Oh Heck! is put together

A map for anyone changing the game. The README covers playing and building it; this covers
where things live, how they connect, and the tools around them.

## Modules

| Module | What it is |
|---|---|
| `core` | Everything shared: the rules, the computer players and the records (plain Java), and the game itself (libGDX). |
| `android` | The Android launcher, icons, signing. The app the phone runs. |
| `lwjgl3` | The desktop launcher (the Windows jar) and `ScreenshotLauncher`, the scripted screenshot run CI uses. |
| `core/src/arena` | The arena: bots against bots, for measuring AI changes. Not part of the game. |
| `tools` | Python scripts that generate the art, fonts and icons in `assets/`. |

## Packages (`core/src/main/java/ohheck`)

Each package uses only those above it in this list, never one below, so there are no
cycles; `ohheck.util` and `ohheck.anim` depend on nothing, and anything may use them. The
first six have no libGDX and test in plain JUnit.

| Package | What's in it |
|---|---|
| `ohheck.rules` | Cards, `Match`, `Trick`, `Move`, `MatchCodec`, `RuleSet`, `HandOrder`, `GameMode`, `TableView` |
| `ohheck.players` | The roster (`Player`), who sits where (`Seating`), levels (`Difficulty`), habits (`Style`), the bots, `BidTable`; the arena lives here too |
| `ohheck.campaign` | The campaign: `Chapter` (mode, table rivals, duel champion), `Campaign` (progress, saved), `TrickShot`, `TrickShots` and `TrickShotPlay` (puzzles), `Badge` and `Feats` (what a game earns), `Cosmetic` (card backs and felts won), `PracticeRound` and `PracticeCoach` (the scripted round that teaches a new player, dealt with `Match.scripted`) |
| `ohheck.records` | `SavedGame`, `Stats`, `HighScores`, and `Records`, which keeps them and the campaign's progress |
| `ohheck.util` | `Log`, where code without a screen reports trouble it recovered from |
| `ohheck.anim` | Motion over time: `Ease` (the curves), `Tween`, `Phases`, `Timeline`, and `FixedSteps` (the game loop's clock) |
| `ohheck.platform` | The device: `Settings` (preferences) and `RecordPrefs` (where the records are kept), `Audio`, `GameOptions` (how a launcher runs the game) |
| `ohheck.ui` | What every screen shares: `BaseScreen`, `Painter`, `MenuChrome`, `Assets`, `Palette`, `CardArt`, `PostProcessor`, `Wording`, `Numbers`, `Scroller`, `TapAreas`, `SideTabs` (the tab column of Options and Records) and `GameHost` |
| `ohheck.fx` | Effects: `Sparks`, `Fireworks`, `Motes`, `Glints`, `FallingCards` |
| `ohheck.table` | The table screen and its parts |
| `ohheck.screens` | Title, Options, Roster, High scores, the campaign and trick shot screens, and the screenshot tour |
| `ohheck` | `OhHeckGame`: makes everything and connects it; `Screens`, the way between screens |

## The rules and the players (no libGDX)

- `Match` is a whole game: hands, bids, tricks, scores, whose turn. Moves are made with
  `placeBid` and `playCard`, or as `Move`s (`Move.Bid`, `Move.Play`) through `apply`;
  anything illegal throws. `moves()` lists every move made (`ReplayTest` plays them back onto a
  fresh match). Bots answer with a `Move` (`Bot.chooseMove`). A bid not yet made is
  `Match.NO_BID` (`hasBid(seat)`), never null. `save()` / `load()` go through
  `MatchCodec`, its text format.
- `RuleSet` is the rules a game is played by: which way the hands run (`HandOrder`: up, down,
  or up and down), the biggest hand (8, or 13 for a marathon, whose 13-card hand deals the
  whole deck and so has no trump: `trump()` is null, and the table shows a dotted outline),
  whether a made bid scores big (8, plus 4 for the first trick bid, doubling with each trick after), and whether bids are blind (made unseen,
  shown together, no dealer's limit: `Match.bidSeenBy` hides them from the other seats and from
  their bots until bidding ends). It also says which card wins, what may be bid and played, and the score.
  A `Match` holds its rules (`rules()`) and `TableView` carries them to the bots. A rule set is
  a value written as a short code (`UP/8`, `UP_AND_DOWN/13/big`) into saves and high scores.
  `GameMode` names four fixed sets (Classic, High Stakes, Poker Face, Marathon) and Custom, which plays
  the rules the player chose; `GameMode.of(rules)` names any rule set. Turn order is `RuleSet.nextSeat`, and
  `Trick` decides when a trick is complete and who takes it, for `Match` and for the Hard
  bot's imagined games alike. Code without a game in hand asks `RuleSet.STANDARD`.
  `HandSort` how your hand is laid out (trumps first, then by suit or by rank). `BidTable` has rows for every hand size: 1 to 8 learned from standard games, 9 to 13 from games counting down from 13 (whose last-dealt 13-card hand has no trump).
- `TableView` is what a player may know at a moment (`Match.view(seat)`). Bots decide from
  it alone and can only read it. A bot that imagines games (the Hard bot) fills its own
  views with a `TableView.Builder`.
- `Difficulty` holds everything that differs by level: its bot (`bot(rng)`), the
  table as an Options setting (each computer seat's chance of each level, `chanceAt`), and how strongly habits show. A new
  level is one more constant, plus its words for the Options note in `Wording`.
- Bots: `Bot` (`Bot.of(level, rng)`), `EasyBot` (Normal that loses count, slips on bids and plays loosely), `NormalBot`
  (bids the best expected score under the game's rules from the learned `BidTable`, reading
  the earlier bids except when bids are blind; plays with card memory), `HardBot` (Monte Carlo over
  imagined deals). Each `Style` writes its own habit (`bid` and `play` in its constant) and
  `StyledBot` applies it on top of the level's bot. A new habit is one more `Style` constant. Every bot's bid passes through `Bot.neverZeroWithTheTrumpAce`. `BotScenarioTest` asks every player positions with an obvious move (a zero bid never leads an ace, the trump ace is never bid as zero, a made bid ducks when it can...) and checks each habit shows where it should.
- `Player` is the roster of twelve: name, level, style, trait, and a **permanent id**.
  Look players up with `Player.byId`, never by position. Never renumber or reuse an id,
  because saves and statistics refer to players by id. `Player.draw` seats a table, using
  the difficulty setting's odds, as a `Seating`: you in `Seating.YOU`, a player's id in each
  other seat (`at(seat)`), saved as "4,10,1".
- `BidTable` is generated by the arena (`learn`). Don't edit it by hand.

## The game (libGDX)

- `OhHeckGame` makes the shared things (sprite batch, `Assets`, `Audio`, `PostProcessor`,
  `Settings`, `Records`) and the screens. `Screens` keeps the back stack (`open` / `back` /
  `replaceAll`) and brings each new screen up out of a short dark tint (`FADE`); the
  screenshot run leaves the tint out so it captures screens settled.
  Long-lived screens are made once through `kept(...)`; the table (`PlayScreen`) is made per
  game (`PlayScreen.fresh` / `PlayScreen.resume`). Screens don't depend on it: they get a
  `GameHost`, which `OhHeckGame` implements: the shared things and the way to the other
  screens. A
  `FramePacer` drops to 30 fps while nothing moves.
- `BaseScreen` is the base for every screen. Its `render` is the game loop: it moves the
  screen on in fixed steps of `STEP` (1/60 s) by calling `update(step)` as many times as the
  frame's time allows (`FixedSteps` carries the rest to the next frame; a stall counts for
  at most 0.1 s), then calls `draw()` once. So the game plays out the same at any frame rate
  and in CI. Screens implement `update` and `draw`, never `render`; game time is the step
  times the game speed. It also has the 800×480 world stretched to the screen's
  shape, touch handling (`tap`, `drag`, `touchDown/Up`), `stateTag()` for the screenshot
  run, and steps the screenshot tour after each update (timed by `tourClock()`).
- `Painter` holds the drawing tools: the fonts (`ui`, `serif`), text placed and aligned,
  frames and buttons, the gold bid numerals (`numeral`). A screen has one (`paint`);
  helpers that draw for a screen take a `Painter`, not the screen. `MenuChrome` draws the
  settings-style header bar with Back (`header`, `HEADER_H`) and section headings
  (`heading`), for the screens that have them.
- `Assets` loads every texture and font. Each loader registers what it makes, so
  `dispose()` releases everything.
- `Wording` builds the game's sentences (who won, hints, hand results, the statistics rows)
  and is tested in `WordingTest`. Text drawn every frame comes from caches, not new strings:
  `Numbers.text` / `signed` for numbers, `Wording`'s cached ordinals and hand results, and
  each screen keeps lines that only change with the game.
- Motion is described with `ohheck.anim`: a `Tween` is a length, a delay and an `Ease`
  (`OUT`, `IN`, `SMOOTH`...); `Phases` splits a span into named parts; a `Timeline` is a
  scene's clock. Name each movement as a constant tween next to what it moves.

### The table (`ohheck.table`)

- `TableFlow` runs the table's state machine:
  `[VERSUS] → PICK_DEALER → SHUFFLE → DEAL → TURN ⇄ TRICK → HAND_END → RESULTS → (SHUFFLE | GAME_OVER)`;
  a new game opens on `PICK_DEALER` (`DealerPick`: a light runs round the placards and stops on the
  dealer), a resumed one does not; a duel first shows the champion's
  VS screen. A trump that beats an ace holds the trick still for a
  moment (`TableFlow.inHitStop`); `GameEnd` raises the podium (player cards) from last place up before the
  scores, except in a duel; a tap finishes it, and the scores then wait for a tap (Continue or Main menu). A duel wears its chapter's look (`Cosmetic`) while it lasts.
  Screen jolts (shake, zoom) are an `ohheck.anim.Kick` read into `BaseScreen`'s camera; `CardWipe` is the
  curtain of card backs into and out of a campaign table. `PlayScreen.practice` plays the practice round:
  `PracticeGuide` reads the table's moment, asks the `PracticeCoach` for a tip, spotlights what it points at and
  holds the table until it is tapped; in the scripted hands only the planned card or bid can be chosen. Each
  `TableState` constant holds its own behaviour (`entered`, `update`, `skip`) and what the
  screen does in it (the overlay, what a tap does, whether it is still moving, the
  screenshot tag).
  The flow holds the timing, makes moves as `Move`s, and asks the bots through a `Thinker`:
  `Thinker.background()` in play, so a slow Hard bot never stalls a frame (the table waits
  while it thinks), and `Thinker.AT_ONCE` in tests and the screenshot run. It tells its
  `Listener`s when things happen. Each listener
  overrides only what it reacts to: your seat (`YourSeat`), the screen's `Effects`,
  `TableSounds` and `GameRecorder` (statistics and high scores). A new reaction is a new
  listener. It's tested in `TableFlowTest`.
- `YourSeat` is who plays your seat: `YourSeat.PERSON` (your taps) in play, `Autoplay` in
  the screenshot run (the same taps, from a bot). The table asks it the little that
  differs (its clock, the paused menu, whether bots think at once), never which it is.
- `PlayScreen` is the table screen. It holds the game and composes each frame from the
  pieces below. Those pieces don't hold the screen: they read the table through
  `TableScene` and act through `TableActions`
  (your bid and card, skipping on, hints, the ways off the table), both implemented by
  `PlayScreen`. `TableInput` takes its touches: tapping or dragging a card, bidding, moving
  the table on, and the pause menu, whose choices are `MenuItem`s (tested in
  `TableInputTest` against a fake table).
  - `CardSprites`: every card on the table is a `CardSprite` with a pose (place, size,
    turn, face up or down) that it glides to when its target changes, so a card flies from
    a fan or your hand to the trick, a gathered trick goes to its winner, and your hand
    closes up. Each frame `update` sets the targets from the table and `draw` draws them
    (`CardSpriteTest`).
  - `TableDrawing`: counters, trump card, players' panels, where your hand's cards go.
  - `TrickView`: the trick's timings and the winner's glow. `DealAnimation`: the deal's
    timings and the shuffle.
  - `TableOverlays`: bid panel, hand results, game over, the menu, hints. Each panel's
    inner offsets are named next to its size. Each button it draws is placed in
    `OverlayTargets`, which `TableInput` and `Autoplay` ask what a tap hit.
  - `TableLayout`: where everything goes, for the current screen shape (`TableLayoutTest`).
  - `DeckShower` sprays a deck's cards across the screen when you lose; `GameEnd` orders
    the ending (the shower, or fireworks if you won, then the scores) and is tested in
    `GameEndTest`.
- `PlayerPanel` draws a player's placard from a `SeatState` (name, score, bid, tricks,
  turn...). `Counters` draws the hand tally (one mark per hand, in gates of five) and the
  bid tracker. `BidPopup` is the "Make Your Bid" panel, drawn the same at the table and in trick shots;
  `ScoreCard` is the end-of-hand results card.

### Other screens (`ohheck.screens`)

`TitleScreen` (with `FallingCards`, the 3D shower; the first Play asks "New to Oh Heck?" and starts the
practice round or the Play menu), `OptionsScreen` (a tab per `OptionsPage`, scrolling when it's long, each a list of
`OptionItem`s: `toggle`, `choice`, `link`, or a custom item like `DifficultyOption`; built twice:
`options` for Preferences and the practice round, and `setup`, the screen before a custom game, whose
Table and Rules pages pick the difficulty and the game mode, where changing any rule makes the
game Custom, and whose header deals the game), `PlayMenuScreen` (where to play, as a row of
cards: Continue, Campaign, Custom game, High scores), `PlayersScreen` (the Roster), `HighScoresScreen` (difficulty tabs down the left filter both the high scores and the
statistics, a switch at the top of the panel picks which, high scores have a button per game
mode, and Reset statistics sits in the header; the panel scrolls, clipped with `BaseScreen.clipTo`).

Facts the screens show come from the rules, not copied text: the Options difficulty note is
`Wording.tableDescription` (from the matchmaking odds), the practice round's points and
hand counts come from `PracticeRound.RULES`, and the tally from the
game's rules.

### The campaign

The title's **Play** opens the Play screen: **Campaign**, **Custom game** (set up, then saved as it
goes), High scores, and Continue when a custom game is in progress.
The campaign (`CampaignScreen`) shows its chapters as a hand of cards; a chapter lays out its
four steps in order: table play (a normal game against the chapter's rivals), trick shots
(`TrickShotScreen`, a fixed deal with every hand face up, a restart and every card gliding
into place; its title and goal pop up on the felt and fade, and the goal stays in the header; the players are
unnamed, each bid shown with the table's icons on a small placard, the one to block outlined), open once the table is won as practice, a final table (the champion sits in for a rival, under the house rules
plus momentum), and a duel against its champion
(`RuleSet.DUEL`: two players, a short deck of nine to ace, five hands, momentum doubling a
bid made after two made running). Winning the duel clears the chapter and wins its card back
and felt (`Cosmetic`, chosen in Preferences); the game-over panel's **Claim prizes** opens
`ChapterClearedScreen`, which turns each prize over in turn (card back, felt, champion's badge)
with fireworks, then puts the new look on. `PlayScreen.campaign` plays a `Stage` (table, final table or duel), each
opened by winning the one before (`Campaign.open`); campaign games are not saved and don't touch the statistics or high scores, but every
game can earn badges (`BadgeKeeper`, `Feats`), shown on Records. A duel seats the champion at
the top (`TableLayout.spot`). A chapter opens with a `Briefing` of a few pages in the
page-by-page style (Back turns back a page; tapping a player shows their habit in a box below); each step opens with one
page of panels, its text in a box beneath and Play in the corner. All are worded from the rules and the roster. The
campaign opens on chapter 1's briefing the first time. A new player learns in the practice round (above); finished
from the first Play, it turns into the Chapter 1 card and opens the campaign. Options, Start fresh forgets everything (preferences,
records, campaign) and replays the intro, as for a new player. The plan the campaign follows is `docs/CAMPAIGN.md`.

### Saving

`Settings` wraps the device's preferences: the options and whether you know how to play. Kept in the
same preferences by `RecordPrefs`: the saved game (`SavedGame`, one text), statistics and
high scores (`HighScores`, stored by field name, ten kept per game mode). Statistics are
kept per difficulty only, in measures that hold for any rules (places, bids made, points a
hand), so every mode counts toward them.
`Records` keeps statistics, high scores and the saved game behind one persist switch,
writing to a `Records.Store` (`RecordPrefs` on the device); it has no libGDX and is tested in
`RecordsTest` with an in-memory store. `Stats` lists each saved number once (`FIELDS`);
`StatsTest` checks every statistic is saved and comes back.
Each format reads only its current version (`SavedGameTest`, `StatsTest`, `HighScoresTest`,
`SaveTest`); anything else, an older save included, is reported through `Log` and set aside.

## Randomness

`PlayScreen` uses separate random streams for seating, the bots and the deck
(`TableStreams`). A change that draws more random numbers from one stream leaves the
others alone, so the screenshot run's seeded game stays the same game. If a change does
alter that game (new players, new bot behaviour), find a new seed where the scripted player
wins (so the fireworks show) by replaying the rules with the same streams, and update
`ScreenshotLauncher`.

## The screenshot run

`lwjgl3/src/main/java/ohheck/lwjgl3/ScreenshotLauncher.java` plays a seeded game with a bot
in your seat (`Autoplay`), at fixed time steps, and saves a picture the first time each
wanted state appears:

- `WANTED` lists state prefixes, as screens report them in `stateTag()` (for example
  `options`, `tip:trump`, `bid:3`, `gameover`).
- `settleFrames` sets how many frames to wait in a state before capturing it.
- `ScreenshotLauncher.Walk` moves the run from screen to screen as a timed script: title,
  high scores, statistics, options, roster, the practice round's first tips, then the game. Each step
  waits in a state for its time, then does one thing (open a screen, scroll, next page).
- Each captured frame is 0.1 s of game time (`fixedStep`), which the screens take as six
  steps of 1/60 s, exactly as on a device.
- `./gradlew lwjgl3:screenshots -Pshots=options,tip` captures only those and stops.
  On CI, add a line starting `[shots: options,tip]` to the commit message.

## CI (`.github/workflows`)

All four share `.github/actions/setup` (JDK 17, the Gradle cache, and xvfb for the
screenshot run).

| Workflow | When | What |
|---|---|---|
| `tests.yml` | every push | rules and table tests, and the desktop build |
| `preview.yml` | `feature/**`, `refactor/**` | the screenshot run, compared with the newest dev build's, put on a private draft release `preview-N` (newest three kept) with `compare.txt`, before/after pictures of changed shots and `phone-view.png` |
| `android.yml` | `develop/**` | signed APK published as pre-release `dev-NNNN` (Obtainium picks these up; newest three per branch kept), then the desktop jar and screenshots attached |
| `arena.yml` | by hand only | arena experiments, deliberately kept off the build |

## Developer tools (`tools/dev`, `.claude`)

Scripts that check a change without Gradle (they fetch libGDX from the newest dev build's
desktop jar into `build/dev`, and find JUnit in a Gradle install or cache):

- `test.sh` compiles everything and runs the tests (`all` adds BotTest; a word runs only
  the matching test classes).
- `arena.sh [ref] [quick|variants|duel]` runs the seeded arena on this tree and on `ref` (develop's newest
  by default) and diffs them: identical unless the bots changed.
- `shots.py` fetches screenshots, compares two runs, and makes the phone view. Each shot is
  `same`, `random` (differs only where the run is random: sparkles, fireworks, the title
  shower, the motes round the trump card) or `CHANGED`. `review [branch]` does it all for a
  branch's newest preview against the newest dev build. The phone view shows the table
  shots at the size a phone shows them, for checking that things can be seen.
- `imports.py` tidies imports.
- `hooks/merge_gate.sh` is a Claude Code hook (`.claude/settings.json`): a `git merge` into
  `develop/` or `main` is blocked unless the branch passes its tests and its preview's
  `compare.txt` shows nothing `CHANGED`, or the merge message says `[shots-reviewed]`.

`.claude/skills` holds two skills: `verify` (which checks fit a change) and `ship` (branch
to published dev build).

## Branches and releases

Work happens on `feature/…` or `refactor/…` branches (previewed), and is merged into
`develop/alpha-X.Y.Z` (published as dev builds). A release is a `release/alpha-X.Y.Z` branch
cut from develop, with develop merged into `main`. Then the next `develop/` branch starts,
with `OhHeckGame.VERSION` bumped.

## The arena

`./gradlew core:arena -Parena="<experiment> N [rules]"`, where an optional rules code
(`UP/8/blind`, `UP_AND_DOWN/13/big`) plays the experiment under those rules:

| Experiment | What it does |
|---|---|
| `levels N` | the levels against each other |
| `vs N` | head to head |
| `duel N` | the levels and every roster player one-on-one under the duel's rules |
| `learn-duel N` | regenerates `DuelBidTable.java`, the bid odds for duels, leaving `BidTable` alone |
| `variants N` | Easy, Normal and Hard across all 24 rule sets (progression, 8 or 13 cards, big and blind bids) |
| `learn N` | regenerates `BidTable.java` |
| `effort N` | Hard's strength at half, normal and double thinking |
| `players N` | each of the twelve against three plain Normal players |

Duplicate deals rotate seats, so luck cancels out. Run it after any AI change.

## The art

`python3 tools/make_sounds.py` writes the button press chime (`assets/sfx/press.wav`); every button a
`Painter` draws (and every bid button and open campaign card) plays it on a tap, with a light vibration
unless Preferences turns it off.

`python3 tools/build_art.py` rebuilds everything generated: the images in `assets/images`
(`make_art.py`, with the court cards from `court_art.py`), the bitmap fonts (`make_fonts.py`)
and the launcher icons (`make_icon.py`). `--check` builds into a temp folder and lists
anything that differs from what's committed. It needs `pip install -r tools/requirements.txt`
(pinned, so the output matches byte for byte) and the fonts-dejavu-core and fonts-texgyre
packages. The six hand-lettered word images from the original game (`titleText.png` and
the like) are source files, not generated. Commit the generated files with the change to
the tools.
