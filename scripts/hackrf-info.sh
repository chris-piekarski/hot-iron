#!/bin/sh
# List attached HackRF devices, the SDK/API this app is built against,
# and whether a newer libhackrf / firmware release exists.
# Invoked by `make info` / `make list-devices`. Always exits 0.
# Set HACKRF_INFO_NO_NET=1 to skip the GitHub latest-release check.

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if [ -t 1 ] && [ -f "$ROOT/scripts/hotiron-banner.sh" ]; then
	sh "$ROOT/scripts/hotiron-banner.sh"
fi
HACKRF_C="$ROOT/src/hotiron/lib/hackrf/host/libhackrf/src/hackrf.c"
HACKRF_H_DIR="$ROOT/src/hotiron/lib/hackrf/host/libhackrf/src"
HACKRF_DIR="$ROOT/src/hotiron/lib/hackrf"
LIST_C="$ROOT/src/hotiron/src-c/hackrf_list.c"
USB_INC="$ROOT/src/hotiron/lib/libusb-1.0.21/include/libusb-1.0"
HELPER="$ROOT/src/hotiron/build/hackrf-list"
USB_DESC="$HACKRF_DIR/firmware/hackrf_usb/usb_descriptor.c"
VERSION_JAVA="$ROOT/src/hotiron/src/main/java/hotiron/Version.java"
POM="$ROOT/src/hotiron/pom.xml"
MAKEFILE_SWEEP="$ROOT/src/hotiron/Makefile"
QUERY_JAVA="$ROOT/src/hotiron/src/main/java/hotiron/nativebridge/HackRFDeviceQuery.java"

product_name() {
	case "$1" in
	6089) echo "HackRF One" ;;
	604b) echo "Jawbreaker" ;;
	cc15) echo "rad1o" ;;
	*) echo "HackRF (unknown PID)" ;;
	esac
}

is_hackrf_pid() {
	case "$1" in
	6089|604b|cc15) return 0 ;;
	*) return 1 ;;
	esac
}

read_sysfs() {
	if [ -f "$1" ]; then
		tr -d '\n' < "$1" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
	fi
}

field() {
	# field "label" "value"
	printf "  %-22s %s\n" "$1" "$2"
}

