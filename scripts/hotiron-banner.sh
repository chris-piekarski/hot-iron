#!/bin/sh
# Shared HotIron wordmark for make help / make info. Keep in sync with
# scripts/hotiron-banner.txt and the README fence.
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TXT="$DIR/hotiron-banner.txt"
TAG="  heat on the dial -- agents copy the RF bins"
if [ ! -f "$TXT" ]; then
	exit 0
fi
if [ -t 1 ]; then
	printf '\033[1;33m'
	cat "$TXT"
	printf '\033[0m\n\033[0;36m%s\033[0m\n' "$TAG"
else
	cat "$TXT"
	printf '\n%s\n' "$TAG"
fi
printf '\n'
