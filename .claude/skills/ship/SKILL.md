---
name: ship
description: Take a finished Oh Heck! change from a feature/ or refactor/ branch to a published dev build - verify, push, review the preview screenshots, merge into develop, confirm the APK and desktop jar. Use when the user asks to ship, merge, release or "get it on my phone".
disable-model-invocation: true
---

# Ship a change

1. **On a branch.** Work is on `feature/...` or `refactor/...`, branched from the newest
   `develop/alpha-X.Y.Z`. Never commit to `develop/` or `main` directly.
2. **Verify** with the `verify` skill: tests, the arena if bots could move, and the
   screenshots. Push and let the Preview run finish.
3. **Review the report.** `python3 tools/dev/shots.py review <branch>`. Every `CHANGED`
   shot must be one you meant; look at its picture. Check `phone-view.png` for anything
   the player has to see.
4. **Merge.**
   `git checkout <develop branch> && git pull && git merge --no-ff <branch> -m "Merge <branch>: <what it does>"`
   - The merge gate hook (`tools/dev/hooks/merge_gate.sh`) runs first: it tests the branch
     and reads the preview's `compare.txt`. If screenshots changed it blocks the merge
     until the message has `[shots-reviewed]` - add it only after step 3, and list in the
     message what changed on screen.
   - End the message with the attribution lines the session asks for.
5. **Push and confirm the build.** `git push`, then wait for "Android dev build"
   (`gh run list --branch <develop branch>`) and check that the newest `dev-NNNN` release
   targets the merge commit and has `oh-heck-dev-NNNN.apk` and `oh-heck-desktop.jar`.
6. **Report** in a few lines: the dev build number, what changed for the player, what
   the checks showed (tests, arena, screenshots), and anything left over.
