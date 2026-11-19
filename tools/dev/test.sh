#!/bin/bash
# Compiles everything and runs the JUnit tests, without Gradle.
#   tools/dev/test.sh            every test but the slow BotTest
#   tools/dev/test.sh all        all of them
#   tools/dev/test.sh Match      only test classes whose name contains "Match"
# Exit status 0 only if it compiles and every test passes.
set -o pipefail
source "$(dirname "$0")/env.sh" || exit 1
OUT="$DEV/classes"
if ! compile_tree "$ROOT" "$OUT" > "$DEV/compile.txt"; then
	grep -E 'error:' -A2 "$DEV/compile.txt" | head -40
	echo "COMPILE FAILED: $(grep -c 'error:' "$DEV/compile.txt") errors"
	exit 1
fi
T=$(cd "$ROOT/core/src/test/java" && find . -name '*Test.java' | sed 's#^\./##; s#\.java$##; s#/#.#g' | sort)
case "$1" in
	all) ;;
	"") T=$(echo "$T" | grep -v BotTest) ;;
	*) T=$(echo "$T" | grep "$1") ;;
esac
cd "$ROOT" && timeout 900 java -cp "$OUT:$CP" org.junit.runner.JUnitCore $T > "$DEV/tests.txt" 2>&1
status=$?
grep -E '^OK|^Tests run|^[0-9]+\) |expected' "$DEV/tests.txt" | head -30
[ $status -eq 0 ] && echo "TESTS PASSED" || echo "TESTS FAILED (full output: build/dev/tests.txt)"
exit $status
