#!/bin/bash
# Plays bots against each other and compares the results with another commit's.
#   tools/dev/arena.sh                 this tree against develop's newest commit
#   tools/dev/arena.sh <ref>           this tree against <ref> (a branch, tag or commit)
#   tools/dev/arena.sh <ref> quick     fewer games (players 20, levels 60): a smoke check
# The arena is seeded, so code that leaves the bots alone gives identical results; a bot
# change shows exactly what moved. Exit status 0 if identical, 1 if they differ.
source "$(dirname "$0")/env.sh" || exit 1
REF=${1:-$(cd "$ROOT" && git for-each-ref --sort=-committerdate --format='%(refname:short)' 'refs/heads/develop/*' 'refs/remotes/origin/develop/*' | head -1)}
P=60; L=200; [ "$2" = quick ] && { P=20; L=60; }

run_arena() { # run_arena <classes> <out file>
	(cd "$ROOT" && java -cp "$1:$CP" ohheck.players.Arena players $P && java -cp "$1:$CP" ohheck.players.Arena levels $L) 2>&1 \
		| grep -v -E '^Picked up|^\([0-9.]+ s\)$' > "$2"
}

echo "Arena: this tree against $REF (players $P, levels $L)..."
compile_tree "$ROOT" "$DEV/arena-new" > /dev/null || { echo "This tree doesn't compile"; exit 1; }
WT="$DEV/arena-ref-tree"
(cd "$ROOT" && git worktree remove --force "$WT" 2>/dev/null; git worktree add -q --detach "$WT" "$REF") || exit 1
compile_tree "$WT" "$DEV/arena-ref" > /dev/null || { echo "$REF doesn't compile"; exit 1; }
(cd "$ROOT" && git worktree remove --force "$WT")
run_arena "$DEV/arena-ref" "$DEV/arena-ref.txt" &
run_arena "$DEV/arena-new" "$DEV/arena-new.txt"
wait
if diff "$DEV/arena-ref.txt" "$DEV/arena-new.txt" > "$DEV/arena-diff.txt"; then
	echo "ARENA IDENTICAL to $REF"
	exit 0
fi
echo "ARENA DIFFERS from $REF (< $REF, > this tree):"
cat "$DEV/arena-diff.txt"
exit 1