APP_VERSION=$(sed -n 's/.*version[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$VERSION_JAVA" 2>/dev/null | head -1)
[ -n "$APP_VERSION" ] || APP_VERSION="unknown"

HACKRF_PIN=$(sed -n 's/.*HACKRF_SDK_PIN=[[:space:]]*\(v[^ ]*\).*/\1/p' "$MAKEFILE_SWEEP" 2>/dev/null | head -1)
[ -n "$HACKRF_PIN" ] || HACKRF_PIN=$(sed -n 's/.*git reset --hard \(v[^ ]*\).*/\1/p' "$MAKEFILE_SWEEP" 2>/dev/null | head -1)
[ -n "$HACKRF_PIN" ] || HACKRF_PIN="v2026.01.3"

HACKRF_CHECKED="(submodule not checked out)"
if [ -d "$HACKRF_DIR/.git" ] || [ -f "$HACKRF_DIR/.git" ]; then
	HACKRF_CHECKED=$(git -C "$HACKRF_DIR" describe --tags --always 2>/dev/null || echo unknown)
fi

USB_API_HEX=""
if [ -f "$USB_DESC" ]; then
	USB_API_HEX=$(sed -n 's/.*#define USB_API_VERSION *(0x\([0-9a-fA-F]*\)).*/\1/p' "$USB_DESC" | head -1)
fi
if [ -n "$USB_API_HEX" ]; then
	USB_API_DOT=$(printf '%d.%02d' $((0x$USB_API_HEX / 256)) $((0x$USB_API_HEX % 256)))
	USB_API_DISP="0x$USB_API_HEX ($USB_API_DOT)  [firmware in this SDK tree]"
else
	USB_API_DISP="unknown (init hackrf submodule)"
fi

JNA_VER=$(tr '\n' ' ' < "$POM" 2>/dev/null | sed -n 's/.*<artifactId>jna<\/artifactId>[[:space:]]*<version>\([^<]*\)<\/version>.*/\1/p')
[ -n "$JNA_VER" ] || JNA_VER="unknown"

MIN_FW=$(sed -n 's/.*MIN_FIRMWARE = "\([^"]*\)".*/\1/p' "$QUERY_JAVA" 2>/dev/null | head -1)
[ -n "$MIN_FW" ] || MIN_FW="2024.02.1"

echo "This application (pinned SDK / API)"
echo "==================================="
echo
field "analyzer:" "$APP_VERSION"
field "libhackrf / SDK pin:" "$HACKRF_PIN"
field "SDK checked out:" "$HACKRF_CHECKED"
field "sweep library:" "hackrf_sweep-as-library patch for $HACKRF_PIN"
field "USB API (this SDK):" "$USB_API_DISP"
field "min firmware (tests):" "$MIN_FW"
field "JNA (host binding):" "$JNA_VER"
field "sweep needs USB API:" "1.04+ (hackrf_start_rx_sweep)"
echo

echo "HackRF devices"
echo "=============="
echo

found=0
if [ ! -d /sys/bus/usb/devices ]; then
	echo "USB: no /sys/bus/usb/devices"
	echo "  WSL2 does not see USB until the device is attached:"
	echo "    usbipd list"
	echo "    usbipd attach --wsl --busid <HackRF-BUSID>"
	echo
else
	echo "USB (sysfs)"
	echo "-----------"
	for dir in /sys/bus/usb/devices/*; do
		[ -f "$dir/idVendor" ] || continue
		vendor=$(read_sysfs "$dir/idVendor")
		product=$(read_sysfs "$dir/idProduct")
		[ "$vendor" = "1d50" ] || continue
		is_hackrf_pid "$product" || continue
		found=$((found + 1))
		bus=$(read_sysfs "$dir/busnum")
		dev=$(read_sysfs "$dir/devnum")
		node=""
		if [ -n "$bus" ] && [ -n "$dev" ]; then
			node=$(printf '/dev/bus/usb/%03d/%03d' "$bus" "$dev")
		fi
		echo
		echo "  [$found] $(product_name "$product")"
		echo "    sysfs:          $dir"
		echo "    vendor:product: $vendor:$product"
		mfr=$(read_sysfs "$dir/manufacturer")
		prod=$(read_sysfs "$dir/product")
		serial=$(read_sysfs "$dir/serial")
		[ -n "$mfr" ] && echo "    manufacturer:   $mfr"
		[ -n "$prod" ] && echo "    product:        $prod"
		[ -n "$serial" ] && echo "    serial (USB):   $serial"
		[ -n "$bus" ] && echo "    bus/device:     $bus/$dev"
		speed=$(read_sysfs "$dir/speed")
		[ -n "$speed" ] && echo "    speed:          ${speed} Mb/s"
		bcd=$(read_sysfs "$dir/version")
		[ -n "$bcd" ] && echo "    usb version:    $bcd"
		if [ -n "$node" ] && [ -e "$node" ]; then
			perms=$(ls -l "$node" | awk '{print $1, $3":"$4}')
			echo "    usbfs:          $node  ($perms)"
			if [ -w "$node" ]; then
				echo "    writable:       yes"
			else
				echo "    writable:       no  (firmware/sweep need write)"
				echo "    fix:            sudo chmod a+rw $node"
			fi
		elif [ -n "$node" ]; then
			echo "    usbfs:          $node  (missing)"
		fi
	done
	if [ "$found" -eq 0 ]; then
		echo
		echo "  No HackRF USB device (1d50:6089 / 604b / cc15)."
		if command -v lsusb >/dev/null 2>&1; then
			echo
			echo "  lsusb:"
			lsusb | sed 's/^/    /'
		fi
	fi
	echo
fi

echo "Firmware on device (libhackrf)"
echo "------------------------------"

build_helper() {
	command -v gcc >/dev/null 2>&1 || return 1
	[ -f "$LIST_C" ] && [ -f "$HACKRF_C" ] || return 1
	mkdir -p "$(dirname "$HELPER")"
	if [ -x "$HELPER" ] && [ "$HELPER" -nt "$LIST_C" ] && [ "$HELPER" -nt "$HACKRF_C" ]; then
		return 0
	fi
	libver="unknown"
	if [ -d "$HACKRF_DIR" ]; then
		libver=$(git -C "$HACKRF_DIR" describe --tags --always 2>/dev/null || echo unknown)
	fi
	if gcc -O2 -o "$HELPER" "$LIST_C" "$HACKRF_C" \
		-I"$HACKRF_H_DIR" -I"$USB_INC" -pthread \
		-DLIBRARY_VERSION="\"$libver\"" -DLIBRARY_RELEASE="\"$libver\"" \
		-lusb-1.0 >/dev/null 2>&1; then
		return 0
	fi
	if [ -f /lib/x86_64-linux-gnu/libusb-1.0.so.0 ]; then
		gcc -O2 -o "$HELPER" "$LIST_C" "$HACKRF_C" \
			-I"$HACKRF_H_DIR" -I"$USB_INC" -pthread \
			-DLIBRARY_VERSION="\"$libver\"" -DLIBRARY_RELEASE="\"$libver\"" \
			/lib/x86_64-linux-gnu/libusb-1.0.so.0 \
			>/dev/null 2>&1
		return $?
	fi
	return 1
}

DEVICE_FW=""
DEVICE_API=""
FW_TMP=$(mktemp 2>/dev/null || echo /tmp/hackrf-info.$$)
if build_helper && [ -x "$HELPER" ]; then
	echo
	"$HELPER" 2>&1 | tee "$FW_TMP" || true
	DEVICE_FW=$(sed -n 's/.*firmware: \([^ ]*\).*/\1/p' "$FW_TMP" | head -1)
	DEVICE_API=$(sed -n 's/.*API \([0-9][0-9.]*\).*/\1/p' "$FW_TMP" | head -1)
elif command -v hackrf_info >/dev/null 2>&1; then
	echo
	hackrf_info 2>&1 | tee "$FW_TMP" || true
	DEVICE_FW=$(sed -n 's/.*Firmware Version: \([^ ]*\).*/\1/p' "$FW_TMP" | head -1)
	DEVICE_API=$(sed -n 's/.*API:\([0-9a-fA-F.]*\).*/\1/p' "$FW_TMP" | head -1)
else
	echo
	echo "  Firmware not read: no libhackrf helper and hackrf_info not in PATH."
	if [ ! -f "$HACKRF_C" ]; then
		echo "  Init the hackrf submodule to build the helper:"
		echo "    git submodule update --init src/hotiron/lib/hackrf"
	fi
	echo "  Or install the official tool: sudo apt install hackrf"
fi
rm -f "$FW_TMP"

echo
echo "Updates (Great Scott Gadgets hackrf)"
echo "------------------------------------"
echo

python3 - "$HACKRF_PIN" "$MIN_FW" "$DEVICE_FW" "${HACKRF_INFO_NO_NET:-}" <<'PY'
import json, os, re, ssl, sys, urllib.request

pin, min_fw, device_fw, no_net = sys.argv[1:5]

def parts(s):
    if not s:
        return None
    m = re.search(r"(\d{4})\.(\d{1,2})\.(\d+)", s)
    if not m:
        return None
    return tuple(int(x) for x in m.groups())

def cmp(a, b):
    pa, pb = parts(a), parts(b)
    if pa is None or pb is None:
        return None
    return (pa > pb) - (pa < pb)

def line(label, value):
    print("  %-22s %s" % (label, value))

latest_tag = None
latest_date = None
latest_notes = None
latest_url = "https://github.com/greatscottgadgets/hackrf/releases/latest"
err = None
if no_net not in ("", "0", "false", "False"):
    err = "skipped (HACKRF_INFO_NO_NET)"
else:
    url = "https://api.github.com/repos/greatscottgadgets/hackrf/releases/latest"
    req = urllib.request.Request(url, headers={
        "Accept": "application/vnd.github+json",
        "User-Agent": "hotiron-make-info",
    })
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        ctx = ssl.create_default_context()
        with urllib.request.urlopen(req, timeout=8, context=ctx) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        latest_tag = data.get("tag_name") or data.get("name")
        latest_date = (data.get("published_at") or "")[:10]
        body = (data.get("body") or "").strip().replace("\r\n", "\n")
        notes = [ln.strip() for ln in body.split("\n") if ln.strip()][:4]
        latest_notes = notes
        latest_url = data.get("html_url") or latest_url
    except Exception as e:
        err = str(e)

if err and latest_tag is None:
    line("latest GSG release:", "could not check — " + err)
    line("check manually:", latest_url)
else:
    extra = " (%s)" % latest_date if latest_date else ""
    line("latest GSG release:", "%s%s" % (latest_tag, extra))
    line("release notes:", latest_url)
    if latest_notes:
        for ln in latest_notes:
            print("    " + ln[:100])

rel = cmp(pin, latest_tag) if latest_tag else None
if rel is None and latest_tag:
    line("SDK vs latest:", "could not compare %s and %s" % (pin, latest_tag))
elif latest_tag and rel == 0:
    line("SDK / libhackrf:", "current (%s)" % pin)
elif latest_tag and rel < 0:
    line("SDK / libhackrf:", "newer available: %s → %s" % (pin, latest_tag))
    print("    recommendation:   firmware on the radio can be updated independently")
    print("                      (https://hackrf.readthedocs.io/en/latest/updating_firmware.html)")
    print("    host SDK:         this app pins libhackrf; a newer GSG tag needs the")
    print("                      sweep-as-library patch rebased before changing HACKRF_SDK_PIN.")
    print("    host+firmware:    GSG requires matching libhackrf + firmware when you upgrade both.")
elif latest_tag:
    line("SDK / libhackrf:", "pin %s is newer than latest tag %s" % (pin, latest_tag))

if device_fw:
    dmin = cmp(device_fw, min_fw)
    dlat = cmp(device_fw, latest_tag) if latest_tag else None
    status = "firmware %s" % device_fw
    if dmin is not None and dmin < 0:
        status += " — BELOW app minimum %s (update firmware)" % min_fw
    elif dlat is not None and dlat < 0:
        status += " — older than latest %s (firmware update recommended)" % latest_tag
    elif dlat == 0:
        status += " — matches latest release"
    else:
        status += " — meets app minimum %s" % min_fw
    line("device firmware:", status)
else:
    line("device firmware:", "not read (need writable usbfs, then re-run make info)")

print("    firmware how-to:  https://hackrf.readthedocs.io/en/latest/updating_firmware.html")
PY

echo
echo "Summary: $found HackRF USB device(s) enumerated. App SDK pin $HACKRF_PIN."
exit 0
