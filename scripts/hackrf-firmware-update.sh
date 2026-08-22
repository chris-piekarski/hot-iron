#!/bin/sh
# Download official Great Scott Gadgets firmware and flash the attached HackRF.
# Invoked by `make firmware-update`. Never runs from build/test.
#
#   make firmware-update                         # dry-run (plan only)
#   make firmware-update CONFIRM=1               # flash latest GSG release
#   make firmware-update VERSION=2026.01.3 CONFIRM=1
#   make firmware-update FIRMWARE=/path/to.bin CONFIRM=1
#
# Refuses: no device, not writable, multiple devices, One/Pro mismatch.

set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
HACKRF_C="$ROOT/src/hotiron/lib/hackrf/host/libhackrf/src/hackrf.c"
HACKRF_H_DIR="$ROOT/src/hotiron/lib/hackrf/host/libhackrf/src"
USB_INC="$ROOT/src/hotiron/lib/libusb-1.0.21/include/libusb-1.0"
SPIFLASH_C="$ROOT/src/hotiron/lib/hackrf/host/hackrf-tools/src/hackrf_spiflash.c"
CACHE="$ROOT/src/hotiron/build/firmware-cache"
TOOL="$ROOT/src/hotiron/build/hackrf_spiflash"
LIST_C="$ROOT/src/hotiron/src-c/hackrf_list.c"
HELPER="$ROOT/src/hotiron/build/hackrf-list"

CONFIRM=${CONFIRM:-0}
VERSION=${VERSION:-}
FIRMWARE=${FIRMWARE:-}
SERIAL=${SERIAL:-}

die() {
	echo "firmware-update: $*" >&2
	exit 1
}

read_sysfs() {
	if [ -f "$1" ]; then
		tr -d '\n' < "$1" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
	fi
}

product_name() {
	case "$1" in
	6089) echo "HackRF One" ;;
	604b) echo "Jawbreaker" ;;
	cc15) echo "rad1o" ;;
	*) echo "unknown ($1)" ;;
	esac
}

firmware_basename() {
	case "$1" in
	6089) echo "hackrf_one_usb.bin" ;;
	604b) echo "hackrf_jawbreaker_usb.bin" ;;
	cc15) echo "hackrf_rad1o_usb.bin" ;;
	*) echo "" ;;
	esac
}

# Collect attached HackRFs: pid|node|serial|writable
devices=""
count=0
if [ -d /sys/bus/usb/devices ]; then
	for dir in /sys/bus/usb/devices/*; do
		[ -f "$dir/idVendor" ] || continue
		vendor=$(read_sysfs "$dir/idVendor")
		product=$(read_sysfs "$dir/idProduct")
		[ "$vendor" = "1d50" ] || continue
		case "$product" in
		6089|604b|cc15) ;;
		*) continue ;;
		esac
		bus=$(read_sysfs "$dir/busnum")
		dev=$(read_sysfs "$dir/devnum")
		node=$(printf '/dev/bus/usb/%03d/%03d' "$bus" "$dev")
		usb_serial=$(read_sysfs "$dir/serial")
		writable=no
		[ -e "$node" ] && [ -w "$node" ] && writable=yes
		count=$((count + 1))
		devices="${devices}${product}|${node}|${usb_serial}|${writable}\n"
	done
fi

echo "HackRF firmware update"
echo "======================"
echo

if [ "$count" -eq 0 ]; then
	die "no HackRF USB device enumerated. Attach it first (WSL: usbipd attach --wsl --busid <id>)."
fi

if [ "$count" -gt 1 ] && [ -z "$SERIAL" ]; then
	printf '%b' "$devices" | while IFS='|' read -r pid node ser wr; do
		echo "  $(product_name "$pid")  $node  serial=$ser  writable=$wr"
	done
	die "multiple HackRFs attached; re-run with SERIAL=<usb-serial>"
fi

pid=""
node=""
usb_serial=""
writable="no"
printf '%b' "$devices" | while IFS='|' read -r p n s w; do
	[ -n "$p" ] || continue
	if [ -n "$SERIAL" ] && [ "$s" != "$SERIAL" ]; then
		continue
	fi
	printf '%s\n' "$p|$n|$s|$w"
done > /tmp/hackrf-fw-dev.$$
line=$(head -1 /tmp/hackrf-fw-dev.$$)
rm -f /tmp/hackrf-fw-dev.$$
[ -n "$line" ] || die "no device matched SERIAL=$SERIAL"
pid=${line%%|*}
rest=${line#*|}
node=${rest%%|*}
rest=${rest#*|}
usb_serial=${rest%%|*}
writable=${rest#*|}

bin_name=$(firmware_basename "$pid")
[ -n "$bin_name" ] || die "unsupported product $pid"

echo "  board:     $(product_name "$pid") ($pid)"
echo "  usbfs:     $node"
echo "  serial:    ${usb_serial:-unknown}"
echo "  writable:  $writable"
echo

if [ "$writable" != "yes" ]; then
	echo "  fix:       sudo chmod a+rw $node"
	echo
	if [ "$CONFIRM" = "1" ]; then
		die "usbfs is not writable. Fix udev or: sudo chmod a+rw $node"
	fi
	echo "  (dry-run continues; a real flash will need write permission)"
	echo
fi

if [ -z "$VERSION" ]; then
	VERSION=$(python3 - <<'PY'
import json, os, ssl, urllib.request
url = "https://api.github.com/repos/greatscottgadgets/hackrf/releases/latest"
req = urllib.request.Request(url, headers={
    "Accept": "application/vnd.github+json",
    "User-Agent": "hotiron-firmware-update",
})
token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
if token:
    req.add_header("Authorization", "Bearer " + token)
ctx = ssl.create_default_context()
with urllib.request.urlopen(req, timeout=15, context=ctx) as resp:
    data = json.loads(resp.read().decode("utf-8"))
tag = data.get("tag_name") or ""
print(tag[1:] if tag.startswith("v") else tag)
PY
	) || die "could not look up latest GSG release (set VERSION=2026.01.3)"
fi
VERSION=${VERSION#v}

if [ -n "$FIRMWARE" ]; then
	bin="$FIRMWARE"
	[ -f "$bin" ] || die "FIRMWARE=$bin not found"
else
	mkdir -p "$CACHE"
	zip="$CACHE/hackrf-${VERSION}.zip"
	url="https://github.com/greatscottgadgets/hackrf/releases/download/v${VERSION}/hackrf-${VERSION}.zip"
	if [ ! -f "$zip" ]; then
		echo "  downloading $url"
		curl -fL --retry 3 --retry-delay 2 -o "$zip.partial" "$url" || die "download failed"
		mv "$zip.partial" "$zip"
	else
		echo "  using cached $zip"
	fi
	extract="$CACHE/hackrf-${VERSION}"
	mkdir -p "$extract"
	if [ ! -f "$extract/firmware-bin/$bin_name" ]; then
		unzip -o -q "$zip" "*/firmware-bin/$bin_name" -d "$extract" || die "zip has no firmware-bin/$bin_name"
		# unzip may nest hackrf-VERSION/
		found=$(find "$extract" -name "$bin_name" -type f | head -1)
		[ -n "$found" ] || die "could not extract $bin_name"
		mkdir -p "$extract/firmware-bin"
		if [ "$found" != "$extract/firmware-bin/$bin_name" ]; then
			cp -f "$found" "$extract/firmware-bin/$bin_name"
		fi
	fi
	bin="$extract/firmware-bin/$bin_name"
