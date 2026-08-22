#!/bin/bash
set -euo pipefail
DIRECTORY=$(cd -- "$(dirname -- "$0")" && pwd)
MIN_JAVA=21

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	JAVA_BIN="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
	JAVA_BIN=$(command -v java)
else
	echo "No copy -- Java $MIN_JAVA+ not found on this rig." >&2
	echo "Install a headful JDK, e.g.: sudo apt install openjdk-21-jdk" >&2
	exit 1
fi

JAVA_VER=$("$JAVA_BIN" -version 2>&1 | awk -F[\".] '/version/ {print ($2=="1"?$3:$2); exit}')
if [ -z "${JAVA_VER:-}" ] || [ "$JAVA_VER" -lt "$MIN_JAVA" ]; then
	echo "No copy -- Java $MIN_JAVA+ required (found: $("$JAVA_BIN" -version 2>&1 | head -1))." >&2
	echo "Install a headful JDK, e.g.: sudo apt install openjdk-21-jdk" >&2
	exit 1
fi

if echo "${JAVA_TOOL_OPTIONS:-} ${JDK_JAVA_OPTIONS:-}" | grep -q 'java.awt.headless=true'; then
	echo "This rig needs a headful JDK (java.awt.headless is set)." >&2
	exit 1
fi

JAVA_HOME_DIR=$("$JAVA_BIN" -XshowSettings:properties -version 2>&1 | awk -F= '/java.home/ {gsub(/^[ \t]+|[ \t]+$/,"",$2); print $2; exit}')
if [ -n "$JAVA_HOME_DIR" ] && [ ! -e "$JAVA_HOME_DIR/lib/libawt_xawt.so" ] && [ ! -e "$JAVA_HOME_DIR/lib/amd64/libawt_xawt.so" ]; then
	echo "This rig needs a headful JDK (no libawt_xawt in $JAVA_HOME_DIR)." >&2
	echo "Install e.g.: sudo apt install openjdk-21-jdk   (not openjdk-21-jdk-headless)" >&2
	exit 1
fi

cd "$DIRECTORY"
if [ -z "${PULSE_SERVER:-}" ] && [ -S /mnt/wslg/PulseServer ]; then
	export PULSE_SERVER=unix:/mnt/wslg/PulseServer
fi

print_banner() {
	f=""
	for cand in \
		"$DIRECTORY/hotiron-banner.txt" \
		"$DIRECTORY/../../../../scripts/hotiron-banner.txt"
	do
		if [ -f "$cand" ]; then
			f="$cand"
			break
		fi
	done
	[ -n "$f" ] || return 0
	if [ -t 1 ]; then
		printf '\033[1;33m'
		cat "$f"
		printf '\033[0m\n\033[0;36m  heat on the dial -- agents copy the RF bins\033[0m\n\n'
	else
		cat "$f"
		printf '\n  heat on the dial -- agents copy the RF bins\n\n'
	fi
}
print_banner

exec "$JAVA_BIN" -Djna.platform.library.path=lib/linux-x86-64 -jar "$DIRECTORY/lib/hotiron.jar" "$@"
