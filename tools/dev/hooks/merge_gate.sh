#!/bin/bash
# A Claude Code PreToolUse hook (see .claude/settings.json): before a `git merge` into a
# develop/ branch or main, the branch being merged must
#   1. compile and pass its tests (tools/dev/test.sh, run on that branch), and
#   2. have a preview whose screenshot report (compare.txt, made by CI) shows nothing
#      CHANGED, unless the merge message says [shots-reviewed]: the changes were looked at
#      and are meant.
# Exit 2 blocks the command and tells Claude why. Anything else passes through.
input=$(cat)
cmd=$(printf '%s' "$input" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("command",""))' 2>/dev/null)
printf '%s' "$cmd" | grep -qE '(^|[;&|[:space:]])git[[:space:]]+(-C[[:space:]]+[^[:space:]]+[[:space:]]+)?merge([[:space:]]|$)' || exit 0
printf '%s' "$cmd" | grep -qE 'merge[[:space:]]+--(abort|continue|quit)' && exit 0

ROOT=${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}
cd "$ROOT" || exit 0
into=$(git branch --show-current)
# (the merge may come after a checkout in the same command)
target=$(printf '%s' "$cmd" | grep -oE 'checkout[[:space:]]+(-q[[:space:]]+)?[^[:space:];&]+' | tail -1 | awk '{print $NF}')
[ -n "$target" ] && into=$target
case "$into" in develop/*|main) ;; *) exit 0 ;; esac

# the branch being merged: the last word after "merge" that isn't an option or a message
branch=$(printf '%s' "$cmd" | python3 -c '
import re, shlex, sys
c = sys.stdin.read()
part = re.split(r"\bgit\s+merge\b", c, 1)[1]
part = re.split(r"[;&|]", part, 1)[0]
words = shlex.split(part)
out, skip = [], False
for w in words:
    if skip: skip = False; continue
    if w in ("-m", "-F", "--message", "--file", "-s", "--strategy", "-X"): skip = True; continue
    if w.startswith("-"): continue
    out.append(w)
print(out[-1] if out else "")' 2>/dev/null)
[ -z "$branch" ] && { echo "merge gate: couldn't tell which branch is being merged; name it explicitly" >&2; exit 2; }
sha=$(git rev-parse --verify -q "$branch^{commit}") || { echo "merge gate: no branch $branch" >&2; exit 2; }

# 1. tests, on the branch being merged
tree="$ROOT/build/dev/gate-tree"
git worktree remove --force "$tree" >/dev/null 2>&1
git worktree add -q --detach "$tree" "$sha" || { echo "merge gate: couldn't check out $branch" >&2; exit 2; }
out=$(OHHECK_TREE="$tree" "$ROOT/tools/dev/test.sh" 2>&1)
status=$?
git worktree remove --force "$tree" >/dev/null 2>&1
if [ $status -ne 0 ]; then
	echo "merge gate: $branch doesn't pass its tests, so it can't go into $into:" >&2
	printf '%s\n' "$out" | tail -25 >&2
	exit 2
fi

# 2. the preview's screenshot report
repo=$(git remote get-url origin | sed -E 's#^.*github\.com[:/]##; s#\.git$##')
asset=$(gh api "repos/$repo/releases?per_page=100" --jq "[.[] | select(.draft and .target_commitish==\"$sha\")][0].assets[]? | select(.name==\"compare.txt\") | .id" 2>/dev/null)
if [ -z "$asset" ]; then
	echo "merge gate: no screenshot report for $branch at ${sha:0:8}. Push it and wait for its Preview run (tools/dev/shots.py review $branch), then merge." >&2
	exit 2
fi
report=$(gh api -H 'Accept: application/octet-stream' "repos/$repo/releases/assets/$asset")
changed=$(printf '%s\n' "$report" | grep -c '^CHANGED')
if [ "$changed" -gt 0 ] && ! printf '%s' "$cmd" | grep -q '\[shots-reviewed\]'; then
	echo "merge gate: $branch changes $changed screenshot(s) against the newest dev build:" >&2
	printf '%s\n' "$report" | grep '^CHANGED' >&2
	echo "Look at them (tools/dev/shots.py review $branch writes before/after pictures to build/dev/shots/changed). If every change is meant, merge again with [shots-reviewed] in the merge message and say what changed; if not, fix it." >&2
	exit 2
fi
exit 0