fi

# Safety: never write the much-larger Pro image onto One / Jawbreaker / rad1o.
size=$(wc -c < "$bin")
if [ "$pid" != "pro" ] && [ "$size" -gt 120000 ]; then
	die "$bin is ${size} bytes — looks like HackRF Pro firmware; refusing to flash $(product_name "$pid")"
fi
case "$bin" in
*hackrf_pro*) die "refusing to flash Pro firmware onto $(product_name "$pid")" ;;
esac

echo "  target:    GSG v$VERSION"
echo "  image:     $bin ($size bytes)"
echo "  sha256:    $(sha256sum "$bin" | awk '{print $1}')"
echo "  docs:      https://hackrf.readthedocs.io/en/latest/updating_firmware.html"
echo

build_tool() {
	command -v gcc >/dev/null 2>&1 || die "gcc required to build hackrf_spiflash"
	[ -f "$SPIFLASH_C" ] && [ -f "$HACKRF_C" ] || die "hackrf sources missing (git submodule update --init src/hotiron/lib/hackrf)"
	if [ -x "$TOOL" ] && [ "$TOOL" -nt "$SPIFLASH_C" ] && [ "$TOOL" -nt "$HACKRF_C" ]; then
		return 0
	fi
	mkdir -p "$(dirname "$TOOL")"
	if gcc -O2 -o "$TOOL" "$SPIFLASH_C" "$HACKRF_C" \
		-I"$HACKRF_H_DIR" -I"$USB_INC" -pthread \
		-DLIBRARY_VERSION='"firmware-update"' -DLIBRARY_RELEASE='"firmware-update"' \
		-lusb-1.0 >/dev/null 2>&1; then
		return 0
	fi
	[ -f /lib/x86_64-linux-gnu/libusb-1.0.so.0 ] || die "cannot link libusb-1.0"
	gcc -O2 -o "$TOOL" "$SPIFLASH_C" "$HACKRF_C" \
		-I"$HACKRF_H_DIR" -I"$USB_INC" -pthread \
		-DLIBRARY_VERSION='"firmware-update"' -DLIBRARY_RELEASE='"firmware-update"' \
		/lib/x86_64-linux-gnu/libusb-1.0.so.0
}

build_tool

if [ "$CONFIRM" != "1" ]; then
	echo "Dry-run only. This did not write SPI flash."
	echo "To flash:  make firmware-update VERSION=$VERSION CONFIRM=1"
	echo
	echo "After a successful write, press RESET on the HackRF (WSL: usbipd detach/attach) and run make info."
	exit 0
fi

echo "Writing SPI flash (hackrf_spiflash -w -R)..."
if [ -n "$SERIAL" ]; then
	"$TOOL" -d "$SERIAL" -w "$bin" -R
else
	"$TOOL" -w "$bin" -R
fi

echo
echo "Write finished. Press RESET on the HackRF, or unplug/replug."
echo "WSL: usbipd detach --busid <id> && usbipd attach --wsl --busid <id>"
echo "Then: make info"
exit 0
