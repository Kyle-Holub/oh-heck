---
name: verify
description: Check a change to Oh Heck! before calling it done - compile and run the tests, compare the bots in the arena if game logic or AI moved, and review the screenshots if anything on screen moved. Use after every change, before committing a finished piece of work.
---

# Verify a change

Run the checks that fit what changed, and report what they printed (not just "passed").

1. **Always: tests.** `tools/dev/test.sh` (add `all` for the slow BotTest too, after any
   change in `ohheck.players` or `ohheck.rules`). It compiles everything without Gradle
   and fetches libGDX itself. `TESTS PASSED` or stop and fix.
2. **Rules, players or anything a bot reads changed: the arena.**
   `tools/dev/arena.sh` compares this tree with develop's newest commit.
   - A change that shouldn't touch the bots must print `ARENA IDENTICAL`.
   - An AI change will differ: check each level's win rate stays in its band and that
     Easy didn't get harder (CLAUDE.md). Quote the before/after lines.
3. **Anything drawn changed: screenshots.** Commit and push the feature branch (add a
   `[shots: state,state]` line to the commit message for a quick run), wait for the Preview
   run (`gh run watch`), then `python3 tools/dev/shots.py review`.
   - `same` and `random` need nothing.
   - Open every `CHANGED` picture in `build/dev/shots/changed/` and say whether it is the
     change you meant. An unexpected one is a bug - fix it before going on.
   - Open `build/dev/shots/phone-view.png` for anything about size, contrast or
     readability: it is the table at a phone's size, which is what the player sees.
4. **Saved formats changed:** `SavedGameTest`, `StatsTest`, `HighScoresTest` and `SaveTest`
   show the new format reads back what it writes (older formats are not read at all).
5. `python3 tools/dev/imports.py` before committing (tidies imports).
