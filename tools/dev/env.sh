# Sourced by the other tools/dev scripts: paths, and the classpath for compiling without Gradle.
#
# libGDX comes from the desktop jar of the newest dev build (its com/ and org/ folders),
# fetched once with gh into build/dev/. JUnit 4 and Hamcrest come from a Gradle install or
# the Gradle/Maven caches. Override with GDX_JAR, JUNIT_JAR, HAMCREST_JAR.
# OHHECK_TREE points the scripts at another checkout's sources (the merge gate tests the
# branch being merged that way); their working files stay in this checkout's build/dev.
HOME_TREE=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
ROOT=${OHHECK_TREE:-$HOME_TREE}
DEV="$HOME_TREE/build/dev"
mkdir -p "$DEV"

find_jar() { # find_jar <name pattern>: the first match in the usual places
	for d in ${GRADLE_HOME:+$GRADLE_HOME/lib} /opt/gradle*/lib ~/.gradle/caches/modules-2/files-2.1 ~/.m2/repository /usr/share/java; do
		[ -d "$d" ] && f=$(find "$d" -name "$1" 2>/dev/null | sort | tail -1) && [ -n "$f" ] && { echo "$f"; return; }
	done
}

GDX_JAR=${GDX_JAR:-$DEV/gdx.jar}
if [ ! -f "$GDX_JAR" ]; then
	echo "Fetching libGDX from the newest dev build's desktop jar..." >&2
	repo=$(cd "$ROOT" && git remote get-url origin | sed -E 's#^.*github\.com[:/]##; s#\.git$##')
	id=$(gh api "repos/$repo/releases?per_page=30" --jq '[.[] | select(.draft|not) | select(.tag_name|startswith("dev-"))][0].assets[] | select(.name=="oh-heck-desktop.jar") | .id')
	tmp=$(mktemp -d)
	(gh api -H 'Accept: application/octet-stream' "repos/$repo/releases/assets/$id" > "$tmp/desktop.jar" \
		&& cd "$tmp" && mkdir x && cd x && unzip -q ../desktop.jar 'com/*' 'org/*' && jar cf "$GDX_JAR" com org) \
		|| { echo "Couldn't fetch the desktop jar (set GDX_JAR to a jar with libGDX)" >&2; rm -rf "$tmp"; return 1 2>/dev/null || exit 1; }
	rm -rf "$tmp"
fi
JUNIT_JAR=${JUNIT_JAR:-$(find_jar 'junit-4.*.jar')}
HAMCREST_JAR=${HAMCREST_JAR:-$(find_jar 'hamcrest-core-*.jar')}
[ -n "$JUNIT_JAR" ] && [ -n "$HAMCREST_JAR" ] || { echo "JUnit 4 / Hamcrest jars not found (set JUNIT_JAR, HAMCREST_JAR)" >&2; return 1 2>/dev/null || exit 1; }
CP="$JUNIT_JAR:$HAMCREST_JAR:$GDX_JAR"

# compile_tree <source root> <out dir>: compiles core (main, test, arena) and the desktop launcher
compile_tree() {
	rm -rf "$2" && mkdir -p "$2"
	(cd "$1" && javac -nowarn --release 17 -encoding UTF-8 -cp "$CP" -d "$2" \
		$(find core/src/main/java core/src/test/java core/src/arena/java lwjgl3/src/main/java -name '*.java')) 2>&1 \
		| grep -v '^Picked up JAVA_TOOL_OPTIONS'
	return "${PIPESTATUS[0]}"
}
