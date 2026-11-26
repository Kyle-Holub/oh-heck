#!/bin/bash
# Plays bots against each other and compares the results with another commit's.
#   tools/dev/arena.sh                 this tree against develop's newest commit
#   tools/dev/arena.sh <ref>           this tree against <ref> (a branch, tag or commit)
#   tools/dev/arena.sh <ref> quick     fewer games (players 20, levels 60): a smoke check
#   tools/dev/arena.sh <ref> variants  the levels across all 24 rule sets
#   tools/dev/arena.sh <ref> duel      the levels and the roster one-on-one under the duel's rules
# The arena is seeded, so code that leaves the bots alone gives identical results; a bot
# change shows exactly what moved. Exit status 0 if identical, 1 if they differ.
source "$(dirname "$0")/env.sh" || exit 1
REF=${1:-$(cd "$ROOT" && git for-each-ref --sort=-committerdate --format='%(refname:short)' 'refs/heads/develop/*' 'refs/remotes/origin/develop/*' | head -1)}
case "$2" in
quick) EXPERIMENTS="players 20,levels 60" ;;
variants) EXPERIMENTS="variants 40" ;;
duel) EXPERIMENTS="duel 200" ;;
*) EXPERIMENTS="players 60,levels 200" ;;
esac

run_arena() { # run_arena <classes> <out file>
	: > "$2"
	IFS=, read -ra RUNS <<< "$EXPERIMENTS"
	for run in "${RUNS[@]}"; do
		(cd "$ROOT" && java -cp "$1:$CP" ohheck.players.Arena $run) 2>&1 | grep -v -E '^Picked up|^\([0-9.]+ s\)$' >> "$2"
	done
}

echo "Arena: this tree against $REF ($EXPERIMENTS)..."
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
