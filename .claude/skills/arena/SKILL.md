---
name: arena
description: Measure an Oh Heck! AI change in the seeded arena - run the bots against develop's, read the win rates against each level's band, check every rule set and the duel, regenerate the bid tables when the odds move, and report before and after. Use whenever bots, bidding, play, Style, Difficulty, RuleSet or anything a bot reads changes, and before shipping such a change.
---

# Measure an AI change in the arena

The arena plays bots against bots on seeded, duplicate deals (seats rotate, so luck cancels
out). Code that leaves the bots alone gives byte-identical results; a bot change shows exactly
what moved. `tools/dev/arena.sh` compiles this tree and a reference commit (develop's newest by
default), runs both, and diffs them. Report the numbers it printed, not just "passed".

## 1. Before you change anything

On the feature branch, before editing, run `tools/dev/arena.sh` once and keep the output
(`build/dev/arena-ref.txt` holds develop's numbers). It must say `ARENA IDENTICAL`; if it
doesn't, something already moved the bots - find out what before going on.

## 2. While iterating

`tools/dev/arena.sh develop/alpha-X.Y.Z quick` (players 20, levels 60) is a smoke check in
about a minute. Use it to compare ideas; don't judge a change on quick numbers alone.

## 3. Judge the change

Run the full checks and read the `<` (develop) and `>` (this tree) lines of each diff:

| Run | Command | What to read |
|---|---|---|
| Roster and levels | `tools/dev/arena.sh` | each player's `wins` vs 3 Normal, and the `Easy`, `Normal`, `Hard (all four)` summaries |
| Every rule set | `tools/dev/arena.sh develop/alpha-X.Y.Z variants` | each level under all 24 rule sets (`== UP/8`, `== UP_AND_DOWN/13/big/blind` ...) plus each champion twist (`== UP/8/momentum`, `== UP/8/pick` ...) |
| Duels | `tools/dev/arena.sh develop/alpha-X.Y.Z duel` | the levels and the roster one-on-one under the duel's rules (only if duels or anything they use changed) |

A chapter's own duel adds its house scoring and twist to the duel rules: pass its code after the count, e.g.
`java -cp "$DEV/arena-new:$CP" ohheck.players.Arena duel 200 UP/5/big/short/momentum/pick` (after `source tools/dev/env.sh`
and `compile_tree "$ROOT" "$DEV/arena-new"`).

Win-rate bands, as wins against three plain Normal players (`players`). These are today's
numbers with some room; tighten them if a level should feel different:

| Level | Band | Today |
|---|---|---|
| Easy (Edmund, Barnaby, Nathaniel, Horatio) | 3-15% | 5-13% |
| Normal (Rupert, Phillip, Cornelius, Oswald) | 14-30% | 17-25% |
| Hard (Wilfred, Virgil, Mortimer, Percival) | 33-52% | 36-47% |

Rules (from CLAUDE.md):
- Every player stays in their level's band, and the levels stay in order (Easy < Normal < Hard).
- **Don't make Easy harder.** An Easy player's wins must not rise, even inside the band.
- A change aimed at one rule set must not drag another out of its band: check `variants`.
- A player's habit (`Style`) should still show: its `exact`, `over`/`under` columns should keep
  their character (Barnaby overbids, Edmund underbids, and so on).

## 4. When the bid odds move

`BidTable.java` and `DuelBidTable.java` are generated; never edit them by hand.
- `./gradlew core:arena -Parena="learn 20000"` regenerates `BidTable.java`.
- `./gradlew core:arena -Parena="learn-duel 20000"` regenerates `DuelBidTable.java`.

Without Gradle: `source tools/dev/env.sh && compile_tree "$ROOT" "$DEV/arena-new" && java -cp
"$DEV/arena-new:$CP" ohheck.players.Arena learn 20000`. Then re-run step 3: the new table is part
of the change.

## 5. After the change

- Bots playing differently can change the scripted screenshot game; if the screenshot run
  stops finding its states, replace the seed (see "Randomness" in `ARCHITECTURE.md`).
- The practice round's hands 3-4 use real bots: `tools/dev/test.sh` must still pass
  `PracticeRoundTest` (the weak hand wins nothing, the dealer always has a barred bid).
- Run `tools/dev/test.sh all` (BotTest is slow but covers the bots).

## 6. Report

In the reply and the commit message, quote the before and after lines that moved (players
and level summaries, and any rule set that left its band), say which bands they sit in, and
whether Easy got harder (it must not). If the change wasn't meant to touch the bots, the whole
report is the line `ARENA IDENTICAL to develop/...`.
